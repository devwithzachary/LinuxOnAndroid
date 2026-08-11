package com.devwithzachary.completelinuxinstaller.engine

import android.util.Log
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed class InstallStepState {
    data class Progress(val packageId: String, val logLine: String) : InstallStepState()
    data class Success(val packageId: String, val notes: String?) : InstallStepState()
    data class Error(val packageId: String, val errorMessage: String) : InstallStepState()
}

class SoftwareInstaller(private val pRootEngine: PRootEngine) {

    companion object {
        private const val TAG = "SoftwareInstaller"
    }

    fun installPackage(pkg: SoftwarePackage): Flow<InstallStepState> = flow {
        emit(InstallStepState.Progress(pkg.id, "Starting installation of ${pkg.name}..."))
        emit(InstallStepState.Progress(pkg.id, "Executing script: ${pkg.installCommand}"))

        try {
            val policyRcd = java.io.File(pRootEngine.rootfsDir, "usr/sbin/policy-rc.d")
            if (!policyRcd.exists()) {
                try {
                    policyRcd.parentFile?.mkdirs()
                    policyRcd.writeText("#!/bin/sh\nexit 101\n")
                    policyRcd.setExecutable(true, false)
                    policyRcd.setReadable(true, false)
                } catch (_: Exception) {}
            }

            val systemdStubs = listOf(
                "usr/bin/systemd-tmpfiles",
                "bin/systemd-tmpfiles",
                "usr/bin/systemd-sysusers",
                "bin/systemd-sysusers",
                "usr/bin/systemd-detect-virt",
                "bin/systemd-detect-virt"
            )
            for (stubPath in systemdStubs) {
                val stubFile = java.io.File(pRootEngine.rootfsDir, stubPath)
                try {
                    stubFile.parentFile?.mkdirs()
                    stubFile.writeText("#!/bin/sh\nexit 0\n")
                    stubFile.setExecutable(true, false)
                    stubFile.setReadable(true, false)
                } catch (_: Exception) {}
            }

            val dpkgPreconfigure = java.io.File(pRootEngine.rootfsDir, "usr/sbin/dpkg-preconfigure")
            if (!dpkgPreconfigure.exists()) {
                try {
                    dpkgPreconfigure.parentFile?.mkdirs()
                    dpkgPreconfigure.writeText("#!/bin/sh\nexit 0\n")
                    dpkgPreconfigure.setExecutable(true, false)
                    dpkgPreconfigure.setReadable(true, false)
                } catch (_: Exception) {}
            }

            val fixSudoScript = "chown 0:0 /usr/bin/sudo /etc/sudo.conf /etc/sudoers /etc/sudoers.d /etc/sudoers.d/* 2>/dev/null || true; " +
                    "chmod 4755 /usr/bin/sudo 2>/dev/null || true; " +
                    "chmod 644 /etc/sudo.conf 2>/dev/null || true; " +
                    "chmod 440 /etc/sudoers /etc/sudoers.d/* 2>/dev/null || true; " +
                    "chmod 755 /etc/sudoers.d 2>/dev/null || true"

            val sanitizedCommand = "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; " +
                    "rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /var/lib/dpkg/lock* /usr/bin/*.dpkg-new /usr/lib/*.dpkg-new 2>/dev/null; " +
                    "mkdir -p /etc/dpkg/dpkg.cfg.d && echo force-unsafe-io > /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-overwrite >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confold >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confdef >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid; " +
                    "mkdir -p /etc/apt/apt.conf.d && echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; " +
                    "chmod 666 /var/lib/dpkg/status* 2>/dev/null; " + pkg.installCommand + "; " + fixSudoScript

            val cmd = pRootEngine.buildPRootCommand(
                command = listOf("/bin/sh", "-c", sanitizedCommand)
            )

            val pb = ProcessBuilder(cmd)
            pb.directory(pRootEngine.rootfsDir)
            
            val env = pb.environment()
            env.putAll(pRootEngine.getEnvironmentVariables())
            env["PROOT_NO_SECCOMP"] = "1"
            env["PROOT_FORCE_SETID"] = "1"
            env["PROOT_LINK2SYMLINK"] = "1"
            env["TMPDIR"] = "/tmp"
            env["TMP"] = "/tmp"
            env["DEBIAN_FRONTEND"] = "noninteractive"
            env["DEBIAN_PRIORITY"] = "critical"
            env["UCF_FORCE_CONFFOLD"] = "1"
            env["NEEDRESTART_MODE"] = "a"
            env["PYTHONUNBUFFERED"] = "1"

            pb.redirectErrorStream(true)
            val process = pb.start()

            // Close stdin immediately so subprocesses don't wait for input on interactive prompts
            try {
                process.outputStream.close()
            } catch (_: Exception) {}

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                Log.d(TAG, "[Install ${pkg.id}] $currentLine")
                emit(InstallStepState.Progress(pkg.id, currentLine))
            }

            val exitCode = process.waitFor()
            reader.close()

            if (exitCode == 0) {
                emit(InstallStepState.Progress(pkg.id, "Installation completed successfully!"))
                emit(InstallStepState.Success(pkg.id, pkg.postInstallNotes))
            } else {
                emit(InstallStepState.Progress(pkg.id, "Process finished with exit code $exitCode"))
                emit(InstallStepState.Error(pkg.id, "Installation failed with exit code $exitCode. See terminal logs."))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed installing package ${pkg.id}", e)
            emit(InstallStepState.Error(pkg.id, e.localizedMessage ?: "Unknown installation error"))
        }
    }.flowOn(Dispatchers.IO)
}
