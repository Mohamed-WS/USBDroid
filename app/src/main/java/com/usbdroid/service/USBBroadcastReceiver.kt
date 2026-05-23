package com.usbdroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.usbdroid.MainActivity
import timber.log.Timber

/**
 * Broadcast receiver for USB device attach/detach events.
 * These events are received even when the app is not in the foreground.
 */
class USBBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_USB_PERMISSION = "com.usbdroid.USB_PERMISSION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Timber.i("USB Broadcast: Device attached - ${it.deviceName} (${"%04X:%04X".format(it.vendorId, it.productId)})")
                    // Launch MainActivity to handle the device
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        action = UsbManager.ACTION_USB_DEVICE_ATTACHED
                        putExtra(UsbManager.EXTRA_DEVICE, it)
                    }
                    context.startActivity(launchIntent)
                }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Timber.i("USB Broadcast: Device detached - ${it.deviceName}")
                }
            }
            ACTION_USB_PERMISSION -> {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                device?.let {
                    Timber.i("USB Broadcast: Permission ${if (granted) "granted" else "denied"} for ${it.deviceName}")
                }
            }
        }
    }
}
