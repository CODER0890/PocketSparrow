package com.pocketsparrow.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.SpamSenderEntity
import com.pocketsparrow.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BLOCK_SENDER = "com.pocketsparrow.ACTION_BLOCK_SENDER"
        const val ACTION_ALLOW_ONCE = "com.pocketsparrow.ACTION_ALLOW_ONCE"
        const val ACTION_VIEW_THREAT_DNA = "com.pocketsparrow.ACTION_VIEW_THREAT_DNA"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_CONTENT_HASH = "extra_content_hash"
        const val EXTRA_PAYLOAD = "extra_payload"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_XAI_REASON = "extra_xai_reason"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: "Unknown"
        val contentHash = intent.getStringExtra(EXTRA_CONTENT_HASH) ?: ""

        when (intent.action) {
            ACTION_BLOCK_SENDER -> {
                // Cancel notification
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }

                // Add sender to local encrypted spam database
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val senderHash = NotificationExtractor.sha256("SENDER_SALT_$sender")
                    val masked = if (sender.length > 4) {
                        "${sender.take(2)}***${sender.takeLast(2)}"
                    } else {
                        "***"
                    }
                    val entity = SpamSenderEntity(
                        hash = senderHash,
                        originalMasked = masked,
                        firstSeen = System.currentTimeMillis(),
                        lastSeen = System.currentTimeMillis(),
                        reportCount = 1,
                        category = "USER_BLOCKED_NOTIFICATION"
                    )
                    db.spamDao().insertSender(entity)
                }
            }

            ACTION_ALLOW_ONCE -> {
                // Cancel notification
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }

                // Mark in temporary in-memory allowlist
                if (contentHash.isNotEmpty()) {
                    LiveShieldManager.addAllowOnce(contentHash)
                }
                if (sender.isNotEmpty()) {
                    LiveShieldManager.addAllowOnce(sender)
                }
            }

            ACTION_VIEW_THREAT_DNA -> {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }

                val payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: ""
                val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "MALICIOUS"
                val xaiReason = intent.getStringExtra(EXTRA_XAI_REASON) ?: ""

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("SHOW_THREAT_DNA", true)
                    putExtra(EXTRA_PAYLOAD, payload)
                    putExtra(EXTRA_CATEGORY, category)
                    putExtra(EXTRA_XAI_REASON, xaiReason)
                }
                context.startActivity(mainIntent)
            }
        }
    }
}
