package com.pocketsparrow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.core.ScanResult
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.ui.theme.*

@Composable
fun DashboardScreen(
    recentLogs: List<ScanLogEntity>,
    onTriggerScan: (contentType: Int, payload: String) -> Unit
) {
    var inputPayload by remember { mutableStateOf("https://g00gle-security-check.cfd/auth/verify?id=9281") }
    var selectedType by remember { mutableIntStateOf(NativeBridge.CONTENT_TYPE_URL) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Airgap Status Banner
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
                            .background(CyberEmerald, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AIRPLANE MODE READY (100% On-Device)",
                        color = CyberEmerald,
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

        // Metrics HUD Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HudMetricCard(
                    title = "LATENCY SLA",
                    value = "< 50 ms",
                    sub = "Tier 1: <5ms | Tier 2: <40ms",
                    accent = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                HudMetricCard(
                    title = "PEAK RAM",
                    value = "42.5 MB",
                    sub = "Budget: < 250 MB",
                    accent = CyberEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Threat Inspector
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
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
                            onClick = {
                                selectedType = NativeBridge.CONTENT_TYPE_URL
                                inputPayload = "https://g00gle-security-check.cfd/auth/verify?id=9281"
                            },
                            label = { Text("URL", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedType == NativeBridge.CONTENT_TYPE_SMS_TEXT,
                            onClick = {
                                selectedType = NativeBridge.CONTENT_TYPE_SMS_TEXT
                                inputPayload = "BANK ALERT: Unusual wire transfer of $2,450.00 initiated. Cancel now: http://fake.com"
                            },
                            label = { Text("SMS / Chat", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPayload,
                        onValueChange = { inputPayload = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onTriggerScan(selectedType, inputPayload) },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                    ) {
                        Text("Evaluate Threat (<50ms)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Recent Audit Logs
        item {
            Text(
                text = "Local Encrypted Logs (Room + SQLCipher)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (recentLogs.isEmpty()) {
            item {
                Text(
                    text = "No threats logged yet. Try evaluating one of the test cases above.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        } else {
            items(recentLogs) { log ->
                LogItemRow(log)
            }
        }
    }
}

@Composable
fun HudMetricCard(title: String, value: String, sub: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = modifier.border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, color = Color(0xFF64748B), fontSize = 9.sp)
        }
    }
}

@Composable
fun LogItemRow(log: ScanLogEntity) {
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
            fontFamily = FontFamily.Monospace
        )
    }
}
