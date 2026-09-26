package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DeviceType(val displayName: String) {
    ROUTER("Router / Gateway"),
    PHONE("Smartphone"),
    TABLET("Tablet"),
    LAPTOP("PC / Laptop"),
    SMART_TV("Smart TV / Cast"),
    SPEAKER("Smart Speaker"),
    SMARTWATCH("Smartwatch / Wearable"),
    IOT("IoT / Smart Home"),
    UNKNOWN("Network Device")
}

@Entity(tableName = "network_devices")
data class NetworkDevice(
    @PrimaryKey
    val ip: String,
    val mac: String = "Unknown",
    val hostname: String = "",
    val vendor: String = "Generic / Unknown",
    val osName: String = "Unknown OS",
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val pingMs: Long = -1L,
    val isOnline: Boolean = true,
    val isBlocked: Boolean = false,
    val isSuspicious: Boolean = false,
    val customName: String? = null,
    val openPorts: String = "", // Comma-separated list of ports e.g. "80, 443, 22"
    val downloadMb: Float = 14.5f,
    val uploadMb: Float = 3.2f,
    val currentSpeedMbps: Float = 1.2f,
    val rssiDbm: Int = -62,
    val estimatedDistanceMeters: Float = 3.8f,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = customName?.takeIf { it.isNotBlank() }
            ?: hostname.takeIf { it.isNotBlank() }
            ?: "$vendor Device ($ip)"
}
