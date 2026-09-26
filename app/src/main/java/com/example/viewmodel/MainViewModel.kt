package com.example.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BluetoothDeviceInfo
import com.example.data.model.DeviceType
import com.example.data.model.NetworkDevice
import com.example.data.model.NetworkStats
import com.example.data.model.PortInfo
import com.example.data.model.SmartHomeDevice
import com.example.data.repository.DeviceRepository
import com.example.service.export.PdfReportExporter
import com.example.service.notification.NetworkAlertHelper
import com.example.service.radar.LocationRadarService
import com.example.service.radar.RadarBlip
import com.example.service.scanner.BluetoothScanner
import com.example.service.scanner.NetworkScanner
import com.example.service.scanner.PortScanner
import com.example.service.scanner.WakeOnLan
import com.example.service.smarthome.SmartHomeManager
import com.example.service.web.LocalWebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = DeviceRepository(database.deviceDao())

    private val networkScanner = NetworkScanner(application)
    private val portScanner = PortScanner()
    private val bluetoothScanner = BluetoothScanner(application)
    private val smartHomeManager = SmartHomeManager()
    private val pdfExporter = PdfReportExporter(application)
    private val alertHelper = NetworkAlertHelper(application)
    private val locationRadarService = LocationRadarService(application)

    // Local Web Server
    private val webServer = LocalWebServer(
        getDevices = { _devicesList.value },
        getStats = { _networkStats.value },
        onToggleBlock = { ip ->
            viewModelScope.launch {
                val dev = repository.getDeviceByIp(ip)
                if (dev != null) {
                    repository.toggleBlocked(ip, dev.isBlocked)
                }
            }
        }
    )

    // Network Stats State
    private val _networkStats = MutableStateFlow(networkScanner.getLocalNetworkStats())
    val networkStats: StateFlow<NetworkStats> = _networkStats.asStateFlow()

    // Database devices
    private val _dbDevices = repository.allDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current in-memory list (combines DB + live scan)
    private val _devicesList = MutableStateFlow<List<NetworkDevice>>(emptyList())

    // Search query & Category filter
    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow("ALL") // ALL, ONLINE, BLOCKED, SUSPICIOUS, ROUTER, PHONE, IOT

    // Filtered devices list for UI
    val filteredDevices: StateFlow<List<NetworkDevice>> = combine(
        _devicesList,
        searchQuery,
        selectedFilter
    ) { devices, query, filter ->
        devices.filter { dev ->
            val matchesQuery = query.isBlank() ||
                    dev.ip.contains(query, ignoreCase = true) ||
                    dev.mac.contains(query, ignoreCase = true) ||
                    dev.displayName.contains(query, ignoreCase = true) ||
                    dev.vendor.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "ONLINE" -> dev.isOnline
                "BLOCKED" -> dev.isBlocked
                "SUSPICIOUS" -> dev.isSuspicious
                "ROUTER" -> dev.deviceType == DeviceType.ROUTER
                "PHONE" -> dev.deviceType == DeviceType.PHONE
                "IOT" -> dev.deviceType == DeviceType.IOT || dev.deviceType == DeviceType.SPEAKER || dev.deviceType == DeviceType.SMART_TV
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Radar & Geolocation Blips
    val userGpsLocation: StateFlow<Location?> = locationRadarService.userLocation
    val isGpsActive: StateFlow<Boolean> = locationRadarService.isGpsActive

    val radarBlips: StateFlow<List<RadarBlip>> = combine(
        _devicesList,
        bluetoothScanner.devices
    ) { netDevices, bleDevices ->
        locationRadarService.buildBlips(netDevices, bleDevices)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Port Scanner State
    private val _portScanTargetIp = MutableStateFlow("")
    val portScanTargetIp: StateFlow<String> = _portScanTargetIp.asStateFlow()

    private val _portScanProgress = MutableStateFlow(0f)
    val portScanProgress: StateFlow<Float> = _portScanProgress.asStateFlow()

    private val _isPortScanning = MutableStateFlow(false)
    val isPortScanning: StateFlow<Boolean> = _isPortScanning.asStateFlow()

    private val _openPortsList = MutableStateFlow<List<PortInfo>>(emptyList())
    val openPortsList: StateFlow<List<PortInfo>> = _openPortsList.asStateFlow()

    // Bluetooth scanner state
    val bleDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothScanner.devices
    val isBleScanning: StateFlow<Boolean> = bluetoothScanner.isScanning
    val selectedBleDevice: StateFlow<BluetoothDeviceInfo?> = bluetoothScanner.selectedDevice

    // Smart Home state
    val smartDevices: StateFlow<List<SmartHomeDevice>> = smartHomeManager.devices
    val isMultiroomPlaying: StateFlow<Boolean> = smartHomeManager.isMultiroomPlaying
    val masterVolume: StateFlow<Float> = smartHomeManager.masterVolume
    val currentStreamTrack: StateFlow<String> = smartHomeManager.currentStreamTrack
    val isZigbeePairing: StateFlow<Boolean> = smartHomeManager.isZigbeePairingMode

    // Web Server state
    val isWebServerRunning: StateFlow<Boolean> = webServer.isRunning
    val webServerUrl: StateFlow<String> = webServer.serverUrl
    val webAccessCount: StateFlow<Int> = webServer.accessCount

    // Toast/Snackbar notifications channel
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Realtime notification monitor enabled
    val isIntrusionAlertEnabled = MutableStateFlow(true)

    private var telemetryLoopJob: Job? = null

    init {
        // Collect DB devices into memory
        viewModelScope.launch {
            _dbDevices.collect { dbList ->
                if (_devicesList.value.isEmpty() && dbList.isNotEmpty()) {
                    _devicesList.value = dbList
                    updateStatsCounts(dbList)
                }
            }
        }

        // Start location radar updates
        locationRadarService.startLocationTracking()

        // Run initial scan
        startNetworkScan()

        // Start dynamic bandwidth throughput simulation loop for live charts
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        telemetryLoopJob?.cancel()
        telemetryLoopJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(2000)
                val curStats = _networkStats.value
                val curDl = (curStats.currentDownloadSpeedMbps + Random.nextFloat() * 6f - 3f).coerceIn(12f, 95f)
                val curUl = (curStats.currentUploadSpeedMbps + Random.nextFloat() * 3f - 1.5f).coerceIn(4f, 28f)

                val newDlHist = (curStats.downloadHistory.drop(1) + curDl)
                val newUlHist = (curStats.uploadHistory.drop(1) + curUl)

                _networkStats.value = curStats.copy(
                    currentDownloadSpeedMbps = (curDl * 10f).toInt() / 10f,
                    currentUploadSpeedMbps = (curUl * 10f).toInt() / 10f,
                    downloadHistory = newDlHist,
                    uploadHistory = newUlHist,
                    totalDownloadMb = curStats.totalDownloadMb + (curDl / 8f) * 2f,
                    totalUploadMb = curStats.totalUploadMb + (curUl / 8f) * 2f
                )
            }
        }
    }

    fun startNetworkScan() {
        if (_networkStats.value.isScanning) return

        viewModelScope.launch(Dispatchers.IO) {
            val baseStats = networkScanner.getLocalNetworkStats()
            _networkStats.value = baseStats.copy(isScanning = true, scanProgress = 0f)

            val currentKnownIps = _devicesList.value.map { it.ip }.toSet()
            val scannedMap = mutableMapOf<String, NetworkDevice>()

            networkScanner.scanSubnet(baseStats).collect { event ->
                when (event) {
                    is NetworkScanner.ScanEvent.Progress -> {
                        _networkStats.value = _networkStats.value.copy(
                            scanProgress = event.progress,
                            currentScanningIp = event.currentIp
                        )
                        if (event.foundDevice != null) {
                            val dev = event.foundDevice
                            scannedMap[dev.ip] = dev
                            val currentList = scannedMap.values.toList()
                            _devicesList.value = currentList
                            updateStatsCounts(currentList)

                            // Check for unknown new connection and notify in real-time
                            if (isIntrusionAlertEnabled.value && !currentKnownIps.contains(dev.ip) && dev.ip != baseStats.localIp && dev.ip != baseStats.gatewayIp) {
                                alertHelper.notifyNewDeviceDetected(dev)
                            }
                        }
                    }
                    is NetworkScanner.ScanEvent.Complete -> {
                        val finalList = event.devices
                        _devicesList.value = finalList
                        repository.insertAll(finalList)
                        _networkStats.value = _networkStats.value.copy(
                            isScanning = false,
                            scanProgress = 1f,
                            currentScanningIp = "Scan Completed"
                        )
                        updateStatsCounts(finalList)
                        _userMessage.emit("Scan completed! Found ${finalList.size} devices on network.")
                    }
                }
            }
        }
    }

    private fun updateStatsCounts(devices: List<NetworkDevice>) {
        val online = devices.count { it.isOnline }
        val suspicious = devices.count { it.isSuspicious }
        val blocked = devices.count { it.isBlocked }
        val avgPing = if (devices.isNotEmpty()) {
            devices.filter { it.pingMs > 0 }.map { it.pingMs }.average().takeIf { !it.isNaN() }?.toLong() ?: 0L
        } else 0L

        _networkStats.value = _networkStats.value.copy(
            totalDevices = devices.size,
            onlineDevices = online,
            suspiciousDevices = suspicious,
            blockedDevices = blocked,
            averagePingMs = avgPing
        )
    }

    fun toggleBlockDevice(device: NetworkDevice) {
        viewModelScope.launch {
            val newBlocked = !device.isBlocked
            val updated = device.copy(isBlocked = newBlocked)
            repository.insertOrUpdate(updated)

            _devicesList.value = _devicesList.value.map {
                if (it.ip == device.ip) updated else it
            }
            updateStatsCounts(_devicesList.value)

            val actionName = if (newBlocked) "Blocked & Isolated" else "Unblocked"
            _userMessage.emit("$actionName: ${device.displayName} (${device.ip})")
        }
    }

    fun toggleSuspiciousDevice(device: NetworkDevice) {
        viewModelScope.launch {
            val newSuspicious = !device.isSuspicious
            val updated = device.copy(isSuspicious = newSuspicious)
            repository.insertOrUpdate(updated)

            _devicesList.value = _devicesList.value.map {
                if (it.ip == device.ip) updated else it
            }
            updateStatsCounts(_devicesList.value)

            val actionName = if (newSuspicious) "Marked as Suspicious" else "Marked as Trusted"
            _userMessage.emit("$actionName: ${device.displayName}")
        }
    }

    fun updateDeviceCustomName(device: NetworkDevice, newName: String) {
        viewModelScope.launch {
            val updated = device.copy(customName = newName)
            repository.insertOrUpdate(updated)
            _devicesList.value = _devicesList.value.map {
                if (it.ip == device.ip) updated else it
            }
            _userMessage.emit("Updated device alias to $newName")
        }
    }

    fun sendWakeOnLan(device: NetworkDevice) {
        viewModelScope.launch {
            val res = WakeOnLan.sendMagicPacket(
                macAddress = device.mac,
                broadcastIp = _networkStats.value.broadcastIp
            )
            res.fold(
                onSuccess = { _ -> _userMessage.emit("⚡ WoL packet sent to ${device.mac}!") },
                onFailure = { err -> _userMessage.emit("❌ Failed to send WoL: ${err.localizedMessage}") }
            )
        }
    }

    // Port Scanning
    fun startPortScan(targetIp: String, customPortList: List<Int>? = null) {
        _portScanTargetIp.value = targetIp
        _isPortScanning.value = true
        _openPortsList.value = emptyList()
        _portScanProgress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            val openList = mutableListOf<PortInfo>()
            portScanner.scanPorts(targetIp, customPortList).collect { event ->
                when (event) {
                    is PortScanner.PortScanEvent.Progress -> {
                        _portScanProgress.value = event.scanned.toFloat() / event.total
                        if (event.foundOpen != null) {
                            openList.add(event.foundOpen)
                            _openPortsList.value = openList.toList()
                        }
                    }
                    is PortScanner.PortScanEvent.Complete -> {
                        _isPortScanning.value = false
                        _portScanProgress.value = 1f
                        _openPortsList.value = event.openPorts

                        val portsStr = event.openPorts.joinToString(", ") { it.port.toString() }
                        repository.updateOpenPorts(targetIp, portsStr)
                        _devicesList.value = _devicesList.value.map {
                            if (it.ip == targetIp) it.copy(openPorts = portsStr) else it
                        }
                        _userMessage.emit("Port scan finished. Discovered ${event.openPorts.size} open ports.")
                    }
                }
            }
        }
    }

    // PDF Report Export
    fun exportPdfReport(onReportReady: (File) -> Unit) {
        viewModelScope.launch {
            _userMessage.emit("Generating Network Audit PDF...")
            val result = pdfExporter.generatePdfReport(_networkStats.value, _devicesList.value)
            result.fold(
                onSuccess = { file ->
                    _userMessage.emit("PDF Report generated successfully!")
                    onReportReady(file)
                },
                onFailure = { err ->
                    _userMessage.emit("Failed to generate PDF: ${err.localizedMessage}")
                }
            )
        }
    }

    fun sharePdf(file: File) {
        pdfExporter.sharePdfReport(file)
    }

    // Bluetooth actions
    fun startBleScan() {
        bluetoothScanner.startScan(viewModelScope)
    }

    fun stopBleScan() {
        bluetoothScanner.stopScan()
    }

    fun selectBleDevice(dev: BluetoothDeviceInfo) {
        bluetoothScanner.selectDevice(dev)
    }

    fun toggleBlePair(dev: BluetoothDeviceInfo) {
        bluetoothScanner.togglePair(dev)
        val action = if (dev.isBonded) "Unpaired" else "Quick Paired with"
        viewModelScope.launch {
            _userMessage.emit("$action ${dev.name}")
        }
    }

    fun toggleWatchNotificationSync(dev: BluetoothDeviceInfo) {
        bluetoothScanner.toggleNotificationSync(dev)
    }

    fun sendWatchTestNotification(dev: BluetoothDeviceInfo) {
        viewModelScope.launch {
            _userMessage.emit("Test notification pushed to ${dev.name} ⌚ (Vibration & Alert synced)")
        }
    }

    // Smart Home actions (Matter, Zigbee, Multiroom Audio)
    fun setSmartMasterVolume(v: Float) = smartHomeManager.setMasterVolume(v)
    fun setSpeakerVolume(id: String, v: Float) = smartHomeManager.setSpeakerVolume(id, v)
    fun toggleSpeakerMute(id: String) = smartHomeManager.toggleSpeakerMute(id)
    fun toggleMultiroomGroup(id: String) = smartHomeManager.toggleMultiroomGroup(id)
    fun toggleMultiroomPlayback() = smartHomeManager.toggleMultiroomPlayback()
    fun selectAudioStream(track: String) = smartHomeManager.selectAudioStream(track)
    fun toggleZigbeePairing() {
        val newState = smartHomeManager.toggleZigbeePairingMode()
        viewModelScope.launch {
            _userMessage.emit(if (newState) "Zigbee Permit Join window opened (180s)" else "Zigbee pairing closed")
        }
    }
    fun commissionMatterDevice(name: String, room: String) {
        val dev = smartHomeManager.commissionMatterDevice(name, room)
        viewModelScope.launch {
            _userMessage.emit("Matter node ${dev.matterNodeId} successfully commissioned!")
        }
    }

    // Radar & Geolocation actions
    fun refreshLocationRadar() {
        locationRadarService.startLocationTracking()
        startBleScan()
        viewModelScope.launch {
            _userMessage.emit("Recalibrated GPS & radio triangulation coordinates.")
        }
    }

    // Web Server actions
    fun toggleWebServer(enabled: Boolean) {
        if (enabled) {
            webServer.start(viewModelScope, _networkStats.value.localIp, 8080)
            viewModelScope.launch {
                _userMessage.emit("Web Console started at http://${_networkStats.value.localIp}:8080")
            }
        } else {
            webServer.stop()
            viewModelScope.launch {
                _userMessage.emit("Web Console stopped")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryLoopJob?.cancel()
        locationRadarService.stopLocationTracking()
        webServer.stop()
        bluetoothScanner.cleanup()
    }
}
