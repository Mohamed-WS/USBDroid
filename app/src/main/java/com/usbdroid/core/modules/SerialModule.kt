package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import com.usbdroid.data.model.DeviceCategory
import com.hoho.android.usbserial.driver.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Serial Terminal Module - Handles FTDI, CH340, CP210x, CDC-ACM, and other UART chips.
 */
class SerialModule : UsbModule {

    override val moduleId: String = "serial"
    override val moduleName: String = "Serial Terminal"
    override val moduleDescription: String = "UART serial communication with Arduino, ESP32, and other microcontrollers"
    override val supportedCategories: List<DeviceCategory> = listOf(DeviceCategory.SERIAL_UART)
    override val supportedVids: List<Int> = listOf(
        0x0403, // FTDI
        0x1A86, // CH340/CH341
        0x10C4, // CP210x
        0x067B, // PL2303
        0x2341, // Arduino
        0x239A, // Adafruit
        0x303A, // Espressif
        0x0483, // STM32
        0x2E8A, // Raspberry Pi
        0x04E8, // Samsung (modem)
        0x12D1, // Huawei (modem)
        0x1E0E  // Qualcomm (modem)
    )
    override val supportedPids: List<Pair<Int, Int>> = emptyList()
    override val isHardwareModule: Boolean = true

    private var driver: UsbSerialDriver? = null
    private var port: UsbSerialPort? = null
    private var deviceConnection: UsbDeviceConnection? = null
    private val _connectionState = MutableStateFlow<ModuleConnectionState>(ModuleConnectionState.Disconnected)
    private val _receivedData = MutableStateFlow<ByteArray?>(null)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    val receivedData: StateFlow<ByteArray?> = _receivedData
    val baudRate = MutableStateFlow(115200)
    val dataBits = MutableStateFlow(8)
    val stopBits = MutableStateFlow(UsbSerialPort.STOPBITS_1)
    val parity = MutableStateFlow(UsbSerialPort.PARITY_NONE)
    val dtr = MutableStateFlow(false)
    val rts = MutableStateFlow(false)

    override fun canHandle(device: UsbDevice): Boolean {
        return canHandleDeviceClass(device) || device.vendorId in supportedVids
    }

    private fun canHandleDeviceClass(device: UsbDevice): Boolean {
        if (device.deviceClass == 0x02) return true // CDC
        if (device.deviceClass == 0x0A) return true // CDC Data
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == 0x02 && iface.interfaceSubclass == 0x02) return true
            if (iface.interfaceClass == 0x0A) return true
        }
        return false
    }

    override fun canHandleCategory(category: DeviceCategory): Boolean = category == DeviceCategory.SERIAL_UART

    override fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean {
        return try {
            _connectionState.value = ModuleConnectionState.Connecting
            deviceConnection = connection

            // Try to find a serial driver for this device using the default prober
            driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            
            // If no driver found with default prober, device might not be supported
            // or might need custom configuration

            if (driver != null && connection != null) {
                port = driver!!.ports.firstOrNull()
                port?.open(connection)
                port?.setParameters(baudRate.value, dataBits.value, stopBits.value, parity.value)
                
                _connectionState.value = ModuleConnectionState.Connected(device.deviceName ?: "Serial Device")
                startReading()
                true
            } else {
                _connectionState.value = ModuleConnectionState.Error("No serial driver found for device")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to attach serial module")
            _connectionState.value = ModuleConnectionState.Error(e.message ?: "Unknown error")
            false
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(4096)
            while (isActive) {
                try {
                    port?.read(buffer, 100)?.let { len ->
                        if (len > 0) {
                            _receivedData.value = buffer.copyOf(len)
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Timber.e(e, "Serial read error")
                    }
                }
                delay(10)
            }
        }
    }

    fun sendData(data: ByteArray): Boolean {
        return try {
            port?.write(data, 1000)
            true
        } catch (e: Exception) {
            Timber.e(e, "Serial write error")
            false
        }
    }

    fun sendString(data: String, lineEnding: LineEnding = LineEnding.CRLF) {
        val bytes = data.toByteArray() + lineEnding.bytes
        sendData(bytes)
    }

    fun setSerialParameters(
        baud: Int = baudRate.value,
        data: Int = dataBits.value,
        stop: Int = stopBits.value,
        par: Int = parity.value
    ) {
        baudRate.value = baud
        dataBits.value = data
        stopBits.value = stop
        parity.value = par

        try {
            port?.setParameters(baud, data, stop, par)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set serial parameters")
        }
    }

    fun setDtr(value: Boolean) {
        dtr.value = value
        try { port?.dtr = value } catch (_: Exception) {}
    }

    fun setRts(value: Boolean) {
        rts.value = value
        try { port?.rts = value } catch (_: Exception) {}
    }

    override fun detach() {
        readJob?.cancel()
        scope.cancel()
        try {
            port?.close()
        } catch (_: Exception) {}
        port = null
        driver = null
        deviceConnection = null
        _connectionState.value = ModuleConnectionState.Disconnected
    }

    override fun isAttached(): Boolean = _connectionState.value is ModuleConnectionState.Connected

    override fun getConnectionState(): StateFlow<ModuleConnectionState> = _connectionState

    override suspend fun executeCommand(command: String): ModuleResult {
        return try {
            sendString(command)
            ModuleResult.Success
        } catch (e: Exception) {
            ModuleResult.Error(e.message ?: "Command failed")
        }
    }

    enum class LineEnding(val bytes: ByteArray, val displayName: String) {
        CR("\r".toByteArray(), "CR"),
        LF("\n".toByteArray(), "LF"),
        CRLF("\r\n".toByteArray(), "CRLF"),
        NONE(ByteArray(0), "None")
    }

    companion object {
        val COMMON_BAUD_RATES = listOf(
            300, 600, 1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400,
            57600, 76800, 115200, 230400, 250000, 460800, 500000, 921600,
            1000000, 1500000, 2000000, 3000000, 4000000
        )

        val PRESETS = mapOf(
            "Arduino Uno" to SerialPreset(9600, 8, 1, UsbSerialPort.PARITY_NONE),
            "Arduino Mega" to SerialPreset(115200, 8, 1, UsbSerialPort.PARITY_NONE),
            "ESP32" to SerialPreset(115200, 8, 1, UsbSerialPort.PARITY_NONE),
            "ESP8266" to SerialPreset(115200, 8, 1, UsbSerialPort.PARITY_NONE),
            "STM32" to SerialPreset(115200, 8, 1, UsbSerialPort.PARITY_NONE),
            "Raspberry Pi Pico" to SerialPreset(115200, 8, 1, UsbSerialPort.PARITY_NONE),
            "GPS NMEA" to SerialPreset(9600, 8, 1, UsbSerialPort.PARITY_NONE),
            "GSM Modem" to SerialPreset(115200, 8, 1, UsbSerialPort.PARITY_NONE),
            "LoRa" to SerialPreset(57600, 8, 1, UsbSerialPort.PARITY_NONE)
        )
    }

    data class SerialPreset(
        val baudRate: Int,
        val dataBits: Int,
        val stopBits: Int,
        val parity: Int
    )
}
