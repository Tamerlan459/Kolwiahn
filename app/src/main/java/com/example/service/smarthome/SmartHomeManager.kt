package com.example.service.smarthome

import com.example.data.model.SmartDeviceCategory
import com.example.data.model.SmartHomeDevice
import com.example.data.model.SmartProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmartHomeManager {

    companion object {
        val AVAILABLE_AUDIO_STREAMS = listOf(
            "Cyber Ambient - Synthetic Pulse (FLAC 24-bit/96kHz)",
            "Lofi Beats / Night Chill Radio (Stream 320kbps)",
            "Daft Punk - Instant Crush (Lossless UPnP)",
            "Local DLNA Media Library (Multiroom Cast)"
        )
    }

    private val _devices = MutableStateFlow<List<SmartHomeDevice>>(
        listOf(
            SmartHomeDevice(
                id = "router_1",
                name = "Main Router AX5400 (Matter & Zigbee Bridge)",
                room = "Hallway / Rack",
                category = SmartDeviceCategory.ROUTER,
                ip = "192.168.1.1",
                protocol = SmartProtocol.MATTER,
                matterNodeId = "0x0001",
                matterFabric = "NetPulse-HomeFabric",
                zigbeeChannel = 25,
                zigbeeLqi = 255,
                isZigbeeCoordinator = true,
                isMatterCommissioned = true,
                adminUrl = "http://192.168.1.1",
                wifiClientsCount = 11,
                bandwidthDownMbps = 184.5f,
                bandwidthUpMbps = 42.1f
            ),
            SmartHomeDevice(
                id = "speaker_living_room",
                name = "Sonos Era 300 (Matter & AirPlay 2)",
                room = "Living Room",
                category = SmartDeviceCategory.SPEAKER,
                ip = "192.168.1.178",
                protocol = SmartProtocol.MATTER,
                matterNodeId = "0x1B20",
                volume = 0.65f,
                isPlaying = true,
                currentTrack = AVAILABLE_AUDIO_STREAMS[0],
                audioSource = "Matter Cast / AirPlay 2",
                isMultiroomGrouped = true
            ),
            SmartHomeDevice(
                id = "speaker_kitchen",
                name = "Google Nest Audio (Matter over Thread)",
                room = "Kitchen",
                category = SmartDeviceCategory.SPEAKER,
                ip = "192.168.1.182",
                protocol = SmartProtocol.THREAD,
                matterNodeId = "0x1C44",
                volume = 0.50f,
                isPlaying = true,
                currentTrack = AVAILABLE_AUDIO_STREAMS[0],
                audioSource = "Thread Mesh / Google Cast",
                isMultiroomGrouped = true
            ),
            SmartHomeDevice(
                id = "speaker_bedroom",
                name = "HomePod mini (Thread & Matter)",
                room = "Bedroom",
                category = SmartDeviceCategory.SPEAKER,
                ip = "192.168.1.195",
                protocol = SmartProtocol.MATTER,
                matterNodeId = "0x1E09",
                volume = 0.40f,
                isPlaying = true,
                currentTrack = AVAILABLE_AUDIO_STREAMS[0],
                audioSource = "Matter / AirPlay 2",
                isMultiroomGrouped = true
            ),
            SmartHomeDevice(
                id = "zigbee_sensor_hub",
                name = "Aqara Zigbee 3.0 Sensor Coordinator",
                room = "Living Room",
                category = SmartDeviceCategory.LIGHT,
                ip = "192.168.1.205",
                protocol = SmartProtocol.ZIGBEE,
                zigbeeChannel = 25,
                zigbeeLqi = 230,
                isZigbeeCoordinator = false,
                isOnline = true
            )
        )
    )
    val devices: StateFlow<List<SmartHomeDevice>> = _devices.asStateFlow()

    private val _isMultiroomPlaying = MutableStateFlow(true)
    val isMultiroomPlaying: StateFlow<Boolean> = _isMultiroomPlaying.asStateFlow()

    private val _currentStreamTrack = MutableStateFlow(AVAILABLE_AUDIO_STREAMS[0])
    val currentStreamTrack: StateFlow<String> = _currentStreamTrack.asStateFlow()

    private val _masterVolume = MutableStateFlow(0.55f)
    val masterVolume: StateFlow<Float> = _masterVolume.asStateFlow()

    private val _isZigbeePairingMode = MutableStateFlow(false)
    val isZigbeePairingMode: StateFlow<Boolean> = _isZigbeePairingMode.asStateFlow()

    fun setMasterVolume(newVolume: Float) {
        _masterVolume.value = newVolume
        val updated = _devices.value.map { dev ->
            if (dev.category == SmartDeviceCategory.SPEAKER && dev.isMultiroomGrouped) {
                dev.copy(volume = newVolume)
            } else dev
        }
        _devices.value = updated
    }

    fun setSpeakerVolume(id: String, volume: Float) {
        val updated = _devices.value.map { dev ->
            if (dev.id == id) dev.copy(volume = volume) else dev
        }
        _devices.value = updated
    }

    fun toggleSpeakerMute(id: String) {
        val updated = _devices.value.map { dev ->
            if (dev.id == id) dev.copy(isMuted = !dev.isMuted) else dev
        }
        _devices.value = updated
    }

    fun toggleMultiroomGroup(id: String) {
        val updated = _devices.value.map { dev ->
            if (dev.id == id) dev.copy(isMultiroomGrouped = !dev.isMultiroomGrouped) else dev
        }
        _devices.value = updated
    }

    fun toggleMultiroomPlayback() {
        val newPlayState = !_isMultiroomPlaying.value
        _isMultiroomPlaying.value = newPlayState
        val updated = _devices.value.map { dev ->
            if (dev.category == SmartDeviceCategory.SPEAKER && dev.isMultiroomGrouped) {
                dev.copy(isPlaying = newPlayState)
            } else dev
        }
        _devices.value = updated
    }

    fun selectAudioStream(trackName: String) {
        _currentStreamTrack.value = trackName
        val updated = _devices.value.map { dev ->
            if (dev.category == SmartDeviceCategory.SPEAKER) {
                dev.copy(currentTrack = trackName)
            } else dev
        }
        _devices.value = updated
    }

    fun toggleZigbeePairingMode(): Boolean {
        val newState = !_isZigbeePairingMode.value
        _isZigbeePairingMode.value = newState
        return newState
    }

    fun commissionMatterDevice(name: String, room: String): SmartHomeDevice {
        val newId = "matter_node_${System.currentTimeMillis() % 1000}"
        val newDevice = SmartHomeDevice(
            id = newId,
            name = name,
            room = room,
            category = SmartDeviceCategory.SPEAKER,
            ip = "192.168.1.${(150..220).random()}",
            protocol = SmartProtocol.MATTER,
            matterNodeId = "0x" + Integer.toHexString((1000..9999).random()).uppercase(),
            matterFabric = "NetPulse-HomeFabric",
            isMatterCommissioned = true,
            currentTrack = _currentStreamTrack.value,
            volume = 0.5f,
            isMultiroomGrouped = true
        )
        _devices.value = _devices.value + newDevice
        return newDevice
    }
}
