package com.example.detox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.detox.R
import com.example.detox.core.Constants
import com.example.detox.data.EmergencyRepository
import com.example.detox.ui.MainActivity

/**
 * MonitorForegroundService - Keeps app alive during blocking sessions
 *
 * REQUIRED by Android 8+ for background execution.
 * Shows persistent notification to user.
 *
 * Features:
 * - Persistent notification (required)
 * - Auto-restart if killed (START_STICKY)
 * - Clean shutdown handling
 * - Proper foreground service type declaration
 */
class MonitorForegroundService : Service() {

    companion object {
        private const val TAG = "MonitorForeground"
        private const val EMERGENCY_CHECK_INTERVAL_MS = 15_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val emergencyCheckRunnable = object : Runnable {
        override fun run() {
            EmergencyRepository(this@MonitorForegroundService).clearExpiredSessionIfNeeded()
            mainHandler.postDelayed(this, EMERGENCY_CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mainHandler.post(emergencyCheckRunnable)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = createNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+: Use multiple foreground service types if available
                startForeground(
                    Constants.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Use DATA_SYNC type for app monitoring
                startForeground(
                    Constants.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                // Android 8-9: startForeground without type parameter
                @Suppress("DEPRECATION")
                startForeground(Constants.NOTIFICATION_ID, notification)
            }

            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }

        // START_STICKY: restart if killed by system (within limits)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainHandler.removeCallbacks(emergencyCheckRunnable)
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    /**
     * Create notification channel (required for Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Detox Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps app blocking active"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Create persistent notification
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Detox Active")
            .setContentText("App blocking is running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
