package com.waktusolat.app.ui.prayer

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.waktusolat.app.data.local.AppDatabase
import com.waktusolat.app.data.preferences.SettingsDataStore
import com.waktusolat.app.data.repository.PrayerTimeRepository
import com.waktusolat.app.data.worker.PrayerAlarmScheduler
import com.waktusolat.app.domain.model.NextPrayer
import com.waktusolat.app.domain.model.PrayerTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrayerUiState(
    val prayerTime: PrayerTime? = null,
    val nextPrayer: NextPrayer? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isGpsActive: Boolean = false,
    val selectedZone: String = "",
    val countdownText: String = "--:--:--"
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val repository: PrayerTimeRepository
    private val fusedLocationClient: FusedLocationProviderClient
    private val settingsDataStore: SettingsDataStore
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    init {
        val app = getApplication<Application>()
        val db = AppDatabase.getInstance(app)
        repository = PrayerTimeRepository(db.prayerTimeDao())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(app)
        settingsDataStore = SettingsDataStore(app)
        viewModelScope.launch {
            try {
                val savedZone = settingsDataStore.selectedZone.first()
                if (savedZone.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(selectedZone = savedZone)
                }
            } catch (_: Exception) { }
        }
        loadPrayerTimes()
        startCountdownTimer()
    }

    private fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val hasLocationPermission = ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            _uiState.value = _uiState.value.copy(isGpsActive = true)
                            viewModelScope.launch {
                                val result = repository.getPrayerTimes(
                                    location.latitude,
                                    location.longitude
                                )
                                handleResult(result)
                            }
                        } else {
                            loadOfflineData()
                        }
                    }.addOnFailureListener {
                        loadOfflineData()
                    }
                } catch (e: Exception) {
                    loadOfflineData()
                }
            } else {
                loadOfflineData()
            }
        }
    }

    private suspend fun handleResult(result: Result<PrayerTime>) {
        result.fold(
            onSuccess = { prayerTime ->
                val nextPrayer = repository.getNextPrayer(prayerTime)
                _uiState.value = _uiState.value.copy(
                    prayerTime = prayerTime,
                    nextPrayer = nextPrayer,
                    isLoading = false,
                    error = null
                )
                try {
                    val app = getApplication<Application>()
                    PrayerAlarmScheduler.schedulePrayerAlarms(app, prayerTime)
                } catch (_: Exception) { }
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal memuat waktu solat"
                )
                loadOfflineData()
            }
        )
    }

    private fun loadOfflineData() {
        viewModelScope.launch {
            val today = dateFormat.format(Date())
            repository.getPrayerTimesForDate(today).collect { entity ->
                if (entity != null) {
                    val prayerTime = entity.toDomain()
                    val nextPrayer = repository.getNextPrayer(prayerTime)
                    _uiState.value = _uiState.value.copy(
                        prayerTime = prayerTime,
                        nextPrayer = nextPrayer,
                        isLoading = false,
                        isGpsActive = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Tiada data. Sila hidupkan internet/lokasi."
                    )
                }
            }
        }
    }

    fun loadByZone(zone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, selectedZone = zone)
            settingsDataStore.setSelectedZone(zone)
            val result = repository.getPrayerTimesByCity(zone)
            handleResult(result)
        }
    }

    fun refresh() {
        loadPrayerTimes()
    }

    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                _uiState.value.nextPrayer?.let { nextPrayer ->
                    val remaining = nextPrayer.remainingMillis
                    if (remaining > 0) {
                        val totalSeconds = remaining / 1000
                        val hours = totalSeconds / 3600
                        val minutes = (totalSeconds % 3600) / 60
                        val seconds = totalSeconds % 60
                        val text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        _uiState.value = _uiState.value.copy(countdownText = text)
                    } else {
                        loadPrayerTimes()
                    }
                }
                delay(1000)
            }
        }
    }
}
