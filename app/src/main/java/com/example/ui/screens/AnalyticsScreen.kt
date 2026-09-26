package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NetworkDevice
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.MainViewModel

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val stats by viewModel.networkStats.collectAsStateWithLifecycle()
    val devices by viewModel.filteredDevices.collectAsStateWithLifecycle()

    val sortedByTraffic = remember(devices) {
        devices.sortedByDescending { it.downloadMb + it.uploadMb }
    }
    val totalDeviceTraffic = remember(sortedByTraffic) {
        sortedByTraffic.sumOf { (it.downloadMb + it.uploadMb).toDouble() }.toFloat().coerceAtLeast(1f)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "Network Telemetry & Deep Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Real-time bandwidth throughput, per-device consumption & socket statistics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Real-Time Throughput Graph Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bandwidth_graph_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Live Bandwidth Monitor",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Duplex Tx/Rx throughput in Mbps",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "REAL-TIME",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Speed figures
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Text(text = " Download", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${stats.currentDownloadSpeedMbps} Mbps",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                                Text(text = " Upload", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${stats.currentUploadSpeedMbps} Mbps",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        }
                        Column {
                            Text(text = "Active Sockets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${stats.activeSocketsCount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Canvas Line Chart with Dual Streams (Download in CyberCyan, Upload in Purple)
                    LiveThroughputChart(
                        downloadPoints = stats.downloadHistory,
                        uploadPoints = stats.uploadHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyberCyan))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Download Rx", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PurpleAccent))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Upload Tx", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Connection Quality & Hardware Stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connection Quality & PHY Parameters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QualityMetricItem(
                            label = "Signal (RSSI)",
                            value = "${stats.signalDbm} dBm",
                            status = if (stats.signalDbm > -65) "Excellent" else "Fair",
                            statusColor = NeonGreen
                        )
                        QualityMetricItem(
                            label = "Link PHY Speed",
                            value = "${stats.linkSpeedMbps} Mbps",
                            status = "Wi-Fi 6 Rate",
                            statusColor = CyberCyan
                        )
                        QualityMetricItem(
                            label = "Frequency",
                            value = "${stats.frequencyMhz} MHz",
                            status = if (stats.frequencyMhz > 4900) "5 GHz Band" else "2.4 GHz Band",
                            statusColor = PurpleAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QualityMetricItem(
                            label = "Packet Loss",
                            value = "${stats.packetLossPercent}%",
                            status = "Zero Drops",
                            statusColor = NeonGreen
                        )
                        QualityMetricItem(
                            label = "Jitter",
                            value = "${stats.jitterMs} ms",
                            status = "Ultra-Low",
                            statusColor = NeonGreen
                        )
                        QualityMetricItem(
                            label = "Cumulative Data",
                            value = "${(stats.totalDownloadMb + stats.totalUploadMb).toInt()} MB",
                            status = "Session Total",
                            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Per-Device Traffic Breakdown Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bandwidth Usage by Endpoint (${sortedByTraffic.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${totalDeviceTraffic.toInt()} MB Total",
                    fontSize = 12.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Per-device traffic list
        items(sortedByTraffic, key = { it.ip }) { device ->
            DeviceTrafficCard(
                device = device,
                totalTraffic = totalDeviceTraffic
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun QualityMetricItem(label: String, value: String, status: String, statusColor: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = status, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DeviceTrafficCard(device: NetworkDevice, totalTraffic: Float) {
    val deviceTotal = device.downloadMb + device.uploadMb
    val percentOfTotal = (deviceTotal / totalTraffic).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "${device.ip} • ${device.vendor}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.1f MB", deviceTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Text(
                        text = "${(percentOfTotal * 100).toInt()}% of network",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dual progress bar: Download (Cyan) and Upload (Purple)
            LinearProgressIndicator(
                progress = { percentOfTotal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyberCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rx: ${device.downloadMb} MB  |  Tx: ${device.uploadMb} MB",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Current: ${device.currentSpeedMbps} Mbps",
                    fontSize = 10.sp,
                    color = NeonGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LiveThroughputChart(
    downloadPoints: List<Float>,
    uploadPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (downloadPoints.size < 2) return@Canvas

        // Find max scale
        val maxVal = maxOf(
            downloadPoints.maxOrNull() ?: 10f,
            uploadPoints.maxOrNull() ?: 10f,
            20f
        ) * 1.15f

        // Draw horizontal grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = h * (i.toFloat() / gridLines)
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Draw Download path
        val dlPath = Path()
        val dlFillPath = Path()
        val stepX = w / (downloadPoints.size - 1)

        downloadPoints.forEachIndexed { i, pt ->
            val x = i * stepX
            val y = h - (pt / maxVal) * h
            if (i == 0) {
                dlPath.moveTo(x, y)
                dlFillPath.moveTo(x, h)
                dlFillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = h - (downloadPoints[i - 1] / maxVal) * h
                val midX = (prevX + x) / 2
                dlPath.cubicTo(midX, prevY, midX, y, x, y)
                dlFillPath.cubicTo(midX, prevY, midX, y, x, y)
            }
        }
        dlFillPath.lineTo(w, h)
        dlFillPath.close()

        // Fill under download curve
        drawPath(
            path = dlFillPath,
            brush = Brush.verticalGradient(
                colors = listOf(CyberCyan.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        // Stroke download curve
        drawPath(
            path = dlPath,
            color = CyberCyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Upload path
        val ulPath = Path()
        uploadPoints.forEachIndexed { i, pt ->
            val x = i * stepX
            val y = h - (pt / maxVal) * h
            if (i == 0) {
                ulPath.moveTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = h - (uploadPoints[i - 1] / maxVal) * h
                val midX = (prevX + x) / 2
                ulPath.cubicTo(midX, prevY, midX, y, x, y)
            }
        }
        drawPath(
            path = ulPath,
            color = PurpleAccent,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
