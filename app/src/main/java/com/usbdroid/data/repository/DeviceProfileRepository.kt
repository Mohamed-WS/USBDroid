package com.usbdroid.data.repository

import com.usbdroid.data.local.dao.DeviceProfileDao
import com.usbdroid.data.model.DeviceProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceProfileRepository @Inject constructor(
    private val dao: DeviceProfileDao
) {
    fun getAllProfiles(): Flow<List<DeviceProfile>> = dao.getAllProfiles()

    suspend fun getProfileForDevice(vid: Int, pid: Int): DeviceProfile? =
        dao.getProfileForDevice(vid, pid)

    fun getProfilesByModule(moduleId: String): Flow<List<DeviceProfile>> =
        dao.getProfilesByModule(moduleId)

    suspend fun saveProfile(profile: DeviceProfile) = dao.insert(profile)

    suspend fun updateProfile(profile: DeviceProfile) = dao.update(profile)

    suspend fun deleteProfile(profile: DeviceProfile) = dao.delete(profile)
}
