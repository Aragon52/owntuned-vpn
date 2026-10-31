package com.example.vpn

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.models.proxy.ProxyConfig
import com.example.data.models.proxy.ProxyProtocolType
import com.example.data.models.proxy.TrojanConfig
import com.example.data.models.proxy.VlessConfig
import com.example.data.models.proxy.VmessConfig

class NexusVpnService : BaseNexusVpnService() {

    companion object {
        private const val TAG = "NexusVpnService"

        val isServiceRunning = isServiceActive
        val vpnMetrics = liveMetrics

        fun startWithProxyConfig(context: Context, proxyConfig: ProxyConfig) {
            val intent = Intent(context, NexusVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_CONFIG_ID, proxyConfig.id)
                putExtra(EXTRA_SERVER_NAME, proxyConfig.name)
                putExtra(EXTRA_SERVER_IP, proxyConfig.serverAddress)
                putExtra(EXTRA_PROTOCOL_TYPE, proxyConfig.protocolType.name)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun switchProxyProtocol(context: Context, newConfig: ProxyConfig) {
            val intent = Intent(context, NexusVpnService::class.java).apply {
                action = ACTION_SWITCH_PROTOCOL
                putExtra(EXTRA_CONFIG_ID, newConfig.id)
                putExtra(EXTRA_SERVER_NAME, newConfig.name)
                putExtra(EXTRA_SERVER_IP, newConfig.serverAddress)
                putExtra(EXTRA_PROTOCOL_TYPE, newConfig.protocolType.name)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NexusVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    override fun onConfigureTunBuilder(builder: Builder, protocolType: String) {
        super.onConfigureTunBuilder(builder, protocolType)
        when (protocolType.uppercase()) {
            "VLESS" -> {
                Log.d(TAG, "Configuring VLESS REALITY (XTLS Vision) socket routes")
                builder.addDnsServer("1.1.1.1")
                builder.addDnsServer("9.9.9.9")
            }
            "VMESS" -> {
                Log.d(TAG, "Configuring VMess WebSocket CDN proxy routes")
                builder.addDnsServer("8.8.8.8")
                builder.addDnsServer("1.0.0.1")
            }
            "TROJAN" -> {
                Log.d(TAG, "Configuring Trojan gRPC HTTPS disguise routes")
                builder.addDnsServer("1.1.1.1")
                builder.addDnsServer("8.8.4.4")
            }
        }
    }

    override fun onProxyEngineStarted(protocolType: String) {
        super.onProxyEngineStarted(protocolType)
        Log.d(TAG, "NexusVpnService active using protocol engine: $protocolType")
    }

    override fun onSwitchProtocol(newConfig: ProxyConfig) {
        super.onSwitchProtocol(newConfig)
        Log.d(TAG, "Switched active tunnel configuration to ${newConfig.protocolType.displayName} -> ${newConfig.name}")
    }
}
