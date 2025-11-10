package com.nyapass.loader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyapass.loader.data.preferences.AppPreferences
import com.nyapass.loader.data.preferences.DarkMode
import com.nyapass.loader.data.preferences.Language
import com.nyapass.loader.data.preferences.SaveLocation
import com.nyapass.loader.data.preferences.ThemeColor
import com.nyapass.loader.data.preferences.UserAgentPreset
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 设置ViewModel
 */
class SettingsViewModel(
    private val preferences: AppPreferences
) : ViewModel() {
    
    // 主题颜色
    val themeColor: StateFlow<ThemeColor> = preferences.themeColor
    
    // 暗色模式
    val darkMode: StateFlow<DarkMode> = preferences.darkMode
    
    // 自定义颜色
    val customColor: StateFlow<String?> = preferences.customColor
    
    // 默认User-Agent
    val defaultUserAgent: StateFlow<String?> = preferences.defaultUserAgent
    
    // 自定义User-Agent预设列表
    val customUserAgentPresets: StateFlow<List<UserAgentPreset>> = preferences.customUserAgentPresets
    
    // 默认保存位置
    val defaultSaveLocation: StateFlow<SaveLocation> = preferences.defaultSaveLocation
    
    // 自定义保存路径
    val customSavePath: StateFlow<String?> = preferences.customSavePath
    
    // 默认线程数
    val defaultThreadCount: StateFlow<Int> = preferences.defaultThreadCount
    
    // 剪贴板监听开关
    val clipboardMonitorEnabled: StateFlow<Boolean> = preferences.clipboardMonitorEnabled
    
    // Firebase 开关
    val firebaseEnabled: StateFlow<Boolean> = preferences.firebaseEnabled
    
    // 语言设置
    val language: StateFlow<Language> = preferences.language
    
    // UI状态
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    /**
     * 更新主题颜色
     */
    fun updateThemeColor(color: ThemeColor) {
        viewModelScope.launch {
            preferences.saveThemeColor(color)
        }
    }
    
    /**
     * 更新暗色模式
     */
    fun updateDarkMode(mode: DarkMode) {
        viewModelScope.launch {
            preferences.saveDarkMode(mode)
        }
    }
    
    /**
     * 更新自定义颜色
     */
    fun updateCustomColor(color: String?) {
        viewModelScope.launch {
            preferences.saveCustomColor(color)
        }
    }
    
    /**
     * 更新默认User-Agent
     */
    fun updateDefaultUserAgent(userAgent: String?) {
        viewModelScope.launch {
            preferences.saveDefaultUserAgent(userAgent)
        }
    }
    
    /**
     * 添加自定义User-Agent预设
     */
    fun addCustomUserAgentPreset(name: String, userAgent: String) {
        viewModelScope.launch {
            preferences.addCustomUserAgentPreset(name, userAgent)
        }
    }
    
    /**
     * 删除自定义User-Agent预设
     */
    fun removeCustomUserAgentPreset(preset: UserAgentPreset) {
        viewModelScope.launch {
            preferences.removeCustomUserAgentPreset(preset)
        }
    }
    
    /**
     * 更新默认保存位置
     */
    fun updateDefaultSaveLocation(location: SaveLocation) {
        viewModelScope.launch {
            preferences.saveDefaultSaveLocation(location)
        }
    }
    
    /**
     * 更新自定义保存路径
     */
    fun updateCustomSavePath(path: String?) {
        viewModelScope.launch {
            preferences.saveCustomSavePath(path)
        }
    }
    
    /**
     * 更新默认线程数
     */
    fun updateDefaultThreadCount(count: Int) {
        viewModelScope.launch {
            preferences.saveDefaultThreadCount(count)
        }
    }
    
    /**
     * 更新剪贴板监听开关
     */
    fun updateClipboardMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveClipboardMonitorEnabled(enabled)
        }
    }
    
    /**
     * 更新 Firebase 开关
     */
    fun updateFirebaseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveFirebaseEnabled(enabled)
        }
    }
    
    /**
     * 更新语言设置
     */
    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            preferences.saveLanguage(language)
        }
    }
    
    /**
     * 显示颜色选择对话框
     */
    fun showColorPicker() {
        _uiState.update { it.copy(showColorPicker = true) }
    }
    
    /**
     * 隐藏颜色选择对话框
     */
    fun hideColorPicker() {
        _uiState.update { it.copy(showColorPicker = false) }
    }
    
    /**
     * 显示UA编辑对话框
     */
    fun showUserAgentDialog() {
        _uiState.update { it.copy(showUserAgentDialog = true) }
    }
    
    /**
     * 隐藏UA编辑对话框
     */
    fun hideUserAgentDialog() {
        _uiState.update { it.copy(showUserAgentDialog = false) }
    }
    
    /**
     * 显示添加自定义UA预设对话框
     * 同时关闭User-Agent对话框，避免对话框嵌套
     */
    fun showAddCustomUADialog() {
        _uiState.update { it.copy(
            showUserAgentDialog = false,
            showAddCustomUADialog = true
        ) }
    }
    
    /**
     * 隐藏添加自定义UA预设对话框
     * 并重新打开User-Agent对话框
     */
    fun hideAddCustomUADialog() {
        _uiState.update { it.copy(
            showAddCustomUADialog = false,
            showUserAgentDialog = true
        ) }
    }
    
    /**
     * 清除所有对话框状态
     * 用于页面导航离开时防止状态残留
     */
    fun clearAllDialogs() {
        _uiState.update { SettingsUiState() }
    }
}

/**
 * 设置UI状态
 */
data class SettingsUiState(
    val showColorPicker: Boolean = false,
    val showUserAgentDialog: Boolean = false,
    val showAddCustomUADialog: Boolean = false
)

