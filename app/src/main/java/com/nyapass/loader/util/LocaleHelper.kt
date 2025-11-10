package com.nyapass.loader.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.nyapass.loader.data.preferences.Language
import java.util.Locale

/**
 * 语言切换帮助类
 */
object LocaleHelper {
    
    /**
     * 应用语言设置到Context
     */
    fun applyLanguage(context: Context, language: Language): Context {
        val locale = getLocaleFromLanguage(language)
        
        return if (locale != null) {
            updateResources(context, locale)
        } else {
            // 跟随系统
            context
        }
    }
    
    /**
     * 从Language枚举获取Locale对象
     */
    private fun getLocaleFromLanguage(language: Language): Locale? {
        return when (language) {
            Language.SYSTEM -> null // 跟随系统
            Language.CHINESE_SIMPLIFIED -> Locale.SIMPLIFIED_CHINESE
            Language.CHINESE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
            Language.ENGLISH -> Locale.ENGLISH
            Language.JAPANESE -> Locale.JAPAN
        }
    }
    
    /**
     * 更新Context的语言资源
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 获取当前语言的Locale
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
}

