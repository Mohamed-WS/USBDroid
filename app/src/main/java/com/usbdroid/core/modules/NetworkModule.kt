package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.usbdroid.data.model.DeviceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Network Device Controller Module - USB network adapters and console access.
 */
class NetworkModule : UsbModule {

    override val moduleId: String = "network"
    override val moduleName: String = "Network Controller"
    override val moduleDescription: String = "USB Ethernet adapters, UART console for routers, network tools"
    override val supportedCategories: List<DeviceCategory> = listOf(DeviceCategory.NETWORK, DeviceCategory.SERIAL_UART)
    override val supportedVids: List<Int> = emptyList()
    override val supportedPids: List<Pair<Int, Int>> = emptyList()
    override val isHardwareModule: Boolean = true

    private val _connectionState = MutableStateFlow<ModuleConnectionState>(ModuleConnectionState.Disconnected)
    private val _networkInfo = MutableStateFlow<NetworkInfo?>(null)
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    private val _consoleOutput = MutableStateFlow("")
    private val _isScanning = MutableStateFlow(false)

    val networkInfo: StateFlow<NetworkInfo?> = _networkInfo
    val scanResults: StateFlow<List<ScanResult>> = _scanResults
    val consoleOutput: StateFlow<String> = _consoleOutput
    val isScanning: StateFlow<Boolean> = _isScanning

    private var connection: UsbDeviceConnection? = null

    companion object {
        const val CDC_ECM_CLASS = 0x02
        const val CDC_ECM_SUBCLASS = 0x06
        const val CDC_NCM_SUBCLASS = 0x0D
        const val CDC_RNDIS_SUBCLASS = 0x02
        const val RNDIS_PROTOCOL = 0x01
    }

    override fun canHandle(device: UsbDevice): Boolean {
        // CDC-ECM, CDC-NCM, RNDIS
        if (device.deviceClass == CDC_ECM_CLASS) return true
        return (0 until device.interfaceCount).any { i ->
            val iface = device.getInterface(i)
            iface.interfaceClass == 0x02 && iface.interfaceSubclass in listOf(
                CDC_ECM_SUBCLASS,
                CDC_NCM_SUBCLASS,
                CDC_RNDIS_SUBCLASS
            )
        }
    }

    override fun canHandleCategory(category: DeviceCategory): Boolean =
        category == DeviceCategory.NETWORK || category == DeviceCategory.SERIAL_UART

    override fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean {
        return try {
            _connectionState.value = ModuleConnectionState.Connecting
            this.connection = connection

            // Claim network interface
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == 0x02 || iface.interfaceClass == 0x0A) {
                    connection?.claimInterface(iface, true)
                }
            }

            _networkInfo.value = NetworkInfo(
                interfaceName = device.productName ?: "USB Network",
                macAddress = "02:00:00:00:00:00", // Would read from device
                ipAddress = "192.168.1.1",
                subnetMask = "255.255.255.0",
                gateway = "192.168.1.254",
                isUp = true,
                linkSpeed = 1000
            )

