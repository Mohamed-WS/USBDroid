package com.usbdroid.ui.screens.modules

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ADBControllerScreen(
    onNavigateToUsbDialog: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Shell", "Logcat", "Files", "Packages", "Info")
    var isConnected by remember { mutableStateOf(false) }
    var commandText by remember { mutableStateOf("") }

    val shellOutput = remember {
        mutableStateListOf(
            "Android Debug Bridge shell",
            "Type 'help' for available commands",
            "",
            "hammerhead:/ $ ls /system/bin",
            "[output truncated - 284 binaries]",
            "hammerhead:/ $ cat /proc/cpuinfo",
            "Processor       : ARMv7 Processor rev 0 (v7l)",
            "BogoMIPS        : 38.40",
            "Features        : swp half thumb fastmult vfp edsp neon vfpv3 tls vfpv4 idiva idivt",
            "CPU implementer : 0x51",
            "CPU architecture: 7",
            "CPU variant     : 0x1",
            "CPU part        : 0x06f",
            "CPU revision    : 0",
            "",
            "Hardware        : Qualcomm MSM 8974 HAMMERHEAD (Flattened Device Tree)",
            "Revision        : 0000",
            "Serial          : 0000000000000000",
            "",
            "hammerhead:/ $ getprop | grep version",
            "[ro.build.version.release]: [13]",
            "[ro.build.version.sdk]: [33]",
            "[ro.build.version.security_patch]: [2023-08-05]",
            "",
            "hammerhead:/ $"
        )
    }

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Bar
        Surface(color = Surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isConnected = !isConnected },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) ErrorRed else SuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(if (isConnected) "DISCONNECT" else "CONNECT", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (isConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SuccessGreen, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "adb shell connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Text(
                        "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onNavigateToUsbDialog, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Security, contentDescription = "USB Dialog", tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Key, contentDescription = "Auth", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = PrimaryCyan,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryCyan
                    )
                }
            },
            divider = { Divider(color = Divider, thickness = 1.dp) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = PrimaryCyan,
                    unselectedContentColor = OnSurfaceVariant
                )
            }
        }

        // Content
        when (selectedTab) {
            0 -> ShellTab(
                output = shellOutput,
                commandText = commandText,
                onCommandChange = { commandText = it },
                onSend = {
                    if (commandText.isNotBlank()) {
                        shellOutput.add("hammerhead:/ $ $commandText")
                        shellOutput.add("Executing...")
                        commandText = ""
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(100)
                            scrollState.animateScrollToItem(shellOutput.size - 1)
                        }
                    }
                },
                scrollState = scrollState
            )
            1 -> LogcatTab()
            2 -> FilesTab()
            3 -> PackagesTab()
            4 -> DeviceInfoTab()
        }
    }
}

@Composable
private fun ShellTab(
    output: List<String>,
    commandText: String,
    onCommandChange: (String) -> Unit,
    onSend: () -> Unit,
    scrollState: androidx.compose.foundation.lazy.LazyListState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .background(TerminalBackground, RoundedCornerShape(8.dp))
                .border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                items(output) { line ->
                    val color = when {
                        line.startsWith("hammerhead:/") -> PrimaryCyan
                        line.startsWith("[") -> Color(0xFF888899)
                        line.contains("error", ignoreCase = true) -> ErrorRed
                        line.contains("success", ignoreCase = true) -> SuccessGreen
                        else -> TerminalGreen
                    }
                    Text(
                        text = line,
                        style = TerminalTextStyle.copy(fontSize = 12.sp),
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = onCommandChange,
                placeholder = { Text("Enter ADB command...", color = OnSurfaceVariant.copy(alpha = 0.4f)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = Border
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryCyan, contentColor = Background)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun LogcatTab() {
    val logLines = remember {
        mutableStateListOf(
            LogLine("08-15 10:23:45.123", "D", "USBManager", "USB device attached: /dev/bus/usb/001/002", Color(0xFF64B5F6)),
            LogLine("08-15 10:23:45.145", "I", "ActivityManager", "Start proc com.android.settings", Color(0xFF81C784)),
            LogLine("08-15 10:23:45.167", "V", "Kernel", "usb 1-1: new high-speed USB device number 2 using xhci_hcd", Color(0xFFAAAAAA)),
            LogLine("08-15 10:23:45.189", "D", "UsbHostManager", "USB device attached: vid=18d1 pid=4ee7", Color(0xFF64B5F6)),
            LogLine("08-15 10:23:45.201", "I", "SystemServer", "Package com.usbdroid granted USB permission", Color(0xFF81C784)),
            LogLine("08-15 10:23:45.234", "W", "UsbService", "Slow USB enumeration: 45ms", WarningOrange),
            LogLine("08-15 10:23:45.456", "E", "NativeDaemon", "Insufficient permissions for /dev/usb/hid0", ErrorRed),
            LogLine("08-15 10:23:45.678", "I", "ConnectivityService", "USB tethering interface added", Color(0xFF81C784)),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(TerminalBackground, RoundedCornerShape(8.dp))
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(logLines) { log ->
                LogLineView(log)
            }
        }
    }
}

private data class LogLine(val time: String, val level: String, val tag: String, val message: String, val color: Color)

@Composable
private fun LogLineView(log: LogLine) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(log.time, color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(140.dp))
        LevelBadge(log.level, log.color)
        Spacer(modifier = Modifier.width(6.dp))
        Text(log.tag, color = PrimaryCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(140.dp))
        Text(log.message, color = OnBackground, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LevelBadge(level: String, color: Color) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            level,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun FilesTab() {
    val files = listOf(
        "/data/app", "/data/data", "/data/local/tmp",
        "/sdcard/Download", "/sdcard/Documents", "/sdcard/Pictures",
        "/system/bin", "/system/etc", "/system/lib",
        "/vendor/lib", "/vendor/bin"
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(files) { path ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(path, color = OnBackground, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
            Divider(color = Divider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun PackagesTab() {
    val packages = listOf(
        "com.android.settings", "com.android.systemui", "com.google.android.gms",
        "com.android.chrome", "com.google.android.apps.maps", "com.usbdroid",
        "com.termux", "com.google.android.apps.messaging", "com.android.vending"
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(packages) { pkg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Android, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(pkg, color = OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Uninstall", tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
            Divider(color = Divider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun DeviceInfoTab() {
    val info = mapOf(
        "Model" to "Pixel 7 Pro",
        "Android Version" to "13 (TQ3A.230805.001)",
        "API Level" to "33",
        "Build ID" to "TQ3A.230805.001",
        "Security Patch" to "2023-08-05",
        "Bootloader" to "gop9-0.5-8928520",
        "Baseband" to "g5300q-230627-230505-B-10210545",
        "Kernel" to "5.10.157-android13-4-00003-g3b2f77a938f9",
        "Uptime" to "3 days, 7 hours, 23 minutes",
        "Battery" to "78% (4.12V, 28.5°C)",
        "Serial" to "18291FDF4003EM",
        "Display" to "3120x1440 @ 120Hz",
        "RAM" to "12GB LPDDR5",
        "Storage" to "256GB UFS 3.1 (67% used)"
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(info.entries.toList()) { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    key,
                    color = OnSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(140.dp)
                )
                Text(
                    value,
                    color = OnBackground,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
            Divider(color = Divider, thickness = 0.5.dp)
        }
    }
}
