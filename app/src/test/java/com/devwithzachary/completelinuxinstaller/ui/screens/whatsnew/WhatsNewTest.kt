package com.devwithzachary.completelinuxinstaller.ui.screens.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {

    @Test
    fun testShouldAutoShowWhatsNew_gatingLogic() {
        assertTrue(
            WhatsNewLogic.shouldAutoShowWhatsNew(
                isEnabled = true,
                lastSeenVersion = "",
                currentVersionKey = "1.9.0"
            )
        )
        assertTrue(
            WhatsNewLogic.shouldAutoShowWhatsNew(
                isEnabled = true,
                lastSeenVersion = "1.8.2",
                currentVersionKey = "1.9.0"
            )
        )
        assertFalse(
            WhatsNewLogic.shouldAutoShowWhatsNew(
                isEnabled = true,
                lastSeenVersion = "1.9.0",
                currentVersionKey = "1.9.0"
            )
        )
        assertFalse(
            WhatsNewLogic.shouldAutoShowWhatsNew(
                isEnabled = false,
                lastSeenVersion = "",
                currentVersionKey = "1.9.0"
            )
        )
    }

    @Test
    fun testCurrentWhatsNewRelease_isComplete() {
        val release = CURRENT_WHATS_NEW_RELEASE
        assertTrue(release.versionKey.isNotBlank())
        assertTrue(release.displayVersion.isNotBlank())
        assertTrue(release.headline.isNotBlank())
        assertTrue(release.subtitle.isNotBlank())
        assertEquals(5, release.features.size)
        release.features.forEach { feature ->
            assertTrue(feature.badge.isNotBlank())
            assertTrue(feature.title.isNotBlank())
            assertTrue(feature.summary.isNotBlank())
            assertTrue(feature.stepsToFind.isNotEmpty())
        }
    }
}
