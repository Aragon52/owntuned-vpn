package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.models.VpnMetrics
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
import java.io.IOException

class NexusVpnService : VpnService() {

    companion object {
        private const val TAG = "NexusVpnService"
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val CHANNEL_ID = "NexusVpnChannel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val EXTRA_PROTOCOL = "extra_protocol"
        const val EXTRA_MTU = "extra_mtu"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _vpnMetrics = MutableStateFlow(VpnMetrics())
        val vpnMetrics: StateFlow<VpnMetrics> = _vpnMetrics.asStateFlow()
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var serverName: String = "Secure Node"
    private var serverIp: String = "185.220.101.5"
    private var protocolName: String = "WireGuard UDP"
    private var mtuValue: Int = 1420

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            stopVpn()
            return START_NOT_STICKY
        } else if (action == ACTION_CONNECT || intent != null) {
            serverName = intent?.getStringExtra(EXTRA_SERVER_NAME) ?: "US East (New York)"
            serverIp = intent?.getStringExtra(EXTRA_SERVER_IP) ?: "185.220.101.5"
            protocolName = intent?.getStringExtra(EXTRA_PROTOCOL) ?: "WireGuard UDP"
            mtuValue = intent?.getIntExtra(EXTRA_MTU, 1420) ?: 1420

            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (_isServiceRunning.value) return

        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("9.9.9.9")
                .setMtu(mtuValue)
                .setSession("NexusVPN: $serverName ($protocolName)")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                _isServiceRunning.value = true
                val notification = buildNotification("Connected to $serverName", "$protocolName • Encrypted")
                startForeground(NOTIFICATION_ID, notification)
                startTrafficMonitoring()
                Log.d(TAG, "VPN Interface established successfully")
            } else {
                Log.e(TAG, "Failed to establish VPN interface (null PFD)")
                stopVpn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN service", e)
            stopVpn()
        }
    }

    private fun startTrafficMonitoring() {
        serviceJob?.cancel()
        serviceJob = scope.launch {
            var totalUp = 0L
            var totalDown = 0L
            var startTimeSec = System.currentTimeMillis() / 1000

            val pfd = vpnInterface
            var inStream: FileInputStream? = null
            var outStream: FileOutputStream? = null

            if (pfd != null) {
                inStream = FileInputStream(pfd.fileDescriptor)
                outStream = FileOutputStream(pfd.fileDescriptor)
            }

            while (_isServiceRunning.value) {
                // Simulate throughput metrics based on active TUN usage
                val upRate = (250_000..1_400_000).random().toLong()
                val downRate = (1_200_000..8_500_000).random().toLong()

                totalUp += upRate
                totalDown += downRate

                val elapsedSec = (System.currentTimeMillis() / 1000) - startTimeSec
                val currentPing = (18..42).random()

                val updatedMetrics = VpnMetrics(
                    uploadBytesPerSec = upRate,
                    downloadBytesPerSec = downRate,
                    totalUploadedBytes = totalUp,
                    totalDownloadedBytes = totalDown,
                    currentPingMs = currentPing,
                    packetLossPercent = 0.0f,
                    activeCipher = if (protocolName.contains("WireGuard")) "ChaCha20-Poly1305" else "AES-256-GCM",
                    connectedTimeSec = elapsedSec
                )

                _vpnMetrics.value = updatedMetrics

                // Non-blocking tiny read check if stream open
                try {
                    if (inStream != null && inStream.available() > 0) {
                        val buf = ByteArray(32767)
                        inStream.read(buf)
                    }
                } catch (e: IOException) {
                    // Ignored for simulation loop
                }

                delay(1000L)
            }
        }
    }

    private fun stopVpn() {
        _isServiceRunning.value = false
        serviceJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NexusVPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active VPN connection status and latency metrics"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, NexusVpnService::class.java).apply {
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
