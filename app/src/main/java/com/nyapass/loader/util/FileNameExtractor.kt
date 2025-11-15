package com.nyapass.loader.util

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.net.URLDecoder

/**
 * 增强的文件名提取工具
 * 专门处理云存储（SharePoint、OneDrive等）的文件名提取问题
 */
object FileNameExtractor {

    private const val TAG = "FileNameExtractor"
    private const val MAX_FILENAME_LENGTH = 255 // Android 文件系统限制

    // 无效的文件名（通常是授权页面或API端点）
    private val INVALID_FILENAMES = setOf(
        "download.aspx",
        "download.php",
        "download.jsp",
        "download",
        "get",
        "api",
        "file",
        "content",
        "download.html",
        "download.htm"
    )

    // Windows 保留文件名
    private val WINDOWS_RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * 智能提取文件名
     *
     * @param url 下载URL
     * @param contentDisposition Content-Disposition响应头
     * @param mimeType MIME类型
     * @return 提取的文件名
     */
    fun extractFileName(
        url: String,
        contentDisposition: String?,
        mimeType: String?
    ): String {
        Log.d(TAG, "开始提取文件名")
        Log.d(TAG, "  URL: $url")
        Log.d(TAG, "  Content-Disposition: $contentDisposition")
        Log.d(TAG, "  MIME Type: $mimeType")

        // 1. 尝试从 Content-Disposition 提取
        contentDisposition?.let { disposition ->
            val fileName = extractFromContentDisposition(disposition)
            if (fileName != null && isValidFileName(fileName)) {
                val sanitized = sanitizeFileName(fileName)
                val truncated = truncateFileName(sanitized)
                Log.i(TAG, "从 Content-Disposition 提取到文件名: $truncated")
                return truncated
            }
        }

        // 2. 尝试从 URL 提取有效的文件名
        val urlFileName = extractFromUrl(url)
        if (urlFileName != null && isValidFileName(urlFileName)) {
            val sanitized = sanitizeFileName(urlFileName)
            val truncated = truncateFileName(sanitized)
            Log.i(TAG, "从 URL 提取到文件名: $truncated")
            return truncated
        }

        // 3. 使用 MIME 类型生成默认文件名
        val defaultFileName = generateDefaultFileName(mimeType)
        Log.w(TAG, "无法提取有效文件名，使用默认值: $defaultFileName")
        return defaultFileName
    }

    /**
     * 清理文件名，移除危险字符和路径遍历
     * 防止路径遍历攻击和文件系统错误
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            // 移除控制字符 (ASCII 0-31 和 127)
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
            // 替换路径分隔符
            .replace(Regex("[/\\\\]"), "_")
            // 移除父目录引用
            .replace("..", "_")
            // 替换 Windows/Android 非法字符
            .replace(Regex("[<>:\"|?*]"), "_")
            // 清理首尾空格和点
            .trim()
            .trim('.')
            // 确保不为空
            .ifEmpty { "file" }
    }

    /**
     * 截断超长文件名
     * Android 文件系统限制文件名最大 255 字节
     */
    private fun truncateFileName(fileName: String): String {
        // 检查字节长度
        if (fileName.toByteArray().size <= MAX_FILENAME_LENGTH) {
            return fileName
        }

        Log.w(TAG, "文件名过长，需要截断: $fileName")

        val extension = fileName.substringAfterLast('.', "")
        val baseName = fileName.substringBeforeLast('.', fileName)

        // 保留扩展名，截断基础名称
        var truncated = baseName
        val extWithDot = if (extension.isNotEmpty()) ".$extension" else ""

        while ((truncated + extWithDot).toByteArray().size > MAX_FILENAME_LENGTH && truncated.isNotEmpty()) {
            truncated = truncated.dropLast(1)
        }

        val result = truncated + extWithDot
        Log.i(TAG, "截断后的文件名: $result")
        return result
    }

    /**
     * 从 Content-Disposition 响应头提取文件名
     * 支持多种格式：
     * - Content-Disposition: attachment; filename="file.pdf"
     * - Content-Disposition: attachment; filename*=UTF-8''file.pdf
     * - Content-Disposition: inline; filename=file.pdf
     */
    private fun extractFromContentDisposition(disposition: String): String? {
        try {
            // 方法1: 匹配 filename*=UTF-8''... (RFC 5987)
            val utf8Pattern = Regex("""filename\*=(?:UTF-8|utf-8)''(.+?)(?:;|$)""", RegexOption.IGNORE_CASE)
            utf8Pattern.find(disposition)?.let { matchResult ->
                val encodedFileName = matchResult.groupValues[1].trim()
                return try {
                    URLDecoder.decode(encodedFileName, "UTF-8")
                } catch (e: Exception) {
                    encodedFileName
                }
            }

            // 方法2: 匹配 filename="..." 或 filename='...'
            val quotedPattern = Regex("""filename\s*=\s*["'](.+?)["']""", RegexOption.IGNORE_CASE)
            quotedPattern.find(disposition)?.let { matchResult ->
                return matchResult.groupValues[1].trim()
            }

            // 方法3: 匹配 filename=... (无引号)
            val unquotedPattern = Regex("""filename\s*=\s*([^;]+)""", RegexOption.IGNORE_CASE)
            unquotedPattern.find(disposition)?.let { matchResult ->
                return matchResult.groupValues[1].trim()
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "解析 Content-Disposition 失败: ${e.message}", e)
            return null
        }
    }

