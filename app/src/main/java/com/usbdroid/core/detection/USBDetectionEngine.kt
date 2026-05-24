package com.usbdroid.core.detection

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.usbdroid.data.model.ConnectionState
import com.usbdroid.data.model.DeviceCategory
import com.usbdroid.data.model.USBDeviceInfo
import com.usbdroid.data.model.VidPidDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class USBDetectionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager
) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.usbdroid.USB_PERMISSION"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val vidPidDatabase = VidPidDatabase.getInstance()

    private val _detectedDevices = MutableStateFlow<Map<String, USBDeviceInfo>>(emptyMap())
    val detectedDevices: StateFlow<Map<String, USBDeviceInfo>> = _detectedDevices.asStateFlow()

    private val _connectionEvents = MutableStateFlow<USBConnectionEvent?>(null)
    val connectionEvents: StateFlow<USBConnectionEvent?> = _connectionEvents.asStateFlow()

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    device?.let { handlePermissionResult(it, granted) }
                }
            }
        }
    }

    private val usbStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let { onDeviceAttached(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let { onDeviceDetached(it) }
                }
            }
        }
    }

    init {
        scope.launch {
            vidPidDatabase.load(context)
            initializeConnectedDevices()
        }
        registerReceivers()
    }

    private fun registerReceivers() {
        val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(permissionReceiver, permissionFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(permissionReceiver, permissionFilter)
        }

        val stateFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbStateReceiver, stateFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbStateReceiver, stateFilter)
        }
    }

    private fun initializeConnectedDevices() {
        val deviceList = usbManager.deviceList
        Timber.i("Initial device scan found ${deviceList.size} devices")
        deviceList.values.forEach { device ->
            processDevice(device)
        }
    }

    private fun onDeviceAttached(device: UsbDevice) {
        Timber.i("USB device attached: ${device.deviceName} VID=${device.vendorId} PID=${device.productId}")
        _connectionEvents.value = USBConnectionEvent.DeviceAttached(device)
        processDevice(device)
    }

    private fun onDeviceDetached(device: UsbDevice) {
        Timber.i("USB device detached: ${device.deviceName}")
        _connectionEvents.value = USBConnectionEvent.DeviceDetached(device)
        val key = "${device.vendorId}:${device.productId}:${device.deviceId}"
        _detectedDevices.update { it - key }
    }

    private fun processDevice(device: UsbDevice) {
        val info = USBDeviceInfo.fromUsbDevice(device).let { raw ->
            resolveDeviceInfo(raw)
        }
        val key = "${device.vendorId}:${device.productId}:${device.deviceId}"
        _detectedDevices.update { it + (key to info) }

        // Auto-request permission
        if (!usbManager.hasPermission(device)) {
            requestPermission(device)
        }
    }

    fun requestPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            device.deviceId,
            Intent(ACTION_USB_PERMISSION).apply {
                `package` = context.packageName
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun handlePermissionResult(device: UsbDevice, granted: Boolean) {
        val key = "${device.vendorId}:${device.productId}:${device.deviceId}"
        val state = if (granted) ConnectionState.CONNECTED else ConnectionState.PERMISSION_DENIED
        _detectedDevices.update { devices ->
            devices.mapValues { (k, info) ->
                if (k == key) info.copy(connectionState = state) else info
            }.toMap()
        }
        _connectionEvents.value = USBConnectionEvent.PermissionResult(device, granted)
        Timber.i("USB permission ${if (granted) "granted" else "denied"} for ${device.deviceName}")
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun openDevice(device: UsbDevice) = usbManager.openDevice(device)

    private fun resolveDeviceInfo(info: USBDeviceInfo): USBDeviceInfo {
        val resolvedManufacturer = vidPidDatabase.lookupVendor(info.vendorId) ?: info.manufacturerName ?: "Unknown"
        val resolvedProduct = vidPidDatabase.lookupProduct(info.vendorId, info.productId)
            ?: info.productName ?: "Unknown Device"
        val category = classifyDevice(info)
        val confidence = calculateConfidence(info, category)

        return info.copy(
            resolvedManufacturer = resolvedManufacturer,
            resolvedProduct = resolvedProduct,
            deviceCategory = category,
            confidence = confidence
        )
    }

    private fun classifyDevice(info: USBDeviceInfo): DeviceCategory {
        // Check VID/PID first for known devices
        return when {
            // Android ADB
            info.vendorId == 0x18D1 && info.productId in listOf(0x4EE7, 0xD001, 0x4EE6) -> DeviceCategory.ANDROID_ADB
            // Android Fastboot
            info.vendorId == 0x18D1 && info.productId == 0x4EE0 -> DeviceCategory.ANDROID_FASTBOOT
            // Samsung Download/Odin mode
            info.vendorId == 0x04E8 && info.productId == 0x6866 -> DeviceCategory.ANDROID_FASTBOOT
            // STM32 DFU
            info.vendorId == 0x0483 && info.productId == 0xDF11 -> DeviceCategory.FIRMWARE_DFU
            // HID devices
            info.deviceClass == 0x03 || info.interfaces.any { it.interfaceClass == 0x03 } -> DeviceCategory.HID_INPUT
            // Mass Storage
            info.deviceClass == 0x08 || info.interfaces.any { it.interfaceClass == 0x08 } -> DeviceCategory.MASS_STORAGE
            // CDC-ACM Serial
            info.deviceClass == 0x02 || info.interfaces.any {
                it.interfaceClass == 0x02 && it.interfaceSubclass == 0x02
            } -> DeviceCategory.SERIAL_UART
            // CDC Data (also serial)
            info.deviceClass == 0x0A || info.interfaces.any { it.interfaceClass == 0x0A } -> DeviceCategory.SERIAL_UART
            // Known serial chip vendors
            info.vendorId in listOf(0x0403, 0x1A86, 0x10C4, 0x067B, 0x2341, 0x239A, 0x303A, 0x2E8A) -> DeviceCategory.SERIAL_UART
            // Hub
            info.deviceClass == 0x09 -> DeviceCategory.HUB
            // Network (CDC-ECM, RNDIS)
            info.interfaces.any { it.interfaceClass == 0x02 && it.interfaceSubclass in listOf(0x06, 0x0D) } -> DeviceCategory.NETWORK
            // Smart Card
            info.deviceClass == 0x0B -> DeviceCategory.SMART_CARD
            // Audio
            info.deviceClass == 0x01 -> DeviceCategory.AUDIO
            // Video
            info.deviceClass == 0x0E -> DeviceCategory.VIDEO
            // Printer
            info.deviceClass == 0x07 -> DeviceCategory.PRINTER
            // Vendor-specific - could be anything
            info.deviceClass == 0xFF -> DeviceCategory.VENDOR_SPECIFIC
            else -> DeviceCategory.UNKNOWN
        }
    }

    private fun calculateConfidence(info: USBDeviceInfo, category: DeviceCategory): Float {
        var score = 0.0f
        // Known VID gives higher confidence
        if (vidPidDatabase.lookupVendor(info.vendorId) != null) score += 0.3f
        // Known PID gives even higher confidence
        if (vidPidDatabase.lookupProduct(info.vendorId, info.productId) != null) score += 0.4f
        // Specific class detection
        if (category != DeviceCategory.UNKNOWN && category != DeviceCategory.VENDOR_SPECIFIC) score += 0.3f
        // Manufacturer string present
        if (!info.manufacturerName.isNullOrBlank()) score += 0.1f
        return score.coerceIn(0.0f, 1.0f)
    }

    fun getDeviceCategoryName(category: DeviceCategory): String = when (category) {
        DeviceCategory.ANDROID_ADB -> "Android ADB"
        DeviceCategory.ANDROID_FASTBOOT -> "Android Fastboot"
        DeviceCategory.SERIAL_UART -> "Serial UART"
        DeviceCategory.HID_INPUT -> "HID Input"
        DeviceCategory.MASS_STORAGE -> "Mass Storage"
        DeviceCategory.NETWORK -> "Network Device"
        DeviceCategory.FIRMWARE_DFU -> "DFU Bootloader"
        DeviceCategory.SMART_CARD -> "Smart Card"
        DeviceCategory.AUDIO -> "Audio Device"
        DeviceCategory.VIDEO -> "Video Device"
        DeviceCategory.PRINTER -> "Printer"
        DeviceCategory.HUB -> "USB Hub"
        DeviceCategory.VENDOR_SPECIFIC -> "Vendor Specific"
        DeviceCategory.UNKNOWN -> "Unknown"
    }

    fun getModuleForCategory(category: DeviceCategory): String = when (category) {
        DeviceCategory.ANDROID_ADB -> "adb"
        DeviceCategory.ANDROID_FASTBOOT -> "fastboot"
        DeviceCategory.SERIAL_UART -> "serial"
        DeviceCategory.HID_INPUT -> "hid"
        DeviceCategory.MASS_STORAGE -> "storage"
        DeviceCategory.NETWORK -> "network"
        DeviceCategory.FIRMWARE_DFU -> "flasher"
        else -> "analyzer"
    }

    fun getConnectedDevices(): List<UsbDevice> = usbManager.deviceList.values.toList()

    fun cleanup() {
        try {
            context.unregisterReceiver(permissionReceiver)
            context.unregisterReceiver(usbStateReceiver)
        } catch (_: Exception) {
            // Receiver may not be registered
        }
    }
}

sealed class USBConnectionEvent {
    data class DeviceAttached(val device: UsbDevice) : USBConnectionEvent()
    data class DeviceDetached(val device: UsbDevice) : USBConnectionEvent()
    data class PermissionResult(val device: UsbDevice, val granted: Boolean) : USBConnectionEvent()
}
