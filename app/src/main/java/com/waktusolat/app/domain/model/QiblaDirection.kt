package com.waktusolat.app.domain.model

data class QiblaDirection(
    val bearing: Float,
    val deviceAzimuth: Float,
    val accuracy: Float,
    val isAccurate: Boolean
)
