package com.pocketsparrow.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.SpamNumberEntity
import com.pocketsparrow.data.UserReportEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

class CallerIdOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        val isSpam = intent.getBooleanExtra("IS_SPAM", false)
        val shouldBlock = intent.getBooleanExtra("SHOULD_BLOCK", false)
        val category = intent.getStringExtra("CATEGORY") ?: "NORMAL"
        val xaiReason = intent.getStringExtra("XAI_REASON") ?: ""
        val stirShaken = intent.getStringExtra("STIR_SHAKEN") ?: "NOT_AVAILABLE"
        val latencyMs = intent.getLongExtra("LATENCY_MS", 0L)

        showOverlay(phoneNumber, isSpam, shouldBlock, category, xaiReason, stirShaken, latencyMs)
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val channelId = "sparrow_caller_id"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pocket Sparrow Caller ID",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "On-device active caller ID screening"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pocket Sparrow Caller ID")
            .setContentText("Screening incoming telephony with zero cloud telemetry")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun showOverlay(
        phoneNumber: String,
        isSpam: Boolean,
        shouldBlock: Boolean,
        category: String,
        xaiReason: String,
        stirShaken: String,
        latencyMs: Long
    ) {
        if (!Settings.canDrawOverlays(this)) {
            // Permission not granted, dismiss gracefully
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        // Programmatically build overlay view container for zero-dependency portability
        val view = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
            setBackgroundColor(
                if (shouldBlock || isSpam) Color.parseColor("#1C1917") // Deep dark stone
                else Color.parseColor("#09090B") // Dark zinc
            )
            elevation = 16f
        }

        // Title and Shield Badge
        val titleView = TextView(this).apply {
            text = if (shouldBlock) "BLOCKED SPAM / FRAUD CALL"
            else if (isSpam) "SUSPECTED SPAM CALL"
            else "VERIFIED CALLER"
            textSize = 14f
            setTextColor(
                if (shouldBlock || isSpam) Color.parseColor("#FB7185") // Rose alert
                else Color.parseColor("#34D399") // Emerald verified
            )
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        view.addView(titleView)

        // Phone Number
        val numberView = TextView(this).apply {
            text = phoneNumber
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 12, 0, 8)
        }
        view.addView(numberView)

        // STIR / SHAKEN Badge
        val stirView = TextView(this).apply {
            text = "STIR/SHAKEN: $stirShaken • Verified On-Device ($latencyMs ms)"
            textSize = 11f
            setTextColor(Color.parseColor("#A1A1AA"))
        }
        view.addView(stirView)

        // XAI Reason
        if (xaiReason.isNotBlank()) {
            val reasonView = TextView(this).apply {
                text = xaiReason
                textSize = 12f
                setTextColor(Color.parseColor("#E4E4E7"))
                setPadding(0, 12, 0, 16)
            }
            view.addView(reasonView)
        }

        // Action Buttons Row
        val buttonRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val dismissBtn = Button(this).apply {
            text = "Dismiss"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#27272A"))
            setOnClickListener { removeOverlayAndStop() }
        }
        buttonRow.addView(dismissBtn)

        val reportBtn = Button(this).apply {
            text = "Report & Block"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E11D48")) // Rose-600
            setOnClickListener {
                reportNumberLocally(phoneNumber, category)
                removeOverlayAndStop()
            }
        }
        val btnParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 24
        }
        buttonRow.addView(reportBtn, btnParams)

        view.addView(buttonRow)

        overlayView = view
        try {
            windowManager?.addView(overlayView, params)
        } catch (_: Exception) {}
    }

    private fun reportNumberLocally(phoneNumber: String, category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest("sparrow_salt_$phoneNumber".toByteArray()).joinToString("") { "%02x".format(it) }
            val now = System.currentTimeMillis()

            val masked = if (phoneNumber.length > 6) {
                "${phoneNumber.take(3)}****${phoneNumber.takeLast(3)}"
            } else "***"

            db.spamDao().insertNumber(
                SpamNumberEntity(
                    hash = hash,
                    originalMasked = masked,
                    firstSeen = now,
                    lastSeen = now,
                    reportCount = 1,
                    category = category.ifBlank { "USER_FLAGGED_SPAM" }
                )
            )

            db.spamDao().insertReport(
                UserReportEntity(
                    id = "rep_$now",
                    targetHash = hash,
                    targetMasked = masked,
                    category = category.ifBlank { "USER_FLAGGED_SPAM" },
                    reason = "Reported directly from Caller ID overlay",
                    timestamp = now
                )
            )
        }
    }

    private fun removeOverlayAndStop() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
        }
    }
}
