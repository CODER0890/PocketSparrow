package com.pocketsparrow.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.QuarantinedMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    private val urlPattern = Pattern.compile(
        "(https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?)",
        Pattern.CASE_INSENSITIVE
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: "Unknown"
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.displayMessageBody ?: "")
        }
        val fullBody = bodyBuilder.toString()
        val timestamp = messages[0].timestampMillis

        // Asynchronously scan SMS message on-device
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scanResult = NativeBridge.scan(NativeBridge.CONTENT_TYPE_SMS_TEXT, fullBody)

                // Inspect any embedded URLs
                val matcher = urlPattern.matcher(fullBody)
                var hasMaliciousUrl = false
                var flaggedUrlReason = ""

                while (matcher.find()) {
                    val url = matcher.group(1) ?: continue
                    val urlResult = NativeBridge.scan(NativeBridge.CONTENT_TYPE_URL, url)
                    if (urlResult.shouldBlock || urlResult.threatLevel > 0) {
                        hasMaliciousUrl = true
                        flaggedUrlReason = urlResult.xaiReason
                        break
                    }
                }

                val shouldQuarantine = scanResult.shouldBlock || hasMaliciousUrl ||
                        fullBody.contains("wire transfer", ignoreCase = true) ||
                        fullBody.contains("account suspended", ignoreCase = true) ||
                        fullBody.contains("usps", ignoreCase = true) && fullBody.contains("http", ignoreCase = true)

                if (shouldQuarantine) {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val id = "sms_${System.currentTimeMillis()}"
                    db.spamDao().insertQuarantined(
                        QuarantinedMessageEntity(
                            id = id,
                            sender = sender,
                            body = fullBody,
                            timestamp = timestamp,
                            category = if (hasMaliciousUrl) "SMISHING_URL" else scanResult.category,
                            isRead = false
                        )
                    )

                    // Post local security warning notification
                    postQuarantineNotification(
                        context,
                        sender,
                        if (hasMaliciousUrl) flaggedUrlReason else scanResult.xaiReason
                    )

                    // Abort broadcast to prevent spam from polluting standard messaging inbox
                    try {
                        abortBroadcast()
                    } catch (_: Exception) {}
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postQuarantineNotification(context: Context, sender: String, reason: String) {
        val channelId = "sparrow_spam_alerts"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pocket Sparrow Spam Defense",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for quarantined phishing & smishing messages"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("SMS Phishing Blocked: $sender")
            .setContentText("Message moved silently to Pocket Sparrow Spam. $reason")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
