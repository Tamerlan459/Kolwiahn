package com.example.data.model

enum class SmartDeviceCategory {
    ROUTER,
    SPEAKER,
    LIGHT,
    THERMOSTAT,
    TV
}

enum class SmartProtocol(val label: String, val badgeColorHex: Long) {
    MATTER("Matter 1.3 / Thread", 0xFF00E5FF),
    ZIGBEE("Zigbee 3.0 Mesh", 0xFFF59E0B),
    THREAD("Thread Border", 0xFF10B981),
    UPNP_DLNA("UPnP / DLNA / Cast", 0xFF8B5CF6),
    AIRPLAY2("Apple AirPlay 2", 0xFF38BDF8)
}

data class SmartHomeDevice(
    val id: String,
    val name: String,
    val room: String,
    val category: SmartDeviceCategory,
    val ip: String,
    val isOnline: Boolean = true,
    val protocol: SmartProtocol = SmartProtocol.MATTER,
    // Matter / Zigbee specifics
    val matterNodeId: String = "0x1A4F",
    val matterFabric: String = "Fabric-4821",
    val zigbeeChannel: Int = 15,
    val zigbeeLqi: Int = 210, // Link Quality Index 0-255
    val isZigbeeCoordinator: Boolean = false,
    val isMatterCommissioned: Boolean = true,
    // Speaker & Multiroom parameters
    val volume: Float = 0.5f, // 0.0 to 1.0
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTrack: String = "Cyber Ambient - Synthetic Pulse",
    val audioSource: String = "Hi-Res Master Stream (FLAC)",
    val isMultiroomGrouped: Boolean = true,
    // Router parameters
    val adminUrl: String = "http://192.168.1.1",
    val wifiClientsCount: Int = 8,
    val bandwidthDownMbps: Float = 145.2f,
    val bandwidthUpMbps: Float = 38.6f
)
