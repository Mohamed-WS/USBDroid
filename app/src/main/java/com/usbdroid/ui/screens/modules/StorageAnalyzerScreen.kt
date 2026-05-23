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
fun StorageAnalyzerScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Partitions", "Files", "Benchmark", "Health")

    Column(modifier = Modifier.fillMaxSize()) {
        // Device Info Header
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
                        .background(Color(0xFF2196F3).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storage, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("SanDisk Ultra USB 3.0", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                    Text("119.2 GB | USB 3.0 | SCSI Transparent", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = Color(0xFF2196F3),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color(0xFF2196F3)
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
                    selectedContentColor = Color(0xFF2196F3),
                    unselectedContentColor = OnSurfaceVariant
                )
            }
        }

        when (selectedTab) {
            0 -> PartitionsTab()
            1 -> FilesTab()
            2 -> BenchmarkTab()
            3 -> HealthTab()
        }
    }
}

@Composable
private fun PartitionsTab() {
    val partitions = listOf(
        Triple("FAT32", "119.2 GB", 0.45f),
        Triple("Unallocated", "0 bytes", 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Visual partition bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                Text("Partition Map", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(16.dp))

                // Visual bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant)
                ) {
                    partitions.forEach { (type, _, percent) ->
                        if (percent > 0) {
                            val color = when (type) {
                                "FAT32" -> Color(0xFF2196F3)
                                "NTFS" -> Color(0xFF4CAF50)
                                "exFAT" -> Color(0xFFFF9800)
                                "ext4" -> Color(0xFF9C27B0)
                                else -> OnSurfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(percent)
                                    .background(color)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    partitions.filter { it.third > 0 }.forEach { (type, size, _) ->
                        val color = when (type) {
                            "FAT32" -> Color(0xFF2196F3)
                            "NTFS" -> Color(0xFF4CAF50)
                            "exFAT" -> Color(0xFFFF9800)
                            "ext4" -> Color(0xFF9C27B0)
                            else -> OnSurfaceVariant
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$type ($size)", color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Partition Details
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Partition Details", style = MaterialTheme.typography.titleSmall, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    mapOf("Partition" to "1", "Type" to "FAT32", "Start" to "0x00000800", "Size" to "119.2 GB", "Status" to "Active"),
                ).forEach { part ->
                    part.forEach { (key, value) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(key, color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                            Text(value, color = OnBackground, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesTab() {
    val files = listOf(
        Triple("Documents", true, "128 MB"),
        Triple("Photos", true, "2.4 GB"),
        Triple("Videos", true, "8.7 GB"),
        Triple("Music", true, "1.2 GB"),
        Triple("firmware.bin", false, "256 KB"),
        Triple("backup.zip", false, "45 MB"),
        Triple("readme.txt", false, "4 KB"),
        Triple("data.csv", false, "12 MB"),
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(files) { (name, isDir, size) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDir) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = if (isDir) WarningOrange else PrimaryCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, color = OnBackground, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(size, color = OnSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Divider(color = Divider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun BenchmarkTab() {
    val results = remember {
        mapOf(
            "Sequential Read" to (145.2 to "MB/s"),
            "Sequential Write" to (67.8 to "MB/s"),
            "Random Read IOPS" to (4200.0 to "IOPS"),
            "Random Write IOPS" to (1800.0 to "IOPS"),
            "Access Time" to (0.45 to "ms")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RUN BENCHMARK", fontWeight = FontWeight.Bold)
        }

        results.forEach { (label, result) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = OnSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${result.first} ${result.second}",
                            color = OnBackground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // SMART-like info
        val healthData = listOf(
            "Device Model" to "SanDisk Ultra USB 3.0",
            "Serial Number" to "4C530001230512109462",
            "Firmware Version" to "1.00",
            "USB Version" to "3.0 (5 Gbps)",
            "Power" to "500mA (Bus powered)",
            "Temperature" to "34°C",
            "Power-on Hours" to "1,247",
            "Power-on Count" to "342",
            "Status" to "Healthy"
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Drive Health", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))

                healthData.forEach { (key, value) ->
                    val isStatus = key == "Status"
                    val valueColor = if (isStatus && value == "Healthy") SuccessGreen else OnBackground
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(key, color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(140.dp))
                        Text(value, color = valueColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isStatus) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        // Temperature gauge
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Temperature", style = MaterialTheme.typography.titleSmall, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = 0.34f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = SuccessGreen,
                    trackColor = SurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("34°C - Normal operating temperature", color = OnSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}
