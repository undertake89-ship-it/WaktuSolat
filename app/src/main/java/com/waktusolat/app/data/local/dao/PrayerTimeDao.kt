package com.waktusolat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.waktusolat.app.data.local.entity.PrayerTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimeDao {
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): PrayerTimeEntity?

    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    fun getByDateFlow(date: String): Flow<PrayerTimeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayerTime: PrayerTimeEntity)

    @Query("SELECT * FROM prayer_times ORDER BY date DESC LIMIT 7")
    suspend fun getLastWeek(): List<PrayerTimeEntity>

    @Query("SELECT * FROM prayer_times ORDER BY date DESC LIMIT 7")
    fun getLastWeekFlow(): Flow<List<PrayerTimeEntity>>

    @Query("DELETE FROM prayer_times WHERE date < :date")
    suspend fun deleteOlderThan(date: String)
}
