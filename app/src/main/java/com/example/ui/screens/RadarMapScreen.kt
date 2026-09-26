package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.radar.RadarBlip
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarMapScreen(viewModel: MainViewModel) {
    val radarBlips by viewModel.radarBlips.collectAsStateWithLifecycle()
    val gpsLocation by viewModel.userGpsLocation.collectAsStateWithLifecycle()
    val isGpsActive by viewModel.isGpsActive.collectAsStateWithLifecycle()
    var selectedBlip by remember { mutableStateOf<RadarBlip?>(null) }

    // Keep selectedBlip synced if list updates
    val activeSelected = selectedBlip?.let { sel -> radarBlips.find { it.id == sel.id } } ?: radarBlips.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "Geolocation & RF Proximity Radar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Locate Wi-Fi stations & Bluetooth devices via Trilateration & RSSI Ranging (No-Root).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // GPS Geolocation Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isGpsActive) NeonGreen.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (isGpsActive) NeonGreen else CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (gpsLocation != null) "GPS Fixed (Zero-Root Ranging)" else "Network Geolocation Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (gpsLocation != null)
                                    String.format("Lat: %.5f, Lon: %.5f (±%.0fm)", gpsLocation!!.latitude, gpsLocation!!.longitude, gpsLocation!!.accuracy)
                                else "Calculating spatial triangulation coordinates...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.refreshLocationRadar() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh GPS", tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Animated Radar Screen Component
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radar_map_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B16)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RadarCanvas(
                        blips = radarBlips,
                        selectedBlipId = activeSelected?.id,
                        onSelectBlip = { blip -> selectedBlip = blip }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Radar range legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "R1: 1.0 m", fontSize = 10.sp, color = NeonGreen)
                        Text(text = "R2: 3.5 m", fontSize = 10.sp, color = CyberCyan)
                        Text(text = "R3: 7.0 m", fontSize = 10.sp, color = PurpleAccent)
                        Text(text = "Max: 15.0 m", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hot/Cold Proximity Tracker for Selected Device
        activeSelected?.let { target ->
            item {
                ProximityTargetTrackerCard(blip = target)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Nearby Radios List Header
        item {
            Text(
                text = "Nearby Radio Stations & BLE Beacons (${radarBlips.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(radarBlips, key = { it.id }) { blip ->
            RadarBlipListItem(
                blip = blip,
                isSelected = blip.id == activeSelected?.id,
                onClick = { selectedBlip = blip }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun RadarCanvas(
    blips: List<RadarBlip>,
    selectedBlipId: String?,
    onSelectBlip: (RadarBlip) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(blips) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f * 0.95f
                        val tapped = blips.minByOrNull { b ->
                            val blipOffset = Offset(
                                center.x + b.xOffsetNormalized * radius,
                                center.y + b.yOffsetNormalized * radius
                            )
                            (offset - blipOffset).getDistance()
                        }
                        if (tapped != null) {
                            onSelectBlip(tapped)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f * 0.95f

            // Concentric range circles
            val ringFractions = listOf(0.2f, 0.45f, 0.72f, 1.0f)
            ringFractions.forEach { frac ->
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.18f),
                    radius = radius * frac,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Crosshairs
            drawLine(
                color = CyberCyan.copy(alpha = 0.2f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CyberCyan.copy(alpha = 0.2f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )

            // Radar sweep beam line & gradient sector
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val beamEnd = Offset(
                (center.x + cos(sweepRad) * radius).toFloat(),
                (center.y + sin(sweepRad) * radius).toFloat()
            )

            drawLine(
                color = CyberCyan.copy(alpha = 0.8f),
                start = center,
                end = beamEnd,
                strokeWidth = 2.dp.toPx()
            )

            // Center device blip (User Phone)
            drawCircle(
                color = NeonGreen,
                radius = 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = NeonGreen.copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = center
            )

            // Draw device blips
            blips.forEach { blip ->
                val blipX = center.x + blip.xOffsetNormalized * radius
                val blipY = center.y + blip.yOffsetNormalized * radius
                val blipOffset = Offset(blipX, blipY)

                val isSelected = blip.id == selectedBlipId
                val blipColor = when {
                    isSelected -> NeonAmber
                    blip.isWifi -> CyberCyan
                    else -> PurpleAccent
                }

                if (isSelected) {
                    drawCircle(
                        color = NeonAmber.copy(alpha = 0.35f),
                        radius = 16.dp.toPx(),
                        center = blipOffset
                    )
                }

                drawCircle(
                    color = blipColor,
                    radius = if (isSelected) 8.dp.toPx() else 5.5f.dp.toPx(),
                    center = blipOffset
                )
            }
        }
    }
}

@Composable
fun ProximityTargetTrackerCard(blip: RadarBlip) {
    val tempColor = when {
        blip.distanceMeters < 1.0f -> NeonRed
        blip.distanceMeters < 3.5f -> NeonAmber
        else -> CyberCyan
    }

    val tempLabel = when {
        blip.distanceMeters < 1.0f -> "HOT • IMMEDIATE VICINITY"
        blip.distanceMeters < 3.5f -> "WARM • NEARBY IN ROOM"
        else -> "COOL • DISTANT / OUT OF ROOM"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, tempColor.copy(alpha = 0.5f))
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(tempColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (blip.isWifi) Icons.Default.Wifi else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = tempColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = blip.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = blip.addressOrIp, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tempColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = tempLabel,
                        color = tempColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large distance indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "ESTIMATED DISTANCE (PATH-LOSS)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${blip.distanceMeters} meters",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tempColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "SIGNAL POWER", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${blip.rssi} dBm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }
        }
    }
}

@Composable
fun RadarBlipListItem(
    blip: RadarBlip,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyberCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CyberCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (blip.isWifi) CyberCyan.copy(alpha = 0.15f) else PurpleAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (blip.isWifi) Icons.Default.Wifi else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (blip.isWifi) CyberCyan else PurpleAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = blip.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "${blip.addressOrIp} • ${if (blip.isWifi) "Wi-Fi AP" else "BLE Beacon"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${blip.distanceMeters} m",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
                Text(
                    text = "${blip.rssi} dBm",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
