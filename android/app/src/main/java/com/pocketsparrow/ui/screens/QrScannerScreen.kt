package com.pocketsparrow.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pocketsparrow.scanners.QrCodeAnalyzer
import com.pocketsparrow.ui.theme.*
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onTriggerQrScan: (qrPayload: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalSparrowColors.current
    val clipboardManager = LocalClipboardManager.current

    var manualPayload by remember { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

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
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "On-Device Quishing & QR Defense",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Parses QR matrix locally, extracts URLs, and validates threat level before any browser intent is dispatched.",
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        // Viewfinder Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (colors.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                .border(1.5.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                // Live CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val analyzer = QrCodeAnalyzer { rawPayload, _ ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                manualPayload = rawPayload
                                onTriggerQrScan(rawPayload)
                            }
                            imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Reticle overlay
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .border(2.dp, colors.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Laser sweep
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = ((laserProgress - 0.5f) * 150).dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        colors.primary,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            } else {
                // Permission request prompt
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Access",
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Grant camera access to scan physical QR codes locally on-device.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Enable Camera",
                            color = if (colors.isDark) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(colors.cardBg.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Offline Zero-Cloud Scanner • Sub-5ms Pipeline",
                    color = colors.emerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Manual Payload Input Console
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Manual QR Payload Inspection",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inspect raw decoded QR strings, deep-links, or embedded intents.",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = manualPayload,
                    onValueChange = { manualPayload = it },
                    placeholder = {
                        Text(
                            text = "Enter or paste raw QR payload...",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.cardBorder
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
                        border = BorderStroke(1.dp, colors.cardBorder),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (manualPayload.trim().isNotEmpty()) {
                                onTriggerQrScan(manualPayload.trim())
                            }
                        },
                        enabled = manualPayload.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            disabledContainerColor = colors.surface,
                            contentColor = if (colors.isDark) Color.Black else Color.White,
                            disabledContentColor = colors.textMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Analyze QR Payload",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "Manual Threat Vectors",
            color = colors.textPrimary,
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
                border = BorderStroke(1.dp, colors.rose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.rose)
            ) {
                Text("Quishing QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    manualPayload = "javascript:alert('Stolen Token: ' + document.cookie)"
                    onTriggerQrScan(manualPayload)
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, colors.rose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.rose)
            ) {
                Text("Script QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    manualPayload = "https://en.wikipedia.org/wiki/Information_security"
                    onTriggerQrScan(manualPayload)
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emerald)
            ) {
                Text("Benign QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
