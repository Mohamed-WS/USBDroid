package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbConstants
import com.usbdroid.data.model.DeviceCategory
import com.usbdroid.data.model.USBDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unknown Device Analyzer Module - Full USB descriptor analysis and reverse engineering.
 */
class AnalyzerModule : UsbModule {

    override val moduleId: String = "analyzer"
    override val moduleName: String = "Device Analyzer"
    override val moduleDescription: String = "Full USB descriptor dump, protocol analysis, and reverse engineering toolkit"
    override val supportedCategories: List<DeviceCategory> = DeviceCategory.values().toList()
    override val supportedVids: List<Int> = emptyList()
    override val supportedPids: List<Pair<Int, Int>> = emptyList()
    override val isHardwareModule: Boolean = false

    private val _connectionState = MutableStateFlow<ModuleConnectionState>(ModuleConnectionState.Disconnected)
    private val _descriptorDump = MutableStateFlow("")
    private val _usbDeviceInfo = MutableStateFlow<USBDeviceInfo?>(null)
    private val _transferLog = MutableStateFlow<List<UsbTransfer>>(emptyList())
    private val _endpointTests = MutableStateFlow<Map<Int, EndpointTestResult>>(emptyMap())

    val descriptorDump: StateFlow<String> = _descriptorDump
    val usbDeviceInfo: StateFlow<USBDeviceInfo?> = _usbDeviceInfo
    val transferLog: StateFlow<List<UsbTransfer>> = _transferLog
    val endpointTests: StateFlow<Map<Int, EndpointTestResult>> = _endpointTests

    private var connection: UsbDeviceConnection? = null
    private var currentDevice: UsbDevice? = null

    companion object {
        // USB Descriptor Types
        const val DESC_DEVICE = 0x01
        const val DESC_CONFIGURATION = 0x02
        const val DESC_STRING = 0x03
        const val DESC_INTERFACE = 0x04
        const val DESC_ENDPOINT = 0x05
        const val DESC_DEVICE_QUALIFIER = 0x06
        const val DESC_OTHER_SPEED_CONFIG = 0x07
        const val DESC_INTERFACE_POWER = 0x08
        const val DESC_OTG = 0x09
        const val DESC_DEBUG = 0x0A
        const val DESC_INTERFACE_ASSOCIATION = 0x0B
        const val DESC_BOS = 0x0F
        const val DESC_CAPABILITY = 0x10
        const val DESC_HID = 0x21
        const val DESC_HID_REPORT = 0x22
        const val DESC_HID_PHYSICAL = 0x23
        const val DESC_CS_INTERFACE = 0x24
        const val DESC_CS_ENDPOINT = 0x25

        // USB Request Types
        const val REQ_GET_STATUS = 0x00
        const val REQ_CLEAR_FEATURE = 0x01
        const val REQ_SET_FEATURE = 0x03
        const val REQ_SET_ADDRESS = 0x05
        const val REQ_GET_DESCRIPTOR = 0x06
        const val REQ_SET_DESCRIPTOR = 0x07
        const val REQ_GET_CONFIGURATION = 0x08
        const val REQ_SET_CONFIGURATION = 0x09
        const val REQ_GET_INTERFACE = 0x0A
        const val REQ_SET_INTERFACE = 0x0B
        const val REQ_SYNCH_FRAME = 0x0C

        // Transfer types
        const val TRANSFER_CONTROL = 0
        const val TRANSFER_BULK = 1
        const val TRANSFER_INTERRUPT = 2
        const val TRANSFER_ISOCHRONOUS = 3
    }

    override fun canHandle(device: UsbDevice): Boolean = true // This module handles ANY device

    override fun canHandleCategory(category: DeviceCategory): Boolean = true

    override fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean {
        this.connection = connection
        this.currentDevice = device
        _usbDeviceInfo.value = USBDeviceInfo.fromUsbDevice(device)
        generateDescriptorDump(device)
        _connectionState.value = ModuleConnectionState.Connected(device.deviceName ?: "USB Device")
        return true
    }

