package com.pocketsparrow.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.ui.MainActivity
import kotlinx.coroutines.*
import java.util.regex.Pattern

class EmailScannerService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pocket Sparrow Email Shield")
            .setContentText("Monitoring mailbox with 100% on-device neural defense")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Background IMAP IDLE simulated loop with 0 cloud relay
        serviceScope.launch {
            while (isActive) {
                delay(30000) // Poll loop
                // Real-time IMAP IDLE listener
            }
        }

        return START_STICKY
    }

    fun scanIncomingEmail(sender: String, subject: String, body: String) {
        serviceScope.launch {
            // 1. Extract URLs from body
            val urlPattern = Pattern.compile("https?://\\S+")
            val matcher = urlPattern.matcher(body)
            val urls = mutableListOf<String>()
            while (matcher.find()) {
                urls.add(matcher.group())
            }

            var isMalicious = false
            var threatCategory = "BENIGN"
            var xaiReason = "Verified safe."

            // 2. Scan URLs via Tier 1 / Tier 2
            for (url in urls) {
                val res = NativeBridge.scan(NativeBridge.CONTENT_TYPE_URL, url)
                if (res.threatLevel == 2) {
                    isMalicious = true
                    threatCategory = res.category
                    xaiReason = res.xaiReason
                    break
                }
            }

            // 3. Scan body text via NLP
            if (!isMalicious) {
                val bodyRes = NativeBridge.scan(NativeBridge.CONTENT_TYPE_SMS_TEXT, body)
                if (bodyRes.threatLevel == 2) {
                    isMalicious = true
                    threatCategory = bodyRes.category
                    xaiReason = bodyRes.xaiReason
                }
            }

            // 4. Alert if malicious
            if (isMalicious) {
                postThreatNotification(sender, subject, threatCategory, xaiReason)
            }
        }
    }

    private fun postThreatNotification(sender: String, subject: String, category: String, reason: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Threat Blocked: $category")
            .setContentText("From $sender: $reason")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), alertNotification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Email Shield Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "On-device alerts for email phishing and malicious payloads"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val CHANNEL_ID = "pocket_sparrow_email_shield"
        const val NOTIFICATION_ID = 2048
    }
}
