package com.devwithzachary.completelinuxinstaller.ui.screens.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutCreditsTest {

    @Test
    fun testCodeContributors_containsContributions() {
        val codeContribs = CODE_CONTRIBUTORS
        assertTrue(codeContribs.any { it.username == "bkodenkt" })

        val bkodenkt = codeContribs.first { it.username == "bkodenkt" }
        assertTrue(bkodenkt.isCodeContributor)
        assertTrue(bkodenkt.contributions.any { it.referenceNumber == 27 && it.isPr })
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/pull/27", bkodenkt.contributions.first { it.referenceNumber == 27 }.url)

        assertTrue(codeContribs.any { it.username == "sleepy-snowflake" })
        val sleepy = codeContribs.first { it.username == "sleepy-snowflake" }
        assertTrue(sleepy.isCodeContributor)
        assertTrue(sleepy.contributions.any { it.referenceNumber == 33 && it.isPr })
        assertTrue(sleepy.contributions.any { it.referenceNumber == 34 && it.isPr })
        assertTrue(sleepy.contributions.any { it.referenceNumber == 35 && it.isPr })
        assertTrue(sleepy.contributions.any { it.referenceNumber == 36 && it.isPr })
        assertTrue(sleepy.contributions.any { it.referenceNumber == 37 && it.isPr })
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/pull/33", sleepy.contributions.first { it.referenceNumber == 33 }.url)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/pull/34", sleepy.contributions.first { it.referenceNumber == 34 }.url)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/pull/35", sleepy.contributions.first { it.referenceNumber == 35 }.url)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/pull/36", sleepy.contributions.first { it.referenceNumber == 36 }.url)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/pull/37", sleepy.contributions.first { it.referenceNumber == 37 }.url)
    }

    @Test
    fun testIssueContributors_containsAllReportedIssues() {
        val expectedIssues = listOf(7, 8, 9, 10, 11, 12, 13, 14, 16, 19, 21, 25, 31)
        val allReportedIssues = ISSUE_CONTRIBUTORS.flatMap { it.contributions }.map { it.referenceNumber }

        for (issueNum in expectedIssues) {
            assertTrue("Issue #$issueNum must be present in credits", allReportedIssues.contains(issueNum))
        }
    }

    @Test
    fun testIssueContributors_authorAttributions() {
        val bkodenkt = ISSUE_CONTRIBUTORS.firstOrNull { it.username == "bkodenkt" }
        assertTrue(bkodenkt != null)
        val bkodenktIssues = bkodenkt!!.contributions.map { it.referenceNumber }
        assertTrue(bkodenktIssues.containsAll(listOf(9, 10, 11, 12, 13, 14, 16, 21, 25)))

        val hax4dazy = ISSUE_CONTRIBUTORS.firstOrNull { it.username == "hax4dazy" }
        assertTrue(hax4dazy != null)
        val haxIssues = hax4dazy!!.contributions.map { it.referenceNumber }
        assertTrue(haxIssues.containsAll(listOf(7, 19)))

        val happyYoyo = ISSUE_CONTRIBUTORS.firstOrNull { it.username == "HappyYoyo09" }
        assertTrue(happyYoyo != null)
        assertTrue(happyYoyo!!.contributions.any { it.referenceNumber == 8 })

        val rayoflight = ISSUE_CONTRIBUTORS.firstOrNull { it.username == "rayoflight3000" }
        assertTrue(rayoflight != null)
        assertTrue(rayoflight!!.contributions.any { it.referenceNumber == 31 })
    }

    @Test
    fun testContributionItem_urlGeneration() {
        val issueItem = ContributionItem(title = "Bug title", referenceNumber = 31, isPr = false)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/issues/31", issueItem.url)
        assertFalse(issueItem.isPr)

        val prItem = ContributionItem(title = "PR title", referenceNumber = 27, isPr = true)
        assertEquals("https://github.com/devwithzachary/LinuxOnAndroid/pull/27", prItem.url)
        assertTrue(prItem.isPr)
    }

    @Test
    fun testContributor_githubUrl() {
        val contrib = Contributor(
            username = "octocat",
            roleBadge = "Tester",
            contributions = emptyList()
        )
        assertEquals("https://github.com/octocat", contrib.githubUrl)
    }

    @Test
    fun testPatreonSponsors_containsSupporters() {
        val sponsors = PATREON_SPONSORS
        assertEquals(2, sponsors.size)

        val oldPcGunk = sponsors.firstOrNull { it.name == "Old PC Gunk (and stuff)" }
        assertTrue("Old PC Gunk (and stuff) must be in Patreon sponsors", oldPcGunk != null)
        assertEquals("Supporter", oldPcGunk!!.tier)

        val naserHorr = sponsors.firstOrNull { it.name == "насэр Хорр" }
        assertTrue("насэр Хорр must be in Patreon sponsors", naserHorr != null)
        assertEquals("Supporter", naserHorr!!.tier)
    }
}
