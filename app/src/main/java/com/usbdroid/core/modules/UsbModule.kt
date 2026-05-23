package com.usbdroid.core.modules

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.usbdroid.data.model.DeviceCategory
import kotlinx.coroutines.flow.StateFlow

/**
 * Base interface that all control modules must implement.
 * Each module is self-contained and manages its own USB lifecycle.
 */
interface UsbModule {
    val moduleId: String
    val moduleName: String
    val moduleDescription: String
    val supportedCategories: List<DeviceCategory>
    val supportedVids: List<Int>
    val supportedPids: List<Pair<Int, Int>>
    val isHardwareModule: Boolean

    /**
     * Check if this module can handle the given device.
     */
    fun canHandle(device: UsbDevice): Boolean

    /**
     * Check if this module can handle the given device category.
     */
    fun canHandleCategory(category: DeviceCategory): Boolean

    /**
     * Attach to a USB device. The connection is managed externally.
     */
    fun attach(device: UsbDevice, connection: UsbDeviceConnection?): Boolean

    /**
     * Detach from the current device and cleanup resources.
     */
    fun detach()

    /**
     * Whether this module is currently attached to a device.
     */
    fun isAttached(): Boolean

    /**
     * Get the current connection state.
     */
    fun getConnectionState(): StateFlow<ModuleConnectionState>

    /**
     * Execute a command within this module.
     */
    suspend fun executeCommand(command: String): ModuleResult
}

sealed class ModuleConnectionState {
    object Disconnected : ModuleConnectionState()
    object Connecting : ModuleConnectionState()
    data class Connected(val deviceName: String) : ModuleConnectionState()
    data class Error(val message: String) : ModuleConnectionState()
}

sealed class ModuleResult {
    object Success : ModuleResult()
    data class SuccessWithData(val data: String) : ModuleResult()
    data class Error(val message: String) : ModuleResult()
    data class Progress(val current: Long, val total: Long, val message: String) : ModuleResult()
}

/**
 * Registry of all available modules.
 */
object ModuleRegistry {
    private val modules = mutableMapOf<String, () -> UsbModule>()

    fun register(moduleId: String, factory: () -> UsbModule) {
        modules[moduleId] = factory
    }

    fun createModule(moduleId: String): UsbModule? = modules[moduleId]?.invoke()

    fun getAllModuleIds(): List<String> = modules.keys.toList()

    fun findModuleForDevice(device: UsbDevice): String? {
        // Return first matching module
        modules.forEach { (id, factory) ->
            val module = factory()
            if (module.canHandle(device)) return id
        }
        return null
    }

    fun findModuleForCategory(category: DeviceCategory): String? {
        modules.forEach { (id, factory) ->
            val module = factory()
            if (module.canHandleCategory(category)) return id
        }
        return "analyzer" // fallback
    }
}