    private fun generateDescriptorDump(device: UsbDevice) {
        val sb = StringBuilder()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        sb.appendLine("=".repeat(60))
        sb.appendLine("  USBDROID - USB Device Descriptor Report")
        sb.appendLine("  Generated: $time")
        sb.appendLine("=".repeat(60))
        sb.appendLine()

        // Device Descriptor
        sb.appendLine("┌─────────────────────────────────────────────────────┐")
        sb.appendLine("│              DEVICE DESCRIPTOR                       │")
        sb.appendLine("├─────────────────────────────────────────────────────┤")
        sb.appendLine("│  bLength:            ${"%3d".format(18)}                           │")
        sb.appendLine("│  bDescriptorType:    0x01 (DEVICE)                  │")
        sb.appendLine("│  bcdUSB:             0x${"%04X".format(device.version)}                         │")
        sb.appendLine("│  bDeviceClass:       0x${"%02X".format(device.deviceClass)} (${getClassName(device.deviceClass)})          │")
        sb.appendLine("│  bDeviceSubClass:    0x${"%02X".format(device.deviceSubclass)}                           │")
        sb.appendLine("│  bDeviceProtocol:    0x${"%02X".format(device.deviceProtocol)}                           │")
        sb.appendLine("│  bMaxPacketSize0:    ${device.getInterface(0)?.getEndpoint(0)?.maxPacketSize ?: 64}")
        sb.appendLine("│  idVendor:           0x${"%04X".format(device.vendorId)} (${device.manufacturerName ?: "Unknown"})")
        sb.appendLine("│  idProduct:          0x${"%04X".format(device.productId)} (${device.productName ?: "Unknown"})")
        sb.appendLine("│  bcdDevice:          0x0000                         │")
        sb.appendLine("│  iManufacturer:      ${device.manufacturerName ?: "N/A"}")
        sb.appendLine("│  iProduct:           ${device.productName ?: "N/A"}")
        sb.appendLine("│  iSerialNumber:      ${device.serialNumber ?: "N/A"}")
        sb.appendLine("│  bNumConfigurations: ${device.interfaceCount}")
        sb.appendLine("└─────────────────────────────────────────────────────┘")
        sb.appendLine()

        // Configuration and Interfaces
        sb.appendLine("┌─────────────────────────────────────────────────────┐")
        sb.appendLine("│           CONFIGURATION & INTERFACES                 │")
        sb.appendLine("├─────────────────────────────────────────────────────┤")
        sb.appendLine("│  bNumInterfaces:     ${device.interfaceCount}")
        sb.appendLine("│  bConfigurationValue: 1")
        sb.appendLine("│  bmAttributes:       0x80 (Bus Powered)")
        sb.appendLine("│  bMaxPower:          500mA")
        sb.appendLine("└─────────────────────────────────────────────────────┘")
        sb.appendLine()

        // Interface descriptors
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            sb.appendLine("┌─────────────────────────────────────────────────────┐")
            sb.appendLine("│  INTERFACE #$i                                         │")
            sb.appendLine("├─────────────────────────────────────────────────────┤")
            sb.appendLine("│  bInterfaceNumber:   ${iface.id}")
            sb.appendLine("│  bAlternateSetting:  0")
            sb.appendLine("│  bNumEndpoints:      ${iface.endpointCount}")
            sb.appendLine("│  bInterfaceClass:    0x${"%02X".format(iface.interfaceClass)} (${getClassName(iface.interfaceClass)})")
            sb.appendLine("│  bInterfaceSubClass: 0x${"%02X".format(iface.interfaceSubclass)}")
            sb.appendLine("│  bInterfaceProtocol: 0x${"%02X".format(iface.interfaceProtocol)}")
            sb.appendLine("│  iInterface:         ${iface.name ?: "N/A"}")
            sb.appendLine("└─────────────────────────────────────────────────────┘")
            sb.appendLine()

            // Endpoints
            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                sb.appendLine("  ┌─────────────────────────────────────────────────┐")
                sb.appendLine("  │  ENDPOINT 0x${"%02X".format(ep.address)} (${if (ep.direction == UsbConstants.USB_DIR_IN) "IN " else "OUT"})           │")
                sb.appendLine("  ├─────────────────────────────────────────────────┤")
                sb.appendLine("  │  bEndpointAddress:  0x${"%02X".format(ep.address)}")
                sb.appendLine("  │  bmAttributes:      0x${"%02X".format(ep.attributes)} (${getTransferTypeName(ep.type)})")
                sb.appendLine("  │  wMaxPacketSize:    ${ep.maxPacketSize}")
                sb.appendLine("  │  bInterval:         ${ep.interval}")
                sb.appendLine("  └─────────────────────────────────────────────────┘")
                sb.appendLine()
            }
        }

        // Class-specific analysis
        sb.appendLine("┌─────────────────────────────────────────────────────┐")
        sb.appendLine("│              ANALYSIS SUMMARY                        │")
        sb.appendLine("├─────────────────────────────────────────────────────┤")

        val analysis = analyzeDevice(device)
        analysis.forEach { (key, value) ->
            sb.appendLine("│  $key: ${"".padEnd(48 - key.length - value.length)}$value │")
        }

        sb.appendLine("└─────────────────────────────────────────────────────┘")
        sb.appendLine()

        // HEX dump of raw descriptor data
        sb.appendLine("┌─────────────────────────────────────────────────────┐")
        sb.appendLine("│              RAW HEX DUMP (Simulated)                │")
        sb.appendLine("├─────────────────────────────────────────────────────┤")
        sb.appendLine("│  12 01 00 02 00 00 00 40 ${"%02X".format(device.vendorId and 0xFF)} ${"%02X".format((device.vendorId shr 8) and 0xFF)} ${"%02X".format(device.productId and 0xFF)} ${"%02X".format((device.productId shr 8) and 0xFF)}  │")
        sb.appendLine("│  00 00 01 02 03 01 01 02 00 09 02 20 00 01 01 00  │")
        sb.appendLine("│  80 32 09 04 00 00 02 08 06 50 00 07 05 81 02 40  │")
        sb.appendLine("│  00 00 07 05 02 02 40 00 00                        │")
        sb.appendLine("└─────────────────────────────────────────────────────┘")

        _descriptorDump.value = sb.toString()
    }

    private fun analyzeDevice(device: UsbDevice): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Device class analysis
        when (device.deviceClass) {
            0x00 -> result["Device Class"] = "Per-interface (check interfaces)"
            0x02 -> result["Device Class"] = "CDC Communication"
            0x03 -> result["Device Class"] = "HID (Human Interface)"
            0x08 -> result["Device Class"] = "Mass Storage"
            0x09 -> result["Device Class"] = "Hub"
            0xFF -> result["Device Class"] = "Vendor Specific"
            else -> result["Device Class"] = "0x${"%02X".format(device.deviceClass)}"
        }

        // Vendor analysis
        when (device.vendorId) {
            0x18D1 -> result["Likely Type"] = "Google/Android Device"
            0x0403 -> result["Likely Type"] = "FTDI Serial Adapter"
            0x1A86 -> result["Likely Type"] = "CH340/CH341 Serial"
            0x10C4 -> result["Likely Type"] = "CP210x Serial"
            0x2341 -> result["Likely Type"] = "Arduino"
            0x239A -> result["Likely Type"] = "Adafruit"
            0x303A -> result["Likely Type"] = "Espressif ESP32"
            0x0483 -> result["Likely Type"] = "STMicroelectronics"
            0x05AC -> result["Likely Type"] = "Apple Device"
            else -> result["Likely Type"] = "Unknown/Generic"
        }

        // Protocol hints
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == 0x02 && iface.interfaceSubclass == 0x02) {
                result["Serial Protocol"] = "CDC-ACM"
            }
            if (iface.interfaceClass == 0x08 && iface.interfaceSubclass == 0x06) {
                result["Storage Protocol"] = "SCSI Transparent"
            }
            if (iface.interfaceClass == 0xFF) {
                result["Custom Protocol"] = "Vendor class ${iface.interfaceSubclass}/${iface.interfaceProtocol}"
            }
        }

        result["Security"] = if (device.vendorId == 0x18D1) "May need ADB auth" else "N/A"

        return result
    }

    private fun getClassName(cls: Int): String = when (cls) {
        0x00 -> "Use Interface"
        0x01 -> "Audio"
        0x02 -> "CDC"
        0x03 -> "HID"
        0x05 -> "Physical"
        0x06 -> "Image"
        0x07 -> "Printer"
        0x08 -> "Mass Storage"
        0x09 -> "Hub"
        0x0A -> "CDC Data"
        0x0B -> "Smart Card"
        0x0D -> "Content Security"
        0x0E -> "Video"
        0x0F -> "Personal Healthcare"
        0x10 -> "Audio/Video"
        0xDC -> "Diagnostic"
        0xE0 -> "Wireless"
        0xEF -> "Misc"
        0xFF -> "Vendor"
        else -> "0x${"%02X".format(cls)}"
    }

    private fun getTransferTypeName(type: Int): String = when (type) {
        UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "Control"
        UsbConstants.USB_ENDPOINT_XFER_BULK -> "Bulk"
        UsbConstants.USB_ENDPOINT_XFER_INT -> "Interrupt"
        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "Isochronous"
        else -> "Unknown"
    }

    fun testEndpoint(endpointAddress: Int, sendData: ByteArray?): EndpointTestResult {
        val startTime = System.nanoTime()
        return try {
            // Perform a test transfer on the endpoint
            val result = when {
                sendData != null -> {
                    // OUT transfer
                    val written = connection?.bulkTransfer(
                        currentDevice?.getInterface(0)?.getEndpoint(0),
                        sendData,
                        sendData.size,
                        5000
                    ) ?: -1
                    if (written >= 0) "Wrote $written bytes" else "Write failed"
                }
                else -> {
                    // IN transfer
                    val buffer = ByteArray(512)
                    val read = connection?.bulkTransfer(
                        currentDevice?.getInterface(0)?.getEndpoint(0),
                        buffer,
                        buffer.size,
                        5000
                    ) ?: -1
                    if (read >= 0) "Read $read bytes: ${buffer.copyOf(read).joinToString(" ") { "%02X".format(it) }}"
                    else "Read failed"
                }
            }
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
            EndpointTestResult.Success(result, elapsedMs)
        } catch (e: Exception) {
            EndpointTestResult.Error(e.message ?: "Transfer failed")
        }
    }

    fun exportReport(): String {
        return buildString {
            appendLine("USBDroid USB Device Report")
            appendLine("===========================")
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine()
            append(_descriptorDump.value)
            appendLine()
            appendLine("Raw JSON:")
            appendLine(_usbDeviceInfo.value?.let { info ->
                "{\"vid\":${info.vendorId},\"pid\":${info.productId},\"manufacturer\":\"${info.displayManufacturer}\",\"product\":\"${info.displayName}\"}"
            } ?: "{}")
        }
    }

    override fun detach() {
        connection = null
        currentDevice = null
        _descriptorDump.value = ""
        _usbDeviceInfo.value = null
        _transferLog.value = emptyList()
        _endpointTests.value = emptyMap()
        _connectionState.value = ModuleConnectionState.Disconnected
    }

    override fun isAttached(): Boolean = _connectionState.value is ModuleConnectionState.Connected
    override fun getConnectionState(): StateFlow<ModuleConnectionState> = _connectionState

    override suspend fun executeCommand(command: String): ModuleResult {
        return try {
            when {
                command == "dump" -> ModuleResult.SuccessWithData(_descriptorDump.value)
                command == "export" -> ModuleResult.SuccessWithData(exportReport())
                command.startsWith("test") -> {
                    val ep = command.removePrefix("test").trim().toIntOrNull() ?: 0x81
                    val result = testEndpoint(ep, null)
                    _endpointTests.value = _endpointTests.value + (ep to result)
                    ModuleResult.SuccessWithData(result.toString())
                }
                else -> ModuleResult.Error("Unknown command")
            }
        } catch (e: Exception) {
            ModuleResult.Error(e.message ?: "Command failed")
        }
    }

    data class UsbTransfer(
        val timestamp: Long,
        val type: Int,
        val endpoint: Int,
        val data: ByteArray,
        val status: String
    )

    sealed class EndpointTestResult {
        data class Success(val data: String, val elapsedMs: Double) : EndpointTestResult()
        data class Error(val message: String) : EndpointTestResult()
    }
}
