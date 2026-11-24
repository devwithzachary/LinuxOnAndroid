package com.devwithzachary.completelinuxinstaller.e2e

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.devwithzachary.completelinuxinstaller.MainActivity
import com.devwithzachary.completelinuxinstaller.engine.ContainerManager
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import com.devwithzachary.completelinuxinstaller.engine.InstallStepState
import com.devwithzachary.completelinuxinstaller.engine.PRootConfig
import com.devwithzachary.completelinuxinstaller.engine.PRootEngine
import com.devwithzachary.completelinuxinstaller.engine.RootfsManager
import com.devwithzachary.completelinuxinstaller.engine.SoftwareInstaller
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end Android instrumentation suite running inside the real APK + PRoot environment.
 * Supports both x86_64 KVM emulator and physical ARM64 devices via adb instrumentation args:
 *   -e distro <id|all>   (default: ubuntu_26_04)
 *   -e pkg <id|all>      (default: all)
 *   -e gui <true|false>  (default: true)
 */
@RunWith(AndroidJUnit4::class)
class DistroAndPackageE2ETest {

    companion object {
        private const val TAG = "DistroAndPackageE2E"

        private val GUI_PACKAGES = mapOf(
            "firefox_browser" to GuiTestSpec("firefox", listOf("firefox.desktop", "firefox-esr.desktop")),
            "vscode_desktop" to GuiTestSpec("code --verbose --no-sandbox --user-data-dir=/root/.vscode", listOf("code.desktop")),
            "blender_3d" to GuiTestSpec("LIBGL_ALWAYS_SOFTWARE=1 blender", listOf("blender.desktop")),
            "kdenlive_video" to GuiTestSpec("kdenlive", listOf("org.kde.kdenlive.desktop", "kdenlive.desktop")),
            "gimp_editor" to GuiTestSpec("gimp", listOf("gimp.desktop", "org.gimp.GIMP.desktop")),
            "libreoffice_suite" to GuiTestSpec("libreoffice", listOf("libreoffice-startcenter.desktop", "libreoffice-writer.desktop")),
            "geany_ide" to GuiTestSpec("geany", listOf("geany.desktop"))
        )

        private val CLI_SMOKE_COMMANDS = mapOf(
            "python_dev" to "python3 --version",
            "node_dev" to "node --version",
            "android_dev" to "java -version",
            "code_server" to "code-server --version",
            "nginx_web" to "nginx -v",
            "openssh_server" to "sshd -V 2>&1 | head -n 1 || true",
            "web_terminal" to "ttyd --version",
            "docker_tools" to "docker --version",
            "c_cpp_dev" to "clang --version && cmake --version",
            "go_dev" to "go version",
            "rust_dev" to "rustc --version && cargo --version",
            "php_dev" to "php -v",
            "ruby_dev" to "ruby -v",
            "vscode_desktop" to "code --version --no-sandbox --user-data-dir=/root/.vscode",
            "postgresql_db" to "psql --version",
            "mariadb_db" to "mariadb --version || mysql --version",
            "redis_server" to "redis-server --version",
            "cli_power_tools" to "tmux -V && htop --version && jq --version",
            "system_monitor" to "btop --version && fastfetch --version",
            "zsh_shell" to "zsh --version",
            "aria2_downloader" to "aria2c --version | head -n 1",
            "rclone_sync" to "rclone version | head -n 1",
            "blender_3d" to "blender --version",
            "kdenlive_video" to "kdenlive --version",
            "gimp_editor" to "gimp --version",
            "ffmpeg_media" to "ffmpeg -version | head -n 1",
            "libreoffice_suite" to "libreoffice --version",
            "geany_ide" to "geany --version",
            "firefox_browser" to "firefox --version || firefox-esr --version",
            "iperf3_bench" to "iperf3 --version | head -n 1",
            "nmap_nettools" to "nmap --version | head -n 1",
            "tshark_net" to "tshark --version | head -n 1"
        )
    }

    private data class GuiTestSpec(
        val launchCommand: String,
        val desktopFileCandidates: List<String>
    )

