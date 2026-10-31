package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.VpnMetrics
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun MetricCard(
    metrics: VpnMetrics,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download speed
            MetricItem(
                icon = Icons.Default.ArrowDownward,
                iconColor = CyberEmerald,
                label = "DOWNLOAD",
                value = formatSpeed(if (isConnected) metrics.downloadBytesPerSec else 0L)
            )

            // Upload speed
            MetricItem(
                icon = Icons.Default.ArrowUpward,
                iconColor = CyberCyan,
                label = "UPLOAD",
                value = formatSpeed(if (isConnected) metrics.uploadBytesPerSec else 0L)
            )

            // Ping
            MetricItem(
                icon = Icons.Default.NetworkCheck,
                iconColor = if (metrics.currentPingMs < 50) CyberEmerald else CyberCyan,
                label = "PING",
                value = if (isConnected) "${metrics.currentPingMs} ms" else "--"
            )

            // Duration
            MetricItem(
                icon = Icons.Default.Timer,
                iconColor = TextMuted,
                label = "TIME",
                value = if (isConnected) formatTime(metrics.connectedTimeSec) else "00:00"
            )
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    val mbps = (bytesPerSec * 8.0) / (1024 * 1024)
    return if (mbps >= 1.0) {
        String.format(Locale.US, "%.1f Mbps", mbps)
    } else {
        val kbps = (bytesPerSec * 8.0) / 1024
        String.format(Locale.US, "%.0f Kbps", kbps)
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    val hours = mins / 60
    val remMins = mins % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, remMins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", remMins, secs)
    }
}
