package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.usbdroid.data.model.DeviceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * HID Analyzer & Emulator Module - USB Human Interface Device analysis.
 */
class HidModule : UsbModule {

    override val moduleId: String = "hid"
    override val moduleName: String = "HID Analyzer"
    override val moduleDescription: String = "Analyze keyboards, mice, gamepads, and other HID devices"
    override val supportedCategories: List<DeviceCategory> = listOf(DeviceCategory.HID_INPUT)
    override val supportedVids: List<Int> = emptyList()
    override val supportedPids: List<Pair<Int, Int>> = emptyList()
    override val isHardwareModule: Boolean = true

    private val _connectionState = MutableStateFlow<ModuleConnectionState>(ModuleConnectionState.Disconnected)
    private val _reports = MutableStateFlow<List<HidReport>>(emptyList())
    private val _reportDescriptor = MutableStateFlow<List<HidDescriptorItem>>(emptyList())
    private val _decodedInput = MutableStateFlow<DecodedInput?>(null)
    private val _isRecording = MutableStateFlow(false)
    private val _recordedSequence = MutableStateFlow<List<HidReport>>(emptyList())

    val reports: StateFlow<List<HidReport>> = _reports
    val reportDescriptor: StateFlow<List<HidDescriptorItem>> = _reportDescriptor
    val decodedInput: StateFlow<DecodedInput?> = _decodedInput
    val isRecording: StateFlow<Boolean> = _isRecording
    val recordedSequence: StateFlow<List<HidReport>> = _recordedSequence

    private var connection: UsbDeviceConnection? = null

    companion object {
        const val HID_CLASS = 0x03
        const val HID_SUBCLASS_BOOT = 0x01
        const val HID_PROTOCOL_KEYBOARD = 0x01
        const val HID_PROTOCOL_MOUSE = 0x02

        // HID Request Types
        const val HID_GET_REPORT = 0x01
        const val HID_GET_IDLE = 0x02
        const val HID_GET_PROTOCOL = 0x03
        const val HID_SET_REPORT = 0x09
        const val HID_SET_IDLE = 0x0A
        const val HID_SET_PROTOCOL = 0x0B
        const val HID_GET_DESCRIPTOR = 0x06
        const val DESC_TYPE_REPORT = 0x22

        // Keyboard modifier bits
        const val MOD_LCTRL = 0x01
        const val MOD_LSHIFT = 0x02
        const val MOD_LALT = 0x04
        const val MOD_LGUI = 0x08
        const val MOD_RCTRL = 0x10
        const val MOD_RSHIFT = 0x20
        const val MOD_RALT = 0x40
        const val MOD_RGUI = 0x80

        // Common HID keycodes
        val KEYCODE_NAMES = mapOf(
            0x04 to "A", 0x05 to "B", 0x06 to "C", 0x07 to "D",
            0x08 to "E", 0x09 to "F", 0x0A to "G", 0x0B to "H",
            0x0C to "I", 0x0D to "J", 0x0E to "K", 0x0F to "L",
            0x10 to "M", 0x11 to "N", 0x12 to "O", 0x13 to "P",
            0x14 to "Q", 0x15 to "R", 0x16 to "S", 0x17 to "T",
            0x18 to "U", 0x19 to "V", 0x1A to "W", 0x1B to "X",
            0x1C to "Y", 0x1D to "Z",
            0x1E to "1", 0x1F to "2", 0x20 to "3", 0x21 to "4",
            0x22 to "5", 0x23 to "6", 0x24 to "7", 0x25 to "8",
            0x26 to "9", 0x27 to "0",
            0x28 to "ENTER", 0x29 to "ESC", 0x2A to "BACKSPACE",
            0x2B to "TAB", 0x2C to "SPACE", 0x2D to "MINUS",
            0x2E to "EQUAL", 0x2F to "LBRACE", 0x30 to "RBRACE",
            0x31 to "BACKSLASH", 0x33 to "SEMICOLON",
            0x34 to "QUOTE", 0x35 to "TILDE", 0x36 to "COMMA",
            0x37 to "PERIOD", 0x38 to "SLASH",
            0x39 to "CAPSLOCK", 0x3A to "F1", 0x3B to "F2",
            0x3C to "F3", 0x3D to "F4", 0x3E to "F5", 0x3F to "F6",
            0x40 to "F7", 0x41 to "F8", 0x42 to "F9", 0x43 to "F10",
            0x44 to "F11", 0x45 to "F12",
            0x46 to "PRINTSCREEN", 0x47 to "SCROLLLOCK", 0x48 to "PAUSE",
            0x49 to "INSERT", 0x4A to "HOME", 0x4B to "PAGEUP",
            0x4C to "DELETE", 0x4D to "END", 0x4E to "PAGEDOWN",
            0x4F to "RIGHT", 0x50 to "LEFT", 0x51 to "DOWN", 0x52 to "UP"
        )
    }

