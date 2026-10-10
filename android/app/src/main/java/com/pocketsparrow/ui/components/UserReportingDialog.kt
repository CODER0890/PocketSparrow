package com.pocketsparrow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.data.AppDatabase
import com.pocketsparrow.data.SpamNumberEntity
import com.pocketsparrow.data.SpamSenderEntity
import com.pocketsparrow.data.UserReportEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Composable
fun UserReportingDialog(
    onDismiss: () -> Unit,
    onReportSubmitted: () -> Unit,
    database: AppDatabase
) {
    var target by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("SCAM") }
    var reason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf(
        "ROBOCALL" to "Robocall / Automated",
        "SCAM" to "Financial / Impersonation Scam",
        "PHISHING" to "Phishing / Credential Harvest",
        "TELEMARKETING" to "Unsolicited Telemarketing",
        "OTHER" to "Other Malicious Activity"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(24.dp)
                )
                Text(text = "Report Spam or Phishing", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Privacy Guarantee Badge
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "100% On-Device: Target is hashed with SHA-256 and stored in your encrypted local SQLCipher vault.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Phone Number or Email Sender", fontSize = 11.sp) },
                    placeholder = { Text("+1 (555) 019-2834", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(text = "Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { (catKey, catLabel) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = (category == catKey),
                                onClick = { category = catKey }
                            )
                            Text(text = catLabel, fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Notes (Optional)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (target.isNotBlank()) {
                        isSubmitting = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val cleanTarget = target.trim()
                            val digest = MessageDigest.getInstance("SHA-256")
                            val hash = digest.digest("sparrow_salt_$cleanTarget".toByteArray())
                                .joinToString("") { "%02x".format(it) }
                            val now = System.currentTimeMillis()

                            val masked = if (cleanTarget.contains("@")) {
                                "${cleanTarget.take(3)}***@${cleanTarget.substringAfter("@")}"
                            } else if (cleanTarget.length > 6) {
                                "${cleanTarget.take(3)}****${cleanTarget.takeLast(3)}"
                            } else "***"

                            if (cleanTarget.contains("@")) {
                                database.spamDao().insertSender(
                                    SpamSenderEntity(
                                        hash = hash,
                                        originalMasked = masked,
                                        firstSeen = now,
                                        lastSeen = now,
                                        reportCount = 1,
                                        category = category
                                    )
                                )
                            } else {
                                database.spamDao().insertNumber(
                                    SpamNumberEntity(
                                        hash = hash,
                                        originalMasked = masked,
                                        firstSeen = now,
                                        lastSeen = now,
                                        reportCount = 1,
                                        category = category
                                    )
                                )
                            }

                            database.spamDao().insertReport(
                                UserReportEntity(
                                    id = "rep_$now",
                                    targetHash = hash,
                                    targetMasked = masked,
                                    category = category,
                                    reason = reason,
                                    timestamp = now
                                )
                            )

                            CoroutineScope(Dispatchers.Main).launch {
                                isSubmitting = false
                                onReportSubmitted()
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = !isSubmitting && target.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
            ) {
                Text("Add to Local Blocklist", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}
