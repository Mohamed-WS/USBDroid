package com.usbdroid.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.usbdroid.MainActivity
import com.usbdroid.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Foreground service that keeps USBDroid alive while modules are active.
 * Prevents Android from killing the process during long-running USB operations.
 */
@AndroidEntryPoint
class USBDroidForegroundService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "usbdroid_active_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.usbdroid.START_FOREGROUND"
        const val ACTION_STOP = "com.usbdroid.STOP_FOREGROUND"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_MODULE_NAME = "module_name"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "USB Device"
                val moduleName = intent.getStringExtra(EXTRA_MODULE_NAME) ?: "Active Module"
                startForegroundService(deviceName, moduleName)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Active USB Module",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when a USB module is actively connected"
                setShowBadge(false)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService(deviceName: String, moduleName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, USBDroidForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("USBDroid Active")
            .setContentText("$moduleName - $deviceName")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
            Timber.i("Foreground service started for $deviceName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start foreground service")
        }
    }
}
