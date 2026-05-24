package com.usbdroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usbdroid.data.model.ConnectionState
import com.usbdroid.data.model.DeviceCategory
import com.usbdroid.data.model.USBDeviceInfo
import com.usbdroid.ui.theme.*
import com.usbdroid.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onDeviceClick: (USBDeviceInfo) -> Unit,
    onOpenAnalyzer: () -> Unit
) {
    val detectedDevices by viewModel.detectedDevices.collectAsStateWithLifecycle()
    val deviceList = detectedDevices.values.toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        HomeHeader(deviceCount = deviceList.size)

        if (deviceList.isEmpty()) {
            EmptyDeviceState()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deviceList, key = { "${it.vendorId}:${it.productId}:${it.deviceId}" }) { device ->
                    DeviceCard(
                        device = device,
                        categoryName = viewModel.getCategoryDisplayName(device.deviceCategory),
                        onClick = { onDeviceClick(device) },
                        onRequestPermission = {
                            // Permission request logic
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(deviceCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Surface.copy(alpha = 0.5f),
                        Background
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "USB Devices",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnBackground
                )
                Text(
                    text = "$deviceCount connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }

            // Status indicator
            val pulseAnim by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "statusPulse"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (deviceCount > 0) SuccessGreen.copy(alpha = pulseAnim) else WarningOrange.copy(alpha = pulseAnim),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }

    Divider(color = Divider, thickness = 1.dp)
}

@Composable
private fun DeviceCard(
    device: USBDeviceInfo,
    categoryName: String,
    onClick: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val categoryColor = getCategoryColor(device.deviceCategory)
    val isConnected = device.connectionState == ConnectionState.CONNECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isConnected) categoryColor.copy(alpha = 0.4f) else Border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon with category color
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = categoryColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = categoryColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(device.deviceCategory),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = device.displayManufacturer,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category badge
                    CategoryBadge(categoryName, categoryColor)
                    // VID:PID
                    Text(
                        text = "VID:%04X PID:%04X".format(device.vendorId, device.productId),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Connection status
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = when (device.connectionState) {
                            ConnectionState.CONNECTED -> SuccessGreen
                            ConnectionState.CONNECTING -> WarningOrange
                            ConnectionState.PERMISSION_DENIED -> ErrorRed
                            else -> OnSurfaceVariant.copy(alpha = 0.3f)
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@Composable
private fun CategoryBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyDeviceState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val floatY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float"
        )

        Icon(
            imageVector = Icons.Filled.UsbOff,
            contentDescription = null,
            tint = OnSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .size(80.dp)
                .offset(y = floatY.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No USB Devices Connected",
            style = MaterialTheme.typography.titleLarge,
            color = OnSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect a USB device via OTG cable\nto get started",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant.copy(alpha = 0.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun getCategoryColor(category: DeviceCategory): Color = when (category) {
    DeviceCategory.ANDROID_ADB -> Color(0xFF4CAF50)
    DeviceCategory.ANDROID_FASTBOOT -> Color(0xFFF44336)
    DeviceCategory.SERIAL_UART -> PrimaryCyan
    DeviceCategory.HID_INPUT -> Color(0xFF9C27B0)
    DeviceCategory.MASS_STORAGE -> Color(0xFF2196F3)
    DeviceCategory.NETWORK -> Color(0xFF00BCD4)
    DeviceCategory.FIRMWARE_DFU -> WarningOrange
    else -> OnSurfaceVariant
}

@Composable
private fun getCategoryIcon(category: DeviceCategory) = when (category) {
    DeviceCategory.ANDROID_ADB -> Icons.Filled.Android
    DeviceCategory.ANDROID_FASTBOOT -> Icons.Filled.Warning
    DeviceCategory.SERIAL_UART -> Icons.Filled.Usb
    DeviceCategory.HID_INPUT -> Icons.Filled.Keyboard
    DeviceCategory.MASS_STORAGE -> Icons.Filled.Storage
    DeviceCategory.NETWORK -> Icons.Filled.NetworkWifi
    DeviceCategory.FIRMWARE_DFU -> Icons.Filled.Memory
    DeviceCategory.HUB -> Icons.Filled.Hub
    DeviceCategory.AUDIO -> Icons.Filled.Headphones
    DeviceCategory.VIDEO -> Icons.Filled.Videocam
    DeviceCategory.PRINTER -> Icons.Filled.Print
    else -> Icons.Filled.Usb
}
