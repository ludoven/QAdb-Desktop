package com.ludoven.adbtool.agent

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps unfinished Agent functionality out of stable desktop releases by default. */
class AgentFeaturePreferences(
    private val preferences: Preferences = Preferences.userNodeForPackage(AgentFeaturePreferences::class.java)
) {
    private val _enabled = MutableStateFlow(preferences.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _reduceMotion = MutableStateFlow(preferences.getBoolean(KEY_REDUCE_MOTION, false))
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        preferences.putBoolean(KEY_ENABLED, enabled)
        preferences.flush()
        _enabled.value = enabled
    }

    fun setReduceMotion(reduceMotion: Boolean) {
        preferences.putBoolean(KEY_REDUCE_MOTION, reduceMotion)
        preferences.flush()
        _reduceMotion.value = reduceMotion
    }

    private companion object {
        const val KEY_ENABLED = "agent.feature.enabled"
        const val KEY_REDUCE_MOTION = "agent.reduce_motion"
    }
}

object AgentFeatureRuntime {
    val preferences: AgentFeaturePreferences by lazy { AgentFeaturePreferences() }
}
