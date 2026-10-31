package com.example.data.models

data class AiVpnConfig(
    val recommendedServer: VpnServer,
    val recommendedProtocol: VpnProtocol,
    val mtu: Int = 1420,
    val handshakeIntervalSec: Int = 60,
    val obfuscationEnabled: Boolean = false,
    val splitTunnelEnabled: Boolean = false,
    val multiHopServer: VpnServer? = null,
    val aiReasoning: String = "AI optimized connection for low latency and high encryption based on network probe results.",
    val aiSecurityScore: Int = 96,
    val calculatedLatencyMs: Int = 24,
    val timestamp: Long = System.currentTimeMillis()
)

enum class VpnStatus {
    DISCONNECTED,
    TESTING_NETWORK,
    AI_ANALYZING,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

data class VpnMetrics(
    val uploadBytesPerSec: Long = 0L,
    val downloadBytesPerSec: Long = 0L,
    val totalUploadedBytes: Long = 0L,
    val totalDownloadedBytes: Long = 0L,
    val currentPingMs: Int = 0,
    val packetLossPercent: Float = 0.0f,
    val activeCipher: String = "ChaCha20-Poly1305",
    val connectedTimeSec: Long = 0L
)

data class SecurityDiagnosticResult(
    val publicIp: String = "185.220.101.5",
    val originalIp: String = "72.14.201.88",
    val isIpMasked: Boolean = true,
    val dnsServers: List<String> = listOf("1.1.1.1 (Cloudflare Secured)", "9.9.9.9 (Quad9 Zero-Log)"),
    val isDnsSecured: Boolean = true,
    val isWebRtcLeaking: Boolean = false,
    val wifiSsid: String = "Home_5G_Protected",
    val wifiSecurityType: String = "WPA3-Personal",
    val isWifiPublicRisk: Boolean = false,
    val overallSecurityScore: Int = 98,
    val aiThreatAssessment: String = "Zero DNS/IP leaks detected. Network connection is securely encrypted with quantum-resistant key exchange."
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedConfig: AiVpnConfig? = null
)
