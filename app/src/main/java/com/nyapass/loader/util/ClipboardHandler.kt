package com.nyapass.loader.util

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 剪贴板检测结果
 */
sealed class ClipboardCheckResult {
    data class DirectDownload(val url: String) : ClipboardCheckResult()  // 直接可下载（有明显文件扩展名）
    data class NeedsValidation(val url: String) : ClipboardCheckResult()  // 需要验证（复杂参数）
    object NoUrl : ClipboardCheckResult()  // 没有URL
    object AlreadyProcessed : ClipboardCheckResult()  // 已处理过的URL
}

/**
 * 剪贴板处理器
 * 统一管理剪贴板监听和URL提取
 * 支持智能识别：简单链接直接弹窗，复杂链接先检测资源
 */
class ClipboardHandler(private val activity: ComponentActivity) {
    
    // 记录最后处理的URL（规范化后），避免重复弹窗
    private var lastProcessedUrl: String = ""
    
    // 常见的下载文件扩展名
    private val downloadExtensions = listOf(
        // 压缩包
        ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz", ".tar.gz", ".tar.bz2",
        // 安装包
        ".apk", ".exe", ".dmg", ".pkg", ".deb", ".rpm", ".msi",
        // 文档
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".epub", ".mobi",
        // 视频
        ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".mpg", ".mpeg",
        // 音频
        ".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".wma", ".ape",
        // 图片
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico", ".tiff",
        // 镜像
        ".iso", ".img", ".bin", ".dmg",
        // 其他
        ".torrent", ".sql", ".db", ".csv"
    )
    
    /**
     * 设置初始的最后处理URL
     * 
     * @param url 上次处理的URL
     */
    fun setLastProcessedUrl(url: String) {
        lastProcessedUrl = url
    }
    
    /**
     * 检查剪贴板中的下载链接（智能识别版本）
     * 每次进入前台都会检查一次
     * 
     * @return ClipboardCheckResult 检测结果
     */
    suspend fun checkClipboardForUrl(): ClipboardCheckResult = withContext(Dispatchers.IO) {
        val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboardManager?.primaryClip
        
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString() ?: ""
            
            if (text.isNotBlank()) {
                // 尝试从文本中提取URL
                val url = extractUrlFromText(text) ?: return@withContext ClipboardCheckResult.NoUrl
                
                if (UrlValidator.isValidUrlFormat(url)) {
                    // 使用规范化后的URL做去重比较
                    val normalized = normalizeUrlForCompare(url)
                    
                    // 检查是否与上次处理的URL相同，避免重复弹窗
                    if (normalized.isNotEmpty() && normalized == lastProcessedUrl) {
                        return@withContext ClipboardCheckResult.AlreadyProcessed
                    }
                    
                    lastProcessedUrl = normalized
                    
                    // 判断是否有明显的文件扩展名
                    return@withContext if (hasObviousDownloadExtension(url)) {
                        ClipboardCheckResult.DirectDownload(url)
                    } else {
                        ClipboardCheckResult.NeedsValidation(url)
                    }
                }
            }
        }
        
        ClipboardCheckResult.NoUrl
    }
    
    /**
     * 判断URL是否有明显的下载文件扩展名
     */
    private fun hasObviousDownloadExtension(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: ""
            val path = uri.path?.lowercase() ?: ""
            
            // 检查是否为CDN或直接下载站点（这些通常是直链）
            if (isDirectDownloadHost(host)) {
                return true
            }
            
            // 检查路径部分（不含参数）是否以常见下载扩展名结尾
            downloadExtensions.any { ext -> path.endsWith(ext) }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 判断是否为直接下载站点（CDN等）
     * 这些站点的链接通常是直链，不需要登录
     */
    private fun isDirectDownloadHost(host: String): Boolean {
        // GitHub Releases 和 CDN（这些是公开的直链）
        val directDownloadHosts = listOf(
            "github.com/",           // GitHub releases
            "githubusercontent.com",  // GitHub raw content
            "jsdelivr.net",          // jsDelivr CDN
            "unpkg.com",             // unpkg CDN
            "raw.githubusercontent.com"
        )
        
        // CDN 前缀（通常是直链）
        val cdnPrefixes = listOf(
            "cdn.",
            "dl.",
            "download.",
            "downloads.",
            "static.",
            "assets."
        )
        
        // 检查完整域名
        if (directDownloadHosts.any { host.contains(it) }) {
            return true
        }
        
        // 检查CDN前缀
        if (cdnPrefixes.any { host.startsWith(it) }) {
            return true
        }
        
        return false
    }
    
    /**
     * 验证复杂URL是否为下载链接
     * 通过发送HEAD请求检查Content-Type和Content-Disposition
     * 支持协程取消，当协程被取消时会立即停止操作
     *
     * @param url 要验证的URL
     * @return true表示是下载链接，false表示不是
     */
    suspend fun validateDownloadUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查协程是否已被取消
            ensureActive()

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = true

            // 添加User-Agent避免被拒绝
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )

            // 在网络请求前再次检查协程状态
            ensureActive()

            connection.connect()

            // 获取响应后检查协程状态
            ensureActive()

            val responseCode = connection.responseCode

            // 只接受成功的响应
            if (responseCode !in 200..299) {
                connection.disconnect()
                return@withContext false
            }

            val contentType = connection.contentType?.lowercase() ?: ""
            val contentDisposition = connection.getHeaderField("Content-Disposition")?.lowercase() ?: ""

            connection.disconnect()

            // 最后检查一次协程状态
            ensureActive()

            // 判断是否为下载链接
            isDownloadResource(url, contentType, contentDisposition)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 协程被取消，直接重新抛出异常
            throw e
        } catch (e: Exception) {
            // 网络错误时，如果URL包含download关键词，也认为可能是下载链接
            url.lowercase().contains("download")
        }
    }
    
    /**
     * 根据Content-Type和Content-Disposition判断是否为下载资源
     */
    private fun isDownloadResource(
        url: String,
        contentType: String,
        contentDisposition: String
    ): Boolean {
        // 检查Content-Disposition是否包含attachment
        if (contentDisposition.contains("attachment")) {
            return true
        }
        
        // 检查Content-Type是否为下载类型
        val downloadMimeTypes = listOf(
            "application/octet-stream",
            "application/zip",
            "application/x-zip",
            "application/x-rar",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/gzip",
            "application/vnd.android.package-archive",
            "application/x-msdownload",
            "application/x-apple-diskimage",
            "application/vnd.debian.binary-package",
            "application/pdf",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "video/",
            "audio/"
        )
        
        if (downloadMimeTypes.any { contentType.contains(it) }) {
            return true
        }
        
        // 如果URL明确包含download参数，也认为是下载链接
        if (url.lowercase().contains("download")) {
            return true
        }
        
        return false
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
        val withExt = cleanedCandidates.firstOrNull { url ->
            hasObviousDownloadExtension(url)
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

