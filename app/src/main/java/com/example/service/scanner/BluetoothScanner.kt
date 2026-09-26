package com.example.service.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.example.data.model.BleGattServiceInfo
import com.example.data.model.BluetoothDeviceInfo
import com.example.data.model.DeviceType
import com.example.data.model.WatchTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BluetoothScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _devices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val devices: StateFlow<List<BluetoothDeviceInfo>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceInfo?> = _selectedDevice.asStateFlow()

    private var scanJob: Job? = null
    private var telemetryJob: Job? = null
    private var activeGatt: BluetoothGatt? = null

    // Standard GATT UUID descriptions
    private val standardGattServices = mapOf(
        "00001800-0000-1000-8000-00805f9b34fb" to Pair("Generic Access", 4),
        "00001801-0000-1000-8000-00805f9b34fb" to Pair("Generic Attribute", 2),
        "0000180a-0000-1000-8000-00805f9b34fb" to Pair("Device Information", 7),
        "0000180f-0000-1000-8000-00805f9b34fb" to Pair("Battery Service", 2),
        "0000180d-0000-1000-8000-00805f9b34fb" to Pair("Heart Rate Service", 3),
        "0000181a-0000-1000-8000-00805f9b34fb" to Pair("Environmental Sensing", 5),
        "00001812-0000-1000-8000-00805f9b34fb" to Pair("Human Interface Device (HID)", 4),
        "0000fef5-0000-1000-8000-00805f9b34fb" to Pair("Fast Pair Service (GFPS)", 2)
    )

    private val sampleDevices = listOf(
        BluetoothDeviceInfo(
            name = "Galaxy Watch 6 Pro",
            address = "D4:F5:13:88:9B:41",
            rssi = -58,
            isBonded = true,
            deviceType = DeviceType.SMARTWATCH,
            services = listOf(
                BleGattServiceInfo("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service", 3),
                BleGattServiceInfo("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service", 2),
                BleGattServiceInfo("0000180a-0000-1000-8000-00805f9b34fb", "Device Information", 7),
                Ble001801Service()
            ),
            watchTelemetry = WatchTelemetry(heartRateBpm = 74, stepsToday = 7840, batteryPercent = 86, notificationSyncActive = true),
            isConnected = true
        ),
        BluetoothDeviceInfo(
            name = "Sony WH-1000XM5",
            address = "70:26:05:4A:12:F0",
            rssi = -64,
            isBonded = true,
            deviceType = DeviceType.SPEAKER,
            services = listOf(
                BleGattServiceInfo("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service", 2),
                BleGattServiceInfo("0000fef5-0000-1000-8000-00805f9b34fb", "Fast Pair Service (GFPS)", 2),
                BleGattServiceInfo("0000180a-0000-1000-8000-00805f9b34fb", "Device Information", 5)
            )
        ),
        BluetoothDeviceInfo(
            name = "Xiaomi Smart Band 8",
            address = "E4:5F:01:3C:90:7E",
            rssi = -71,
            isBonded = false,
            deviceType = DeviceType.SMARTWATCH,
            services = listOf(
                BleGattServiceInfo("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service", 2),
                BleGattServiceInfo("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service", 1)
            ),
            watchTelemetry = WatchTelemetry(heartRateBpm = 68, stepsToday = 5230, batteryPercent = 91, notificationSyncActive = true)
        ),
        BluetoothDeviceInfo(
            name = "Aqara Temperature & Humidity (BLE)",
            address = "54:EF:44:A2:18:03",
            rssi = -79,
            isBonded = false,
            deviceType = DeviceType.IOT,
            services = listOf(
                BleGattServiceInfo("0000181a-0000-1000-8000-00805f9b34fb", "Environmental Sensing", 3),
                BleGattServiceInfo("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service", 1)
            )
        ),
        BluetoothDeviceInfo(
            name = "Smart Air Purifier 4 Pro",
            address = "60:01:94:DD:52:19",
            rssi = -82,
            isBonded = false,
            deviceType = DeviceType.IOT,
            services = listOf(
                BleGattServiceInfo("0000180a-0000-1000-8000-00805f9b34fb", "Device Information", 4)
            )
        )
    )

    private fun Ble001801Service() = BleGattServiceInfo("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute", 2)

    init {
        _devices.value = sampleDevices
        startWatchTelemetryLoop()
    }

    @SuppressLint("MissingPermission")
    fun startScan(scope: CoroutineScope) {
        _isScanning.value = true
        scanJob?.cancel()

        scanJob = scope.launch(Dispatchers.IO) {
            val foundMap = mutableMapOf<String, BluetoothDeviceInfo>()
            // Populate initial known devices
            sampleDevices.forEach { foundMap[it.address] = it }

            // Try real BLE scan if available & enabled
            var realScannerActive = false
            try {
                if (adapter != null && adapter.isEnabled) {
                    val scanner = adapter.bluetoothLeScanner
                    if (scanner != null) {
                        val callback = object : ScanCallback() {
                            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                                result?.let { res ->
                                    val dev = res.device
                                    val name = dev.name ?: "BLE Sensor ${dev.address.takeLast(5)}"
                                    val address = dev.address
                                    val rssi = res.rssi
                                    val info = BluetoothDeviceInfo(
                                        name = name,
                                        address = address,
                                        rssi = rssi,
                                        isBonded = dev.bondState == BluetoothDevice.BOND_BONDED,
                                        deviceType = if (name.contains("watch", true) || name.contains("band", true)) DeviceType.SMARTWATCH else DeviceType.IOT
                                    )
                                    foundMap[address] = info
                                    _devices.value = foundMap.values.toList().sortedByDescending { it.rssi }
                                }
                            }
                        }
                        scanner.startScan(callback)
                        realScannerActive = true
                        delay(5000)
                        scanner.stopScan(callback)
                    }
                }
            } catch (_: Exception) {}

            if (!realScannerActive) {
                // Simulate progressive scanning of local RF space
                for (i in 1..4) {
                    delay(700)
                    // Random small RSSI jitter for live feel
                    val updated = sampleDevices.map { dev ->
                        dev.copy(rssi = (dev.rssi + (-3..3).random()).coerceIn(-95, -40))
                    }
                    _devices.value = updated.sortedByDescending { it.rssi }
                }
            }

            _isScanning.value = false
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _isScanning.value = false
    }

    fun selectDevice(device: BluetoothDeviceInfo) {
        _selectedDevice.value = device
    }

    fun togglePair(device: BluetoothDeviceInfo) {
        val updated = _devices.value.map {
            if (it.address == device.address) {
                it.copy(
                    isBonded = !it.isBonded,
                    isConnected = !it.isBonded
                )
            } else it
        }
        _devices.value = updated
        if (_selectedDevice.value?.address == device.address) {
            _selectedDevice.value = updated.find { it.address == device.address }
        }
    }

    fun toggleNotificationSync(device: BluetoothDeviceInfo) {
        val current = device.watchTelemetry ?: return
        val newTelemetry = current.copy(notificationSyncActive = !current.notificationSyncActive)
        updateDeviceTelemetry(device.address, newTelemetry)
    }

    private fun updateDeviceTelemetry(address: String, telemetry: WatchTelemetry) {
        val updated = _devices.value.map {
            if (it.address == address) it.copy(watchTelemetry = telemetry) else it
        }
        _devices.value = updated
        if (_selectedDevice.value?.address == address) {
            _selectedDevice.value = updated.find { it.address == address }
        }
    }

    private fun startWatchTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(3000)
                val currentDevices = _devices.value
                val updated = currentDevices.map { dev ->
                    if (dev.watchTelemetry != null && dev.isConnected) {
                        val currentBpm = dev.watchTelemetry.heartRateBpm
                        val jitter = (-2..2).random()
                        val newBpm = (currentBpm + jitter).coerceIn(60, 110)
                        val newSteps = dev.watchTelemetry.stepsToday + (0..4).random()
                        dev.copy(watchTelemetry = dev.watchTelemetry.copy(heartRateBpm = newBpm, stepsToday = newSteps))
                    } else dev
                }
                _devices.value = updated
                val sel = _selectedDevice.value
                if (sel != null) {
                    _selectedDevice.value = updated.find { it.address == sel.address }
                }
            }
        }
    }

    fun cleanup() {
        scanJob?.cancel()
        telemetryJob?.cancel()
        try {
            activeGatt?.close()
        } catch (_: Exception) {}
    }
}
