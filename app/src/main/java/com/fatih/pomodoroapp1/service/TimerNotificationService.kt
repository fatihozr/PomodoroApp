package com.fatih.pomodoroapp1.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fatih.pomodoroapp1.MainActivity
import com.fatih.pomodoroapp1.R

class TimerNotificationService : Service() {

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_RESET = "ACTION_RESET"

        const val EXTRA_TIME_REMAINING = "EXTRA_TIME_REMAINING"
        const val EXTRA_IS_PAUSED = "EXTRA_IS_PAUSED"

        fun startService(
            context: Context,
            timeRemaining: Int,
            isPaused: Boolean
        ) {
            val intent = Intent(context, TimerNotificationService::class.java).apply {
                putExtra(EXTRA_TIME_REMAINING, timeRemaining)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, TimerNotificationService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timeRemaining = intent?.getIntExtra(EXTRA_TIME_REMAINING, 0) ?: 0
        val isPaused = intent?.getBooleanExtra(EXTRA_IS_PAUSED, true) ?: true

        val notification = createNotification(timeRemaining, isPaused)

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            println("❌ Notification hatası: ${e.message}")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus To-Do",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pomodoro Timer"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        timeRemaining: Int,
        isPaused: Boolean
    ): Notification {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        // Ana aktiviteyi aç
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause action
        val playPauseIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reset action (sadece pause durumunda)
        val resetIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_RESET
        }
        val resetPendingIntent = PendingIntent.getBroadcast(
            this, 2, resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Timer ikonu
            .setContentTitle("Focus To-Do")
            .setContentText(timeText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(timeText))
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSound(null)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        // Butonlar
        if (isPaused) {
            // Duraklatıldı: Play ve Reset butonları
            builder.addAction(
                android.R.drawable.ic_media_play,
                "▶",
                playPausePendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_delete,
                "✕",
                resetPendingIntent
            )
        } else {
            // Çalışıyor: Sadece Pause butonu
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "||",
                playPausePendingIntent
            )
        }

        return builder.build()
    }
}