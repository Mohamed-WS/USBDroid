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
import com.usbdroid.core.modules.SerialModule
import com.usbdroid.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialTerminalScreen() {
    var isConnected by remember { mutableStateOf(false) }
    var baudRate by remember { mutableIntStateOf(115200) }
    var dataBits by remember { mutableIntStateOf(8) }
    var stopBits by remember { mutableIntStateOf(1) }
    var parity by remember { mutableStateOf("None") }
    var lineEnding by remember { mutableStateOf("CRLF") }
    var hexMode by remember { mutableStateOf(false) }
    var timestamps by remember { mutableStateOf(false) }
    var autoscroll by remember { mutableStateOf(true) }
    var dtr by remember { mutableStateOf(false) }
    var rts by remember { mutableStateOf(false) }
    var sendText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(true) }
    var showPresets by remember { mutableStateOf(false) }

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Simulated received data
    val receivedLines = remember {
        mutableStateListOf(
            "[10:23:45.123] ESP32 Boot: SPI Speed: 40MHz",
            "[10:23:45.145] ESP32 Boot: SPI Mode: DIO",
            "[10:23:45.167] ESP32 Boot: SPI Flash Size: 4MB",
            "[10:23:45.189] Loading partition table...",
            "[10:23:45.201] Partition table loaded successfully",
            "[10:23:45.234] Loading app...",
            "[10:23:45.456] App loaded: firmware_v2.1.0",
            "[10:23:45.678] WiFi: Connecting to network...",
            "[10:23:46.123] WiFi: Connected to HomeNetwork (192.168.1.105)",
            "[10:23:46.234] MQTT: Connecting to broker...",
            "[10:23:46.456] MQTT: Connected to 192.168.1.50:1883",
            "[10:23:46.789] System ready. Type 'help' for commands.",
            "> ",
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Connection Bar
        ConnectionBar(
            isConnected = isConnected,
            onConnectToggle = { isConnected = !isConnected },
            baudRate = baudRate,
            onBaudRateChange = { baudRate = it },
            dtr = dtr,
            rts = rts,
            onDtrToggle = { dtr = !dtr },
            onRtsToggle = { rts = !rts },
            onShowSettings = { showSettings = !showSettings },
            onShowPresets = { showPresets = !showPresets }
        )

        // Settings Panel
        AnimatedVisibility(
            visible = showSettings,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SerialSettingsPanel(
                dataBits = dataBits,
                onDataBitsChange = { dataBits = it },
                stopBits = stopBits,
                onStopBitsChange = { stopBits = it },
                parity = parity,
                onParityChange = { parity = it },
                lineEnding = lineEnding,
                onLineEndingChange = { lineEnding = it },
                hexMode = hexMode,
                onHexModeToggle = { hexMode = !hexMode },
                timestamps = timestamps,
                onTimestampsToggle = { timestamps = !timestamps },
                autoscroll = autoscroll,
                onAutoscrollToggle = { autoscroll = !autoscroll }
            )
        }

        // Terminal Output
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .background(
                    TerminalBackground,
                    RoundedCornerShape(8.dp)
                )
                .border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(receivedLines) { line ->
                    val color = when {
                        line.contains("Error") -> ErrorRed
                        line.contains("success", ignoreCase = true) || line.contains("ready", ignoreCase = true) -> SuccessGreen
                        line.contains("WiFi") -> Color(0xFF64B5F6)
                        line.contains("MQTT") -> Color(0xFF81C784)
                        line.startsWith(">") -> PrimaryCyan
                        else -> TerminalGreen
                    }
                    Text(
                        text = line,
                        style = TerminalTextStyle.copy(fontSize = 11.sp),
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Send Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = sendText,
                onValueChange = { sendText = it },
                placeholder = {
                    Text(
                        "Enter command...",
                        color = OnSurfaceVariant.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = Border,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = {
                    if (sendText.isNotBlank()) {
                        receivedLines.add("> $sendText")
                        receivedLines.add("Command executed: $sendText")
                        sendText = ""
                        if (autoscroll) {
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(receivedLines.size - 1)
                            }
                        }
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = PrimaryCyan,
                    contentColor = Background
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }

        // Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = { receivedLines.add("\n--- Log Cleared ---\n") },
                colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
            ) {
                Text("Clear", fontSize = 11.sp)
            }
            TextButton(
                onClick = { },
                colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
            ) {
                Text("Macros", fontSize = 11.sp)
            }
            TextButton(
                onClick = { },
                colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
            ) {
                Text("Plotter", fontSize = 11.sp)
            }
            TextButton(
                onClick = { },
                colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
            ) {
                Text("Save Log", fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionBar(
    isConnected: Boolean,
    onConnectToggle: () -> Unit,
    baudRate: Int,
    onBaudRateChange: (Int) -> Unit,
    dtr: Boolean,
    rts: Boolean,
    onDtrToggle: () -> Unit,
    onRtsToggle: () -> Unit,
    onShowSettings: () -> Unit,
    onShowPresets: () -> Unit
) {
    Surface(
        color = Surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connect Button
                Button(
                    onClick = onConnectToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) ErrorRed else SuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        if (isConnected) "DISCONNECT" else "CONNECT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Baud Rate Dropdown
                var baudExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = baudExpanded,
                    onExpandedChange = { baudExpanded = it },
                    modifier = Modifier.width(110.dp)
                ) {
                    OutlinedTextField(
                        value = "$baudRate",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = Border,
                            focusedTextColor = OnBackground,
                            unfocusedTextColor = OnBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baudExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = baudExpanded,
                        onDismissRequest = { baudExpanded = false },
                        modifier = Modifier.background(SurfaceElevated)
                    ) {
                        SerialModule.COMMON_BAUD_RATES.forEach { rate ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "$rate",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (rate == baudRate) PrimaryCyan else OnBackground
                                    )
                                },
                                onClick = {
                                    onBaudRateChange(rate)
                                    baudExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Control line indicators
                ControlLineIndicator("DTR", dtr, onDtrToggle)
                Spacer(modifier = Modifier.width(4.dp))
                ControlLineIndicator("RTS", rts, onRtsToggle)

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onShowSettings, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = "Settings", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }

            // Status line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = if (isConnected) SuccessGreen else OnSurfaceVariant.copy(alpha = 0.4f)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "Connected at $baudRate baud" else "Disconnected",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ControlLineIndicator(label: String, active: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) PrimaryCyan.copy(alpha = 0.2f) else SurfaceVariant)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) PrimaryCyan else OnSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SerialSettingsPanel(
    dataBits: Int,
    onDataBitsChange: (Int) -> Unit,
    stopBits: Int,
    onStopBitsChange: (Int) -> Unit,
    parity: String,
    onParityChange: (String) -> Unit,
    lineEnding: String,
    onLineEndingChange: (String) -> Unit,
    hexMode: Boolean,
    onHexModeToggle: () -> Unit,
    timestamps: Boolean,
    onTimestampsToggle: () -> Unit,
    autoscroll: Boolean,
    onAutoscrollToggle: () -> Unit
) {
    Surface(
        color = SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Data Bits
                SettingDropdown("Data", listOf("5", "6", "7", "8"), dataBits.toString()) {
                    onDataBitsChange(it.toInt())
                }
                // Stop Bits
                SettingDropdown("Stop", listOf("1", "1.5", "2"), "$stopBits") {
                    onStopBitsChange(it.toFloat().toInt())
                }
                // Parity
                SettingDropdown("Parity", listOf("None", "Even", "Odd", "Mark", "Space"), parity, onParityChange)
                // Line Ending
                SettingDropdown("End", listOf("CR", "LF", "CRLF", "None"), lineEnding, onLineEndingChange)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ToggleChip("HEX", hexMode, onHexModeToggle)
                ToggleChip("Time", timestamps, onTimestampsToggle)
                ToggleChip("Auto", autoscroll, onAutoscrollToggle)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingDropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(70.dp)
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = OnBackground
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = Border,
                    focusedTextColor = OnBackground
                ),
                shape = RoundedCornerShape(6.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SurfaceElevated)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                opt,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (opt == selected) PrimaryCyan else OnBackground
                            )
                        },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) PrimaryCyan.copy(alpha = 0.15f) else Surface)
            .border(1.dp, if (active) PrimaryCyan.copy(alpha = 0.4f) else Border, RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (active) "[$label]" else label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) PrimaryCyan else OnSurfaceVariant,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
