package com.example.data.models

enum class SecurityPreference(
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    BALANCED_AUTO(
        title = "Smart AI Dynamic",
        subtitle = "AI dynamically tunes protocol & route based on real-time latency & risks",
        iconName = "AutoAwesome"
    ),
    LOW_LATENCY_SPEED(
        title = "Gaming & Low Latency",
        subtitle = "Prioritizes minimal ping (ms) and raw UDP throughput",
        iconName = "Speed"
    ),
    MAX_SECURITY(
        title = "Maximum Security & Multi-Hop",
        subtitle = "Quantum-resistant encryption, zero-log policies, double-hop routing",
        iconName = "Security"
    ),
    STEALTH_ANTI_CENSORED(
        title = "Stealth & Anti-Censorship",
        subtitle = "Masks VPN traffic as HTTPS to bypass strict DPI firewalls",
        iconName = "VisibilityOff"
    ),
    STREAMING_OPTIMIZED(
        title = "Streaming & Media Unblock",
        subtitle = "Bypasses geo-blocks with high bandwidth streaming nodes",
        iconName = "Movie"
    ),
    P2P_TORRENT(
        title = "P2P & Port Forwarding",
        subtitle = "Optimized for peer-to-peer sharing and port forwarding",
        iconName = "CloudDownload"
    )
}
