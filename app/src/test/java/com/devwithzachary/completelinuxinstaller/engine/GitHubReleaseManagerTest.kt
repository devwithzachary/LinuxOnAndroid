package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseManagerTest {

    @Test
    fun testParseSemVer() {
        assertArrayEquals(intArrayOf(1, 4, 0), GitHubReleaseManager.parseSemVer("1.4.0"))
        assertArrayEquals(intArrayOf(1, 4, 0), GitHubReleaseManager.parseSemVer("v1.4.0"))
        assertArrayEquals(intArrayOf(1, 4, 0), GitHubReleaseManager.parseSemVer("V1.4.0"))
        assertArrayEquals(intArrayOf(2, 0, 1), GitHubReleaseManager.parseSemVer("v2.0.1-rc1"))
        assertArrayEquals(intArrayOf(1, 10, 5), GitHubReleaseManager.parseSemVer("1.10.5+build.123"))
        assertArrayEquals(intArrayOf(1, 0, 0), GitHubReleaseManager.parseSemVer("1"))
        assertArrayEquals(intArrayOf(0, 0, 0), GitHubReleaseManager.parseSemVer(""))
        assertArrayEquals(intArrayOf(0, 0, 0), GitHubReleaseManager.parseSemVer("unknown"))
    }

    @Test
    fun testCompareVersions() {
        // Newer versions
        assertTrue(GitHubReleaseManager.compareVersions("1.4.1", "1.4.0") > 0)
        assertTrue(GitHubReleaseManager.compareVersions("v1.5.0", "1.4.0") > 0)
        assertTrue(GitHubReleaseManager.compareVersions("2.0.0", "1.4.0") > 0)
        assertTrue(GitHubReleaseManager.compareVersions("1.10.0", "1.9.0") > 0)
        assertTrue(GitHubReleaseManager.compareVersions("1.4.10", "1.4.2") > 0)
        assertTrue(GitHubReleaseManager.compareVersions("v1.4.1", "v1.4.0") > 0)
        assertTrue(GitHubReleaseManager.compareVersions("v1.4.10", "v1.4.2") > 0)

        // Equal versions
        assertEquals(0, GitHubReleaseManager.compareVersions("1.4.0", "1.4.0"))
        assertEquals(0, GitHubReleaseManager.compareVersions("v1.4.0", "1.4.0"))
        assertEquals(0, GitHubReleaseManager.compareVersions("1.4.0", "v1.4.0"))
        assertEquals(0, GitHubReleaseManager.compareVersions("1.4", "1.4.0"))
        assertEquals(0, GitHubReleaseManager.compareVersions("v1.4.0-rc1", "1.4.0"))

        // Older versions
        assertTrue(GitHubReleaseManager.compareVersions("1.3.9", "1.4.0") < 0)
        assertTrue(GitHubReleaseManager.compareVersions("v1.3.0", "v1.4.0") < 0)
        assertTrue(GitHubReleaseManager.compareVersions("1.4.2", "1.4.10") < 0)
        assertTrue(GitHubReleaseManager.compareVersions("0.9.9", "1.4.0") < 0)
    }

    @Test
    fun testParseReleaseJson_withApkAsset() {
        val sampleJson = """
            {
              "tag_name": "1.4.1",
              "name": "LinuxOnAndroid 1.4.1",
              "body": "## Bug Fixes\n- Fixed VNC resolution\n- Added GitHub update checker",
              "html_url": "https://github.com/devwithzachary/LinuxOnAndroid/releases/tag/1.4.1",
              "published_at": "2026-08-28T12:00:00Z",
              "assets": [
                {
                  "name": "source.zip",
                  "browser_download_url": "https://github.com/devwithzachary/LinuxOnAndroid/archive/1.4.1.zip"
                },
                {
                  "name": "app-release.apk",
                  "browser_download_url": "https://github.com/devwithzachary/LinuxOnAndroid/releases/download/1.4.1/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val release = GitHubReleaseManager.parseReleaseJson(sampleJson)
        assertEquals("1.4.1", release.tagName)
        assertEquals("LinuxOnAndroid 1.4.1", release.name)
        assertTrue(release.body.contains("Fixed VNC resolution"))
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/releases/tag/1.4.1", release.htmlUrl)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/releases/download/1.4.1/app-release.apk", release.apkDownloadUrl)
        assertEquals("2026-08-28T12:00:00Z", release.publishedAt)
    }

    @Test
    fun testParseReleaseJson_fallbackToHtmlUrl() {
        val sampleJson = """
            {
              "tag_name": "1.4.1",
              "name": "LinuxOnAndroid 1.4.1",
              "body": "No direct APK attached.",
              "html_url": "https://github.com/devwithzachary/LinuxOnAndroid/releases/tag/1.4.1",
              "published_at": "2026-08-28T12:00:00Z",
              "assets": []
            }
        """.trimIndent()

        val release = GitHubReleaseManager.parseReleaseJson(sampleJson)
        assertEquals("1.4.1", release.tagName)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/releases/tag/1.4.1", release.apkDownloadUrl)
    }
}
