package com.pocketsparrow.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationScanService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "pocket_sparrow_threat_alerts"
        const val NOTIFICATION_ID_BASE = 91000
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val startTime = System.nanoTime()

        // 1. Noise Filter & Ignore Check (<0.1ms)
        if (NotificationExtractor.shouldIgnoreNotification(sbn, packageName)) {
            return
        }

        // 2. Protection Active & 15-min Pause Check
        if (!LiveShieldManager.isProtectionEnabled.value || LiveShieldManager.isPaused()) {
            return
        }

        // 3. Per-App Monitoring Toggle Check
        if (!LiveShieldManager.isAppMonitored(sbn.packageName)) {
            return
        }

        // 4. Extraction (<2ms)
        val extracted = NotificationExtractor.extract(sbn, applicationContext) ?: return

        // 5. 5-Second Debounce Check
        val debounceKey = "${extracted.packageName}:${extracted.contentHash}"
        if (LiveShieldManager.shouldDebounce(debounceKey)) {
            return
        }

        // 6. Asynchronous Evaluation Pipeline (<50ms SLA)
        serviceScope.launch {
            val totalStartNanos = System.nanoTime()

            // Check in-memory SHA-256 verdict cache
            var scanResult = LiveShieldManager.getCachedVerdict(extracted.contentHash)

            if (scanResult == null) {
                // Perform On-Device Tier 1 & Tier 2 Detection
                // Check if embedded URLs exist
                if (extracted.urls.isNotEmpty()) {
                    for (url in extracted.urls) {
                        val urlResult = NativeBridge.scan(NativeBridge.CONTENT_TYPE_URL, url)
                        if (urlResult.threatLevel > 0) {
                            scanResult = urlResult
                            break
                        }
                    }
                }

                // If URLs clean or absent, evaluate full text payload
                if (scanResult == null || scanResult.threatLevel == 0) {
                    scanResult = NativeBridge.scan(
                        NativeBridge.CONTENT_TYPE_SMS_TEXT,
                        extracted.fullCombinedText
                    )
                }

                // Cache verdict
                LiveShieldManager.cacheVerdict(extracted.contentHash, scanResult)
            }

            val totalLatencyMicros = (System.nanoTime() - totalStartNanos) / 1000

            // Check if user previously marked this content or sender as Allow Once
            if (LiveShieldManager.isAllowedOnce(extracted.contentHash) ||
                LiveShieldManager.isAllowedOnce(extracted.sender)) {
                LiveShieldManager.recordEvent(
                    LiveShieldEvent(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        packageName = extracted.packageName,
                        appName = extracted.appName,
                        title = extracted.title,
                        snippet = extracted.fullCombinedText.take(120),
                        threatLevel = scanResult.threatLevel,
                        category = scanResult.category,
                        xaiReason = scanResult.xaiReason,
                        latencyMicros = totalLatencyMicros,
                        actionTaken = "ALLOWED_ONCE",
                        urlsFound = extracted.urls
                    )
                )
                return@launch
            }

            if (scanResult.threatLevel == 0) {
                // SAFE: Leave untouched.
                LiveShieldManager.recordEvent(
                    LiveShieldEvent(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        packageName = extracted.packageName,
                        appName = extracted.appName,
                        title = extracted.title,
                        snippet = extracted.fullCombinedText.take(120),
                        threatLevel = 0,
                        category = "SAFE",
                        xaiReason = scanResult.xaiReason,
                        latencyMicros = totalLatencyMicros,
                        actionTaken = "PASSED",
                        urlsFound = extracted.urls
                    )
                )
            } else {
                // SUSPICIOUS OR MALICIOUS:
                // 1. Cancel original notification
                try {
                    cancelNotification(sbn.key)
                } catch (_: Exception) {}

                val notificationId = NOTIFICATION_ID_BASE + (extracted.contentHash.hashCode() and 0x7FFF)

                // 2. Post replacement Pocket Sparrow warning notification
                postReplacementWarningNotification(
                    notificationId = notificationId,
                    extracted = extracted,
                    result = scanResult
                )

                // 3. Record to Forensic Vault unless Zero Retention Mode is active
                if (!LiveShieldManager.isZeroRetentionEnabled.value) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val log = ScanLogEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        contentType = "NOTIFICATION_${extracted.appName.uppercase()}",
                        payloadSnippet = extracted.fullCombinedText.take(120),
                        threatLevel = scanResult.threatLevel,
                        category = scanResult.category,
                        latencyMicros = totalLatencyMicros,
                        xaiReason = scanResult.xaiReason,
                        shouldBlock = scanResult.shouldBlock
                    )
                    db.scanLogDao().insertLog(log)
                }

                // 4. Record to live real-time stream
                LiveShieldManager.recordEvent(
                    LiveShieldEvent(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        packageName = extracted.packageName,
                        appName = extracted.appName,
                        title = extracted.title,
                        snippet = extracted.fullCombinedText.take(120),
                        threatLevel = scanResult.threatLevel,
                        category = scanResult.category,
                        xaiReason = scanResult.xaiReason,
                        latencyMicros = totalLatencyMicros,
                        actionTaken = "BLOCKED",
                        urlsFound = extracted.urls
                    )
                )
            }
        }
    }

    private fun postReplacementWarningNotification(
        notificationId: Int,
        extracted: ExtractedNotification,
        result: ScanResult
    ) {
        val context = applicationContext

        // Action 1: View Threat DNA
        val dnaIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_VIEW_THREAT_DNA
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_PAYLOAD, extracted.fullCombinedText)
            putExtra(NotificationActionReceiver.EXTRA_CATEGORY, result.category)
            putExtra(NotificationActionReceiver.EXTRA_XAI_REASON, result.xaiReason)
        }
        val dnaPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            dnaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Block Sender
        val blockIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_BLOCK_SENDER
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_SENDER, extracted.sender)
        }
        val blockPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            blockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Allow Once
        val allowIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ALLOW_ONCE
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_CONTENT_HASH, extracted.contentHash)
            putExtra(NotificationActionReceiver.EXTRA_SENDER, extracted.sender)
        }
        val allowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val warningTitle = "⚠ Pocket Sparrow: ${extracted.appName} Threat Quarantined"
        val warningBody = "[${result.category}] ${result.xaiReason}"
        val expandedBody = "Source: ${extracted.sender} (${extracted.appName})\n" +
                "Detected: ${result.category}\n" +
                "Analysis: ${result.xaiReason}\n\n" +
                "Quarantined Snippet:\n\"${extracted.fullCombinedText.take(160)}\""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(warningTitle)
            .setContentText(warningBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(dnaPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Threat DNA", dnaPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Block Sender", blockPendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Allow Once", allowPendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Threat Interception Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate on-device alerts when phishing links or scams are intercepted."
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
