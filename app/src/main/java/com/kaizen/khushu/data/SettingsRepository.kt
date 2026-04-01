package com.kaizen.khushu.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val VOLUME_COUNTING = booleanPreferencesKey("volume_counting")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_STEP_TIMER = booleanPreferencesKey("show_step_timer")
        val FLUID_TRANSITIONS = booleanPreferencesKey("fluid_transitions")
        val VIBRATION_ON_COUNT = booleanPreferencesKey("vibration_on_count")
        val SHOW_LAP_COUNTER = booleanPreferencesKey("show_lap_counter")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            UserSettings(
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true,
                pureBlack = preferences[PreferencesKeys.PURE_BLACK] ?: false,
                keepScreenAwake = preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: true,
                volumeCounting = preferences[PreferencesKeys.VOLUME_COUNTING] ?: false,
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "System",
                showStepTimer = preferences[PreferencesKeys.SHOW_STEP_TIMER] ?: true,
                fluidTransitions = preferences[PreferencesKeys.FLUID_TRANSITIONS] ?: true,
                vibrationOnCount = preferences[PreferencesKeys.VIBRATION_ON_COUNT] ?: true,
                showLapCounter = preferences[PreferencesKeys.SHOW_LAP_COUNTER] ?: true
            )
        }

    suspend fun updateHaptics(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updatePureBlack(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PURE_BLACK] = enabled }
    }

    suspend fun updateKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun updateVolumeCounting(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VOLUME_COUNTING] = enabled }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun updateShowStepTimer(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_STEP_TIMER] = show }
    }

    suspend fun updateFluidTransitions(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FLUID_TRANSITIONS] = enabled }
    }

    suspend fun updateVibrationOnCount(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATION_ON_COUNT] = enabled }
    }

    suspend fun updateShowLapCounter(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_LAP_COUNTER] = show }
    }
}

data class UserSettings(
    val hapticsEnabled: Boolean,
    val dynamicColor: Boolean,
    val pureBlack: Boolean,
    val keepScreenAwake: Boolean,
    val volumeCounting: Boolean,
    val themeMode: String,
    val showStepTimer: Boolean,
    val fluidTransitions: Boolean,
    val vibrationOnCount: Boolean,
    val showLapCounter: Boolean
)
