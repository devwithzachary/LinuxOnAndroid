package com.devwithzachary.completelinuxinstaller.engine.desktop

enum class DesktopStatus {
    IDLE,
    INSTALLING_PACKAGES,
    STARTING_SERVER,
    RUNNING,
    STOPPING,
    ERROR
}

enum class DesktopTouchMode(val label: String) {
    TRACKPAD("Trackpad"),
    DIRECT_TOUCH("Touch Screen")
}


data class DesktopResolution(
    val width: Int,
    val height: Int,
    val label: String,
    val isAutomatic: Boolean = false,
    val scalePercent: Int = 100
) {
    companion object {
        val AUTO = DesktopResolution(0, 0, "Automatic (Match Screen Space)", isAutomatic = true, scalePercent = 100)
        val HD_720P = DesktopResolution(1280, 720, "1280x720 (HD)")
        val FHD_1080P = DesktopResolution(1920, 1080, "1920x1080 (Full HD)")
        val XGA_4_3 = DesktopResolution(1024, 768, "1024x768 (XGA 4:3)")
        val WXGA_16_10 = DesktopResolution(1280, 800, "1280x800 (16:10)")

        val DEFAULT_PRESETS = listOf(AUTO, HD_720P, FHD_1080P, XGA_4_3, WXGA_16_10)

        val SCALE_PRESETS = listOf(100, 125, 150, 175, 200)

        fun createCustom(width: Int, height: Int): DesktopResolution {
            val clampedW = (width / 2) * 2
            val clampedH = (height / 2) * 2
            return DesktopResolution(
                clampedW.coerceIn(320, 3840),
                clampedH.coerceIn(320, 3840),
                "${clampedW}x${clampedH} (Device Screen)"
            )
        }
    }
}

sealed class VncConnectionState {
    object Disconnected : VncConnectionState()
    data class Connecting(val message: String = "Connecting to desktop server...") : VncConnectionState()
    data class Connected(val desktopName: String, val width: Int, val height: Int) : VncConnectionState()
    data class Error(val message: String) : VncConnectionState()
}

data class DesktopUiState(
    val activeContainerId: String? = null,
    val activeContainerName: String? = null,
    val sessionStatus: DesktopStatus = DesktopStatus.IDLE,
    val resolution: DesktopResolution = DesktopResolution.AUTO,
    val uiScalePercent: Int = 100,
    val touchMode: DesktopTouchMode = DesktopTouchMode.TRACKPAD,
    val selectedUser: String = "root",
    val availableUsers: List<String> = listOf("root"),
    val displayPort: Int = 5901,
    val errorMessage: String? = null,
    val isPackagesInstalled: Boolean = false,
    val hasUpgradeAvailable: Boolean = false,
    val packageInstallProgress: String? = null,
    val packageInstallLogs: String = "",
    val isCtrlActive: Boolean = false,
    val isAltActive: Boolean = false,
    val isShiftActive: Boolean = false,
    val isFitToScreen: Boolean = true
)
