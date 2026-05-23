package com.usbdroid.ui.screens.modules

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.usbdroid.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DeviceAnalyzerScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Descriptor", "Hex Dump", "Test")
    val descriptorText = remember {
        buildString {
            appendLine("=".repeat(58))
            appendLine("  USBDROID - USB Device Descriptor Report")
            appendLine("  Generated: 2024-01-15 10:23:45")
            appendLine("=".repeat(58))
            appendLine()
            appendLine("+--------------------------------------------------------+")
            appendLine("|              DEVICE DESCRIPTOR                         |")
            appendLine("+--------------------------------------------------------+")
            appendLine("|  bLength:              18                              |")
            appendLine("|  bDescriptorType:      0x01 (DEVICE)                   |")
            appendLine("|  bcdUSB:               0x0200 (USB 2.0)                |")
            appendLine("|  bDeviceClass:         0x00 (Use Interface)            |")
            appendLine("|  bDeviceSubClass:      0x00                            |")
            appendLine("|  bDeviceProtocol:      0x00                            |")
            appendLine("|  bMaxPacketSize0:      64                              |")
            appendLine("|  idVendor:             0x18D1 (Google Inc.)            |")
            appendLine("|  idProduct:            0x4EE7 (Nexus ADB)              |")
            appendLine("|  bcdDevice:            0x0100                          |")
            appendLine("|  iManufacturer:        Google                          |")
            appendLine("|  iProduct:             Pixel 7 Pro                     |")
            appendLine("|  iSerialNumber:        18291FDF4003EM                  |")
            appendLine("|  bNumConfigurations:   1                               |")
            appendLine("+--------------------------------------------------------+")
            appendLine()
            appendLine("+--------------------------------------------------------+")
            appendLine("|  CONFIGURATION #0                                      |")
            appendLine("+--------------------------------------------------------+")
            appendLine("|  bNumInterfaces:       2                               |")
            appendLine("|  bConfigurationValue:  1                               |")
            appendLine("|  bmAttributes:         0x80 (Bus Powered)              |")
            appendLine("|  bMaxPower:            500mA                           |")
            appendLine("+--------------------------------------------------------+")
            appendLine()
            appendLine("  +----------------------------------------------------+")
            appendLine("  |  INTERFACE #0 - ADB (Android Debug Bridge)         |")
            appendLine("  +----------------------------------------------------+")
            appendLine("  |  bInterfaceNumber:     0                             |")
            appendLine("  |  bInterfaceClass:      0xFF (Vendor Specific)        |")
            appendLine("  |  bInterfaceSubClass:   0x42 (ADB)                    |")
            appendLine("  |  bInterfaceProtocol:   0x01                          |")
            appendLine("  |  bNumEndpoints:        2                             |")
            appendLine("  |                                                      |")
            appendLine("  |  ENDPOINT 0x01 (OUT)  - Bulk transfer                |")
            appendLine("  |  ENDPOINT 0x81 (IN)   - Bulk transfer                |")
            appendLine("  +----------------------------------------------------+")
            appendLine()
            appendLine("  +----------------------------------------------------+")
            appendLine("  |  INTERFACE #1 - MTP (Media Transfer)                |")
            appendLine("  +----------------------------------------------------+")
            appendLine("  |  bInterfaceNumber:     1                             |")
            appendLine("  |  bInterfaceClass:      0x06 (Image)                  |")
            appendLine("  |  bInterfaceSubClass:   0x01 (Still Imaging)          |")
            appendLine("  |  bInterfaceProtocol:   0x01                          |")
            appendLine("  |  bNumEndpoints:        3                             |")
            appendLine("  +----------------------------------------------------+")
            appendLine()
            appendLine("+--------------------------------------------------------+")
            appendLine("|  ANALYSIS SUMMARY                                      |")
            appendLine("+--------------------------------------------------------+")
            appendLine("|  Device Class:         Per-interface configuration     |")
            appendLine("|  Likely Type:          Google/Android Device           |")
            appendLine("|  ADB Interface:        Yes (subclass 0x42)             |")
            appendLine("|  MTP Interface:        Yes (subclass 0x01)             |")
            appendLine("|  Security:             May need ADB RSA authentication |")
            appendLine("|  Recommended Module:   ADB Controller                  |")
            appendLine("+--------------------------------------------------------+")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Device Header
        Surface(color = Surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(OnSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, OnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("USB Device Analyzer", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                    Text("Full descriptor dump and reverse engineering", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
            divider = { Divider(color = Divider) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) },
                    selectedContentColor = PrimaryCyan,
                    unselectedContentColor = OnSurfaceVariant
                )
            }
        }

        when (selectedTab) {
            0 -> DescriptorTab(text = descriptorText)
            1 -> HexDumpTab()
            2 -> EndpointTestTab()
        }
    }
}

@Composable
private fun DescriptorTab(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(TerminalBackground, RoundedCornerShape(8.dp))
            .border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val lines = text.lines()
            items(lines.size) { index ->
                val line = lines[index]
                val color = when {
                    line.contains("0x") && line.contains("|") -> TerminalGreen
                    line.contains("+") || line.contains("=") -> PrimaryCyan
                    line.contains("ADB", ignoreCase = true) -> WarningOrange
                    line.contains("Google") || line.contains("Pixel") -> Color(0xFF64B5F6)
                    line.contains("Recommended") -> SuccessGreen
                    line.trim().startsWith("|") -> OnBackground.copy(alpha = 0.7f)
                    else -> OnSurfaceVariant
                }
                Text(
                    text = line,
                    color = color,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HexDumpTab() {
    val hexData = remember {
        (0 until 256).map { "%02X".format((it * 7 + 13) % 256) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(TerminalBackground, RoundedCornerShape(8.dp))
            .border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            items(16) { row ->
                Row {
                    // Address
                    Text(
                        "${(row * 16).toString(16).padStart(6, '0')}:  ",
                        color = PrimaryCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    // Hex bytes
                    (0 until 16).forEach { col ->
                        val idx = row * 16 + col
                        if (idx < hexData.size) {
                            Text(
                                "${hexData[idx]} ",
                                color = TerminalGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    // ASCII
                    Text(" |", color = OnSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    (0 until 16).forEach { col ->
                        val idx = row * 16 + col
                        if (idx < hexData.size) {
                            val byteVal = hexData[idx].toInt(16)
                            val char = if (byteVal in 32..126) byteVal.toChar() else '.'
                            Text(
                                "$char",
                                color = if (char == '.') OnSurfaceVariant else OnBackground,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text("|", color = OnSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun EndpointTestTab() {
    val endpoints = listOf(
        Triple("0x01", "Bulk OUT", "Send data to device"),
        Triple("0x81", "Bulk IN", "Read data from device"),
        Triple("0x02", "Bulk OUT", "Send data (MTP)")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        endpoints.forEach { (addr, type, desc) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            addr,
                            color = PrimaryCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(type, color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = OnSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Send Test Data")
                        }
                        OutlinedButton(
                            onClick = { },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Read Data")
                        }
                    }
                }
            }
        }

        // Custom data send
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Custom Transfer", style = MaterialTheme.typography.titleSmall, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                var dataText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = dataText,
                    onValueChange = { dataText = it },
                    placeholder = { Text("Hex data (e.g., 01 02 03 04)...") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = Border
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                ) {
                    Text("SEND CUSTOM DATA")
                }
            }
        }
    }
}
