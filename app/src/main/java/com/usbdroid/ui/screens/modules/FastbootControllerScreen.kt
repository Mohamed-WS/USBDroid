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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.ui.theme.*

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FastbootControllerScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Variables", "Flash", "Partitions", "Reboot")

    Column(modifier = Modifier.fillMaxSize()) {
        // Warning Header
        Surface(
            color = Color(0xFF3D0A00),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "BOOTLOADER MODE",
                        color = WarningOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "High-risk operations. Incorrect flashing may brick your device.",
                        color = WarningOrange.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Divider(color = WarningOrange.copy(alpha = 0.3f), thickness = 1.dp)

        // Status
        Surface(color = Surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(SuccessGreen, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Fastboot connected: Google Pixel 7 Pro",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = WarningOrange,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = WarningOrange
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
                    selectedContentColor = WarningOrange,
                    unselectedContentColor = OnSurfaceVariant
                )
            }
        }

        when (selectedTab) {
            0 -> FastbootVariablesTab()
            1 -> FastbootFlashTab()
            2 -> FastbootPartitionsTab()
            3 -> FastbootRebootTab()
        }
    }
}

@Composable
private fun FastbootVariablesTab() {
    val variables = remember {
        mapOf(
            "version" to "0.5",
            "version-bootloader" to "gop9-0.5-8928520",
            "version-baseband" to "g5300q-230627-230505-B-10210545",
            "product" to "cheetah",
            "serialno" to "18291FDF4003EM",
            "secure" to "yes",
            "unlocked" to "yes",
            "max-download-size" to "268435456",
            "partition-type:boot" to "raw",
            "partition-size:boot" to "0x06000000",
            "partition-type:recovery" to "raw",
            "partition-size:recovery" to "0x06400000",
            "partition-type:system" to "ext4",
            "partition-size:system" to "0x00001000",
            "partition-type:vendor" to "ext4",
            "partition-size:vendor" to "0x00001000",
            "current-slot" to "a",
            "slot-count" to "2",
            "slot-suffixes" to "a,b",
            "battery-voltage" to "4123mV",
            "battery-soc-ok" to "yes",
            "off-mode-charge" to "0",
            "variant" to "US",
            "hw-revision" to "PVT"
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(variables.entries.toList()) { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    key,
                    color = WarningOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(200.dp)
                )
                Text(
                    value,
                    color = OnBackground,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
            Divider(color = Divider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun FastbootFlashTab() {
    var selectedFile by remember { mutableStateOf("") }
    var selectedPartition by remember { mutableStateOf("boot") }
    val partitions = listOf("boot", "recovery", "system", "vendor", "bootloader", "radio")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // File Selection
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Image", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedFile,
                    onValueChange = { selectedFile = it },
                    placeholder = { Text("Tap to browse .img or .zip files...") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarningOrange,
                        unfocusedBorderColor = Border
                    ),
                    trailingIcon = {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Browse", tint = OnSurfaceVariant)
                    }
                )
            }
        }

        // Partition Selection
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Target Partition", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    partitions.forEach { partition ->
                        FilterChip(
                            selected = selectedPartition == partition,
                            onClick = { selectedPartition = partition },
                            label = { Text(partition, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningOrange.copy(alpha = 0.15f),
                                selectedLabelColor = WarningOrange
                            )
                        )
                    }
                }
            }
        }

        // Progress
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Flash Progress", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = WarningOrange,
                    trackColor = SurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Waiting to start...", color = OnSurfaceVariant, fontSize = 12.sp)
            }
        }

        // Action Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify")
            }
            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("FLASH")
            }
        }

        // Anti-brick safeguards
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A0A)),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "ANTI-BRICK SAFEGUARDS",
                    color = WarningOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                listOf("Checksum verification before flash", "Partition size validation", "Battery level check (>30%)", "Confirmation dialog for critical partitions").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(it, color = OnSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FastbootPartitionsTab() {
    val partitions = listOf(
        Triple("boot_a", "0x06000000", "Raw"),
        Triple("boot_b", "0x06000000", "Raw"),
        Triple("recovery", "0x06400000", "Raw"),
        Triple("system_a", "0x80000000", "Ext4"),
        Triple("system_b", "0x80000000", "Ext4"),
        Triple("vendor_a", "0x40000000", "Ext4"),
        Triple("vendor_b", "0x40000000", "Ext4"),
        Triple("userdata", "0x100000000", "F2FS"),
        Triple("metadata", "0x01000000", "Ext4"),
        Triple("misc", "0x00400000", "Raw"),
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("NAME", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
                Text("SIZE", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f))
                Text("TYPE", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("ACTIONS", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
            }
            Divider(color = Divider)
        }
        items(partitions) { (name, size, type) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = OnBackground, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
                Text(size, color = OnSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f))
                Text(type, color = PrimaryCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Row(modifier = Modifier.width(80.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.FlashOn, contentDescription = "Flash", tint = WarningOrange, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = "Erase", tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Divider(color = Divider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun FastbootRebootTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RebootButton("Reboot to System", "Normal boot into Android OS", Icons.Filled.RestartAlt, SuccessGreen) { }
        RebootButton("Reboot to Recovery", "Boot into recovery mode", Icons.Filled.Healing, PrimaryCyan) { }
        RebootButton("Reboot to Bootloader", "Stay in fastboot/bootloader", Icons.Filled.Warning, WarningOrange) { }
        RebootButton("Power Off", "Shutdown the device", Icons.Filled.PowerOff, ErrorRed) { }
    }
}

@Composable
private fun RebootButton(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(description, color = OnSurfaceVariant, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = color)
        }
    }
}
