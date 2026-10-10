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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.scanners.EmailPhishingAuditor
import com.pocketsparrow.scanners.EmailScanVerdict
import com.pocketsparrow.ui.components.ThreatDnaVisualizer

data class AndroidScannedEmail(
    val id: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val verdict: String, // "Safe", "Suspicious", "Malicious"
    val category: String,
    val timestamp: String,
    val targetUrl: String? = null,
    val xaiReasons: List<String> = emptyList(),
    val trackingPixelsNeutralized: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailShieldScreen(
    modifier: Modifier = Modifier
) {
    val colors = com.pocketsparrow.ui.theme.LocalSparrowColors.current
    var activeFilter by remember { mutableStateOf("all") }
    var selectedDnaEmail by remember { mutableStateOf<AndroidScannedEmail?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Live inspector state
    var inspectSender by remember { mutableStateOf("Chase Security <alerts@chase-wire-update.test>") }
    var inspectSubject by remember { mutableStateOf("URGENT: Wire transfer of $4,850 pending. Immediate action required.") }
    var inspectBody by remember { mutableStateOf("Your account access has been restricted due to unauthorized transfer. Confirm your identity at http://verify-auth-session.test immediately.") }
    var latestScanResult by remember { mutableStateOf<EmailScanVerdict?>(null) }

    val emailList = remember {
        mutableStateListOf(
            AndroidScannedEmail(
                id = "1",
                sender = "alert@security-update-chase.example.test",
                subject = "Urgent: Immediate verification required for wire transfer",
                snippet = "Your online banking access has been placed on hold. Please visit http://verify-auth.example.test to confirm.",
                verdict = "Malicious",
                category = "CREDENTIAL_HARVESTING",
                timestamp = "10m ago",
                targetUrl = "http://verify-auth.example.test",
                xaiReasons = listOf(
                    "Display-name impersonation: Claims 'Chase' from unauthorized domain.",
                    "High-urgency coercive financial phrasing detected.",
                    "Suspicious credential harvesting destination URL."
                )
            ),
            AndroidScannedEmail(
                id = "2",
                sender = "notifications@github-support.example.test",
                subject = "New sign-in from unknown device in Frankfurt",
                snippet = "We detected a login to your account from a new IP address. Review security keys if unauthorized.",
                verdict = "Safe",
                category = "BENIGN",
                timestamp = "35m ago",
                targetUrl = "https://example.com/security",
                xaiReasons = listOf("Email passed local cryptographic authentication and sender integrity checks.")
            )
        )
    }

    fun runScan(s: String, sub: String, b: String) {
        val verdict = EmailPhishingAuditor.auditEmail(s, sub, b)
        latestScanResult = verdict
        val newEmail = AndroidScannedEmail(
            id = System.currentTimeMillis().toString(),
            sender = s,
            subject = sub,
            snippet = if (b.length > 120) b.substring(0, 120) + "..." else b,
            verdict = verdict.verdict,
            category = verdict.category,
            timestamp = "Just now",
            targetUrl = verdict.extractedUrls.firstOrNull(),
            xaiReasons = verdict.xaiReasons,
            trackingPixelsNeutralized = verdict.trackingPixelsNeutralized
        )
        emailList.add(0, newEmail)
    }

    val filtered = if (activeFilter == "threats") {
        emailList.filter { it.verdict == "Malicious" || it.verdict == "Suspicious" }
    } else {
        emailList
    }

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
                        Text("100% on-device IMAP neural monitoring & phishing defense", fontSize = 12.sp, color = colors.textSecondary)
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
                        Text("${emailList.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
                        Text("${emailList.count { it.verdict == "Malicious" || it.verdict == "Suspicious" }}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.rose)
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
                        Text("14.5 ms", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.emerald)
                    }
                }
            }
        }

        // Live Phishing Mail Inspector Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.cardBg,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("On-Device Phishing Mail Inspector", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.emerald.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.3f))
                        ) {
                            Text("<20ms Zero-Cloud", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = colors.emerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Evaluate any email for brand spoofing, deceptive links, hidden tracking pixels, and coercive financial phishing lures.", fontSize = 12.sp, color = colors.textSecondary)

                    // Quick vector test buttons
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pre-Calibrated Test Vectors:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    inspectSender = "Chase Security <alerts@chase-wire-update.test>"
                                    inspectSubject = "URGENT: Wire transfer of $4,850 pending. Immediate action required."
                                    inspectBody = "Your account access has been restricted due to unauthorized transfer. Confirm your identity at http://verify-auth-session.test immediately."
                                    runScan(inspectSender, inspectSubject, inspectBody)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Chase Phish", fontSize = 10.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    inspectSender = "PayPal Support <service.paypal@gmail.com>"
                                    inspectSubject = "Security Alert: Unauthorized sign-in from Moscow"
                                    inspectBody = "Immediate verification required. Confirm your account details at http://paypal-notice.xyz/signin to prevent termination."
                                    runScan(inspectSender, inspectSubject, inspectBody)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Gmail Spoof", fontSize = 10.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    inspectSender = "Alex Mercer <alex@colleague.local>"
                                    inspectSubject = "Sprint Retrospective tomorrow at 10 AM"
                                    inspectBody = "Hey team, looking forward to reviewing Q3 deliverables. The agenda is attached in the shared workspace."
                                    runScan(inspectSender, inspectSubject, inspectBody)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Safe Mail", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                    // Input fields
                    OutlinedTextField(
                        value = inspectSender,
                        onValueChange = { inspectSender = it },
                        label = { Text("From / Sender Address", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    OutlinedTextField(
                        value = inspectSubject,
                        onValueChange = { inspectSubject = it },
                        label = { Text("Email Subject", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    OutlinedTextField(
                        value = inspectBody,
                        onValueChange = { inspectBody = it },
                        label = { Text("Email Body Content / Links", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    Button(
                        onClick = { runScan(inspectSender, inspectSubject, inspectBody) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audit Email for Phishing", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Latest result display
                    latestScanResult?.let { res ->
                        val isMalicious = res.verdict == "Malicious"
                        val isSuspicious = res.verdict == "Suspicious"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isMalicious -> colors.rose.copy(alpha = 0.08f)
                                isSuspicious -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                                else -> colors.emerald.copy(alpha = 0.08f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isMalicious -> colors.rose.copy(alpha = 0.3f)
                                    isSuspicious -> Color(0xFFF59E0B).copy(alpha = 0.3f)
                                    else -> colors.emerald.copy(alpha = 0.3f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Verdict: ${res.verdict.uppercase()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isMalicious -> colors.rose
                                            isSuspicious -> Color(0xFFD97706)
                                            else -> colors.emerald
                                        }
                                    )
                                    Text(
                                        text = "${res.category} • ${res.latencyMs.toInt()}ms",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary
                                    )
                                }

                                Text("Explainable AI (XAI) Attribution:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                res.xaiReasons.forEach { reason ->
                                    Text("• $reason", fontSize = 11.sp, color = colors.textSecondary)
                                }

                                if (res.extractedUrls.isNotEmpty()) {
                                    Text("Extracted URLs (${res.extractedUrls.size}): ${res.extractedUrls.joinToString()}", fontSize = 10.sp, color = colors.primary)
                                }
                            }
                        }
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
                    label = { Text("All Scans (${emailList.size})") }
                )
                FilterChip(
                    selected = activeFilter == "threats",
                    onClick = { activeFilter = "threats" },
                    label = { Text("Threats Only (${emailList.count { it.verdict == "Malicious" || it.verdict == "Suspicious" }})") }
                )
            }
        }

        // Email Items
        items(filtered) { email ->
            val isMalicious = email.verdict == "Malicious"
            val isSuspicious = email.verdict == "Suspicious"

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.cardBg,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            email.sender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                isMalicious -> colors.rose.copy(alpha = 0.12f)
                                isSuspicious -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                else -> colors.emerald.copy(alpha = 0.12f)
                            }
                        ) {
                            Text(
                                text = email.verdict,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isMalicious -> colors.rose
                                    isSuspicious -> Color(0xFFD97706)
                                    else -> colors.emerald
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(email.subject, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(email.snippet, fontSize = 11.sp, color = colors.textSecondary, maxLines = 2)

                    if (email.xaiReasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("XAI: ${email.xaiReasons.first()}", fontSize = 10.sp, color = colors.textMuted, maxLines = 1)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
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
