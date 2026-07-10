package com.waktusolat.app.data.worker

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object AzanPlayer {
    private const val TAG = "AzanPlayer"
    private var mediaPlayer: MediaPlayer? = null

    private val AZAN_URLS = listOf(
        "https://www.islamcan.com/audio/adhan/azan1.mp3",
        "https://download.quranicaudio.com/adan/abdullah_al_mattrod/adan.mp3"
    )

    fun play(context: Context) {
        playFromUrl(context)
    }

    private fun playFromUrl(context: Context) {
        stop()
        vibrate(context)
        try {
            val url = AZAN_URLS.firstOrNull() ?: return
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(context, Uri.parse(url))
                setOnPreparedListener { mp ->
                    mp.start()
                    Log.d(TAG, "Azan playback started")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Azan playback error: $what $extra")
                    tryNextUrl(context)
                    true
                }
                setOnCompletionListener {
                    stop()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play azan", e)
        }
    }

    private fun tryNextUrl(context: Context) {
        stop()
    }

    private fun vibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 200, 500, 200, 500),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate failed", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Stop failed", e)
        }
    }
}
