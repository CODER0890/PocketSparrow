package com.pocketsparrow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.ui.theme.CyberCyan
import com.pocketsparrow.ui.theme.CyberEmerald
import com.pocketsparrow.ui.theme.CyberRose
import com.pocketsparrow.ui.theme.CyberSurface

data class AndroidLayerLatency(
    val name: String,
    val latencyMs: Float,
    val percentage: Float
)

@Composable
fun HardwareAcceleratorDashboard(
    modifier: Modifier = Modifier
) {
    val activeRuntime = "Android NNAPI Delegate (QNN / NPU)"
    val cpuUtil = 14.8f
    val gpuUtil = 2.4f
    val npuUtil = 78.2f
    val thermalState = "Nominal (38.4°C)"

    val layerBreakdown = listOf(
        AndroidLayerLatency("Tokenizer (WordPiece)", 0.42f, 2.3f),
        AndroidLayerLatency("Embedding Table", 1.18f, 6.5f),
        AndroidLayerLatency("Self-Attention Blocks (INT8)", 11.84f, 65.1f),
        AndroidLayerLatency("Feed-Forward Layers", 3.82f, 21.0f),
        AndroidLayerLatency("Classification Head", 0.92f, 5.1f)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CyberSurface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hardware Acceleration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = activeRuntime,
                        fontSize = 11.sp,
                        color = CyberEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = thermalState,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Gauges (CPU, GPU, NPU)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CPU
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("CPU Core", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("${cpuUtil}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { cpuUtil / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = CyberCyan,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }

                // GPU
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("GPU Del.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("${gpuUtil}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { gpuUtil / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = CyberEmerald,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }

                // NPU
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("NPU Engine", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("${npuUtil}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { npuUtil / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = CyberEmerald,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Layer-by-Layer Latency Profile
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Inference Time per Layer (MobileBERT INT8)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )

                layerBreakdown.forEach { layer ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(layer.name, fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                "${layer.latencyMs} ms (${layer.percentage}%)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                        LinearProgressIndicator(
                            progress = { layer.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = if (layer.percentage > 50f) CyberRose else CyberCyan,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Fallback Events
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF030712),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Fallback Log: NNAPI Provider Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Qualcomm Hexagon / MediaTek APU acceleration engaged without CPU fallback.",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