    @Test
    fun runDistroAndPackageMatrix() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val distroArg = args.getString("distro", "ubuntu_26_04")
        val pkgArg = args.getString("pkg", "all")
        val runGuiTests = args.getString("gui", "true").toBoolean()
        val reuseContainer = args.getString("reuseContainer", "false").toBoolean()
        val skipUninstall = args.getString("skipUninstall", "false").toBoolean()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)

        val pRootEngine = PRootEngine(context)
        pRootEngine.ensurePRootExecutable()

        val containerManager = ContainerManager(context)
        if (!reuseContainer) {
            // Clean any leftover containers from prior test runs so Dashboard starts fresh
            containerManager.getAllContainers().forEach { existing ->
                containerManager.deleteContainer(existing.id)
            }
        }
        val rootfsManager = RootfsManager(context, pRootEngine)
        val softwareInstaller = SoftwareInstaller(pRootEngine)

        val targetDistros = if (distroArg == "all") {
            DistroCatalog.ALL_DISTROS
        } else {
            val ids = distroArg.split(",").map { it.trim() }.toSet()
            DistroCatalog.ALL_DISTROS.filter { it.id in ids }
        }
        assertTrue("No matching distros for '$distroArg'", targetDistros.isNotEmpty())

        val sharedUtilityBins = setOf("usr/bin/git", "bin/git", "usr/bin/curl", "usr/bin/wget", "usr/bin/python3")
        val passedCases = mutableListOf<String>()
        val failures = mutableListOf<String>()

        for (distroDef in targetDistros) {
            val testContainerId = "e2e_${distroDef.id}"
            val existingContainer = if (reuseContainer) {
                containerManager.getAllContainers().firstOrNull { it.id == testContainerId }
            } else null

            val container = if (existingContainer != null && ContainerManager.isRealRootfs(File(existingContainer.rootDirPath))) {
                Log.i(TAG, "=== Reusing existing container ${distroDef.id} ===")
                existingContainer
            } else {
                containerManager.deleteContainer(testContainerId)
                // Always clear any cached rootfs archives so download is verified fresh every run
                clearRootfsArchiveCache(context)
                containerManager.createContainerEntry(
                    id = testContainerId,
                    name = "E2E ${distroDef.name}",
                    distroId = distroDef.id,
                    defaultUser = "root",
                    userPassword = "e2epassword"
                )
            }
            val rootfsDir = File(container.rootDirPath)

            try {
                if (existingContainer == null || !ContainerManager.isRealRootfs(rootfsDir)) {
                    Log.i(TAG, "=== Provisioning ${distroDef.id} (fresh download, no cache) ===")
                    var downloadError: String? = null
                    rootfsManager.downloadAndInstallDistro(
                        targetDir = rootfsDir,
                        distroDef = distroDef,
                        containerName = container.name,
                        rootPassword = "e2epassword",
                        username = "user",
                        userPassword = "e2epassword"
                    ).collect { state ->
                        when (state) {
                            is DownloadState.Error -> downloadError = state.message
                            is DownloadState.Extracting -> Log.d(TAG, "[${distroDef.id}] ${state.message}")
                            else -> Unit
                        }
                    }
                    if (downloadError != null) {
                        fail("Distro ${distroDef.id} provisioning failed: $downloadError")
                    }
                }
                assertTrue("Rootfs missing after provisioning ${distroDef.id}", ContainerManager.isRealRootfs(rootfsDir))

                val allPresets = SoftwarePackage.getPresetsForContainer(container)
                val targetPackages = if (pkgArg == "all") {
                    allPresets
                } else {
                    val ids = pkgArg.split(",").map { it.trim() }.toSet()
                    allPresets.filter { it.id in ids }
                }
                if (targetPackages.isEmpty()) {
                    Log.i(TAG, "[SKIP] ${distroDef.id}/$pkgArg: not supported on ${distroDef.id}")
                    continue
                }

                var xfceInstalled = false
                for (pkg in targetPackages) {
                    val caseId = "${distroDef.id}/${pkg.id}"
                    try {
                        Log.i(TAG, "=== [${distroDef.id}] Installing ${pkg.id} ===")
                        val preExistingBins = pkg.expectedBinaries.filter {
                            SoftwarePackage.isBinaryPresent(rootfsDir, it)
                        }.toSet()
                        var installErr: String? = null
                        softwareInstaller.installPackage(pkg, rootfsDir, distroDef).collect { step ->
                            if (step is InstallStepState.Error) installErr = step.errorMessage
                        }
                        if (installErr != null) {
                            fail("[$caseId] Install failed: $installErr")
                        }
                        if (pkg.id == "xfce_desktop") {
                            xfceInstalled = true
                        }

                        // Verify expected binaries
                        for (bin in pkg.expectedBinaries) {
                            assertTrue(
                                "[$caseId] Expected binary missing: $bin",
                                SoftwarePackage.isBinaryPresent(rootfsDir, bin)
                            )
                        }

                        // Run CLI smoke check if applicable
                        CLI_SMOKE_COMMANDS[pkg.id]?.let { smokeCmd ->
                            val smokeResult = runInPRoot(pRootEngine, rootfsDir, distroDef, smokeCmd)
                            assertTrue(
                                "[$caseId] CLI smoke command '$smokeCmd' failed (exit=${smokeResult.first}): ${smokeResult.second}",
                                smokeResult.first == 0
                            )
                        }

                        // Verify .desktop entry for GUI packages even when --no-gui skips UI/VNC automation
                        val guiSpec = GUI_PACKAGES[pkg.id]
                        if (guiSpec != null) {
                            verifyDesktopEntry(rootfsDir, distroDef, pkg.id, guiSpec)
                        }
                        if (runGuiTests && guiSpec != null) {
                            if (!xfceInstalled) {
                                val xfcePkg = allPresets.first { it.id == "xfce_desktop" }
                                var xfceErr: String? = null
                                softwareInstaller.installPackage(xfcePkg, rootfsDir, distroDef).collect { step ->
                                    if (step is InstallStepState.Error) xfceErr = step.errorMessage
                                }
                                if (xfceErr != null) fail("[${distroDef.id}] XFCE prerequisite install failed: $xfceErr")
                                xfceInstalled = true
                            }
                            verifyGuiAppInDesktopAndVnc(
                                context = context,
                                device = device,
                                pRootEngine = pRootEngine,
                                rootfsDir = rootfsDir,
                                distroDef = distroDef,
                                containerName = container.name,
                                pkgId = pkg.id,
                                guiSpec = guiSpec
                            )
                        }

                        // Skip uninstalling when skipUninstall=true or for xfce_desktop
                        if (!skipUninstall && pkg.id != "xfce_desktop") {
                            Log.i(TAG, "=== [${distroDef.id}] Uninstalling ${pkg.id} ===")
                            var uninstallErr: String? = null
                            softwareInstaller.uninstallPackage(pkg, rootfsDir, distroDef).collect { step ->
                                if (step is InstallStepState.Error) uninstallErr = step.errorMessage
                            }
                            if (uninstallErr != null) {
                                fail("[$caseId] Uninstall failed: $uninstallErr")
                            }
                            for (bin in pkg.expectedBinaries) {
                                if (bin !in preExistingBins && bin !in sharedUtilityBins) {
                                    assertFalse(
                                        "[$caseId] Binary still present after uninstall: $bin",
                                        SoftwarePackage.isBinaryPresent(rootfsDir, bin)
                                    )
                                }
                            }
                        }
                        passedCases.add(caseId)
                        Log.i(TAG, "[PASS] $caseId")
                    } catch (t: Throwable) {
                        val msg = t.message ?: t.javaClass.simpleName
                        failures.add("$caseId: $msg")
                        Log.e(TAG, "[FAIL] $caseId: $msg")
                    }
                }
            } catch (t: Throwable) {
                val msg = t.message ?: t.javaClass.simpleName
                failures.add("${distroDef.id}/provision: $msg")
                Log.e(TAG, "[FAIL] ${distroDef.id}/provision: $msg")
            } finally {
                if (!reuseContainer && !skipUninstall) {
                    containerManager.deleteContainer(testContainerId)
                }
            }
        }

        Log.i(TAG, "=== E2E MATRIX SUMMARY: ${passedCases.size} passed, ${failures.size} failed ===")
        passedCases.forEach { Log.i(TAG, "  PASS: $it") }
        failures.forEach { Log.e(TAG, "  FAIL: $it") }
        if (failures.isNotEmpty()) {
            fail("${failures.size} E2E case(s) failed (${passedCases.size} passed):\n" + failures.joinToString("\n"))
        }
    }

    private fun verifyDesktopEntry(
        rootfsDir: File,
        distroDef: DistroDefinition,
        pkgId: String,
        guiSpec: GuiTestSpec
    ) {
        val appsDir = File(rootfsDir, "usr/share/applications")
        val desktopFiles = appsDir.listFiles()?.map { it.name }.orEmpty()
        val matchedDesktop = guiSpec.desktopFileCandidates.firstOrNull { candidate ->
            candidate in desktopFiles
        } ?: desktopFiles.firstOrNull { name ->
            name.endsWith(".desktop") && name.contains(pkgId.substringBefore("_"), ignoreCase = true)
        }
        assertTrue(
            "[${distroDef.id}/$pkgId] No .desktop menu entry found in /usr/share/applications/ (found: ${desktopFiles.take(15)})",
            matchedDesktop != null
        )

        val desktopContent = File(appsDir, matchedDesktop!!).readText()
        assertFalse(
            "[${distroDef.id}/$pkgId] $matchedDesktop has NoDisplay=true and would be hidden from XFCE menu",
            desktopContent.contains("NoDisplay=true", ignoreCase = true)
        )
    }

    private fun verifyGuiAppInDesktopAndVnc(
        context: Context,
        device: UiDevice,
        pRootEngine: PRootEngine,
        rootfsDir: File,
        distroDef: DistroDefinition,
        containerName: String,
        pkgId: String,
        guiSpec: GuiTestSpec
    ) {
        // 1. Verify .desktop file is present in /usr/share/applications/ for XFCE Applications menu
        verifyDesktopEntry(rootfsDir, distroDef, pkgId, guiSpec)

        // 2. Wake device and launch fresh MainActivity (clear-task reloads MainViewModel containers)
        try {
            device.wakeUp()
            device.executeShellCommand("wm dismiss-keyguard")
        } catch (_: Throwable) {}
        device.executeShellCommand("am start -W --activity-clear-task -n ${context.packageName}/.MainActivity")
        device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10_000L)

        device.wait(Until.findObject(By.text("Get Started")), 3_000L)?.click()
        device.wait(Until.hasObject(By.text("Desktop")), 10_000L)
        try {
            UiScrollable(UiSelector().scrollable(true)).scrollTextIntoView(containerName)
        } catch (_: Throwable) {}
        assertTrue(
            "[${distroDef.id}/$pkgId] Container card '$containerName' not visible on Dashboard",
            device.wait(Until.hasObject(By.text(containerName)), 10_000L)
        )
        val titleNode = device.findObject(By.text(containerName))
        val navBarCutoff = device.displayHeight - 250
        var desktopButtons = device.findObjects(By.text("Desktop"))
        var desktopBtn = desktopButtons.firstOrNull { btn ->
            btn.visibleBounds.top < navBarCutoff && (titleNode == null || btn.visibleBounds.top >= titleNode.visibleBounds.top)
        }
        if (desktopBtn == null) {
            try {
                UiScrollable(UiSelector().scrollable(true)).scrollForward()
            } catch (_: Throwable) {}
            val refreshedTitle = device.findObject(By.text(containerName))
            desktopButtons = device.findObjects(By.text("Desktop"))
            desktopBtn = desktopButtons.firstOrNull { btn ->
                btn.visibleBounds.top < navBarCutoff && (refreshedTitle == null || btn.visibleBounds.top >= refreshedTitle.visibleBounds.top)
            }
        }
        assertTrue(
            "[${distroDef.id}/$pkgId] Card 'Desktop' button not found on Dashboard (candidates=${desktopButtons.map { it.visibleBounds }})",
            desktopBtn != null
        )
        desktopBtn!!.click()

        // Navigate to Desktop tab; card "Desktop" already starts the session, click "Start Desktop Session" only if shown
        device.wait(Until.findObject(By.text("Start Desktop Session")), 3_000L)?.click()

        // 3. Wait for Xvnc + XFCE session to start and verify inside PRoot
        val deadline = System.currentTimeMillis() + 45_000L
        var xfceRunning = false
        while (System.currentTimeMillis() < deadline) {
            val check = runInPRoot(
                pRootEngine,
                rootfsDir,
                distroDef,
                "pgrep -x Xvnc >/dev/null 2>&1 || pgrep -x Xtigervnc >/dev/null 2>&1"
            )
            if (check.first == 0) {
                xfceRunning = true
                break
            }
            Thread.sleep(1500L)
        }
        assertTrue("[${distroDef.id}/$pkgId] Desktop VNC server (Xvnc) did not start within 45s", xfceRunning)

        // 4. Launch GUI app on DISPLAY=:1 and confirm it stays alive for 6 seconds (no crash reporter!)
        val binName = pkgId.substringBefore("_")
        val launchCheck = runInPRoot(
            pRootEngine,
            rootfsDir,
            distroDef,
            "DISPLAY=:1 GLYCIN_SANDBOX=disable ${guiSpec.launchCommand} </dev/null >/tmp/e2e_gui.log 2>&1 & APP_PID=\$!; sleep 6; ALIVE=1; if kill -0 \$APP_PID 2>/dev/null || pgrep -x \"$binName\" >/dev/null 2>&1 || pgrep -x code >/dev/null 2>&1 || pgrep -x electron >/dev/null 2>&1; then ALIVE=0; fi; kill -9 \$APP_PID 2>/dev/null || true; for proc in $binName code electron dbus-daemon dbus-launch at-spi-bus-launcher at-spi2-registryd dconf-service gvfsd; do pkill -9 -x \"\$proc\" 2>/dev/null || true; done; if [ \$ALIVE -eq 0 ]; then echo E2E_GUI_ALIVE; exit 0; else env | grep -i glycin >> /tmp/e2e_gui.log 2>&1; ls -la /usr/bin/bwrap /usr/local/bin/bwrap >> /tmp/e2e_gui.log 2>&1; head -n 5 /usr/bin/bwrap >> /tmp/e2e_gui.log 2>&1; cat /tmp/e2e_gui.log; exit 1; fi",
            timeoutSeconds = 25L
        )
        assertTrue(
            "[${distroDef.id}/$pkgId] GUI app crashed within 6s inside VNC DISPLAY=:1 (exit=${launchCheck.first}): ${launchCheck.second}",
            launchCheck.first == 0 || launchCheck.second.contains("E2E_GUI_ALIVE")
        )

        // 5. Clean up VNC session and return to Dashboard
        runInPRoot(
            pRootEngine,
            rootfsDir,
            distroDef,
            "vncserver -kill :1 2>/dev/null || true; pkill -9 -x Xvnc 2>/dev/null || pkill -9 -x Xtigervnc 2>/dev/null || true"
        )
        device.pressBack()
        device.waitForIdle()
    }

    private fun clearRootfsArchiveCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("rootfs_base.tar")) {
                file.delete()
            }
        }
    }

    private fun runInPRoot(
        pRootEngine: PRootEngine,
        rootfsDir: File,
        distroDef: DistroDefinition,
        shellCommand: String,
        timeoutSeconds: Long = 60L
    ): Pair<Int, String> {
        val shell = if (distroDef.id.startsWith("alpine")) "/bin/sh" else "/bin/bash"
        val tmpDir = File(pRootEngine.context.cacheDir, "proot_tmp").apply { mkdirs() }
        val outFile = File(tmpDir, "e2e_cmd_${System.nanoTime()}.log")
        val config = PRootConfig(
            rootfsDir = rootfsDir,
            tmpDir = tmpDir,
            defaultShell = shell
        )
        val cmd = pRootEngine.buildPRootCommand(
            config = config,
            command = listOf(shell, "-c", shellCommand)
        )
        val pb = ProcessBuilder(cmd)
        pb.environment().putAll(pRootEngine.getEnvironmentVariables())
        pb.redirectErrorStream(true)
        pb.redirectOutput(outFile)
        val proc = pb.start()
        val finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            proc.destroyForcibly()
        }
        val output = if (outFile.exists()) outFile.readText() else ""
        outFile.delete()
        val exitCode = if (finished) proc.exitValue() else -1
        return exitCode to output
    }
}
