package com.devwithzachary.completelinuxinstaller.engine

import android.util.Log
import java.io.File
import java.io.StringReader
import java.util.Properties

data class RootfsVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val installedAt: Long = System.currentTimeMillis(),
    val lastUpgradedAt: Long = System.currentTimeMillis()
)

sealed class UpgradeState {
    data object Idle : UpgradeState()
    data class Upgrading(
        val currentStepName: String,
        val logs: List<String> = emptyList(),
        val progressPercent: Int = 0
    ) : UpgradeState()
    data class Success(
        val fromVersion: Int,
        val toVersion: Int,
        val logs: List<String> = emptyList()
    ) : UpgradeState()
    data class Error(
        val message: String,
        val logs: List<String> = emptyList()
    ) : UpgradeState()
}

interface RootfsMigrationStep {
    val targetVersionCode: Int
    val name: String
    val description: String
    fun execute(pRootEngine: PRootEngine? = null, rootfsDir: File, emitLog: (String) -> Unit): Boolean
}

object RootfsMigrationManager {

    private const val TAG = "RootfsMigration"
    const val VERSION_FILE_PATH = "etc/linuxonandroid_version"
    const val PACKAGE_VERSION_FILE_PATH = "etc/linuxonandroid_packages"
    const val LEGACY_VERSION_CODE = 1
    const val LEGACY_VERSION_NAME = "1.0.0"

    fun readVersion(rootfsDir: File): RootfsVersionInfo? {
        val versionFile = File(rootfsDir, VERSION_FILE_PATH)
        if (!versionFile.exists()) {
            return if (File(rootfsDir, "bin").exists() || File(rootfsDir, "usr").exists()) {
                RootfsVersionInfo(
                    versionCode = LEGACY_VERSION_CODE,
                    versionName = LEGACY_VERSION_NAME,
                    installedAt = versionFile.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis(),
                    lastUpgradedAt = 0L
                )
            } else {
                null
            }
        }

        return try {
            val content = versionFile.readText()
            val props = Properties()
            props.load(StringReader(content))
            val vCode = props.getProperty("version_code")?.toIntOrNull() ?: LEGACY_VERSION_CODE
            val vName = props.getProperty("version_name") ?: LEGACY_VERSION_NAME
            val instAt = props.getProperty("installed_at")?.toLongOrNull() ?: System.currentTimeMillis()
            val upAt = props.getProperty("last_upgraded_at")?.toLongOrNull() ?: System.currentTimeMillis()
            RootfsVersionInfo(vCode, vName, instAt, upAt)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading rootfs version", e)
            RootfsVersionInfo(LEGACY_VERSION_CODE, LEGACY_VERSION_NAME)
        }
    }

    fun writeVersion(rootfsDir: File, info: RootfsVersionInfo) {
        val versionFile = File(rootfsDir, VERSION_FILE_PATH)
        try {
            versionFile.parentFile?.mkdirs()
            val content = buildString {
                appendLine("version_code=${info.versionCode}")
                appendLine("version_name=${info.versionName}")
                appendLine("installed_at=${info.installedAt}")
                appendLine("last_upgraded_at=${info.lastUpgradedAt}")
            }
            versionFile.writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing rootfs version", e)
        }
    }

