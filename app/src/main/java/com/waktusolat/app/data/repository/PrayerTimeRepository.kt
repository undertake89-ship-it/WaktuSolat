package com.waktusolat.app.data.repository

import com.waktusolat.app.data.local.dao.PrayerTimeDao
import com.waktusolat.app.data.local.entity.PrayerTimeEntity
import com.waktusolat.app.data.remote.RetrofitClient
import com.waktusolat.app.domain.model.NextPrayer
import com.waktusolat.app.domain.model.PrayerTime
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerTimeRepository(
    private val prayerTimeDao: PrayerTimeDao
) {
    private val api = RetrofitClient.prayerApi
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    companion object {
        private val prayerOrder = listOf("Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak")
    }

    suspend fun getPrayerTimes(lat: Double, lng: Double): Result<PrayerTime> {
        return try {
            val today = dateFormat.format(Date())
            val local = prayerTimeDao.getByDate(today)
            if (local != null) {
                return Result.success(local.toDomain())
            }

            val response = api.getPrayerTimes(today, lat, lng)
            if (response.code == 200 && response.status == "OK") {
                val timings = response.data.timings
                val gregorianDate = response.data.date.gregorian.date

                val prayerTime = PrayerTime(
                    date = gregorianDate,
                    subuh = timings["Fajr"] ?: "",
                    syuruk = timings["Sunrise"] ?: "",
                    zohor = timings["Dhuhr"] ?: "",
                    asar = timings["Asr"] ?: "",
                    maghrib = timings["Maghrib"] ?: "",
                    isyak = timings["Isha"] ?: ""
                )

                prayerTimeDao.insert(PrayerTimeEntity.fromDomain(prayerTime))
                Result.success(prayerTime)
            } else {
                Result.failure(Exception("API Error: ${response.code}"))
            }
        } catch (e: Exception) {
            val today = dateFormat.format(Date())
            val cached = prayerTimeDao.getByDate(today)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getPrayerTimesByCity(city: String): Result<PrayerTime> {
        return try {
            val today = dateFormat.format(Date())
            val response = api.getPrayerTimesByCity(today, city)
            if (response.code == 200 && response.status == "OK") {
                val timings = response.data.timings
                val gregorianDate = response.data.date.gregorian.date

                val prayerTime = PrayerTime(
                    date = gregorianDate,
                    subuh = timings["Fajr"] ?: "",
                    syuruk = timings["Sunrise"] ?: "",
                    zohor = timings["Dhuhr"] ?: "",
                    asar = timings["Asr"] ?: "",
                    maghrib = timings["Maghrib"] ?: "",
                    isyak = timings["Isha"] ?: "",
                    zone = city
                )

                prayerTimeDao.insert(PrayerTimeEntity.fromDomain(prayerTime))
                Result.success(prayerTime)
            } else {
                Result.failure(Exception("API Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOfflinePrayerTimes(): Flow<List<PrayerTimeEntity>> {
        return prayerTimeDao.getLastWeekFlow()
    }

    suspend fun getNextPrayer(prayerTime: PrayerTime): NextPrayer? {
        val now = Calendar.getInstance()
        val timeMap = mapOf(
            "Subuh" to prayerTime.subuh,
            "Syuruk" to prayerTime.syuruk,
            "Zohor" to prayerTime.zohor,
            "Asar" to prayerTime.asar,
            "Maghrib" to prayerTime.maghrib,
            "Isyak" to prayerTime.isyak
        )

        val prayerCalendars = prayerOrder.mapNotNull { name ->
            timeMap[name]?.let { time ->
                val parts = time.split(":")
                if (parts.size == 2) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                        set(Calendar.MINUTE, parts[1].toInt())
                        set(Calendar.SECOND, 0)
                    }
                    Pair(name, cal)
                } else null
            }
        }

        val nowMillis = now.timeInMillis
        for ((name, cal) in prayerCalendars) {
            val diff = cal.timeInMillis - nowMillis
            if (diff > 0) {
                return NextPrayer(name = name, time = timeMap[name] ?: "", remainingMillis = diff)
            }
        }

        val tomorrowSubuh = prayerCalendars.firstOrNull()?.let { (name, cal) ->
            cal.apply { add(Calendar.DAY_OF_MONTH, 1) }
            Pair(name, cal)
        }
        return tomorrowSubuh?.let { (name, cal) ->
            NextPrayer(name = name, time = timeMap[name] ?: "", remainingMillis = cal.timeInMillis - nowMillis)
        }
    }

    fun getPrayerTimesForDate(date: String): Flow<PrayerTimeEntity?> {
        return prayerTimeDao.getByDateFlow(date)
    }
}
