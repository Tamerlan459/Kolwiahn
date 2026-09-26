package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SmartDeviceCategory
import com.example.data.model.SmartHomeDevice
import com.example.data.model.SmartProtocol
import com.example.service.smarthome.SmartHomeManager
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.MainViewModel

@Composable
fun SmartHomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val smartDevices by viewModel.smartDevices.collectAsStateWithLifecycle()
    val isMultiroomPlaying by viewModel.isMultiroomPlaying.collectAsStateWithLifecycle()
    val masterVolume by viewModel.masterVolume.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentStreamTrack.collectAsStateWithLifecycle()
    val isZigbeePairing by viewModel.isZigbeePairing.collectAsStateWithLifecycle()

    val router = smartDevices.find { it.category == SmartDeviceCategory.ROUTER }
    val speakers = smartDevices.filter { it.category == SmartDeviceCategory.SPEAKER }
    val sensors = smartDevices.filter { it.category != SmartDeviceCategory.ROUTER && it.category != SmartDeviceCategory.SPEAKER }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "Smart Home & Multi-Room Audio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Unified management for Matter 1.3, Zigbee 3.0 Mesh, Routers and Multi-Room Cast.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Router & Matter / Zigbee Bridge Card
        if (router != null) {
            item {
                RouterGatewayCard(
                    router = router,
                    isZigbeePairing = isZigbeePairing,
                    onToggleZigbeePairing = { viewModel.toggleZigbeePairing() },
                    onCommissionMatter = { viewModel.commissionMatterDevice("Smart Room Speaker", "Studio") },
                    onOpenAdmin = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(router.adminUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Multi-Room Master Audio Controller with Source Picker
        item {
            MultiroomMasterCard(
                isPlaying = isMultiroomPlaying,
                currentTrack = currentTrack,
                masterVolume = masterVolume,
                groupedCount = speakers.count { it.isMultiroomGrouped },
                onTogglePlay = { viewModel.toggleMultiroomPlayback() },
                onVolumeChange = { viewModel.setSmartMasterVolume(it) },
                onSelectStream = { viewModel.selectAudioStream(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Individual Speaker Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Multi-Room Speakers (${speakers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${speakers.count { it.isMultiroomGrouped }} Grouped",
                    fontSize = 11.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(speakers, key = { it.id }) { speaker ->
            SmartSpeakerCard(
                speaker = speaker,
                onVolumeChange = { viewModel.setSpeakerVolume(speaker.id, it) },
                onToggleMute = { viewModel.toggleSpeakerMute(speaker.id) },
                onToggleGroup = { viewModel.toggleMultiroomGroup(speaker.id) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Zigbee & Matter Sensor Hub
        if (sensors.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Zigbee 3.0 & Matter Mesh Nodes (${sensors.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(sensors, key = { it.id }) { sensor ->
                SmartMeshNodeCard(node = sensor)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RouterGatewayCard(
    router: SmartHomeDevice,
    isZigbeePairing: Boolean,
    onToggleZigbeePairing: () -> Unit,
    onCommissionMatter: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = router.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gateway: ${router.ip} • Matter & Zigbee Hub",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "COORDINATOR",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Protocol badges: Matter + Zigbee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberCyan.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "MATTER 1.3 FABRIC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        Text(text = "${router.matterFabric} (${router.matterNodeId})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonAmber.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "ZIGBEE 3.0 MESH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                        Text(text = "Ch ${router.zigbeeChannel} • LQI ${router.zigbeeLqi}/255", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bandwidth and active client metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Active Clients", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${router.wifiClientsCount} Devices", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(13.dp))
                        Text(text = " Download", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "${router.bandwidthDownMbps} Mbps", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(13.dp))
                        Text(text = " Upload", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "${router.bandwidthUpMbps} Mbps", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: Zigbee Permit Join & Matter Commissioning & Router Web Admin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleZigbeePairing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isZigbeePairing) NeonAmber else MaterialTheme.colorScheme.surface,
                        contentColor = if (isZigbeePairing) Color.Black else NeonAmber
                    )
                ) {
                    Text(if (isZigbeePairing) "Pairing ON (180s)" else "Zigbee Join", fontSize = 11.sp)
                }

                Button(
                    onClick = onCommissionMatter,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan)
                ) {
                    Text("+ Matter Node", fontSize = 11.sp)
                }

                Button(
                    onClick = onOpenAdmin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) {
                    Text("Admin Web", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MultiroomMasterCard(
    isPlaying: Boolean,
    currentTrack: String,
    masterVolume: Float,
    groupedCount: Int,
    onTogglePlay: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectStream: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SpeakerGroup,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Multi-Room Synchronized Audio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$groupedCount Speakers in Sync • Matter / Cast / AirPlay",
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberCyan)
                        .testTag("multiroom_play_toggle")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Track banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "CURRENT LOSSLESS STREAM", fontSize = 9.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        Text(text = currentTrack, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Text(text = "24-bit/96kHz", fontSize = 10.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Audio Stream Selector Chips
            Text(text = "Switch Audio Feed / Source:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartHomeManager.AVAILABLE_AUDIO_STREAMS.forEach { streamName ->
                    val isSelected = streamName == currentTrack
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { onSelectStream(streamName) },
                        label = { Text(streamName.take(24), fontSize = 11.sp) },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Master Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VolumeDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = masterVolume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyberCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "${(masterVolume * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SmartSpeakerCard(
    speaker: SmartHomeDevice,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleGroup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = null,
                            tint = if (speaker.isPlaying) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = speaker.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(speaker.protocol.badgeColorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = speaker.protocol.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(speaker.protocol.badgeColorHex),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(text = "${speaker.room} • ${speaker.ip}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Multi-Room", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Checkbox(
                        checked = speaker.isMultiroomGrouped,
                        onCheckedChange = { onToggleGroup() },
                        colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleMute, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (speaker.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeDown,
                        contentDescription = "Mute",
                        tint = if (speaker.isMuted) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Slider(
                    value = if (speaker.isMuted) 0f else speaker.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )

                Text(
                    text = if (speaker.isMuted) "Muted" else "${(speaker.volume * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SmartMeshNodeCard(node: SmartHomeDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                        .background(NeonAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = node.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "${node.room} • Channel ${node.zigbeeChannel}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeonGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "LQI: ${node.zigbeeLqi}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
