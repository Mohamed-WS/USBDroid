package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.usbdroid.data.model.DeviceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * ADB Controller Module - Implements ADB protocol for Android device control.
 */
class AdbModule : UsbModule {

    override val moduleId: String = "adb"
    override val moduleName: String = "ADB Controller"
    override val moduleDescription: String = "Android Debug Bridge protocol for device control, shell, file transfer, and debugging"
    override val supportedCategories: List<DeviceCategory> = listOf(DeviceCategory.ANDROID_ADB)
    override val supportedVids: List<Int> = listOf(0x18D1)
    override val supportedPids: List<Pair<Int, Int>> = listOf(
        0x18D1 to 0x4EE7,
        0x18D1 to 0xD001,
        0x18D1 to 0x4EE6,
        0x04E8 to 0x685E,
        0x12D1 to 0x1057,
        0x0502 to 0x3604,
        0x0BB4 to 0x0C03,
        0x2A45 to 0x8A11
    )
    override val isHardwareModule: Boolean = true

    private val _connectionState = MutableStateFlow<ModuleConnectionState>(ModuleConnectionState.Disconnected)
    private val _shellOutput = MutableStateFlow("")
    private val _logcatOutput = MutableStateFlow("")
    private val _deviceInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _packages = MutableStateFlow<List<String>>(emptyList())
    private val _batteryInfo = MutableStateFlow<Map<String, String>>(emptyMap())

    val shellOutput: StateFlow<String> = _shellOutput
    val logcatOutput: StateFlow<String> = _logcatOutput
    val deviceInfo: StateFlow<Map<String, String>> = _deviceInfo
    val packages: StateFlow<List<String>> = _packages
    val batteryInfo: StateFlow<Map<String, String>> = _batteryInfo

    private var connection: UsbDeviceConnection? = null
    private var currentDevice: UsbDevice? = null

    // ADB Protocol Constants
    companion object {
        const val ADB_CLASS = 0xFF
        const val ADB_SUBCLASS = 0x42
        const val ADB_PROTOCOL = 0x01

        // ADB Messages
        const val A_SYNC = 0x434E5953
        const val A_CNXN = 0x4E584E43
        const val A_AUTH = 0x48545541
        const val A_OPEN = 0x4E45504F
        const val A_OKAY = 0x59414B4F
        const val A_CLSE = 0x45534C43
        const val A_WRTE = 0x45545257

        const val ADB_VERSION = 0x01000000
        const val MAX_PAYLOAD = 4096
        const val ADB_TOKEN_SIZE = 20

        // Authentication types
        const val ADB_AUTH_TOKEN = 1
        const val ADB_AUTH_SIGNATURE = 2
        const val ADB_AUTH_RSAPUBLICKEY = 3

        // Service strings
        const val SHELL_SERVICE = "shell:\u0000"
        const val SYNC_SERVICE = "sync:\u0000"
        const val LOGCAT_SERVICE = "shell:logcat\u0000"
        const val DEVICES_SERVICE = "host:devices\u0000"
    }

