package com.pocketsparrow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsparrow.ui.theme.*

@Composable
fun QrScannerScreen(
    onTriggerQrScan: (qrPayload: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "On-Device Quishing & QR Defense",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Parses QR matrix locally, extracts URLs, and validates threat level before any browser intent is dispatched.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )

        // Camera Viewport Mockup / Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                .border(2.dp, CyberCyan, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "[ LOCAL CAMERAX SCANNER ]",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Offline ZXing Engine • Zero Cloud Lookups",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "Test Case B: Quishing Simulation Presets:",
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )

        Button(
            onClick = { onTriggerQrScan("MEBKM:TITLE:Claim Reward;URL:https://chase-login.top/account/suspend;;") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Simulate Quishing QR Link (chase-login.top)", color = Color.White, fontSize = 12.sp)
        }

        Button(
            onClick = { onTriggerQrScan("javascript:alert('Stolen Token: ' + document.cookie)") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Simulate Executable Script QR (javascript:alert)", color = CyberRose, fontSize = 12.sp)
        }

        Button(
            onClick = { onTriggerQrScan("https://en.wikipedia.org/wiki/Information_security") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Simulate Clean QR Code (wikipedia.org)", color = CyberEmerald, fontSize = 12.sp)
        }
    }
}
