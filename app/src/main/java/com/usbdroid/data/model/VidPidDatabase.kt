package com.usbdroid.data.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Offline VID/PID database parsed from usb.ids format.
 * Bundled as raw asset for offline operation.
 */
class VidPidDatabase private constructor() {

    private val vendors = mutableMapOf<Int, Vendor>()

    data class Vendor(
        val id: Int,
        val name: String,
        val products: MutableMap<Int, String> = mutableMapOf()
    )

    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        try {
            context.assets.open("usb.ids").use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                var currentVendor: Vendor? = null
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                    if (!trimmed.startsWith("\t")) {
                        // Vendor line: "VID  VendorName"
                        val spaceIdx = trimmed.indexOf(' ')
                        if (spaceIdx > 0) {
                            try {
                                val vid = trimmed.substring(0, spaceIdx).toInt(16)
                                val name = trimmed.substring(spaceIdx + 1).trim()
                                currentVendor = Vendor(vid, name)
                                vendors[vid] = currentVendor
                            } catch (_: NumberFormatException) {
                                // Skip malformed lines
                            }
                        }
                    } else if (currentVendor != null && trimmed.startsWith("\t") && !trimmed.startsWith("\t\t")) {
                        // Product line: "PID  ProductName"
                        val content = trimmed.substring(1)
                        val spaceIdx = content.indexOf(' ')
                        if (spaceIdx > 0) {
                            try {
                                val pid = content.substring(0, spaceIdx).toInt(16)
                                val name = content.substring(spaceIdx + 1).trim()
                                currentVendor.products[pid] = name
                            } catch (_: NumberFormatException) {
                                // Skip malformed lines
                            }
                        }
                    }
                }
            }
            Timber.i("Loaded ${vendors.size} vendors from usb.ids database")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load usb.ids database")
            // Load built-in fallback data
            loadFallbackData()
        }
    }

    private fun loadFallbackData() {
        // Include critical vendor data as fallback
        val fallbackVendors = mapOf(
            0x18D1 to Vendor(0x18D1, "Google Inc.", mutableMapOf(
                0x4EE0 to "Nexus/Pixel (Fastboot)",
                0x4EE1 to "Nexus/Pixel (PTP)",
                0x4EE2 to "Nexus/Pixel (MTP)",
                0x4EE3 to "Nexus/Pixel (MIDI)",
                0x4EE4 to "Nexus/Pixel (RNDIS)",
                0x4EE5 to "Nexus/Pixel (Audio)",
                0x4EE6 to "Nexus/Pixel (Accessory)",
                0x4EE7 to "Nexus/Pixel (ADB)",
                0xD001 to "Android ADB Device"
            )),
            0x0403 to Vendor(0x0403, "Future Technology Devices International, Ltd", mutableMapOf(
                0x6001 to "FT232 USB-Serial (UART) IC",
                0x6010 to "FT2232C Dual USB-UART/FIFO IC",
                0x6011 to "FT4232H Quad HS USB-UART/FIFO IC",
                0x6014 to "FT232H Single HS USB-UART/FIFO IC",
                0x6015 to "FT231X USB-Serial UART IC"
            )),
            0x1A86 to Vendor(0x1A86, "QinHeng Electronics", mutableMapOf(
                0x7523 to "CH340 serial converter",
                0x7522 to "CH340 serial converter",
                0x5523 to "CH341A serial converter",
                0xE008 to "HL-340 USB-Serial adapter"
            )),
            0x10C4 to Vendor(0x10C4, "Silicon Labs", mutableMapOf(
                0xEA60 to "CP210x UART Bridge",
                0xEA61 to "CP210x UART Bridge",
                0xEA63 to "CP2110 Basic UART",
                0xEA70 to "CP2105 Dual UART Bridge",
                0xEA71 to "CP2108 Quad UART Bridge"
            )),
            0x2341 to Vendor(0x2341, "Arduino SA", mutableMapOf(
                0x0043 to "Uno R3 (CDC ACM)",
                0x0001 to "Serial Adapter (CDC ACM)",
                0x0010 to "Mega 2560 (CDC ACM)",
                0x0036 to "Leo (CDC ACM)",
                0x0037 to "Micro (CDC ACM)",
                0x0042 to "Mega ADK (CDC ACM)",
                0x0044 to "Mega 2560 R3 (CDC ACM)",
                0x8036 to "Leonardo",
                0x8037 to "Micro",
                0x9205 to "Robot Control Board",
                0x9206 to "Robot Motor Board"
            )),
            0x239A to Vendor(0x239A, "Adafruit Industries LLC", mutableMapOf(
                0x800B to "CircuitPlayground Express",
                0x8015 to "Feather M0",
                0x8019 to "Feather M4 Express",
                0x8023 to "ItsyBitsy M0",
                0x802F to "Metro M4 Airlift",
                0x00C5 to "QT Py ESP32-S2",
                0x8119 to "MacroPad RP2040"
            )),
            0x303A to Vendor(0x303A, "Espressif Inc.", mutableMapOf(
                0x0002 to "ESP32 DevKitC",
                0x0003 to "ESP32-S2 DevKit",
                0x0004 to "ESP32-S3 DevKit",
                0x0005 to "ESP32-C3 DevKit",
                0x0006 to "ESP32-C6 DevKit",
                0x0009 to "ESP32-S3 USB OTG",
                0x1001 to "ESP32-C3 USB-JTAG"
            )),
            0x0483 to Vendor(0x0483, "STMicroelectronics", mutableMapOf(
                0x5740 to "STM32 Virtual COM Port",
                0xDF11 to "STM32 BOOTLOADER (DFU)",
                0x3744 to "STLINK-V1",
                0x3748 to "STLINK-V2",
                0x374B to "STLINK-V2.1",
                0x3752 to "STLINK-V3",
                0x374D to "STLINK-V3 PWR",
                0x374E to "STLINK-V3 MINIE",
                0x374F to "STLINK-V3 MODS"
            )),
            0x05AC to Vendor(0x05AC, "Apple, Inc.", mutableMapOf(
                0x12A8 to "iPhone (MTP)",
                0x12AA to "iPhone (PTP)",
                0x12AB to "iPhone (Recovery Mode)",
                0x1281 to "iPhone (DFU Mode)",
                0x1290 to "iPad",
                0x129C to "iPad (Recovery Mode)",
                0x0221 to "Keyboard",
                0x0223 to "Optical Mouse"
            )),
            0x04E8 to Vendor(0x04E8, "Samsung Electronics Co., Ltd", mutableMapOf(
                0x6860 to "Galaxy S Series (MTP)",
                0x685E to "Galaxy S Series (MTP + ADB)",
                0x6866 to "Odin Download Mode"
            )),
            0x067B to Vendor(0x067B, "Prolific Technology, Inc.", mutableMapOf(
                0x2303 to "PL2303 Serial Port",
                0x2304 to "PL2303 Serial Port (H)",
                0x23A3 to "PL2303TA USB Serial"
            )),
            0x045E to Vendor(0x045E, "Microsoft Corp.", mutableMapOf(
                0x028E to "Xbox360 Controller",
                0x02DD to "Xbox One Controller",
                0x0B12 to "Xbox Series X Controller"
            )),
            0x054C to Vendor(0x054C, "Sony Corp.", mutableMapOf(
                0x05C4 to "DualShock 4",
                0x09CC to "DualShock 4 (new)",
                0x0CE6 to "DualSense (PS5)",
                0x0DF2 to "DualSense Edge"
            )),
            0x057E to Vendor(0x057E, "Nintendo Co., Ltd", mutableMapOf(
                0x2009 to "Switch Pro Controller",
                0x200E to "Joy-Con (L)",
                0x200F to "Joy-Con (R)",
                0x2010 to "Joy-Con Charging Grip"
            )),
            0x0B05 to Vendor(0x0B05, "ASUSTek Computer, Inc.", mutableMapOf()),
            0x0FCE to Vendor(0x0FCE, "Sony Ericsson Mobile Communications", mutableMapOf()),
            0x1004 to Vendor(0x1004, "LG Electronics, Inc.", mutableMapOf()),
            0x12D1 to Vendor(0x12D1, "Huawei Technologies Co., Ltd.", mutableMapOf()),
            0x17EF to Vendor(0x17EF, "Lenovo", mutableMapOf()),
            0x1BBB to Vendor(0x1BBB, "T & A Mobile Phones", mutableMapOf()),
            0x1D6B to Vendor(0x1D6B, "Linux Foundation", mutableMapOf(
                0x0001 to "1.1 root hub",
                0x0002 to "2.0 root hub",
                0x0003 to "3.0 root hub"
            )),
            0x1E0E to Vendor(0x1E0E, "Qualcomm / Option", mutableMapOf()),
            0x1FC9 to Vendor(0x1FC9, "NXP Semiconductors", mutableMapOf()),
            0x2A03 to Vendor(0x2A03, "dog hunter AG", mutableMapOf(
                0x0036 to "Arduino Leonardo"
            )),
            0x2E04 to Vendor(0x2E04, "KeepKey LLC", mutableMapOf()),
            0x2E8A to Vendor(0x2E8A, "Raspberry Pi", mutableMapOf(
                0x0003 to "RPi4 CDC UART",
                0x0005 to "Pico (CDC)",
                0x000A to "Pico W (CDC)"
            )),
            0x0D28 to Vendor(0x0D28, "ARM Ltd", mutableMapOf(
                0x0204 to "MBED CMSIS-DAP"
            ))
        )

        vendors.putAll(fallbackVendors)
        Timber.i("Loaded ${vendors.size} vendors from fallback data")
    }

    fun lookupVendor(vid: Int): String? = vendors[vid]?.name

    fun lookupProduct(vid: Int, pid: Int): String? = vendors[vid]?.products?.get(pid)

    fun getVendor(vid: Int): Vendor? = vendors[vid]

    companion object {
        @Volatile
        private var instance: VidPidDatabase? = null

        fun getInstance(context: Context): VidPidDatabase {
            return instance ?: synchronized(this) {
                instance ?: VidPidDatabase().also { db ->
                    // Note: load() should be called from coroutine
                    instance = db
                }
            }
        }

        fun getInstance(): VidPidDatabase {
            return instance ?: VidPidDatabase().also { instance = it }
        }
    }
}
