package com.nyapass.loader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
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
    val errorMessage: String? = null // 错误信息（失败时）
)

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
