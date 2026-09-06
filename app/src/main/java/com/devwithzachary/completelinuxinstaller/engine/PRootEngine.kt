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
    val customMounts: List<String> = emptyList(),
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
                    java.nio.file.Files.createSymbolicLink(usrBinBash.toPath(), java.nio.file.Paths.get("/bin/bash"))
                }
            } catch (_: Exception) {}
        }

        fun isValidShell(relPath: String): Boolean {
            val file = File(targetRootfs, relPath.removePrefix("/"))
            if (!file.exists()) return false
            if (relPath.contains("bash")) {
                try {
                    val canonical = file.canonicalPath
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
                java.nio.file.Files.createSymbolicLink(
                    File(targetRootfs, "usr/bin/service").toPath(),
                    java.nio.file.Paths.get("/usr/sbin/service")
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
            mounts.addAll(config.customMounts)

            for (m in mounts) {
                if (m.contains(":")) {
                    cmdList.add("-b")
                    cmdList.add(m)
                } else {
                    addBindMount(cmdList, m)
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
            "VERSION_CODENAME" to codename
        )

        if (loaderFile.exists() && loaderFile.length() > 0L) {
            env["PROOT_LOADER"] = loaderFile.absolutePath
        }
        if (loader32File.exists() && loader32File.length() > 0L) {
            env["PROOT_LOADER32"] = loader32File.absolutePath
        }

        return env
    }
}
