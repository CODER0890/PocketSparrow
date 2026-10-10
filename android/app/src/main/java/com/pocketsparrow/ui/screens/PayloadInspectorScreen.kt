package com.pocketsparrow.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.ui.theme.LocalSparrowColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.log2

@Composable
fun PayloadInspectorScreen(
    initialPayload: String = ""
) {
    val colors = LocalSparrowColors.current
    val coroutineScope = rememberCoroutineScope()

    var payloadInput by remember { mutableStateOf(initialPayload.ifEmpty { "https://g00gle-secure-verify.cfd/login" }) }
    var selectedType by remember { mutableIntStateOf(NativeBridge.CONTENT_TYPE_URL) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    fun runDeepInspection() {
        if (payloadInput.isBlank()) return
        isAnalyzing = true
        coroutineScope.launch(Dispatchers.IO) {
            val result = NativeBridge.scan(selectedType, payloadInput)
            launch(Dispatchers.Main) {
                scanResult = result
                isAnalyzing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        runDeepInspection()
    }

    val entropy = remember(payloadInput) { calculateShannonEntropy(payloadInput) }
    val homoglyphCount = remember(payloadInput) {
        payloadInput.count { it.code in 0x0400..0x04FF }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Payload Inspector",
                                color = colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Deep Entropy & Heuristic Analyzer",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Type Selector Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(NativeBridge.CONTENT_TYPE_URL, "URL", Icons.Default.Link),
                            Triple(NativeBridge.CONTENT_TYPE_SMS_TEXT, "SMS / Text", Icons.Default.Chat),
                            Triple(NativeBridge.CONTENT_TYPE_QR_PAYLOAD, "QR Code", Icons.Default.QrCode)
                        ).forEach { (typeId, label, icon) ->
                            val isSelected = selectedType == typeId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.bg,
                                border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Button(
                                    onClick = { selectedType = typeId },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) colors.primary else colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = payloadInput,
                        onValueChange = { payloadInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste payload string to inspect...", fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Phish URL" to "https://security-paypal.test/confirm",
                            "Wire Scam" to "URGENT: wire transfer $4,850 pending. Account suspended. Confirm at chase-login.test",
                            "Clean URL" to "https://github.com/torvalds/linux"
                        ).forEach { (name, payload) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.bg,
                                border = BorderStroke(1.dp, colors.cardBorder)
                            ) {
                                TextButton(
                                    onClick = {
                                        payloadInput = payload
                                        runDeepInspection()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(name, fontSize = 10.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { runDeepInspection() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing Payload...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run On-Device Deep Inspection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Shannon Entropy & Forensic Metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Shannon Entropy & Character Metrics",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "High entropy (>4.5 bits) indicates algorithmic generation, obfuscation, or homoglyphs.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Gauge row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.bg,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("SHANNON ENTROPY", fontSize = 10.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                                Text("${entropy.format(2)} bits", fontSize = 18.sp, color = if (entropy > 4.5) colors.rose else colors.emerald, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(if (entropy > 4.5) "Elevated (High Randomness)" else "Normal Distribution", fontSize = 10.sp, color = colors.textSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.bg,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("HOMOGLYPHS", fontSize = 10.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                                Text("$homoglyphCount Char", fontSize = 18.sp, color = if (homoglyphCount > 0) colors.rose else colors.emerald, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(if (homoglyphCount > 0) "Cyrillic Spoof Detected" else "Clean Latin Glyphs", fontSize = 10.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Entropy Progress Bar
                    val progress = (entropy / 8.0).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (entropy > 4.5) colors.rose else colors.emerald,
                        trackColor = colors.cardBorder
                    )
                }
            }
        }

        // 3. Verdict & XAI Attribution Breakdown
        scanResult?.let { res ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (res.threatLevel == 2) colors.rose.copy(alpha = 0.5f)
                        else if (res.threatLevel == 1) colors.amber.copy(alpha = 0.5f)
                        else colors.emerald.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Verdict & Attribution",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (res.threatLevel == 2) colors.rose.copy(alpha = 0.15f)
                                else if (res.threatLevel == 1) colors.amber.copy(alpha = 0.15f)
                                else colors.emerald.copy(alpha = 0.15f),
                                border = BorderStroke(
                                    1.dp,
                                    if (res.threatLevel == 2) colors.rose
                                    else if (res.threatLevel == 1) colors.amber
                                    else colors.emerald
                                )
                            ) {
                                Text(
                                    text = if (res.threatLevel == 2) "MALICIOUS"
                                    else if (res.threatLevel == 1) "SUSPICIOUS"
                                    else "SAFE",
                                    color = if (res.threatLevel == 2) colors.rose
                                    else if (res.threatLevel == 1) colors.amber
                                    else colors.emerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Category: ${res.category} • Tier ${res.tierTriggered} • Latency: ${res.latencyMicros}µs",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = colors.cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Explainable AI (XAI) Attribution:",
                            color = colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = res.xaiReason,
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

fun calculateShannonEntropy(input: String): Double {
    if (input.isEmpty()) return 0.0
    val freq = mutableMapOf<Char, Int>()
    for (c in input) {
        freq[c] = (freq[c] ?: 0) + 1
    }
    var entropy = 0.0
    val len = input.length.toDouble()
    for (count in freq.values) {
        val p = count / len
        entropy -= p * log2(p)
    }
    return entropy
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