    /**
     * 从 URL 提取文件名
     * 只提取有明显文件扩展名的文件名
     */
    private fun extractFromUrl(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val path = uri.path ?: return null

            // 提取路径中的最后一部分
            val segments = path.split('/').filter { it.isNotBlank() }
            if (segments.isEmpty()) return null

            val lastSegment = segments.last()

            // 检查是否有有效的文件扩展名
            if (hasValidExtension(lastSegment)) {
                // URL解码
                return try {
                    URLDecoder.decode(lastSegment, "UTF-8")
                } catch (e: Exception) {
                    lastSegment
                }
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "从 URL 提取文件名失败: ${e.message}", e)
            return null
        }
    }

    /**
     * 检查文件名是否有有效的扩展名
     */
    private fun hasValidExtension(fileName: String): Boolean {
        val lowerName = fileName.lowercase()

        // 常见的文件扩展名
        val validExtensions = listOf(
            // 压缩包
            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz",
            // 安装包
            ".apk", ".exe", ".dmg", ".pkg", ".deb", ".rpm", ".msi",
            // 文档
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".rtf", ".odt", ".ods", ".odp",
            // 视频
            ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v",
            // 音频
            ".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".wma",
            // 图片
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico",
            // 镜像
            ".iso", ".img", ".bin",
            // 代码
            ".java", ".kt", ".py", ".js", ".ts", ".cpp", ".c", ".h",
            ".json", ".xml", ".yaml", ".yml", ".md",
            // 其他
            ".torrent", ".sql", ".db", ".csv"
        )

        return validExtensions.any { lowerName.endsWith(it) }
    }

    /**
     * 检查文件名是否有效
     */
    private fun isValidFileName(fileName: String): Boolean {
        val lowerName = fileName.lowercase()

        // 检查是否在无效文件名列表中
        if (INVALID_FILENAMES.contains(lowerName)) {
            Log.d(TAG, "文件名 '$fileName' 在无效列表中")
            return false
        }

        // 检查 Windows 保留名称
        val baseName = fileName.substringBeforeLast('.').uppercase()
        if (WINDOWS_RESERVED_NAMES.contains(baseName)) {
            Log.d(TAG, "文件名 '$fileName' 是 Windows 保留名称")
            return false
        }

        // 文件名太短
        if (fileName.length < 3) {
            Log.d(TAG, "文件名 '$fileName' 太短")
            return false
        }

        // 必须有扩展名
        if (!fileName.contains('.')) {
            Log.d(TAG, "文件名 '$fileName' 没有扩展名")
            return false
        }

        return true
    }

    /**
     * 根据 MIME 类型生成默认文件名
     */
    private fun generateDefaultFileName(mimeType: String?): String {
        val timestamp = System.currentTimeMillis()

        // 根据 MIME 类型获取扩展名
        val extension = mimeType?.let { mime ->
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        } ?: getExtensionFromMimeTypeFallback(mimeType)

        return if (extension != null) {
            "download_$timestamp.$extension"
        } else {
            "download_$timestamp.bin"
        }
    }

    /**
     * MIME类型到扩展名的映射（备用方案）
     */
    private fun getExtensionFromMimeTypeFallback(mimeType: String?): String? {
        return when {
            mimeType == null -> null
            mimeType.contains("pdf") -> "pdf"
            mimeType.contains("zip") -> "zip"
            mimeType.contains("rar") -> "rar"
            mimeType.contains("7z") -> "7z"
            mimeType.contains("apk") || mimeType.contains("android.package") -> "apk"
            mimeType.contains("msword") -> "doc"
            mimeType.contains("officedocument.word") -> "docx"
            mimeType.contains("ms-excel") -> "xls"
            mimeType.contains("officedocument.spreadsheet") -> "xlsx"
            mimeType.contains("ms-powerpoint") -> "ppt"
            mimeType.contains("officedocument.presentation") -> "pptx"
            mimeType.startsWith("video/mp4") -> "mp4"
            mimeType.startsWith("video/") -> "mp4"
            mimeType.startsWith("audio/mpeg") -> "mp3"
            mimeType.startsWith("audio/") -> "mp3"
            mimeType.startsWith("image/jpeg") -> "jpg"
            mimeType.startsWith("image/png") -> "png"
            mimeType.startsWith("image/") -> "jpg"
            mimeType.contains("text/plain") -> "txt"
            mimeType.contains("text/html") -> "html"
            mimeType.contains("application/json") -> "json"
            mimeType.contains("application/xml") -> "xml"
            else -> null
        }
    }
}
