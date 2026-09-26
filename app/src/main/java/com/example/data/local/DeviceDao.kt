package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NetworkDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM network_devices ORDER BY isOnline DESC, ip ASC")
    fun getAllDevices(): Flow<List<NetworkDevice>>

    @Query("SELECT * FROM network_devices WHERE isBlocked = 1")
    fun getBlockedDevices(): Flow<List<NetworkDevice>>

    @Query("SELECT * FROM network_devices WHERE isSuspicious = 1")
    fun getSuspiciousDevices(): Flow<List<NetworkDevice>>

    @Query("SELECT * FROM network_devices WHERE ip = :ip LIMIT 1")
    suspend fun getDeviceByIp(ip: String): NetworkDevice?

    @Query("SELECT * FROM network_devices WHERE mac = :mac LIMIT 1")
    suspend fun getDeviceByMac(mac: String): NetworkDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: NetworkDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<NetworkDevice>)

    @Update
    suspend fun update(device: NetworkDevice)

    @Query("UPDATE network_devices SET isBlocked = :blocked WHERE ip = :ip")
    suspend fun setBlockedStatus(ip: String, blocked: Boolean)

    @Query("UPDATE network_devices SET isSuspicious = :suspicious WHERE ip = :ip")
    suspend fun setSuspiciousStatus(ip: String, suspicious: Boolean)

    @Query("UPDATE network_devices SET customName = :name WHERE ip = :ip")
    suspend fun updateCustomName(ip: String, name: String)

    @Query("UPDATE network_devices SET openPorts = :ports WHERE ip = :ip")
    suspend fun updateOpenPorts(ip: String, ports: String)

    @Query("DELETE FROM network_devices WHERE ip = :ip")
    suspend fun deleteByIp(ip: String)

    @Query("DELETE FROM network_devices")
    suspend fun clearAll()
}
