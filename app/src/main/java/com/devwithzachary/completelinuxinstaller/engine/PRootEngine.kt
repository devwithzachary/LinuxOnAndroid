package com.devwithzachary.completelinuxinstaller.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PRootConfig(
    val rootfsDir: File,
    val tmpDir: File,
    val bindSdCard: Boolean = true,
    val externalDirectory: File? = null,
    val workingDir: String = "/root",
    val defaultShell: String = "/bin/bash"
)

class PRootEngine(val context: Context) {

    companion object {
        private const val TAG = "PRootEngine"
        private const val PROOT_BIN_NAME = "proot"
    }

    private val filesDir: File get() = context.filesDir
    private val binDir: File get() = File(filesDir, "bin").apply { if (!exists()) mkdirs() }
    val rootfsDir: File get() {
        val legacy = File(filesDir, "ubuntu_rootfs")
        if (ContainerManager.isRealRootfs(legacy)) {
            return legacy
        }
        val containersDir = File(filesDir, "containers")
        if (containersDir.exists() && containersDir.isDirectory) {
            val prefs = context.getSharedPreferences("containers_prefs", Context.MODE_PRIVATE)
            val defaultId = prefs.getString("default_container_id", null)
            if (defaultId != null) {
                val defaultDir = File(containersDir, defaultId)
                val defaultRootfs = File(defaultDir, "rootfs")
                if (ContainerManager.isRealRootfs(defaultRootfs)) return defaultRootfs
                if (ContainerManager.isRealRootfs(defaultDir)) return defaultDir
            }
            val subdirs = containersDir.listFiles()
            if (subdirs != null) {
                for (sub in subdirs) {
                    val candidate = File(sub, "rootfs")
                    if (ContainerManager.isRealRootfs(candidate)) return candidate
                    if (ContainerManager.isRealRootfs(sub)) return sub
                }
            }
        }
        return legacy
    }

    val tmpDir: File get() = File(filesDir, "tmp").apply { if (!exists()) mkdirs() }
    val prootBinary: File get() {
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val libProot = File(nativeLibDir, "libproot.so")
        if (libProot.exists() && libProot.length() > 0L) {
            return libProot
        }
        val downloadedProot = File(binDir, "proot.native")
        if (downloadedProot.exists() && downloadedProot.length() > 0L) {
            return downloadedProot
        }
        return File(binDir, PROOT_BIN_NAME)
    }

