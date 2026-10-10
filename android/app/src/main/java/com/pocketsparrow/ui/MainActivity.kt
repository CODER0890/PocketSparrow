package com.pocketsparrow.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.services.NotificationActionReceiver
import com.pocketsparrow.ui.screens.*
import com.pocketsparrow.ui.theme.LocalSparrowColors
import com.pocketsparrow.ui.theme.MotionTokens
import com.pocketsparrow.ui.theme.PocketSparrowTheme
import com.pocketsparrow.ui.theme.isReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class ModuleItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null
)

class MainActivity : ComponentActivity() {

    private var initialThreatPayload: String? = null
    private var initialThreatResult: ScanResult? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parseThreatIntent(intent)
        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            // Light Theme by default as requested: "make it light theme"
            var isDarkTheme by remember { mutableStateOf(false) }

            PocketSparrowTheme(isDarkTheme = isDarkTheme) {
                val colors = LocalSparrowColors.current
                val reducedMotion = isReducedMotion()

                val view = androidx.compose.ui.platform.LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = this@MainActivity.window
                        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !isDarkTheme
                        insetsController.isAppearanceLightNavigationBars = !isDarkTheme
                    }
                }

                var selectedTab by remember { mutableIntStateOf(0) }
                var showModulesHub by remember { mutableStateOf(false) }
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

                val allModules = remember {
                    listOf(
                        ModuleItem(0, "Overview", "Threat metrics & HUD", Icons.Default.Shield),
                        ModuleItem(1, "Live Shield", "Real-time interceptor", Icons.Default.NotificationsActive, "LIVE"),
                        ModuleItem(2, "Network Interceptor", "Live graph & 0 WAN", Icons.Default.AllInclusive, "GRAPH"),
                        ModuleItem(3, "Comm Shield", "Calls, SMS, spam block", Icons.Default.Phone),
                        ModuleItem(4, "Email Shield", "IMAP & tracking pixels", Icons.Default.Mail),
                        ModuleItem(5, "Payload Inspector", "Shannon entropy & DGA", Icons.Default.Search),
                        ModuleItem(6, "Process Monitor", "Background task auditor", Icons.Default.Memory),
                        ModuleItem(7, "Forensic Vault", "AES-256 logs & export", Icons.Default.FolderSpecial),
                        ModuleItem(8, "Hardware Accel", "NPU & GPU benchmarks", Icons.Default.Bolt),
                        ModuleItem(9, "QR Defense", "Offline CameraX quishing", Icons.Default.QrCodeScanner),
                        ModuleItem(10, "APK Audit", "Rogue permission combo", Icons.Default.Security),
                        ModuleItem(11, "Engine Settings", "Thresholds & metadata", Icons.Default.Tune)
                    )
                }

                Scaffold(
                    topBar = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.cardBg,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = com.pocketsparrow.R.drawable.app_logo),
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Pocket Sparrow",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = allModules.find { it.id == selectedTab }?.title ?: "Threat Defense",
                                            fontSize = 11.sp,
                                            color = colors.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Airgap Badge
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.emerald.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.emerald)
                                            )
                                            Text(
                                                text = "0 WAN B",
                                                color = colors.emerald,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Light / Dark Theme Toggle
                                    val rotation by animateFloatAsState(
                                        targetValue = if (isDarkTheme) 180f else 0f,
                                        animationSpec = MotionTokens.macroTween(),
                                        label = "themeRotation"
                                    )
                                    IconButton(
                                        onClick = { isDarkTheme = !isDarkTheme },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Theme",
                                            tint = if (isDarkTheme) Color(0xFFFBBF24) else colors.textPrimary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .rotate(if (reducedMotion) 0f else rotation)
                                        )
                                    }

                                    // All Modules Hub Button
                                    IconButton(
                                        onClick = { showModulesHub = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "All Modules Hub",
                                            tint = colors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = colors.cardBg,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Shield, contentDescription = "Overview") },
                                label = { Text("Overview", fontSize = 10.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Live Shield") },
                                label = { Text("Live Shield", fontSize = 10.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.AllInclusive, contentDescription = "Network") },
                                label = { Text("Network", fontSize = 10.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Phone, contentDescription = "Comm Shield") },
                                label = { Text("Comm", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = showModulesHub || selectedTab > 3,
                                onClick = { showModulesHub = true },
                                icon = { Icon(Icons.Default.GridView, contentDescription = "All Modules") },
                                label = { Text("Modules", fontSize = 10.sp) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.bg)
                            .padding(innerPadding)
                    ) {
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
                                    onViewThreatDna = { payload, category, reason, confidence ->
                                        activeWarningPayload = payload
                                        activeWarning = ScanResult(
                                            threatLevel = 2,
                                            tierTriggered = if (reason.contains("Tier 1")) 1 else 2,
                                            confidence = confidence,
                                            latencyMicros = 120,
                                            category = category,
                                            xaiReason = reason,
                                            shouldBlock = true
                                        )
                                    }
                                )
                                2 -> NetworkInterceptorScreen()
                                3 -> CommunicationShieldScreen(
                                    database = db,
                                    onBack = { selectedTab = 0 }
                                )
                                4 -> EmailShieldScreen()
                                5 -> PayloadInspectorScreen(
                                    initialPayload = activeWarningPayload
                                )
                                6 -> ProcessMonitorScreen()
                                7 -> ForensicVaultScreen(
                                    database = db
                                )
                                8 -> HardwareScreen()
                                9 -> QrScannerScreen(
                                    onTriggerQrScan = { payload ->
                                        performScan(NativeBridge.CONTENT_TYPE_QR_PAYLOAD, payload)
                                    }
                                )
                                10 -> ApkAuditScreen(
                                    onTriggerAudit = performAudit
                                )
                                11 -> SettingsScreen(
                                    database = db
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

                // All Modules Hub Bottom Sheet (Full Cross-Platform Parity Hub)
                if (showModulesHub) {
                    ModalBottomSheet(
                        onDismissRequest = { showModulesHub = false },
                        containerColor = colors.cardBg,
                        tonalElevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "All Defense Modules",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "12 on-device modules with full desktop parity",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "12 ACTIVE",
                                        color = colors.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allModules) { mod ->
                                    val isSelected = selectedTab == mod.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.bg,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) colors.primary else colors.cardBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedTab = mod.id
                                                showModulesHub = false
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = mod.icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) colors.primary else colors.textPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                mod.badge?.let { b ->
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = colors.emerald.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = b,
                                                            color = colors.emerald,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = mod.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colors.primary else colors.textPrimary
                                            )
                                            Text(
                                                text = mod.subtitle,
                                                fontSize = 10.sp,
                                                color = colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
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
