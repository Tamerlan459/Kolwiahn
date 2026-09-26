package com.example.data.repository

import com.example.data.local.DeviceDao
import com.example.data.model.NetworkDevice
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val deviceDao: DeviceDao) {
    val allDevices: Flow<List<NetworkDevice>> = deviceDao.getAllDevices()
    val blockedDevices: Flow<List<NetworkDevice>> = deviceDao.getBlockedDevices()
    val suspiciousDevices: Flow<List<NetworkDevice>> = deviceDao.getSuspiciousDevices()

    suspend fun getDeviceByIp(ip: String): NetworkDevice? = deviceDao.getDeviceByIp(ip)

    suspend fun insertOrUpdate(device: NetworkDevice) = deviceDao.insertOrUpdate(device)

    suspend fun insertAll(devices: List<NetworkDevice>) = deviceDao.insertAll(devices)

    suspend fun toggleBlocked(ip: String, isCurrentlyBlocked: Boolean) {
        deviceDao.setBlockedStatus(ip, !isCurrentlyBlocked)
    }

    suspend fun toggleSuspicious(ip: String, isCurrentlySuspicious: Boolean) {
        deviceDao.setSuspiciousStatus(ip, !isCurrentlySuspicious)
    }

    suspend fun updateCustomName(ip: String, name: String) {
        deviceDao.updateCustomName(ip, name)
    }

    suspend fun updateOpenPorts(ip: String, ports: String) {
        deviceDao.updateOpenPorts(ip, ports)
    }

    suspend fun deleteDevice(ip: String) = deviceDao.deleteByIp(ip)

    suspend fun clearAll() = deviceDao.clearAll()
}
