package com.pocketsparrow.services

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import java.security.MessageDigest
import java.util.regex.Pattern

data class ExtractedNotification(
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val bigText: String,
    val fullCombinedText: String,
    val urls: List<String>,
    val contentHash: String,
    val sender: String,
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationExtractor {

    private val URL_PATTERN = Pattern.compile(
        "(?:https?://|www\\.)[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+|(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(?:/[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]*)?",
        Pattern.CASE_INSENSITIVE
    )

    private val SYSTEM_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.googlequicksearchbox"
    )

    /**
     * Determines if a notification should be skipped based on ongoing/media/system flags.
     * Evaluates in <0.1ms.
     */
    fun shouldIgnoreNotification(sbn: StatusBarNotification, myPackageName: String): Boolean {
        if (sbn.packageName == myPackageName) return true
        if (SYSTEM_PACKAGES.contains(sbn.packageName)) return true

        val notification = sbn.notification ?: return true

        // Ignore ongoing / foreground service / playback notifications
        if (sbn.isOngoing) return true
        val flags = notification.flags
        if ((flags and Notification.FLAG_ONGOING_EVENT) != 0) return true
        if ((flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return true
        if ((flags and Notification.FLAG_LOCAL_ONLY) != 0 && sbn.packageName.startsWith("com.android.")) return true

        // Ignore progress / transport / navigation / call categories
        val category = notification.category
        if (category != null) {
            when (category) {
                Notification.CATEGORY_PROGRESS,
                Notification.CATEGORY_TRANSPORT,
                Notification.CATEGORY_SERVICE,
                Notification.CATEGORY_SYSTEM,
                Notification.CATEGORY_NAVIGATION,
                Notification.CATEGORY_CALL -> return true
            }
        }

        return false
    }

    /**
     * Extracts text, sender, bigText, and URLs from a notification in <2ms.
     */
    fun extract(sbn: StatusBarNotification, context: Context): ExtractedNotification? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim() ?: ""

        // Extract lines if InboxStyle
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" ") { it.toString().trim() } ?: ""

        // Extract MessagingStyle messages
        val messagingContent = StringBuilder()
        var extractedSender = title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                for (item in messages) {
                    if (item is android.os.Bundle) {
                        val senderPerson = item.getCharSequence("sender")?.toString() ?: ""
                        val msgText = item.getCharSequence("text")?.toString() ?: ""
                        if (senderPerson.isNotEmpty()) extractedSender = senderPerson
                        messagingContent.append(" ").append(msgText)
                    }
                }
            }
        }

        val combined = buildString {
            if (title.isNotEmpty()) append(title).append(". ")
            if (text.isNotEmpty()) append(text).append(" ")
            if (bigText.isNotEmpty() && bigText != text) append(bigText).append(" ")
            if (subText.isNotEmpty()) append(subText).append(" ")
            if (summaryText.isNotEmpty()) append(summaryText).append(" ")
            if (textLines.isNotEmpty()) append(textLines).append(" ")
            if (messagingContent.isNotEmpty()) append(messagingContent.toString())
        }.trim()

        if (combined.isEmpty()) return null

        val urls = extractUrls(combined)
        val hash = sha256("$title|$combined|${urls.joinToString(",")}")
        val appName = resolveAppName(sbn.packageName, context)

        return ExtractedNotification(
            notificationKey = sbn.key ?: "${sbn.packageName}_${sbn.id}",
            packageName = sbn.packageName,
            appName = appName,
            title = if (title.isNotEmpty()) title else appName,
            text = text,
            bigText = bigText,
            fullCombinedText = combined,
            urls = urls,
            contentHash = hash,
            sender = if (extractedSender.isNotEmpty()) extractedSender else appName,
            timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        )
    }

    fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            if (url.length >= 4 && !urls.contains(url)) {
                urls.add(url)
            }
        }
        return urls
    }

    fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }

    private fun resolveAppName(packageName: String, context: Context): String {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            when (packageName) {
                "com.whatsapp" -> "WhatsApp"
                "org.telegram.messenger" -> "Telegram"
                "org.thoughtcrime.securesms" -> "Signal"
                "com.google.android.apps.messaging" -> "Messages (SMS)"
                "com.google.android.gm" -> "Gmail"
                "com.instagram.android" -> "Instagram"
                "com.facebook.orca" -> "Messenger"
                "com.discord" -> "Discord"
                "com.Slack" -> "Slack"
                else -> packageName.substringAfterLast('.')
            }
        }
    }
}
