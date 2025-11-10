package com.nyapass.loader.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * URL验证工具
 */
object UrlValidator {
    
    /**
     * 验证URL是否可访问（使用HEAD请求）
     * @param url 要验证的URL
     * @param timeoutMs 超时时间（毫秒）
     * @return true表示资源存在，false表示不存在或无法访问
     */
    suspend fun isUrlAccessible(url: String, timeoutMs: Int = 5000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 验证URL格式
                if (!url.startsWith("http://", ignoreCase = true) && 
                    !url.startsWith("https://", ignoreCase = true)) {
                    return@withContext false
                }
                
                val urlObj = URL(url)
                val connection = urlObj.openConnection() as HttpURLConnection
                
                try {
                    // 使用HEAD请求，更快且不下载内容
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = timeoutMs
                    connection.readTimeout = timeoutMs
                    
                    // 设置User-Agent，避免被某些服务器拒绝
                    connection.setRequestProperty("User-Agent", 
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    
                    connection.connect()
                    
                    val responseCode = connection.responseCode
                    
                    // 200-299: 成功
                    // 300-399: 重定向（也算有效）
                    // 400+: 客户端/服务器错误
                    when (responseCode) {
                        in 200..399 -> true
                        else -> false
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                // 任何异常都视为URL不可访问
                false
            }
        }
    }
    
    /**
     * 快速验证URL格式（不进行网络请求）
     */
    fun isValidUrlFormat(url: String): Boolean {
        if (url.isBlank()) return false
        
        val trimmed = url.trim()
        return trimmed.startsWith("http://", ignoreCase = true) || 
               trimmed.startsWith("https://", ignoreCase = true)
    }
}

