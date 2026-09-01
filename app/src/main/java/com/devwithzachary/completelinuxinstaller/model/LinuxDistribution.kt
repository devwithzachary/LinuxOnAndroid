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
            val arch = DistroCatalog.getForSystemArch(archName)
            return DistroCatalog.UBUNTU_26_04.toLinuxDistribution(arch)
        }

        fun forDistroAndArch(distroId: String, archName: String): LinuxDistribution {
            val arch = DistroCatalog.getForSystemArch(archName)
            val distroDef = DistroCatalog.getById(distroId)
            return distroDef.toLinuxDistribution(arch)
        }
    }
}
