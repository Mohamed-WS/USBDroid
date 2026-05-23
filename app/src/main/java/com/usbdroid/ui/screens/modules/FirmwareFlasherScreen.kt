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
import com.usbdroid.core.modules.FlasherModule
import com.usbdroid.ui.theme.*

@Composable
fun FirmwareFlasherScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Flash", "Chip Info", "Log")

    val chipInfo = remember {
        FlasherModule.ChipInfo(
            name = "ESP32-D0WD",
            type = FlasherModule.ChipType.ESP32,
            flashSize = 4194304,
            pageSize = 256,
            signature = emptyList(),
            description = "Espressif ESP32 (Xtensa dual-core @ 240MHz)"
        )
    }

    val flashLogs = remember {
        mutableStateListOf(
            "[INFO] Detected ESP32-D0WD (revision v1.1)",
            "[INFO] Crystal: 40MHz, Flash: 4MB (QIO)",
            "[INFO] MAC: 24:6F:28:A4:B2:C0",
            "[INFO] Erasing flash...",
            "[SUCCESS] Flash erased in 2.3s",
            "[INFO] Writing firmware (262144 bytes)...",
            "[INFO] Writing at 0x00010000... (12%)",
            "[INFO] Writing at 0x00020000... (25%)",
            "[INFO] Writing at 0x00040000... (50%)",
            "[INFO] Writing at 0x00060000... (75%)",
            "[INFO] Writing at 0x00080000... (100%)",
            "[SUCCESS] Flash complete!",
            "[INFO] Verifying...",
            "[SUCCESS] Verification passed!"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Bar
        Surface(color = Surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(SuccessGreen, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Target: ${chipInfo.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SuccessGreen,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${chipInfo.flashSize / 1024}KB Flash",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = PrimaryCyan,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
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
            0 -> FlashTab(chipInfo = chipInfo, logs = flashLogs)
            1 -> ChipInfoTab(chipInfo = chipInfo)
            2 -> FlashLogTab(logs = flashLogs)
        }
    }
}

@Composable
private fun FlashTab(chipInfo: FlasherModule.ChipInfo, logs: List<String>) {
    var selectedFile by remember { mutableStateOf("") }
    var flashAddress by remember { mutableStateOf("0x10000") }
    var verify by remember { mutableStateOf(true) }
    var eraseBeforeFlash by remember { mutableStateOf(true) }

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
                Text("Firmware File", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = selectedFile,
                    onValueChange = { selectedFile = it },
                    placeholder = { Text("Select .bin, .hex, .elf, or .dfu file...") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = Border
                    ),
                    trailingIcon = {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Browse", tint = OnSurfaceVariant)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text("Supported: ", color = OnSurfaceVariant, fontSize = 11.sp)
                    Text(".bin, .hex, .elf, .dfu, .ino", color = PrimaryCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Flash Address
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Flash Address", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = flashAddress,
                    onValueChange = { flashAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = Border,
                        focusedTextColor = OnBackground
                    ),
                    prefix = { Text("0x", color = OnSurfaceVariant) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AddressPresetButton("0x0000") { flashAddress = "0x0000" }
                    AddressPresetButton("0x1000") { flashAddress = "0x1000" }
                    AddressPresetButton("0x10000") { flashAddress = "0x10000" }
                    AddressPresetButton("0x8000") { flashAddress = "0x8000" }
                }
            }
        }

        // Options
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = verify,
                        onCheckedChange = { verify = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryCyan)
                    )
                    Text("Verify after flash", color = OnBackground, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = eraseBeforeFlash,
                        onCheckedChange = { eraseBeforeFlash = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryCyan)
                    )
                    Text("Erase before flash", color = OnBackground, fontSize = 13.sp)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Progress", style = MaterialTheme.typography.titleSmall, color = OnBackground)
                    Text("0 / 0 bytes (0%)", color = OnSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryCyan,
                    trackColor = SurfaceVariant
                )
            }
        }

        // Flash Button
        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("FLASH FIRMWARE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        // Secondary Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text("Erase Chip")
            }
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text("Read Flash")
            }
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.5f))
            ) {
                Text("Bootloader")
            }
        }
    }
}

@Composable
private fun AddressPresetButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryCyan),
        modifier = Modifier.height(32.dp)
    ) {
        Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ChipInfoTab(chipInfo: FlasherModule.ChipInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chip Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimaryCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, PrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Memory, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(chipInfo.name, style = MaterialTheme.typography.titleLarge, color = OnBackground, fontWeight = FontWeight.Bold)
                        Text(chipInfo.description, color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }

        // Specifications
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Specifications", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("Flash Size", "${chipInfo.flashSize / 1024} KB (${chipInfo.flashSize / 1024 / 1024} MB)")
                InfoRow("Page Size", "${chipInfo.pageSize} bytes")
                InfoRow("Type", chipInfo.type.name)
            }
        }

        // Supported Chips List
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Supported Microcontrollers", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "Arduino Uno/Nano (ATmega328P)" to "STK500",
                    "Arduino Mega (ATmega2560)" to "STK500v2",
                    "ESP32 / ESP32-S2 / ESP32-S3" to "ESP ROM Bootloader",
                    "ESP8266" to "ESP ROM Bootloader",
                    "STM32 (F1/F4/L4)" to "STM32 UART / DFU",
                    "Raspberry Pi Pico (RP2040)" to "UF2 / Serial"
                ).forEach { (chip, protocol) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(chip, color = OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(protocol, color = OnSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashLogTab(logs: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(TerminalBackground, RoundedCornerShape(8.dp))
            .border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            items(logs) { log ->
                val color = when {
                    log.contains("[SUCCESS]") -> SuccessGreen
                    log.contains("[ERROR]") -> ErrorRed
                    log.contains("[WARN]") -> WarningOrange
                    log.contains("[INFO]") -> TerminalGreen
                    else -> OnBackground
                }
                Text(log, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(140.dp))
        Text(value, color = OnBackground, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}
