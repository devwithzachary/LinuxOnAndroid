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
    val rootfsDir: File get() = File(filesDir, "ubuntu_rootfs")
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

    fun isRootfsInstalled(): Boolean {
        if (!rootfsDir.exists()) return false
        val osRelease = File(rootfsDir, "etc/os-release")
        val dpkg = File(rootfsDir, "usr/bin/dpkg")
        val apt = File(rootfsDir, "usr/bin/apt-get")
        val libGlibc = File(rootfsDir, "usr/lib")
        return osRelease.exists() && (dpkg.exists() || apt.exists() || libGlibc.exists())
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
        command: List<String> = listOf("/bin/bash", "-l")
    ): List<String> {
        val cmdList = mutableListOf<String>()
        val prootExec = prootBinary
        val hostFallbackPath = "${config.rootfsDir.absolutePath}/usr/local/sbin:${config.rootfsDir.absolutePath}/usr/local/bin:${config.rootfsDir.absolutePath}/usr/sbin:${config.rootfsDir.absolutePath}/usr/bin:${config.rootfsDir.absolutePath}/sbin:${config.rootfsDir.absolutePath}/bin:/system/bin"

        val usePRoot = prootExec.exists() && prootExec.length() > 0L

        val effectiveCommand = if (command == listOf("/bin/bash", "-l") && !isRootfsInstalled()) {
            listOf("/system/bin/sh")
        } else {
            command
        }

        if (usePRoot) {
            val workDirFile = File(config.rootfsDir, config.workingDir.removePrefix("/"))
            if (!workDirFile.exists()) {
                workDirFile.mkdirs()
            }

            cmdList.add(prootExec.absolutePath)
            cmdList.add("-0")
            cmdList.add("-l")
            cmdList.add("-r")
            cmdList.add(config.rootfsDir.absolutePath)

            val mounts = mutableListOf(
                "/proc", "/sys", "/dev", "/dev/pts", "/system",
                "/proc/self/fd:/dev/fd",
                "/proc/self/fd/0:/dev/stdin",
                "/proc/self/fd/1:/dev/stdout",
                "/proc/self/fd/2:/dev/stderr",
                "/proc/self:/proc/1"
            )
            if (config.bindSdCard) {
                val hostSdcard = Environment.getExternalStorageDirectory()
                if (hostSdcard.exists()) {
                    val hostPath = hostSdcard.absolutePath
                    try {
                        File(config.rootfsDir, "sdcard").mkdirs()
                        File(config.rootfsDir, "mnt/sdcard").mkdirs()
                        File(config.rootfsDir, "storage/emulated/0").mkdirs()
                    } catch (_: Exception) {}

                    mounts.add("$hostPath:/sdcard")
                    mounts.add("$hostPath:/storage/emulated/0")
                    mounts.add("$hostPath:/mnt/sdcard")

                    val downloadsHost = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsHost.exists()) {
                        try { File(config.rootfsDir, "root/Downloads").mkdirs() } catch (_: Exception) {}
                        mounts.add("${downloadsHost.absolutePath}:/root/Downloads")
                    }
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
            }

            cmdList.add("-w")
            cmdList.add(config.workingDir)

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
            val chrootCmd = buildString {
                append("export PROOT_TMP_DIR='${config.tmpDir.absolutePath}'; ")
                append("export PROOT_NO_SECCOMP='1'; ")
                append("export PROOT_FORCE_SETID='1'; ")
                append("export HOME='${config.workingDir}'; ")
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

    fun getEnvironmentVariables(): Map<String, String> {
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

        val env = mutableMapOf(
            "LD_LIBRARY_PATH" to nativeLibDir,
            "PROOT_TMP_DIR" to tmpDir.absolutePath,
            "PROOT_L2S_DIR" to l2sDir.absolutePath,
            "PROOT_UNSETENV" to "LD_LIBRARY_PATH:TMPDIR:TMP",
            "PROOT_NO_SECCOMP" to "1",
            "PROOT_FORCE_SETID" to "1",
            "PROOT_LINK2SYMLINK" to "1",
            "USER" to "root",
            "LOGNAME" to "root",
            "HOME" to "/root",
            "PYTHONPATH" to "/usr/lib/python3/dist-packages",
            "PATH" to guestPath,
            "TERM" to "xterm-256color",
            "LANG" to "C.UTF-8",
            "TMPDIR" to "/tmp",
            "TMP" to "/tmp"
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
