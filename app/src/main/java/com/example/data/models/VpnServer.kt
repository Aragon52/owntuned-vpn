package com.example.data.models

data class VpnServer(
    val id: String,
    val countryCode: String,
    val countryName: String,
    val city: String,
    val flagEmoji: String,
    val ipAddress: String,
    val loadPercentage: Int, // 0 to 100%
    val basePingMs: Int,
    var currentPingMs: Int = basePingMs,
    val supportsP2P: Boolean = true,
    val supportsStreaming: Boolean = true,
    val isStealth: Boolean = false,
    val supportsVless: Boolean = true,
    val supportsVmess: Boolean = true,
    val supportsTrojan: Boolean = true,
    val supportsHysteria: Boolean = true,
    val securityScore: Int = 98,
    val region: ServerRegion = ServerRegion.AMERICAS
)

enum class ServerRegion(val displayName: String) {
    AMERICAS("Americas"),
    EUROPE("Europe"),
    ASIA_PACIFIC("Asia Pacific"),
    MIDDLE_EAST_AFRICA("Middle East & Africa")
}
