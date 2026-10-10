package com.pocketsparrow.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.services.ExtractedNotification
import com.pocketsparrow.services.LiveShieldEvent
import com.pocketsparrow.services.LiveShieldManager
import com.pocketsparrow.services.NotificationExtractor
import com.pocketsparrow.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveShieldScreen(
    onViewThreatDna: (String, String, String, Float) -> Unit = { _, _, _, _ -> }
) {
    val colors = LocalSparrowColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reducedMotion = isReducedMotion()

    // State flows from LiveShieldManager
    val isProtectionEnabled by LiveShieldManager.isProtectionEnabled.collectAsState()
    val isZeroRetentionEnabled by LiveShieldManager.isZeroRetentionEnabled.collectAsState()
    val pauseUntil by LiveShieldManager.pauseUntilTimestamp.collectAsState()
    val sensitivity by LiveShieldManager.sensitivityLevel.collectAsState()
    val monitoredApps by LiveShieldManager.monitoredApps.collectAsState()
    val liveEvents by LiveShieldManager.liveEvents.collectAsState()

    // Countdown timer for 15-min pause
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    val isPaused = remember(pauseUntil) {
        val now = System.currentTimeMillis()
        pauseUntil > now
    }

    LaunchedEffect(pauseUntil) {
        while (pauseUntil > System.currentTimeMillis()) {
            val diff = (pauseUntil - System.currentTimeMillis()) / 1000
            remainingSeconds = diff.coerceAtLeast(0L)
            delay(1000)
        }
        remainingSeconds = 0L
    }

    // Check system Notification Listener permission status
    var hasNotificationPermission by remember {
        mutableStateOf(isNotificationServiceEnabled(context))
    }

    // Refresh permission status on resume
    LaunchedEffect(Unit) {
        while (true) {
            hasNotificationPermission = isNotificationServiceEnabled(context)
            delay(2000)
        }
    }

    // Pulse animation state for Threat Detected
    val recentBlocked = liveEvents.firstOrNull { it.threatLevel > 0 }
    val radarScale = remember { Animatable(1f) }
    LaunchedEffect(recentBlocked?.id) {
        if (recentBlocked != null && !reducedMotion) {
            radarScale.animateTo(1.25f, MotionTokens.microTween())
            radarScale.animateTo(1.0f, MotionTokens.threatSpring())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Header & Live Status HUD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(radarScale.value)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPaused) colors.amber.copy(alpha = 0.15f)
                                        else if (recentBlocked != null && System.currentTimeMillis() - recentBlocked.timestamp < 10000) colors.rose.copy(alpha = 0.15f)
                                        else colors.emerald.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PauseCircle
                                    else if (recentBlocked != null && System.currentTimeMillis() - recentBlocked.timestamp < 10000) Icons.Default.Warning
                                    else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isPaused) CyberAmber
                                    else if (recentBlocked != null && System.currentTimeMillis() - recentBlocked.timestamp < 10000) CyberRose
                                    else CyberEmerald,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Live Shield",
                                    color = colors.textPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = if (isPaused) "Paused (${formatRemainingTime(remainingSeconds)})"
                                    else if (!hasNotificationPermission) "Access Required"
                                    else "Active • Intercepting <2ms",
                                    color = if (isPaused) CyberAmber
                                    else if (!hasNotificationPermission) CyberRose
                                    else CyberEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Air-gap zero WAN badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.emerald.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald)
                                )
                                Text(
                                    text = "0 BYTES WAN",
                                    color = CyberEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Buttons (Pause 15m & Zero Retention)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isPaused) LiveShieldManager.resumeNow()
                                else LiveShieldManager.pauseFor15Minutes()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) colors.amber else if (colors.isDark) Color(0xFF1E293B) else colors.surface,
                                contentColor = if (isPaused) Color.White else colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, if (isPaused) colors.amber else colors.cardBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isPaused) Color.White else colors.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPaused) "Resume Now" else "Pause 15m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPaused) Color.White else colors.textPrimary
                            )
                        }

                        Button(
                            onClick = {
                                LiveShieldManager.setZeroRetentionEnabled(!isZeroRetentionEnabled)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isZeroRetentionEnabled) colors.emerald else if (colors.isDark) Color(0xFF1E293B) else colors.surface,
                                contentColor = if (isZeroRetentionEnabled) Color.White else colors.textSecondary
                            ),
                            border = BorderStroke(1.dp, if (isZeroRetentionEnabled) colors.emerald else colors.cardBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isZeroRetentionEnabled) Color.White else colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isZeroRetentionEnabled) "Zero Retention: ON" else "Zero Retention: OFF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isZeroRetentionEnabled) Color.White else colors.textSecondary
                            )
                        }
                    }

                    if (isZeroRetentionEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🛡 Zero Retention Active: Threat scans kept ephemeral in volatile RAM. 0 bytes written to SQLite Forensic Vault.",
                            color = if (colors.isDark) Color(0xFF34D399) else colors.emerald,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 2. Onboarding / Permission Alert Card (if permission missing)
        if (!hasNotificationPermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (colors.isDark) Color(0xFF1E1B4B) else colors.primary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF4338CA) else colors.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Notification Interception Setup",
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pocket Sparrow intercepts incoming phishing links and scam text in real-time. Because Pocket Sparrow has 0 internet permission declared, notifications are scanned 100% on-device and never leave your phone.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Grant Notification Access in Settings",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 3. Sensitivity Slider & Monitored Apps
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Threat Sensitivity",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                sensitivity >= 0.9f -> "Aggressive (0.95)"
                                sensitivity >= 0.7f -> "High (0.75)"
                                sensitivity >= 0.45f -> "Medium (0.50)"
                                else -> "Low (0.25)"
                            },
                            color = colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = sensitivity,
                        onValueChange = { LiveShieldManager.setSensitivity(it) },
                        valueRange = 0.25f..0.95f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.cardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Monitored Apps & Channels",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monitoredApps.toList()) { (pkg, enabled) ->
                            val appName = when (pkg) {
                                "com.whatsapp" -> "WhatsApp"
                                "org.telegram.messenger" -> "Telegram"
                                "org.thoughtcrime.securesms" -> "Signal"
                                "com.google.android.apps.messaging" -> "SMS"
                                "com.google.android.gm" -> "Gmail"
                                "com.instagram.android" -> "Instagram"
                                "com.facebook.orca" -> "Messenger"
                                "com.discord" -> "Discord"
                                "com.Slack" -> "Slack"
                                else -> pkg.substringAfterLast('.')
                            }
                            FilterChip(
                                selected = enabled,
                                onClick = { LiveShieldManager.toggleApp(pkg, !enabled) },
                                label = { Text(appName, fontSize = 11.sp) },
                                leadingIcon = if (enabled) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (colors.isDark) Color(0xFF1E293B) else colors.primary.copy(alpha = 0.12f),
                                    selectedLabelColor = if (colors.isDark) Color.White else colors.primary,
                                    containerColor = colors.surface,
                                    labelColor = colors.textSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (enabled) colors.primary else colors.cardBorder,
                                    selectedBorderColor = colors.primary,
                                    enabled = true,
                                    selected = enabled
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Safe Simulation Test Runner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = colors.amber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Safe Threat Simulator (RFC 2606)",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Verify on-device interception using safe reserved test domains (.test) and dummy numbers (555). No live attacks or external network calls.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    val testCases = remember { getSafeTestCases() }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        testCases.forEach { testCase ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.bg,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = testCase.name,
                                                color = colors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (testCase.isThreat) Color(0x33F43F5E) else Color(0x3310B981)
                                            ) {
                                                Text(
                                                    text = if (testCase.isThreat) "THREAT" else "SAFE",
                                                    color = if (testCase.isThreat) CyberRose else CyberEmerald,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = testCase.snippet,
                                            color = colors.textSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                runSimulation(context, testCase)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (testCase.isThreat) colors.rose else colors.emerald
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Simulate", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Real-Time Interception Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Intercept Feed (${liveEvents.size})",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (liveEvents.isNotEmpty()) {
                    TextButton(onClick = { LiveShieldManager.clearEvents() }) {
                        Text("Clear", color = colors.textMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        if (liveEvents.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Awaiting Notifications",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Incoming notifications from WhatsApp, Telegram, Signal, and SMS are evaluated here in <50ms.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(liveEvents, key = { it.id }) { event ->
                LiveEventCard(
                    event = event,
                    onViewThreatDna = onViewThreatDna
                )
            }
        }
    }
}

@Composable
fun LiveEventCard(
    event: LiveShieldEvent,
    onViewThreatDna: (String, String, String, Float) -> Unit
) {
    val colors = LocalSparrowColors.current
    var expanded by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (event.threatLevel == 2) colors.rose.copy(alpha = 0.5f)
            else if (event.threatLevel == 1) colors.amber.copy(alpha = 0.5f)
            else colors.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surface
                    ) {
                        Text(
                            text = event.appName,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = timeFormat.format(Date(event.timestamp)),
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Verdict Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (event.actionTaken) {
                        "BLOCKED" -> colors.rose.copy(alpha = 0.15f)
                        "ALLOWED_ONCE" -> colors.amber.copy(alpha = 0.15f)
                        else -> colors.emerald.copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (event.actionTaken) {
                            "BLOCKED" -> colors.rose.copy(alpha = 0.6f)
                            "ALLOWED_ONCE" -> colors.amber.copy(alpha = 0.6f)
                            else -> colors.emerald.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when (event.actionTaken) {
                                        "BLOCKED" -> colors.rose
                                        "ALLOWED_ONCE" -> colors.amber
                                        else -> colors.emerald
                                    }
                                )
                        )
                        Text(
                            text = event.actionTaken,
                            color = when (event.actionTaken) {
                                "BLOCKED" -> colors.rose
                                "ALLOWED_ONCE" -> colors.amber
                                else -> colors.emerald
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.snippet,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = if (expanded) 8 else 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category: ${event.category} • Latency: ${event.latencyMicros / 1000f}ms",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (event.threatLevel > 0) {
                    TextButton(
                        onClick = {
                            onViewThreatDna(event.snippet, event.category, event.xaiReason, event.confidence)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("View Threat DNA →", color = colors.primary, fontSize = 11.sp)
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = colors.cardBorder)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explainable AI (XAI) Insight:",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = event.xaiReason,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (event.urlsFound.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extracted URLs: ${event.urlsFound.joinToString(", ")}",
                        color = colors.amber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

data class SafeTestCase(
    val name: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val snippet: String,
    val isThreat: Boolean
)

fun getSafeTestCases(): List<SafeTestCase> {
    return listOf(
        SafeTestCase(
            name = "PayPal Impersonation",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            sender = "Service Security",
            snippet = "Security alert: Unauthorized login from Moscow. Verify identity within 15 min at https://security-paypal.test",
            isThreat = true
        ),
        SafeTestCase(
            name = "Chase Wire Transfer Scam",
            packageName = "com.google.android.apps.messaging",
            appName = "SMS",
            sender = "+1-555-0199",
            snippet = "URGENT: Wire transfer of $4,850 pending. Account suspended. Confirm at https://chase-login-verify.test",
            isThreat = true
        ),
        SafeTestCase(
            name = "Elon Musk BTC Giveaway",
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            sender = "Tesla Official Promo",
            snippet = "Elon Musk is giving away 50 BTC! Send 0.1 BTC to receive 1.0 BTC immediately at https://elon-giveaway.test",
            isThreat = true
        ),
        SafeTestCase(
            name = "Casual Lunch Chat",
            packageName = "org.thoughtcrime.securesms",
            appName = "Signal",
            sender = "Alice",
            snippet = "Hey! Are we still meeting for lunch at 12:30pm today at the bistro downtown?",
            isThreat = false
        ),
        SafeTestCase(
            name = "Book Delivery Update",
            packageName = "com.google.android.gm",
            appName = "Gmail",
            sender = "Bookstore Dispatch",
            snippet = "Your book order #84920 has been delivered to your front porch. Have a wonderful weekend!",
            isThreat = false
        ),
        SafeTestCase(
            name = "Drive-By APK Download",
            packageName = "com.discord",
            appName = "Discord",
            sender = "Game Mod Bot",
            snippet = "Critical security update needed to join server: http://patch-download.test/system_update.apk",
            isThreat = true
        )
    )
}

suspend fun runSimulation(context: Context, testCase: SafeTestCase) {
    val startNanos = System.nanoTime()
    val urls = NotificationExtractor.extractUrls(testCase.snippet)
    val contentHash = NotificationExtractor.sha256("${testCase.sender}|${testCase.snippet}|${urls.joinToString(",")}")

    var result: ScanResult? = null
    if (urls.isNotEmpty()) {
        for (url in urls) {
            val urlRes = NativeBridge.scan(NativeBridge.CONTENT_TYPE_URL, url)
            if (urlRes.threatLevel > 0) {
                result = urlRes
                break
            }
        }
    }
    if (result == null || result.threatLevel == 0) {
        result = NativeBridge.scan(NativeBridge.CONTENT_TYPE_SMS_TEXT, testCase.snippet)
    }

    val elapsedMicros = (System.nanoTime() - startNanos) / 1000
    val isBlocked = result.threatLevel > 0

    // Record in LiveShieldManager
    LiveShieldManager.recordEvent(
        LiveShieldEvent(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            packageName = testCase.packageName,
            appName = testCase.appName,
            title = testCase.sender,
            snippet = testCase.snippet,
            threatLevel = result.threatLevel,
            category = result.category,
            xaiReason = result.xaiReason,
            latencyMicros = elapsedMicros,
            actionTaken = if (isBlocked) "BLOCKED" else "PASSED",
            urlsFound = urls
        )
    )

    // If Zero Retention is OFF and threat detected, persist
    if (isBlocked && !LiveShieldManager.isZeroRetentionEnabled.value) {
        val db = AppDatabase.getDatabase(context)
        db.scanLogDao().insertLog(
            ScanLogEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                contentType = "NOTIFICATION_${testCase.appName.uppercase()}",
                payloadSnippet = testCase.snippet.take(120),
                threatLevel = result.threatLevel,
                category = result.category,
                latencyMicros = elapsedMicros,
                xaiReason = result.xaiReason,
                shouldBlock = result.shouldBlock
            )
        )
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return flat.contains(pkgName)
}

fun formatRemainingTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
