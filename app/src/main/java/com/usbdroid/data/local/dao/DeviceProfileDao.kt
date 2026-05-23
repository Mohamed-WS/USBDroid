package com.usbdroid.data.local.dao

import androidx.room.*
import com.usbdroid.data.model.DeviceProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceProfileDao {

    @Query("SELECT * FROM device_profiles ORDER BY lastUsed DESC")
    fun getAllProfiles(): Flow<List<DeviceProfile>>

    @Query("SELECT * FROM device_profiles WHERE vendorId = :vid AND productId = :pid LIMIT 1")
    suspend fun getProfileForDevice(vid: Int, pid: Int): DeviceProfile?

    @Query("SELECT * FROM device_profiles WHERE moduleId = :moduleId")
    fun getProfilesByModule(moduleId: String): Flow<List<DeviceProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: DeviceProfile)

    @Update
    suspend fun update(profile: DeviceProfile)

    @Delete
    suspend fun delete(profile: DeviceProfile)

    @Query("DELETE FROM device_profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}