    fun readPackageVersions(rootfsDir: File): Map<String, Int> {
        val pkgFile = File(rootfsDir, PACKAGE_VERSION_FILE_PATH)
        if (!pkgFile.exists()) return emptyMap()
        return try {
            val props = Properties()
            props.load(StringReader(pkgFile.readText()))
            props.stringPropertyNames().associateWith { key ->
                props.getProperty(key)?.toIntOrNull() ?: 1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading package versions", e)
            emptyMap()
        }
    }

    fun writePackageVersion(rootfsDir: File, packageId: String, version: Int) {
        val pkgFile = File(rootfsDir, PACKAGE_VERSION_FILE_PATH)
        try {
            pkgFile.parentFile?.mkdirs()
            val currentMap = readPackageVersions(rootfsDir).toMutableMap()
            currentMap[packageId] = version
            val content = buildString {
                for ((id, ver) in currentMap) {
                    appendLine("$id=$ver")
                }
            }
            pkgFile.writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing package version for $packageId", e)
        }
    }

    fun getPackageVersion(rootfsDir: File, packageId: String): Int? {
        val map = readPackageVersions(rootfsDir)
        return map[packageId]
    }

    val ALL_MIGRATIONS: List<RootfsMigrationStep> = listOf(
        // v2: DNS Resolver & Hosts Configuration
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 2
            override val name: String = "Network & DNS Resolver Setup"
            override val description: String = "Configures reliable Google & Cloudflare DNS nameservers and localhost mappings."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Applying DNS and hosts configuration...")
                val etcDir = File(rootfsDir, "etc").apply { mkdirs() }
                File(etcDir, "resolv.conf").writeText(
                    "nameserver 8.8.8.8\nnameserver 1.1.1.1\nnameserver 8.8.4.4\n"
                )
                File(etcDir, "hosts").writeText(
                    "127.0.0.1   localhost localhost.localdomain ubuntu\n::1         localhost ip6-localhost ip6-loopback\n"
                )
                emitLog("DNS and hosts configured successfully.")
                return true
            }
        },

        // v3: APT Sandboxing & Dpkg Compatibility Options
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 3
            override val name: String = "APT & DPKG Sandbox Configs"
            override val description: String = "Sets up non-root APT sandbox flags and unsafe-io/force-confold dpkg configurations."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Configuring APT sandboxing & dpkg flags...")
                val aptConfDir = File(rootfsDir, "etc/apt/apt.conf.d").apply { mkdirs() }
                File(aptConfDir, "99linuxonandroid").writeText(
                    "APT::Sandbox::User \"root\";\n" +
                            "Acquire::http::Pipeline-Depth \"0\";\n" +
                            "Acquire::http::No-Cache \"true\";\n" +
                            "Acquire::PDiffs \"false\";\n" +
                            "Acquire::ForceIPv4 \"true\";\n"
                )

                val dpkgCfgDir = File(rootfsDir, "etc/dpkg/dpkg.cfg.d").apply { mkdirs() }
                File(dpkgCfgDir, "00-linuxonandroid").writeText(
                    "force-all\nforce-unsafe-io\nforce-overwrite\nforce-confold\nforce-confdef\nforce-depends\n"
                )

                val tmpDir = File(rootfsDir, "tmp").apply { mkdirs() }
                val varTmpDir = File(rootfsDir, "var/tmp").apply { mkdirs() }
                try {
                    tmpDir.setReadable(true, false)
                    tmpDir.setWritable(true, false)
                    tmpDir.setExecutable(true, false)
                    varTmpDir.setReadable(true, false)
                    varTmpDir.setWritable(true, false)
                    varTmpDir.setExecutable(true, false)
                } catch (_: Exception) {}

