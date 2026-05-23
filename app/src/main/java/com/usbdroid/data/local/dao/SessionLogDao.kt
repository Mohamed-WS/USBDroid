package com.usbdroid.data.local.dao

import androidx.room.*
import com.usbdroid.data.model.LogLevel
import com.usbdroid.data.model.SessionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {

    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 1000): Flow<List<SessionLog>>

    @Query("SELECT * FROM session_logs WHERE moduleName = :module ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByModule(module: String, limit: Int = 500): Flow<List<SessionLog>>

    @Query("SELECT * FROM session_logs WHERE level IN (:levels) ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByLevel(levels: List<LogLevel>, limit: Int = 500): Flow<List<SessionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SessionLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SessionLog>)

    @Query("DELETE FROM session_logs WHERE timestamp < :olderThan")
    suspend fun deleteOldLogs(olderThan: Long)

    @Query("DELETE FROM session_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM session_logs")
    suspend fun getLogCount(): Int
}
