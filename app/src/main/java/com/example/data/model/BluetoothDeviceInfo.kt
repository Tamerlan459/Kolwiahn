package com.example.data.model

data class BleGattServiceInfo(
    val uuid: String,
    val name: String,
    val characteristicsCount: Int
)

data class WatchTelemetry(
    val heartRateBpm: Int = 72,
    val stepsToday: Int = 6420,
    val batteryPercent: Int = 84,
    val notificationSyncActive: Boolean = true,
    val isTracking: Boolean = true
)

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val isBonded: Boolean = false,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val services: List<BleGattServiceInfo> = emptyList(),
    val watchTelemetry: WatchTelemetry? = null,
    val isConnected: Boolean = false,
    val estimatedDistanceMeters: Float = 2.4f,
    val radarAngleDegrees: Float = 45f,
    val proximityZone: String = "Near (< 3m)"
) {
    val signalStrengthPercent: Int
        get() = ((rssi + 100).coerceIn(0, 100) * 100) / 100
}
