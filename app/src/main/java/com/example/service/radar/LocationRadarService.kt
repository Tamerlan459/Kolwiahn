package com.example.service.radar

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.data.model.BluetoothDeviceInfo
import com.example.data.model.NetworkDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class RadarBlip(
    val id: String,
    val name: String,
    val addressOrIp: String,
    val isWifi: Boolean,
    val rssi: Int,
    val distanceMeters: Float,
    val angleDegrees: Float,
    val proximityState: String, // "Immediate (<1m)", "Near (<3.5m)", "Far (>3.5m)"
    val xOffsetNormalized: Float, // -1.0 to 1.0
    val yOffsetNormalized: Float  // -1.0 to 1.0
)

class LocationRadarService(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive.asStateFlow()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _userLocation.value = location
            _isGpsActive.value = true
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) { _isGpsActive.value = true }
        override fun onProviderDisabled(provider: String) { _isGpsActive.value = false }
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking() {
        try {
            if (locationManager != null) {
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000L,
                        2f,
                        locationListener
                    )
                    _userLocation.value = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    _isGpsActive.value = true
                } else if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000L,
                        2f,
                        locationListener
                    )
                    _userLocation.value = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    _isGpsActive.value = true
                }
            }
        } catch (_: SecurityException) {
            // Permission not yet granted, fallback gracefully
        }
    }

    fun stopLocationTracking() {
        try {
            locationManager?.removeUpdates(locationListener)
            _isGpsActive.value = false
        } catch (_: Exception) {}
    }

    /**
     * Estimates physical distance in meters from RSSI using log-distance path loss model:
     * d = 10 ^ ((TxPower - RSSI) / (10 * n))
     * TxPower calibrated at 1 meter (-59 dBm for BLE, -50 dBm for 5GHz Wi-Fi)
     * n = 2.4 (indoor attenuation factor)
     */
    fun calculateDistance(rssi: Int, isWifi: Boolean): Float {
        if (rssi == 0) return -1f
        val txPower = if (isWifi) -48.0 else -59.0
        val n = 2.4
        val ratio = (txPower - rssi) / (10.0 * n)
        val rawDist = 10.0.pow(ratio).toFloat()
        return (rawDist * 10f).toInt() / 10f // round to 1 decimal place
    }

    fun buildBlips(
        networkDevices: List<NetworkDevice>,
        bleDevices: List<BluetoothDeviceInfo>,
        maxRadiusMeters: Float = 15f
    ): List<RadarBlip> {
        val blips = mutableListOf<RadarBlip>()

        // BLE devices
        bleDevices.forEachIndexed { index, ble ->
            val dist = calculateDistance(ble.rssi, false).coerceIn(0.5f, maxRadiusMeters)
            // Distribute angles in a spread based on hash and index
            val angle = ((ble.address.hashCode() % 360 + 360) % 360).toFloat()
            val rad = Math.toRadians(angle.toDouble())
            val normDist = (dist / maxRadiusMeters).coerceIn(0.1f, 0.95f)

            val x = (sin(rad) * normDist).toFloat()
            val y = (-cos(rad) * normDist).toFloat()

            val state = when {
                dist < 1.0f -> "Immediate (<1m)"
                dist < 3.5f -> "Near (<3.5m)"
                else -> "Far (>3.5m)"
            }

            blips.add(
                RadarBlip(
                    id = ble.address,
                    name = ble.name,
                    addressOrIp = ble.address,
                    isWifi = false,
                    rssi = ble.rssi,
                    distanceMeters = dist,
                    angleDegrees = angle,
                    proximityState = state,
                    xOffsetNormalized = x,
                    yOffsetNormalized = y
                )
            )
        }

        // Network devices with calculated signal
        networkDevices.forEachIndexed { index, dev ->
            val dist = dev.estimatedDistanceMeters.coerceIn(0.5f, maxRadiusMeters)
            val angle = ((dev.ip.hashCode() % 360 + 360) % 360).toFloat()
            val rad = Math.toRadians(angle.toDouble())
            val normDist = (dist / maxRadiusMeters).coerceIn(0.1f, 0.95f)

            val x = (sin(rad) * normDist).toFloat()
            val y = (-cos(rad) * normDist).toFloat()

            val state = when {
                dist < 1.0f -> "Immediate (<1m)"
                dist < 3.5f -> "Near (<3.5m)"
                else -> "Far (>3.5m)"
            }

            blips.add(
                RadarBlip(
                    id = dev.ip,
                    name = dev.displayName,
                    addressOrIp = dev.ip,
                    isWifi = true,
                    rssi = dev.rssiDbm,
                    distanceMeters = dist,
                    angleDegrees = angle,
                    proximityState = state,
                    xOffsetNormalized = x,
                    yOffsetNormalized = y
                )
            )
        }

        return blips.sortedBy { it.distanceMeters }
    }
}
