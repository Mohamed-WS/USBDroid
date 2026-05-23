package com.usbdroid

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class USBDroidApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Initialize module registry
        initializeModules()
    }

    private fun initializeModules() {
        com.usbdroid.core.modules.ModuleRegistry.register("adb") { com.usbdroid.core.modules.AdbModule() }
        com.usbdroid.core.modules.ModuleRegistry.register("fastboot") { com.usbdroid.core.modules.FastbootModule() }
        com.usbdroid.core.modules.ModuleRegistry.register("serial") { com.usbdroid.core.modules.SerialModule() }
        com.usbdroid.core.modules.ModuleRegistry.register("flasher") { com.usbdroid.core.modules.FlasherModule() }
        com.usbdroid.core.modules.ModuleRegistry.register("hid") { com.usbdroid.core.modules.HidModule() }
        com.usbdroid.core.modules.ModuleRegistry.register("storage") { com.usbdroid.core.modules.StorageModule() }
        com.usbdroid.core.modules.ModuleRegistry.register("network") { com.usbdroid.core.modules.NetworkModule() }
        com.usbdroid.core.modules.ModuleRegistry.register("analyzer") { com.usbdroid.core.modules.AnalyzerModule() }
    }
}
