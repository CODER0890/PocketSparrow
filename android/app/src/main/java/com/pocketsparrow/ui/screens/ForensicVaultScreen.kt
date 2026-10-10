package com.pocketsparrow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.ScanLogEntity
import com.pocketsparrow.ui.components.ForensicExportDialog
import com.pocketsparrow.ui.theme.LocalSparrowColors
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ForensicVaultScreen(
    database: AppDatabase
) {
    val colors = LocalSparrowColors.current
    var logs by remember { mutableStateOf<List<ScanLogEntity>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedThreatFilter by remember { mutableStateOf<Int?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        database.scanLogDao().getAllLogs().collectLatest {
            logs = it
        }
    }

    val filteredLogs = remember(logs, searchQuery, selectedThreatFilter) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isBlank() ||
                    log.payloadSnippet.contains(searchQuery, ignoreCase = true) ||
                    log.category.contains(searchQuery, ignoreCase = true) ||
                    log.xaiReason.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedThreatFilter == null || log.threatLevel == selectedThreatFilter
            matchesSearch && matchesFilter
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Forensic Vault",
                                    color = colors.textPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "AES-256 SQLCipher Evidence Ledger",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search forensic snippet or category...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Threat Filters
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedThreatFilter == null,
                            onClick = { selectedThreatFilter = null },
                            label = { Text("All (${logs.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedThreatFilter == 2,
                            onClick = { selectedThreatFilter = if (selectedThreatFilter == 2) null else 2 },
                            label = { Text("Malicious (${logs.count { it.threatLevel == 2 }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedThreatFilter == 1,
                            onClick = { selectedThreatFilter = if (selectedThreatFilter == 1) null else 1 },
                            label = { Text("Suspicious (${logs.count { it.threatLevel == 1 }})", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 2. Log Cards
        if (filteredLogs.isEmpty()) {
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
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Forensic Records Found", color = colors.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (log.threatLevel == 2) colors.rose.copy(alpha = 0.4f)
                        else if (log.threatLevel == 1) colors.amber.copy(alpha = 0.4f)
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
                                    color = colors.bg,
                                    border = BorderStroke(1.dp, colors.cardBorder)
                                ) {
                                    Text(
                                        text = log.contentType,
                                        color = colors.textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    color = colors.textMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (log.threatLevel == 2) colors.rose.copy(alpha = 0.12f)
                                else if (log.threatLevel == 1) colors.amber.copy(alpha = 0.12f)
                                else colors.emerald.copy(alpha = 0.12f),
                                border = BorderStroke(
                                    1.dp,
                                    if (log.threatLevel == 2) colors.rose.copy(alpha = 0.3f)
                                    else if (log.threatLevel == 1) colors.amber.copy(alpha = 0.3f)
                                    else colors.emerald.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = if (log.threatLevel == 2) "MALICIOUS"
                                    else if (log.threatLevel == 1) "SUSPICIOUS"
                                    else "SAFE",
                                    color = if (log.threatLevel == 2) colors.rose
                                    else if (log.threatLevel == 1) colors.amber
                                    else colors.emerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = log.payloadSnippet,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = if (expanded) 8 else 2
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Category: ${log.category} • Latency: ${log.latencyMicros}µs",
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = colors.cardBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Forensic Attribution:",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = log.xaiReason,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        val selectedOrTop = logs.firstOrNull()
        ForensicExportDialog(
            payload = selectedOrTop?.payloadSnippet ?: "Forensic Ledger (${logs.size} entries)",
            verdict = if (selectedOrTop?.threatLevel == 2) "MALICIOUS" else "SAFE",
            category = selectedOrTop?.category ?: "SYSTEM_AUDIT",
            xaiReason = selectedOrTop?.xaiReason ?: "Full ledger forensic snapshot compiled from Room SQLCipher database.",
            latencyMicros = selectedOrTop?.latencyMicros ?: 1200L,
            onDismiss = { showExportDialog = false }
        )
    }
}
