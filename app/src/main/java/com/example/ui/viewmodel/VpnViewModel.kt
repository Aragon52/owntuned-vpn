package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAgentManager
import com.example.data.db.VpnLogEntity
import com.example.data.models.AiVpnConfig
import com.example.data.models.AppInfo
import com.example.data.models.ChatMessage
import com.example.data.models.SecurityDiagnosticResult
import com.example.data.models.SecurityPreference
import com.example.data.models.ServerRegion
import com.example.data.models.VpnMetrics
import com.example.data.models.VpnProtocol
import com.example.data.models.VpnServer
import com.example.data.models.VpnStatus
import com.example.data.models.proxy.ProxyConfig
import com.example.data.models.proxy.ProxyProtocolType
import com.example.data.repository.ProxyConfigRepository
import com.example.data.repository.VpnRepository
import com.example.vpn.NexusVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VpnRepository(application)
    private val proxyRepository = ProxyConfigRepository()
    private val aiAgentManager = AiAgentManager()

    // Service status binding
    val isServiceRunning: StateFlow<Boolean> = NexusVpnService.isServiceRunning
    val serviceMetrics: StateFlow<VpnMetrics> = NexusVpnService.vpnMetrics

    // Proxy repository state
    val proxyConfigs: StateFlow<List<ProxyConfig>> = proxyRepository.configs
    val activeProxyConfig: StateFlow<ProxyConfig> = proxyRepository.activeConfig

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _vpnStatus = MutableStateFlow(VpnStatus.DISCONNECTED)
    val vpnStatus: StateFlow<VpnStatus> = _vpnStatus.asStateFlow()

    private val _serversList = MutableStateFlow<List<VpnServer>>(emptyList())
    val serversList: StateFlow<List<VpnServer>> = _serversList.asStateFlow()

    private val _activeServer = MutableStateFlow(
        VpnServer("us_ny_01", "US", "United States", "New York", "🇺🇸", "185.220.101.5", 34, 24, 24, supportsP2P = true, supportsStreaming = true, isStealth = true, region = ServerRegion.AMERICAS)
    )
    val activeServer: StateFlow<VpnServer> = _activeServer.asStateFlow()

    private val _activeProtocol = MutableStateFlow(VpnProtocol.VLESS_REALITY)
    val activeProtocol: StateFlow<VpnProtocol> = _activeProtocol.asStateFlow()

    private val _securityPreference = MutableStateFlow(SecurityPreference.STEALTH_ANTI_CENSORED)
    val securityPreference: StateFlow<SecurityPreference> = _securityPreference.asStateFlow()

    private val _aiConfig = MutableStateFlow<AiVpnConfig?>(null)
    val aiConfig: StateFlow<AiVpnConfig?> = _aiConfig.asStateFlow()

    private val _selectedRegionFilter = MutableStateFlow<ServerRegion?>(null)
    val selectedRegionFilter: StateFlow<ServerRegion?> = _selectedRegionFilter.asStateFlow()

    private val _serverSearchQuery = MutableStateFlow("")
    val serverSearchQuery: StateFlow<String> = _serverSearchQuery.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<SecurityDiagnosticResult?>(null)
    val diagnosticResult: StateFlow<SecurityDiagnosticResult?> = _diagnosticResult.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                sender = "AI",
                text = "Hello! I am your NexusVPN AI Security Agent. Supporting VLESS REALITY, VMess, Trojan, Hysteria 2, and WireGuard. Paste any vless://, vmess://, or trojan:// share link or ask me to optimize your stealth route!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatThinking = MutableStateFlow(false)
    val isChatThinking: StateFlow<Boolean> = _isChatThinking.asStateFlow()

    private val _killSwitchEnabled = MutableStateFlow(true)
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled.asStateFlow()

    private val _autoConnectOnBoot = MutableStateFlow(false)
    val autoConnectOnBoot: StateFlow<Boolean> = _autoConnectOnBoot.asStateFlow()

    private val _customMtu = MutableStateFlow(1420)
    val customMtu: StateFlow<Int> = _customMtu.asStateFlow()

    private val _vpnPermissionNeededIntent = MutableStateFlow<Intent?>(null)
    val vpnPermissionNeededIntent: StateFlow<Intent?> = _vpnPermissionNeededIntent.asStateFlow()

    val installedApps: StateFlow<List<AppInfo>> = repository.installedApps
    val logs: StateFlow<List<VpnLogEntity>> = repository.vpnLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            repository.servers.collect { list ->
                _serversList.value = list
                if (list.isNotEmpty() && _aiConfig.value == null) {
                    triggerAiOptimization("")
                }
            }
        }
    }

    // Proxy Switching & Import Actions
    fun selectProxyConfig(configId: String, context: Context? = null) {
        viewModelScope.launch {
            val selected = proxyRepository.selectConfig(configId)
            if (selected != null) {
                _activeProtocol.value = when (selected.protocolType) {
                    ProxyProtocolType.VLESS -> VpnProtocol.VLESS_REALITY
                    ProxyProtocolType.VMESS -> VpnProtocol.VMESS_AEAD
                    ProxyProtocolType.TROJAN -> VpnProtocol.TROJAN_GRPC
                }

                repository.logVpnAction(
                    actionType = "SWITCH_PROXY",
                    serverName = selected.name,
                    protocolName = selected.protocolType.displayName,
                    pingMs = selected.latencyMs,
                    notes = "Switched active proxy configuration (${selected.serverAddress}:${selected.serverPort})"
                )

                if (isServiceRunning.value && context != null) {
                    NexusVpnService.switchProxyProtocol(context, selected)
                }
            }
        }
    }

    fun importShareLink(link: String) {
        viewModelScope.launch {
            val result = proxyRepository.importShareLink(link)
            result.onSuccess { imported ->
                _importError.value = null
                repository.logVpnAction(
                    actionType = "IMPORT_PROXY",
                    serverName = imported.name,
                    protocolName = imported.protocolType.displayName,
                    pingMs = imported.latencyMs,
                    notes = "Imported share link for ${imported.serverAddress}"
                )
            }.onFailure { ex ->
                _importError.value = ex.message ?: "Invalid proxy URL"
            }
        }
    }

    fun testProxyLatencies() {
        viewModelScope.launch {
            proxyRepository.testAllLatencies()
        }
    }

    fun deleteProxyConfig(configId: String) {
        viewModelScope.launch {
            proxyRepository.deleteConfig(configId)
        }
    }

    fun dismissImportError() {
        _importError.value = null
    }

    fun onVpnPermissionGranted(context: Context) {
        _vpnPermissionNeededIntent.value = null
        startVpnServiceInternal(context)
    }

    fun toggleVpnConnection(context: Context) {
        if (_vpnStatus.value == VpnStatus.CONNECTED || isServiceRunning.value) {
            disconnectVpn(context)
        } else {
            connectVpn(context)
        }
    }

    fun connectVpn(context: Context) {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            _vpnPermissionNeededIntent.value = prepareIntent
        } else {
            startVpnServiceInternal(context)
        }
    }

    private fun startVpnServiceInternal(context: Context) {
        viewModelScope.launch {
            _vpnStatus.value = VpnStatus.CONNECTING
            val currentProxy = activeProxyConfig.value

            NexusVpnService.startWithProxyConfig(context, currentProxy)
            _vpnStatus.value = VpnStatus.CONNECTED

            repository.logVpnAction(
                actionType = "CONNECT",
                serverName = currentProxy.name,
                protocolName = currentProxy.protocolType.displayName,
                pingMs = currentProxy.latencyMs,
                notes = "Connected to ${currentProxy.serverAddress}:${currentProxy.serverPort}"
            )
        }
    }

    fun disconnectVpn(context: Context) {
        viewModelScope.launch {
            _vpnStatus.value = VpnStatus.DISCONNECTING
            NexusVpnService.stop(context)
            _vpnStatus.value = VpnStatus.DISCONNECTED

            repository.logVpnAction(
                actionType = "DISCONNECT",
                serverName = activeProxyConfig.value.name,
                protocolName = activeProxyConfig.value.protocolType.displayName,
                pingMs = activeProxyConfig.value.latencyMs,
                notes = "Disconnected by user"
            )
        }
    }

    fun triggerAiOptimization(userPrompt: String = "") {
        viewModelScope.launch {
            val previousStatus = _vpnStatus.value
            if (previousStatus != VpnStatus.CONNECTED) {
                _vpnStatus.value = VpnStatus.TESTING_NETWORK
            }

            val probedServers = repository.runNetworkLatencyProbe()
            _serversList.value = probedServers

            if (previousStatus != VpnStatus.CONNECTED) {
                _vpnStatus.value = VpnStatus.AI_ANALYZING
            }

            val generatedConfig = aiAgentManager.generateOptimizedConfig(
                preference = _securityPreference.value,
                availableServers = probedServers,
                networkType = "Wi-Fi (5 GHz / Low Jitter)",
                userInstructions = userPrompt
            )

            _aiConfig.value = generatedConfig
            _activeServer.value = generatedConfig.recommendedServer
            _activeProtocol.value = generatedConfig.recommendedProtocol
            _customMtu.value = generatedConfig.mtu

            if (previousStatus != VpnStatus.CONNECTED) {
                _vpnStatus.value = VpnStatus.DISCONNECTED
            }

            repository.logVpnAction(
                actionType = "AI_OPTIMIZE",
                serverName = generatedConfig.recommendedServer.city,
                protocolName = generatedConfig.recommendedProtocol.displayName,
                pingMs = generatedConfig.calculatedLatencyMs,
                notes = generatedConfig.aiReasoning
            )
        }
    }

    fun selectServer(server: VpnServer, context: Context? = null) {
        _activeServer.value = server
        val currentAi = _aiConfig.value
        if (currentAi != null) {
            _aiConfig.value = currentAi.copy(
                recommendedServer = server,
                calculatedLatencyMs = server.currentPingMs
            )
        }
        if (isServiceRunning.value && context != null) {
            connectVpn(context)
        }
    }

    fun selectProtocol(protocol: VpnProtocol, context: Context? = null) {
        _activeProtocol.value = protocol
        val currentAi = _aiConfig.value
        if (currentAi != null) {
            _aiConfig.value = currentAi.copy(recommendedProtocol = protocol)
        }
        if (isServiceRunning.value && context != null) {
            connectVpn(context)
        }
    }

    fun setSecurityPreference(preference: SecurityPreference, context: Context? = null) {
        _securityPreference.value = preference
        triggerAiOptimization("")
    }

    fun setRegionFilter(region: ServerRegion?) {
        _selectedRegionFilter.value = region
    }

    fun setSearchQuery(query: String) {
        _serverSearchQuery.value = query
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        if (userText.startsWith("vless://") || userText.startsWith("vmess://") || userText.startsWith("trojan://")) {
            importShareLink(userText)
        }

        val userMsg = ChatMessage(sender = "USER", text = userText)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatThinking.value = true

        viewModelScope.launch {
            val currentAi = _aiConfig.value ?: aiAgentManager.generateHeuristicConfig(_securityPreference.value, _serversList.value, userText)
            val reply = aiAgentManager.chatWithAi(
                chatHistory = _chatMessages.value,
                userMessage = userText,
                currentConfig = currentAi,
                availableServers = _serversList.value
            )
            _isChatThinking.value = false
            _chatMessages.value = _chatMessages.value + reply
        }
    }

    fun applyAiSuggestedConfig(config: AiVpnConfig, context: Context? = null) {
        _aiConfig.value = config
        _activeServer.value = config.recommendedServer
        _activeProtocol.value = config.recommendedProtocol
        _customMtu.value = config.mtu
        if (isServiceRunning.value && context != null) {
            connectVpn(context)
        }
    }

    fun runSecurityDiagnostic() {
        viewModelScope.launch {
            _isDiagnosing.value = true
            val isConnected = isServiceRunning.value || _vpnStatus.value == VpnStatus.CONNECTED
            val result = repository.runSecurityDiagnostic(isConnected, _activeServer.value.ipAddress)
            _diagnosticResult.value = result
            _isDiagnosing.value = false

            repository.logVpnAction(
                actionType = "LEAK_TEST",
                serverName = if (isConnected) _activeServer.value.city else "Exposed (No VPN)",
                protocolName = if (isConnected) _activeProtocol.value.displayName else "None",
                pingMs = _activeServer.value.currentPingMs,
                notes = "Diagnostic score: ${result.overallSecurityScore}/100"
            )
        }
    }

    fun toggleKillSwitch() {
        _killSwitchEnabled.value = !_killSwitchEnabled.value
    }

    fun toggleAutoConnect() {
        _autoConnectOnBoot.value = !_autoConnectOnBoot.value
    }

    fun toggleAppSplitTunnel(packageName: String) {
        repository.toggleAppSplitTunnel(packageName)
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
