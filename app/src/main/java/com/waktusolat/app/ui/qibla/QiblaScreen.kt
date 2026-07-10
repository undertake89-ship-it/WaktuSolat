package com.waktusolat.app.ui.qibla

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(viewModel: QiblaViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Mengkalibrasi sensor...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            }
        }
        else -> QiblaCompass(uiState.qiblaBearing, uiState.deviceAzimuth, uiState.isAccurate, uiState.calibrationHint)
    }
}

@Composable
private fun QiblaCompass(qiblaBearing: Float, deviceAzimuth: Float, isAccurate: Boolean, calibrationHint: Boolean) {
    val animatedRotation by animateFloatAsState(targetValue = -deviceAzimuth, animationSpec = tween(300), label = "rotation")
    val diff = (qiblaBearing - deviceAzimuth + 360) % 360

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Arah Kiblat", style = MaterialTheme.typography.headlineMedium, color = primaryColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(300.dp)) {
                val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2 - 20
                drawCircle(Color.LightGray.copy(alpha = 0.3f), r, Offset(cx, cy))
                drawCircle(Color.LightGray.copy(alpha = 0.2f), r * 0.85f, Offset(cx, cy))

                rotate(animatedRotation, Offset(cx, cy)) {
                    for (i in 0 until 360 step 30) {
                        val rad = Math.toRadians(i.toDouble()).toFloat()
                        val outer = Offset(cx + r * cos(rad), cy + r * sin(rad))
                        val inner = Offset(cx + (r - size.width / 30) * cos(rad), cy + (r - size.width / 30) * sin(rad))
                        drawLine(if (i % 90 == 0) primaryColor else Color.Gray, outer, inner, strokeWidth = if (i % 90 == 0) 4f else 2f)
                    }
                    val aRad = Math.toRadians(diff.toDouble()).toFloat()
                    val aLen = r * 0.7f
                    val aEnd = Offset(cx + aLen * sin(aRad), cy - aLen * cos(aRad))
                    drawLine(Color.Red, Offset(cx, cy), aEnd, strokeWidth = 6f)
                    drawCircle(Color.Red, 10f, aEnd)
                    drawCircle(Color.Blue, 12f, Offset(cx, cy))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("°".format(qiblaBearing.toDouble()), style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("dari utara sebenar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        if (calibrationHint) {
            Spacer(Modifier.height(16.dp))
            Text("Kalibrasi sensor: gerakkan peranti dalam bentuk angka 8", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
