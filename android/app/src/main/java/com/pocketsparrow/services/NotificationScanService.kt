package com.pocketsparrow.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationScanService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "pocket_sparrow_threat_alerts"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val combined = "$title $text".trim()

        if (combined.isEmpty()) return

        serviceScope.launch {
            // 100% on-device heuristic + transformer scan
            val result = NativeBridge.scan(NativeBridge.CONTENT_TYPE_SMS_TEXT, combined)

            if (result.threatLevel == 2) { // MALICIOUS
                // Save to encrypted Room DB
                val db = AppDatabase.getDatabase(applicationContext)
                val log = ScanLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    contentType = "NOTIFICATION_SMS",
                    payloadSnippet = combined.take(60),
                    threatLevel = result.threatLevel,
                    category = result.category,
                    latencyMicros = result.latencyMicros,
                    xaiReason = result.xaiReason,
                    shouldBlock = result.shouldBlock
                )
                db.scanLogDao().insertLog(log)

                // Dispatch local XAI warning alert
                postThreatWarningNotification(title, result.category, result.xaiReason)
            }
        }
    }

    private fun postThreatWarningNotification(sourceTitle: String, category: String, xaiReason: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠ Pocket Sparrow: Malicious Threat Blocked")
            .setContentText("[$category] $xaiReason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Threat intercepted from '$sourceTitle':\n$xaiReason"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Threat Interception Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate on-device alerts for phishing links and scam SMS messages."
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
