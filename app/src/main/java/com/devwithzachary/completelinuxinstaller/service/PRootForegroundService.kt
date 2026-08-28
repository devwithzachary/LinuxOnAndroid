package com.devwithzachary.completelinuxinstaller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.devwithzachary.completelinuxinstaller.MainActivity
import com.devwithzachary.completelinuxinstaller.R
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PRootForegroundService : Service() {

    companion object {
        private const val TAG = "PRootFGS"
        const val CHANNEL_ID = "linuxonandroid_fgs_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.devwithzachary.completelinuxinstaller.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.devwithzachary.completelinuxinstaller.action.STOP_SERVICE"
        const val ACTION_STOP_SESSION = "com.devwithzachary.completelinuxinstaller.action.STOP_SESSION"
        const val EXTRA_NAV_TARGET = "NAV_TARGET"
        const val NAV_TARGET_TERMINAL = "TERMINAL"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _currentStatus = MutableStateFlow(ContainerResourceStatus())
        val currentStatus: StateFlow<ContainerResourceStatus> = _currentStatus.asStateFlow()

        var onStopSessionRequested: (() -> Unit)? = null
        var isTerminalActiveProvider: (() -> Boolean)? = null
        var rootfsDirProvider: (() -> File?)? = null
        var sshPortProvider: (() -> Int)? = null

        fun start(context: Context) {
            val intent = Intent(context, PRootForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PRootForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop foreground service", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        _isServiceRunning.value = true
        startStatusMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SESSION -> {
                Log.d(TAG, "Stop session action received from notification")
                onStopSessionRequested?.invoke()
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }

            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Stop service requested")
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }

            else -> {
                val initialStatus = fetchCurrentStatus()
                _currentStatus.value = initialStatus
                val notification = buildNotification(initialStatus)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceCompat.startForeground(
                            this,
                            NOTIFICATION_ID,
                            notification,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                            } else {
                                0
                            }
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting foreground notification", e)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: Activity swiped/closed. Service will remain active in foreground.")
    }

    override fun onDestroy() {
        serviceJob.cancel()
        releaseWakeLock()
        _isServiceRunning.value = false
        super.onDestroy()
    }

    private fun stopForegroundAndSelf() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.notification_channel_name)
            val channelDesc = getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDesc
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "LinuxOnAndroid:PRootWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
                Log.d(TAG, "Partial WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Partial WakeLock released")
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wake lock", e)
        }
    }

    private fun startStatusMonitor() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                delay(3000)
                val status = fetchCurrentStatus()
                _currentStatus.value = status
                updateNotification(status)
            }
        }
    }

    private fun fetchCurrentStatus(): ContainerResourceStatus {
        val isTerminal = isTerminalActiveProvider?.invoke() ?: false
        val rootfsDir = rootfsDirProvider?.invoke()
        val sshPort = sshPortProvider?.invoke() ?: 2222

        return ServiceStatusManager.checkStatus(
            isTerminalActive = isTerminal,
            rootfsDir = rootfsDir,
            sshPort = sshPort
        )
    }

    private fun updateNotification(status: ContainerResourceStatus) {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, buildNotification(status))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }

    private fun buildNotification(status: ContainerResourceStatus): Notification {
        val openTerminalIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_TERMINAL)
        }
        val openTerminalPendingIntent = PendingIntent.getActivity(
            this,
            101,
            openTerminalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopSessionIntent = Intent(this, PRootForegroundService::class.java).apply {
            action = ACTION_STOP_SESSION
        }
        val stopSessionPendingIntent = PendingIntent.getService(
            this,
            102,
            stopSessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (status.hasActiveServices) {
            getString(R.string.notification_title_active)
        } else {
            getString(R.string.notification_title_idle)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_logo)
            .setContentTitle(title)
            .setContentText(status.buildSummaryText())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openTerminalPendingIntent)
            .addAction(
                R.drawable.ic_launcher_logo,
                getString(R.string.notification_action_open_terminal),
                openTerminalPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_logo,
                getString(R.string.notification_action_stop_session),
                stopSessionPendingIntent
            )
            .build()
    }
}
