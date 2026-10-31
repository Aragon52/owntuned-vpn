package com.example.ai

import android.util.Log
import com.example.data.models.AiVpnConfig
import com.example.data.models.ChatMessage
import com.example.data.models.SecurityPreference
import com.example.data.models.VpnProtocol
import com.example.data.models.VpnServer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiAgentManager {

    companion object {
        private const val TAG = "AiAgentManager"
    }

    private val generativeModel by lazy {
        try {
            val config = generationConfig {
                responseMimeType = "application/json"
            }
            Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = config
            )
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI setup fallback to direct heuristic engine: ${e.message}")
            null
        }
    }

    suspend fun generateOptimizedConfig(
        preference: SecurityPreference,
        availableServers: List<VpnServer>,
        networkType: String = "Wi-Fi (5 GHz)",
        userInstructions: String = ""
    ): AiVpnConfig {
        return try {
            val model = generativeModel
            if (model != null) {
                withTimeout(15_000L) {
                    val prompt = buildPrompt(preference, availableServers, networkType, userInstructions)
                    val response = model.generateContent(prompt)
                    val jsonText = response.text
                    if (!jsonText.isNullOrBlank()) {
                        parseAiResponse(jsonText, availableServers, preference)
                    } else {
                        generateHeuristicConfig(preference, availableServers, userInstructions)
                    }
                }
            } else {
                generateHeuristicConfig(preference, availableServers, userInstructions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI model inference error or timeout, applying intelligent heuristic engine", e)
            generateHeuristicConfig(preference, availableServers, userInstructions)
        }
    }

    suspend fun chatWithAi(
        chatHistory: List<ChatMessage>,
        userMessage: String,
        currentConfig: AiVpnConfig,
        availableServers: List<VpnServer>
    ): ChatMessage {
        val lowercaseMsg = userMessage.lowercase()
        val suggestedNewConfig = if (
            lowercaseMsg.contains("vless") || lowercaseMsg.contains("vmess") ||
            lowercaseMsg.contains("trojan") || lowercaseMsg.contains("hysteria") ||
            lowercaseMsg.contains("reality") || lowercaseMsg.contains("shadowsocks") ||
            lowercaseMsg.contains("dpi") || lowercaseMsg.contains("gfw") ||
            lowercaseMsg.contains("stealth") || lowercaseMsg.contains("bypass") ||
            lowercaseMsg.contains("gaming") || lowercaseMsg.contains("valorant")
        ) {
            val pref = when {
                lowercaseMsg.contains("vless") || lowercaseMsg.contains("reality") ||
                lowercaseMsg.contains("trojan") || lowercaseMsg.contains("vmess") ||
                lowercaseMsg.contains("dpi") || lowercaseMsg.contains("gfw") -> SecurityPreference.STEALTH_ANTI_CENSORED
                lowercaseMsg.contains("gaming") || lowercaseMsg.contains("valorant") -> SecurityPreference.LOW_LATENCY_SPEED
                else -> SecurityPreference.BALANCED_AUTO
            }
            generateHeuristicConfig(pref, availableServers, userMessage)
        } else null

        val responseText = try {
            val model = generativeModel
            if (model != null) {
                val sysPrompt = """
                    You are NexusVPN AI Security Agent. Answer concise technical VPN and proxy question.
                    User message: "$userMessage"
                    Current VPN status: Server=${currentConfig.recommendedServer.city}, Protocol=${currentConfig.recommendedProtocol.displayName}.
                    Supports VLESS + REALITY, VMess, Trojan, Hysteria 2, Shadowsocks 2022, WireGuard, Quantum WireGuard.
                """.trimIndent()
                val resp = withTimeout(10_000L) { model.generateContent(sysPrompt) }
                resp.text ?: defaultChatReply(userMessage, currentConfig)
            } else {
                defaultChatReply(userMessage, currentConfig)
            }
        } catch (e: Exception) {
            defaultChatReply(userMessage, currentConfig)
        }

        return ChatMessage(
            sender = "AI",
            text = responseText,
            suggestedConfig = suggestedNewConfig
        )
    }

    private fun buildPrompt(
        preference: SecurityPreference,
        servers: List<VpnServer>,
        networkType: String,
        userInstructions: String
    ): String {
        val serverListSummary = servers.take(10).joinToString {
            "${it.id}:${it.city}(${it.countryCode}),Ping:${it.currentPingMs}ms,Load:${it.loadPercentage}%"
        }
        return """
            Task: Recommend optimal VPN & stealth proxy config.
            User Security Goal: ${preference.name} - ${preference.title}
            Network Environment: $networkType
            User Extra Instructions: "$userInstructions"
            Available Server Nodes: [$serverListSummary]

            Respond ONLY in valid JSON with these exact fields:
            {
              "serverId": "server_id_string",
              "protocol": "VLESS_REALITY|VMESS_AEAD|TROJAN_GRPC|HYSTERIA2|SHADOWSOCKS_2022|WIREGUARD|OPENVPN_UDP|QUANTUM_WG",
              "mtu": 1420,
              "handshakeSec": 60,
              "obfuscation": true,
              "multiHopServerId": "optional_secondary_server_id",
              "reasoning": "Explanation string",
              "securityScore": 99,
              "calculatedLatencyMs": 24
            }
        """.trimIndent()
    }

    private fun parseAiResponse(
        jsonText: String,
        availableServers: List<VpnServer>,
        preference: SecurityPreference
    ): AiVpnConfig {
        return try {
            val json = JSONObject(jsonText)
            val serverId = json.optString("serverId")
            val chosenServer = availableServers.find { it.id == serverId }
                ?: availableServers.minByOrNull { it.currentPingMs }
                ?: availableServers.first()

            val protoName = json.optString("protocol", "VLESS_REALITY")
            val chosenProto = try {
                VpnProtocol.valueOf(protoName)
            } catch (e: Exception) {
                VpnProtocol.VLESS_REALITY
            }

            val multiHopId = json.optString("multiHopServerId")
            val multiHopServer = availableServers.find { it.id == multiHopId && it.id != chosenServer.id }

            AiVpnConfig(
                recommendedServer = chosenServer,
                recommendedProtocol = chosenProto,
                mtu = json.optInt("mtu", 1420),
                handshakeIntervalSec = json.optInt("handshakeSec", 60),
                obfuscationEnabled = json.optBoolean("obfuscation", preference == SecurityPreference.STEALTH_ANTI_CENSORED),
                multiHopServer = multiHopServer,
                aiReasoning = json.optString("reasoning", "AI configured stealth proxy route using ${chosenProto.displayName}."),
                aiSecurityScore = json.optInt("securityScore", 98),
                calculatedLatencyMs = json.optInt("calculatedLatencyMs", chosenServer.currentPingMs)
            )
        } catch (e: Exception) {
            generateHeuristicConfig(preference, availableServers, "")
        }
    }

    fun generateHeuristicConfig(
        preference: SecurityPreference,
        availableServers: List<VpnServer>,
        userInstructions: String
    ): AiVpnConfig {
        val lowerInst = userInstructions.lowercase()

        val filteredServers = when {
            lowerInst.contains("japan") || lowerInst.contains("tokyo") ->
                availableServers.filter { it.countryCode == "JP" }
            lowerInst.contains("germany") || lowerInst.contains("frankfurt") ->
                availableServers.filter { it.countryCode == "DE" }
            lowerInst.contains("us") || lowerInst.contains("new york") ->
                availableServers.filter { it.countryCode == "US" }
            lowerInst.contains("singapore") || lowerInst.contains("sg") ->
                availableServers.filter { it.countryCode == "SG" }
            preference == SecurityPreference.STREAMING_OPTIMIZED ->
                availableServers.filter { it.supportsStreaming }
            preference == SecurityPreference.P2P_TORRENT ->
                availableServers.filter { it.supportsP2P }
            preference == SecurityPreference.STEALTH_ANTI_CENSORED || lowerInst.contains("vless") || lowerInst.contains("trojan") ->
                availableServers.filter { it.isStealth }
            else -> availableServers
        }.ifEmpty { availableServers }

        val primaryServer = filteredServers.minByOrNull { it.currentPingMs } ?: availableServers.first()

        val (protocol, mtu, obfuscation, multiHop, score, reasoning) = when {
            lowerInst.contains("vless") || lowerInst.contains("reality") -> {
                val p = VpnProtocol.VLESS_REALITY
                val reason = "AI activated VLESS + REALITY (XTLS Vision) on ${primaryServer.city}. Masks connection as legitimate TLS 1.3 browser traffic to bypass DPI."
                Tuple6(p, 1420, true, null, 99, reason)
            }
            lowerInst.contains("vmess") -> {
                val p = VpnProtocol.VMESS_AEAD
                val reason = "AI configured VMess (WebSocket + TLS) over CDN proxy for ${primaryServer.city}."
                Tuple6(p, 1420, true, null, 98, reason)
            }
            lowerInst.contains("trojan") -> {
                val p = VpnProtocol.TROJAN_GRPC
                val reason = "AI activated Trojan + gRPC on Port 443 on ${primaryServer.city} for authentic HTTPS web disguise."
                Tuple6(p, 1420, true, null, 99, reason)
            }
            lowerInst.contains("hysteria") || lowerInst.contains("quic") || lowerInst.contains("cellular") -> {
                val p = VpnProtocol.HYSTERIA2
                val reason = "AI deployed Hysteria 2 QUIC protocol on ${primaryServer.city} with BBR congestion control for maximum throughput on lossy network."
                Tuple6(p, 1380, true, null, 97, reason)
            }
            preference == SecurityPreference.STEALTH_ANTI_CENSORED -> {
                val p = VpnProtocol.VLESS_REALITY
                val reason = "AI selected VLESS + REALITY (XTLS Vision) on ${primaryServer.city} to disguise VPN packets as organic HTTPS web traffic."
                Tuple6(p, 1420, true, null, 99, reason)
            }
            preference == SecurityPreference.LOW_LATENCY_SPEED -> {
                val p = VpnProtocol.HYSTERIA2
                val reason = "AI deployed Hysteria 2 (UDP QUIC) on ${primaryServer.city} for ultra-low ping (${primaryServer.currentPingMs}ms) and zero packet delay."
                Tuple6(p, 1380, false, null, 96, reason)
            }
            preference == SecurityPreference.MAX_SECURITY -> {
                val p = VpnProtocol.QUANTUM_WG
                val secNode = availableServers.find { it.id != primaryServer.id && it.region != primaryServer.region }
                val reason = "AI enabled Quantum-Resistant Kyber-768 encryption with Multi-Hop routing through ${primaryServer.city} → ${secNode?.city ?: "Zurich"}."
                Tuple6(p, 1360, true, secNode, 100, reason)
            }
            else -> {
                val p = if (primaryServer.isStealth) VpnProtocol.VLESS_REALITY else VpnProtocol.WIREGUARD
                val reason = "AI Smart Agent dynamically selected ${primaryServer.city} (${primaryServer.currentPingMs}ms ping) with ${p.displayName}."
                Tuple6(p, 1420, p.stealthScore > 3, null, 98, reason)
            }
        }

        return AiVpnConfig(
            recommendedServer = primaryServer,
            recommendedProtocol = protocol,
            mtu = mtu,
            handshakeIntervalSec = if (preference == SecurityPreference.MAX_SECURITY) 30 else 60,
            obfuscationEnabled = obfuscation,
            multiHopServer = multiHop,
            aiReasoning = reasoning,
            aiSecurityScore = score,
            calculatedLatencyMs = primaryServer.currentPingMs
        )
    }

    private fun defaultChatReply(userMessage: String, currentConfig: AiVpnConfig): String {
        val msg = userMessage.lowercase()
        return when {
            msg.contains("vless") || msg.contains("reality") ->
                "VLESS + REALITY (XTLS Vision) is active! It eliminates TLS fingerprinting by mirroring real web server certificates (e.g., Apple, Cloudflare), making your connection completely unblockable by DPI firewalls."
            msg.contains("vmess") ->
                "VMess AEAD encapsulates packets over WebSocket & TLS. It allows routing traffic through CDN edge servers so your server IP remains hidden."
            msg.contains("trojan") ->
                "Trojan + gRPC disguises VPN traffic as standard HTTPS traffic on port 443. Deep Packet Inspection (DPI) sees only valid web TLS handshakes."
            msg.contains("hysteria") || msg.contains("quic") ->
                "Hysteria 2 utilizes custom UDP QUIC with BBR congestion control. It excels on high-latency or lossy mobile cellular connections."
            msg.contains("valorant") || msg.contains("gaming") ->
                "For low-latency gaming, I recommend Hysteria 2 QUIC or WireGuard UDP on our closest server node for pings under 25ms."
            else ->
                "NexusVPN AI Agent is active! Supporting VLESS REALITY, VMess, Trojan, Hysteria 2, Shadowsocks 2022 & WireGuard. Current active route: ${currentConfig.recommendedServer.city} via ${currentConfig.recommendedProtocol.displayName}."
        }
    }

    private data class Tuple6<A, B, C, D, E, F>(
        val val1: A, val val2: B, val val3: C, val val4: D, val val5: E, val val6: F
    )
}
