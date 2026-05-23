package com.usbdroid.data.repository

import com.usbdroid.data.local.dao.SessionLogDao
import com.usbdroid.data.model.LogLevel
import com.usbdroid.data.model.SessionLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLogRepository @Inject constructor(
    private val dao: SessionLogDao
) {
    fun getRecentLogs(limit: Int = 1000): Flow<List<SessionLog>> = dao.getRecentLogs(limit)

    fun getLogsByModule(module: String, limit: Int = 500): Flow<List<SessionLog>> =
        dao.getLogsByModule(module, limit)

    fun getLogsByLevel(levels: List<LogLevel>, limit: Int = 500): Flow<List<SessionLog>> =
        dao.getLogsByLevel(levels, limit)

    suspend fun log(moduleName: String, level: LogLevel, message: String, deviceId: Int? = null) {
        dao.insert(
            SessionLog(
                moduleName = moduleName,
                level = level,
                message = message,
                deviceId = deviceId
            )
        )
    }

    suspend fun logVerbose(module: String, message: String, deviceId: Int? = null) =
        log(module, LogLevel.VERBOSE, message, deviceId)

    suspend fun logDebug(module: String, message: String, deviceId: Int? = null) =
        log(module, LogLevel.DEBUG, message, deviceId)

    suspend fun logInfo(module: String, message: String, deviceId: Int? = null) =
        log(module, LogLevel.INFO, message, deviceId)

    suspend fun logWarning(module: String, message: String, deviceId: Int? = null) =
        log(module, LogLevel.WARNING, message, deviceId)

    suspend fun logError(module: String, message: String, deviceId: Int? = null) =
        log(module, LogLevel.ERROR, message, deviceId)

    suspend fun clearOldLogs(olderThan: Long) = dao.deleteOldLogs(olderThan)

    suspend fun clearAll() = dao.clearAll()
}
