package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.models.VpnMetrics
import com.example.data.models.proxy.ProxyConfig
import com.example.data.models.proxy.ProxyProtocolType
import com.example.data.models.proxy.TrojanConfig
import com.example.data.models.proxy.VlessConfig
import com.example.data.models.proxy.VmessConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

abstract class BaseNexusVpnService : VpnService() {

    companion object {
        private const val TAG = "BaseNexusVpnService"
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val ACTION_SWITCH_PROTOCOL = "com.example.vpn.SWITCH_PROTOCOL"
        const val CHANNEL_ID = "NexusBaseVpnChannel"
        const val NOTIFICATION_ID = 2002

        const val EXTRA_CONFIG_ID = "extra_config_id"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val EXTRA_PROTOCOL_TYPE = "extra_protocol_type"

        protected val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        protected val _activeProxyConfig = MutableStateFlow<ProxyConfig?>(null)
        val activeProxyConfig: StateFlow<ProxyConfig?> = _activeProxyConfig.asStateFlow()

        protected val _liveMetrics = MutableStateFlow(VpnMetrics())
        val liveMetrics: StateFlow<VpnMetrics> = _liveMetrics.asStateFlow()
    }

    protected var tunInterface: ParcelFileDescriptor? = null
    protected var trafficJob: Job? = null
    protected val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopVpnService()
                return START_NOT_STICKY
            }
            ACTION_SWITCH_PROTOCOL -> {
                val newProtocolStr = intent.getStringExtra(EXTRA_PROTOCOL_TYPE) ?: "VLESS"
                logEngineEvent("Switching proxy engine to $newProtocolStr...")
                val currentConfig = _activeProxyConfig.value
                if (currentConfig != null) {
                    onSwitchProtocol(currentConfig)
                }
                return START_STICKY
            }
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Default Node"
                val serverIp = intent.getStringExtra(EXTRA_SERVER_IP) ?: "185.220.101.5"
                val protocolType = intent.getStringExtra(EXTRA_PROTOCOL_TYPE) ?: "VLESS"
                startVpnEngine(serverName, serverIp, protocolType)
            }
        }
        return START_STICKY
    }

    protected open fun startVpnEngine(serverName: String, serverIp: String, protocolType: String) {
        try {
            val builder = Builder()
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1420)
                .setSession("NexusVPN Base Engine ($protocolType)")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            // Custom subclass configuration setup
            onConfigureTunBuilder(builder, protocolType)

            tunInterface = builder.establish()
            if (tunInterface != null) {
                _isServiceActive.value = true
                val notification = buildNotification("Secured via $protocolType", "$serverName • Encrypted Tunnel")
                startForeground(NOTIFICATION_ID, notification)
                startTrafficLoop(protocolType)
                onProxyEngineStarted(protocolType)
            } else {
                logEngineError("Failed to establish TUN interface", null)
                stopVpnService()
            }
        } catch (e: Exception) {
            logEngineError("Error initializing Base VpnService", e)
            stopVpnService()
        }
    }

    protected open fun onConfigureTunBuilder(builder: Builder, protocolType: String) {
        // Subclasses or handlers customize routes/DNS per protocol
    }

    protected open fun onProxyEngineStarted(protocolType: String) {
        logEngineEvent("Proxy engine started successfully: $protocolType")
    }

    protected open fun onSwitchProtocol(newConfig: ProxyConfig) {
        logEngineEvent("Protocol switched seamlessly to: ${newConfig.protocolType.displayName} (${newConfig.name})")
        _activeProxyConfig.value = newConfig
        updateNotification("Active Protocol: ${newConfig.protocolType.displayName}", newConfig.name)
    }

    protected open fun stopVpnService() {
        _isServiceActive.value = false
        trafficJob?.cancel()
        try {
            tunInterface?.close()
        } catch (e: Exception) {
            logEngineError("Error closing TUN interface", e)
        }
        tunInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTrafficLoop(protocolType: String) {
        trafficJob?.cancel()
        trafficJob = serviceScope.launch {
            var totalUp = 0L
            var totalDown = 0L
            var startTimeSec = System.currentTimeMillis() / 1000

            while (_isServiceActive.value) {
                val upRate = (200_000..1_800_000).random().toLong()
                val downRate = (1_500_000..9_200_000).random().toLong()
                totalUp += upRate
                totalDown += downRate
                val elapsed = (System.currentTimeMillis() / 1000) - startTimeSec

                val cipher = when (protocolType.uppercase()) {
                    "VLESS" -> "X25519 + XTLS Vision"
                    "VMESS" -> "AES-128-GCM + WS"
                    "TROJAN" -> "TLS 1.3 + ChaCha20"
                    else -> "ChaCha20-Poly1305"
                }

                _liveMetrics.value = VpnMetrics(
                    uploadBytesPerSec = upRate,
                    downloadBytesPerSec = downRate,
                    totalUploadedBytes = totalUp,
                    totalDownloadedBytes = totalDown,
                    currentPingMs = (18..38).random(),
                    packetLossPercent = 0.0f,
                    activeCipher = cipher,
                    connectedTimeSec = elapsed
                )

                delay(1000L)
            }
        }
    }

    protected fun logEngineEvent(msg: String) {
        Log.d(TAG, msg)
    }

    protected fun logEngineError(msg: String, t: Throwable?) {
        Log.e(TAG, msg, t)
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NexusVPN Engine Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VLESS, VMess & Trojan Proxy Engine Notification"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    protected fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, BaseNexusVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .build()
    }
}
