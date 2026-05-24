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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.data.model.LogLevel
import com.usbdroid.data.model.SessionLog
import com.usbdroid.ui.theme.*
import com.usbdroid.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    var searchText by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, selectedFilter, searchText) {
        logs.filter { log ->
            val levelMatch = selectedFilter == null || log.level == selectedFilter
            val searchMatch = searchText.isEmpty() || log.message.contains(searchText, ignoreCase = true) || log.moduleName.contains(searchText, ignoreCase = true)
            levelMatch && searchMatch
        }
    }

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
                    text = "Session Logs",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnBackground
                )
                Text(
                    text = "${logs.size} entries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        Divider(color = Divider, thickness = 1.dp)

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryCyan.copy(alpha = 0.15f),
                    selectedLabelColor = PrimaryCyan
                )
            )
            FilterChip(
                selected = selectedFilter == LogLevel.ERROR,
                onClick = { selectedFilter = if (selectedFilter == LogLevel.ERROR) null else LogLevel.ERROR },
                label = { Text("Errors") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ErrorRed.copy(alpha = 0.15f),
                    selectedLabelColor = ErrorRed
                )
            )
            FilterChip(
                selected = selectedFilter == LogLevel.WARNING,
                onClick = { selectedFilter = if (selectedFilter == LogLevel.WARNING) null else LogLevel.WARNING },
                label = { Text("Warnings") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WarningOrange.copy(alpha = 0.15f),
                    selectedLabelColor = WarningOrange
                )
            )
            FilterChip(
                selected = selectedFilter == LogLevel.INFO,
                onClick = { selectedFilter = if (selectedFilter == LogLevel.INFO) null else LogLevel.INFO },
                label = { Text("Info") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SuccessGreen.copy(alpha = 0.15f),
                    selectedLabelColor = SuccessGreen
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { viewModel.clearLogs() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear logs", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Share, contentDescription = "Export", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No logs match the filter",
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs) { log ->
                    LogEntryCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(log: SessionLog) {
    val (levelColor, levelBg) = when (log.level) {
        LogLevel.ERROR -> ErrorRed to ErrorRed.copy(alpha = 0.08f)
        LogLevel.WARNING -> WarningOrange to WarningOrange.copy(alpha = 0.08f)
        LogLevel.INFO -> SuccessGreen to SuccessGreen.copy(alpha = 0.08f)
        LogLevel.DEBUG -> PrimaryCyan to PrimaryCyan.copy(alpha = 0.08f)
        LogLevel.VERBOSE -> OnSurfaceVariant to OnSurfaceVariant.copy(alpha = 0.05f)
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, levelColor.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level badge
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .background(levelBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    log.level.name.take(1),
                    color = levelColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Timestamp
            Text(
                dateFormat.format(Date(log.timestamp)),
                color = OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(80.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Module name
            Text(
                log.moduleName.uppercase(),
                color = PrimaryCyan.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(60.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Message
            Text(
                log.message,
                color = OnBackground.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                lineHeight = 16.sp
            )
        }
    }
}
