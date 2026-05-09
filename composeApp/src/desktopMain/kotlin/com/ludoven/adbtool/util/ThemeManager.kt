package com.ludoven.adbtool.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

object ThemeManager {
    enum class ThemeMode(val code: String, val displayNameZh: String, val displayNameEn: String) {
        SYSTEM("system", "跟随系统", "Follow System"),
        LIGHT("light", "浅色", "Light"),
        DARK("dark", "深色", "Dark")
    }

    private const val THEME_KEY = "selected_theme_mode"
    private val preferences = Preferences.userNodeForPackage(ThemeManager::class.java)

    private val _currentThemeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val currentThemeMode: StateFlow<ThemeMode> = _currentThemeMode.asStateFlow()

    fun initialize() {
        _currentThemeMode.value = getCurrentThemeMode()
    }

    fun getCurrentThemeMode(): ThemeMode {
        val savedCode = preferences.get(THEME_KEY, ThemeMode.SYSTEM.code)
        return ThemeMode.entries.find { it.code == savedCode } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(themeMode: ThemeMode) {
        preferences.put(THEME_KEY, themeMode.code)
        _currentThemeMode.value = themeMode
    }

    fun resolveUseDarkTheme(systemDark: Boolean): Boolean {
        return when (_currentThemeMode.value) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }
}
