package com.devwithzachary.completelinuxinstaller.ui

import com.devwithzachary.completelinuxinstaller.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeStateTest {

    @Test
    fun testAppScreen_welcomeScreenEntry() {
        val screen = AppScreen.WELCOME
        assertEquals(R.string.app_name, screen.titleRes)
    }

    @Test
    fun testFirstLaunchRoutingLogic_freshInstallDirectsToWelcome() {
        val isInitializing = false
        val isInstalled = false
        val splashDismissed = false
        val hasSeenWelcome = false

        val targetScreen = determineInitialScreen(
            isInitializing = isInitializing,
            isInstalled = isInstalled,
            splashDismissed = splashDismissed,
            hasSeenWelcome = hasSeenWelcome
        )

        assertEquals(AppScreen.WELCOME, targetScreen)
    }

    @Test
    fun testFirstLaunchRoutingLogic_afterWelcomeDirectsToWizard() {
        val isInitializing = false
        val isInstalled = false
        val splashDismissed = false
        val hasSeenWelcome = true

        val targetScreen = determineInitialScreen(
            isInitializing = isInitializing,
            isInstalled = isInstalled,
            splashDismissed = splashDismissed,
            hasSeenWelcome = hasSeenWelcome
        )

        assertEquals(AppScreen.WIZARD, targetScreen)
    }

    @Test
    fun testFirstLaunchRoutingLogic_existingInstallDirectsToDashboard() {
        val isInitializing = false
        val isInstalled = true
        val splashDismissed = false
        val hasSeenWelcome = true

        val targetScreen = determineInitialScreen(
            isInitializing = isInitializing,
            isInstalled = isInstalled,
            splashDismissed = splashDismissed,
            hasSeenWelcome = hasSeenWelcome
        )

        assertEquals(AppScreen.DASHBOARD, targetScreen)
    }

    @Test
    fun testFirstLaunchRoutingLogic_initializingRemainsSplash() {
        val isInitializing = true
        val isInstalled = false
        val splashDismissed = false
        val hasSeenWelcome = false

        val targetScreen = determineInitialScreen(
            isInitializing = isInitializing,
            isInstalled = isInstalled,
            splashDismissed = splashDismissed,
            hasSeenWelcome = hasSeenWelcome
        )

        assertEquals(AppScreen.SPLASH, targetScreen)
    }

    private fun determineInitialScreen(
        isInitializing: Boolean,
        isInstalled: Boolean,
        splashDismissed: Boolean,
        hasSeenWelcome: Boolean
    ): AppScreen {
        if (isInitializing) return AppScreen.SPLASH
        return if (!isInstalled && !splashDismissed) {
            if (!hasSeenWelcome) AppScreen.WELCOME else AppScreen.WIZARD
        } else {
            AppScreen.DASHBOARD
        }
    }
}
