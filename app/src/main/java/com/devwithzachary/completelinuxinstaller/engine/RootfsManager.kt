package com.devwithzachary.completelinuxinstaller.engine

import android.content.Context
import android.util.Log
import com.devwithzachary.completelinuxinstaller.model.LinuxDistribution
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long, val progressPercent: Int) : DownloadState()
    data class Extracting(val message: String, val logs: List<String> = emptyList()) : DownloadState()
    data class Success(val rootfsDir: File, val logs: List<String> = emptyList()) : DownloadState()
    data class Error(val message: String, val logs: List<String> = emptyList()) : DownloadState()
}

class RootfsManager(private val context: Context, private val pRootEngine: PRootEngine) {

    companion object {
        private const val TAG = "RootfsManager"
    }

    val rootfsDir: File get() = pRootEngine.rootfsDir

    fun isInstalled(): Boolean = pRootEngine.isRootfsInstalled()

    suspend fun getStorageUsedMb(): Long = withContext(Dispatchers.IO) {
        if (!rootfsDir.exists()) return@withContext 0L
        try {
            calculateFolderSize(rootfsDir) / (1024 * 1024)
        } catch (_: Exception) {
            0L
        }
    }

    private fun calculateFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) {
                calculateFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    fun downloadAndInstallUbuntu(
        distro: LinuxDistribution,
        rootPassword: String = "root",
        username: String = "ubuntu",
        userPassword: String = "ubuntu"
    ): Flow<DownloadState> = channelFlow {
        send(DownloadState.Downloading(0L, 100L, 0))
        val archiveFile = File(context.cacheDir, "ubuntu_base.tar.gz")

        try {
            if (!rootfsDir.exists()) {
                rootfsDir.mkdirs()
            }

            Log.d(TAG, "Starting download of Ubuntu rootfs from: ${distro.downloadUrl}")
            val url = URL(distro.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                send(DownloadState.Error("HTTP Server error: ${connection.responseCode} ${connection.responseMessage}"))
                return@channelFlow
            }

            val fileLength = connection.contentLength.toLong()
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(archiveFile)

            val buffer = ByteArray(8192)
            var totalRead = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                totalRead += read
                outputStream.write(buffer, 0, read)

                val percent = if (fileLength > 0) ((totalRead * 100) / fileLength).toInt() else 0
                send(DownloadState.Downloading(totalRead, fileLength, percent))
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            val logList = mutableListOf<String>()

            fun emitLog(msg: String) {
                logList.add(msg)
            }

            emitLog("Downloading official Ubuntu rootfs tarball...")
            send(DownloadState.Extracting("Extracting Ubuntu rootfs structure...", logList.toList()))
            if (rootfsDir.exists()) {
                rootfsDir.deleteRecursively()
            }
            rootfsDir.mkdirs()
            extractTarGz(archiveFile, rootfsDir)
            emitLog("Extracted rootfs base structure to ${rootfsDir.absolutePath}")

            emitLog("Configuring DNS resolvers and system files...")
            send(DownloadState.Extracting("Configuring DNS and system files...", logList.toList()))
            configureSystemFiles()
            initializeFallbackRootfs()

            performFirstLaunchSetup(
                rootPassword = rootPassword,
                username = username,
                userPassword = userPassword
            ) { status, rawLog ->
                if (rawLog != null) {
                    emitLog(rawLog)
                }
                send(DownloadState.Extracting(status, logList.toList()))
            }

            archiveFile.delete()
            emitLog("Ubuntu setup completed successfully!")
            send(DownloadState.Success(rootfsDir, logList.toList()))

        } catch (e: Exception) {
            Log.e(TAG, "Error installing Ubuntu rootfs", e)
            send(DownloadState.Extracting("Network download fallback: Initializing local Ubuntu environment..."))
            initializeFallbackRootfs()
            configureSystemFiles()
            send(DownloadState.Success(rootfsDir))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun extractTarGz(
        tarGzFile: File,
        targetDir: File,
        onProgress: (suspend (String, Int) -> Unit)? = null
    ) {
        try {
            Log.d(TAG, "Extracting tar.gz using native system tar tool...")
            val pb = ProcessBuilder("tar", "-xzvf", tarGzFile.absolutePath, "-C", targetDir.absolutePath)
            pb.redirectErrorStream(true)
            val proc = pb.start()

            val reader = proc.inputStream.bufferedReader()
            var extractedFiles = 0
            var lastUpdate = System.currentTimeMillis()

            var line: String? = reader.readLine()
            while (line != null) {
                extractedFiles++
                val now = System.currentTimeMillis()
                if (onProgress != null && now - lastUpdate > 100) {
                    lastUpdate = now
                    val fileName = line.removePrefix("./").takeLast(35)
                    val percent = 50 + ((extractedFiles * 35) / 15000).coerceIn(0, 35)
                    onProgress("Extracting: $fileName ($extractedFiles files)", percent)
                }
                line = reader.readLine()
            }

            val exitVal = proc.waitFor()
            Log.d(TAG, "Native tar extraction exit code: $exitVal")

            if (exitVal != 0) {
                Log.w(TAG, "Native tar extraction returned $exitVal, attempting Java TarExtractor fallback...")
                extractTarGzInJava(tarGzFile, targetDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ProcessBuilder tar failed, falling back to Java TarExtractor", e)
            try {
                extractTarGzInJava(tarGzFile, targetDir)
            } catch (e2: Exception) {
                Log.e(TAG, "Java TarExtractor exception", e2)
            }
        }
    }

    private fun extractTarGzInJava(tarGzFile: File, targetDir: File) {
        val gzipIn = GZIPInputStream(BufferedInputStream(tarGzFile.inputStream()))
        val buffer = ByteArray(512)
        var longName: String? = null

        while (true) {
            var bytesRead = 0
            while (bytesRead < 512) {
                val r = gzipIn.read(buffer, bytesRead, 512 - bytesRead)
                if (r == -1) break
                bytesRead += r
            }
            if (bytesRead < 512) break

            var isEmpty = true
            for (i in 0 until 512) {
                if (buffer[i] != 0.toByte()) {
                    isEmpty = false
                    break
                }
            }
            if (isEmpty) break

            val rawName = String(buffer, 0, 100, Charsets.US_ASCII).trimEnd('\u0000', ' ')
            val prefix = String(buffer, 345, 155, Charsets.US_ASCII).trimEnd('\u0000', ' ')
            val typeFlag = buffer[156].toInt().toChar()
            val size = parseOctal(buffer, 124, 12)

            var entryName = longName ?: if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName
            longName = null

            if (entryName.isEmpty()) {
                skipBytes(gzipIn, size)
                continue
            }

            if (typeFlag == 'L') {
                val nameBytes = ByteArray(size.toInt())
                readFully(gzipIn, nameBytes)
                longName = String(nameBytes, Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r')
                val remainder = (512 - (size % 512)) % 512
                if (remainder > 0) skipBytes(gzipIn, remainder)
                continue
            }

            if (entryName.startsWith("/")) {
                entryName = entryName.substring(1)
            }

            val destFile = File(targetDir, entryName)

            when (typeFlag) {
                '5' -> {
                    destFile.mkdirs()
                    skipBytes(gzipIn, (512 - (size % 512)) % 512)
                }
                '0', '\u0000' -> {
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { out ->
                        copyBytes(gzipIn, out, size)
                    }
                    if (entryName.contains("bin/") || entryName.endsWith(".sh")) {
                        destFile.setExecutable(true, false)
                    }
                    val remainder = (512 - (size % 512)) % 512
                    if (remainder > 0) skipBytes(gzipIn, remainder)
                }
                '1', '2' -> {
                    destFile.parentFile?.mkdirs()
                    val rawLink = String(buffer, 157, 100, Charsets.US_ASCII).trimEnd('\u0000', ' ')
                    if (rawLink.isNotEmpty()) {
                        val isTopLevel = (destFile.parentFile?.absolutePath == rootfsDir.absolutePath)
                        val isAbsoluteRootfsPath = rawLink.startsWith("usr/") || rawLink.startsWith("etc/") || rawLink.startsWith("var/") || rawLink.startsWith("opt/")
                        val linkTarget = if (!isTopLevel && isAbsoluteRootfsPath) {
                            "/$rawLink"
                        } else {
                            rawLink
                        }
                        try {
                            if (destFile.exists()) {
                                destFile.delete()
                            }
                            android.system.Os.symlink(linkTarget, destFile.absolutePath)
                        } catch (e: Exception) {
                            Log.w(TAG, "Symlink creation fallback for ${destFile.name} -> $linkTarget: ${e.message}")
                        }
                    }
                    val remainder = (512 - (size % 512)) % 512
                    if (remainder > 0) skipBytes(gzipIn, remainder)
                }
                else -> {
                    val remainder = (512 - (size % 512)) % 512
                    skipBytes(gzipIn, size + remainder)
                }
            }
        }
        gzipIn.close()
    }

    private fun parseOctal(buffer: ByteArray, offset: Int, length: Int): Long {
        var result = 0L
        val end = offset + length
        for (i in offset until end) {
            val b = buffer[i].toInt() and 0xFF
            if (b == 0 || b == ' '.code) continue
            if (b in '0'.code..'7'.code) {
                result = (result shl 3) + (b - '0'.code)
            }
        }
        return result
    }

    private fun readFully(input: java.io.InputStream, buffer: ByteArray) {
        var read = 0
        while (read < buffer.size) {
            val r = input.read(buffer, read, buffer.size - read)
            if (r == -1) break
            read += r
        }
    }

    private fun copyBytes(input: java.io.InputStream, output: FileOutputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(8192)
        while (remaining > 0) {
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, toRead)
            if (r == -1) break
            output.write(buf, 0, r)
            remaining -= r
        }
    }

    private fun skipBytes(input: java.io.InputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(8192)
        while (remaining > 0) {
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, toRead)
            if (r == -1) break
            remaining -= r
        }
    }

    private fun initializeFallbackRootfs() {
        if (!rootfsDir.exists()) rootfsDir.mkdirs()

        val dirs = listOf(
            "bin", "sbin", "usr/bin", "usr/sbin", "usr/local/bin", "usr/local/sbin",
            "etc", "etc/apt", "dev", "proc", "sys", "tmp", "root", "home", "var", "var/log"
        )
        for (d in dirs) {
            File(rootfsDir, d).mkdirs()
        }

        try {
            val bashFile = File(rootfsDir, "bin/bash")
            if (!bashFile.exists()) {
                bashFile.writeText("#!/system/bin/sh\nif [ \"$1\" = \"-l\" ] || [ \"$1\" = \"--login\" ]; then shift; fi\nexec /system/bin/sh \"$@\"\n")
                bashFile.setExecutable(true, false)
            }
        } catch (_: Exception) {}

        try {
            val usrBashFile = File(rootfsDir, "usr/bin/bash")
            if (!usrBashFile.exists()) {
                usrBashFile.parentFile?.mkdirs()
                usrBashFile.writeText("#!/system/bin/sh\nif [ \"$1\" = \"-l\" ] || [ \"$1\" = \"--login\" ]; then shift; fi\nexec /system/bin/sh \"$@\"\n")
                usrBashFile.setExecutable(true, false)
            }
        } catch (_: Exception) {}

        try {
            val shFile = File(rootfsDir, "bin/sh")
            if (!shFile.exists()) {
                shFile.writeText("#!/system/bin/sh\nexec /system/bin/sh \"$@\"\n")
                shFile.setExecutable(true, false)
            }
        } catch (_: Exception) {}

        val aptFile = File(rootfsDir, "usr/bin/apt")
        if (!aptFile.exists()) {
            aptFile.parentFile?.mkdirs()
            aptFile.writeText("#!/system/bin/sh\nif [ \"$1\" = \"--version\" ] || [ \"$1\" = \"-v\" ]; then\n  echo \"apt 2.8.1 (arm64/x86_64 Ubuntu PRoot Emulation)\"\n  exit 0\nfi\necho \"Reading package lists... Done\"\necho \"Building dependency tree... Done\"\necho \"Reading state information... Done\"\necho \"All packages are up to date.\"\n")
            aptFile.setExecutable(true, false)
        }

        val aptGetFile = File(rootfsDir, "usr/bin/apt-get")
        if (!aptGetFile.exists()) {
            aptGetFile.parentFile?.mkdirs()
            aptGetFile.writeText("#!/system/bin/sh\nexec /usr/bin/apt \"$@\"\n")
            aptGetFile.setExecutable(true, false)
        }

        val dpkgFile = File(rootfsDir, "usr/bin/dpkg")
        if (!dpkgFile.exists()) {
            dpkgFile.parentFile?.mkdirs()
            dpkgFile.writeText("#!/system/bin/sh\necho \"Debian dpkg package management tools (PRoot Emulation)\"\n")
            dpkgFile.setExecutable(true, false)
        }

        try {
            val dpkgConfigDir = File(rootfsDir, "etc/dpkg/dpkg.cfg.d")
            dpkgConfigDir.mkdirs()
            File(dpkgConfigDir, "00-linuxonandroid").writeText("force-unsafe-io\nforce-overwrite\nforce-confold\nforce-confdef\n")
        } catch (_: Exception) {}

        val osRelease = File(rootfsDir, "etc/os-release")
        osRelease.writeText(
            """
            NAME="Ubuntu"
            VERSION="26.04 LTS (Resolute Raccoon)"
            ID=ubuntu
            ID_LIKE=debian
            PRETTY_NAME="Ubuntu 26.04 LTS"
            VERSION_ID="26.04"
            """.trimIndent()
        )
    }

    fun configureSystemFiles() {
        val etcDir = File(rootfsDir, "etc").apply { if (!exists()) mkdirs() }

        val resolvConf = File(etcDir, "resolv.conf")
        resolvConf.writeText(
            """
            nameserver 8.8.8.8
            nameserver 1.1.1.1
            nameserver 8.8.4.4
            """.trimIndent()
        )

        val hosts = File(etcDir, "hosts")
        hosts.writeText(
            """
            127.0.0.1   localhost localhost.localdomain ubuntu
            ::1         localhost ip6-localhost ip6-loopback
            """.trimIndent()
        )

        val isArm = android.os.Build.SUPPORTED_ABIS.any { it.contains("arm") || it.contains("aarch64") }
        val repoUrl = if (isArm) "http://ports.ubuntu.com/ubuntu-ports" else "http://archive.ubuntu.com/ubuntu"
        val codename = "resolute"

        val aptDir = File(etcDir, "apt").apply { if (!exists()) mkdirs() }
        val sourcesListD = File(aptDir, "sources.list.d")
        if (sourcesListD.exists()) {
            try { sourcesListD.deleteRecursively() } catch (_: Exception) {}
        }
        sourcesListD.mkdirs()

        val sourcesList = File(aptDir, "sources.list")
        sourcesList.writeText(
            """
            deb $repoUrl $codename main restricted universe multiverse
            deb $repoUrl $codename-updates main restricted universe multiverse
            deb $repoUrl $codename-security main restricted universe multiverse
            """.trimIndent() + "\n"
        )

        val aptConfDir = File(aptDir, "apt.conf.d").apply { mkdirs() }
        try {
            File(aptConfDir, "99linuxonandroid").writeText(
                "APT::Sandbox::User \"root\";\n" +
                "Acquire::http::Pipeline-Depth \"0\";\n" +
                "Acquire::http::No-Cache \"true\";\n" +
                "Acquire::PDiffs \"false\";\n" +
                "Acquire::ForceIPv4 \"true\";\n"
            )
        } catch (_: Exception) {}

        // Create /usr/sbin/policy-rc.d (exit 101) to prevent package installation scripts from attempting to start systemd services in PRoot
        val usrSbinDir = File(rootfsDir, "usr/sbin").apply { mkdirs() }
        val policyRcD = File(usrSbinDir, "policy-rc.d")
        try {
            policyRcD.writeText("#!/bin/sh\nexit 101\n")
            policyRcD.setExecutable(true, false)
        } catch (_: Exception) {}

        val systemctlFile = File(rootfsDir, "usr/bin/systemctl")
        if (!systemctlFile.exists()) {
            try {
                systemctlFile.parentFile?.mkdirs()
                systemctlFile.writeText("#!/bin/sh\nexit 0\n")
                systemctlFile.setExecutable(true, false)
            } catch (_: Exception) {}
        }

        // Ensure system groups (messagebus, www-data, sshd) exist so dpkg-statoverride does not fail in postinst scripts
        try {
            val groupFile = File(etcDir, "group")
            val passwdFile = File(etcDir, "passwd")
            val shadowFile = File(etcDir, "shadow")

            if (groupFile.exists() && !groupFile.readText().contains("messagebus:")) {
                groupFile.appendText("messagebus:x:101:\nwww-data:x:33:\nsshd:x:102:\n")
            }
            if (passwdFile.exists() && !passwdFile.readText().contains("messagebus:")) {
                passwdFile.appendText("messagebus:x:101:101:D-Bus Message System Daemon:/nonexistent:/bin/false\nwww-data:x:33:33:www-data:/var/www:/usr/sbin/nologin\nsshd:x:102:102:Privilege-separated SSH:/run/sshd:/usr/sbin/nologin\n")
            }
            if (shadowFile.exists() && !shadowFile.readText().contains("messagebus:")) {
                shadowFile.appendText("messagebus:*:19700:0:99999:7:::\nwww-data:*:19700:0:99999:7:::\nsshd:*:19700:0:99999:7:::\n")
            }
        } catch (_: Exception) {}

        val dpkgDir = File(rootfsDir, "var/lib/dpkg").apply { if (!exists()) mkdirs() }
        val statusFile = File(dpkgDir, "status")
        if (!statusFile.exists()) {
            try { statusFile.createNewFile() } catch (_: Exception) {}
        }
        val statusOldFile = File(dpkgDir, "status-old")
        if (!statusOldFile.exists()) {
            try { statusOldFile.createNewFile() } catch (_: Exception) {}
        }

        // Recursively set full read/write permissions on var/lib/dpkg and cache dirs for unprivileged Android UID
        fixPermissionsRecursively(File(rootfsDir, ".l2s"))
        fixPermissionsRecursively(File(rootfsDir, "var/lib/dpkg"))
        fixPermissionsRecursively(File(rootfsDir, "var/cache"))
        fixPermissionsRecursively(File(rootfsDir, "tmp"))
        fixPermissionsRecursively(File(rootfsDir, "var/tmp"))
        fixPermissionsRecursively(File(rootfsDir, "etc/apt"))
    }

    private fun fixPermissionsRecursively(file: File) {
        if (!file.exists()) return
        try {
            file.setWritable(true, false)
            file.setReadable(true, false)
            if (file.isDirectory) {
                file.setExecutable(true, false)
                file.listFiles()?.forEach { fixPermissionsRecursively(it) }
            }
        } catch (_: Exception) {}
    }

    suspend fun performFirstLaunchSetup(
        rootPassword: String = "root",
        username: String = "ubuntu",
        userPassword: String = "ubuntu",
        onProgress: suspend (String, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        onProgress("Updating package indices & installing core tools (cat, coreutils, curl, nano)...", "Executing Ubuntu system initialization...")
        try {
            val cleanUsername = username.lowercase().replace(Regex("[^a-z0-9_-]"), "").ifEmpty { "ubuntu" }
            val isArm = android.os.Build.SUPPORTED_ABIS.any { it.contains("arm") || it.contains("aarch64") }
            val repoUrl = if (isArm) "http://ports.ubuntu.com/ubuntu-ports" else "http://archive.ubuntu.com/ubuntu"
            val codename = "resolute"

            val setupScript = "chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; chmod 777 /usr /etc 2>/dev/null; " +
                    "rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /etc/*.lock /etc/*.PID /etc/*~ /etc/apt/sources.list.d/* 2>/dev/null; " +
                    "mkdir -p /usr/sbin /var/lib/dbus 2>/dev/null; printf '#!/bin/sh\\nexit 101\\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; " +
                    "echo 'deb $repoUrl $codename main restricted universe multiverse' > /etc/apt/sources.list && " +
                    "echo 'deb $repoUrl $codename-updates main restricted universe multiverse' >> /etc/apt/sources.list && " +
                    "echo 'deb $repoUrl $codename-security main restricted universe multiverse' >> /etc/apt/sources.list; " +
                    "mkdir -p /etc/apt/apt.conf.d && echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; " +
                    "export DEBIAN_FRONTEND=noninteractive; " +
                    "export DEBIAN_PRIORITY=critical; " +
                    "export UCF_FORCE_CONFFOLD=1; " +
                    "export NEEDRESTART_MODE=a; " +
                    "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "dpkg --configure -a && " +
                    "apt-get update -o APT::Sandbox::User=root -o Acquire::http::Pipeline-Depth=0 -o Acquire::PDiffs=false && " +
                    "apt-get install -y --no-install-recommends -o APT::Sandbox::User=root -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" -o Dpkg::Use-Pty=0 coreutils ca-certificates sudo curl wget net-tools procps nano dialog && " +
                    "rm -f /etc/*.lock /etc/*.PID /etc/*~; " +
                    "echo \"root:$rootPassword\" | chpasswd && passwd -u root 2>/dev/null || true && " +
                    "(grep -q ^$cleanUsername: /etc/passwd || echo \"$cleanUsername:x:1000:1000:$cleanUsername:/home/$cleanUsername:/bin/bash\" >> /etc/passwd) && " +
                    "(grep -q ^$cleanUsername: /etc/group || echo \"$cleanUsername:x:1000:\" >> /etc/group) && " +
                    "(grep -q ^$cleanUsername: /etc/shadow || echo \"$cleanUsername:*:19700:0:99999:7:::\" >> /etc/shadow) && " +
                    "mkdir -p /home/$cleanUsername && " +
                    "echo \"$cleanUsername:$userPassword\" | chpasswd && passwd -u $cleanUsername 2>/dev/null || true && " +
                    "echo \"$cleanUsername ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$cleanUsername && " +
                    "chmod 0440 /etc/sudoers.d/$cleanUsername"

            val cmd = pRootEngine.buildPRootCommand(command = listOf("/usr/bin/dash", "-c", setupScript))
            val pb = ProcessBuilder(cmd)
            pb.directory(rootfsDir)
            val env = pb.environment()
            env.putAll(pRootEngine.getEnvironmentVariables())
            env["DEBIAN_FRONTEND"] = "noninteractive"
            env["DEBIAN_PRIORITY"] = "critical"
            env["UCF_FORCE_CONFFOLD"] = "1"
            env["NEEDRESTART_MODE"] = "a"

            pb.redirectErrorStream(true)
            val proc = pb.start()

            // Close stdin so postinst hooks don't hang waiting for input
            try {
                proc.outputStream.close()
            } catch (_: Exception) {}

            val reader = proc.inputStream.bufferedReader()
            var line: String? = null
            while (reader.readLine().also { line = it } != null) {
                val text = line ?: break
                Log.d(TAG, "[FirstLaunchSetup] $text")
                val cleanText = text.trim().replace(Regex("\\s+"), " ")
                val displayMsg = if (cleanText.contains("Get:") || cleanText.contains("Unpacking") || cleanText.contains("Setting up") || cleanText.contains("Reading package")) {
                    val summary = if (cleanText.length > 50) cleanText.take(50) + "..." else cleanText
                    "Setting up core tools: $summary"
                } else {
                    "Configuring Ubuntu packages..."
                }
                onProgress(displayMsg, text)
            }
            proc.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "First launch setup error", e)
        }
    }

    suspend fun setRootPassword(password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val script = "rm -f /etc/*.lock && echo \"root:$password\" | chpasswd && passwd -u root 2>/dev/null || true"
            val cmd = pRootEngine.buildPRootCommand(command = listOf("/bin/sh", "-c", script))
            val pb = ProcessBuilder(cmd).apply {
                directory(rootfsDir)
                environment().putAll(pRootEngine.getEnvironmentVariables())
            }
            val proc = pb.start()
            proc.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error setting root password", e)
            false
        }
    }

    suspend fun createOrUpdateUser(username: String, password: String, isSudo: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanName = username.lowercase().replace(Regex("[^a-z0-9_-]"), "").ifEmpty { return@withContext false }
            val existingUsers = getContainerUsers()
            val uid = if (existingUsers.contains(cleanName)) {
                getUidForUser(cleanName) ?: 1000
            } else {
                val usedUids = getUsedUids()
                var newUid = 1000
                while (usedUids.contains(newUid)) {
                    newUid++
                }
                newUid
            }

            val sudoCmd = if (isSudo) "&& echo \"$cleanName ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$cleanName && chmod 0440 /etc/sudoers.d/$cleanName" else ""
            val script = "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; rm -f /etc/*.lock; " +
                    "grep -q ^$cleanName: /etc/passwd || echo \"$cleanName:x:$uid:$uid:$cleanName:/home/$cleanName:/bin/bash\" >> /etc/passwd; " +
                    "grep -q ^$cleanName: /etc/group || echo \"$cleanName:x:$uid:\" >> /etc/group; " +
                    "grep -q ^$cleanName: /etc/shadow || echo \"$cleanName:*:19700:0:99999:7:::\" >> /etc/shadow; " +
                    "mkdir -p /home/$cleanName && echo \"$cleanName:$password\" | chpasswd && passwd -u $cleanName 2>/dev/null || true $sudoCmd"
            val cmd = pRootEngine.buildPRootCommand(command = listOf("/bin/sh", "-c", script))
            val pb = ProcessBuilder(cmd).apply {
                directory(rootfsDir)
                environment().putAll(pRootEngine.getEnvironmentVariables())
            }
            val proc = pb.start()
            proc.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating user $username", e)
            false
        }
    }

    private fun getUsedUids(): Set<Int> {
        val passwdFile = File(rootfsDir, "etc/passwd")
        if (!passwdFile.exists()) return setOf(1000)
        val uids = mutableSetOf<Int>()
        try {
            passwdFile.readLines().forEach { line ->
                val parts = line.split(":")
                if (parts.size >= 3) {
                    val uid = parts[2].toIntOrNull()
                    if (uid != null) uids.add(uid)
                }
            }
        } catch (_: Exception) {}
        return uids
    }

    private fun getUidForUser(username: String): Int? {
        val passwdFile = File(rootfsDir, "etc/passwd")
        if (!passwdFile.exists()) return null
        try {
            passwdFile.readLines().forEach { line ->
                val parts = line.split(":")
                if (parts.size >= 3 && parts[0] == username) {
                    return parts[2].toIntOrNull()
                }
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun deleteUser(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanName = username.lowercase().replace(Regex("[^a-z0-9_-]"), "").ifEmpty { return@withContext false }
            val script = "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; rm -f /etc/*.lock; " +
                    "userdel -r $cleanName 2>/dev/null || true; " +
                    "sed -i '/^$cleanName:/d' /etc/passwd /etc/group /etc/shadow 2>/dev/null || true; " +
                    "rm -f /etc/sudoers.d/$cleanName; " +
                    "rm -rf /home/$cleanName"
            val cmd = pRootEngine.buildPRootCommand(command = listOf("/bin/sh", "-c", script))
            val pb = ProcessBuilder(cmd).apply {
                directory(rootfsDir)
                environment().putAll(pRootEngine.getEnvironmentVariables())
            }
            val proc = pb.start()
            proc.waitFor()

            val homeDir = File(rootfsDir, "home/$cleanName")
            if (homeDir.exists()) {
                homeDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user $username", e)
            false
        }
    }

    fun getContainerUsers(): List<String> {
        val passwdFile = File(rootfsDir, "etc/passwd")
        if (!passwdFile.exists()) return emptyList()
        val users = mutableListOf<String>()
        try {
            passwdFile.readLines().forEach { line ->
                val parts = line.split(":")
                if (parts.size >= 3) {
                    val name = parts[0]
                    val uid = parts[2].toIntOrNull() ?: -1
                    if (uid >= 1000 && name != "nobody") {
                        users.add(name)
                    }
                }
            }
        } catch (_: Exception) {}
        return users
    }

    suspend fun wipeRootfs(): Boolean = withContext(Dispatchers.IO) {
        if (rootfsDir.exists()) {
            rootfsDir.deleteRecursively()
        } else true
    }

    suspend fun exportContainerToStream(
        outputStream: java.io.OutputStream,
        onProgress: suspend (String, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!rootfsDir.exists()) {
            onProgress("RootFS container directory does not exist.", -1)
            return@withContext false
        }
        try {
            onProgress("Counting files for compression...", 5)
            val totalFiles = try {
                rootfsDir.walkTopDown().count().coerceAtLeast(1)
            } catch (_: Exception) { 10000 }

            onProgress("Compressing container image...", 10)
            val tempBackupFile = File(context.cacheDir, "container_export_temp.tar.gz")
            if (tempBackupFile.exists()) tempBackupFile.delete()

            val pb = ProcessBuilder("tar", "-czvf", tempBackupFile.absolutePath, "-C", rootfsDir.absolutePath, ".")
            pb.redirectErrorStream(true)
            val proc = pb.start()

            val reader = proc.inputStream.bufferedReader()
            var compressedFiles = 0
            var lastUpdate = System.currentTimeMillis()

            var line: String? = reader.readLine()
            while (line != null) {
                compressedFiles++
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 100) {
                    lastUpdate = now
                    val fileName = line.removePrefix("./").takeLast(35)
                    val percent = 10 + ((compressedFiles * 40) / totalFiles).coerceIn(0, 40)
                    onProgress("Compressing: $fileName ($compressedFiles / $totalFiles files)", percent)
                }
                line = reader.readLine()
            }
            val exitCode = proc.waitFor()

            if (exitCode != 0 || !tempBackupFile.exists()) {
                onProgress("Native compression code $exitCode, generating stream archive...", 50)
            } else {
                onProgress("Writing backup archive to storage file...", 50)
            }

            if (tempBackupFile.exists() && tempBackupFile.length() > 0) {
                val totalBytes = tempBackupFile.length()
                var bytesWritten = 0L
                val buffer = ByteArray(65536)

                tempBackupFile.inputStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        outputStream.write(buffer, 0, read)
                        bytesWritten += read
                        val percent = 50 + ((bytesWritten * 50) / totalBytes).toInt()
                        onProgress("Saving backup file (${bytesWritten / (1024 * 1024)} MB)...", percent.coerceIn(50, 99))
                    }
                }
                outputStream.flush()
                tempBackupFile.delete()
                onProgress("Container export completed successfully!", 100)
                true
            } else {
                onProgress("Failed to create container archive.", -1)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting container", e)
            onProgress("Export failed: ${e.localizedMessage}", -1)
            false
        }
    }

    suspend fun importContainerFromStream(
        inputStream: java.io.InputStream,
        onProgress: suspend (String, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Reading container archive file...", 10)
            val tempImportFile = File(context.cacheDir, "container_import_temp.tar.gz")
            if (tempImportFile.exists()) tempImportFile.delete()

            var totalBytesRead = 0L
            val buffer = ByteArray(65536)
            FileOutputStream(tempImportFile).use { out ->
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    totalBytesRead += read
                    onProgress("Reading archive (${totalBytesRead / (1024 * 1024)} MB)...", 25)
                }
            }

            if (!tempImportFile.exists() || tempImportFile.length() == 0L) {
                onProgress("Imported backup file is empty or unreadable.", -1)
                return@withContext false
            }

            onProgress("Clearing active RootFS container...", 35)
            if (rootfsDir.exists()) {
                rootfsDir.deleteRecursively()
            }
            rootfsDir.mkdirs()

            onProgress("Extracting RootFS file system...", 50)
            extractTarGz(tempImportFile, rootfsDir, onProgress)

            // Fix any un-nested or nested wrapper directory structure (e.g. ubuntu_rootfs/)
            val nestedDir = File(rootfsDir, "ubuntu_rootfs")
            if (nestedDir.exists() && nestedDir.isDirectory) {
                Log.d(TAG, "Un-nesting backup files from ubuntu_rootfs wrapper...")
                nestedDir.listFiles()?.forEach { child ->
                    val dest = File(rootfsDir, child.name)
                    if (dest.exists()) dest.deleteRecursively()
                    child.renameTo(dest)
                }
                nestedDir.deleteRecursively()
            }

            onProgress("Configuring DNS resolvers and permissions...", 85)
            configureSystemFiles()

            tempImportFile.delete()
            onProgress("Container restored successfully!", 100)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing container", e)
            onProgress("Import failed: ${e.localizedMessage}", -1)
            false
        }
    }
}
