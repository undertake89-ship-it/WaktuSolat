package com.waktusolat.app.ui.prayer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

val malaysianZones = listOf(
    "Johor", "Kedah", "Kelantan", "Kuala Lumpur", "Labuan",
    "Melaka", "Negeri Sembilan", "Pahang", "Penang", "Perak",
    "Perlis", "Putrajaya", "Sabah", "Sarawak", "Selangor",
    "Terengganu"
)

@Composable
fun PrayerScreen(
    viewModel: PrayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showZonePicker by remember { mutableStateOf(false) }

    if (showZonePicker) {
        ZonePickerDialog(
            currentZone = uiState.selectedZone,
            onZoneSelected = { zone ->
                viewModel.loadByZone(zone)
                showZonePicker = false
            },
            onDismiss = { showZonePicker = false }
        )
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Memuatkan waktu solat...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        uiState.error != null && uiState.prayerTime == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = uiState.error ?: "Ralat",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Cuba Lagi")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showZonePicker = true }) {
                        Text("Pilih Zon Manual")
                    }
                }
            }
        }

        else -> {
            uiState.prayerTime?.let { prayerTime ->
                PrayerContent(
                    prayerTime = prayerTime,
                    nextPrayer = uiState.nextPrayer,
                    countdownText = uiState.countdownText,
                    countdownName = uiState.nextPrayer?.name ?: "",
                    isGpsActive = uiState.isGpsActive,
                    selectedZone = uiState.selectedZone,
                    onRefresh = { viewModel.refresh() },
                    onZonePickerClick = { showZonePicker = true }
                )
            }
        }
    }
}

@Composable
private fun PrayerContent(
    prayerTime: com.waktusolat.app.domain.model.PrayerTime,
    nextPrayer: com.waktusolat.app.domain.model.NextPrayer?,
    countdownText: String,
    countdownName: String,
    isGpsActive: Boolean,
    selectedZone: String,
    onRefresh: () -> Unit,
    onZonePickerClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            NextPrayerCard(
                nextPrayerName = countdownName,
                countdownText = countdownText,
                date = prayerTime.date,
                onRefresh = onRefresh
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Waktu Solat",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isGpsActive && selectedZone.isNotBlank()) {
                        Text(
                            selectedZone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = onZonePickerClick) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Pilih Zon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        val prayerItems = listOf(
            "Subuh" to prayerTime.subuh,
            "Syuruk" to prayerTime.syuruk,
            "Zohor" to prayerTime.zohor,
            "Asar" to prayerTime.asar,
            "Maghrib" to prayerTime.maghrib,
            "Isyak" to prayerTime.isyak
        )

        items(prayerItems) { (name, time) ->
            PrayerTimeCard(
                name = name,
                time = time,
                isNext = name == countdownName
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ZonePickerDialog(
    currentZone: String,
    onZoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Negeri/Zon") },
        text = {
            Column {
                malaysianZones.forEach { zone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onZoneSelected(zone) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = zone == currentZone,
                            onClick = { onZoneSelected(zone) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(zone, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun NextPrayerCard(
    nextPrayerName: String,
    countdownText: String,
    date: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Waktu Seterusnya",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = nextPrayerName.ifEmpty { "--" },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = countdownText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PrayerTimeCard(
    name: String,
    time: String,
    isNext: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNext) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = time.take(5),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isNext)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
