package com.waktusolat.app.ui.qibla

import android.app.Application
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationServices
import com.waktusolat.app.domain.model.QiblaDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class QiblaUiState(
    val direction: QiblaDirection? = null,
    val qiblaBearing: Float = 0f,
    val deviceAzimuth: Float = 0f,
    val isAccurate: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val calibrationHint: Boolean = false
)

class QiblaViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private val sensorManager = application.getSystemService(Application.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false
    private var userLat = 3.139
    private var userLng = 101.6869

    companion object {
        private const val KAABAH_LAT = 21.4225
        private const val KAABAH_LNG = 39.8262
    }

    init { getLastLocation() }

    private fun getLastLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(getApplication())
                client.lastLocation.addOnSuccessListener { location ->
                    if (location != null) { userLat = location.latitude; userLng = location.longitude }
                    finishInit()
                }.addOnFailureListener { finishInit() }
            } catch (e: Exception) { finishInit() }
        } else { finishInit() }
    }

    private fun finishInit() {
        _uiState.value = _uiState.value.copy(isLoading = false)
        calculateQiblaBearing()
        registerSensors()
    }

    private fun calculateQiblaBearing() {
        val lat1 = Math.toRadians(userLat)
        val lon1 = Math.toRadians(userLng)
        val lat2 = Math.toRadians(KAABAH_LAT)
        val lon2 = Math.toRadians(KAABAH_LNG)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        var bearing = Math.toDegrees(atan2(y, x)).toFloat()
        bearing = (bearing + 360) % 360
        _uiState.value = _uiState.value.copy(qiblaBearing = bearing)
    }

    private fun registerSensors() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> { System.arraycopy(event.values, 0, gravity, 0, 3); hasGravity = true }
            Sensor.TYPE_MAGNETIC_FIELD -> { System.arraycopy(event.values, 0, geomagnetic, 0, 3); hasGeomagnetic = true }
        }
        if (hasGravity && hasGeomagnetic) {
            val rot = FloatArray(9); val incl = FloatArray(9); val az = FloatArray(1)
            if (SensorManager.getRotationMatrix(rot, incl, gravity, geomagnetic)) {
                SensorManager.getOrientation(rot, az)
                val deg = Math.toDegrees(az[0].toDouble()).toFloat()
                val normAz = (deg + 360) % 360
                val acc = SensorManager.getInclination(incl)
                val isAcc = !acc.isNaN() && acc > 0.1f
                _uiState.value = _uiState.value.copy(
                    deviceAzimuth = normAz, isAccurate = isAcc, calibrationHint = !isAcc,
                    direction = QiblaDirection(_uiState.value.qiblaBearing, normAz, acc, isAcc)
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            _uiState.value = _uiState.value.copy(calibrationHint = true, isAccurate = false)
        }
    }

    override fun onCleared() { super.onCleared(); sensorManager.unregisterListener(this) }
}
