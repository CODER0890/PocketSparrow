package com.pocketsparrow.ui.screens

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
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.services.LiveShieldManager
import com.pocketsparrow.ui.theme.LocalSparrowColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    database: AppDatabase
) {
    val colors = LocalSparrowColors.current
    val coroutineScope = rememberCoroutineScope()

    val sensitivity by LiveShieldManager.sensitivityLevel.collectAsState()
    val isZeroRetention by LiveShieldManager.isZeroRetentionEnabled.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    var clearSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Header Card
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Engine Settings",
                                color = colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "On-Device Neural Engine & Privacy Controls",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Detection Sensitivity Card
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
                        Text(
                            text = "Global Threat Sensitivity",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
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
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Controls Tier 1 heuristic ambiguity thresholds before activating Tier 2 MobileBERT transformer inference.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

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

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Zero Retention Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zero Retention Mode",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Keeps threat evaluations in volatile RAM only. Bypasses SQLite writes to the Forensic Vault for 0-footprint flash privacy.",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = isZeroRetention,
                            onCheckedChange = { LiveShieldManager.setZeroRetentionEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.emerald
                            )
                        )
                    }
                }
            }
        }

        // 3. Model Architecture Metadata
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Neural Runtime & Architecture",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "Model Architecture" to "MobileBERT INT8 PTQ Quantized",
                        "Parameter Footprint" to "33.0 MB on-device weights",
                        "Vocabulary Tokens" to "30,522 WordPiece subwords",
                        "Latency Target" to "<50ms P99 Hard SLA",
                        "Local Database" to "SQLCipher AES-256 GCM",
                        "Network Permitted" to "STRICTLY NONE (0 WAN Bytes)"
                    ).forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, color = colors.textSecondary, fontSize = 12.sp)
                            Text(text = value, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // 4. Data Management & Clear History
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Evidence Vault Maintenance",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Permanently purge encrypted scan logs from local SQLite flash storage.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Button(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.rose.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, colors.rose.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = colors.rose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purge Forensic Vault Logs", color = colors.rose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (clearSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Vault logs cleared successfully.", color = colors.emerald, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Purge Forensic Vault?") },
            text = { Text("This will permanently delete all encrypted scan history and quarantined records stored on device.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.clearAllTables()
                            LiveShieldManager.clearEvents()
                            launch(Dispatchers.Main) {
                                showClearConfirm = false
                                clearSuccess = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.rose)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
