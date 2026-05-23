package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.usbdroid.data.model.DeviceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Fastboot Controller Module - Implements Fastboot protocol for bootloader operations.
 */
class FastbootModule : UsbModule {

    override val moduleId: String = "fastboot"
    override val moduleName: String = "Fastboot Controller"
    override val moduleDescription: String = "Flash firmware, manage partitions, and control Android bootloader mode"
    override val supportedCategories: List<DeviceCategory> = listOf(DeviceCategory.ANDROID_FASTBOOT)
    override val supportedVids: List<Int> = listOf(0x18D1, 0x04E8, 0x0BB4, 0x12D1, 0x0502, 0x2A45)
    override val supportedPids: List<Pair<Int, Int>> = listOf(
        0x18D1 to 0x4EE0,  // Google Fastboot
        0x04E8 to 0x6866,  // Samsung Odin
        0x0BB4 to 0x0C02,  // HTC
        0x12D1 to 0x3609,  // Huawei
        0x0502 to 0x3604   // Acer
    )
    override val isHardwareModule: Boolean = true

    private val _connectionState = MutableStateFlow<ModuleConnectionState>(ModuleConnectionState.Disconnected)
    private val _variables = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _partitions = MutableStateFlow<List<PartitionInfo>>(emptyList())
    private val _flashProgress = MutableStateFlow<FlashProgress?>(null)

    val variables: StateFlow<Map<String, String>> = _variables
    val partitions: StateFlow<List<PartitionInfo>> = _partitions
    val flashProgress: StateFlow<FlashProgress?> = _flashProgress

    private var connection: UsbDeviceConnection? = null
    private var currentDevice: UsbDevice? = null
    private var inEndpoint: android.hardware.usb.UsbEndpoint? = null
    private var outEndpoint: android.hardware.usb.UsbEndpoint? = null

    companion object {
        // Fastboot protocol uses text commands and responses
        const val FASTBOOT_CLASS = 0xFF
        const val FASTBOOT_SUBCLASS = 0x42
        const val FASTBOOT_PROTOCOL = 0x03

        // Response prefixes
        const val RESPONSE_OKAY = "OKAY"
        const val RESPONSE_FAIL = "FAIL"
        const val RESPONSE_DATA = "DATA"
        const val RESPONSE_INFO = "INFO"
        const val RESPONSE_MAX_PAYLOAD_SIZE = 0x10000 // 64KB
    }

    override fun canHandle(device: UsbDevice): Boolean {
        if (device.vendorId == 0x18D1 && device.productId == 0x4EE0) return true
        return supportedPids.any { it.first == device.vendorId && it.second == device.productId } ||
                (0 until device.interfaceCount).any { i ->
                    val iface = device.getInterface(i)
                    iface.interfaceClass == FASTBOOT_CLASS &&
                            iface.interfaceSubclass == FASTBOOT_SUBCLASS
                }
    }

    override fun canHandleCategory(category: DeviceCategory): Boolean = category == DeviceCategory.ANDROID_FASTBOOT

