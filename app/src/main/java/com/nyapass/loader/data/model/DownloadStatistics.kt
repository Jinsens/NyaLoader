package com.nyapass.loader.data.model

import androidx.compose.runtime.Immutable

/**
 * 下载统计数据模型
 *
 * @author 小花生FMR
 * @version 2.4.0
 */

/**
 * 文件类型统计
 */
@Immutable
data class FileTypeStats(
    val type: String,
    val count: Int,
    val totalSize: Long
)

/**
 * 总体下载统计
 */
@Immutable
data class DownloadStatsSummary(
    val totalDownloads: Int,
    val completedDownloads: Int,
    val failedDownloads: Int,
    val totalDownloadedSize: Long,
    val fileTypeStats: List<FileTypeStats>
)

/**
 * 常见文件类型分类
 */
object FileTypeCategory {
    // 视频
    val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "m3u8"
    )

    // 音频
    val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "aac", "ogg", "wma", "m4a", "opus"
    )

    // 图片
    val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico", "heic", "heif"
    )

    // 文档
    val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt"
    )

    // 压缩包
    val ARCHIVE_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "cab"
    )

    // 应用程序
    val APP_EXTENSIONS = setOf(
        "apk", "exe", "msi", "dmg", "deb", "rpm", "appimage"
    )

    /**
     * 根据文件扩展名获取分类
     */
    fun getCategoryForExtension(extension: String): String {
        val ext = extension.lowercase()
        return when {
            VIDEO_EXTENSIONS.contains(ext) -> "video"
            AUDIO_EXTENSIONS.contains(ext) -> "audio"
            IMAGE_EXTENSIONS.contains(ext) -> "image"
            DOCUMENT_EXTENSIONS.contains(ext) -> "document"
            ARCHIVE_EXTENSIONS.contains(ext) -> "archive"
            APP_EXTENSIONS.contains(ext) -> "application"
            else -> "other"
        }
    }

    /**
     * 从文件名中提取扩展名
     */
    fun getExtensionFromFileName(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }
}
