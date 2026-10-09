package com.pocketsparrow.services

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class ClipboardScanService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var clipboardManager: ClipboardManager? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clipData = clipboardManager?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString() ?: ""
            if (text.isNotBlank()) {
                inspectCopiedText(text)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    private fun inspectCopiedText(text: String) {
        serviceScope.launch {
            val cType = if (text.startsWith("http://") || text.startsWith("https://")) {
                NativeBridge.CONTENT_TYPE_URL
            } else {
                NativeBridge.CONTENT_TYPE_SMS_TEXT
            }

            val result = NativeBridge.scan(cType, text)

            if (result.threatLevel == 2) { // Malicious
                val db = AppDatabase.getDatabase(applicationContext)
                val log = ScanLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    contentType = "CLIPBOARD",
                    payloadSnippet = text.take(60),
                    threatLevel = result.threatLevel,
                    category = result.category,
                    latencyMicros = result.latencyMicros,
                    xaiReason = result.xaiReason,
                    shouldBlock = result.shouldBlock
                )
                db.scanLogDao().insertLog(log)

                launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "⚠ Pocket Sparrow: Dangerous link copied! [${result.category}]",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
