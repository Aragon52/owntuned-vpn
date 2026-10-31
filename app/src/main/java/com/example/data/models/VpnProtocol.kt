package com.example.data.models

enum class VpnProtocol(
    val displayName: String,
    val defaultPort: Int,
    val cipherName: String,
    val speedScore: Int, // 1 to 5
    val securityScore: Int, // 1 to 5
    val stealthScore: Int, // 1 to 5
    val description: String,
    val transportType: String
) {
    VLESS_REALITY(
        displayName = "VLESS + REALITY",
        defaultPort = 443,
        cipherName = "X25519 + TLS 1.3 Vision",
        speedScore = 5,
        securityScore = 5,
        stealthScore = 5,
        description = "Next-gen stealth protocol masking traffic as legitimate TLS 1.3 website handshakes to bypass strict GFW & DPI.",
        transportType = "TCP / XTLS Vision"
    ),
    VMESS_AEAD(
        displayName = "VMess (WebSocket + TLS)",
        defaultPort = 443,
        cipherName = "AES-128-GCM + WS",
        speedScore = 4,
        securityScore = 5,
        stealthScore = 5,
        description = "Encrypted VMess protocol over WebSocket & CDN proxies to bypass IP blocking.",
        transportType = "WebSocket / gRPC"
    ),
    TROJAN_GRPC(
        displayName = "Trojan + gRPC",
        defaultPort = 443,
        cipherName = "TLS 1.3 / ChaCha20",
        speedScore = 5,
        securityScore = 5,
        stealthScore = 5,
        description = "Disguises VPN traffic as authentic HTTPS web traffic to legitimate domain names.",
        transportType = "gRPC / TLS 1.3"
    ),
    HYSTERIA2(
        displayName = "Hysteria 2 (QUIC)",
        defaultPort = 8443,
        cipherName = "QUIC + BBR Congestion",
        speedScore = 5,
        securityScore = 4,
        stealthScore = 4,
        description = "Custom QUIC protocol engineered for maximum speed on lossy or throttled mobile networks.",
        transportType = "UDP QUIC"
    ),
    SHADOWSOCKS_2022(
        displayName = "Shadowsocks 2022",
        defaultPort = 8388,
        cipherName = "2022-blake3-chacha20-poly1305",
        speedScore = 5,
        securityScore = 5,
        stealthScore = 4,
        description = "Modernized 2022 AEAD spec with Blake3 hashing and fast session multiplexing.",
        transportType = "TCP / UDP"
    ),
    WIREGUARD(
        displayName = "WireGuard® UDP",
        defaultPort = 51820,
        cipherName = "ChaCha20-Poly1305",
        speedScore = 5,
        securityScore = 4,
        stealthScore = 2,
        description = "Ultra fast, lightweight kernel-level modern protocol.",
        transportType = "UDP"
    ),
    OPENVPN_UDP(
        displayName = "OpenVPN (UDP)",
        defaultPort = 1194,
        cipherName = "AES-256-GCM",
        speedScore = 4,
        securityScore = 5,
        stealthScore = 3,
        description = "Industry standard robust protocol with high security.",
        transportType = "UDP"
    ),
    QUANTUM_WG(
        displayName = "Quantum WireGuard",
        defaultPort = 51821,
        cipherName = "Kyber-768 + ChaCha20",
        speedScore = 4,
        securityScore = 5,
        stealthScore = 4,
        description = "Post-quantum hybrid handshake protection against future key decryption.",
        transportType = "UDP Hybrid"
    )
}
