package com.example.guardianassist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.guardianassist.appctrl.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class HourlyCheckService : Service() {

    private lateinit var sessionManager: SessionManager
    private val handler = Handler(Looper.getMainLooper())
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val CHANNEL_ID = "HourlyCheckServiceChannel"
        private const val NOTIF_ID  = 1
        private const val REMINDER_ID = 2
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
    }

    private var nextDueMillis: Long = 0L

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)

        // 1) Determine baseTime: last check or book-on
        val baseTimeStr = sessionManager.fetchLastHourlyCheckTime()
            ?: sessionManager.fetchBookOnTime().also {
                if (it == null) {
                    stopSelf()
                    return
                }
            }
        val baseMillis = try {
            dateFmt.parse(baseTimeStr!!)?.time
        } catch (_: Exception) {
            null
        } ?: run {
            stopSelf()
            return
        }

        // 2) Compute next due
        nextDueMillis = baseMillis + ONE_HOUR_MS

        createNotificationChannel()
        startForeground(NOTIF_ID, buildForegroundNotification())

        // 3) Schedule first reminder
        scheduleReminder()
    }

    private fun scheduleReminder() {
        val delay = nextDueMillis - System.currentTimeMillis()
        handler.postDelayed(reminderRunnable, if (delay > 0) delay else 0)
    }

    private val reminderRunnable = object : Runnable {
        override fun run() {
            sendHourlyCheckReminder()

            // Update last check time to now
            val nowStr = dateFmt.format(Date())
            sessionManager.saveLastHourlyCheckTime(nowStr)

            // Schedule next hour
            nextDueMillis = System.currentTimeMillis() + ONE_HOUR_MS
            handler.postDelayed(this, ONE_HOUR_MS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(reminderRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendHourlyCheckReminder() {
        val realName = sessionManager.fetchRealName() ?: "User"
        val message  = "Time for your hourly check, $realName."

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Hourly Check Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(REMINDER_ID, notif)
    }

    private fun buildForegroundNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Guardian Assist")
            .setContentText("Hourly check service running")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hourly Check Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Sends hourly check reminders" }

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
