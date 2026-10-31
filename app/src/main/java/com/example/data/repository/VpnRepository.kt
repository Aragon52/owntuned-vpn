package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.db.NexusVpnDatabase
import com.example.data.db.VpnLogEntity
import com.example.data.models.AppInfo
import com.example.data.models.SecurityDiagnosticResult
import com.example.data.models.ServerRegion
import com.example.data.models.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class VpnRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        NexusVpnDatabase::class.java,
        "nexus_vpn.db"
    ).fallbackToDestructiveMigration().build()

    private val logDao = db.vpnLogDao()

    val vpnLogs: Flow<List<VpnLogEntity>> = logDao.getRecentLogs()

    private val initialServers = listOf(
        // Americas
        VpnServer("us_ny_01", "US", "United States", "New York", "🇺🇸", "185.220.101.5", 34, 24, 24, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.AMERICAS),
        VpnServer("us_la_02", "US", "United States", "Los Angeles", "🇺🇸", "185.220.101.12", 48, 38, 38, supportsP2P = true, supportsStreaming = true, isStealth = false, region = ServerRegion.AMERICAS),
        VpnServer("ca_tr_01", "CA", "Canada", "Toronto", "🇨🇦", "198.51.100.44", 22, 31, 31, supportsP2P = true, supportsStreaming = true, isStealth = false, region = ServerRegion.AMERICAS),
        VpnServer("br_sp_01", "BR", "Brazil", "São Paulo", "🇧🇷", "177.12.98.5", 65, 115, 115, supportsP2P = true, supportsStreaming = false, isStealth = false, region = ServerRegion.AMERICAS),

        // Europe
        VpnServer("de_fk_01", "DE", "Germany", "Frankfurt", "🇩🇪", "185.220.102.8", 29, 82, 82, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.EUROPE),
        VpnServer("uk_ld_01", "GB", "United Kingdom", "London", "🇬🇧", "185.220.102.15", 41, 74, 74, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.EUROPE),
        VpnServer("ch_zh_01", "CH", "Switzerland", "Zurich", "🇨🇭", "194.230.14.50", 18, 88, 88, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.EUROPE),
        VpnServer("nl_am_01", "NL", "Netherlands", "Amsterdam", "🇳🇱", "185.220.101.99", 52, 79, 79, supportsP2P = true, supportsStreaming = true, isStealth = false, region = ServerRegion.EUROPE),
        VpnServer("fr_pa_01", "FR", "France", "Paris", "🇫🇷", "185.220.103.11", 38, 84, 84, supportsP2P = true, supportsStreaming = true, isStealth = false, region = ServerRegion.EUROPE),
        VpnServer("se_st_01", "SE", "Sweden", "Stockholm", "🇸🇪", "193.10.252.1", 25, 96, 96, supportsP2P = true, supportsStreaming = true, isStealth = false, region = ServerRegion.EUROPE),

        // Asia Pacific
        VpnServer("jp_tk_01", "JP", "Japan", "Tokyo", "🇯🇵", "202.214.194.1", 45, 132, 132, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.ASIA_PACIFIC),
        VpnServer("sg_sg_01", "SG", "Singapore", "Singapore", "🇸🇬", "165.21.83.88", 30, 145, 145, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.ASIA_PACIFIC),
        VpnServer("au_sy_01", "AU", "Australia", "Sydney", "🇦🇺", "139.130.4.5", 58, 195, 195, supportsP2P = true, supportsStreaming = true, isStealth = false, region = ServerRegion.ASIA_PACIFIC),
        VpnServer("kr_se_01", "KR", "South Korea", "Seoul", "🇰🇷", "211.234.238.1", 50, 140, 140, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.ASIA_PACIFIC),
        VpnServer("in_mb_01", "IN", "India", "Mumbai", "🇮🇳", "115.240.0.1", 62, 168, 168, supportsP2P = true, supportsStreaming = false, isStealth = false, region = ServerRegion.ASIA_PACIFIC),

        // Middle East & Africa
        VpnServer("ae_db_01", "AE", "UAE", "Dubai", "🇦🇪", "94.200.0.1", 33, 158, 158, supportsP2P = false, supportsStreaming = true, isStealth = true, region = ServerRegion.MIDDLE_EAST_AFRICA),
        VpnServer("za_cpt_01", "ZA", "South Africa", "Cape Town", "🇿🇦", "196.25.1.1", 42, 210, 210, supportsP2P = true, supportsStreaming = false, isStealth = false, region = ServerRegion.MIDDLE_EAST_AFRICA)
    )

    private val _servers = MutableStateFlow(initialServers)
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _installedApps = MutableStateFlow(
        listOf(
            AppInfo("com.netflix.mediaclient", "Netflix", isBypassingVpn = false),
            AppInfo("com.google.android.youtube", "YouTube", isBypassingVpn = false),
            AppInfo("com.spotify.music", "Spotify", isBypassingVpn = false),
            AppInfo("com.whatsapp", "WhatsApp", isBypassingVpn = false),
            AppInfo("com.telegram.messenger", "Telegram", isBypassingVpn = false),
            AppInfo("com.chrome.canary", "Google Chrome", isBypassingVpn = false),
            AppInfo("com.valvesoftware.android.steam", "Steam Mobile", isBypassingVpn = false),
            AppInfo("com.pubg.krmobile", "PUBG Mobile", isBypassingVpn = false)
        )
    )
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    suspend fun runNetworkLatencyProbe(): List<VpnServer> {
        return withContext(Dispatchers.IO) {
            delay(1200L) // Simulate active network ping probe round-trip
            val updated = _servers.value.map { server ->
                val jitter = (-5..8).random()
                val newPing = (server.basePingMs + jitter).coerceAtLeast(12)
                server.copy(currentPingMs = newPing)
            }
            _servers.value = updated
            updated
        }
    }

    suspend fun logVpnAction(
        actionType: String,
        serverName: String,
        protocolName: String,
        pingMs: Int,
        notes: String
    ) {
        withContext(Dispatchers.IO) {
            logDao.insertLog(
                VpnLogEntity(
                    actionType = actionType,
                    serverName = serverName,
                    protocolName = protocolName,
                    pingMs = pingMs,
                    notes = notes
                )
            )
        }
    }

    suspend fun clearLogs() {
        withContext(Dispatchers.IO) {
            logDao.clearLogs()
        }
    }

    suspend fun runSecurityDiagnostic(isVpnActive: Boolean, activeServerIp: String): SecurityDiagnosticResult {
        return withContext(Dispatchers.IO) {
            delay(1500L) // Simulate deep packet inspection and leak tests
            if (isVpnActive) {
                SecurityDiagnosticResult(
                    publicIp = activeServerIp,
                    originalIp = "72.14.201.88 (Hidden by VPN)",
                    isIpMasked = true,
                    dnsServers = listOf("1.1.1.1 (Cloudflare Zero-Log)", "9.9.9.9 (Quad9 Encrypted)"),
                    isDnsSecured = true,
                    isWebRtcLeaking = false,
                    wifiSsid = "CoffeeShop_Guest",
                    wifiSecurityType = "Open (Secured by NexusVPN Tunnel)",
                    isWifiPublicRisk = false,
                    overallSecurityScore = 99,
                    aiThreatAssessment = "EXCELLENT PROTECTION: All outbound traffic is routed through encrypted VPN interface ($activeServerIp). Zero IP, DNS, or WebRTC leaks detected."
                )
            } else {
                SecurityDiagnosticResult(
                    publicIp = "72.14.201.88",
                    originalIp = "72.14.201.88 (Exposed)",
                    isIpMasked = false,
                    dnsServers = listOf("192.168.1.1 (ISP Unencrypted Router DNS)"),
                    isDnsSecured = false,
                    isWebRtcLeaking = true,
                    wifiSsid = "CoffeeShop_Guest",
                    wifiSecurityType = "Open Wi-Fi (UNENCRYPTED)",
                    isWifiPublicRisk = true,
                    overallSecurityScore = 32,
                    aiThreatAssessment = "CRITICAL RISK: You are on an unencrypted public Wi-Fi network with exposed IP (72.14.201.88) and plain-text ISP DNS. Enable NexusVPN AI immediately!"
                )
            }
        }
    }

    fun toggleAppSplitTunnel(packageName: String) {
        val updated = _installedApps.value.map {
            if (it.packageName == packageName) it.copy(isBypassingVpn = !it.isBypassingVpn) else it
        }
        _installedApps.value = updated
    }
}
