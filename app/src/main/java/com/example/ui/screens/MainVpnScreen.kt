package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.SecurityPreference
import com.example.data.models.VpnProtocol
import com.example.data.models.VpnStatus
import com.example.ui.components.AiReasoningCard
import com.example.ui.components.MetricCard
import com.example.ui.components.ProxyConfigSelectorCard
import com.example.ui.components.VpnConnectButton
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.VpnViewModel

@Composable
fun MainVpnScreen(
    viewModel: VpnViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToAiAgent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnStatus by viewModel.vpnStatus.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val activeServer by viewModel.activeServer.collectAsState()
    val activeProtocol by viewModel.activeProtocol.collectAsState()
    val securityPref by viewModel.securityPreference.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val metrics by viewModel.serviceMetrics.collectAsState()

    val proxyConfigs by viewModel.proxyConfigs.collectAsState()
    val activeProxyConfig by viewModel.activeProxyConfig.collectAsState()
    val importError by viewModel.importError.collectAsState()

    val isConnected = vpnStatus == VpnStatus.CONNECTED || isRunning

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Server selector bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
                .clickable { onNavigateToServers() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("server_selector_bar")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = activeServer.flagEmoji, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeServer.city,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${activeServer.countryCode})",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "${activeServer.currentPingMs} ms • ${activeProtocol.displayName}",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHANGE",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Change Server",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Proxy Config Repository Selector (VLESS, VMESS, Trojan)
        ProxyConfigSelectorCard(
            configs = proxyConfigs,
            activeConfig = activeProxyConfig,
            onSelectConfig = { configId -> viewModel.selectProxyConfig(configId, context) },
            onImportShareLink = { link -> viewModel.importShareLink(link) },
            onDeleteConfig = { configId -> viewModel.deleteProxyConfig(configId) },
            onTestLatencies = { viewModel.testProxyLatencies() },
            importError = importError,
            onDismissError = { viewModel.dismissImportError() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Center Connect Button with pulsing rings
        VpnConnectButton(
            status = vpnStatus,
            isConnected = isConnected,
            onClick = { viewModel.toggleVpnConnection(context) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Metrics Bar (Speed / Ping / Duration)
        MetricCard(
            metrics = metrics,
            isConnected = isConnected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI Dynamic Optimization Card
        AiReasoningCard(
            aiConfig = aiConfig,
            onOptimizeClick = { viewModel.triggerAiOptimization("Optimize route for best latency") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Security Preferences Selector Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SECURITY & PROTOCOL PROFILE",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "AI Tuning Active",
                color = CyberEmerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Security Profile Chips Horizontal List
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SecurityPreference.entries.toTypedArray()) { pref ->
                val isSelected = pref == securityPref
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else CyberSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CyberCyan else CyberCardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setSecurityPreference(pref, context) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (pref) {
                                SecurityPreference.BALANCED_AUTO -> Icons.Default.AutoAwesome
                                SecurityPreference.LOW_LATENCY_SPEED -> Icons.Default.Speed
                                SecurityPreference.MAX_SECURITY -> Icons.Default.Security
                                SecurityPreference.STEALTH_ANTI_CENSORED -> Icons.Default.VisibilityOff
                                SecurityPreference.STREAMING_OPTIMIZED -> Icons.Default.Movie
                                SecurityPreference.P2P_TORRENT -> Icons.Default.CloudDownload
                            },
                            contentDescription = null,
                            tint = if (isSelected) CyberCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pref.title,
                            color = if (isSelected) CyberCyan else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Protocol Selector Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurface)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Protocol",
                            tint = CyberAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STEALTH PROTOCOL TUNER",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = activeProtocol.cipherName,
                        color = CyberAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VpnProtocol.entries.toTypedArray()) { proto ->
                        val isSelected = proto == activeProtocol
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberAmber.copy(alpha = 0.2f) else Color(0xFF090D16))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CyberAmber else CyberCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.selectProtocol(proto, context) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = proto.displayName,
                                color = if (isSelected) CyberAmber else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