    fun isRootfsInstalled(dir: File = rootfsDir): Boolean {
        if (ContainerManager.isRealRootfs(dir)) return true
        if (dir == rootfsDir) {
            val containersDir = File(filesDir, "containers")
            if (containersDir.exists() && containersDir.isDirectory) {
                val subdirs = containersDir.listFiles()
                if (subdirs != null) {
                    for (sub in subdirs) {
                        if (ContainerManager.isRealRootfs(sub) || ContainerManager.isRealRootfs(File(sub, "rootfs"))) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    suspend fun ensurePRootExecutable(): File = withContext(Dispatchers.IO) {
        val oldProot = File(binDir, "proot")
        if (oldProot.exists()) {
            oldProot.delete()
        }
        val prootExec = prootBinary
        Log.d(TAG, "Using bundled native library proot executable at: ${prootExec.absolutePath}")
        return@withContext prootExec
    }

    private fun addBindMount(cmdList: MutableList<String>, path: String) {
        val file = File(path)
        if (file.exists()) {
            cmdList.add("-b")
            cmdList.add(file.absolutePath)
        }
    }

    private fun ensureFakeProcFiles(): File {
        val fakeProcDir = File(filesDir, "fake_proc").apply { if (!exists()) mkdirs() }
        val numCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        val statFile = File(fakeProcDir, "stat")
        val statContent = buildString {
            appendLine("cpu  2000 0 2000 200000 0 0 0 0 0 0")
            for (i in 0 until numCores) {
                appendLine("cpu$i ${2000 / numCores} 0 ${2000 / numCores} ${200000 / numCores} 0 0 0 0 0 0")
            }
            appendLine("intr 1000 0 0")
            appendLine("ctxt 5000")
            appendLine("btime ${System.currentTimeMillis() / 1000 - 86400}")
            appendLine("processes 1000")
            appendLine("procs_running 1")
            appendLine("procs_blocked 0")
            appendLine("softirq 1000 0 0 0 0 0 0 0 0 0")
        }
        statFile.writeText(statContent)

        val versionFile = File(fakeProcDir, "version")
        if (!versionFile.exists() || versionFile.length() == 0L) {
            versionFile.writeText("Linux version 6.1.0-android-proot (gcc version 11.4.0) #1 SMP PREEMPT\n")
        }

        val vmstatFile = File(fakeProcDir, "vmstat")
        if (!vmstatFile.exists() || vmstatFile.length() == 0L) {
            vmstatFile.writeText("nr_free_pages 100000\nnr_alloc_batch 1000\npgpgin 50000\npgpgout 50000\npswpin 0\npswpout 0\n")
        }

        return fakeProcDir
    }

    fun buildPRootCommand(
        config: PRootConfig = PRootConfig(rootfsDir = rootfsDir, tmpDir = tmpDir),
        command: List<String> = listOf("/bin/bash", "-l"),
        loginUser: String? = null,
        candidateShells: List<String>? = null
    ): List<String> {
        val cmdList = mutableListOf<String>()
        val prootExec = prootBinary
        val hostFallbackPath = "${config.rootfsDir.absolutePath}/usr/local/sbin:${config.rootfsDir.absolutePath}/usr/local/bin:${config.rootfsDir.absolutePath}/usr/sbin:${config.rootfsDir.absolutePath}/usr/bin:${config.rootfsDir.absolutePath}/sbin:${config.rootfsDir.absolutePath}/bin:/system/bin"

        val usePRoot = prootExec.exists() && prootExec.length() > 0L

        val targetRootfs = config.rootfsDir
        val targetUser = loginUser?.takeIf { it.isNotBlank() }

        // Heal stale /usr/bin/bash -> /bin/sh symlinks if real GNU bash is present at /bin/bash
        val binBash = File(targetRootfs, "bin/bash")
        val usrBinBash = File(targetRootfs, "usr/bin/bash")
        if (binBash.exists() && binBash.length() > 10000L) {
            try {
                val canonical = if (usrBinBash.exists()) usrBinBash.canonicalPath else ""
                if (!usrBinBash.exists() || canonical.endsWith("/sh") || canonical.endsWith("/busybox")) {
                    usrBinBash.delete()
                    android.system.Os.symlink("/bin/bash", usrBinBash.absolutePath)
                }
            } catch (_: Exception) {}
        }

        fun isValidShell(relPath: String): Boolean {
            val file = File(targetRootfs, relPath.removePrefix("/"))
            if (!ContainerManager.fileOrGuestSymlinkExists(targetRootfs, relPath)) return false
            if (relPath.contains("bash")) {
                try {
                    val canonical = if (file.exists()) file.canonicalPath else ""
                    if (canonical.endsWith("/busybox") || canonical.endsWith("/sh")) {
                        return false
                    }
                } catch (_: Exception) {}
            }
            return true
        }

        val defaultCandidates = candidateShells ?: listOf(
            "/bin/bash",
            "/usr/bin/bash",
            "/bin/dash",
            "/usr/bin/dash",
            "/bin/ash",
            "/usr/bin/ash",
            "/bin/sh",
            "/usr/bin/sh"
        )
        val guestShell = defaultCandidates.firstOrNull { isValidShell(it) } ?: "/bin/sh"

        val etcDir = File(targetRootfs, "etc").apply { if (!exists()) mkdirs() }
        val profileD = File(etcDir, "profile.d").apply { if (!exists()) mkdirs() }
        val pathScript = File(profileD, "00-linuxonandroid-path.sh")
        if (!pathScript.exists() || !pathScript.readText().contains("/usr/sbin")) {
            try {
                pathScript.writeText("export PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\"\n")
                pathScript.setExecutable(true, false)
            } catch (_: Exception) {}
        }
        FastfetchConfig.ensureFastfetchConfig(targetRootfs)
        NetworkShims.ensureNetworkShims(targetRootfs, context)
        ensureContainerIntegrity(targetRootfs)
        val loginDefs = File(etcDir, "login.defs")
        if (loginDefs.exists()) {
            try {
                val content = loginDefs.readText()
                if (content.contains("ENV_PATH") && !content.contains("ENV_PATH\tPATH=/usr/local/sbin")) {
                    loginDefs.writeText(content.replace(Regex("ENV_PATH\\s+PATH=[^\n]+"), "ENV_PATH\tPATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"))
                }
            } catch (_: Exception) {}
        }
        val bashrc = File(etcDir, "bash.bashrc")
        if (bashrc.exists()) {
            try {
                val content = bashrc.readText()
                if (!content.contains("/usr/sbin")) {
                    bashrc.appendText("\nexport PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\"\n")
                }
            } catch (_: Exception) {}
        }

        val usrLocalBin = File(targetRootfs, "usr/local/bin").apply { if (!exists()) mkdirs() }
        val serviceShim = File(usrLocalBin, "service")
        if (!serviceShim.exists() || serviceShim.length() == 0L) {
            try {
                serviceShim.writeText(
                    """
                    #!/bin/sh
                    NAME="${'$'}1"
                    ACTION="${'$'}2"
                    shift 2 2>/dev/null || true
                    if [ -x "/usr/sbin/service" ]; then
                        exec /usr/sbin/service "${'$'}NAME" "${'$'}ACTION" "${'$'}@"
                    fi
                    if [ -x "/etc/init.d/${'$'}NAME" ]; then
                        exec "/etc/init.d/${'$'}NAME" "${'$'}ACTION" "${'$'}@"
                    fi
                    if [ "${'$'}NAME" = "nginx" ]; then
                        case "${'$'}ACTION" in
                            start) exec /usr/sbin/nginx "${'$'}@" 2>/dev/null || exec /usr/bin/nginx "${'$'}@" 2>/dev/null || exec nginx "${'$'}@" ;;
                            stop) exec /usr/sbin/nginx -s stop 2>/dev/null || exec nginx -s stop ;;
                            reload) exec /usr/sbin/nginx -s reload 2>/dev/null || exec nginx -s reload ;;
                            status) ps aux | grep -v grep | grep nginx ;;
                        esac
                    fi
                    if [ "${'$'}NAME" = "ssh" ] || [ "${'$'}NAME" = "sshd" ]; then
                        case "${'$'}ACTION" in
                            start)
                                sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true
                                exec /usr/sbin/sshd "${'$'}@"
                                ;;
                            stop) pkill -f sshd ;;
                            status) ps aux | grep -v grep | grep sshd ;;
                        esac
                    fi
                    """.trimIndent() + "\n"
                )
                serviceShim.setExecutable(true, false)
            } catch (_: Exception) {}
        }

        val sshConfigFile = File(targetRootfs, "etc/ssh/sshd_config")
        if (sshConfigFile.exists()) {
            try {
                val content = sshConfigFile.readText()
                if (content.contains(Regex("(?m)^Subsystem\\s+sftp"))) {
                    sshConfigFile.writeText(content.replace(Regex("(?m)^Subsystem\\s+sftp"), "#Subsystem sftp"))
                }
            } catch (_: Exception) {}
        }

        if (File(targetRootfs, "usr/sbin/service").exists() && !File(targetRootfs, "usr/bin/service").exists()) {
            try {
                android.system.Os.symlink(
                    "/usr/sbin/service",
                    File(targetRootfs, "usr/bin/service").absolutePath
                )
            } catch (_: Exception) {
                try {
                    File(targetRootfs, "usr/sbin/service").copyTo(File(targetRootfs, "usr/bin/service"), overwrite = true)
                    File(targetRootfs, "usr/bin/service").setExecutable(true, false)
                } catch (_: Exception) {}
            }
        }

        if (targetUser != null && targetUser != "root") {
            val passwdFile = File(etcDir, "passwd")
            val groupFile = File(etcDir, "group")
            val shadowFile = File(etcDir, "shadow")
            val homeDir = File(targetRootfs, "home/$targetUser").apply { if (!exists()) mkdirs() }
            val passwdContent = if (passwdFile.exists()) try { passwdFile.readText() } catch (_: Exception) { "" } else ""
            if (!passwdContent.lines().any { it.startsWith("$targetUser:") }) {
                try {
                    passwdFile.appendText("$targetUser:x:1000:1000:$targetUser:/home/$targetUser:$guestShell\n")
                    if (!groupFile.exists() || !groupFile.readText().lines().any { it.startsWith("$targetUser:") }) {
                        groupFile.appendText("$targetUser:x:1000:\n")
                    }
                    if (!shadowFile.exists() || !shadowFile.readText().lines().any { it.startsWith("$targetUser:") }) {
                        shadowFile.appendText("$targetUser:*:19700:0:99999:7:::\n")
                    }
                } catch (_: Exception) {}
            }

            val userBashrc = File(homeDir, ".bashrc")
            if (!userBashrc.exists()) {
                try {
                    val skelBashrc = File(targetRootfs, "etc/skel/.bashrc")
                    if (skelBashrc.exists()) {
                        skelBashrc.copyTo(userBashrc, overwrite = true)
                    } else {
                        userBashrc.writeText("export PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\"\n")
                    }
                } catch (_: Exception) {}
            }
            if (userBashrc.exists()) {
                try {
                    val content = userBashrc.readText()
                    if (!content.contains("/usr/sbin")) {
                        userBashrc.appendText("\nexport PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\"\n")
                    }
                } catch (_: Exception) {}
            }

            val userProfile = File(homeDir, ".profile")
            if (!userProfile.exists()) {
                try {
                    val skelProfile = File(targetRootfs, "etc/skel/.profile")
                    if (skelProfile.exists()) {
                        skelProfile.copyTo(userProfile, overwrite = true)
                    } else {
                        userProfile.writeText("export PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\"\n")
                    }
                } catch (_: Exception) {}
            }
            if (userProfile.exists()) {
                try {
                    val content = userProfile.readText()
                    if (!content.contains("/usr/sbin")) {
                        userProfile.appendText("\nexport PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\"\n")
                    }
                } catch (_: Exception) {}
            }
        }

        val suBin = when {
            File(targetRootfs, "usr/bin/su").exists() -> "/usr/bin/su"
            File(targetRootfs, "bin/su").exists() -> "/bin/su"
            else -> "/bin/su"
        }

        val effectiveCommand = if (targetUser != null && targetUser != "root") {
            val requestedShell = command.firstOrNull()?.takeIf {
                it.startsWith("/") && isValidShell(it)
            }
            val shellToUse = requestedShell ?: guestShell
            listOf(suBin, "-s", shellToUse, "-", targetUser)
        } else if (command.isNotEmpty() && command[0].startsWith("/") && !isValidShell(command[0])) {
            listOf(guestShell) + command.drop(1)
        } else {
            command
        }

        val targetWorkDir = if (config.workingDir == "/root" && targetUser != null && targetUser != "root") {
            "/home/$targetUser"
        } else {
            config.workingDir
        }

        if (usePRoot) {
            val workDirFile = File(config.rootfsDir, targetWorkDir.removePrefix("/"))
            if (!workDirFile.exists()) {
                workDirFile.mkdirs()
            }

            cmdList.add(prootExec.absolutePath)
            cmdList.add("-0")
            cmdList.add("-l")
            cmdList.add("-r")
            cmdList.add(config.rootfsDir.absolutePath)

            val mounts = mutableListOf(
                "/proc", "/sys", "/dev", "/dev/pts",
                "/proc/self/fd:/dev/fd",
                "/proc/self/fd/0:/dev/stdin",
                "/proc/self/fd/1:/dev/stdout",
                "/proc/self/fd/2:/dev/stderr",
                "/proc/self:/proc/1"
            )
            val emptySelinuxDir = File(filesDir, "empty_selinux").apply { if (!exists()) mkdirs() }
            if (File("/sys/fs/selinux").exists()) {
                mounts.add("${emptySelinuxDir.absolutePath}:/sys/fs/selinux")
            }
            if (config.bindSdCard) {
                val appExternalFilesDir = context.getExternalFilesDir(null)
                val appSdcard = if (appExternalFilesDir != null) {
                    File(appExternalFilesDir, "sdcard").apply { if (!exists()) mkdirs() }
                } else null

                try {
                    File(config.rootfsDir, "sdcard").mkdirs()
                    File(config.rootfsDir, "mnt/sdcard").mkdirs()
                    File(config.rootfsDir, "storage/emulated/0").mkdirs()
                    File(config.rootfsDir, "root/Downloads").mkdirs()
                    File(config.rootfsDir, "sdcard/AppStorage").mkdirs()
                    File(config.rootfsDir, "sdcard/Download").mkdirs()
                    File(config.rootfsDir, "sdcard/Documents").mkdirs()
                } catch (_: Exception) {}

                if (appSdcard != null) {
                    mounts.add("${appSdcard.absolutePath}:/sdcard")
                    mounts.add("${appSdcard.absolutePath}:/mnt/sdcard")
                } else if (appExternalFilesDir != null) {
                    mounts.add("${appExternalFilesDir.absolutePath}:/sdcard")
                    mounts.add("${appExternalFilesDir.absolutePath}:/mnt/sdcard")
                }

                val hostSdcard = Environment.getExternalStorageDirectory()
                if (hostSdcard.exists()) {
                    mounts.add("${hostSdcard.absolutePath}:/storage/emulated/0")
                }

                val downloadsHost = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsHost.exists()) {
                    mounts.add("${downloadsHost.absolutePath}:/root/Downloads")
                    mounts.add("${downloadsHost.absolutePath}:/sdcard/Download")
                }

                val documentsHost = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (documentsHost.exists()) {
                    mounts.add("${documentsHost.absolutePath}:/sdcard/Documents")
                }

                if (appExternalFilesDir != null) {
                    mounts.add("${appExternalFilesDir.absolutePath}:/sdcard/AppStorage")
                }
                val storageDir = File("/storage")
                if (storageDir.exists()) {
                    mounts.add("/storage")
                }
            }

            // Auto-mount container's dedicated external files directory at /external
            val effectiveExternalDir = config.externalDirectory ?: run {
                try {
                    val cm = ContainerManager(context)
                    val container = cm.containers.value.find { it.rootDirPath == config.rootfsDir.absolutePath }
                    if (container != null && container.externalFolderName.isNotBlank()) {
                        container.getExternalDirectory(context)
                    } else null
                } catch (_: Exception) { null }
            }
            if (effectiveExternalDir != null && effectiveExternalDir.exists()) {
                val guestExternal = File(config.rootfsDir, "external")
                if (!guestExternal.exists()) {
                    try { guestExternal.mkdirs() } catch (_: Exception) {}
                }
                mounts.add("${effectiveExternalDir.absolutePath}:/external")
            }

            for (m in mounts) {
                if (m.contains(":")) {
                    val parts = m.split(":", limit = 2)
                    val hostPath = parts[0].trim()
                    val guestPath = parts[1].trim()
                    val hostFile = File(hostPath)
                    // If host directory does not exist, attempt to create it if within external storage
                    if (!hostFile.exists() && (hostPath.startsWith("/sdcard") || hostPath.startsWith("/storage"))) {
                        try { hostFile.mkdirs() } catch (_: Exception) {}
                    }
                    if (hostFile.exists()) {
                        // Ensure guest mountpoint exists inside rootfs to prevent PRoot ENOENT startup errors
                        val guestMountpoint = File(config.rootfsDir, guestPath.removePrefix("/"))
                        if (!guestMountpoint.exists()) {
                            try { guestMountpoint.mkdirs() } catch (_: Exception) {}
                        }
                        cmdList.add("-b")
                        cmdList.add("$hostPath:$guestPath")
                    } else {
                        Log.w(TAG, "Skipping custom bind mount; host path does not exist: $hostPath")
                    }
                } else {
                    addBindMount(cmdList, m)
                }
            }

            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            if (nativeLibDir.exists()) {
                addBindMount(cmdList, nativeLibDir.absolutePath)
                val loaderFile = File(nativeLibDir, "libproot_loader.so")
                if (loaderFile.exists() && loaderFile.length() > 0L) {
                    try {
                        val guestUsrLocalLib = File(config.rootfsDir, "usr/local/lib").apply { mkdirs() }
                        val guestLoader = File(guestUsrLocalLib, "libproot_loader.so")
                        if (!guestLoader.exists() || guestLoader.length() != loaderFile.length()) {
                            loaderFile.copyTo(guestLoader, overwrite = true)
                            guestLoader.setExecutable(true, false)
                            guestLoader.setReadable(true, false)
                        }
                    } catch (_: Exception) {}
                }
            }

            // Bind-mount synthetic /proc files to bypass Android 10+ SELinux restrictions for htop/top/free
            val fakeProcDir = ensureFakeProcFiles()
            val fakeStat = File(fakeProcDir, "stat")
            if (fakeStat.exists()) {
                cmdList.add("-b")
                cmdList.add("${fakeStat.absolutePath}:/proc/stat")
            }
            val fakeVersion = File(fakeProcDir, "version")
            if (fakeVersion.exists()) {
                cmdList.add("-b")
                cmdList.add("${fakeVersion.absolutePath}:/proc/version")
            }
            val fakeVmstat = File(fakeProcDir, "vmstat")
            if (fakeVmstat.exists()) {
                cmdList.add("-b")
                cmdList.add("${fakeVmstat.absolutePath}:/proc/vmstat")
            }

            if (config.tmpDir.exists()) {
                cmdList.add("-b")
                cmdList.add("${config.tmpDir.absolutePath}:/tmp")
            }

            val hostCache = context.cacheDir
            if (hostCache.exists()) {
                cmdList.add("-b")
                cmdList.add(hostCache.absolutePath)
            }

            val l2sDir = File(filesDir, "l2s").apply { mkdirs() }
            if (l2sDir.exists()) {
                cmdList.add("-b")
                cmdList.add(l2sDir.absolutePath)
                val dataDataL2s = File(l2sDir.absolutePath.replace("/data/user/0/", "/data/data/"))
                if (dataDataL2s.exists() && dataDataL2s.absolutePath != l2sDir.absolutePath) {
                    cmdList.add("-b")
                    cmdList.add(dataDataL2s.absolutePath)
                }
                val user0L2s = File(l2sDir.absolutePath.replace("/data/data/", "/data/user/0/"))
                if (user0L2s.exists() && user0L2s.absolutePath != l2sDir.absolutePath) {
                    cmdList.add("-b")
                    cmdList.add(user0L2s.absolutePath)
                }
            }

            cmdList.add("-w")
            cmdList.add(targetWorkDir)

            cmdList.addAll(effectiveCommand)
        } else {
            cmdList.add("/system/bin/sh")
            cmdList.add("-c")
            val execCmd = if (command.size >= 3 && (command[0] == "/bin/sh" || command[0] == "/bin/bash") && command[1] == "-c") {
                command[2]
            } else if (command.isNotEmpty()) {
                command.joinToString(" ")
            } else {
                "/system/bin/sh"
            }
            val userHomeDir = if (targetUser != null && targetUser != "root") "/home/$targetUser" else config.workingDir
            val chrootCmd = buildString {
                append("export PROOT_TMP_DIR='${config.tmpDir.absolutePath}'; ")
                append("export PROOT_NO_SECCOMP='1'; ")
                append("export PROOT_FORCE_SETID='1'; ")
                append("export HOME='$userHomeDir'; ")
                append("export PATH='$hostFallbackPath'; ")
                append("export TERM='xterm-256color'; ")
                append("export LANG='C.UTF-8'; ")
                append("export TMPDIR='/tmp'; ")
                append("export TMP='/tmp'; ")
                append("cd '${config.rootfsDir.absolutePath}' 2>/dev/null; ")
                append("exec $execCmd")
            }
            cmdList.add(chrootCmd)
        }

        return cmdList
    }

    fun getEnvironmentVariables(loginUser: String? = null): Map<String, String> {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val loaderFile = File(nativeLibDir, "libproot_loader.so")
        val loader32File = File(nativeLibDir, "libproot_loader32.so")
        val l2sDir = File(filesDir, "l2s").apply {
            mkdirs()
            try {
                setWritable(true, false)
                setReadable(true, false)
                setExecutable(true, false)
            } catch (_: Exception) {}
        }

        val guestPath = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        val targetUser = loginUser?.takeIf { it.isNotBlank() } ?: "root"
        val userHome = if (targetUser == "root") "/root" else "/home/$targetUser"

        val codename = try {
            val osReleaseFile = File(rootfsDir, "etc/os-release")
            if (osReleaseFile.exists()) {
                val match = Regex("""(?:UBUNTU_CODENAME|VERSION_CODENAME)=["']?([a-zA-Z0-9_-]+)["']?""").find(osReleaseFile.readText())
                match?.groupValues?.get(1) ?: "resolute"
            } else "resolute"
        } catch (_: Exception) {
            "resolute"
        }

        val env = mutableMapOf(
            "LD_LIBRARY_PATH" to nativeLibDir,
            "PROOT_TMP_DIR" to tmpDir.absolutePath,
            "PROOT_L2S_DIR" to l2sDir.absolutePath,
            "PROOT_UNSETENV" to "LD_LIBRARY_PATH:TMPDIR:TMP",
            "PROOT_NO_SECCOMP" to "1",
            "PROOT_FORCE_SETID" to "1",
            "PROOT_LINK2SYMLINK" to "1",
            "USER" to targetUser,
            "LOGNAME" to targetUser,
            "HOME" to userHome,
            "PYTHONPATH" to "/usr/lib/python3/dist-packages",
            "PATH" to guestPath,
            "TERM" to "xterm-256color",
            "LANG" to "C.UTF-8",
            "TMPDIR" to "/tmp",
            "TMP" to "/tmp",
            "UBUNTU_CODENAME" to codename,
            "VERSION_CODENAME" to codename,
            "GLYCIN_DISABLE_SANDBOX" to "1",
            "GLYCIN_ENABLE_SANDBOX" to "0",
            "LIBGL_ALWAYS_SOFTWARE" to "1"
        )

        if (loaderFile.exists() && loaderFile.length() > 0L) {
            env["PROOT_LOADER"] = loaderFile.absolutePath
        }
        if (loader32File.exists() && loader32File.length() > 0L) {
            env["PROOT_LOADER32"] = loader32File.absolutePath
        }

        return env
    }

    private fun ensureContainerIntegrity(targetRootfs: File) {
        try {
            val etcDir = File(targetRootfs, "etc").apply { if (!exists()) mkdirs() }

            // 1. D-Bus Machine ID (must be valid 32-char hex string, not empty or "uninitialized")
            val machineIdFile = File(etcDir, "machine-id")
            val dbusDir = File(targetRootfs, "var/lib/dbus").apply { if (!exists()) mkdirs() }
            val dbusMachineIdFile = File(dbusDir, "machine-id")
            val currentId = if (machineIdFile.exists()) {
                try { machineIdFile.readText().trim() } catch (_: Exception) { "" }
            } else ""
            val isValid = currentId.length == 32 &&
                    currentId != "uninitialized" &&
                    currentId.all { it in "0123456789abcdefABCDEF" }

            val effectiveId = if (isValid) currentId else java.util.UUID.randomUUID().toString().replace("-", "").lowercase()
            if (!isValid) {
                try {
                    machineIdFile.writeText("$effectiveId\n")
                    dbusMachineIdFile.writeText("$effectiveId\n")
                } catch (_: Exception) {}
            } else if (!dbusMachineIdFile.exists() || dbusMachineIdFile.readText().trim() != effectiveId) {
                try {
                    dbusMachineIdFile.writeText("$effectiveId\n")
                } catch (_: Exception) {}
            }

            // 2. Fedora / RPM SELinux configuration and plugin removal (SELinux syscalls fail inside PRoot)
            val selinuxConfigFile = File(targetRootfs, "etc/selinux/config")
            if (!selinuxConfigFile.exists() || !selinuxConfigFile.readText().contains("SELINUX=disabled")) {
                try {
                    selinuxConfigFile.parentFile?.mkdirs()
                    selinuxConfigFile.writeText("SELINUX=disabled\nSELINUXTYPE=targeted\n")
                } catch (_: Exception) {}
            }
            try {
                File(targetRootfs, "sys/fs/selinux").mkdirs()
            } catch (_: Exception) {}

            val selinuxRpmPlugins = listOf(
                File(targetRootfs, "usr/lib64/rpm-plugins/selinux.so"),
                File(targetRootfs, "usr/lib/rpm-plugins/selinux.so")
            )
            for (so in selinuxRpmPlugins) {
                if (so.exists()) {
                    try { so.delete() } catch (_: Exception) {}
                }
            }

            // 3. Bubblewrap (bwrap) bypass shim for Glycin / GTK SVG loaders in PRoot
            val usrBin = File(targetRootfs, "usr/bin")
            val usrLocalBin = File(targetRootfs, "usr/local/bin").apply { if (!exists()) mkdirs() }
            val bwrapFile = File(usrBin, "bwrap")
            val bwrapReal = File(usrBin, "bwrap.real")
            val bwrapLocalFile = File(usrLocalBin, "bwrap")

            if (bwrapFile.exists() && !bwrapReal.exists()) {
                try {
                    val isElf = bwrapFile.inputStream().use { stream ->
                        val bytes = stream.readNBytes(4)
                        bytes.size >= 4 && bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()
                    }
                    if (isElf) {
                        bwrapFile.renameTo(bwrapReal)
                    }
                } catch (_: Exception) {}
            }

            val bwrapShimScript = """
                #!/bin/sh
                # Bubblewrap bypass shim for PRoot environments
                if [ "${'$'}1" = "--version" ] || [ "${'$'}1" = "-v" ]; then
                    echo "bubblewrap 0.11.0 (PRoot shim)"
                    exit 0
                fi

                while [ ${'$'}# -gt 0 ]; do
                    case "${'$'}1" in
                        --)
                            shift
                            if [ -n "${'$'}1" ]; then
                                exec "${'$'}@"
                            fi
                            exit 0
                            ;;
                        /*|./*)
                            if [ -x "${'$'}1" ] && [ ! -d "${'$'}1" ]; then
                                exec "${'$'}@"
                            fi
                            ;;
                    esac
                    shift
                done
                exit 0
            """.trimIndent() + "\n"

            if (!bwrapLocalFile.exists() || !bwrapLocalFile.readText().contains("PRoot shim")) {
                try {
                    bwrapLocalFile.writeText(bwrapShimScript)
                    bwrapLocalFile.setExecutable(true, false)
                } catch (_: Exception) {}
            }

            if (usrBin.exists() && (!bwrapFile.exists() || !bwrapFile.readText().contains("PRoot shim"))) {
                try {
                    bwrapFile.writeText(bwrapShimScript)
                    bwrapFile.setExecutable(true, false)
                } catch (_: Exception) {}
            }

            // 4. Heal vncserver executable if corrupted or 0-byte
            val usrBinVnc = File(targetRootfs, "usr/bin/vncserver")
            val usrLocalBinVnc = File(targetRootfs, "usr/local/bin/vncserver")
            if (usrLocalBinVnc.exists() && usrLocalBinVnc.length() > 0L && (!usrBinVnc.exists() || usrBinVnc.length() == 0L)) {
                try {
                    usrLocalBinVnc.copyTo(usrBinVnc, overwrite = true)
                    usrBinVnc.setExecutable(true, false)
                } catch (_: Exception) {}
            }

            // 5. VNC xstartup validation: if xstartup exists and lacks sandbox overrides or uses startxfce4, heal it
            val vncXstartup = File(targetRootfs, "etc/vnc/xstartup")
            val hasVnc = vncXstartup.exists() || usrBinVnc.exists() || usrLocalBinVnc.exists()
            if (hasVnc) {
                val content = try { vncXstartup.readText() } catch (_: Exception) { "" }
                if (!content.contains("GLYCIN_DISABLE_SANDBOX") || content.contains("--exit-with-session") || content.contains("exec xfdesktop") || !content.contains("export PATH=") || !content.contains("command -v startxfce4") || !content.contains("xfconfd")) {
                    val standardXstartup = """
                        #!/bin/sh
                        export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/xfce4/xfconf:/usr/lib64/xfce4/xfconf:/usr/libexec/xfce4/xfconf:${'$'}PATH"
                        unset SESSION_MANAGER
                        unset DBUS_SESSION_BUS_ADDRESS
                        export XDG_SESSION_TYPE=x11
                        export XDG_CURRENT_DESKTOP=XFCE
                        export DESKTOP_SESSION=xfce
                        export NO_AT_BRIDGE=1
                        export GDK_BACKEND=x11
                        export GTK_OVERLAY_SCROLLING=0
                        export GLYCIN_DISABLE_SANDBOX=1
                        export GLYCIN_ENABLE_SANDBOX=0
                        export LIBGL_ALWAYS_SOFTWARE=1
                        [ -r "${'$'}HOME/.Xresources" ] && xrdb "${'$'}HOME/.Xresources" 2>/dev/null || true

                        if command -v dbus-launch >/dev/null 2>&1; then
                            eval ${'$'}(dbus-launch --sh-syntax)
                        fi

                        if command -v xfconfd >/dev/null 2>&1; then
                            xfconfd &
                        elif [ -x /usr/lib64/xfce4/xfconf/xfconfd ]; then
                            /usr/lib64/xfce4/xfconf/xfconfd &
                        elif [ -x /usr/lib/xfce4/xfconf/xfconfd ]; then
                            /usr/lib/xfce4/xfconf/xfconfd &
                        elif [ -x /usr/libexec/xfce4/xfconf/xfconfd ]; then
                            /usr/libexec/xfce4/xfconf/xfconfd &
                        fi

                        xsetroot -solid "#1e293b" 2>/dev/null || true
                        xfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s false 2>/dev/null || true

                        if command -v startxfce4 >/dev/null 2>&1; then
                            exec startxfce4
                        elif command -v xfce4-session >/dev/null 2>&1; then
                            exec xfce4-session
                        else
                            xfsettingsd --daemon 2>/dev/null || true
                            xfwm4 --compositor=off &
                            xfce4-panel &
                            Thunar --daemon 2>/dev/null &
                            if command -v xfdesktop >/dev/null 2>&1; then
                                exec xfdesktop
                            else
                                exec xterm
                            fi
                        fi
                    """.trimIndent() + "\n"

                    try {
                        vncXstartup.parentFile?.mkdirs()
                        vncXstartup.writeText(standardXstartup)
                        vncXstartup.setExecutable(true, false)

                        val x11Session = File(targetRootfs, "etc/X11/Xtigervnc-session")
                        if (x11Session.parentFile?.exists() == true) {
                            x11Session.writeText(standardXstartup)
                            x11Session.setExecutable(true, false)
                        }

                        val rootVncDir = File(targetRootfs, "root/.vnc").apply { if (!exists()) mkdirs() }
                        val rootXstartup = File(rootVncDir, "xstartup")
                        rootXstartup.writeText(standardXstartup)
                        rootXstartup.setExecutable(true, false)

                        val skelVncDir = File(targetRootfs, "etc/skel/.vnc").apply { if (!exists()) mkdirs() }
                        val skelXstartup = File(skelVncDir, "xstartup")
                        skelXstartup.writeText(standardXstartup)
                        skelXstartup.setExecutable(true, false)

                        val homeDir = File(targetRootfs, "home")
                        if (homeDir.exists()) {
                            homeDir.listFiles()?.filter { it.isDirectory }?.forEach { userDir ->
                                val uVncDir = File(userDir, ".vnc").apply { if (!exists()) mkdirs() }
                                val uXstartup = File(uVncDir, "xstartup")
                                uXstartup.writeText(standardXstartup)
                                uXstartup.setExecutable(true, false)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }
}
