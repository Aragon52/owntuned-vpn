package com.example.data.repository

import com.example.data.models.proxy.ProxyConfig
import com.example.data.models.proxy.ProxyProtocolType
import com.example.data.models.proxy.ProxySecurityType
import com.example.data.models.proxy.ProxyTransportType
import com.example.data.models.proxy.TrojanConfig
import com.example.data.models.proxy.VlessConfig
import com.example.data.models.proxy.VmessConfig
import com.example.data.parser.ProxyConfigParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ProxyConfigRepository {

    private val defaultConfigs: List<ProxyConfig> = listOf(
        VlessConfig(
            id = "vless_tokyo_reality",
            name = "Tokyo VLESS REALITY (XTLS Vision)",
            serverAddress = "jp-vless.nexus-vpn.net",
            serverPort = 443,
            uuid = "a8f3b2c1-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
            transport = ProxyTransportType.TCP,
            security = ProxySecurityType.REALITY,
            sni = "www.apple.com",
            publicKey = "q8X9aB7c...REALITY_KEY",
            shortId = "6ba7b810",
            latencyMs = 24,
            isSelected = true
        ),
        VlessConfig(
            id = "vless_singapore_grpc",
            name = "Singapore VLESS gRPC",
            serverAddress = "sg-vless.nexus-vpn.net",
            serverPort = 443,
            uuid = "b7c2a1f4-3e5d-6a7b-8c9d-0e1f2a3b4c5d",
            transport = ProxyTransportType.GRPC,
            security = ProxySecurityType.TLS,
            sni = "sg.cloud.google.com",
            serviceName = "vless-grpc-service",
            latencyMs = 31,
            isSelected = false
        ),
        VmessConfig(
            id = "vmess_frankfurt_ws",
            name = "Frankfurt VMess (WebSocket + TLS)",
            serverAddress = "de-vmess.nexus-vpn.net",
            serverPort = 443,
            uuid = "c6d1e2f3-4a5b-6c7d-8e9f-0a1b2c3d4e5f",
            alterId = 0,
            cipher = "aes-128-gcm",
            transport = ProxyTransportType.WS,
            security = ProxySecurityType.TLS,
            sni = "de.nexus-cdn.com",
            path = "/vmess-ws-tunnel",
            latencyMs = 78,
            isSelected = false
        ),
        TrojanConfig(
            id = "trojan_london_grpc",
            name = "London Trojan (gRPC HTTPS)",
            serverAddress = "uk-trojan.nexus-vpn.net",
            serverPort = 443,
            password = "trojan_secret_pass_2026",
            transport = ProxyTransportType.GRPC,
            security = ProxySecurityType.TLS,
            sni = "uk-web.microsoft.com",
            serviceName = "trojan-service-node",
            latencyMs = 84,
            isSelected = false
        )
    )

    private val _configs = MutableStateFlow<List<ProxyConfig>>(defaultConfigs)
    val configs: StateFlow<List<ProxyConfig>> = _configs.asStateFlow()

    private val _activeConfig = MutableStateFlow<ProxyConfig>(defaultConfigs.first())
    val activeConfig: StateFlow<ProxyConfig> = _activeConfig.asStateFlow()

    suspend fun selectConfig(configId: String): ProxyConfig? {
        return withContext(Dispatchers.Default) {
            val currentList = _configs.value
            val target = currentList.find { it.id == configId } ?: return@withContext null
            val updated = currentList.map { config ->
                when (config) {
                    is VlessConfig -> config.copy(isSelected = config.id == configId)
                    is VmessConfig -> config.copy(isSelected = config.id == configId)
                    is TrojanConfig -> config.copy(isSelected = config.id == configId)
                }
            }
            _configs.value = updated
            val newActive = updated.find { it.id == configId } ?: target
            _activeConfig.value = newActive
            newActive
        }
    }

    suspend fun importShareLink(link: String): Result<ProxyConfig> {
        return withContext(Dispatchers.Default) {
            val parsed = ProxyConfigParser.parseShareUrl(link)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid VLESS, VMess, or Trojan URL format"))

            val currentList = _configs.value
            _configs.value = currentList + parsed
            selectConfig(parsed.id)
            Result.success(parsed)
        }
    }

    suspend fun addCustomConfig(config: ProxyConfig) {
        withContext(Dispatchers.Default) {
            _configs.value = _configs.value + config
            selectConfig(config.id)
        }
    }

    suspend fun deleteConfig(configId: String) {
        withContext(Dispatchers.Default) {
            val updated = _configs.value.filter { it.id != configId }
            _configs.value = updated
            if (_activeConfig.value.id == configId && updated.isNotEmpty()) {
                selectConfig(updated.first().id)
            }
        }
    }

    suspend fun testAllLatencies(): List<ProxyConfig> {
        return withContext(Dispatchers.IO) {
            delay(800L) // Simulate network latency probing round-trip
            val updated = _configs.value.map { config ->
                val jitter = (-4..6).random()
                val newLatency = (config.latencyMs + jitter).coerceAtLeast(15)
                when (config) {
                    is VlessConfig -> config.copy(latencyMs = newLatency)
                    is VmessConfig -> config.copy(latencyMs = newLatency)
                    is TrojanConfig -> config.copy(latencyMs = newLatency)
                }
            }
            _configs.value = updated
            val currentActiveId = _activeConfig.value.id
            updated.find { it.id == currentActiveId }?.let { _activeConfig.value = it }
            updated
        }
    }
}
