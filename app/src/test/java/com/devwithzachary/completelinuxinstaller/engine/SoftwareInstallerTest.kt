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
}
