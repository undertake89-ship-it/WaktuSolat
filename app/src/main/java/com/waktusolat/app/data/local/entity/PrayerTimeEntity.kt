package com.waktusolat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.waktusolat.app.domain.model.PrayerTime

@Entity(tableName = "prayer_times")
data class PrayerTimeEntity(
    @PrimaryKey
    val date: String,
    val subuh: String,
    val syuruk: String,
    val zohor: String,
    val asar: String,
    val maghrib: String,
    val isyak: String,
    val zone: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain(): PrayerTime = PrayerTime(
        date = date,
        subuh = subuh,
        syuruk = syuruk,
        zohor = zohor,
        asar = asar,
        maghrib = maghrib,
        isyak = isyak,
        zone = zone
    )

    companion object {
        fun fromDomain(prayerTime: PrayerTime): PrayerTimeEntity = PrayerTimeEntity(
            date = prayerTime.date,
            subuh = prayerTime.subuh,
            syuruk = prayerTime.syuruk,
            zohor = prayerTime.zohor,
            asar = prayerTime.asar,
            maghrib = prayerTime.maghrib,
            isyak = prayerTime.isyak,
            zone = prayerTime.zone
        )
    }
}
