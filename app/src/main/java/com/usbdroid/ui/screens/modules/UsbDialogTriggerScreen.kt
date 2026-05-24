package com.usbdroid.ui.screens.modules

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
import androidx.compose.ui.unit.dp
import com.usbdroid.core.modules.AdbModule
import kotlinx.coroutines.launch

/**
 * USB Dialog Trigger Screen
 * 
 * Provides UI for triggering the USB debugging authorization dialog
 * on target Android devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbDialogTriggerScreen(
    adbModule: AdbModule,
    onBack: () -> Unit
) {
    var deviceIp by remember { mutableStateOf("192.168.147.24:5555") }
    var triggerResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB Dialog Trigger") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInstructions = !showInstructions }) {
                        Icon(Icons.Default.Info, "Instructions")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "USB Debugging Authorization",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Trigger the 'Allow USB debugging?' dialog on target device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Instructions Card (collapsible)
            if (showInstructions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "How It Works",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        InstructionStep(
                            number = "1",
                            title = "Remove Authorization",
                            description = "Deletes /data/misc/adb/adb_keys on target device"
                        )
                        
                        InstructionStep(
                            number = "2",
                            title = "Restart ADB",
                            description = "Forces ADB daemon to restart and reconnect"
                        )
                        
                        InstructionStep(
                            number = "3",
                            title = "Dialog Appears",
                            description = "Target device shows 'Allow USB debugging?' dialog"
                        )
                        
                        InstructionStep(
                            number = "4",
                            title = "User Action Required",
                            description = "User must click 'Allow' on target device"
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "No root required - uses legitimate ADB mechanism",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Device IP Input
            OutlinedTextField(
                value = deviceIp,
                onValueChange = { deviceIp = it },
                label = { Text("Target Device IP:Port") },
                placeholder = { Text("192.168.1.100:5555") },
                leadingIcon = {
                    Icon(Icons.Default.Devices, "Device")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Trigger Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Method 1: Device-side trigger
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val success = adbModule.triggerUsbDebugDialog()
                            triggerResult = if (success) {
                                "✅ Trigger sent!\n\nCheck target device for dialog.\nClick 'Allow' to authorize."
                            } else {
                                "❌ Failed to send trigger.\n\nEnsure device is connected via ADB."
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, "Trigger", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Trigger Dialog")
                }

                // Method 2: PC-side commands
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            triggerResult = adbModule.triggerUsbDialogViaAdb(deviceIp)
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Code, "Commands", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Get Commands")
                }
            }

            // Quick Actions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    adbModule.executeShell("ls -la /data/misc/adb/adb_keys")
                                    triggerResult = "Checking authorization status..."
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Check Auth", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    adbModule.executeShell("rm /data/misc/adb/adb_keys")
                                    triggerResult = "Authorization keys removed"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove Keys", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Result Display
            if (triggerResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (triggerResult.startsWith("✅"))
                            MaterialTheme.colorScheme.tertiaryContainer
                        else if (triggerResult.startsWith("❌"))
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (triggerResult.startsWith("✅")) Icons.Default.CheckCircle
                                else if (triggerResult.startsWith("❌")) Icons.Default.Error
                                else Icons.Default.Info,
                                contentDescription = null
                            )
                            Text(
                                "Result",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            triggerResult,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Expected Dialog Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Expected Dialog on Target Device",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Mock dialog preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Allow USB debugging?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "The computer's RSA key fingerprint is:\nXX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = false, onCheckedChange = {})
                                Text(
                                    "Always allow from this computer",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                TextButton(onClick = {}) {
                                    Text("Cancel")
                                }
                                Button(onClick = {}) {
                                    Text("Allow")
                                }
                            }
                        }
                    }
                    
                    Text(
                        "👆 User must click 'Allow' to authorize this PC",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun InstructionStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
