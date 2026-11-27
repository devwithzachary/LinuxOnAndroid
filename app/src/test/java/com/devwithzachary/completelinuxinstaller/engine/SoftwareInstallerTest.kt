package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftwareInstallerTest {

    @Test
    fun testInstallStepState_logDeduplicationLogic() {
        var installLogs = "Starting installation of OpenSSH Server into container...\nExecuting script: apt-get install\n"

        val steps = listOf(
            InstallStepState.Progress("openssh_server", "Unpacking openssh-server..."),
            InstallStepState.Progress("openssh_server", "Setting up openssh-server..."),
            InstallStepState.Progress("openssh_server", "Installation completed successfully!"),
            InstallStepState.Success("openssh_server", "Notes: SSH port 2222")
        )

        for (step in steps) {
            when (step) {
                is InstallStepState.Progress -> {
                    installLogs += step.logLine + "\n"
                }
                is InstallStepState.Success -> {
                    val currentLogs = installLogs
                    val newLogs = if (!currentLogs.trimEnd().endsWith("Installation completed successfully!")) {
                        currentLogs + "Installation completed successfully!\n"
                    } else {
                        currentLogs
                    }
                    installLogs = newLogs
                }
                is InstallStepState.Error -> {
                    installLogs += "ERROR: " + step.errorMessage + "\n"
                }
            }
        }

        val occurrences = installLogs.lines().count { it == "Installation completed successfully!" }
        assertEquals("Log must contain 'Installation completed successfully!' exactly once", 1, occurrences)
    }

    @Test
    fun testInstallStepState_successWithoutProgress_appendsCompletionMessageOnce() {
        var installLogs = "Starting installation of Custom Package...\n"

        val steps = listOf(
            InstallStepState.Progress("custom_pkg", "Building from source..."),
            InstallStepState.Success("custom_pkg", null)
        )

        for (step in steps) {
            when (step) {
                is InstallStepState.Progress -> {
                    installLogs += step.logLine + "\n"
                }
                is InstallStepState.Success -> {
                    val currentLogs = installLogs
                    val newLogs = if (!currentLogs.trimEnd().endsWith("Installation completed successfully!")) {
                        currentLogs + "Installation completed successfully!\n"
                    } else {
                        currentLogs
                    }
                    installLogs = newLogs
                }
                is InstallStepState.Error -> {
                    installLogs += "ERROR: " + step.errorMessage + "\n"
                }
            }
        }

        val occurrences = installLogs.lines().count { it == "Installation completed successfully!" }
        assertEquals("Log must append 'Installation completed successfully!' when missing", 1, occurrences)
        assertTrue("Log ends with completion message", installLogs.trimEnd().endsWith("Installation completed successfully!"))
    }

    @Test
    fun testHealUbuntuRustCoreutilsSymlinks_repointsOnlyRustAndL2sCoreutils() {
        val tempRoot = java.nio.file.Files.createTempDirectory("loa_coreutils_test").toFile()
        try {
            val usrBin = java.io.File(tempRoot, "usr/bin").apply { mkdirs() }
            java.io.File(usrBin, "gnumkdir").writeText("gnu-mkdir")
            java.io.File(usrBin, "gnubasename").writeText("gnu-basename")
            java.io.File(usrBin, "gnuplot").writeText("gnuplot-bin")

            val mkdirPath = java.io.File(usrBin, "mkdir").toPath()
            val basenamePath = java.io.File(usrBin, "basename").toPath()
            val plotPath = java.io.File(usrBin, "plot").toPath()

            java.nio.file.Files.createSymbolicLink(mkdirPath, java.nio.file.Paths.get("../lib/cargo/bin/coreutils/mkdir"))
            java.nio.file.Files.createSymbolicLink(basenamePath, java.nio.file.Paths.get(".l2s.coreutils.dpkg-new0001.0116"))
            java.nio.file.Files.createSymbolicLink(plotPath, java.nio.file.Paths.get("other-plot"))

            val healed = PRootEngine.healUbuntuRustCoreutilsSymlinks(tempRoot)
            assertEquals(2, healed)
            assertEquals("gnumkdir", java.nio.file.Files.readSymbolicLink(mkdirPath).toString())
            assertEquals("gnubasename", java.nio.file.Files.readSymbolicLink(basenamePath).toString())
            assertEquals("other-plot", java.nio.file.Files.readSymbolicLink(plotPath).toString())
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
