package com.usbdroid.data.model

import android.hardware.usb.UsbDevice
import kotlinx.serialization.Serializable

@Serializable
data class USBDeviceInfo(
    val deviceId: Int,
    val vendorId: Int,
    val productId: Int,
    val manufacturerName: String?,
    val productName: String?,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val deviceProtocol: Int,
    val interfaceCount: Int,
    val resolvedManufacturer: String = "Unknown",
    val resolvedProduct: String = "Unknown Device",
    val deviceCategory: DeviceCategory = DeviceCategory.UNKNOWN,
    val confidence: Float = 0.0f,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val serialNumber: String? = null,
    val interfaces: List<USBInterfaceInfo> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (resolvedProduct != "Unknown Device") resolvedProduct
        else productName ?: resolvedProduct

    val displayManufacturer: String
        get() = if (resolvedManufacturer != "Unknown") resolvedManufacturer
        else manufacturerName ?: resolvedManufacturer

    companion object {
        fun fromUsbDevice(device: UsbDevice): USBDeviceInfo {
            val interfaces = mutableListOf<USBInterfaceInfo>()
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                val endpoints = mutableListOf<USBEndpointInfo>()
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    endpoints.add(
                        USBEndpointInfo(
                            address = ep.address,
                            attributes = ep.attributes,
                            direction = ep.direction,
                            type = ep.type,
                            maxPacketSize = ep.maxPacketSize,
                            interval = ep.interval
                        )
                    )
                }
                interfaces.add(
                    USBInterfaceInfo(
                        id = iface.id,
                        interfaceClass = iface.interfaceClass,
                        interfaceSubclass = iface.interfaceSubclass,
                        interfaceProtocol = iface.interfaceProtocol,
                        name = iface.name,
                        endpointCount = iface.endpointCount,
                        endpoints = endpoints
                    )
                )
            }

            return USBDeviceInfo(
                deviceId = device.deviceId,
                vendorId = device.vendorId,
                productId = device.productId,
                manufacturerName = device.manufacturerName,
                productName = device.productName,
                deviceClass = device.deviceClass,
                deviceSubclass = device.deviceSubclass,
                deviceProtocol = device.deviceProtocol,
                interfaceCount = device.interfaceCount,
                serialNumber = device.serialNumber,
                interfaces = interfaces
            )
        }
    }
}

@Serializable
data class USBInterfaceInfo(
    val id: Int,
    val interfaceClass: Int,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val name: String?,
    val endpointCount: Int,
    val endpoints: List<USBEndpointInfo> = emptyList()
)

@Serializable
data class USBEndpointInfo(
    val address: Int,
    val attributes: Int,
    val direction: Int,
    val type: Int,
    val maxPacketSize: Int,
    val interval: Int
)

enum class DeviceCategory {
    ANDROID_ADB,
    ANDROID_FASTBOOT,
    SERIAL_UART,
    HID_INPUT,
    MASS_STORAGE,
    NETWORK,
    FIRMWARE_DFU,
    SMART_CARD,
    AUDIO,
    VIDEO,
    PRINTER,
    HUB,
    VENDOR_SPECIFIC,
    UNKNOWN
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    REQUESTING_PERMISSION,
    PERMISSION_DENIED,
    CONNECTED,
    ERROR
}
