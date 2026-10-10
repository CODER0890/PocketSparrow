package com.pocketsparrow.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.scanners.AndroidProcessInfo
import com.pocketsparrow.scanners.ProcessAuditor
import com.pocketsparrow.ui.theme.LocalSparrowColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProcessMonitorScreen() {
    val context = LocalContext.current
    val colors = LocalSparrowColors.current
    val coroutineScope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }
    var processes by remember { mutableStateOf<List<AndroidProcessInfo>>(emptyList()) }
    var filterSuspiciousOnly by remember { mutableStateOf(false) }

    fun refresh() {
        isRefreshing = true
        coroutineScope.launch {
            val auditor = ProcessAuditor(context)
            val scanned = auditor.scanProcesses()
            processes = scanned
            delay(300)
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val suspiciousCount = processes.count { it.isSuspicious }
    val systemCount = processes.count { it.isSystem }
    val displayedProcesses = if (filterSuspiciousOnly) processes.filter { it.isSuspicious } else processes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Header & Stats
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
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Process Monitor",
                                    color = colors.textPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Daemon & Task Auditor",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { refresh() },
                            enabled = !isRefreshing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.bg,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SCANNED", fontSize = 9.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                                Text("${processes.size}", fontSize = 16.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Processes", fontSize = 9.sp, color = colors.textSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.bg,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SYSTEM OS", fontSize = 9.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                                Text("$systemCount", fontSize = 16.sp, color = colors.emerald, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Protected", fontSize = 9.sp, color = colors.textSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.bg,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SUSPICIOUS", fontSize = 9.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                                Text("$suspiciousCount", fontSize = 16.sp, color = if (suspiciousCount > 0) colors.rose else colors.emerald, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(if (suspiciousCount > 0) "Alert" else "Zero Alert", fontSize = 9.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle filter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !filterSuspiciousOnly,
                            onClick = { filterSuspiciousOnly = false },
                            label = { Text("All Tasks (${processes.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterSuspiciousOnly,
                            onClick = { filterSuspiciousOnly = true },
                            label = { Text("Alerts Only ($suspiciousCount)", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 1.5 Security Info Banner
        item {
            Surface(
                color = colors.cardBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sandbox Process Isolation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Android restricts process visibility for security. Monitoring active foreground services and accessible package metadata.",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Process Items or All Clear Empty State
        if (displayedProcesses.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(colors.emerald.copy(alpha = 0.12f))
                                .border(1.5.dp, colors.emerald.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "All Clear",
                                tint = colors.emerald,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (filterSuspiciousOnly) "Zero Threat Alerts" else "All Clear — Zero Anomalies",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (filterSuspiciousOnly)
                                "No active processes or background services match known malware signatures or reverse shells."
                            else
                                "All active processes and background services are within normal operating bounds.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(displayedProcesses, key = { it.pid }) { proc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (proc.isSuspicious) colors.rose.copy(alpha = 0.5f)
                    else colors.cardBorder
                ),
                modifier = Modifier.fillMaxWidth()
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
                                color = colors.bg,
                                border = BorderStroke(1.dp, colors.cardBorder)
                            ) {
                                Text(
                                    text = "PID ${proc.pid}",
                                    color = colors.textMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = proc.name,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (proc.isSuspicious) colors.rose.copy(alpha = 0.12f)
                            else if (proc.isSystem) colors.emerald.copy(alpha = 0.12f)
                            else colors.primary.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (proc.isSuspicious) colors.rose.copy(alpha = 0.3f)
                                else if (proc.isSystem) colors.emerald.copy(alpha = 0.3f)
                                else colors.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = if (proc.isSuspicious) "SUSPICIOUS"
                                else if (proc.isSystem) "SYSTEM"
                                else "USER APP",
                                color = if (proc.isSuspicious) colors.rose
                                else if (proc.isSystem) colors.emerald
                                else colors.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = proc.path,
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (proc.isSuspicious) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠ ${proc.threatDetail}",
                            color = colors.rose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        }
    }
}
