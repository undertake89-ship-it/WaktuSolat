package com.waktusolat.app.ui.quran

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waktusolat.app.data.local.AppDatabase
import com.waktusolat.app.data.repository.QuranRepository
import com.waktusolat.app.domain.model.Ayat
import com.waktusolat.app.domain.model.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    surahId: Int,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { QuranRepository(AppDatabase.getInstance(context).quranDao()) }
    var surah by remember { mutableStateOf<Surah?>(null) }
    var ayahs by remember { mutableStateOf<List<Ayat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var playingAyat by remember { mutableIntStateOf(-1) }
    var isPaused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(surahId) {
        surah = repository.getSurahById(surahId)
        surah?.let {
            repository.updateLastRead(it.id, it.lastReadAyat)
        }

        val result = repository.getAyatList(surahId)
        result.fold(
            onSuccess = { ayahs = it },
            onFailure = { isLoading = false }
        )

        val translationResult = repository.getAyatTranslation(surahId)
        translationResult.fold(
            onSuccess = { translations ->
                ayahs = ayahs.map { ayat ->
                    ayat.copy(textTranslation = translations[ayat.ayatNumber] ?: "")
                }
                isLoading = false
            },
            onFailure = { isLoading = false }
        )
    }

    LaunchedEffect(surah, ayahs) {
        surah?.let { s ->
            if (s.lastReadAyat > 0 && ayahs.isNotEmpty()) {
                val targetIndex = ayahs.indexOfFirst { it.ayatNumber == s.lastReadAyat }
                if (targetIndex >= 0) {
                    listState.animateScrollToItem(targetIndex + 1)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.value?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surah?.nameRumi ?: "Surah ${surahId}") },
                navigationIcon = {
                    IconButton(onClick = {
                        mediaPlayer.value?.release()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                surah?.let {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(it.nameArabic, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(it.nameRumi, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.height(4.dp))
                                Text("— ${it.nameTranslation} —", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Spacer(Modifier.height(4.dp))
                                Text("${it.totalAyat} ayat | ${it.revelationType}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                items(ayahs, key = { it.ayatNumber }) { ayat ->
                    AyatCard(
                        ayat = ayat,
                        isPlaying = playingAyat == ayat.ayatNumber,
                        onPlay = {
                            if (playingAyat == ayat.ayatNumber) {
                                mediaPlayer.value?.let { mp ->
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPaused = true
                                    } else {
                                        mp.start()
                                        isPaused = false
                                    }
                                }
                            } else {
                                mediaPlayer.value?.release()
                                playingAyat = ayat.ayatNumber
                                isPaused = false
                                if (ayat.audioUrl.isNotBlank()) {
                                    try {
                                        val mp = MediaPlayer().apply {
                                            setAudioAttributes(
                                                AudioAttributes.Builder()
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                    .build()
                                            )
                                            setDataSource(context, Uri.parse(ayat.audioUrl))
                                            setOnPreparedListener { it.start() }
                                            setOnCompletionListener {
                                                playingAyat = -1
                                            }
                                            setOnErrorListener { _, _, _ ->
                                                playingAyat = -1
                                                true
                                            }
                                            prepareAsync()
                                        }
                                        mediaPlayer.value = mp
                                    } catch (e: Exception) {
                                        playingAyat = -1
                                    }
                                }
                            }
                        },
                        onAyatViewed = {
                            repository.updateLastRead(surahId, ayat.ayatNumber)
                        }
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AyatCard(
    ayat: Ayat,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onAyatViewed: () -> Unit
) {
    LaunchedEffect(ayat.ayatNumber) {
        onAyatViewed()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ayat ${ayat.ayatNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                if (ayat.audioUrl.isNotBlank()) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Mainkan",
                            modifier = Modifier.size(20.dp),
                            tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                ayat.textArabic,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 40.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            if (ayat.textTranslation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    ayat.textTranslation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}
