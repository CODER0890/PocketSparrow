package com.pocketsparrow.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.ui.screens.ApkAuditScreen
import com.pocketsparrow.ui.screens.DashboardScreen
import com.pocketsparrow.ui.screens.QrScannerScreen
import com.pocketsparrow.ui.screens.XaiWarningDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.pocketsparrow.ui.theme.MotionTokens
import com.pocketsparrow.ui.theme.PocketSparrowTheme
import com.pocketsparrow.ui.theme.isReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            PocketSparrowTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var activeWarning by remember { mutableStateOf<ScanResult?>(null) }
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
                            activeWarning = result
                        }
                    }
                }

                val performAudit: (Array<String>) -> Unit = { perms ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = NativeBridge.audit(perms)
                        launch(Dispatchers.Main) {
                            activeWarning = result
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF030712)) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Shield, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Scanner") },
                                label = { Text("QR Defense") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Security, contentDescription = "APK Audit") },
                                label = { Text("APK Audit") }
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
                                1 -> QrScannerScreen(
                                    onTriggerQrScan = { payload ->
                                        performScan(NativeBridge.CONTENT_TYPE_QR_PAYLOAD, payload)
                                    }
                                )
                                2 -> ApkAuditScreen(
                                    onTriggerAudit = performAudit
                                )
                            }
                        }

                        // Display Explainable AI Warning Card
                        activeWarning?.let { warning ->
                            XaiWarningDialog(
                                result = warning,
                                onDismiss = { activeWarning = null }
                            )
                        }
                    }
                }
            }
        }
    }
}
