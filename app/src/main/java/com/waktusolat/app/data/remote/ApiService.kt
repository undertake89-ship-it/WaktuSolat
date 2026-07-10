package com.waktusolat.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ===================== Prayer API (Aladhan) =====================
interface PrayerApiService {
    @GET("timings/{date}")
    suspend fun getPrayerTimes(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 1
    ): PrayerTimingsResponse

    @GET("timingsByCity/{date}")
    suspend fun getPrayerTimesByCity(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String = "Malaysia",
        @Query("method") method: Int = 1
    ): PrayerTimingsResponse
}

data class PrayerTimingsResponse(
    val code: Int,
    val status: String,
    val data: PrayerTimingsData
)

data class PrayerTimingsData(
    val timings: Map<String, String>,
    val date: DateInfo,
    val meta: MetaInfo
)

data class DateInfo(
    val readable: String,
    val gregorian: GregorianDate,
    val hijri: HijriDate
)

data class GregorianDate(val date: String)
data class HijriDate(val date: String, val day: String, val month: MonthInfo, val year: String)
data class MonthInfo(val number: Int, val en: String, val ar: String)
data class MetaInfo(val timezone: String)

// ===================== Quran API (Alquran.cloud) =====================
interface QuranApiService {
    @GET("surah")
    suspend fun getAllSurahs(): SurahListResponse

    @GET("surah/{number}")
    suspend fun getSurahDetail(
        @Path("number") number: Int
    ): SurahDetailResponse

    @GET("surah/{number}/editions/{editions}")
    suspend fun getSurahWithEditions(
        @Path("number") number: Int,
        @Path("editions") editions: String
    ): SurahDetailResponse

    @GET("surah/{number}/{edition}")
    suspend fun getSurahEdition(
        @Path("number") number: Int,
        @Path("edition") edition: String
    ): SurahEditionResponse
}

data class SurahListResponse(
    val code: Int,
    val status: String,
    val data: List<SurahData>
)

data class SurahData(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfAyahs: Int
)

data class SurahDetailResponse(
    val code: Int,
    val status: String,
    val data: SurahDetailData
)

data class SurahDetailData(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val ayahs: List<AyatData>
)

data class SurahEditionResponse(
    val code: Int,
    val status: String,
    val data: SurahEditionData
)

data class SurahEditionData(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val edition: EditionInfo,
    val ayahs: List<AyatData>
)

data class EditionInfo(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val format: String,
    val type: String
)

data class AyatData(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int,
    val audio: String? = null,
    val audioSecondary: List<String>? = null
)
