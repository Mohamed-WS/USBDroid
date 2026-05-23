package com.usbdroid.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.ui.theme.*
import com.usbdroid.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    var autoConnect by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(true) }
    var notifyConnect by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }
    var bufferSize by remember { mutableFloatStateOf(64f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(color = Surface.copy(alpha = 0.5f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnBackground
                )
                Text(
                    text = "Configure USBDroid behavior",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        Divider(color = Divider, thickness = 1.dp)

        // Appearance Section
        SettingsSection(title = "Appearance") {
            SettingsToggleItem(
                icon = Icons.Filled.DarkMode,
                title = "Dark Theme",
                subtitle = "Use dark color scheme",
                checked = darkTheme,
                onCheckedChange = { viewModel.toggleTheme() }
            )
        }

        // Behavior Section
        SettingsSection(title = "Behavior") {
            SettingsToggleItem(
                icon = Icons.Filled.Usb,
                title = "Auto-connect to known devices",
                subtitle = "Automatically open modules for recognized devices",
                checked = autoConnect,
                onCheckedChange = { autoConnect = it }
            )
            SettingsToggleItem(
                icon = Icons.Filled.WbSunny,
                title = "Keep screen on during operations",
                subtitle = "Prevent screen timeout while USB operations are active",
                checked = keepScreenOn,
                onCheckedChange = { keepScreenOn = it }
            )
        }

        // Notifications Section
        SettingsSection(title = "Notifications") {
            SettingsToggleItem(
                icon = Icons.Filled.Notifications,
                title = "Notify on device connect",
                subtitle = "Show notification when USB device is connected",
                checked = notifyConnect,
                onCheckedChange = { notifyConnect = it }
            )
            SettingsToggleItem(
                icon = Icons.Filled.Vibration,
                title = "Haptic feedback",
                subtitle = "Vibrate on important actions",
                checked = vibration,
                onCheckedChange = { vibration = it }
            )
        }

        // Advanced Section
        SettingsSection(title = "Advanced") {
            // Buffer size slider
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Serial Buffer Size", color = OnBackground, fontSize = 14.sp)
                            Text("${bufferSize.toInt()} KB", color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = bufferSize,
                        onValueChange = { bufferSize = it },
                        valueRange = 16f..256f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryCyan,
                            activeTrackColor = PrimaryCyan,
                            inactiveTrackColor = SurfaceVariant
                        )
                    )
                }
            }

            SettingsActionItem(
                icon = Icons.Filled.Security,
                title = "Root-enhanced features",
                subtitle = "Enable features requiring root access",
                onClick = { }
            )
        }

        // About Section
        SettingsSection(title = "About") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimaryCyan.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Usb,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("USBDroid", color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Version 1.0.0", color = OnSurfaceVariant, fontSize = 13.sp)
                        Text("Universal USB Hardware Control Center", color = OnSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }

            SettingsActionItem(
                icon = Icons.Filled.Code,
                title = "Open Source Licenses",
                subtitle = "View third-party library licenses",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnBackground, fontSize = 14.sp)
                Text(subtitle, color = OnSurfaceVariant, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryCyan,
                    checkedTrackColor = PrimaryCyan.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnBackground, fontSize = 14.sp)
                Text(subtitle, color = OnSurfaceVariant, fontSize = 12.sp)
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
