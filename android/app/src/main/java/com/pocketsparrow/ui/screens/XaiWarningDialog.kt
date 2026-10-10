package com.pocketsparrow.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.ui.components.ForensicExportDialog
import com.pocketsparrow.ui.components.ThreatDnaVisualizer
import com.pocketsparrow.ui.theme.*

@Composable
fun XaiWarningDialog(
    result: ScanResult,
    payload: String = "",
    onDismiss: () -> Unit,
    onProceedAnyway: (() -> Unit)? = null
) {
    val isMalicious = result.threatLevel == 2
    val headerColor = if (isMalicious) CyberRose else CyberAmber
    val confPct = (result.confidence * 100).toInt()
    val reducedMotion = isReducedMotion()
    var showForensicExport by remember { mutableStateOf(false) }

    // Threat attention shake on entrance for malicious threats
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(result) {
        if (isMalicious && !reducedMotion) {
            shakeOffset.animateTo(-6f, tween(50, easing = MotionTokens.StandardEasing))
            shakeOffset.animateTo(6f, tween(60, easing = MotionTokens.StandardEasing))
            shakeOffset.animateTo(-4f, tween(60, easing = MotionTokens.StandardEasing))
            shakeOffset.animateTo(4f, tween(60, easing = MotionTokens.StandardEasing))
            shakeOffset.animateTo(0f, MotionTokens.threatSpring())
        }
    }

    // Button micro-interaction
    val buttonInteraction = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed && !reducedMotion) 0.98f else 1.0f,
        animationSpec = MotionTokens.microTween(),
        label = "btnScale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    translationX = shakeOffset.value
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isMalicious) "⚠ THREAT BLOCKED" else "⚠ SUSPICIOUS WARNING",
                        color = headerColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$confPct% CONFIDENCE",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Category: ${result.category}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // XAI Reason Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "EXPLAINABLE AI DIAGNOSIS:",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.xaiReason,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Threat DNA Attention Heatmap
                if (payload.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ThreatDnaVisualizer(
                        payload = payload,
                        category = result.category
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Latency and Engine metadata
                Text(
                    text = "Engine: ${if (result.tierTriggered == 1) "Tier 1 Heuristics" else "Tier 2 INT8 MobileBERT"} • Latency: ${result.latencyMicros / 1000.0} ms",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showForensicExport = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                    ) {
                        Text("Export Audit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onProceedAnyway != null) {
                            TextButton(onClick = onProceedAnyway) {
                                Text("Proceed (Unsafe)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = onDismiss,
                            interactionSource = buttonInteraction,
                            modifier = Modifier.scale(buttonScale),
                            colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Block & Dismiss", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (showForensicExport) {
            ForensicExportDialog(
                payload = payload.ifBlank { result.category },
                verdict = if (isMalicious) "MALICIOUS" else "SUSPICIOUS",
                category = result.category,
                xaiReason = result.xaiReason,
                latencyMicros = result.latencyMicros,
                onDismiss = { showForensicExport = false }
            )
        }
    }
}