                emitLog("APT & DPKG configs applied.")
                return true
            }
        },

        // v4: PRoot Daemon Policy-RC.D & Systemctl Stubs
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 4
            override val name: String = "Service Daemon Prevention Stubs"
            override val description: String = "Installs policy-rc.d (exit 101) and systemctl stubs to prevent dpkg installation failures."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Installing policy-rc.d service lock...")
                val usrSbinDir = File(rootfsDir, "usr/sbin").apply { mkdirs() }
                val policyRcD = File(usrSbinDir, "policy-rc.d")
                policyRcD.writeText("#!/bin/sh\nexit 101\n")
                policyRcD.setExecutable(true, false)

                val systemctl = File(rootfsDir, "usr/bin/systemctl").apply { parentFile?.mkdirs() }
                systemctl.writeText("#!/bin/sh\nexit 0\n")
                systemctl.setExecutable(true, false)

                val binSystemctl = File(rootfsDir, "bin/systemctl").apply { parentFile?.mkdirs() }
                binSystemctl.writeText("#!/bin/sh\nexit 0\n")
                binSystemctl.setExecutable(true, false)

                emitLog("Daemon control stubs installed.")
                return true
            }
        },

        // v5: Systemd & Dpkg Component Stubs
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 5
            override val name: String = "Systemd Tool Stubs"
            override val description: String = "Stubs out systemd-tmpfiles, systemd-sysusers, and dpkg-preconfigure."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Setting up systemd component stubs...")
                val stubs = listOf(
                    "usr/bin/systemd-tmpfiles",
                    "bin/systemd-tmpfiles",
                    "usr/bin/systemd-sysusers",
                    "bin/systemd-sysusers",
                    "usr/bin/systemd-detect-virt",
                    "bin/systemd-detect-virt",
                    "usr/sbin/dpkg-preconfigure"
                )
                for (stub in stubs) {
                    val file = File(rootfsDir, stub).apply { parentFile?.mkdirs() }
                    file.writeText("#!/bin/sh\nexit 0\n")
                    file.setExecutable(true, false)
                }
                emitLog("Systemd tool stubs created.")
                return true
            }
        },

        // v6: Core System Accounts
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 6
            override val name: String = "System User Accounts & Groups"
            override val description: String = "Ensures messagebus, www-data, and sshd accounts exist in passwd/group/shadow."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Ensuring essential system users (messagebus, www-data, sshd)...")
                val etcDir = File(rootfsDir, "etc").apply { mkdirs() }
                val groupFile = File(etcDir, "group").apply { if (!exists()) createNewFile() }
                val passwdFile = File(etcDir, "passwd").apply { if (!exists()) createNewFile() }
                val shadowFile = File(etcDir, "shadow").apply { if (!exists()) createNewFile() }

                val groupText = groupFile.readText()
                val passwdText = passwdFile.readText()
                val shadowText = shadowFile.readText()

                val newGroups = StringBuilder(groupText)
                if (!groupText.contains("messagebus:")) newGroups.appendLine("messagebus:x:101:")
                if (!groupText.contains("www-data:")) newGroups.appendLine("www-data:x:33:")
                if (!groupText.contains("sshd:")) newGroups.appendLine("sshd:x:102:")
                groupFile.writeText(newGroups.toString())

                val newPasswd = StringBuilder(passwdText)
                if (!passwdText.contains("messagebus:")) newPasswd.appendLine("messagebus:x:101:101:D-Bus Message System Daemon:/nonexistent:/bin/false")
                if (!passwdText.contains("www-data:")) newPasswd.appendLine("www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin")
                if (!passwdText.contains("sshd:")) newPasswd.appendLine("sshd:x:102:102:Privilege-separated SSH:/run/sshd:/usr/sbin/nologin")
                passwdFile.writeText(newPasswd.toString())

                val newShadow = StringBuilder(shadowText)
                if (!shadowText.contains("messagebus:")) newShadow.appendLine("messagebus:*:19700:0:99999:7:::")
                if (!shadowText.contains("www-data:")) newShadow.appendLine("www-data:*:19700:0:99999:7:::")
                if (!shadowText.contains("sshd:")) newShadow.appendLine("sshd:*:19700:0:99999:7:::")
                shadowFile.writeText(newShadow.toString())

                emitLog("System users verified.")
                return true
            }
        },

        // v7: Interactive Ubuntu Sudo & PAM Rules
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 7
            override val name: String = "Native Ubuntu Sudo & PAM Authentication"
            override val description: String = "Configures pam_permit.so rules and sets 4755 setuid permissions on sudo binary."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Configuring PAM authentication and sudo permissions...")
                val pamDir = File(rootfsDir, "etc/pam.d").apply { mkdirs() }
                val pamContent = "auth sufficient pam_permit.so\n" +
                        "account sufficient pam_permit.so\n" +
                        "session sufficient pam_permit.so\n" +
                        "password sufficient pam_permit.so\n"

                File(pamDir, "sudo").writeText(pamContent)
                File(pamDir, "su").writeText(pamContent)
                File(pamDir, "su-l").writeText(pamContent)

                val sudoBin = File(rootfsDir, "usr/bin/sudo")
                if (sudoBin.exists()) {
                    try {
                        sudoBin.setExecutable(true, false)
                        sudoBin.setReadable(true, false)
                    } catch (_: Exception) {}
                }

                val sudoersDir = File(rootfsDir, "etc/sudoers.d").apply { mkdirs() }
                val sudoersFile = File(rootfsDir, "etc/sudoers")
                if (sudoersFile.exists()) {
                    try {
                        sudoersFile.setReadable(true, false)
                    } catch (_: Exception) {}
                }

                emitLog("Sudo & PAM authentication rules configured.")
                return true
            }
        },

        // v8: OpenSSH Configuration, PTY Device Node & Dpkg Database Health
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 8
            override val name: String = "OpenSSH, PTY Node & DPKG Database Recovery"
            override val description: String = "Configures OpenSSH security settings, /dev/ptmx node compatibility, and recovers dpkg lock states."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Configuring OpenSSH and PTY terminal subsystem...")
                val sshConfigDir = File(rootfsDir, "etc/ssh/sshd_config.d").apply { mkdirs() }
                val sshConfigFile = File(sshConfigDir, "00-linuxonandroid.conf")
                sshConfigFile.writeText(
                    "Port 2222\n" +
                            "PermitRootLogin yes\n" +
                            "PasswordAuthentication yes\n" +
                            "KbdInteractiveAuthentication yes\n" +
                            "UsePAM no\n" +
                            "StrictModes no\n" +
                            "SetEnv PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n" +
                            "Subsystem sftp internal-sftp\n"
                )

                val pamSshd = File(rootfsDir, "etc/pam.d/sshd")
                if (pamSshd.exists()) {
                    try {
                        val text = pamSshd.readText()
                        val fixed = text.replace(Regex("^session.*pam_loginuid.so", RegexOption.MULTILINE), "#$0")
                        pamSshd.writeText(fixed)
                    } catch (_: Exception) {}
                }

                val runSshd = File(rootfsDir, "run/sshd").apply { mkdirs() }
                val varRunSshd = File(rootfsDir, "var/run/sshd").apply { mkdirs() }
                val varEmpty = File(rootfsDir, "var/empty").apply { mkdirs() }
                try {
                    runSshd.setReadable(true, false)
                    runSshd.setExecutable(true, false)
                    varRunSshd.setReadable(true, false)
                    varRunSshd.setExecutable(true, false)
                    varEmpty.setReadable(true, false)
                    varEmpty.setExecutable(true, false)
                } catch (_: Exception) {}

                // Clean stale dpkg locks
                emitLog("Cleaning stale dpkg locks and temporary files...")
                val lockFiles = listOf(
                    File(rootfsDir, "var/lib/dpkg/lock"),
                    File(rootfsDir, "var/lib/dpkg/lock-frontend"),
                    File(rootfsDir, "var/cache/apt/archives/lock")
                )
                for (lf in lockFiles) {
                    if (lf.exists()) {
                        try { lf.delete() } catch (_: Exception) {}
                    }
                }

                emitLog("OpenSSH and terminal subsystem upgraded.")
                return true
            }
        },

        // v10: OS Release, LSB Release & Environment Codename Variables
        object : RootfsMigrationStep {
            override val targetVersionCode: Int = 10
            override val name: String = "OS Release & Environment Codename Configuration"
            override val description: String = "Configures UBUNTU_CODENAME and VERSION_CODENAME across /etc/os-release, /etc/environment, /etc/lsb-release, and /etc/profile.d/ for Docker and third-party apt repositories."

            override fun execute(pRootEngine: PRootEngine?, rootfsDir: File, emitLog: (String) -> Unit): Boolean {
                emitLog("Configuring OS release and environment codename exports...")
                val etcDir = File(rootfsDir, "etc").apply { mkdirs() }

                // Read existing codename from os-release or default to resolute
                var codename = "resolute"
                val osReleaseFile = File(etcDir, "os-release")
                if (osReleaseFile.exists()) {
                    val content = osReleaseFile.readText()
                    val match = Regex("""(?:UBUNTU_CODENAME|VERSION_CODENAME)=["']?([a-zA-Z0-9_-]+)["']?""").find(content)
                    if (match != null) {
                        codename = match.groupValues[1]
                    }
                    if (!content.contains("UBUNTU_CODENAME=") || !content.contains("VERSION_CODENAME=")) {
                        val updated = buildString {
                            append(content.trimEnd())
                            appendLine()
                            if (!content.contains("UBUNTU_CODENAME=")) appendLine("UBUNTU_CODENAME=$codename")
                            if (!content.contains("VERSION_CODENAME=")) appendLine("VERSION_CODENAME=$codename")
                        }
                        osReleaseFile.writeText(updated)
                    }
                } else {
                    osReleaseFile.writeText(
                        """
                        NAME="Ubuntu"
                        VERSION="26.04 LTS (Resolute Raccoon)"
                        ID=ubuntu
                        ID_LIKE=debian
                        PRETTY_NAME="Ubuntu 26.04 LTS"
                        VERSION_ID="26.04"
                        UBUNTU_CODENAME=resolute
                        VERSION_CODENAME=resolute
                        """.trimIndent() + "\n"
                    )
                }

                // /etc/lsb-release
                val lsbReleaseFile = File(etcDir, "lsb-release")
                if (!lsbReleaseFile.exists() || !lsbReleaseFile.readText().contains("DISTRIB_CODENAME=")) {
                    lsbReleaseFile.writeText(
                        """
                        DISTRIB_ID=Ubuntu
                        DISTRIB_RELEASE=26.04
                        DISTRIB_CODENAME=$codename
                        DISTRIB_DESCRIPTION="Ubuntu 26.04 LTS"
                        """.trimIndent() + "\n"
                    )
                }

                // /etc/environment
                val envFile = File(etcDir, "environment")
                val envContent = if (envFile.exists()) envFile.readText() else ""
                if (!envContent.contains("UBUNTU_CODENAME=") || !envContent.contains("VERSION_CODENAME=")) {
                    val updated = buildString {
                        append(envContent.trimEnd())
                        if (envContent.isNotBlank()) appendLine()
                        if (!envContent.contains("PATH=")) appendLine("PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"")
                        if (!envContent.contains("LANG=")) appendLine("LANG=\"C.UTF-8\"")
                        if (!envContent.contains("UBUNTU_CODENAME=")) appendLine("UBUNTU_CODENAME=\"$codename\"")
                        if (!envContent.contains("VERSION_CODENAME=")) appendLine("VERSION_CODENAME=\"$codename\"")
                    }
                    envFile.writeText(updated)
                }

                // /etc/profile.d/00-linuxonandroid-env.sh
                val profileD = File(etcDir, "profile.d").apply { mkdirs() }
                val envProfileScript = File(profileD, "00-linuxonandroid-env.sh")
                envProfileScript.writeText(
                    """
                    #!/bin/sh
                    if [ -f /etc/os-release ]; then
                        . /etc/os-release
                        export UBUNTU_CODENAME="${'$'}{UBUNTU_CODENAME:-${'$'}VERSION_CODENAME}"
                        export VERSION_CODENAME="${'$'}{VERSION_CODENAME:-${'$'}UBUNTU_CODENAME}"
                    else
                        export UBUNTU_CODENAME="$codename"
                        export VERSION_CODENAME="$codename"
                    fi
                    """.trimIndent() + "\n"
                )
                envProfileScript.setExecutable(true, false)
                envProfileScript.setReadable(true, false)

                emitLog("Environment codename exports configured.")
                return true
            }
        }
    )

    val LATEST_ROOTFS_MIGRATION_VERSION: Int get() = ALL_MIGRATIONS.maxOfOrNull { it.targetVersionCode } ?: LEGACY_VERSION_CODE

    fun getPendingMigrations(currentVersionCode: Int, targetVersionCode: Int): List<RootfsMigrationStep> {
        return ALL_MIGRATIONS
            .filter { it.targetVersionCode > currentVersionCode && it.targetVersionCode <= targetVersionCode }
            .sortedBy { it.targetVersionCode }
    }

    fun hasRootfsImprovements(currentVersionCode: Int, targetVersionCode: Int = LATEST_ROOTFS_MIGRATION_VERSION): Boolean {
        return getPendingMigrations(currentVersionCode, targetVersionCode).isNotEmpty()
    }
}
