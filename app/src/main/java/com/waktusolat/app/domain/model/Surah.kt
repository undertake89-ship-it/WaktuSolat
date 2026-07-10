package com.waktusolat.app.domain.model

data class Surah(
    val id: Int,
    val nameArabic: String,
    val nameRumi: String,
    val nameTranslation: String,
    val revelationType: String,
    val totalAyat: Int,
    val isBookmarked: Boolean = false,
    val lastReadAyat: Int = 0
)

data class Ayat(
    val surahId: Int,
    val ayatNumber: Int,
    val textArabic: String,
    val textTranslation: String,
    val audioUrl: String = ""
)

data class Bookmark(
    val surahId: Int,
    val surahName: String,
    val ayatNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)
