package com.nyapass.loader.util

import android.content.Context
import android.os.Build
import android.webkit.WebSettings

/**
 * User-Agent 辅助类
 */
object UserAgentHelper {
    
    /**
     * 获取默认的WebView User-Agent
     */
    fun getDefaultUserAgent(context: Context): String {
        return try {
            // 使用WebView的默认UA
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                WebSettings.getDefaultUserAgent(context)
            } else {
                // 对于旧版本Android，使用系统默认UA
                System.getProperty("http.agent") ?: getCustomUserAgent()
            }
        } catch (e: Exception) {
            // 如果获取失败，返回自定义UA
            getCustomUserAgent()
        }
    }
    
    /**
     * 获取自定义的User-Agent（作为后备方案）
     */
    private fun getCustomUserAgent(): String {
        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = Build.MODEL
        val buildNumber = Build.ID
        
        return "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel Build/$buildNumber) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
    }
    
    /**
     * 常用的User-Agent预设
     */
    object Presets {
        // Chrome桌面
        const val CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
        
        // Chrome Mac
        const val CHROME_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
        
        // Firefox
        const val FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) " +
                "Gecko/20100101 Firefox/121.0"
        
        // Edge
        const val EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
        
        // Safari
        const val SAFARI = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                "Version/17.1 Safari/605.1.15"
        
        // Android Chrome
        const val ANDROID_CHROME = "Mozilla/5.0 (Linux; Android 13) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
        
        // iPhone Safari
        const val IPHONE_SAFARI = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                "Version/17.1 Mobile/15E148 Safari/604.1"
    }
}

