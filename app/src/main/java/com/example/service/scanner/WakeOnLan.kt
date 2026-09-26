package com.example.service.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Locale

object WakeOnLan {

    suspend fun sendMagicPacket(
        macAddress: String,
        broadcastIp: String = "255.255.255.255",
        port: Int = 9
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.trim().replace("[:-]".toRegex(), "")
            if (cleanMac.length != 12) {
                return@withContext Result.failure(IllegalArgumentException("Invalid MAC address length: $macAddress"))
            }

            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                val byteStr = cleanMac.substring(i * 2, (i * 2) + 2)
                macBytes[i] = byteStr.toInt(16).toByte()
            }

            // Magic Packet structure: 6 bytes of 0xFF, followed by 16 repetitions of MAC
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0 until 6) {
                bytes[i] = 0xFF.toByte()
            }
            var step = 6
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, bytes, step, macBytes.size)
                step += macBytes.size
            }

            val address = InetAddress.getByName(broadcastIp)
            val packet = DatagramPacket(bytes, bytes.size, address, port)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(packet)
            }

            Result.success("Magic packet successfully sent to $macAddress via $broadcastIp:$port")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
