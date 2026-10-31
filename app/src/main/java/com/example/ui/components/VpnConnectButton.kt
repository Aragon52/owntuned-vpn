package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.VpnStatus
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VpnDisconnected

@Composable
fun VpnConnectButton(
    status: VpnStatus,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }
    val pulseAnim = remember { Animatable(0.8f) }

    val isConnectingOrAnalyzing = status == VpnStatus.CONNECTING ||
            status == VpnStatus.AI_ANALYZING ||
            status == VpnStatus.TESTING_NETWORK

    LaunchedEffect(isConnected, isConnectingOrAnalyzing) {
        if (isConnected || isConnectingOrAnalyzing) {
            pulseAnim.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAnim.snapTo(1f)
        }
    }

    val primaryColor = when {
        isConnected -> CyberEmerald
        isConnectingOrAnalyzing -> CyberAmber
        else -> CyberCyan
    }

    val outerGlowColor = primaryColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .size(200.dp)
            .testTag("vpn_connect_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring Canvas
        Canvas(modifier = Modifier.size(190.dp)) {
            val radius = size.minDimension / 2
            if (isConnected || isConnectingOrAnalyzing) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = radius * pulseAnim.value
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(outerGlowColor, Color.Transparent)
                ),
                radius = radius
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.6f),
                radius = radius - 4,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Main inner circle button
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when {
                        status == VpnStatus.AI_ANALYZING || status == VpnStatus.TESTING_NETWORK -> Icons.Default.AutoAwesome
                        isConnectingOrAnalyzing -> Icons.Default.Refresh
                        isConnected -> Icons.Default.Lock
                        else -> Icons.Default.PowerSettingsNew
                    },
                    contentDescription = "VPN Power Switch",
                    tint = primaryColor,
                    modifier = Modifier
                        .size(48.dp)
                        .scale(if (isConnectingOrAnalyzing) pulseAnim.value else 1f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (status) {
                        VpnStatus.CONNECTED -> "SECURED"
                        VpnStatus.CONNECTING -> "TUNNELING..."
                        VpnStatus.AI_ANALYZING -> "AI TUNING"
                        VpnStatus.TESTING_NETWORK -> "PROBING..."
                        VpnStatus.DISCONNECTING -> "STOPPING"
                        else -> "TAP TO CONNECT"
                    },
                    color = if (isConnected) CyberEmerald else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