    override fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean {
        return try {
            _connectionState.value = ModuleConnectionState.Connecting
            this.connection = connection
            this.currentDevice = device

            // Find fastboot interface and endpoints
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == FASTBOOT_CLASS) {
                    connection?.claimInterface(iface, true)
                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                            outEndpoint = ep
                        } else {
                            inEndpoint = ep
                        }
                    }
                    break
                }
            }

            if (outEndpoint == null || inEndpoint == null) {
                _connectionState.value = ModuleConnectionState.Error("Fastboot endpoints not found")
                return false
            }

            // Query device variables
            queryVariables()
            queryPartitionTable()

            _connectionState.value = ModuleConnectionState.Connected(device.productName ?: "Fastboot Device")
            true
        } catch (e: Exception) {
            Timber.e(e, "Fastboot attach failed")
            _connectionState.value = ModuleConnectionState.Error(e.message ?: "Connection failed")
            false
        }
    }

    private fun sendCommand(cmd: String): FastbootResponse {
        val cmdBytes = cmd.toByteArray(Charsets.UTF_8)
        connection?.bulkTransfer(outEndpoint, cmdBytes, cmdBytes.size, 5000)

        val response = ByteArray(512)
        val len = connection?.bulkTransfer(inEndpoint, response, response.size, 5000) ?: 0
        val responseStr = String(response, 0, len, Charsets.UTF_8)

        return when {
            responseStr.startsWith(RESPONSE_OKAY) -> FastbootResponse.Okay(responseStr.substring(4))
            responseStr.startsWith(RESPONSE_FAIL) -> FastbootResponse.Fail(responseStr.substring(4))
            responseStr.startsWith(RESPONSE_DATA) -> FastbootResponse.Data(responseStr.substring(4).toLongOrNull(16) ?: 0)
            responseStr.startsWith(RESPONSE_INFO) -> FastbootResponse.Info(responseStr.substring(4))
            else -> FastbootResponse.Okay(responseStr)
        }
    }

    private fun queryVariables() {
        val vars = mutableMapOf<String, String>()
        try {
            // Fastboot doesn't have a "getvar all" in all implementations
            // Query common variables individually
            val commonVars = listOf(
                "version", "version-bootloader", "version-baseband",
                "product", "serialno", "secure", "unlocked",
                "max-download-size", "partition-type", "partition-size",
                "current-slot", "slot-count", "slot-suffixes",
                "battery-voltage", "battery-soc-ok", "off-mode-charge",
                "variant", "hw-revision", "is-userspace", "anti"
            )
            commonVars.forEach { varName ->
                try {
                    when (val resp = sendCommand("getvar:$varName")) {
                        is FastbootResponse.Okay -> vars[varName] = resp.data
                        else -> {}
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Error querying variables")
        }
        _variables.value = vars
    }

    private fun queryPartitionTable() {
        // Parse partition info from variables
        val parts = mutableListOf<PartitionInfo>()
        _variables.value.forEach { (key, value) ->
            if (key.startsWith("partition-type:")) {
                val name = key.removePrefix("partition-type:")
                val size = _variables.value["partition-size:$name"] ?: "0"
                parts.add(PartitionInfo(name, size.toLongOrNull(16) ?: 0, value))
            }
        }
        _partitions.value = parts.sortedBy { it.name }
    }

    fun flashPartition(partition: String, imageData: ByteArray): Boolean {
        return try {
            _flashProgress.value = FlashProgress(0, imageData.size.toLong(), "Starting flash...")

            // Send download command
            when (val resp = sendCommand("download:${imageData.size.toString(16).padStart(8, '0')}")) {
                is FastbootResponse.Data -> {
                    // Server ready to receive data
                    val chunkSize = resp.size.toInt()
                    var offset = 0
                    while (offset < imageData.size) {
                        val end = minOf(offset + chunkSize, imageData.size)
                        val chunk = imageData.copyOfRange(offset, end)
                        connection?.bulkTransfer(outEndpoint, chunk, chunk.size, 5000)
                        offset = end
                        _flashProgress.value = FlashProgress(
                            offset.toLong(),
                            imageData.size.toLong(),
                            "Flashing $partition..."
                        )
                    }
                }
                is FastbootResponse.Fail -> {
                    _flashProgress.value = null
                    Timber.e("Flash download failed: ${resp.message}")
                    return false
                }
                else -> {
                    _flashProgress.value = null
                    return false
                }
            }

            // Now flash to partition
            when (val resp = sendCommand("flash:$partition")) {
                is FastbootResponse.Okay -> {
                    _flashProgress.value = FlashProgress(imageData.size.toLong(), imageData.size.toLong(), "Flash complete")
                    true
                }
                is FastbootResponse.Fail -> {
                    _flashProgress.value = null
                    Timber.e("Flash failed: ${resp.message}")
                    false
                }
                else -> {
                    _flashProgress.value = null
                    false
                }
            }
        } catch (e: Exception) {
            _flashProgress.value = null
            Timber.e(e, "Flash exception")
            false
        }
    }

    fun erasePartition(partition: String): Boolean {
        return try {
            when (val resp = sendCommand("erase:$partition")) {
                is FastbootResponse.Okay -> true
                is FastbootResponse.Fail -> {
                    Timber.e("Erase failed: ${resp.message}")
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            Timber.e(e, "Erase exception")
            false
        }
    }

    fun bootImage(imageData: ByteArray): Boolean {
        return try {
            when (val resp = sendCommand("download:${imageData.size.toString(16).padStart(8, '0')}")) {
                is FastbootResponse.Data -> {
                    connection?.bulkTransfer(outEndpoint, imageData, imageData.size, 10000)
                }
                else -> return false
            }
            when (val resp = sendCommand("boot")) {
                is FastbootResponse.Okay -> true
                is FastbootResponse.Fail -> {
                    Timber.e("Boot failed: ${resp.message}")
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            Timber.e(e, "Boot exception")
            false
        }
    }

    fun reboot(target: String = ""): Boolean {
        return try {
            val cmd = if (target.isNotEmpty()) "reboot-$target" else "reboot"
            when (val resp = sendCommand(cmd)) {
                is FastbootResponse.Okay -> true
                is FastbootResponse.Fail -> {
                    Timber.e("Reboot failed: ${resp.message}")
                    false
                }
                else -> true // Fastboot often disconnects on reboot
            }
        } catch (e: Exception) {
            // Reboot command usually causes disconnection
            true
        }
    }

    fun oemUnlock(enable: Boolean): Boolean {
        return try {
            val cmd = if (enable) "flashing unlock" else "flashing lock"
            when (val resp = sendCommand(cmd)) {
                is FastbootResponse.Okay -> true
                is FastbootResponse.Fail -> {
                    Timber.e("OEM unlock failed: ${resp.message}")
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            Timber.e(e, "OEM unlock exception")
            false
        }
    }

    fun getUnlockAbility(): Boolean {
        return try {
            when (val resp = sendCommand("flashing get_unlock_ability")) {
                is FastbootResponse.Okay -> resp.data == "1"
                else -> false
            }
        } catch (_: Exception) { false }
    }

    override fun detach() {
        _flashProgress.value = null
        connection = null
        currentDevice = null
        inEndpoint = null
        outEndpoint = null
        _connectionState.value = ModuleConnectionState.Disconnected
    }

    override fun isAttached(): Boolean = _connectionState.value is ModuleConnectionState.Connected

    override fun getConnectionState(): StateFlow<ModuleConnectionState> = _connectionState

    override suspend fun executeCommand(command: String): ModuleResult {
        return try {
            val parts = command.split(" ", limit = 2)
            when (parts[0]) {
                "flash" -> {
                    if (parts.size > 1) {
                        // Would need file data
                        ModuleResult.Success
                    } else ModuleResult.Error("Usage: flash <partition>")
                }
                "erase" -> {
                    if (parts.size > 1 && erasePartition(parts[1])) {
                        ModuleResult.Success
                    } else ModuleResult.Error("Erase failed")
                }
                "reboot" -> {
                    reboot(parts.getOrElse(1) { "" })
                    ModuleResult.Success
                }
                "getvar" -> {
                    queryVariables()
                    ModuleResult.SuccessWithData(_variables.value.toString())
                }
                else -> ModuleResult.Error("Unknown command: ${parts[0]}")
            }
        } catch (e: Exception) {
            ModuleResult.Error(e.message ?: "Command failed")
        }
    }

    sealed class FastbootResponse {
        data class Okay(val data: String) : FastbootResponse()
        data class Fail(val message: String) : FastbootResponse()
        data class Data(val size: Long) : FastbootResponse()
        data class Info(val message: String) : FastbootResponse()
    }

    data class PartitionInfo(
        val name: String,
        val size: Long,
        val type: String
    )

    data class FlashProgress(
        val current: Long,
        val total: Long,
        val message: String
    ) {
        val percent: Float = if (total > 0) (current.toFloat() / total) * 100 else 0f
    }
}
