package com.waktusolat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.waktusolat.app.data.local.entity.SurahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM surahs ORDER BY id ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE id = :id LIMIT 1")
    suspend fun getSurahById(id: Int): SurahEntity?

    @Query("SELECT * FROM surahs WHERE id = :id LIMIT 1")
    fun getSurahByIdFlow(id: Int): Flow<SurahEntity?>

    @Query("SELECT * FROM surahs WHERE nameRumi LIKE '%' || :query || '%' OR nameTranslation LIKE '%' || :query || '%' ORDER BY id ASC")
    fun searchSurahs(query: String): Flow<List<SurahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surahs: List<SurahEntity>)

    @Query("UPDATE surahs SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, isBookmarked: Boolean)

    @Query("UPDATE surahs SET lastReadAyat = :ayatNumber WHERE id = :surahId")
    suspend fun updateLastRead(surahId: Int, ayatNumber: Int)

    @Query("SELECT * FROM surahs WHERE isBookmarked = 1 ORDER BY id ASC")
    fun getBookmarkedSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun getCount(): Int
}
