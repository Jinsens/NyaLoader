package com.nyapass.loader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nyapass.loader.data.preferences.AppPreferences
import com.nyapass.loader.repository.DownloadRepository

/**
 * ViewModel工厂
 */
class ViewModelFactory(
    private val repository: DownloadRepository,
    private val preferences: AppPreferences? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DownloadViewModel::class.java) -> {
                DownloadViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                requireNotNull(preferences) { "AppPreferences is required for SettingsViewModel" }
                SettingsViewModel(preferences) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