    override fun canHandle(device: UsbDevice): Boolean {
        // Check VID/PID
        if (device.vendorId == 0x18D1 && device.productId in listOf(0x4EE7, 0xD001, 0x4EE6)) return true
        // Check ADB interface class
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == ADB_CLASS &&
                iface.interfaceSubclass == ADB_SUBCLASS &&
                iface.interfaceProtocol == ADB_PROTOCOL) {
                return true
            }
        }
        // Check other known ADB VIDs
        return device.vendorId in supportedVids ||
                supportedPids.any { it.first == device.vendorId && it.second == device.productId }
    }

    override fun canHandleCategory(category: DeviceCategory): Boolean = category == DeviceCategory.ANDROID_ADB

    override fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean {
        return try {
            _connectionState.value = ModuleConnectionState.Connecting
            this.connection = connection
            this.currentDevice = device

            // Find ADB interface
            var adbInterfaceIndex = -1
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == ADB_CLASS &&
                    iface.interfaceSubclass == ADB_SUBCLASS) {
                    adbInterfaceIndex = i
                    break
                }
            }

            if (adbInterfaceIndex < 0) {
                _connectionState.value = ModuleConnectionState.Error("No ADB interface found")
                return false
            }

            val iface = device.getInterface(adbInterfaceIndex)
            connection?.claimInterface(iface, true)

            // Perform ADB handshake
            performAdbHandshake()

            _connectionState.value = ModuleConnectionState.Connected(device.productName ?: "Android Device")
            true
        } catch (e: Exception) {
            Timber.e(e, "ADB attach failed")
            _connectionState.value = ModuleConnectionState.Error(e.message ?: "Failed to connect")
            false
        }
    }

    private fun performAdbHandshake() {
        // Send ADB CNXN message
        val systemIdentity = "host::\u0000".toByteArray()
        sendAdbMessage(A_CNXN, ADB_VERSION, MAX_PAYLOAD, systemIdentity)

        // Expect AUTH message with token
        // Sign token and send AUTH with signature
        // Then expect OKAY

        // This is a simplified implementation
        // Full implementation would include RSA key signing
    }

    private fun sendAdbMessage(command: Int, arg0: Int, arg1: Int, payload: ByteArray = ByteArray(0)) {
        val msg = ByteArray(24 + payload.size)
        // Write command (little-endian)
        msg[0] = (command and 0xFF).toByte()
        msg[1] = ((command shr 8) and 0xFF).toByte()
        msg[2] = ((command shr 16) and 0xFF).toByte()
        msg[3] = ((command shr 24) and 0xFF).toByte()
        // Write arg0
        msg[4] = (arg0 and 0xFF).toByte()
        msg[5] = ((arg0 shr 8) and 0xFF).toByte()
        msg[6] = ((arg0 shr 16) and 0xFF).toByte()
        msg[7] = ((arg0 shr 24) and 0xFF).toByte()
        // Write arg1
        msg[8] = (arg1 and 0xFF).toByte()
        msg[9] = ((arg1 shr 8) and 0xFF).toByte()
        msg[10] = ((arg1 shr 16) and 0xFF).toByte()
        msg[11] = ((arg1 shr 24) and 0xFF).toByte()
        // Write data length
        msg[12] = (payload.size and 0xFF).toByte()
        msg[13] = ((payload.size shr 8) and 0xFF).toByte()
        msg[14] = ((payload.size shr 16) and 0xFF).toByte()
        msg[15] = ((payload.size shr 24) and 0xFF).toByte()
        // Write checksum
        val checksum = payload.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) }
        msg[16] = (checksum and 0xFF).toByte()
        msg[17] = ((checksum shr 8) and 0xFF).toByte()
        msg[18] = ((checksum shr 16) and 0xFF).toByte()
        msg[19] = ((checksum shr 24) and 0xFF).toByte()
        // Write magic
        val magic = command xor 0xFFFFFFFF.toInt()
        msg[20] = (magic and 0xFF).toByte()
        msg[21] = ((magic shr 8) and 0xFF).toByte()
        msg[22] = ((magic shr 16) and 0xFF).toByte()
        msg[23] = ((magic shr 24) and 0xFF).toByte()
        // Copy payload
        payload.copyInto(msg, 24)

        // Send via bulk transfer
        val iface = currentDevice?.getInterface(0)
        val endpoint = iface?.getEndpoint(1) // Usually OUT endpoint
        connection?.bulkTransfer(endpoint, msg, msg.size, 5000)
    }

    fun executeShell(command: String) {
        // Send A_OPEN with shell service
        val serviceBytes = "shell:$command\u0000".toByteArray()
        sendAdbMessage(A_OPEN, 1, 0, serviceBytes)
    }

    fun startLogcat(filter: String = "") {
        val service = if (filter.isNotEmpty()) {
            "shell:logcat -s $filter\u0000"
        } else {
            LOGCAT_SERVICE
        }
        sendAdbMessage(A_OPEN, 2, 0, service.toByteArray())
    }

    fun pushFile(localPath: String, remotePath: String) {
        // SYNC protocol implementation for file push
        sendAdbMessage(A_OPEN, 3, 0, SYNC_SERVICE.toByteArray())
        // Then send SYNC commands (SEND, DATA, DONE)
    }

    fun pullFile(remotePath: String, localPath: String) {
        sendAdbMessage(A_OPEN, 4, 0, SYNC_SERVICE.toByteArray())
        // Then send RECV command
    }

    fun installApk(apkPath: String) {
        val cmd = "shell:pm install -r $apkPath\u0000"
        sendAdbMessage(A_OPEN, 5, 0, cmd.toByteArray())
    }

    fun uninstallPackage(packageName: String) {
        val cmd = "shell:pm uninstall $packageName\u0000"
        sendAdbMessage(A_OPEN, 6, 0, cmd.toByteArray())
    }

    fun reboot(target: String = "") {
        val cmd = if (target.isNotEmpty()) {
            "shell:reboot $target\u0000"
        } else {
            "shell:reboot\u0000"
        }
        sendAdbMessage(A_OPEN, 7, 0, cmd.toByteArray())
    }

    fun forwardPort(local: Int, remote: Int) {
        val cmd = "shell:forward tcp:$local tcp:$remote\u0000"
        sendAdbMessage(A_OPEN, 8, 0, cmd.toByteArray())
    }

    fun getDeviceInfo() {
        executeShell("getprop")
    }

    fun getBatteryInfo() {
        executeShell("dumpsys battery")
    }

    fun getPackages() {
        executeShell("pm list packages")
    }

    fun takeScreenshot(): ByteArray? {
        executeShell("screencap -p")
        // Read response data
        return null
    }

    override fun detach() {
        connection?.let { conn ->
            currentDevice?.let { device ->
                for (i in 0 until device.interfaceCount) {
                    conn.releaseInterface(device.getInterface(i))
                }
            }
        }
        connection = null
        currentDevice = null
        _connectionState.value = ModuleConnectionState.Disconnected
    }

    override fun isAttached(): Boolean = _connectionState.value is ModuleConnectionState.Connected

    override fun getConnectionState(): StateFlow<ModuleConnectionState> = _connectionState

    override suspend fun executeCommand(command: String): ModuleResult {
        return try {
            when {
                command.startsWith("shell:") -> {
                    executeShell(command.removePrefix("shell:"))
                    ModuleResult.Success
                }
                command == "getprop" -> {
                    getDeviceInfo()
                    ModuleResult.Success
                }
                command == "packages" -> {
                    getPackages()
                    ModuleResult.Success
                }
                command == "battery" -> {
                    getBatteryInfo()
                    ModuleResult.Success
                }
                command.startsWith("reboot") -> {
                    val target = command.removePrefix("reboot").trim()
                    reboot(target)
                    ModuleResult.Success
                }
                else -> {
                    executeShell(command)
                    ModuleResult.Success
                }
            }
        } catch (e: Exception) {
            ModuleResult.Error(e.message ?: "Command failed")
        }
    }

    data class AdbDeviceInfo(
        val model: String,
        val androidVersion: String,
        val buildNumber: String,
        val sdkVersion: String,
        val serialNumber: String,
        val manufacturer: String
    )
}
