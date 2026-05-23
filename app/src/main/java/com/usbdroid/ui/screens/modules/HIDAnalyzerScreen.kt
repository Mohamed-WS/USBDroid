package com.usbdroid.ui.screens.modules

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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

@Composable
fun HIDAnalyzerScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Monitor", "Descriptor", "Record")

    val reports = remember {
        mutableStateListOf(
            HidReportDisplay("10:23:45.123", "00 00 04 00 00 00 00 00", "Key: 'a' pressed", Color(0xFF81C784)),
            HidReportDisplay("10:23:45.189", "00 00 00 00 00 00 00 00", "All keys released", OnSurfaceVariant),
            HidReportDisplay("10:23:45.456", "00 00 05 00 00 00 00 00", "Key: 'b' pressed", Color(0xFF81C784)),
            HidReportDisplay("10:23:45.678", "00 00 00 00 00 00 00 00", "All keys released", OnSurfaceVariant),
            HidReportDisplay("10:23:46.123", "00 02 04 00 00 00 00 00", "Key: 'A' (shift+a)", Color(0xFF64B5F6)),
            HidReportDisplay("10:23:46.234", "02 00 00 00 00 00 00 00", "Mouse: L-click", WarningOrange),
            HidReportDisplay("10:23:46.345", "02 00 05 FA 00 00 00 00", "Mouse: move (-6, +5)", PrimaryCyan),
            HidReportDisplay("10:23:46.456", "00 00 2C 00 00 00 00 00", "Key: SPACE pressed", Color(0xFF81C784)),
            HidReportDisplay("10:23:46.567", "00 00 00 00 00 00 00 00", "All keys released", OnSurfaceVariant),
            HidReportDisplay("10:23:46.789", "00 00 28 00 00 00 00 00", "Key: ENTER pressed", Color(0xFFCE93D8)),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Device Info
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
                        .background(Color(0xFF9C27B0).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF9C27B0).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("USB Keyboard", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                    Text("HID Boot Protocol | 8-byte reports", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = Color(0xFF9C27B0),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color(0xFF9C27B0)
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
                    selectedContentColor = Color(0xFF9C27B0),
                    unselectedContentColor = OnSurfaceVariant
                )
            }
        }

        when (selectedTab) {
            0 -> HidMonitorTab(reports = reports)
            1 -> HidDescriptorTab()
            2 -> HidRecordTab()
        }
    }
}

private data class HidReportDisplay(val time: String, val hex: String, val decoded: String, val color: Color)

@Composable
private fun HidMonitorTab(reports: List<HidReportDisplay>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(reports) { report ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        report.time,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(90.dp)
                    )
                    Text(
                        report.hex,
                        color = report.color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(220.dp)
                    )
                    Text(
                        report.decoded,
                        color = report.color.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HidDescriptorTab() {
    val descriptorItems = listOf(
        Triple("Global", "Usage Page", "0x01 (Generic Desktop)"),
        Triple("Local", "Usage", "0x06 (Keyboard)"),
        Triple("Main", "Collection", "0x01 (Application)"),
        Triple("Global", "Usage Page", "0x07 (Keyboard/Keypad)"),
        Triple("Local", "Usage Minimum", "0xE0 (Left Control)"),
        Triple("Local", "Usage Maximum", "0xE7 (Right GUI)"),
        Triple("Global", "Logical Minimum", "0x00"),
        Triple("Global", "Logical Maximum", "0x01"),
        Triple("Global", "Report Size", "0x01 (1 bit)"),
        Triple("Global", "Report Count", "0x08 (8 fields)"),
        Triple("Main", "Input", "0x02 (Data,Var,Abs)"),
        Triple("Global", "Report Count", "0x01"),
        Triple("Global", "Report Size", "0x08"),
        Triple("Main", "Input", "0x01 (Const,Arr,Abs)"),
        Triple("Global", "Report Count", "0x05"),
        Triple("Global", "Report Size", "0x01"),
        Triple("Main", "Output", "0x02 (Data,Var,Abs)"),
        Triple("Main", "End Collection", "")
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(descriptorItems) { (type, tag, value) ->
            val typeColor = when (type) {
                "Global" -> PrimaryCyan
                "Local" -> SuccessGreen
                "Main" -> WarningOrange
                else -> OnBackground
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    type.uppercase(),
                    color = typeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(60.dp)
                )
                Text(
                    tag,
                    color = OnBackground,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(160.dp)
                )
                Text(
                    value,
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HidRecordTab() {
    var isRecording by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val pulseAnim by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        ErrorRed.copy(alpha = 0.15f),
                        androidx.compose.foundation.shape.CircleShape
                    )
                    .border(2.dp, ErrorRed.copy(alpha = pulseAnim), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recording...", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Input events are being captured", color = OnSurfaceVariant, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { isRecording = false },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("STOP RECORDING")
            }
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        PrimaryCyan.copy(alpha = 0.1f),
                        androidx.compose.foundation.shape.CircleShape
                    )
                    .border(2.dp, PrimaryCyan.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ready to Record", color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Record and replay HID input sequences", color = OnSurfaceVariant, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { isRecording = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
            ) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("START RECORDING")
            }
        }
    }
}