    override fun canHandle(device: UsbDevice): Boolean {
        if (device.deviceClass == HID_CLASS) return true
        return (0 until device.interfaceCount).any { i ->
            val iface = device.getInterface(i)
            iface.interfaceClass == HID_CLASS
        }
    }

    override fun canHandleCategory(category: DeviceCategory): Boolean = category == DeviceCategory.HID_INPUT

    override fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean {
        return try {
            _connectionState.value = ModuleConnectionState.Connecting
            this.connection = connection

            // Find HID interface
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == HID_CLASS) {
                    connection?.claimInterface(iface, true)
                    // Request report descriptor
                    requestReportDescriptor(iface.id)
                    break
                }
            }

            _connectionState.value = ModuleConnectionState.Connected(device.productName ?: "HID Device")
            true
        } catch (e: Exception) {
            Timber.e(e, "HID attach failed")
            _connectionState.value = ModuleConnectionState.Error(e.message ?: "Connection failed")
            false
        }
    }

    private fun requestReportDescriptor(interfaceId: Int) {
        val buffer = ByteArray(256)
        // USB control transfer to get report descriptor
        connection?.controlTransfer(
            0x81, // DEV_TO_HOST | CLASS | INTERFACE
            HID_GET_DESCRIPTOR,
            (DESC_TYPE_REPORT shl 8) or interfaceId,
            interfaceId,
            buffer,
            buffer.size,
            5000
        )?.let { len ->
            if (len > 0) {
                parseReportDescriptor(buffer.copyOf(len))
            }
        }
    }

    private fun parseReportDescriptor(data: ByteArray) {
        val items = mutableListOf<HidDescriptorItem>()
        var i = 0
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            val tag = b shr 4
            val type = (b shr 2) and 0x03
            val size = when (b and 0x03) {
                0 -> 0
                1 -> 1
                2 -> 2
                3 -> 4
                else -> 0
            }
            if (i + size >= data.size) break

            val value = when (size) {
                0 -> 0
                1 -> data[i + 1].toInt() and 0xFF
                2 -> (data[i + 1].toInt() and 0xFF) or ((data[i + 2].toInt() and 0xFF) shl 8)
                4 -> (data[i + 1].toInt() and 0xFF) or ((data[i + 2].toInt() and 0xFF) shl 8) or
                        ((data[i + 3].toInt() and 0xFF) shl 16) or ((data[i + 4].toInt() and 0xFF) shl 24)
                else -> 0
            }

            val typeName = when (type) {
                0 -> "Main"
                1 -> "Global"
                2 -> "Local"
                else -> "Reserved"
            }

            val tagName = getTagName(type, tag)
            items.add(HidDescriptorItem(typeName, tagName, size, value))
            i += 1 + size
        }
        _reportDescriptor.value = items
    }

    private fun getTagName(type: Int, tag: Int): String {
        return when (type) {
            0 -> when (tag) { // Main
                8 -> "Input"
                9 -> "Output"
                11 -> "Feature"
                10 -> "Collection"
                12 -> "End Collection"
                else -> "Main_$tag"
            }
            1 -> when (tag) { // Global
                0 -> "Usage Page"
                1 -> "Logical Minimum"
                2 -> "Logical Maximum"
                3 -> "Physical Minimum"
                4 -> "Physical Maximum"
                5 -> "Unit Exponent"
                6 -> "Unit"
                7 -> "Report Size"
                8 -> "Report ID"
                9 -> "Report Count"
                10 -> "Push"
                11 -> "Pop"
                else -> "Global_$tag"
            }
            2 -> when (tag) { // Local
                0 -> "Usage"
                1 -> "Usage Minimum"
                2 -> "Usage Maximum"
                3 -> "Designator Index"
                4 -> "Designator Minimum"
                5 -> "Designator Maximum"
                7 -> "String Index"
                8 -> "String Minimum"
                9 -> "String Maximum"
                10 -> "Delimiter"
                else -> "Local_$tag"
            }
            else -> "Tag_$tag"
        }
    }

    fun parseKeyboardReport(report: ByteArray): DecodedInput {
        if (report.size < 8) return DecodedInput.Keyboard(emptyList(), emptyList())

        val modifiers = report[0].toInt() and 0xFF
        val pressedKeys = mutableListOf<String>()
        val pressedModifiers = mutableListOf<String>()

        if (modifiers and MOD_LCTRL != 0) pressedModifiers.add("LCtrl")
        if (modifiers and MOD_LSHIFT != 0) pressedModifiers.add("LShift")
        if (modifiers and MOD_LALT != 0) pressedModifiers.add("LAlt")
        if (modifiers and MOD_LGUI != 0) pressedModifiers.add("LGui")
        if (modifiers and MOD_RCTRL != 0) pressedModifiers.add("RCtrl")
        if (modifiers and MOD_RSHIFT != 0) pressedModifiers.add("RShift")
        if (modifiers and MOD_RALT != 0) pressedModifiers.add("RAlt")
        if (modifiers and MOD_RGUI != 0) pressedModifiers.add("RGui")

        for (i in 2 until minOf(8, report.size)) {
            val keycode = report[i].toInt() and 0xFF
            if (keycode != 0) {
                pressedKeys.add(KEYCODE_NAMES[keycode] ?: "0x${keycode.toString(16).padStart(2, '0').uppercase()}")
            }
        }

        return DecodedInput.Keyboard(pressedKeys, pressedModifiers)
    }

    fun parseMouseReport(report: ByteArray): DecodedInput {
        if (report.size < 3) return DecodedInput.Mouse(0, 0, false, false, false, 0)

        val buttons = report[0].toInt() and 0xFF
        val x = report[1].toInt() // Signed
        val y = if (report.size > 2) report[2].toInt() else 0 // Signed
        val wheel = if (report.size > 3) report[3].toInt() else 0

        return DecodedInput.Mouse(
            xMovement = x,
            yMovement = y,
            leftButton = buttons and 0x01 != 0,
            rightButton = buttons and 0x02 != 0,
            middleButton = buttons and 0x04 != 0,
            wheelDelta = wheel
        )
    }

    fun parseGamepadReport(report: ByteArray): DecodedInput {
        // Generic gamepad parsing - varies by controller
        return DecodedInput.Gamepad(
            buttons = emptyMap(),
            leftX = 0, leftY = 0,
            rightX = 0, rightY = 0,
            leftTrigger = 0, rightTrigger = 0
        )
    }

    fun startRecording() {
        _isRecording.value = true
        _recordedSequence.value = emptyList()
    }

    fun stopRecording(): List<HidReport> {
        _isRecording.value = false
        return _recordedSequence.value
    }

    fun clearReports() {
        _reports.value = emptyList()
    }

    override fun detach() {
        connection = null
        _reports.value = emptyList()
        _reportDescriptor.value = emptyList()
        _decodedInput.value = null
        _isRecording.value = false
        _connectionState.value = ModuleConnectionState.Disconnected
    }

    override fun isAttached(): Boolean = _connectionState.value is ModuleConnectionState.Connected
    override fun getConnectionState(): StateFlow<ModuleConnectionState> = _connectionState

    override suspend fun executeCommand(command: String): ModuleResult {
        return try {
            when (command) {
                "record" -> { startRecording(); ModuleResult.Success }
                "stop" -> { stopRecording(); ModuleResult.Success }
                "clear" -> { clearReports(); ModuleResult.Success }
                else -> ModuleResult.Error("Unknown command: $command")
            }
        } catch (e: Exception) {
            ModuleResult.Error(e.message ?: "Command failed")
        }
    }

    data class HidDescriptorItem(
        val type: String,
        val tag: String,
        val size: Int,
        val value: Int
    )

    data class HidReport(
        val timestamp: Long,
        val data: ByteArray,
        val decoded: String
    )

    sealed class DecodedInput {
        data class Keyboard(
            val keys: List<String>,
            val modifiers: List<String>
        ) : DecodedInput()

        data class Mouse(
            val xMovement: Int,
            val yMovement: Int,
            val leftButton: Boolean,
            val rightButton: Boolean,
            val middleButton: Boolean,
            val wheelDelta: Int
        ) : DecodedInput()

        data class Gamepad(
            val buttons: Map<String, Boolean>,
            val leftX: Int, val leftY: Int,
            val rightX: Int, val rightY: Int,
            val leftTrigger: Int, val rightTrigger: Int
        ) : DecodedInput()
    }
}