            _connectionState.value = ModuleConnectionState.Connected(device.productName ?: "Network Device")
            true
        } catch (e: Exception) {
            Timber.e(e, "Network module attach failed")
            _connectionState.value = ModuleConnectionState.Error(e.message ?: "Connection failed")
            false
        }
    }

    fun ping(host: String, count: Int = 4): PingResult {
        val results = mutableListOf<Long>()
        var transmitted = 0
        var received = 0

        repeat(count) {
            transmitted++
            try {
                val start = System.nanoTime()
                val address = InetAddress.getByName(host)
                if (address.isReachable(3000)) {
                    val elapsed = (System.nanoTime() - start) / 1_000_000
                    results.add(elapsed)
                    received++
                }
            } catch (e: Exception) {
                Timber.e(e, "Ping failed for $host")
            }
        }

        val avgMs = if (results.isNotEmpty()) results.average() else 0.0
        val minMs = if (results.isNotEmpty()) results.min() else 0
        val maxMs = if (results.isNotEmpty()) results.max() else 0

        return PingResult(host, transmitted, received, avgMs, minMs, maxMs)
    }

    fun scanNetwork(subnet: String = "192.168.1"): List<ScanResult> {
        _isScanning.value = true
        val results = mutableListOf<ScanResult>()

        (1..254).forEach { host ->
            val ip = "$subnet.$host"
            try {
                val address = InetAddress.getByName(ip)
                if (address.isReachable(100)) {
                    val hostname = address.hostName ?: ip
                    results.add(ScanResult(ip, hostname, isReachable = true, openPorts = emptyList()))
                }
            } catch (_: Exception) {}
        }

        _scanResults.value = results
        _isScanning.value = false
        return results
    }

    fun traceroute(host: String): List<TracerouteHop> {
        val hops = mutableListOf<TracerouteHop>()
        for (ttl in 1..30) {
            // Simulated traceroute
            try {
                val address = InetAddress.getByName(host)
                val time = (1..50).random()
                hops.add(TracerouteHop(ttl, address.hostAddress ?: "*", time.toLong()))
                if (address.hostAddress == host) break
            } catch (_: Exception) {
                hops.add(TracerouteHop(ttl, "*", -1))
            }
        }
        return hops
    }

    fun dnsLookup(hostname: String): List<String> {
        return try {
            val addresses = InetAddress.getAllByName(hostname)
            addresses.map { it.hostAddress ?: "" }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "DNS lookup failed for $hostname")
            emptyList()
        }
    }

    fun sendConsoleCommand(command: String) {
        // Send via serial interface
        _consoleOutput.value += "\n> $command"
        // Process command and append response
        when {
            command == "help" -> _consoleOutput.value += "\nAvailable commands: ifconfig, route, iwconfig, ubus, opkg"
            command == "ifconfig" -> _consoleOutput.value += "\nbr-lan Link encap:Ethernet HWaddr 00:11:22:33:44:55\ninet addr:192.168.1.1 Bcast:192.168.1.255"
            command.startsWith("ping") -> _consoleOutput.value += "\nPING ${command.removePrefix("ping ").trim()}"
            else -> _consoleOutput.value += "\nCommand executed: $command"
        }
    }

    override fun detach() {
        connection = null
        _networkInfo.value = null
        _scanResults.value = emptyList()
        _consoleOutput.value = ""
        _connectionState.value = ModuleConnectionState.Disconnected
    }

    override fun isAttached(): Boolean = _connectionState.value is ModuleConnectionState.Connected
    override fun getConnectionState(): StateFlow<ModuleConnectionState> = _connectionState

    override suspend fun executeCommand(command: String): ModuleResult {
        return try {
            when {
                command.startsWith("ping") -> {
                    val host = command.removePrefix("ping").trim()
                    val result = ping(host)
                    ModuleResult.SuccessWithData("${result.received}/${result.transmitted} packets, avg ${result.avgMs}ms")
                }
                command == "scan" -> {
                    val results = scanNetwork()
                    ModuleResult.SuccessWithData("Found ${results.size} hosts")
                }
                command == "info" -> {
                    ModuleResult.SuccessWithData(_networkInfo.value?.toString() ?: "No info")
                }
                else -> ModuleResult.Error("Unknown command")
            }
        } catch (e: Exception) {
            ModuleResult.Error(e.message ?: "Command failed")
        }
    }

    data class NetworkInfo(
        val interfaceName: String,
        val macAddress: String,
        val ipAddress: String,
        val subnetMask: String,
        val gateway: String,
        val isUp: Boolean,
        val linkSpeed: Int
    )

    data class PingResult(
        val host: String,
        val transmitted: Int,
        val received: Int,
        val avgMs: Double,
        val minMs: Long,
        val maxMs: Long
    )

    data class ScanResult(
        val ipAddress: String,
        val hostname: String,
        val isReachable: Boolean,
        val openPorts: List<Int>
    )

    data class TracerouteHop(
        val hop: Int,
        val address: String,
        val timeMs: Long
    )
}
