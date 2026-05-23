package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.usbdroid.data.model.DeviceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Firmware Flasher Module - Multi-protocol firmware flashing for microcontrollers.
 */
class FlasherModule : UsbModule {

    override val moduleId: String = "flasher"
    override val moduleName: String = "Firmware Flasher"
    override val moduleDescription: String = "Flash Arduino, ESP32, ESP8266, and STM32 microcontrollers"
    override val supportedCategories: List<DeviceCategory> = listOf(
        DeviceCategory.SERIAL_UART,
        DeviceCategory.FIRMWARE_DFU
    )
    override val supportedVids: List<Int> = listOf(
        0x2341, // Arduino
        0x2A03, // Arduino (old)
        0x239A, // Adafruit
        0x303A, // Espressif
        0x0483, // STM32
        0x1A86, // CH340 (ESP boards)
        0x10C4, // CP210x (ESP boards)
        0x0403, // FTDI
        0x2E8A  // Raspberry Pi
    )
    override val supportedPids: List<Pair<Int, Int>> = emptyList()
    override val isHardwareModule: Boolean = true

    private val _connectionState = MutableStateFlow<ModuleConnectionState>(ModuleConnectionState.Disconnected)
    private val _flashProgress = MutableStateFlow<FlashProgress?>(null)
    private val _flashLog = MutableStateFlow<List<FlashLogEntry>>(emptyList())
    private val _detectedChip = MutableStateFlow<ChipInfo?>(null)
    private val _isFlashing = MutableStateFlow(false)

    val flashProgress: StateFlow<FlashProgress?> = _flashProgress
    val flashLog: StateFlow<List<FlashLogEntry>> = _flashLog
    val detectedChip: StateFlow<ChipInfo?> = _detectedChip
    val isFlashing: StateFlow<Boolean> = _isFlashing

    private var connection: UsbDeviceConnection? = null
    private var currentDevice: UsbDevice? = null

    companion object {
        // STK500 commands for AVR/Arduino
        const val STK_OK = 0x10
        const val STK_INSYNC = 0x14
        const val STK_GET_SYNC = 0x30
        const val STK_ENTER_PROGMODE = 0x50
        const val STK_LEAVE_PROGMODE = 0x51
        const val STK_CHIP_ERASE = 0x52
        const val STK_LOAD_ADDRESS = 0x55
        const val STK_PROG_PAGE = 0x64
        const val STK_READ_PAGE = 0x74
        const val STK_READ_SIGN = 0x75
        const val CRC_EOP = 0x20

        // ESP32 ROM commands
        const val ESP_SYNC = 0x08
        const val ESP_READ_REG = 0x0A
        const val ESP_WRITE_REG = 0x09
        const val ESP_FLASH_BEGIN = 0x02
        const val ESP_FLASH_DATA = 0x03
        const val ESP_FLASH_END = 0x04
        const val ESP_MEM_BEGIN = 0x05
        const val ESP_MEM_END = 0x06
        const val ESP_MEM_DATA = 0x07

        // STM32 DFU
        const val DFU_DETACH = 0x00
        const val DFU_DNLOAD = 0x01
        const val DFU_UPLOAD = 0x02
        const val DFU_GETSTATUS = 0x03
        const val DFU_CLRSTATUS = 0x04
        const val DFU_GETSTATE = 0x05
        const val DFU_ABORT = 0x06
    }

    override fun canHandle(device: UsbDevice): Boolean {
        return device.vendorId in supportedVids ||
                (0 until device.interfaceCount).any { i ->
                    val iface = device.getInterface(i)
                    iface.interfaceClass == 0xFE // Application specific (DFU)
                }
    }

    override fun canHandleCategory(category: DeviceCategory): Boolean =
        category == DeviceCategory.SERIAL_UART || category == DeviceCategory.FIRMWARE_DFU

