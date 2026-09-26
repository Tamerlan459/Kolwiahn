package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SpeakerGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.BluetoothScreen
import com.example.ui.screens.PortScannerScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SmartHomeScreen
import com.example.ui.screens.WebServerScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var scannerSubTab by rememberSaveable { mutableIntStateOf(0) }
    val stats by viewModel.networkStats.collectAsStateWithLifecycle()

    // Handle back button when on sub-tab
    BackHandler(enabled = currentTab != 0 || scannerSubTab != 0) {
        if (scannerSubTab != 0) {
            scannerSubTab = 0
        } else {
            currentTab = 0
        }
    }

    // Permission launcher for Bluetooth & Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled gracefully
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Listen for user messages / notifications
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NetPulse Pro",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = NeonGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                                    text = stats.ssid.take(14),
                                    fontSize = 10.sp,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            currentTab = 0
                            scannerSubTab = 1
                        },
                        modifier = Modifier.testTag("top_action_analytics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Network Analytics",
                            tint = if (currentTab == 0 && scannerSubTab == 1) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            currentTab = 0
                            scannerSubTab = 2
                        },
                        modifier = Modifier.testTag("top_action_radar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Proximity Radar",
                            tint = if (currentTab == 0 && scannerSubTab == 2) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = {
                        if (currentTab == 0) {
                            scannerSubTab = 0
                        } else {
                            currentTab = 0
                        }
                    },
                    icon = {
                        Icon(
                            if (currentTab == 0) Icons.Filled.Radar else Icons.Outlined.Radar,
                            contentDescription = "Scanner"
                        )
                    },
                    label = { Text("Scanner", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_scanner")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            if (currentTab == 1) Icons.Filled.Security else Icons.Outlined.Security,
                            contentDescription = "Ports"
                        )
                    },
                    label = { Text("Ports", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_ports")
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            if (currentTab == 2) Icons.Filled.Bluetooth else Icons.Outlined.Bluetooth,
                            contentDescription = "Wireless"
                        )
                    },
                    label = { Text("Wireless", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_wireless")
                )

                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        Icon(
                            if (currentTab == 3) Icons.Filled.SpeakerGroup else Icons.Outlined.SpeakerGroup,
                            contentDescription = "Smart Home"
                        )
                    },
                    label = { Text("Smart Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_smarthome")
                )

                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = {
                        Icon(
                            if (currentTab == 4) Icons.Filled.Language else Icons.Outlined.Language,
                            contentDescription = "Web & Sec"
                        )
                    },
                    label = { Text("Web & Sec", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_web")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                0 -> ScanScreen(
                    viewModel = viewModel,
                    activeSubTab = scannerSubTab,
                    onSubTabChange = { scannerSubTab = it },
                    onNavigateToPorts = { targetIp ->
                        viewModel.startPortScan(targetIp)
                        currentTab = 1
                    },
                    onPdfReady = { file ->
                        viewModel.sharePdf(file)
                    }
                )
                1 -> PortScannerScreen(viewModel = viewModel)
                2 -> BluetoothScreen(viewModel = viewModel)
                3 -> SmartHomeScreen(viewModel = viewModel)
                4 -> WebServerScreen(
                    viewModel = viewModel,
                    onPdfReady = { file ->
                        viewModel.sharePdf(file)
                    },
                    onApkReady = { file ->
                        viewModel.shareApk(file)
                    }
                )
            }
        }
    }
}
