package com.waktusolat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.waktusolat.app.domain.model.Surah

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey
    val id: Int,
    val nameArabic: String,
    val nameRumi: String,
    val nameTranslation: String,
    val revelationType: String,
    val totalAyat: Int,
    val isBookmarked: Boolean = false,
    val lastReadAyat: Int = 0
) {
    fun toDomain(): Surah = Surah(
        id = id,
        nameArabic = nameArabic,
        nameRumi = nameRumi,
        nameTranslation = nameTranslation,
        revelationType = revelationType,
        totalAyat = totalAyat,
        isBookmarked = isBookmarked,
        lastReadAyat = lastReadAyat
    )

    companion object {
        fun fromDomain(surah: Surah): SurahEntity = SurahEntity(
            id = surah.id,
            nameArabic = surah.nameArabic,
            nameRumi = surah.nameRumi,
            nameTranslation = surah.nameTranslation,
            revelationType = surah.revelationType,
            totalAyat = surah.totalAyat,
            isBookmarked = surah.isBookmarked,
            lastReadAyat = surah.lastReadAyat
        )
    }
}
