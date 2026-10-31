package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AiAgentScreen
import com.example.ui.screens.LogsAndSettingsScreen
import com.example.ui.screens.MainVpnScreen
import com.example.ui.screens.SecurityDiagnosticsScreen
import com.example.ui.screens.ServerListScreen
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NexusVpnTheme
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NexusVpnTheme {
                val context = LocalContext.current
                val vpnPermissionIntent by viewModel.vpnPermissionNeededIntent.collectAsState()

                // Activity launcher for Android VpnService permission dialog
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.onVpnPermissionGranted(context)
                    }
                }

                LaunchedEffect(vpnPermissionIntent) {
                    vpnPermissionIntent?.let { intent ->
                        launcher.launch(intent)
                    }
                }

                NexusVpnApp(viewModel = viewModel)
            }
        }
    }
}

enum class VpnTab(val title: String, val icon: ImageVector) {
    HOME("VPN", Icons.Default.Lock),
    SERVERS("Nodes", Icons.Default.Public),
    AI_AGENT("AI Agent", Icons.Default.AutoAwesome),
    SECURITY("Audit", Icons.Default.Security),
    LOGS("Logs", Icons.Default.History)
}

@Composable
fun NexusVpnApp(viewModel: VpnViewModel) {
    var selectedTabOrdinal by remember { mutableIntStateOf(VpnTab.HOME.ordinal) }
    val currentTab = VpnTab.entries[selectedTabOrdinal]

    if (currentTab != VpnTab.HOME) {
        BackHandler {
            selectedTabOrdinal = VpnTab.HOME.ordinal
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF080C14),
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                contentColor = TextMuted,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                VpnTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabOrdinal = tab.ordinal },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) CyberCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (isSelected) CyberCyan else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = CyberCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                VpnTab.HOME -> MainVpnScreen(
                    viewModel = viewModel,
                    onNavigateToServers = { selectedTabOrdinal = VpnTab.SERVERS.ordinal },
                    onNavigateToAiAgent = { selectedTabOrdinal = VpnTab.AI_AGENT.ordinal }
                )
                VpnTab.SERVERS -> ServerListScreen(
                    viewModel = viewModel,
                    onBackClick = { selectedTabOrdinal = VpnTab.HOME.ordinal }
                )
                VpnTab.AI_AGENT -> AiAgentScreen(
                    viewModel = viewModel
                )
                VpnTab.SECURITY -> SecurityDiagnosticsScreen(
                    viewModel = viewModel
                )
                VpnTab.LOGS -> LogsAndSettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
