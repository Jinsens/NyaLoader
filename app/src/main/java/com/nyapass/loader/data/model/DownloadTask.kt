package com.nyapass.loader.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "download_tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["priority"])
    ]
)
data class DownloadTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String? = null,
    val destinationUri: String? = null, // SAF 目录 URI（需要复制）
    val finalContentUri: String? = null, // SAF 文件 URI（复制完成后）
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val threadCount: Int = 4,
    val supportRange: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val speed: Long = 0, // 下载速度 bytes/s
    val md5: String? = null,
    val saveToPublicDir: Boolean = true, // true=公共Download目录, false=应用私有目录
    val userAgent: String? = null, // 自定义User-Agent
    val cookie: String? = null, // Cookie 请求头
    val referer: String? = null, // Referer 请求头
    val customHeaders: String? = null, // 自定义请求头 (JSON格式: {"key":"value"})
    val errorMessage: String? = null, // 错误信息（失败时）
    val priority: Int = DownloadPriority.NORMAL.value // 下载优先级
)

/**
 * 下载优先级枚举
 */
enum class DownloadPriority(val value: Int, val displayName: String) {
    LOW(0, "低"),
    NORMAL(1, "普通"),
    HIGH(2, "高");

    companion object {
        fun fromValue(value: Int): DownloadPriority {
            return entries.find { it.value == value } ?: NORMAL
        }
    }
}

enum class DownloadStatus {
    PENDING,    // 等待中
    RUNNING,    // 下载中
    PAUSED,     // 已暂停
    COMPLETED,  // 已完成
    FAILED,     // 失败
    CANCELLED   // 已取消
}

// 计算进度百分比
fun DownloadTask.getProgress(): Float {
    return if (totalSize > 0) {
        (downloadedSize.toFloat() / totalSize.toFloat() * 100).coerceIn(0f, 100f)
    } else {
        0f
    }
}

fun DownloadTask.getDisplayLocation(): String {
    return finalContentUri ?: filePath
}
