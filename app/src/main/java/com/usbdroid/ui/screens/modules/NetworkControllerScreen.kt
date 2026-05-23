package com.usbdroid.ui.screens.modules

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.ui.theme.*

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NetworkControllerScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Info", "Scan", "Tools")

    Column(modifier = Modifier.fillMaxSize()) {
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
                        .background(Color(0xFF00BCD4).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF00BCD4).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.NetworkWifi, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("USB Ethernet Adapter", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                    Text("CDC-ECM | 1 Gbps | Link Up", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = Color(0xFF00BCD4),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color(0xFF00BCD4)
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
                    selectedContentColor = Color(0xFF00BCD4),
                    unselectedContentColor = OnSurfaceVariant
                )
            }
        }

        when (selectedTab) {
            0 -> NetworkInfoTab()
            1 -> NetworkScanTab()
            2 -> NetworkToolsTab()
        }
    }
}

@Composable
private fun NetworkInfoTab() {
    val info = mapOf(
        "Interface" to "eth1 (USB-ETH)",
        "MAC Address" to "00:1A:2B:3C:4D:5E",
        "IP Address" to "192.168.1.105/24",
        "Gateway" to "192.168.1.1",
        "DNS" to "8.8.8.8, 8.8.4.4",
        "MTU" to "1500",
        "Link Speed" to "1000 Mbps",
        "Duplex" to "Full",
        "RX Packets" to "1,234,567",
        "TX Packets" to "987,654",
        "RX Bytes" to "1.2 GB",
        "TX Bytes" to "456 MB"
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
                    modifier = Modifier.width(120.dp)
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

@Composable
private fun NetworkScanTab() {
    var isScanning by remember { mutableStateOf(false) }
    val scanResults = remember {
        mutableStateListOf(
            ScanResult("192.168.1.1", "router.local", true, listOf(80, 443, 22)),
            ScanResult("192.168.1.105", "android-abc123", true, listOf(5555)),
            ScanResult("192.168.1.110", "raspberrypi", true, listOf(22, 80)),
            ScanResult("192.168.1.150", "printer-lexmark", true, listOf(80, 443, 9100)),
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { isScanning = !isScanning },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                if (isScanning) Icons.Filled.Stop else Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isScanning) "STOP SCAN" else "SCAN NETWORK", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(scanResults) { result ->
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SuccessGreen, androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(result.ip, color = OnBackground, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(result.hostname, color = OnSurfaceVariant, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            result.ports.forEach { port ->
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryCyan.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Port $port", color = PrimaryCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ScanResult(val ip: String, val hostname: String, val reachable: Boolean, val ports: List<Int>)

@Composable
private fun NetworkToolsTab() {
    var pingHost by remember { mutableStateOf("8.8.8.8") }
    var pingResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var dnsQuery by remember { mutableStateOf("") }
    var dnsResults by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Ping Tool
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ping", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pingHost,
                        onValueChange = { pingHost = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter host...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00BCD4),
                            unfocusedBorderColor = Border
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            pingResults = listOf(
                                "PING $pingHost: 56 data bytes",
                                "64 bytes from $pingHost: seq=0 ttl=118 time=12.4 ms",
                                "64 bytes from $pingHost: seq=1 ttl=118 time=11.8 ms",
                                "64 bytes from $pingHost: seq=2 ttl=118 time=13.2 ms",
                                "64 bytes from $pingHost: seq=3 ttl=118 time=12.0 ms",
                                "",
                                "--- $pingHost ping statistics ---",
                                "4 packets transmitted, 4 received, 0% packet loss",
                                "round-trip min/avg/max = 11.8/12.4/13.2 ms"
                            )
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF00BCD4))
                    ) {
                        Icon(Icons.Filled.NetworkPing, contentDescription = "Ping", modifier = Modifier.size(20.dp))
                    }
                }
                if (pingResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerminalBackground, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            pingResults.forEach { line ->
                                val color = when {
                                    line.contains("seq=") -> TerminalGreen
                                    line.contains("statistics") -> WarningOrange
                                    else -> OnSurfaceVariant
                                }
                                Text(line, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // DNS Lookup
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("DNS Lookup", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = dnsQuery,
                        onValueChange = { dnsQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter hostname...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00BCD4),
                            unfocusedBorderColor = Border
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            dnsResults = listOf(
                                "$dnsQuery has address 142.250.185.78",
                                "$dnsQuery has IPv6 address 2607:f8b0:4004:c06::64"
                            )
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF00BCD4))
                    ) {
                        Icon(Icons.Filled.Dns, contentDescription = "DNS", modifier = Modifier.size(20.dp))
                    }
                }
                if (dnsResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    dnsResults.forEach { line ->
                        Text(line, color = TerminalGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
