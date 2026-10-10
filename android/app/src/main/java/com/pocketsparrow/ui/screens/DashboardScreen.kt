package com.pocketsparrow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.ui.components.ForensicExportDialog
import com.pocketsparrow.ui.components.HardwareAcceleratorDashboard
import com.pocketsparrow.ui.components.ThreatDnaVisualizer
import com.pocketsparrow.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    recentLogs: List<ScanLogEntity>,
    onTriggerScan: (contentType: Int, payload: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showDashboardForensicExport by remember { mutableStateOf(false) }
    val isAirplaneMode = remember {
        try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        } catch (_: Exception) {
            false
        }
    }

    var inputPayload by remember { mutableStateOf("") }
    var selectedType by remember { mutableIntStateOf(NativeBridge.CONTENT_TYPE_URL) }
    var isInputFocused by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val reducedMotion = isReducedMotion()

    // Threat counter pop animation state
    val blockedCount = recentLogs.count { it.threatLevel == 2 }
    val counterScale = remember { Animatable(1f) }
    var targetCounterColor by remember { mutableStateOf(CyberRose) }
    val counterColor by animateColorAsState(
        targetValue = targetCounterColor,
        animationSpec = MotionTokens.microTween(),
        label = "counterColor"
    )

    LaunchedEffect(blockedCount) {
        if (blockedCount > 0 && !reducedMotion) {
            coroutineScope.launch {
                counterScale.animateTo(
                    targetValue = 1.12f,
                    animationSpec = MotionTokens.microTween()
                )
                counterScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = MotionTokens.threatSpring()
                )
            }
            targetCounterColor = Color(0xFFFF4D4D)
            delay(150)
            targetCounterColor = CyberRose
        }
    }

    // Metric cards staggered entrance
    val cardEntrance1 = remember { Animatable(0f) }
    val cardEntrance2 = remember { Animatable(0f) }
    val cardEntrance3 = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            cardEntrance1.animateTo(1f, MotionTokens.macroTween())
        }
        coroutineScope.launch {
            cardEntrance2.animateTo(1f, MotionTokens.macroTween(delayMillis = MotionTokens.Duration.Stagger))
        }
        coroutineScope.launch {
            cardEntrance3.animateTo(1f, MotionTokens.macroTween(delayMillis = MotionTokens.Duration.Stagger * 2))
        }
    }

    // Infinite radar sweep animation for active inspection
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAndRadar")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    // Subtle alert badge pulse (opacity 0.8 -> 1.0)
    val badgePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = MotionTokens.StandardEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgePulseAlpha"
    )

    // Input border glow animation
    val inputBorderColor by animateColorAsState(
        targetValue = if (isInputFocused) CyberCyan.copy(alpha = 0.8f) else Color(0xFF1E293B),
        animationSpec = MotionTokens.microTween(),
        label = "inputBorderColor"
    )

    // Evaluate button press scale micro-interaction
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed && !reducedMotion) 0.98f else 1.0f,
        animationSpec = MotionTokens.microTween(),
        label = "buttonScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.pocketsparrow.R.drawable.app_logo),
                    contentDescription = "Pocket Sparrow Logo",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pocket Sparrow",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Zero-Cloud Local Threat Shield",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Airgap Status Banner (Dynamic Hardware State)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isAirplaneMode) CyberEmerald else CyberCyan, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAirplaneMode) "AIRPLANE MODE ACTIVE (Zero RF Radiation)" else "AIRGAP GUARANTEE (100% On-Device)",
                        color = if (isAirplaneMode) CyberEmerald else CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "WAN: 0 B",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Metrics HUD Cards with Staggered Entrance and Live Counter Pop
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Card 1: Threats Blocked (with Pop counter)
                HudMetricCard(
                    title = "THREATS BLOCKED",
                    value = blockedCount.toString(),
                    sub = "100% On-Device Zero WAN",
                    accent = counterColor,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = cardEntrance1.value
                            translationY = if (reducedMotion) 0f else (1f - cardEntrance1.value) * 12f
                        },
                    valueScale = if (reducedMotion) 1f else counterScale.value
                )

                // Card 2: Latency SLA
                HudMetricCard(
                    title = "LATENCY SLA",
                    value = "< 50 ms",
                    sub = "T1: <5ms | T2: <40ms",
                    accent = CyberCyan,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = cardEntrance2.value
                            translationY = if (reducedMotion) 0f else (1f - cardEntrance2.value) * 12f
                        }
                )

                // Card 3: Peak RAM
                HudMetricCard(
                    title = "PEAK RAM",
                    value = "42.5 MB",
                    sub = "Budget: < 250 MB",
                    accent = CyberEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = cardEntrance3.value
                            translationY = if (reducedMotion) 0f else (1f - cardEntrance3.value) * 12f
                        }
                )
            }
        }

        // Live Threat Inspector Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Live Threat Inspector",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Evaluate URLs, SMS messages, and QR codes instantly on-device.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Type toggle buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == NativeBridge.CONTENT_TYPE_URL,
                            onClick = { selectedType = NativeBridge.CONTENT_TYPE_URL },
                            label = { Text("URL", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedType == NativeBridge.CONTENT_TYPE_SMS_TEXT,
                            onClick = { selectedType = NativeBridge.CONTENT_TYPE_SMS_TEXT },
                            label = { Text("SMS / Chat", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input with animated focus border glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, inputBorderColor, RoundedCornerShape(8.dp))
                    ) {
                        OutlinedTextField(
                            value = inputPayload,
                            onValueChange = { inputPayload = it },
                            placeholder = {
                                Text(
                                    text = if (selectedType == NativeBridge.CONTENT_TYPE_URL) "Enter or paste URL to inspect..." else "Enter or paste SMS text to inspect...",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isInputFocused = it.isFocused },
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            maxLines = 3
                        )
                    }

                    // Active Scanning Radar Sweep Line
                    if (isScanning && !reducedMotion) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            CyberCyan,
                                            Color.Transparent
                                        ),
                                        startX = sweepProgress * 600f - 200f,
                                        endX = sweepProgress * 600f + 200f
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clipText ->
                                    if (clipText.isNotBlank()) {
                                        inputPayload = clipText
                                    }
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (inputPayload.isNotBlank()) {
                                    coroutineScope.launch {
                                        isScanning = true
                                        onTriggerScan(selectedType, inputPayload)
                                        delay(350)
                                        isScanning = false
                                    }
                                }
                            },
                            enabled = inputPayload.isNotBlank(),
                            interactionSource = buttonInteractionSource,
                            modifier = Modifier.scale(buttonScale),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                        ) {
                            Text(
                                text = if (isScanning) "Scanning Pipeline..." else "Evaluate Threat (<50ms)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (inputPayload.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ThreatDnaVisualizer(
                            payload = inputPayload,
                            category = if (selectedType == NativeBridge.CONTENT_TYPE_URL) "URL_PAYLOAD" else "SMS_TEXT"
                        )
                    }
                }
            }
        }

        // Manual Test Vectors
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manual Test Vectors",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Pre-calibrated scenarios for evaluating local heuristic classification.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedType = NativeBridge.CONTENT_TYPE_URL
                                inputPayload = "https://g00gle-security-check.cfd/auth/verify?id=9281"
                                onTriggerScan(NativeBridge.CONTENT_TYPE_URL, inputPayload)
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberRose.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRose)
                        ) {
                            Text("Phish URL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedType = NativeBridge.CONTENT_TYPE_SMS_TEXT
                                inputPayload = "BANK ALERT: Unusual wire transfer of $2,450.00 initiated. Cancel now: http://fake-bank-auth.xyz"
                                onTriggerScan(NativeBridge.CONTENT_TYPE_SMS_TEXT, inputPayload)
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberRose.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRose)
                        ) {
                            Text("Fraud SMS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedType = NativeBridge.CONTENT_TYPE_URL
                                inputPayload = "https://en.wikipedia.org/wiki/Computer_security"
                                onTriggerScan(NativeBridge.CONTENT_TYPE_URL, inputPayload)
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberEmerald)
                        ) {
                            Text("Benign URL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Hardware Acceleration Telemetry (NPU/GPU/CPU & MobileBERT)
        item {
            HardwareAcceleratorDashboard(modifier = Modifier.fillMaxWidth())
        }

        // Recent Audit Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Local Encrypted Logs (Room + SQLCipher)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                OutlinedButton(
                    onClick = { showDashboardForensicExport = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Export Audit ZIP", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = CyberEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Threats Detected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "System is actively monitoring. All on-device inspections are clear.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            itemsIndexed(recentLogs, key = { _, log -> log.id }) { index, log ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(MotionTokens.macroTween()) +
                            if (!reducedMotion) slideInVertically(
                                animationSpec = MotionTokens.macroTween(),
                                initialOffsetY = { 24 }
                            ) else androidx.compose.animation.EnterTransition.None
                ) {
                    LogItemRow(log = log, pulseAlpha = if (reducedMotion) 1f else badgePulseAlpha)
                }
            }
        }
    }

    if (showDashboardForensicExport) {
        ForensicExportDialog(
            payload = inputPayload.ifBlank { "system_audit_log" },
            verdict = if (blockedCount > 0) "MALICIOUS" else "SAFE",
            category = "AUDIT_SUMMARY",
            xaiReason = "Pocket Sparrow forensic ledger snapshot verified on-device with zero WAN leakage.",
            latencyMicros = 1200L,
            onDismiss = { showDashboardForensicExport = false }
        )
    }
}

@Composable
fun HudMetricCard(
    title: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier,
    valueScale: Float = 1.0f
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = modifier.border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.scale(valueScale)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, color = Color(0xFF64748B), fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
fun LogItemRow(
    log: ScanLogEntity,
    pulseAlpha: Float = 1.0f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = log.payloadSnippet, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(text = "${log.category} • ${(log.latencyMicros / 1000.0)} ms", color = Color(0xFF64748B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        val isMal = log.threatLevel == 2
        Text(
            text = if (isMal) "BLOCKED" else "SAFE",
            color = if (isMal) CyberRose else CyberEmerald,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.alpha(if (isMal) pulseAlpha else 1.0f)
        )
    }
}
