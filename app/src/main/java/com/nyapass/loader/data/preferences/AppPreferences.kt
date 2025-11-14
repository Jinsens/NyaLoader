package com.nyapass.loader.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 应用设置管理类
 */
class AppPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "gdownload_preferences",
        Context.MODE_PRIVATE
    )
    
    // 主题颜色
    private val _themeColor = MutableStateFlow(getThemeColor())
    val themeColor: StateFlow<ThemeColor> = _themeColor.asStateFlow()
    
    // 暗色模式
    private val _darkMode = MutableStateFlow(getDarkMode())
    val darkMode: StateFlow<DarkMode> = _darkMode.asStateFlow()
    
    // 自定义主题颜色
    private val _customColor = MutableStateFlow(getCustomColor())
    val customColor: StateFlow<String?> = _customColor.asStateFlow()
    
    // 默认User-Agent
    private val _defaultUserAgent = MutableStateFlow(getDefaultUserAgent())
    val defaultUserAgent: StateFlow<String?> = _defaultUserAgent.asStateFlow()
    
    // 自定义User-Agent预设列表
    private val _customUserAgentPresets = MutableStateFlow(getCustomUserAgentPresets())
    val customUserAgentPresets: StateFlow<List<UserAgentPreset>> = _customUserAgentPresets.asStateFlow()
    
    // 默认保存位置
    private val _defaultSaveLocation = MutableStateFlow(getDefaultSaveLocation())
    val defaultSaveLocation: StateFlow<SaveLocation> = _defaultSaveLocation.asStateFlow()
    
    // 默认自定义路径
    private val _customSavePath = MutableStateFlow(getCustomSavePath())
    val customSavePath: StateFlow<String?> = _customSavePath.asStateFlow()
    
    // 默认线程数
    private val _defaultThreadCount = MutableStateFlow(getDefaultThreadCount())
    val defaultThreadCount: StateFlow<Int> = _defaultThreadCount.asStateFlow()
    
    // 是否启用剪贴板监听
    private val _clipboardMonitorEnabled = MutableStateFlow(getClipboardMonitorEnabled())
    val clipboardMonitorEnabled: StateFlow<Boolean> = _clipboardMonitorEnabled.asStateFlow()
    
    // 是否已显示剪贴板监听首次提示
    private val _hasShownClipboardTip = MutableStateFlow(getHasShownClipboardTip())
    val hasShownClipboardTip: StateFlow<Boolean> = _hasShownClipboardTip.asStateFlow()
    
    // 是否启用 Firebase
    private val _firebaseEnabled = MutableStateFlow(getFirebaseEnabled())
    val firebaseEnabled: StateFlow<Boolean> = _firebaseEnabled.asStateFlow()
    
    // 是否已显示 Firebase 首次提示
    private val _hasShownFirebaseTip = MutableStateFlow(getHasShownFirebaseTip())
    val hasShownFirebaseTip: StateFlow<Boolean> = _hasShownFirebaseTip.asStateFlow()
    
    // 语言设置
    private val _language = MutableStateFlow(getLanguage())
    val language: StateFlow<Language> = _language.asStateFlow()

    // 最近一次处理的剪贴板URL（用于避免重复弹窗）
    fun saveLastClipboardUrl(url: String?) {
        if (url.isNullOrBlank()) {
            prefs.edit().remove(KEY_LAST_CLIPBOARD_URL).apply()
        } else {
            prefs.edit().putString(KEY_LAST_CLIPBOARD_URL, url).apply()
        }
    }

    fun getLastClipboardUrl(): String? {
        return prefs.getString(KEY_LAST_CLIPBOARD_URL, null)
    }
    
    /**
     * 保存主题颜色
     */
    fun saveThemeColor(color: ThemeColor) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
        _themeColor.value = color
    }
    
    /**
     * 获取主题颜色
     */
    private fun getThemeColor(): ThemeColor {
        val colorName = prefs.getString(KEY_THEME_COLOR, ThemeColor.DYNAMIC.name)
        return try {
            ThemeColor.valueOf(colorName ?: ThemeColor.DYNAMIC.name)
        } catch (e: Exception) {
            ThemeColor.DYNAMIC
        }
    }
    
    /**
     * 保存暗色模式
     */
    fun saveDarkMode(mode: DarkMode) {
        prefs.edit().putString(KEY_DARK_MODE, mode.name).apply()
        _darkMode.value = mode
    }
    
    /**
     * 获取暗色模式
     */
    private fun getDarkMode(): DarkMode {
        val modeName = prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.name)
        return try {
            DarkMode.valueOf(modeName ?: DarkMode.SYSTEM.name)
        } catch (e: Exception) {
            DarkMode.SYSTEM
        }
    }
    
    /**
     * 保存自定义颜色
     */
    fun saveCustomColor(color: String?) {
        if (color != null) {
            prefs.edit().putString(KEY_CUSTOM_COLOR, color).apply()
        } else {
            prefs.edit().remove(KEY_CUSTOM_COLOR).apply()
        }
        _customColor.value = color
    }
    
    /**
     * 获取自定义颜色
     */
    private fun getCustomColor(): String? {
        return prefs.getString(KEY_CUSTOM_COLOR, null)
    }
    
    /**
     * 保存默认User-Agent
     */
    fun saveDefaultUserAgent(userAgent: String?) {
        if (userAgent != null) {
            prefs.edit().putString(KEY_DEFAULT_USER_AGENT, userAgent).apply()
        } else {
            prefs.edit().remove(KEY_DEFAULT_USER_AGENT).apply()
        }
        _defaultUserAgent.value = userAgent
    }
    
    /**
     * 获取默认User-Agent
     */
    private fun getDefaultUserAgent(): String? {
        return prefs.getString(KEY_DEFAULT_USER_AGENT, null)
    }
    
    /**
     * 保存自定义User-Agent预设列表
     */
    fun saveCustomUserAgentPresets(presets: List<UserAgentPreset>) {
        val json = kotlinx.serialization.json.Json.encodeToString(presets)
        prefs.edit().putString(KEY_CUSTOM_USER_AGENT_PRESETS, json).apply()
        _customUserAgentPresets.value = presets
    }
    
    /**
     * 获取自定义User-Agent预设列表
     */
    private fun getCustomUserAgentPresets(): List<UserAgentPreset> {
        val json = prefs.getString(KEY_CUSTOM_USER_AGENT_PRESETS, null) ?: return emptyList()
        return try {
            kotlinx.serialization.json.Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 添加自定义User-Agent预设
     */
    fun addCustomUserAgentPreset(name: String, userAgent: String) {
        val currentPresets = _customUserAgentPresets.value.toMutableList()
        currentPresets.add(UserAgentPreset(name, userAgent))
        saveCustomUserAgentPresets(currentPresets)
    }
    
    /**
     * 删除自定义User-Agent预设
     */
    fun removeCustomUserAgentPreset(preset: UserAgentPreset) {
        val currentPresets = _customUserAgentPresets.value.toMutableList()
        currentPresets.remove(preset)
        saveCustomUserAgentPresets(currentPresets)
    }
    
    /**
     * 保存默认保存位置
     */
    fun saveDefaultSaveLocation(location: SaveLocation) {
        prefs.edit().putString(KEY_DEFAULT_SAVE_LOCATION, location.name).apply()
        _defaultSaveLocation.value = location
    }
    
    /**
     * 获取默认保存位置
     */
    private fun getDefaultSaveLocation(): SaveLocation {
        val locationName = prefs.getString(KEY_DEFAULT_SAVE_LOCATION, SaveLocation.PUBLIC_DOWNLOAD.name)
        return try {
            SaveLocation.valueOf(locationName ?: SaveLocation.PUBLIC_DOWNLOAD.name)
        } catch (e: Exception) {
            SaveLocation.PUBLIC_DOWNLOAD
        }
    }
    
    /**
     * 保存自定义保存路径
     */
    fun saveCustomSavePath(path: String?) {
        if (path != null) {
            prefs.edit().putString(KEY_CUSTOM_SAVE_PATH, path).apply()
        } else {
            prefs.edit().remove(KEY_CUSTOM_SAVE_PATH).apply()
        }
        _customSavePath.value = path
    }
    
    /**
     * 获取自定义保存路径
     */
    private fun getCustomSavePath(): String? {
        return prefs.getString(KEY_CUSTOM_SAVE_PATH, null)
    }
    
    /**
     * 保存默认线程数
     */
    fun saveDefaultThreadCount(count: Int) {
        prefs.edit().putInt(KEY_DEFAULT_THREAD_COUNT, count).apply()
        _defaultThreadCount.value = count
    }
    
    /**
     * 获取默认线程数
     */
    private fun getDefaultThreadCount(): Int {
        return prefs.getInt(KEY_DEFAULT_THREAD_COUNT, 32)
    }
    
    /**
     * 保存剪贴板监听开关
     */
    fun saveClipboardMonitorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLIPBOARD_MONITOR_ENABLED, enabled).apply()
        _clipboardMonitorEnabled.value = enabled
    }
    
    /**
     * 获取剪贴板监听开关
     */
    private fun getClipboardMonitorEnabled(): Boolean {
        return prefs.getBoolean(KEY_CLIPBOARD_MONITOR_ENABLED, true)
    }
    
    /**
     * 保存是否已显示剪贴板提示
     */
    fun saveHasShownClipboardTip(shown: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SHOWN_CLIPBOARD_TIP, shown).apply()
        _hasShownClipboardTip.value = shown
    }
    
    /**
     * 获取是否已显示剪贴板提示
     */
    private fun getHasShownClipboardTip(): Boolean {
        return prefs.getBoolean(KEY_HAS_SHOWN_CLIPBOARD_TIP, false)
    }
    
    /**
     * 保存 Firebase 开关
     */
    fun saveFirebaseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FIREBASE_ENABLED, enabled).apply()
        _firebaseEnabled.value = enabled
    }
    
    /**
     * 获取 Firebase 开关
     */
    private fun getFirebaseEnabled(): Boolean {
        return prefs.getBoolean(KEY_FIREBASE_ENABLED, false)
    }
    
    /**
     * 保存是否已显示 Firebase 提示
     */
    fun saveHasShownFirebaseTip(shown: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SHOWN_FIREBASE_TIP, shown).apply()
        _hasShownFirebaseTip.value = shown
    }
    
    /**
     * 获取是否已显示 Firebase 提示
     */
    private fun getHasShownFirebaseTip(): Boolean {
        return prefs.getBoolean(KEY_HAS_SHOWN_FIREBASE_TIP, false)
    }
    
    /**
     * 保存语言设置
     */
    fun saveLanguage(language: Language) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _language.value = language
    }
    
    /**
     * 获取语言设置
     */
    private fun getLanguage(): Language {
        val languageName = prefs.getString(KEY_LANGUAGE, Language.SYSTEM.name)
        return try {
            Language.valueOf(languageName ?: Language.SYSTEM.name)
        } catch (e: Exception) {
            Language.SYSTEM
        }
    }
    
    companion object {
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CUSTOM_COLOR = "custom_color"
        private const val KEY_DEFAULT_USER_AGENT = "default_user_agent"
        private const val KEY_CUSTOM_USER_AGENT_PRESETS = "custom_user_agent_presets"
        private const val KEY_DEFAULT_SAVE_LOCATION = "default_save_location"
        private const val KEY_CUSTOM_SAVE_PATH = "custom_save_path"
        private const val KEY_DEFAULT_THREAD_COUNT = "default_thread_count"
        private const val KEY_CLIPBOARD_MONITOR_ENABLED = "clipboard_monitor_enabled"
        private const val KEY_HAS_SHOWN_CLIPBOARD_TIP = "has_shown_clipboard_tip"
        private const val KEY_FIREBASE_ENABLED = "firebase_enabled"
        private const val KEY_HAS_SHOWN_FIREBASE_TIP = "has_shown_firebase_tip"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_LAST_CLIPBOARD_URL = "last_clipboard_url"
    }
}

/**
 * User-Agent预设
 */
@kotlinx.serialization.Serializable
data class UserAgentPreset(
    val name: String,
    val userAgent: String
)

/**
 * 主题颜色
 */
enum class ThemeColor(val displayName: String) {
    DYNAMIC("自动取色"),
    BLUE("蓝色"),
    GREEN("绿色"),
    PURPLE("紫色"),
    ORANGE("橙色"),
    RED("红色"),
    PINK("粉色"),
    TEAL("青色"),
    CUSTOM("自定义")
}

/**
 * 暗色模式
 */
enum class DarkMode(val displayName: String) {
    LIGHT("浅色"),
    DARK("深色"),
    SYSTEM("跟随系统")
}

/**
 * 语言设置
 */
enum class Language(val displayName: String, val locale: String) {
    SYSTEM("跟随系统", ""),
    CHINESE_SIMPLIFIED("简体中文", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文", "zh-TW"),
    ENGLISH("English", "en"),
    JAPANESE("日本語", "ja")
}

/**
 * 保存位置
 */
enum class SaveLocation(val displayName: String) {
    PUBLIC_DOWNLOAD("公共下载目录"),
    PRIVATE_STORAGE("应用私有目录"),
    CUSTOM("自定义目录")
}

