package com.usbdroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.usbdroid.data.local.dao.DeviceProfileDao
import com.usbdroid.data.local.dao.SessionLogDao
import com.usbdroid.data.model.DeviceProfile
import com.usbdroid.data.model.LogLevel
import com.usbdroid.data.model.SessionLog

@Database(
    entities = [SessionLog::class, DeviceProfile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionLogDao(): SessionLogDao
    abstract fun deviceProfileDao(): DeviceProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "usbdroid_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

class Converters {
    @TypeConverter
    fun fromLogLevel(value: LogLevel): String = value.name

    @TypeConverter
    fun toLogLevel(value: String): LogLevel = LogLevel.valueOf(value)
}
