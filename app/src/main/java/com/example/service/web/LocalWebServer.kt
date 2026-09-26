package com.example.service.web

import android.content.Context
import java.io.File
import java.io.FileInputStream
import com.example.data.model.NetworkDevice
import com.example.data.model.NetworkStats
import com.example.service.scanner.WakeOnLan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class LocalWebServer(
    private val context: Context,
    private val getDevices: () -> List<NetworkDevice>,
    private val getStats: () -> NetworkStats,
    private val onToggleBlock: (String) -> Unit
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _accessCount = MutableStateFlow(0)
    val accessCount: StateFlow<Int> = _accessCount.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val pool = Executors.newFixedThreadPool(4)

    fun start(scope: CoroutineScope, localIp: String, port: Int = 8080) {
        if (_isRunning.value) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                _isRunning.value = true
                _serverUrl.value = "http://$localIp:$port"

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        pool.execute {
                            handleClient(client)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                _isRunning.value = false
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        _serverUrl.value = ""
        serverJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val firstLine = reader.readLine() ?: return
            val parts = firstLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val uri = parts[1]

            _accessCount.value = _accessCount.value + 1
            val out = socket.getOutputStream()

            when {
                uri == "/" || uri.startsWith("/index") -> {
                    serveDashboard(out)
                }
                uri.startsWith("/download/apk") || uri.startsWith("/download-apk") || uri.endsWith(".apk") -> {
                    serveApk(out)
                }
                uri.startsWith("/api/devices") -> {
                    serveDevicesJson(out)
                }
                uri.startsWith("/api/wol") -> {
                    // Extract mac param
                    val mac = extractQueryParam(uri, "mac")
                    if (mac != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            WakeOnLan.sendMagicPacket(mac)
                        }
                        sendJsonResponse(out, """{"status":"ok","message":"Wake-on-LAN packet dispatched to $mac"}""")
                    } else {
                        sendJsonResponse(out, """{"status":"error","message":"Missing mac parameter"}""", 400)
                    }
                }
                uri.startsWith("/api/toggle-block") -> {
                    val ip = extractQueryParam(uri, "ip")
                    if (ip != null) {
                        onToggleBlock(ip)
                        sendJsonResponse(out, """{"status":"ok","message":"Toggled status for $ip"}""")
                    } else {
                        sendJsonResponse(out, """{"status":"error","message":"Missing ip parameter"}""", 400)
                    }
                }
                else -> {
                    send404(out)
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun serveApk(out: OutputStream) {
        try {
            val apkFile = File(context.applicationInfo.sourceDir)
            if (!apkFile.exists() || !apkFile.canRead()) {
                send404(out)
                return
            }
            val length = apkFile.length()
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/vnd.android.package-archive\r\n" +
                    "Content-Disposition: attachment; filename=\"NetPulse-Pro.apk\"\r\n" +
                    "Content-Length: $length\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray(Charsets.UTF_8))
            apkFile.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
            out.flush()
        } catch (_: Exception) {
            send404(out)
        }
    }

    private fun extractQueryParam(uri: String, param: String): String? {
        val qIndex = uri.indexOf('?')
        if (qIndex == -1) return null
        val query = uri.substring(qIndex + 1)
        val pairs = query.split("&")
        for (p in pairs) {
            val kv = p.split("=")
            if (kv.size == 2 && kv[0] == param) {
                return java.net.URLDecoder.decode(kv[1], "UTF-8")
            }
        }
        return null
    }

    private fun serveDashboard(out: OutputStream) {
        val devices = getDevices()
        val stats = getStats()

        val rows = StringBuilder()
        for (dev in devices) {
            val statusBadge = if (dev.isBlocked) {
                """<span class="badge danger">BLOCKED</span>"""
            } else if (dev.isSuspicious) {
                """<span class="badge warning">SUSPICIOUS</span>"""
            } else {
                """<span class="badge success">ONLINE</span>"""
            }

            rows.append(
                """
                <tr>
                    <td><strong>${dev.ip}</strong></td>
                    <td><code>${dev.mac}</code></td>
                    <td>${dev.displayName}</td>
                    <td>${dev.vendor}</td>
                    <td>${dev.osName}</td>
                    <td><span class="ping">${dev.pingMs}ms</span></td>
                    <td>$statusBadge</td>
                    <td>
                        <button class="btn btn-sm btn-wol" onclick="sendWol('${dev.mac}')">⚡ WoL</button>
                        <button class="btn btn-sm ${if (dev.isBlocked) "btn-unblock" else "btn-block"}" onclick="toggleBlock('${dev.ip}')">
                            ${if (dev.isBlocked) "Unblock" else "Block"}
                        </button>
                    </td>
                </tr>
                """.trimIndent()
            )
        }

        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>NetPulse Pro - Remote Network Dashboard</title>
                <style>
                    :root {
                        --bg: #090e1a;
                        --card: #111a2e;
                        --card-border: #1e2c4a;
                        --primary: #00e5ff;
                        --primary-glow: rgba(0, 229, 255, 0.2);
                        --text: #e2e8f0;
                        --text-muted: #8b9bb4;
                        --success: #10b981;
                        --danger: #ef4444;
                        --warning: #f59e0b;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
                    body { background: var(--bg); color: var(--text); padding: 24px; }
                    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--card-border); }
                    .brand { display: flex; align-items: center; gap: 12px; }
                    .brand h1 { font-size: 22px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px; }
                    .brand .tag { background: var(--primary-glow); color: var(--primary); padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; text-transform: uppercase; border: 1px solid var(--primary); }
                    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 24px; }
                    .stat-card { background: var(--card); border: 1px solid var(--card-border); padding: 16px; border-radius: 12px; }
                    .stat-card .label { font-size: 12px; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }
                    .stat-card .val { font-size: 24px; font-weight: 700; color: #fff; margin-top: 6px; }
                    .card { background: var(--card); border: 1px solid var(--card-border); border-radius: 14px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }
                    .card-header { padding: 16px 20px; border-bottom: 1px solid var(--card-border); display: flex; justify-content: space-between; align-items: center; }
                    .card-header h2 { font-size: 16px; font-weight: 600; }
                    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 13px; }
                    th { padding: 12px 16px; background: rgba(0,0,0,0.2); color: var(--text-muted); font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; }
                    td { padding: 12px 16px; border-bottom: 1px solid var(--card-border); vertical-align: middle; }
                    tr:hover { background: rgba(255,255,255,0.02); }
                    code { background: rgba(255,255,255,0.06); padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 12px; }
                    .badge { padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 600; }
                    .badge.success { background: rgba(16, 185, 129, 0.15); color: var(--success); }
                    .badge.danger { background: rgba(239, 68, 68, 0.15); color: var(--danger); }
                    .badge.warning { background: rgba(245, 158, 11, 0.15); color: var(--warning); }
                    .ping { color: var(--primary); font-weight: 600; }
                    .btn { cursor: pointer; border: none; border-radius: 6px; padding: 6px 12px; font-size: 12px; font-weight: 600; transition: all 0.2s; }
                    .btn-sm { padding: 4px 8px; font-size: 11px; }
                    .btn-wol { background: #2563eb; color: #fff; margin-right: 6px; }
                    .btn-wol:hover { background: #1d4ed8; }
                    .btn-block { background: rgba(239,68,68,0.2); color: var(--danger); border: 1px solid var(--danger); }
                    .btn-block:hover { background: var(--danger); color: #fff; }
                    .btn-unblock { background: rgba(16,185,129,0.2); color: var(--success); border: 1px solid var(--success); }
                    .btn-unblock:hover { background: var(--success); color: #fff; }
                    .btn-refresh { background: var(--primary); color: #000; padding: 8px 16px; }
                    .btn-refresh:hover { opacity: 0.9; box-shadow: 0 0 15px var(--primary-glow); }
                    #toast { position: fixed; bottom: 24px; right: 24px; background: #1e293b; border: 1px solid var(--primary); color: #fff; padding: 12px 20px; border-radius: 8px; display: none; box-shadow: 0 10px 25px rgba(0,0,0,0.5); z-index: 100; font-size: 13px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="brand">
                        <h1>NetPulse Pro</h1>
                        <span class="tag">Web Management Console</span>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <a href="/download/apk" class="btn" style="background: #10b981; color: #ffffff; text-decoration: none; padding: 8px 16px; display: inline-flex; align-items: center; gap: 6px; font-weight: 600;">📥 Скачать APK</a>
                        <button class="btn btn-refresh" onclick="location.reload()">↻ Refresh Status</button>
                    </div>
                </div>

                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="label">Total Scanned</div>
                        <div class="val">${devices.size}</div>
                    </div>
                    <div class="stat-card">
                        <div class="label">Online Devices</div>
                        <div class="val" style="color: var(--success)">${devices.count { it.isOnline }}</div>
                    </div>
                    <div class="stat-card">
                        <div class="label">Blocked / Suspicious</div>
                        <div class="val" style="color: var(--danger)">${devices.count { it.isBlocked || it.isSuspicious }}</div>
                    </div>
                    <div class="stat-card">
                        <div class="label">Local Gateway</div>
                        <div class="val" style="font-size: 18px; margin-top: 10px">${stats.gatewayIp}</div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <h2>Connected Network Endpoints (${devices.size})</h2>
                    </div>
                    <table>
                        <thead>
                            <tr>
                                <th>IP Address</th>
                                <th>MAC Address</th>
                                <th>Name / Host</th>
                                <th>Manufacturer</th>
                                <th>Operating System</th>
                                <th>Ping</th>
                                <th>Policy</th>
                                <th>Remote Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            $rows
                        </tbody>
                    </table>
                </div>

                <div id="toast"></div>

                <script>
                    function showToast(msg) {
                        const t = document.getElementById('toast');
                        t.innerText = msg;
                        t.style.display = 'block';
                        setTimeout(() => { t.style.display = 'none'; }, 3000);
                    }

                    function sendWol(mac) {
                        fetch('/api/wol?mac=' + encodeURIComponent(mac))
                            .then(r => r.json())
                            .then(data => showToast(data.message || 'Wake-on-LAN dispatched!'))
                            .catch(e => showToast('Error sending WoL packet'));
                    }

                    function toggleBlock(ip) {
                        fetch('/api/toggle-block?ip=' + encodeURIComponent(ip))
                            .then(r => r.json())
                            .then(data => {
                                showToast(data.message || 'Updated status');
                                setTimeout(() => location.reload(), 600);
                            })
                            .catch(e => showToast('Error changing device status'));
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun serveDevicesJson(out: OutputStream) {
        val devices = getDevices()
        val jsonArray = devices.joinToString(prefix = "[", postfix = "]", separator = ",") { d ->
            """{"ip":"${d.ip}","mac":"${d.mac}","name":"${d.displayName}","vendor":"${d.vendor}","os":"${d.osName}","ping":${d.pingMs},"isBlocked":${d.isBlocked},"isSuspicious":${d.isSuspicious}}"""
        }
        sendJsonResponse(out, jsonArray)
    }

    private fun sendJsonResponse(out: OutputStream, json: String, code: Int = 200) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun send404(out: OutputStream) {
        val body = "404 Not Found"
        val header = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.flush()
    }
}
