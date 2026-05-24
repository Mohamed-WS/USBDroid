package com.usbdroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.ui.theme.*
import com.usbdroid.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ModulesScreen(viewModel: MainViewModel) {
    val modules = remember { viewModel.getAllModules() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Surface(color = Surface.copy(alpha = 0.5f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Control Modules",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnBackground
                )
                Text(
                    text = "${modules.size} modules available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        Divider(color = Divider, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(modules) { module ->
                ModuleListCard(module = module)
            }
        }
    }
}

@Composable
private fun ModuleListCard(module: com.usbdroid.viewmodel.ModuleDisplayInfo) {
    val accentColor = when (module.id) {
        "adb" -> Color(0xFF4CAF50)
        "fastboot" -> ErrorRed
        "serial" -> PrimaryCyan
        "flasher" -> Color(0xFFAB47BC)
        "hid" -> Color(0xFF9C27B0)
        "storage" -> Color(0xFF2196F3)
        "network" -> Color(0xFF00BCD4)
        "analyzer" -> OnSurfaceVariant
        else -> PrimaryCyan
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (module.id) {
                    "adb" -> Icons.Filled.Android
                    "fastboot" -> Icons.Filled.Warning
                    "serial" -> Icons.Filled.Usb
                    "flasher" -> Icons.Filled.Memory
                    "hid" -> Icons.Filled.Keyboard
                    "storage" -> Icons.Filled.Storage
                    "network" -> Icons.Filled.NetworkWifi
                    "analyzer" -> Icons.Filled.Search
                    else -> Icons.Filled.Settings
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
