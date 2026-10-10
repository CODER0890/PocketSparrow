package com.pocketsparrow.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.services.NotificationActionReceiver
import com.pocketsparrow.ui.screens.ApkAuditScreen
import com.pocketsparrow.ui.screens.CommunicationShieldScreen
import com.pocketsparrow.ui.screens.DashboardScreen
import com.pocketsparrow.ui.screens.EmailShieldScreen
import com.pocketsparrow.ui.screens.LiveShieldScreen
import com.pocketsparrow.ui.screens.QrScannerScreen
import com.pocketsparrow.ui.screens.XaiWarningDialog
import com.pocketsparrow.ui.theme.MotionTokens
import com.pocketsparrow.ui.theme.PocketSparrowTheme
import com.pocketsparrow.ui.theme.isReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var initialThreatPayload: String? = null
    private var initialThreatResult: ScanResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parseThreatIntent(intent)
        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            PocketSparrowTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var activeWarning by remember { mutableStateOf<ScanResult?>(initialThreatResult) }
                var activeWarningPayload by remember { mutableStateOf(initialThreatPayload ?: "") }
                var recentLogs by remember { mutableStateOf<List<ScanLogEntity>>(emptyList()) }

                // Observe local encrypted logs
                LaunchedEffect(Unit) {
                    db.scanLogDao().getAllLogs().collectLatest { logs ->
                        recentLogs = logs
                    }
                }

                val performScan: (Int, String) -> Unit = { cType, payload ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = NativeBridge.scan(cType, payload)

                        // Store in encrypted DB
                        val log = ScanLogEntity(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            contentType = if (cType == 0) "URL" else if (cType == 1) "SMS" else "QR",
                            payloadSnippet = payload.take(60),
                            threatLevel = result.threatLevel,
                            category = result.category,
                            latencyMicros = result.latencyMicros,
                            xaiReason = result.xaiReason,
                            shouldBlock = result.shouldBlock
                        )
                        db.scanLogDao().insertLog(log)

                        launch(Dispatchers.Main) {
                            activeWarningPayload = payload
                            activeWarning = result
                        }
                    }
                }

                val performAudit: (Array<String>) -> Unit = { perms ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = NativeBridge.audit(perms)
                        launch(Dispatchers.Main) {
                            activeWarningPayload = perms.joinToString("\n")
                            activeWarning = result
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF030712),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Shield, contentDescription = "Dashboard") },
                                label = { Text("Overview", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Live Shield") },
                                label = { Text("Live Shield", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Phone, contentDescription = "Comm Shield") },
                                label = { Text("Comm", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Mail, contentDescription = "Email Shield") },
                                label = { Text("Email", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Scanner") },
                                label = { Text("QR", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 5,
                                onClick = { selectedTab = 5 },
                                icon = { Icon(Icons.Default.Security, contentDescription = "APK Audit") },
                                label = { Text("APK", fontSize = 10.sp) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val reducedMotion = isReducedMotion()
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (reducedMotion) {
                                    fadeIn(animationSpec = MotionTokens.microTween()) togetherWith
                                            fadeOut(animationSpec = MotionTokens.microTween())
                                } else {
                                    (fadeIn(animationSpec = MotionTokens.macroTween()) +
                                            slideInVertically(
                                                initialOffsetY = { 20 },
                                                animationSpec = MotionTokens.macroTween()
                                            )) togetherWith fadeOut(animationSpec = MotionTokens.microTween())
                                }
                            },
                            label = "tabPageTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> DashboardScreen(
                                    recentLogs = recentLogs,
                                    onTriggerScan = performScan
                                )
                                1 -> LiveShieldScreen(
                                    onViewThreatDna = { payload, category, reason ->
                                        activeWarningPayload = payload
                                        activeWarning = ScanResult(
                                            threatLevel = 2,
                                            tierTriggered = 1,
                                            confidence = 0.98f,
                                            latencyMicros = 120,
                                            category = category,
                                            xaiReason = reason,
                                            shouldBlock = true
                                        )
                                    }
                                )
                                2 -> CommunicationShieldScreen(
                                    database = db,
                                    onBack = { selectedTab = 0 }
                                )
                                3 -> EmailShieldScreen()
                                4 -> QrScannerScreen(
                                    onTriggerQrScan = { payload ->
                                        performScan(NativeBridge.CONTENT_TYPE_QR_PAYLOAD, payload)
                                    }
                                )
                                5 -> ApkAuditScreen(
                                    onTriggerAudit = performAudit
                                )
                            }
                        }

                        // Display Explainable AI Warning Card
                        activeWarning?.let { warning ->
                            XaiWarningDialog(
                                result = warning,
                                payload = activeWarningPayload,
                                onDismiss = { activeWarning = null }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        parseThreatIntent(intent)
    }

    private fun parseThreatIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("SHOW_THREAT_DNA", false)) {
            val payload = intent.getStringExtra(NotificationActionReceiver.EXTRA_PAYLOAD) ?: ""
            val category = intent.getStringExtra(NotificationActionReceiver.EXTRA_CATEGORY) ?: "MALICIOUS"
            val reason = intent.getStringExtra(NotificationActionReceiver.EXTRA_XAI_REASON) ?: "Identified via on-device heuristics."
            initialThreatPayload = payload
            initialThreatResult = ScanResult(
                threatLevel = 2,
                tierTriggered = 1,
                confidence = 0.98f,
                latencyMicros = 120,
                category = category,
                xaiReason = reason,
                shouldBlock = true
            )
        }
    }
}
