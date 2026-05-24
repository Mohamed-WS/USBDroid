package com.usbdroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usbdroid.ui.theme.*
import com.usbdroid.ui.screens.modules.*
import com.usbdroid.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUsbDialog: () -> Unit = {}
) {
    val moduleInfo = remember(moduleId) { viewModel.getModuleInfo(moduleId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = moduleInfo?.name ?: "Module",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnBackground
                        )
                        Text(
                            text = moduleInfo?.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnBackground
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (moduleId) {
                "serial" -> SerialTerminalScreen()
                "adb" -> ADBControllerScreen(
                    onNavigateToUsbDialog = onNavigateToUsbDialog
                )
                "fastboot" -> FastbootControllerScreen()
                "flasher" -> FirmwareFlasherScreen()
                "hid" -> HIDAnalyzerScreen()
                "storage" -> StorageAnalyzerScreen()
                "network" -> NetworkControllerScreen()
                "analyzer" -> DeviceAnalyzerScreen(viewModel = viewModel)
                else -> UnsupportedModuleScreen(moduleId)
            }
        }
    }
}

@Composable
private fun UnsupportedModuleScreen(moduleId: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Construction,
            contentDescription = null,
            tint = WarningOrange,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Module: $moduleId",
            style = MaterialTheme.typography.titleLarge,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This module is under development and will be available in a future update.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
