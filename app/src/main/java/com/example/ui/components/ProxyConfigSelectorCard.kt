package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.proxy.ProxyConfig
import com.example.data.models.proxy.ProxyProtocolType
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ProxyConfigSelectorCard(
    configs: List<ProxyConfig>,
    activeConfig: ProxyConfig,
    onSelectConfig: (String) -> Unit,
    onImportShareLink: (String) -> Unit,
    onDeleteConfig: (String) -> Unit,
    onTestLatencies: () -> Unit,
    importError: String?,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var inputLink by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("proxy_config_selector_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch Proxy",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PROXY CONFIG REPOSITORY",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Active: ${activeConfig.protocolType.displayName} • ${activeConfig.name}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProtocolBadge(activeConfig.protocolType.displayName, CyberEmerald)
                    ProtocolBadge("${activeConfig.transport.wireName.uppercase()} / ${activeConfig.security.wireName.uppercase()}", CyberCyan)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${activeConfig.latencyMs} ms",
                        color = CyberEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showImportDialog = !showImportDialog },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan.copy(alpha = 0.15f),
                            contentColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Import Link", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Share Link Import Panel
            AnimatedVisibility(visible = showImportDialog) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF090D16), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(text = "IMPORT SHARE LINK (vless://, vmess://, trojan://)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inputLink,
                        onValueChange = { inputLink = it },
                        placeholder = { Text("Paste vless://, vmess://, or trojan:// share link", color = TextMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurface,
                            unfocusedContainerColor = CyberSurface,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = importError, color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (inputLink.isNotBlank()) {
                                onImportShareLink(inputLink)
                                inputLink = ""
                                showImportDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text(text = "Add to Repository", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expanded Config List Selector
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "SAVED PROXY CONFIGURATIONS (${configs.size})", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Test Latency",
                            color = CyberEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onTestLatencies() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    configs.forEach { config ->
                        val isSelected = config.id == activeConfig.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color(0xFF090D16))
                                .border(1.dp, if (isSelected) CyberCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectConfig(config.id) }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) CyberCyan else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = config.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${config.protocolType.displayName} • ${config.serverAddress}:${config.serverPort}",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "${config.latencyMs} ms", color = CyberEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteConfig(config.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
