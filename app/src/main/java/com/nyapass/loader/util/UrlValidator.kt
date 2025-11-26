package com.nyapass.loader.util

/**
 * URL验证工具
 */
object UrlValidator {

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

