package com.example.guardianassist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.guardianassist.appctrl.SessionManager

class HourlyCheckService : Service() {

    private val handler = Handler()
    private val totalDuration = 3600 // 1 hour in seconds
    private var elapsedSeconds = 0
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val CHANNEL_ID = "HourlyCheckServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        createNotificationChannel()
        startForeground(1, getNotification("Hourly check is running..."))

        // Schedule hourly checks
        handler.postDelayed(object : Runnable {
            override fun run() {
                elapsedSeconds++
                if (elapsedSeconds >= totalDuration) {
                    sendHourlyCheckReminder()
                    elapsedSeconds = 0
                }
                handler.postDelayed(this, 1000) // Repeat every second
            }
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun sendHourlyCheckReminder() {
        val realName = sessionManager.fetchRealName() ?: "User"
        val notificationMessage = "It's time to perform your hourly check, $realName."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Hourly Check Reminder")
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hourly Check Service"
            val descriptionText = "Runs hourly checks in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(content: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Guardian Assist")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
