package com.devwithzachary.completelinuxinstaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.devwithzachary.completelinuxinstaller.service.PRootForegroundService
import com.devwithzachary.completelinuxinstaller.theme.LinuxOnAndroidTheme
import com.devwithzachary.completelinuxinstaller.ui.AppScreen
import com.devwithzachary.completelinuxinstaller.ui.MainAppContent
import com.devwithzachary.completelinuxinstaller.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && mainViewModel.isKeepAliveEnabled.value && mainViewModel.isSessionRunning.value) {
            PRootForegroundService.start(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleNavigationIntent(intent)
        setContent {
            LinuxOnAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
}
