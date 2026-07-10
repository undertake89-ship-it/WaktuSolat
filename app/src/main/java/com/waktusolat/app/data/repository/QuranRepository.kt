package com.waktusolat.app.data.repository

import com.waktusolat.app.data.local.dao.QuranDao
import com.waktusolat.app.data.local.entity.SurahEntity
import com.waktusolat.app.data.remote.RetrofitClient
import com.waktusolat.app.domain.model.Ayat
import com.waktusolat.app.domain.model.Surah
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuranRepository(
    private val quranDao: QuranDao
) {
    private val api = RetrofitClient.quranApi

    fun getAllSurahs(): Flow<List<Surah>> {
        return quranDao.getAllSurahs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchSurahs(query: String): Flow<List<Surah>> {
        return quranDao.searchSurahs(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getBookmarkedSurahs(): Flow<List<Surah>> {
        return quranDao.getBookmarkedSurahs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getSurahById(id: Int): Surah? {
        return quranDao.getSurahById(id)?.toDomain()
    }

    suspend fun toggleBookmark(surahId: Int, currentState: Boolean) {
        quranDao.updateBookmark(surahId, !currentState)
    }

    suspend fun updateLastRead(surahId: Int, ayatNumber: Int) {
        quranDao.updateLastRead(surahId, ayatNumber)
    }

    suspend fun refreshSurahs(): Result<Unit> {
        return try {
            val count = quranDao.getCount()
            if (count > 0) return Result.success(Unit)

            val response = api.getAllSurahs()
            if (response.code == 200) {
                val entities = response.data.map { data ->
                    SurahEntity(
                        id = data.number,
                        nameArabic = data.name,
                        nameRumi = data.englishName,
                        nameTranslation = data.englishNameTranslation,
                        revelationType = data.revelationType,
                        totalAyat = data.numberOfAyahs
                    )
                }
                quranDao.insertAll(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch surah list"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAyatList(surahId: Int, edition: String = "ar.alafasy"): Result<List<Ayat>> {
        return try {
            val response = api.getSurahEdition(surahId, edition)
            if (response.code == 200) {
                val ayahs = response.data.ayahs.map { ayatData ->
                    Ayat(
                        surahId = surahId,
                        ayatNumber = ayatData.numberInSurah,
                        textArabic = ayatData.text,
                        textTranslation = "",
                        audioUrl = ayatData.audio ?: ""
                    )
                }
                Result.success(ayahs)
            } else {
                Result.failure(Exception("Failed to fetch ayahs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAyatTranslation(surahId: Int, edition: String = "ms.basmeih"): Result<Map<Int, String>> {
        return try {
            val response = api.getSurahEdition(surahId, edition)
            if (response.code == 200) {
                val translationMap = response.data.ayahs.associate { it.numberInSurah to it.text }
                Result.success(translationMap)
            } else {
                Result.failure(Exception("Failed to fetch translation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
