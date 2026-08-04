package com.devwithzachary.completelinuxinstaller.model

enum class SystemArchitecture(val label: String, val rootfsArch: String) {
    ARM64("ARM64 (aarch64)", "arm64"),
    X86_64("x86_64 (amd64)", "amd64"),
    ARMV7("ARMv7 (armhf)", "armhf")
}

data class LinuxDistribution(
    val id: String = "ubuntu_26_04",
    val name: String = "Ubuntu 26.04 LTS (Resolute Raccoon)",
    val version: String = "26.04",
    val architecture: SystemArchitecture,
    val downloadUrl: String,
    val expectedSizeMb: Int = 28,
    val description: String = "Barebones minimal Ubuntu LTS rootfs image for ARM64 / x86_64 non-rooted PRoot container."
) {
    companion object {
        fun defaultForArch(archName: String): LinuxDistribution {
            val arch = when {
                archName.contains("aarch64") || archName.contains("arm64") -> SystemArchitecture.ARM64
                archName.contains("x86_64") || archName.contains("amd64") -> SystemArchitecture.X86_64
                else -> SystemArchitecture.ARMV7
            }
            val url = when (arch) {
                SystemArchitecture.ARM64 -> "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-arm64.tar.gz"
                SystemArchitecture.X86_64 -> "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-amd64.tar.gz"
                SystemArchitecture.ARMV7 -> "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-armhf.tar.gz"
            }
            return LinuxDistribution(
                architecture = arch,
                downloadUrl = url
            )
        }
    }
}
