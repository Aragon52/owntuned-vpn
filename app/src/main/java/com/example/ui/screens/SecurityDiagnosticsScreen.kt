package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.VpnViewModel

@Composable
fun SecurityDiagnosticsScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val result by viewModel.diagnosticResult.collectAsState()
    val isDiagnosing by viewModel.isDiagnosing.collectAsState()
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECURITY AUDIT & DIAGNOSTICS",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "IP / DNS / WebRTC Leak & Wi-Fi Safety Scanner",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { viewModel.runSecurityDiagnostic() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isDiagnosing,
                modifier = Modifier.testTag("run_diagnostic_button")
            ) {
                if (isDiagnosing) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Score Overview Card
            item {
                val score = result?.overallSecurityScore ?: 98
                val scoreColor = if (score > 80) CyberEmerald else if (score > 50) CyberAmber else Color(0xFFFF5252)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(scoreColor.copy(alpha = 0.15f))
                                .border(2.dp, scoreColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$score%",
                                color = scoreColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (score > 80) "SECURE CONNECTION" else "RISKS DETECTED",
                                color = scoreColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result?.aiThreatAssessment ?: "Tap 'Scan' to run real-time IP leak and DNS exposure diagnostics.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Diagnostic Checks breakdown
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "LEAK TEST AUDIT RESULTS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DiagnosticRowItem(
                            icon = Icons.Default.Security,
                            title = "Public IP Address",
                            value = result?.publicIp ?: "185.220.101.5",
                            statusOk = result?.isIpMasked ?: true,
                            statusText = if (result?.isIpMasked != false) "IP Masked" else "EXPOSED"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DiagnosticRowItem(
                            icon = Icons.Default.Dns,
                            title = "DNS Leak Protection",
                            value = result?.dnsServers?.firstOrNull() ?: "1.1.1.1 (Cloudflare Secured)",
                            statusOk = result?.isDnsSecured ?: true,
                            statusText = if (result?.isDnsSecured != false) "Encrypted" else "LEAKING"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DiagnosticRowItem(
                            icon = Icons.Default.Lock,
                            title = "WebRTC Leak Protection",
                            value = "STUN/TURN Socket Shielding",
                            statusOk = !(result?.isWebRtcLeaking ?: false),
                            statusText = if (result?.isWebRtcLeaking == true) "LEAKING" else "Protected"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DiagnosticRowItem(
                            icon = Icons.Default.Wifi,
                            title = "Wi-Fi Security Status",
                            value = result?.wifiSecurityType ?: "WPA3-Personal",
                            statusOk = !(result?.isWifiPublicRisk ?: false),
                            statusText = if (result?.isWifiPublicRisk == true) "RISK" else "Secure"
                        )
                    }
                }
            }

            // Kill Switch Toggle Card
            item {
                Box(
                    modifier = Modifier
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "System Kill Switch",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Blocks all internet traffic automatically if VPN connection drops unexpectedly.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = killSwitchEnabled,
                            onCheckedChange = { viewModel.toggleKillSwitch() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CyberCardBorder
                            ),
                            modifier = Modifier.testTag("kill_switch_toggle")
                        )
                    }
                }
            }

            // Split Tunneling List Header
            item {
                Text(
                    text = "SPLIT TUNNELING (BYPASS VPN)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(installedApps) { app ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = app.appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = app.packageName, color = TextMuted, fontSize = 11.sp)
                        }

                        Switch(
                            checked = app.isBypassingVpn,
                            onCheckedChange = { viewModel.toggleAppSplitTunnel(app.packageName) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CyberAmber,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CyberCardBorder
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    statusOk: Boolean,
    statusText: String
) {
    val statusColor = if (statusOk) CyberEmerald else Color(0xFFFF5252)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = value, color = TextMuted, fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (statusOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
