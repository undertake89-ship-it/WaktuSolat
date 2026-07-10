package com.waktusolat.app.domain.model

data class PrayerTime(
    val date: String,
    val subuh: String,
    val syuruk: String,
    val zohor: String,
    val asar: String,
    val maghrib: String,
    val isyak: String,
    val zone: String = ""
)

data class NextPrayer(
    val name: String,
    val time: String,
    val remainingMillis: Long
)
