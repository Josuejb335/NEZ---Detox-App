package com.example.detox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.detox.R
import com.example.detox.ui.MainActivity

/**
 * Foreground Service - keeps the app running in background
 *
 * PURPOSE:
 * - Prevents system from killing the app
 * - Shows persistent notification to user
 * - Can trigger monitoring tasks periodically
 *
 * START THIS SERVICE:
 * - From MainActivity on app launch
 * - From BootReceiver on device restart (if implemented)
 * - From AccessibilityService to ensure persistence
 *
 * EXTEND THIS:
 * - Add periodic checks for app usage
 * - Add notification actions (pause blocking, etc.)
 * - Add quick settings tile
 */
class AppBlockService : Service() {

    companion object {
        private const val TAG = "AppBlockService"
        private const val CHANNEL_ID = "detox_service_channel"
        private const val NOTIFICATION_ID = 1

        // Actions for service control
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Start as foreground service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                Log.d(TAG, "Service started in foreground")
            }
        }

        // Return START_STICKY to restart if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        // EXTEND: Restart service if needed or notify user
    }

    /**
     * Create notification channel (required for Android 8+)
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Detox App Blocker",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps app blocker running"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create the persistent notification
     */
    private fun createNotification(): Notification {
        // Intent to open MainActivity when notification tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Detox App Blocker")
            .setContentText("App blocking is active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}