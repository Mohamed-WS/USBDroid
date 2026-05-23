package com.usbdroid.viewmodel

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usbdroid.core.detection.USBConnectionEvent
import com.usbdroid.core.detection.USBDetectionEngine
import com.usbdroid.core.modules.*
import com.usbdroid.data.model.*
import com.usbdroid.data.repository.DeviceProfileRepository
import com.usbdroid.data.repository.SessionLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val detectionEngine: USBDetectionEngine,
    private val usbManager: UsbManager,
    private val logRepository: SessionLogRepository,
    private val profileRepository: DeviceProfileRepository
) : ViewModel() {

    val detectedDevices: StateFlow<Map<String, USBDeviceInfo>> = detectionEngine.detectedDevices
    val connectionEvents: StateFlow<USBConnectionEvent?> = detectionEngine.connectionEvents

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeModule = MutableStateFlow<UsbModule?>(null)
    val activeModule: StateFlow<UsbModule?> = _activeModule.asStateFlow()

    private val _selectedDevice = MutableStateFlow<USBDeviceInfo?>(null)
    val selectedDevice: StateFlow<USBDeviceInfo?> = _selectedDevice.asStateFlow()

    private val _logs = MutableStateFlow<List<SessionLog>>(emptyList())
    val logs: StateFlow<List<SessionLog>> = _logs.asStateFlow()

    private val _profiles = MutableStateFlow<List<DeviceProfile>>(emptyList())
    val profiles: StateFlow<List<DeviceProfile>> = _profiles.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AIMessage>>(emptyList())
    val aiMessages: StateFlow<List<AIMessage>> = _aiMessages.asStateFlow()

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _isLoading.value = false
        }
        loadLogs()
        loadProfiles()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            logRepository.getRecentLogs(500).collect { _logs.value = it }
        }
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepository.getAllProfiles().collect { _profiles.value = it }
        }
    }

    fun onDeviceAttached(device: UsbDevice) {
        viewModelScope.launch {
            logRepository.logInfo("Detection", "Device attached: ${device.deviceName}", device.deviceId)
        }
    }

    fun requestPermission(device: UsbDevice) {
        detectionEngine.requestPermission(device)
    }

    fun getSuggestedModule(deviceInfo: USBDeviceInfo): String {
        return detectionEngine.getModuleForCategory(deviceInfo.deviceCategory)
    }

    fun getCategoryDisplayName(category: DeviceCategory): String {
        return detectionEngine.getDeviceCategoryName(category)
    }

    fun activateModule(moduleId: String, deviceInfo: USBDeviceInfo? = null) {
        _activeModule.value?.detach()

        val module = ModuleRegistry.createModule(moduleId)
        _activeModule.value = module
        _selectedDevice.value = deviceInfo

        if (module != null && deviceInfo != null) {
            viewModelScope.launch {
                val usbDevice = usbManager.deviceList.values.find {
                    it.vendorId == deviceInfo.vendorId && it.productId == deviceInfo.productId
                }
                usbDevice?.let { device ->
                    if (usbManager.hasPermission(device)) {
                        val connection = detectionEngine.openDevice(device)
                        module.attach(device, connection)
                        logRepository.logInfo(moduleId, "Module activated for ${deviceInfo.displayName}")
                    }
                }
            }
        }
    }

    fun deactivateModule() {
        _activeModule.value?.detach()
        _activeModule.value = null
        _selectedDevice.value = null
    }

    fun getModuleInfo(moduleId: String): ModuleDisplayInfo? {
        val module = ModuleRegistry.createModule(moduleId) ?: return null
        return ModuleDisplayInfo(
            id = module.moduleId,
            name = module.moduleName,
            description = module.moduleDescription,
            iconName = when (moduleId) {
                "adb" -> "Terminal"
                "fastboot" -> "Warning"
                "serial" -> "Usb"
                "flasher" -> "Memory"
                "hid" -> "Keyboard"
                "storage" -> "Storage"
                "network" -> "NetworkWifi"
                "analyzer" -> "Search"
                else -> "Settings"
            }
        )
    }

    fun getAllModules(): List<ModuleDisplayInfo> {
        return ModuleRegistry.getAllModuleIds().mapNotNull { getModuleInfo(it) }
    }

    fun sendAIMessage(message: String, context: String = "") {
        viewModelScope.launch {
            _aiMessages.value = _aiMessages.value + AIMessage.User(message)
            _aiMessages.value = _aiMessages.value + AIMessage.Thinking

            try {
                // Simulated AI response - in production, call Claude API
                kotlinx.coroutines.delay(1500)
                val response = generateAIResponse(message, context)
                _aiMessages.value = _aiMessages.value.filterNot { it is AIMessage.Thinking }
                _aiMessages.value = _aiMessages.value + AIMessage.Assistant(response)
            } catch (e: Exception) {
                _aiMessages.value = _aiMessages.value.filterNot { it is AIMessage.Thinking }
                _aiMessages.value = _aiMessages.value + AIMessage.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun generateAIResponse(query: String, context: String): String {
        return when {
            query.contains("(?i)what.*(device|this)".toRegex()) -> {
                "Based on the USB descriptors, this appears to be ${context.ifEmpty { "a USB device" }}. " +
                "You can use the Device Analyzer module to get a complete breakdown of all descriptors and interfaces."
            }
            query.contains("(?i)(flash|program|upload)".toRegex()) -> {
                "To flash firmware to this device, use the Firmware Flasher module. " +
                "Make sure the device is in bootloader mode (often requires holding a BOOT button while resetting). " +
                "Select the appropriate .hex or .bin file and verify the flash address before proceeding."
            }
            query.contains("(?i)(serial|uart|baud)".toRegex()) -> {
                "For serial communication, open the Serial Terminal module. Common settings: " +
                "Arduino: 9600 baud, ESP32: 115200 baud, GPS: 9600 baud. " +
                "Make sure to match the data bits (usually 8), stop bits (1), and parity (None) with your device configuration."
            }
            query.contains("(?i)(pinout|gpio|pins)".toRegex()) -> {
                "Pinout information depends on the specific board. Common configurations:\n\n" +
                "Arduino Uno:\n" +
                "  D0(RX), D1(TX), D2-D13 (GPIO), A0-A5 (Analog), 5V, 3.3V, GND\n\n" +
                "ESP32:\n" +
                "  TX0(GPIO1), RX0(GPIO3), GPIO0(BOOT), GPIO2, GPIO4-5, GPIO12-15, GPIO18-23\n\n" +
                "STM32:\n" +
                "  PA9(USART1_TX), PA10(USART1_RX), PA13(SWDIO), PA14(SWCLK)"
            }
            else -> {
                "I'm your USB hardware assistant. I can help you:\n\n" +
                "• Identify unknown USB devices from their descriptors\n" +
                "• Guide you through firmware flashing procedures\n" +
                "• Explain serial communication parameters\n" +
                "• Provide pinout references for common boards\n" +
                "• Troubleshoot connection issues\n\n" +
                "What would you like to know about your connected device?"
            }
        }
    }

    fun clearAIChat() {
        _aiMessages.value = emptyList()
    }

    fun toggleTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearAll()
        }
    }

    fun exportLogs() {
        // Implementation for log export
    }

    fun cleanup() {
        detectionEngine.cleanup()
        _activeModule.value?.detach()
    }
}

data class ModuleDisplayInfo(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String
)

sealed class AIMessage {
    data class User(val content: String) : AIMessage()
    data class Assistant(val content: String) : AIMessage()
    data class Error(val message: String) : AIMessage()
    object Thinking : AIMessage()
}
