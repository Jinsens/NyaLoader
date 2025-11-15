package com.nyapass.loader.util

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity

/**
 * 剪贴板处理器
 * 统一管理剪贴板监听和URL提取
 */
class ClipboardHandler(private val activity: ComponentActivity) {
    
    // 记录最后处理的URL（规范化后），避免重复弹窗
    private var lastProcessedUrl: String = ""
    
    /**
     * 设置初始的最后处理URL
     * 
     * @param url 上次处理的URL
     */
    fun setLastProcessedUrl(url: String) {
        lastProcessedUrl = url
    }
    
    /**
     * 检查剪贴板中的下载链接
     * 每次进入前台都会检查一次
     * 
     * @return URL字符串，如果没有有效链接返回null
     */
    fun checkClipboardForUrl(): String? {
        val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboardManager?.primaryClip
        
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString() ?: ""
            
            if (text.isNotBlank()) {
                // 尝试从文本中提取URL
                val url = extractUrlFromText(text) ?: return null
                
                if (UrlValidator.isValidUrlFormat(url)) {
                    // 使用规范化后的URL做去重比较
                    val normalized = normalizeUrlForCompare(url)
                    
                    // 检查是否与上次处理的URL相同，避免重复弹窗
                    if (normalized.isNotEmpty() && normalized != lastProcessedUrl) {
                        lastProcessedUrl = normalized
                        return url
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * 从文本中提取URL（增强版）
     * - 支持一段文本中包含多个URL
     * - 去掉中文/英文标点等尾部字符
     * - 优先返回看起来像真实资源文件的链接（带常见扩展名）
     */
    private fun extractUrlFromText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        
        // 使用正则提取文本中的所有 http/https 链接
        val regex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(trimmed).map { it.value }.toList()
        if (matches.isEmpty()) return null
        
        // 清理每个候选URL尾部的标点符号
        val cleanedCandidates = matches.map { raw ->
            raw.trim().trimEnd(')', ']', '}', '>', '，', '。', '！', '；', ';', ',', '、', '"', '\'')
        }.filter { it.isNotBlank() }
        
        if (cleanedCandidates.isEmpty()) return null
        
        // 优先选择看起来像真实资源文件的URL（带常见扩展名）
        val exts = listOf(
            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz",
            ".apk", ".exe", ".iso",
            ".mp4", ".mkv", ".avi", ".mov",
            ".mp3", ".flac", ".wav",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
        )
        
        val withExt = cleanedCandidates.firstOrNull { url ->
            val lower = url.lowercase()
            val path = lower.substringBefore('?').substringBefore('#')
            exts.any { ext -> path.endsWith(ext) }
        }
        
        return withExt ?: cleanedCandidates.first()
    }
    
    /**
     * 将URL规范化用于去重比较：去掉结尾的斜杠、忽略大小写和fragment
     */
    private fun normalizeUrlForCompare(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: return url.trim()
            val host = uri.host?.lowercase() ?: return url.trim()
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val path = (uri.path ?: "").trimEnd('/')
            val query = uri.query?.let { "?$it" } ?: ""
            "$scheme://$host$port$path$query"
        } catch (e: Exception) {
            url.trim()
        }
    }
    
    /**
     * 获取当前最后处理的URL
     */
    fun getLastProcessedUrl(): String = lastProcessedUrl
}

