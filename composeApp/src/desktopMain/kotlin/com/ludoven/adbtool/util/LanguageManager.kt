package com.ludoven.adbtool.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences
import java.util.Locale

/**
 * 语言管理器，用于处理应用程序的多语言切换功能
 */
object LanguageManager {
    
    /**
     * 支持的语言枚举
     */
    enum class Language(val code: String, val displayName: String, val locale: Locale) {
        CHINESE("zh", "中文", Locale.SIMPLIFIED_CHINESE),
        ENGLISH("en", "English", Locale.ENGLISH)
    }
    
    private val preferences = Preferences.userNodeForPackage(LanguageManager::class.java)
    private val LANGUAGE_KEY = "selected_language"
    
    private val _currentLanguage = MutableStateFlow(Language.CHINESE)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()
    
    /**
     * 初始化语言管理器，从配置中读取保存的语言设置
     */
    fun initialize() {
        val savedLanguage = getCurrentLanguage()
        _currentLanguage.value = savedLanguage
        // 设置系统默认Locale
        Locale.setDefault(savedLanguage.locale)
    }
    
    /**
     * 获取当前选择的语言
     */
    fun getCurrentLanguage(): Language {
        val savedLanguageCode = preferences.get(LANGUAGE_KEY, Language.CHINESE.code)
        return Language.values().find { it.code == savedLanguageCode } ?: Language.CHINESE
    }
    
    /**
     * 设置语言
     */
    fun setLanguage(language: Language) {
        preferences.put(LANGUAGE_KEY, language.code)
        _currentLanguage.value = language
        // 更新系统默认Locale
        Locale.setDefault(language.locale)
    }
    
    /**
     * 重置语言设置为默认值
     */
    fun resetLanguage() {
        setLanguage(Language.CHINESE)
    }
    
    /**
     * 获取所有支持的语言
     */
    fun getSupportedLanguages(): List<Language> {
        return Language.values().toList()
    }
    
    /**
     * 检查是否需要重启应用以应用语言更改
     */
    fun isRestartRequired(): Boolean {
        // 在Compose Multiplatform中，语言切换通常需要重启应用
        return true
    }
}