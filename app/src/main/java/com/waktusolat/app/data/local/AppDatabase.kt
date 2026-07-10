package com.waktusolat.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.waktusolat.app.data.local.dao.PrayerTimeDao
import com.waktusolat.app.data.local.dao.QuranDao
import com.waktusolat.app.data.local.entity.PrayerTimeEntity
import com.waktusolat.app.data.local.entity.SurahEntity

@Database(
    entities = [PrayerTimeEntity::class, SurahEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "waktu_solat_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
