package com.usbdroid.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_logs")
data class SessionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val moduleName: String,
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: Int? = null
)

@Entity(tableName = "device_profiles")
data class DeviceProfile(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val moduleId: String,
    val configJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)

enum class LogLevel { VERBOSE, DEBUG, INFO, WARNING, ERROR }
