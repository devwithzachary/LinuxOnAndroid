package com.devwithzachary.completelinuxinstaller

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.completelinuxinstaller.service.PRootForegroundService
import com.devwithzachary.completelinuxinstaller.ui.AppScreen
import com.devwithzachary.completelinuxinstaller.ui.MainAppContent
import com.devwithzachary.completelinuxinstaller.ui.MainViewModel
import com.devwithzachary.completelinuxinstaller.ui.theme.AppThemeMode
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleNavigationIntent(intent)
        setContent {
            val appThemeMode by mainViewModel.appThemeMode.collectAsStateWithLifecycle()
            val useDynamicColor by mainViewModel.useDynamicColor.collectAsStateWithLifecycle()
            val darkTheme = when (appThemeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            LinuxOnAndroidTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    MainAppContent(viewModel = mainViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val navTarget = intent?.getStringExtra(PRootForegroundService.EXTRA_NAV_TARGET)
        if (navTarget == PRootForegroundService.NAV_TARGET_TERMINAL) {
            mainViewModel.navigateToScreen(AppScreen.TERMINAL)
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshStatus()
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event != null && (event.isCtrlPressed || event.isMetaPressed) && event.isAltPressed && keyCode == android.view.KeyEvent.KEYCODE_T) {
            mainViewModel.startTerminalSession()
            mainViewModel.navigateToScreen(AppScreen.TERMINAL)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
