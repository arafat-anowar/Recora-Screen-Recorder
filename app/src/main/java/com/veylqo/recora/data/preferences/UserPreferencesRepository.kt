package com.veylqo.recora.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veylqo.recora.core.model.AudioSource
import com.veylqo.recora.core.model.FpsOption
import com.veylqo.recora.core.model.OrientationOption
import com.veylqo.recora.core.model.ResolutionPreset
import com.veylqo.recora.core.model.TimerPreset
import com.veylqo.recora.core.model.UserEntitlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recora_settings")

data class RecoraSettings(
    val audioSource: AudioSource = AudioSource.INTERNAL,
    val resolutionPreset: ResolutionPreset = ResolutionPreset.RES_4K,
    val fpsOption: FpsOption = FpsOption.FPS_60,
    val orientationOption: OrientationOption = OrientationOption.PORTRAIT,
    val timerPreset: TimerPreset = TimerPreset.OFF,
    val customTimerSeconds: Int = 3,
    val customBitrateBps: Int = 0,
    val showControls: Boolean = false,
    val showPenTools: Boolean = false,
    val showFacecam: Boolean = false,
    val showTouches: Boolean = false,
    val hapticFeedback: Boolean = true,
    val smoothAnimations: Boolean = true,
    val entitlement: UserEntitlement = UserEntitlement.MONTHLY,
    val promoBannerDismissed: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val AUDIO_SOURCE = stringPreferencesKey("audio_source")
        val RESOLUTION_PRESET = stringPreferencesKey("resolution_preset")
        val FPS_OPTION = stringPreferencesKey("fps_option")
        val ORIENTATION_OPTION = stringPreferencesKey("orientation_option")
        val TIMER_PRESET = stringPreferencesKey("timer_preset")
        val CUSTOM_TIMER_SECONDS = intPreferencesKey("custom_timer_seconds")
        val CUSTOM_BITRATE_BPS = intPreferencesKey("custom_bitrate_bps")
        val SHOW_CONTROLS = booleanPreferencesKey("show_controls")
        val SHOW_PEN_TOOLS = booleanPreferencesKey("show_pen_tools")
        val SHOW_FACECAM = booleanPreferencesKey("show_facecam")
        val SHOW_TOUCHES = booleanPreferencesKey("show_touches")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SMOOTH_ANIMATIONS = booleanPreferencesKey("smooth_animations")
        val ENTITLEMENT = stringPreferencesKey("user_entitlement")
        val PROMO_BANNER_DISMISSED = booleanPreferencesKey("promo_banner_dismissed")
    }

    val settingsFlow: Flow<RecoraSettings> = context.dataStore.data.map { prefs ->
        RecoraSettings(
            audioSource = prefs[Keys.AUDIO_SOURCE]?.let { runCatching { AudioSource.valueOf(it) }.getOrNull() } ?: AudioSource.INTERNAL,
            resolutionPreset = prefs[Keys.RESOLUTION_PRESET]?.let { runCatching { ResolutionPreset.valueOf(it) }.getOrNull() } ?: ResolutionPreset.RES_4K,
            fpsOption = prefs[Keys.FPS_OPTION]?.let { runCatching { FpsOption.valueOf(it) }.getOrNull() } ?: FpsOption.FPS_60,
            orientationOption = prefs[Keys.ORIENTATION_OPTION]?.let { runCatching { OrientationOption.valueOf(it) }.getOrNull() } ?: OrientationOption.PORTRAIT,
            timerPreset = prefs[Keys.TIMER_PRESET]?.let { runCatching { TimerPreset.valueOf(it) }.getOrNull() } ?: TimerPreset.OFF,
            customTimerSeconds = prefs[Keys.CUSTOM_TIMER_SECONDS] ?: 3,
            customBitrateBps = prefs[Keys.CUSTOM_BITRATE_BPS] ?: 0,
            showControls = prefs[Keys.SHOW_CONTROLS] ?: false,
            showPenTools = prefs[Keys.SHOW_PEN_TOOLS] ?: false,
            showFacecam = prefs[Keys.SHOW_FACECAM] ?: false,
            showTouches = prefs[Keys.SHOW_TOUCHES] ?: false,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            smoothAnimations = prefs[Keys.SMOOTH_ANIMATIONS] ?: true,
            entitlement = prefs[Keys.ENTITLEMENT]?.let { runCatching { UserEntitlement.valueOf(it) }.getOrNull() } ?: UserEntitlement.MONTHLY,
            promoBannerDismissed = prefs[Keys.PROMO_BANNER_DISMISSED] ?: false
        )
    }

    suspend fun setAudioSource(source: AudioSource) {
        context.dataStore.edit { it[Keys.AUDIO_SOURCE] = source.name }
    }

    suspend fun setResolutionPreset(preset: ResolutionPreset) {
        context.dataStore.edit { it[Keys.RESOLUTION_PRESET] = preset.name }
    }

    suspend fun setFpsOption(fps: FpsOption) {
        context.dataStore.edit { it[Keys.FPS_OPTION] = fps.name }
    }

    suspend fun setOrientationOption(orientation: OrientationOption) {
        context.dataStore.edit { it[Keys.ORIENTATION_OPTION] = orientation.name }
    }

    suspend fun setTimerPreset(preset: TimerPreset) {
        context.dataStore.edit { it[Keys.TIMER_PRESET] = preset.name }
    }

    suspend fun setCustomTimerSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CUSTOM_TIMER_SECONDS] = seconds }
    }

    suspend fun setCustomBitrateBps(bitrate: Int) {
        context.dataStore.edit { it[Keys.CUSTOM_BITRATE_BPS] = bitrate }
    }

    suspend fun setShowControls(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_CONTROLS] = enabled }
    }

    suspend fun setShowPenTools(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PEN_TOOLS] = enabled }
    }

    suspend fun setShowFacecam(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FACECAM] = enabled }
    }

    suspend fun setShowTouches(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TOUCHES] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setSmoothAnimations(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMOOTH_ANIMATIONS] = enabled }
    }

    suspend fun setEntitlement(entitlement: UserEntitlement) {
        context.dataStore.edit { it[Keys.ENTITLEMENT] = entitlement.name }
    }

    suspend fun setPromoBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.PROMO_BANNER_DISMISSED] = dismissed }
    }
}
