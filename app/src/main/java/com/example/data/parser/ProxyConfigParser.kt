package com.example.data.parser

import android.net.Uri
import android.util.Base64
import com.example.data.models.proxy.ProxyConfig
import com.example.data.models.proxy.ProxyProtocolType
import com.example.data.models.proxy.ProxySecurityType
import com.example.data.models.proxy.ProxyTransportType
import com.example.data.models.proxy.TrojanConfig
import com.example.data.models.proxy.VlessConfig
import com.example.data.models.proxy.VmessConfig
import org.json.JSONObject

object ProxyConfigParser {

    fun parseShareUrl(url: String): ProxyConfig? {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVlessUrl(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmessUrl(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojanUrl(trimmed)
            else -> null
        }
    }

    private fun parseVlessUrl(url: String): VlessConfig? {
        return try {
            val uri = Uri.parse(url)
            val userInfo = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port > 0) uri.port else 443
            val name = uri.fragment?.let { Uri.decode(it) } ?: "VLESS - $host"

            val transportStr = uri.getQueryParameter("type") ?: "tcp"
            val securityStr = uri.getQueryParameter("security") ?: "reality"
            val sni = uri.getQueryParameter("sni") ?: uri.getQueryParameter("host") ?: host
            val flow = uri.getQueryParameter("flow") ?: "xtls-rprx-vision"
            val pbk = uri.getQueryParameter("pbk") ?: ""
            val sid = uri.getQueryParameter("sid") ?: ""
            val path = uri.getQueryParameter("path") ?: ""
            val serviceName = uri.getQueryParameter("serviceName") ?: ""

            VlessConfig(
                name = name,
                serverAddress = host,
                serverPort = port,
                uuid = userInfo,
                transport = parseTransport(transportStr),
                security = parseSecurity(securityStr),
                sni = sni,
                flow = flow,
                publicKey = pbk,
                shortId = sid,
                path = path,
                serviceName = serviceName
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVmessUrl(url: String): VmessConfig? {
        return try {
            val b64Content = url.substringAfter("vmess://").trim()
            val decodedJsonStr = String(Base64.decode(b64Content, Base64.DEFAULT or Base64.NO_WRAP))
            val json = JSONObject(decodedJsonStr)

            val name = json.optString("ps", "VMess Server")
            val host = json.optString("add", "127.0.0.1")
            val port = json.optInt("port", 443)
            val uuid = json.optString("id", "")
            val alterId = json.optInt("aid", 0)
            val cipher = json.optString("scy", "auto")
            val net = json.optString("net", "ws")
            val tls = json.optString("tls", "tls")
            val sni = json.optString("sni", json.optString("host", host))
            val path = json.optString("path", "/vmess-ws")

            VmessConfig(
                name = name,
                serverAddress = host,
                serverPort = port,
                uuid = uuid,
                alterId = alterId,
                cipher = cipher,
                transport = parseTransport(net),
                security = if (tls == "tls") ProxySecurityType.TLS else ProxySecurityType.NONE,
                sni = sni,
                path = path,
                host = json.optString("host", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTrojanUrl(url: String): TrojanConfig? {
        return try {
            val uri = Uri.parse(url)
            val password = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port > 0) uri.port else 443
            val name = uri.fragment?.let { Uri.decode(it) } ?: "Trojan - $host"

            val transportStr = uri.getQueryParameter("type") ?: "grpc"
            val securityStr = uri.getQueryParameter("security") ?: "tls"
            val sni = uri.getQueryParameter("sni") ?: host
            val serviceName = uri.getQueryParameter("serviceName") ?: "trojan-grpc"
            val alpn = uri.getQueryParameter("alpn") ?: "h2,http/1.1"

            TrojanConfig(
                name = name,
                serverAddress = host,
                serverPort = port,
                password = password,
                transport = parseTransport(transportStr),
                security = parseSecurity(securityStr),
                sni = sni,
                serviceName = serviceName,
                alpn = alpn
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTransport(str: String): ProxyTransportType {
        return when (str.lowercase()) {
            "ws", "websocket" -> ProxyTransportType.WS
            "grpc" -> ProxyTransportType.GRPC
            "quic" -> ProxyTransportType.QUIC
            else -> ProxyTransportType.TCP
        }
    }

    private fun parseSecurity(str: String): ProxySecurityType {
        return when (str.lowercase()) {
            "reality" -> ProxySecurityType.REALITY
            "tls" -> ProxySecurityType.TLS
            else -> ProxySecurityType.NONE
        }
    }
}