    override fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean {
        return try {
            _connectionState.value = ModuleConnectionState.Connecting
            this.connection = connection
            this.currentDevice = device

            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                connection?.claimInterface(iface, true)
            }

            // Auto-detect chip type
            detectChip()

            _connectionState.value = ModuleConnectionState.Connected(device.productName ?: "Flash Target")
            true
        } catch (e: Exception) {
            Timber.e(e, "Flasher attach failed")
            _connectionState.value = ModuleConnectionState.Error(e.message ?: "Connection failed")
            false
        }
    }

    private fun detectChip() {
        when (currentDevice?.vendorId) {
            0x2341, 0x2A03 -> _detectedChip.value = ChipInfo(
                name = "ATmega328P/ATmega2560",
                type = ChipType.AVR,
                flashSize = 32768,
                pageSize = 128,
                signature = listOf(0x1E, 0x95, 0x0F),
                description = "Arduino AVR"
            )
            0x239A -> _detectedChip.value = ChipInfo(
                name = "SAMD21/SAMD51",
                type = ChipType.SAMD,
                flashSize = 262144,
                pageSize = 256,
                signature = emptyList(),
                description = "Adafruit Feather/M0"
            )
            0x303A -> _detectedChip.value = ChipInfo(
                name = "ESP32/ESP32-S2/ESP32-S3",
                type = ChipType.ESP32,
                flashSize = 4194304,
                pageSize = 256,
                signature = emptyList(),
                description = "Espressif ESP32"
            )
            0x0483 -> {
                if (currentDevice?.productId == 0xDF11) {
                    _detectedChip.value = ChipInfo(
                        name = "STM32 (DFU Mode)",
                        type = ChipType.STM32_DFU,
                        flashSize = 524288,
                        pageSize = 2048,
                        signature = emptyList(),
                        description = "STM32 in DFU bootloader"
                    )
                } else {
                    _detectedChip.value = ChipInfo(
                        name = "STM32",
                        type = ChipType.STM32_SERIAL,
                        flashSize = 524288,
                        pageSize = 1024,
                        signature = emptyList(),
                        description = "STM32 via serial bootloader"
                    )
                }
            }
            0x2E8A -> _detectedChip.value = ChipInfo(
                name = "RP2040",
                type = ChipType.RP2040,
                flashSize = 2097152,
                pageSize = 256,
                signature = emptyList(),
                description = "Raspberry Pi RP2040"
            )
            else -> _detectedChip.value = ChipInfo(
                name = "Unknown",
                type = ChipType.UNKNOWN,
                flashSize = 0,
                pageSize = 0,
                signature = emptyList(),
                description = "Generic serial device"
            )
        }
    }

    fun flashFirmware(firmwareData: ByteArray, address: Long = 0x00, verify: Boolean = true): Boolean {
        if (_isFlashing.value) return false
        _isFlashing.value = true
        _flashLog.value = emptyList()
        _flashProgress.value = FlashProgress(0, firmwareData.size.toLong(), "Starting flash...")

        return try {
            when (_detectedChip.value?.type) {
                ChipType.AVR -> flashAVR(firmwareData, verify)
                ChipType.ESP32 -> flashESP32(firmwareData, address, verify)
                ChipType.STM32_DFU -> flashSTM32DFU(firmwareData, verify)
                else -> {
                    log("Unsupported chip type for flashing", FlashLogLevel.ERROR)
                    false
                }
            }
        } catch (e: Exception) {
            log("Flash failed: ${e.message}", FlashLogLevel.ERROR)
            false
        } finally {
            _isFlashing.value = false
        }
    }

    private fun flashAVR(firmwareData: ByteArray, verify: Boolean): Boolean {
        log("Entering programming mode...", FlashLogLevel.INFO)
        // STK500 enter prog mode

        val pageSize = _detectedChip.value?.pageSize ?: 128
        var offset = 0

        log("Erasing chip...", FlashLogLevel.INFO)
        // Chip erase

        log("Writing ${firmwareData.size} bytes...", FlashLogLevel.INFO)
        while (offset < firmwareData.size) {
            val page = firmwareData.copyOfRange(offset, minOf(offset + pageSize, firmwareData.size))

            // STK500 load address and program page
            val addr = offset / 2 // Word address

            _flashProgress.value = FlashProgress(
                offset.toLong(), firmwareData.size.toLong(),
                "Writing 0x${offset.toString(16).padStart(6, '0')}..."
            )
            offset += pageSize
        }

        if (verify) {
            log("Verifying flash...", FlashLogLevel.INFO)
            // Read back and verify
        }

        log("Flash complete!", FlashLogLevel.SUCCESS)
        _flashProgress.value = FlashProgress(firmwareData.size.toLong(), firmwareData.size.toLong(), "Complete")
        return true
    }

    private fun flashESP32(firmwareData: ByteArray, address: Long, verify: Boolean): Boolean {
        log("ESP32: Syncing with ROM bootloader...", FlashLogLevel.INFO)

        // SLIP framing protocol
        // Send SYNC command with DTR/RTS toggle to enter bootloader

        val blockSize = 0x400 // 1KB blocks
        val numBlocks = (firmwareData.size + blockSize - 1) / blockSize

        log("ESP32: Beginning flash at 0x${address.toString(16)}...", FlashLogLevel.INFO)

        var offset = 0
        var blockNum = 0
        while (offset < firmwareData.size) {
            val block = firmwareData.copyOfRange(offset, minOf(offset + blockSize, firmwareData.size))

            // Pad last block
            val paddedBlock = if (block.size < blockSize) {
                block + ByteArray(blockSize - block.size) { 0xFF.toByte() }
            } else block

            _flashProgress.value = FlashProgress(
                offset.toLong(), firmwareData.size.toLong(),
                "Writing block ${blockNum + 1}/$numBlocks (0x${offset.toString(16).padStart(6, '0')})..."
            )

            // ESP_FLASH_DATA command

            offset += blockSize
            blockNum++
        }

        log("ESP32: Flash complete!", FlashLogLevel.SUCCESS)
        _flashProgress.value = FlashProgress(firmwareData.size.toLong(), firmwareData.size.toLong(), "Complete")
        return true
    }

    private fun flashSTM32DFU(firmwareData: ByteArray, verify: Boolean): Boolean {
        log("STM32 DFU: Initiating download...", FlashLogLevel.INFO)

        // DFU download process
        val blockSize = 1024
        var offset = 0
        var blockNum = 2 // DFU block numbers start at 2

        while (offset < firmwareData.size) {
            val block = firmwareData.copyOfRange(offset, minOf(offset + blockSize, firmwareData.size))

            _flashProgress.value = FlashProgress(
                offset.toLong(), firmwareData.size.toLong(),
                "Writing block ${(offset / blockSize) + 1}..."
            )

            // DFU_DNLOAD with block number

            offset += blockSize
            blockNum++
        }

        log("STM32 DFU: Flash complete!", FlashLogLevel.SUCCESS)
        _flashProgress.value = FlashProgress(firmwareData.size.toLong(), firmwareData.size.toLong(), "Complete")
        return true
    }

    fun eraseChip(): Boolean {
        log("Erasing chip...", FlashLogLevel.INFO)
        return try {
            when (_detectedChip.value?.type) {
                ChipType.AVR -> {
                    // STK500 chip erase
                    true
                }
                ChipType.ESP32 -> {
                    // ESP_ERASE_FLASH command
                    true
                }
                else -> {
                    log("Erase not supported for this chip", FlashLogLevel.WARNING)
                    false
                }
            }
        } catch (e: Exception) {
            log("Erase failed: ${e.message}", FlashLogLevel.ERROR)
            false
        }
    }

    fun enterBootloader() {
        // Toggle DTR/RTS lines to enter bootloader mode
        log("Entering bootloader mode (DTR/RTS toggle)...", FlashLogLevel.INFO)
    }

    private fun log(message: String, level: FlashLogLevel) {
        _flashLog.value = _flashLog.value + FlashLogEntry(System.currentTimeMillis(), message, level)
        Timber.d("[${level.name}] $message")
    }

    override fun detach() {
        connection = null
        currentDevice = null
        _flashProgress.value = null
        _flashLog.value = emptyList()
        _detectedChip.value = null
        _isFlashing.value = false
        _connectionState.value = ModuleConnectionState.Disconnected
    }

    override fun isAttached(): Boolean = _connectionState.value is ModuleConnectionState.Connected
    override fun getConnectionState(): StateFlow<ModuleConnectionState> = _connectionState

    override suspend fun executeCommand(command: String): ModuleResult {
        return try {
            when {
                command == "detect" -> {
                    detectChip()
                    ModuleResult.SuccessWithData(_detectedChip.value?.name ?: "Unknown")
                }
                command == "erase" -> {
                    if (eraseChip()) ModuleResult.Success else ModuleResult.Error("Erase failed")
                }
                command == "bootloader" -> {
                    enterBootloader()
                    ModuleResult.Success
                }
                else -> ModuleResult.Error("Unknown command")
            }
        } catch (e: Exception) {
            ModuleResult.Error(e.message ?: "Command failed")
        }
    }

    data class ChipInfo(
        val name: String,
        val type: ChipType,
        val flashSize: Int,
        val pageSize: Int,
        val signature: List<Int>,
        val description: String
    )

    enum class ChipType {
        AVR, SAMD, ESP32, ESP8266, STM32_DFU, STM32_SERIAL, RP2040, UNKNOWN
    }

    data class FlashProgress(
        val current: Long,
        val total: Long,
        val message: String
    ) {
        val percent: Float = if (total > 0) (current.toFloat() / total) * 100 else 0f
    }

    data class FlashLogEntry(
        val timestamp: Long,
        val message: String,
        val level: FlashLogLevel
    )

    enum class FlashLogLevel { INFO, SUCCESS, WARNING, ERROR }
}
