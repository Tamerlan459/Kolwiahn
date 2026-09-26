package com.example.service.scanner

import com.example.data.model.PortInfo
import com.example.data.model.PortRisk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class PortScanner {

    companion object {
        val WELL_KNOWN_PORTS = listOf(
            PortDefinition(21, "FTP", "File Transfer Protocol (often unencrypted)", PortRisk.WARNING),
            PortDefinition(22, "SSH", "Secure Shell Remote Admin", PortRisk.SAFE),
            PortDefinition(23, "Telnet", "Unencrypted Plaintext Remote Shell", PortRisk.CRITICAL),
            PortDefinition(25, "SMTP", "Simple Mail Transfer Protocol", PortRisk.INFO),
            PortDefinition(53, "DNS", "Domain Name System server", PortRisk.SAFE),
            PortDefinition(80, "HTTP", "Hypertext Transfer Protocol (Web Server)", PortRisk.INFO),
            PortDefinition(110, "POP3", "Post Office Protocol (Email)", PortRisk.INFO),
            PortDefinition(139, "NetBIOS", "NetBIOS Session Service", PortRisk.WARNING),
            PortDefinition(143, "IMAP", "Internet Message Access Protocol", PortRisk.INFO),
            PortDefinition(443, "HTTPS", "Encrypted Web Server (TLS/SSL)", PortRisk.SAFE),
            PortDefinition(445, "SMB", "Microsoft Server Message Block (File Sharing)", PortRisk.WARNING),
            PortDefinition(554, "RTSP", "Real Time Streaming Protocol (IP Cameras)", PortRisk.INFO),
            PortDefinition(1400, "Sonos", "Sonos Multiroom Audio Stream", PortRisk.SAFE),
            PortDefinition(1883, "MQTT", "IoT Message Queuing Telemetry Transport", PortRisk.INFO),
            PortDefinition(3306, "MySQL", "MySQL Database Server", PortRisk.WARNING),
            PortDefinition(3389, "RDP", "Microsoft Remote Desktop Protocol", PortRisk.WARNING),
            PortDefinition(5000, "UPnP", "Universal Plug and Play / Synology", PortRisk.INFO),
            PortDefinition(8008, "Cast", "Google Cast / Smart TV Receiver", PortRisk.SAFE),
            PortDefinition(8080, "HTTP-Alt", "Common Alternate Web Server / Proxy", PortRisk.INFO),
            PortDefinition(8443, "HTTPS-Alt", "Alternate Secure Web Server", PortRisk.SAFE),
            PortDefinition(9000, "Sonar", "Media Server / Diagnostic Web UI", PortRisk.INFO)
        )
    }

    data class PortDefinition(
        val port: Int,
        val service: String,
        val description: String,
        val risk: PortRisk
    )

    sealed class PortScanEvent {
        data class Progress(val scanned: Int, val total: Int, val foundOpen: PortInfo? = null) : PortScanEvent()
        data class Complete(val openPorts: List<PortInfo>) : PortScanEvent()
    }

    fun scanPorts(ip: String, customPortList: List<Int>? = null): Flow<PortScanEvent> = flow {
        val targets = if (customPortList != null && customPortList.isNotEmpty()) {
            customPortList.map { p ->
                val def = WELL_KNOWN_PORTS.find { it.port == p }
                PortDefinition(p, def?.service ?: "TCP-$p", def?.description ?: "Custom Port", def?.risk ?: PortRisk.INFO)
            }
        } else {
            WELL_KNOWN_PORTS
        }

        val total = targets.size
        val openPorts = mutableListOf<PortInfo>()
        var count = 0

        // Batch scan in chunks of 5 for safety and responsiveness
        for (chunk in targets.chunked(5)) {
            val results = withContext(Dispatchers.IO) {
                chunk.map { def ->
                    async {
                        val isOpen = checkPort(ip, def.port, 250)
                        if (isOpen) {
                            val banner = grabBanner(ip, def.port)
                            PortInfo(
                                port = def.port,
                                service = def.service,
                                isOpen = true,
                                description = def.description,
                                risk = def.risk,
                                banner = banner
                            )
                        } else {
                            null
                        }
                    }
                }.awaitAll()
            }

            for (res in results) {
                count++
                if (res != null) {
                    openPorts.add(res)
                    emit(PortScanEvent.Progress(count, total, res))
                } else {
                    emit(PortScanEvent.Progress(count, total, null))
                }
            }
        }

        emit(PortScanEvent.Complete(openPorts))
    }.flowOn(Dispatchers.IO)

    private fun checkPort(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun grabBanner(ip: String, port: Int): String {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 200)
            socket.soTimeout = 200

            // If HTTP, send HEAD request
            if (port in listOf(80, 443, 8080, 8443)) {
                val writer = OutputStreamWriter(socket.getOutputStream())
                writer.write("HEAD / HTTP/1.0\r\n\r\n")
                writer.flush()
            }

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val firstLine = reader.readLine() ?: ""
            socket.close()
            firstLine.trim().take(60)
        } catch (_: Exception) {
            ""
        }
    }
}
