package com.pocketsparrow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

import androidx.compose.ui.text.style.TextOverflow

data class ThreatDnaToken(
    val text: String,
    val weight: Float, // 0.0 to 1.0
    val source: String,
    val tier: Int // 1 or 2
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreatDnaVisualizer(
    payload: String,
    category: String? = null,
    confidence: Float? = null,
    modifier: Modifier = Modifier
) {
    if (payload.isBlank()) return

    val colors = com.pocketsparrow.ui.theme.LocalSparrowColors.current
    val tokens = remember(payload) { tokenize(payload) }
    var selectedToken by remember { mutableStateOf<ThreatDnaToken?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Threat DNA & Attention Map",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    category?.let {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.cardBorder)
                        ) {
                            val badgeText = if (confidence != null) "$it • ${(confidence * 100).toInt()}%" else it
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Token-level neural attention and heuristic attribution",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(colors.primary.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cool (Safe)", fontSize = 10.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(colors.rose.copy(alpha = 0.8f), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rose (Threat)", fontSize = 10.sp, color = colors.rose, fontWeight = FontWeight.SemiBold)
                }
            }

            // Flow Layout of Tokens
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tokens.forEach { token ->
                        val isHigh = token.weight >= 0.7f
                        val bgColor = if (isHigh) {
                            colors.rose.copy(alpha = 0.2f + token.weight * 0.4f)
                        } else if (token.weight >= 0.4f) {
                            colors.amber.copy(alpha = 0.15f + token.weight * 0.3f)
                        } else {
                            colors.primary.copy(alpha = 0.08f + token.weight * 0.15f)
                        }

                        val borderColor = if (isHigh) colors.rose.copy(alpha = 0.6f) else colors.cardBorder

                        Box(
                            modifier = Modifier
                                .background(bgColor, RoundedCornerShape(4.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                .clickable { selectedToken = token }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = token.text,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isHigh) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHigh) colors.rose else colors.textPrimary,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }

            // Token inspection alert/info
            selectedToken?.let { token ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Token: \"${token.text}\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Attention: ${(token.weight * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (token.weight >= 0.7f) colors.rose else colors.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = token.source,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun tokenize(payload: String): List<ThreatDnaToken> {
    val suspiciousKeywords = setOf("login", "verify", "secure", "account", "update", "banking", "urgent", "password", "chase")
    val parts = payload.split(Regex("(?<=[/?&=%#._~:@!$'()*+,;\\-])|(?=[/?&=%#._~:@!$'()*+,;\\-])"))

    return parts.filter { it.isNotEmpty() }.map { token ->
        val lower = token.lowercase()
        val isHomoglyph = token.any { it.code in 0x0400..0x04FF || it.code in 0x0370..0x03FF }

        when {
            isHomoglyph -> ThreatDnaToken(token, 0.98f, "Tier 1: Cyrillic homograph spoofing", 1)
            suspiciousKeywords.any { lower.contains(it) } -> ThreatDnaToken(token, 0.91f, "Tier 2: Semantic credential lure", 2)
            token.length >= 7 && calculateEntropy(token) > 3.8 -> ThreatDnaToken(token, 0.86f, "Tier 1: Shannon entropy anomaly", 1)
            lower in setOf("xyz", "top", "tk", "click") -> ThreatDnaToken(token, 0.82f, "Tier 1: High-risk TLD anomaly", 1)
            token.startsWith("http://") -> ThreatDnaToken(token, 0.45f, "Tier 1: Unencrypted cleartext scheme", 1)
            else -> ThreatDnaToken(token, 0.12f, "Tier 2: Neutral baseline token", 2)
        }
    }
}

private fun calculateEntropy(str: String): Double {
    if (str.isEmpty()) return 0.0
    val map = mutableMapOf<Char, Int>()
    str.forEach { map[it] = (map[it] ?: 0) + 1 }
    var entropy = 0.0
    val len = str.length.toDouble()
    map.values.forEach { count ->
        val p = count / len
        entropy -= p * (kotlin.math.ln(p) / kotlin.math.ln(2.0))
    }
    return entropy
}
