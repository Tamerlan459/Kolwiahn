package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceType
import com.example.data.model.NetworkDevice
import com.example.ui.components.DeviceDetailDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.viewmodel.MainViewModel
import java.io.File

@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    activeSubTab: Int = 0,
    onSubTabChange: (Int) -> Unit = {},
    onNavigateToPorts: (String) -> Unit,
    onPdfReady: (File) -> Unit
) {
    val stats by viewModel.networkStats.collectAsStateWithLifecycle()
    val devices by viewModel.filteredDevices.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    var selectedDeviceForDetails by remember { mutableStateOf<NetworkDevice?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // High-tech Material 3 Sub-tab navigation
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = CyberCyan
                )
            }
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { onSubTabChange(0) },
                text = { Text("Endpoints (${devices.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("subtab_endpoints")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { onSubTabChange(1) },
                text = { Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("subtab_analytics")
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { onSubTabChange(2) },
                text = { Text("Radar (No Root)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("subtab_radar")
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                1 -> {
                    AnalyticsScreen(viewModel = viewModel)
                }
                2 -> {
                    RadarMapScreen(viewModel = viewModel)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
                    ) {
                        // Network Overview & Analytics Card
                        item {
                            NetworkOverviewCard(
                                stats = stats,
                                onExportPdf = { viewModel.exportPdfReport(onPdfReady) },
                                onStartScan = { viewModel.startNetworkScan() },
                                onOpenAnalytics = { onSubTabChange(1) },
                                onOpenRadar = { onSubTabChange(2) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Scanning progress indicator
                        if (stats.isScanning) {
                            item {
                                ScanProgressCard(
                                    progress = stats.scanProgress,
                                    currentIp = stats.currentScanningIp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Search Bar & Filter Chips
                        item {
                            SearchAndFilterSection(
                                searchQuery = searchQuery,
                                onSearchChange = { viewModel.searchQuery.value = it },
                                selectedFilter = selectedFilter,
                                onFilterSelect = { viewModel.selectedFilter.value = it }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Section Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Discovered Endpoints (${devices.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${stats.onlineDevices} Online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Empty state
                        if (devices.isEmpty() && !stats.isScanning) {
                            item {
                                EmptyDevicesCard(onScanAgain = { viewModel.startNetworkScan() })
                            }
                        }

                        // Devices list
                        items(devices, key = { it.ip }) { device ->
                            DeviceCard(
                                device = device,
                                onClick = { selectedDeviceForDetails = device },
                                onSendWol = { viewModel.sendWakeOnLan(device) },
                                onToggleBlock = { viewModel.toggleBlockDevice(device) },
                                onScanPorts = { onNavigateToPorts(device.ip) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Details modal
            selectedDeviceForDetails?.let { dev ->
                DeviceDetailDialog(
                    device = dev,
                    onDismiss = { selectedDeviceForDetails = null },
                    onToggleBlock = {
                        viewModel.toggleBlockDevice(it)
                        selectedDeviceForDetails = it.copy(isBlocked = !it.isBlocked)
                    },
                    onToggleSuspicious = {
                        viewModel.toggleSuspiciousDevice(it)
                        selectedDeviceForDetails = it.copy(isSuspicious = !it.isSuspicious)
                    },
                    onSendWol = { viewModel.sendWakeOnLan(it) },
                    onScanPorts = { ip ->
                        selectedDeviceForDetails = null
                        onNavigateToPorts(ip)
                    },
                    onUpdateName = { d, name ->
                        viewModel.updateDeviceCustomName(d, name)
                        selectedDeviceForDetails = d.copy(customName = name)
                    }
                )
            }
        }
    }
}

@Composable
fun NetworkOverviewCard(
    stats: com.example.data.model.NetworkStats,
    onExportPdf: () -> Unit,
    onStartScan: () -> Unit,
    onOpenAnalytics: () -> Unit = {},
    onOpenRadar: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("network_overview_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: SSID and Scan/PDF actions
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
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stats.ssid,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gateway: ${stats.gatewayIp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onStartScan,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan)
                            .testTag("refresh_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Subnet",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subnet Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Local IP", value = stats.localIp)
                MetricItem(label = "Subnet", value = stats.subnetMask)
                MetricItem(label = "Avg Ping", value = "${stats.averagePingMs} ms", highlightColor = CyberCyan)
                MetricItem(
                    label = "Blocked",
                    value = "${stats.blockedDevices}",
                    highlightColor = if (stats.blockedDevices > 0) NeonRed else null
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick access to Analytics & Radar Map
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAnalytics,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_analytics"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live Analytics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onOpenRadar,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_radar"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RF Radar (No Root)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, highlightColor: Color? = null) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ScanProgressCard(progress: Float, currentIp: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Subnet Sweep...",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyberCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Probing: $currentIp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelect: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_search_input"),
            placeholder = { Text("Search by IP, MAC, Vendor, or Name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "ALL" to "All Devices",
                "ONLINE" to "Online",
                "BLOCKED" to "Blocked",
                "SUSPICIOUS" to "Suspicious",
                "ROUTER" to "Routers",
                "PHONE" to "Phones",
                "IOT" to "IoT / Audio"
            )

            filters.forEach { (key, label) ->
                val selected = selectedFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelect(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan
                    )
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: NetworkDevice,
    onClick: () -> Unit,
    onSendWol: () -> Unit,
    onToggleBlock: () -> Unit,
    onScanPorts: () -> Unit
) {
    val icon = when (device.deviceType) {
        DeviceType.ROUTER -> Icons.Default.Router
        DeviceType.PHONE -> Icons.Default.PhoneAndroid
        DeviceType.LAPTOP -> Icons.Default.Laptop
        DeviceType.SMART_TV -> Icons.Default.Tv
        DeviceType.SPEAKER -> Icons.Default.Speaker
        DeviceType.SMARTWATCH -> Icons.Default.Watch
        DeviceType.IOT -> Icons.Default.Sensors
        else -> Icons.Default.Devices
    }

    val cardBorder = when {
        device.isBlocked -> NeonRed.copy(alpha = 0.6f)
        device.isSuspicious -> NeonAmber.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("device_card_${device.ip}"),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isBlocked) NeonRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (device.isBlocked) NeonRed.copy(alpha = 0.2f)
                                else CyberCyan.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (device.isBlocked) NeonRed else CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${device.ip} • ${device.mac}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // Ping badge & status
                Column(horizontalAlignment = Alignment.End) {
                    if (device.isBlocked) {
                        StatusBadge(label = "BLOCKED", color = NeonRed)
                    } else if (device.isSuspicious) {
                        StatusBadge(label = "SUSPICIOUS", color = NeonAmber)
                    } else {
                        PingBadge(pingMs = device.pingMs)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = device.osName.take(14),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.vendor,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // WoL Button
                    IconButton(
                        onClick = onSendWol,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("wol_button_${device.ip}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Wake-on-LAN",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Scan Ports Button
                    IconButton(
                        onClick = onScanPorts,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("port_scan_button_${device.ip}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Scan Ports",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Block/Unblock toggle
                    IconButton(
                        onClick = onToggleBlock,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (device.isBlocked) NeonRed.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("block_button_${device.ip}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = if (device.isBlocked) "Unblock" else "Block",
                            tint = if (device.isBlocked) NeonRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PingBadge(pingMs: Long) {
    val color = when {
        pingMs < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        pingMs < 30 -> NeonGreen
        pingMs < 80 -> CyberCyan
        else -> NeonAmber
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = if (pingMs >= 0) "$pingMs ms" else "Offline",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyDevicesCard(onScanAgain: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No devices detected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Scan your local Wi-Fi subnet to find connected hardware, routers, IoT, and smart devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onScanAgain,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Full Network Scan")
            }
        }
    }
}
