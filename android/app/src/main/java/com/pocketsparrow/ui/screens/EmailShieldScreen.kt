package com.pocketsparrow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.ui.components.ThreatDnaVisualizer

data class AndroidScannedEmail(
    val id: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val verdict: String, // "Safe", "Malicious"
    val category: String,
    val timestamp: String,
    val targetUrl: String? = null
)

@Composable
fun EmailShieldScreen(
    modifier: Modifier = Modifier
) {
    val colors = com.pocketsparrow.ui.theme.LocalSparrowColors.current
    var activeFilter by remember { mutableStateOf("all") }
    var selectedDnaEmail by remember { mutableStateOf<AndroidScannedEmail?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val sampleEmails = remember {
        listOf(
            AndroidScannedEmail(
                id = "1",
                sender = "alert@security-update-chase.example.test",
                subject = "Urgent: Immediate verification required for wire transfer",
                snippet = "Your online banking access has been placed on hold. Please visit http://verify-auth.example.test to confirm.",
                verdict = "Malicious",
                category = "CREDENTIAL_HARVESTING",
                timestamp = "10m ago",
                targetUrl = "http://verify-auth.example.test"
            ),
            AndroidScannedEmail(
                id = "2",
                sender = "notifications@github-support.example.test",
                subject = "New sign-in from unknown device in Frankfurt",
                snippet = "We detected a login to your account from a new IP address. Review security keys if unauthorized.",
                verdict = "Safe",
                category = "BENIGN",
                timestamp = "35m ago",
                targetUrl = "https://example.com/security"
            )
        )
    }

    val filtered = if (activeFilter == "threats") sampleEmails.filter { it.verdict == "Malicious" } else sampleEmails

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.bg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.cardBg,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mail, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email Background Shield", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Text("100% on-device IMAP neural monitoring", fontSize = 12.sp, color = colors.textSecondary)
                    }

                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account", tint = colors.primary)
                    }
                }
            }
        }

        // Stats Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Scanned Today", fontSize = 11.sp, color = colors.textMuted)
                        Text("${sampleEmails.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Threats Blocked", fontSize = 11.sp, color = colors.textMuted)
                        Text("${sampleEmails.count { it.verdict == "Malicious" }}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.rose)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Avg Latency", fontSize = 11.sp, color = colors.textMuted)
                        Text("16.2 ms", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.emerald)
                    }
                }
            }
        }

        // Filter Tabs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeFilter == "all",
                    onClick = { activeFilter = "all" },
                    label = { Text("All Scans (${sampleEmails.size})") }
                )
                FilterChip(
                    selected = activeFilter == "threats",
                    onClick = { activeFilter = "threats" },
                    label = { Text("Threats Only (${sampleEmails.count { it.verdict == "Malicious" }})") }
                )
            }
        }

        // Email Items
        items(filtered) { email ->
            val isMalicious = email.verdict == "Malicious"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.cardBg,
                border = BorderStroke(1.dp, if (isMalicious) colors.rose.copy(alpha = 0.5f) else colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(email.sender, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isMalicious) colors.rose.copy(alpha = 0.15f) else colors.emerald.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isMalicious) colors.rose else colors.emerald)
                        ) {
                            Text(
                                text = email.verdict,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMalicious) colors.rose else colors.emerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(email.subject, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(email.snippet, fontSize = 11.sp, color = colors.textSecondary, maxLines = 2)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(email.timestamp, fontSize = 10.sp, color = colors.textMuted)

                        TextButton(
                            onClick = { selectedDnaEmail = email },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("View Threat DNA →", fontSize = 11.sp, color = colors.primary)
                        }
                    }
                }
            }
        }
    }

    // Threat DNA popup dialog
    selectedDnaEmail?.let { email ->
        AlertDialog(
            onDismissRequest = { selectedDnaEmail = null },
            title = { Text("Email Threat DNA", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Subject & URL Vector:", fontSize = 12.sp, color = colors.textSecondary)
                    ThreatDnaVisualizer(
                        payload = email.targetUrl ?: email.subject,
                        category = email.category
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDnaEmail = null }) {
                    Text("Close", color = colors.primary)
                }
            },
            containerColor = colors.cardBg
        )
    }

    // Add Account Dialog
    if (showAddDialog) {
        var emailInput by remember { mutableStateOf("") }
        var passInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Connect Protected Mailbox", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Credentials stored in on-device SQLCipher vault.", fontSize = 11.sp, color = colors.textSecondary)
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        label = { Text("App Password / Token") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emerald)
                ) {
                    Text("Enable Shield", color = if (colors.isDark) Color.Black else Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBg
        )
    }
}
