package com.pocketsparrow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.pocketsparrow.ui.theme.LocalSparrowColors

@Composable
fun ApkAuditScreen(
    onTriggerAudit: (permissions: Array<String>) -> Unit
) {
    val colors = LocalSparrowColors.current

    // Malicious permission vectors for security auditing
    val bankingTrojanVector = arrayOf(
        "android.permission.RECEIVE_SMS",
        "android.permission.INTERNET",
        "android.permission.SYSTEM_ALERT_WINDOW"
    )

    val dropperVector = arrayOf(
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.INTERNET"
    )

    val accessibilityVector = arrayOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.INTERNET"
    )

    val cleanControlVector = arrayOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Sideloaded APK Permission Auditor",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Static analysis of APK manifests identifying Banking Trojans, Accessibility exploits, and silent droppers.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Permission Profile Inspection Vectors:",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Interactive audit trigger cards
        item {
            AuditTriggerCard(
                title = "Banking Trojan Profile (SMS Intercept + Overlay)",
                permissions = bankingTrojanVector,
                expectedRisk = "High Risk Signature",
                accent = colors.rose,
                onAudit = { onTriggerAudit(bankingTrojanVector) }
            )
        }

        item {
            AuditTriggerCard(
                title = "Accessibility Hijacker Profile (Input Hooking)",
                permissions = accessibilityVector,
                expectedRisk = "Critical Risk Signature",
                accent = colors.rose,
                onAudit = { onTriggerAudit(accessibilityVector) }
            )
        }

        item {
            AuditTriggerCard(
                title = "Silent Dropper Profile (Package Installer)",
                permissions = dropperVector,
                expectedRisk = "Elevated Risk Signature",
                accent = colors.rose,
                onAudit = { onTriggerAudit(dropperVector) }
            )
        }

        item {
            AuditTriggerCard(
                title = "Standard Utility Profile (Baseline)",
                permissions = cleanControlVector,
                expectedRisk = "Benign Baseline",
                accent = colors.emerald,
                onAudit = { onTriggerAudit(cleanControlVector) }
            )
        }
    }
}

@Composable
fun AuditTriggerCard(
    title: String,
    permissions: Array<String>,
    expectedRisk: String,
    accent: Color,
    onAudit: () -> Unit
) {
    val colors = LocalSparrowColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        modifier = Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = expectedRisk,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Permissions: ${permissions.joinToString(", ") { it.substringAfterLast(".") }}",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAudit,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = colors.bg),
                border = BorderStroke(1.dp, colors.cardBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Audit Manifest Permissions", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
