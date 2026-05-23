package com.usbdroid.di

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import com.usbdroid.core.detection.USBDetectionEngine
import com.usbdroid.data.local.AppDatabase
import com.usbdroid.data.local.dao.SessionLogDao
import com.usbdroid.data.repository.DeviceProfileRepository
import com.usbdroid.data.repository.SessionLogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager {
        return context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    @Provides
    @Singleton
    fun provideUSBDetectionEngine(
        @ApplicationContext context: Context,
        usbManager: UsbManager
    ): USBDetectionEngine {
        return USBDetectionEngine(context, usbManager)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSessionLogDao(database: AppDatabase): SessionLogDao {
        return database.sessionLogDao()
    }

    @Provides
    @Singleton
    fun provideSessionLogRepository(dao: SessionLogDao): SessionLogRepository {
        return SessionLogRepository(dao)
    }

    @Provides
    @Singleton
    fun provideDeviceProfileRepository(database: AppDatabase): DeviceProfileRepository {
        return DeviceProfileRepository(database.deviceProfileDao())
    }
}
