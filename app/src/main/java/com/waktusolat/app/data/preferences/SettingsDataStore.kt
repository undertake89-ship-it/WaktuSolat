package com.waktusolat.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SELECTED_ZONE = stringPreferencesKey("selected_zone")
        private val ENABLE_AZAN = intPreferencesKey("enable_azan")
        private val ENABLE_NOTIFICATIONS = intPreferencesKey("enable_notifications")

        private const val TRUE = 1
        private const val FALSE = 0
    }

    val selectedZone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_ZONE] ?: ""
    }

    val isAzanEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ENABLE_AZAN] ?: TRUE == TRUE
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ENABLE_NOTIFICATIONS] ?: TRUE == TRUE
    }

    suspend fun setSelectedZone(zone: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_ZONE] = zone
        }
    }

    suspend fun setAzanEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ENABLE_AZAN] = if (enabled) TRUE else FALSE
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ENABLE_NOTIFICATIONS] = if (enabled) TRUE else FALSE
        }
    }
}
