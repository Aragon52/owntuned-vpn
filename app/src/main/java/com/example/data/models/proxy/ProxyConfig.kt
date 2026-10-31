package com.example.data.models.proxy

sealed interface ProxyConfig {
    val id: String
    val name: String
    val serverAddress: String
    val serverPort: Int
    val protocolType: ProxyProtocolType
    val transport: ProxyTransportType
    val security: ProxySecurityType
    val sni: String
    val latencyMs: Int
    val isSelected: Boolean

    fun toShareUrl(): String
}

enum class ProxyProtocolType(val displayName: String) {
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan")
}

enum class ProxyTransportType(val wireName: String) {
    TCP("tcp"),
    WS("ws"),
    GRPC("grpc"),
    QUIC("quic")
}

enum class ProxySecurityType(val wireName: String) {
    NONE("none"),
    TLS("tls"),
    REALITY("reality")
}

data class VlessConfig(
    override val id: String = java.util.UUID.randomUUID().toString(),
    override val name: String,
    override val serverAddress: String,
    override val serverPort: Int,
    override val transport: ProxyTransportType = ProxyTransportType.TCP,
    override val security: ProxySecurityType = ProxySecurityType.REALITY,
    override val sni: String = "www.microsoft.com",
    override val latencyMs: Int = 24,
    override val isSelected: Boolean = false,
    val uuid: String,
    val flow: String = "xtls-rprx-vision",
    val encryption: String = "none",
    val publicKey: String = "", // REALITY X25519 public key
    val shortId: String = "",
    val path: String = "", // For WS transport
    val serviceName: String = "" // For gRPC transport
) : ProxyConfig {
    override val protocolType: ProxyProtocolType = ProxyProtocolType.VLESS

    override fun toShareUrl(): String {
        val params = mutableListOf<String>()
        params.add("type=${transport.wireName}")
        params.add("security=${security.wireName}")
        if (sni.isNotBlank()) params.add("sni=$sni")
        if (flow.isNotBlank()) params.add("flow=$flow")
        if (publicKey.isNotBlank()) params.add("pbk=$publicKey")
        if (shortId.isNotBlank()) params.add("sid=$shortId")
        if (path.isNotBlank()) params.add("path=$path")
        if (serviceName.isNotBlank()) params.add("serviceName=$serviceName")
        val queryString = params.joinToString("&")
        return "vless://$uuid@$serverAddress:$serverPort?$queryString#${android.net.Uri.encode(name)}"
    }
}

data class VmessConfig(
    override val id: String = java.util.UUID.randomUUID().toString(),
    override val name: String,
    override val serverAddress: String,
    override val serverPort: Int,
    override val transport: ProxyTransportType = ProxyTransportType.WS,
    override val security: ProxySecurityType = ProxySecurityType.TLS,
    override val sni: String = "",
    override val latencyMs: Int = 38,
    override val isSelected: Boolean = false,
    val uuid: String,
    val alterId: Int = 0,
    val cipher: String = "auto", // auto, aes-128-gcm, chacha20-poly1305
    val path: String = "/vmess-ws",
    val host: String = ""
) : ProxyConfig {
    override val protocolType: ProxyProtocolType = ProxyProtocolType.VMESS

    override fun toShareUrl(): String {
        val json = org.json.JSONObject().apply {
            put("v", "2")
            put("ps", name)
            put("add", serverAddress)
            put("port", serverPort)
            put("id", uuid)
            put("aid", alterId)
            put("scy", cipher)
            put("net", transport.wireName)
            put("type", "none")
            put("host", host.ifBlank { sni })
            put("path", path)
            put("tls", if (security == ProxySecurityType.TLS) "tls" else "none")
            put("sni", sni)
        }
        val b64 = android.util.Base64.encodeToString(json.toString().toByteArray(), android.util.Base64.NO_WRAP)
        return "vmess://$b64"
    }
}

data class TrojanConfig(
    override val id: String = java.util.UUID.randomUUID().toString(),
    override val name: String,
    override val serverAddress: String,
    override val serverPort: Int,
    override val transport: ProxyTransportType = ProxyTransportType.GRPC,
    override val security: ProxySecurityType = ProxySecurityType.TLS,
    override val sni: String = "",
    override val latencyMs: Int = 32,
    override val isSelected: Boolean = false,
    val password: String,
    val serviceName: String = "trojan-grpc",
    val alpn: String = "h2,http/1.1"
) : ProxyConfig {
    override val protocolType: ProxyProtocolType = ProxyProtocolType.TROJAN

    override fun toShareUrl(): String {
        val params = mutableListOf<String>()
        params.add("type=${transport.wireName}")
        params.add("security=${security.wireName}")
        if (sni.isNotBlank()) params.add("sni=$sni")
        if (serviceName.isNotBlank()) params.add("serviceName=$serviceName")
        if (alpn.isNotBlank()) params.add("alpn=$alpn")
        val queryString = params.joinToString("&")
        return "trojan://$password@$serverAddress:$serverPort?$queryString#${android.net.Uri.encode(name)}"
    }
}
