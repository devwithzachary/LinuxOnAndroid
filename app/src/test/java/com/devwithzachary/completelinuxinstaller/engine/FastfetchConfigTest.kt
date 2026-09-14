package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FastfetchConfigTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testLogoContentContainsExpectedColorsAndArt() {
        val logo = FastfetchConfig.LOGO_CONTENT
        assertTrue("Logo should contain orange ANSI escape", logo.contains("[38;5;208m"))
        assertTrue("Logo should contain green ANSI escape", logo.contains("[38;5;112m"))
        assertTrue("Logo should contain yellow ANSI escape", logo.contains("[38;5;214m"))
        assertTrue("Logo should contain Tux body characters", logo.contains("@@@@@@@"))
        assertTrue("Logo should contain reset ANSI escape", logo.contains("[0m"))
    }

    @Test
    fun testConfigJsoncValidFormat() {
        val config = FastfetchConfig.CONFIG_JSONC
        assertTrue("Config must reference logo.txt", config.contains("/etc/fastfetch/logo.txt"))
        assertTrue("Config must specify file-raw type", config.contains("\"type\": \"file-raw\""))
        assertTrue("Config must specify top logo position", config.contains("\"position\": \"top\""))
        assertTrue("Config must have OS module", config.contains("\"type\": \"os\""))
        assertTrue("Config must have brand key color", config.contains("38;5;208"))
    }

    @Test
    fun testEnsureFastfetchConfigCreatesFiles() {
        val rootfsDir = tempFolder.newFolder("rootfs")
        val etcDir = File(rootfsDir, "etc/fastfetch")
        assertFalse(etcDir.exists())

        FastfetchConfig.ensureFastfetchConfig(rootfsDir)

        val logoFile = File(rootfsDir, "etc/fastfetch/logo.txt")
        val configFile = File(rootfsDir, "etc/fastfetch/config.jsonc")

        assertTrue("logo.txt must exist", logoFile.exists())
        assertTrue("config.jsonc must exist", configFile.exists())
        assertTrue("logo.txt must not be empty", logoFile.length() > 0L)
        assertTrue("config.jsonc must not be empty", configFile.length() > 0L)
    }

    @Test
    fun testEnsureFastfetchConfigPreservesExistingFiles() {
        val rootfsDir = tempFolder.newFolder("rootfs")
        val fastfetchDir = File(rootfsDir, "etc/fastfetch").apply { mkdirs() }
        val logoFile = File(fastfetchDir, "logo.txt").apply { writeText("CUSTOM_LOGO") }
        val configFile = File(fastfetchDir, "config.jsonc").apply { writeText("{\"custom\": true}") }

        FastfetchConfig.ensureFastfetchConfig(rootfsDir)

        assertTrue("Custom logo should be preserved", logoFile.readText() == "CUSTOM_LOGO")
        assertTrue("Custom config should be preserved", configFile.readText() == "{\"custom\": true}")
    }
}
