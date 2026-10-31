package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.models.ServerRegion
import com.example.data.models.VpnServer
import com.example.ui.components.ServerCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.VpnViewModel

@Composable
fun ServerListScreen(
    viewModel: VpnViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val servers by viewModel.serversList.collectAsState()
    val activeServer by viewModel.activeServer.collectAsState()
    val selectedRegion by viewModel.selectedRegionFilter.collectAsState()
    val searchQuery by viewModel.serverSearchQuery.collectAsState()

    var selectedPurposeFilter by remember { mutableStateOf("ALL") }

    val filteredServers = servers.filter { server ->
        val matchesRegion = selectedRegion == null || server.region == selectedRegion
        val matchesSearch = searchQuery.isBlank() ||
                server.city.contains(searchQuery, ignoreCase = true) ||
                server.countryName.contains(searchQuery, ignoreCase = true) ||
                server.ipAddress.contains(searchQuery, ignoreCase = true)
        val matchesPurpose = when (selectedPurposeFilter) {
            "VLESS" -> server.supportsVless && server.isStealth
            "VMESS" -> server.supportsVmess
            "TROJAN" -> server.supportsTrojan
            "HYSTERIA" -> server.supportsHysteria
            "STREAMING" -> server.supportsStreaming
            "P2P" -> server.supportsP2P
            else -> true
        }
        matchesRegion && matchesSearch && matchesPurpose
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackClick() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "GLOBAL SERVER NODES",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredServers.size} Locations • VLESS / VMess / Trojan / Hysteria2",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search city, country, or protocol...", color = TextMuted) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("server_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Region Filter Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChipItem(
                    label = "All Regions",
                    isSelected = selectedRegion == null,
                    onClick = { viewModel.setRegionFilter(null) }
                )
            }
            items(ServerRegion.entries.toTypedArray()) { region ->
                FilterChipItem(
                    label = region.displayName,
                    isSelected = selectedRegion == region,
                    onClick = { viewModel.setRegionFilter(region) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Protocol & Purpose Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PurposeChip("ALL", "All", selectedPurposeFilter == "ALL") { selectedPurposeFilter = "ALL" }
                }
                item {
                    PurposeChip("VLESS", "VLESS REALITY 🛡️", selectedPurposeFilter == "VLESS") { selectedPurposeFilter = "VLESS" }
                }
                item {
                    PurposeChip("TROJAN", "Trojan gRPC 🔒", selectedPurposeFilter == "TROJAN") { selectedPurposeFilter = "TROJAN" }
                }
                item {
                    PurposeChip("HYSTERIA", "Hysteria 2 🚀", selectedPurposeFilter == "HYSTERIA") { selectedPurposeFilter = "HYSTERIA" }
                }
                item {
                    PurposeChip("VMESS", "VMess WS 🌐", selectedPurposeFilter == "VMESS") { selectedPurposeFilter = "VMESS" }
                }
                item {
                    PurposeChip("STREAMING", "Streaming 🎬", selectedPurposeFilter == "STREAMING") { selectedPurposeFilter = "STREAMING" }
                }
                item {
                    PurposeChip("P2P", "P2P Torrent ⚡", selectedPurposeFilter == "P2P") { selectedPurposeFilter = "P2P" }
                }
            }

            Button(
                onClick = { viewModel.triggerAiOptimization("Ping probe servers") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberEmerald.copy(alpha = 0.15f),
                    contentColor = CyberEmerald
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Ping", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Server List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredServers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    isSelected = server.id == activeServer.id,
                    onClick = {
                        viewModel.selectServer(server, context)
                        onBackClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CyberCyan else CyberSurface)
            .border(1.dp, if (isSelected) CyberCyan else CyberCardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PurposeChip(
    key: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (isSelected) CyberCyan else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) CyberCyan else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
