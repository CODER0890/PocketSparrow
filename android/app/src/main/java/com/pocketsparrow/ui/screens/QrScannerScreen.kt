package com.pocketsparrow.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.ui.theme.*

@Composable
fun QrScannerScreen(
    onTriggerQrScan: (qrPayload: String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var manualPayload by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "qrLaserSweep")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "On-Device Quishing & QR Defense",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Parses QR matrix locally, extracts URLs, and validates threat level before any browser intent is dispatched.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )

        // Viewfinder Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                .border(1.5.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Viewfinder reticle
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .border(2.dp, CyberCyan, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Laser sweep
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = ((laserProgress - 0.5f) * 130).dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CyberCyan,
                                    Color.Transparent
                                )
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scanner Active",
                    tint = CyberCyan.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Offline Zero-Cloud Scanner • Sub-5ms Pipeline",
                    color = CyberEmerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Manual Payload Input Console
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Manual QR Payload Inspection",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inspect raw decoded QR strings, deep-links, or embedded intents.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = manualPayload,
                    onValueChange = { manualPayload = it },
                    placeholder = {
                        Text(
                            text = "Enter or paste raw QR payload...",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0xFF1E293B)
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let { clipText ->
                                if (clipText.isNotBlank()) {
                                    manualPayload = clipText
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
                            if (manualPayload.isNotBlank()) {
                                onTriggerQrScan(manualPayload)
                            }
                        },
                        enabled = manualPayload.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Analyze QR Payload", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Text(
            text = "Manual Threat Vectors",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    manualPayload = "MEBKM:TITLE:Claim Reward;URL:https://chase-login.top/account/suspend;;"
                    onTriggerQrScan(manualPayload)
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, CyberRose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRose)
            ) {
                Text("Quishing QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    manualPayload = "javascript:alert('Stolen Token: ' + document.cookie)"
                    onTriggerQrScan(manualPayload)
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, CyberRose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRose)
            ) {
                Text("Script QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    manualPayload = "https://en.wikipedia.org/wiki/Information_security"
                    onTriggerQrScan(manualPayload)
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberEmerald)
            ) {
                Text("Benign QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
