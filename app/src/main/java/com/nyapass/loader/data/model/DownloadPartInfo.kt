package com.nyapass.loader.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 下载分片信息，用于多线程下载和断点续传
 */
@Immutable
@Entity(tableName = "download_parts")
data class DownloadPartInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,           // 关联的下载任务ID
    val partIndex: Int,         // 分片索引
    val startByte: Long,        // 起始字节位置
    val endByte: Long,          // 结束字节位置
    val downloadedByte: Long = 0, // 已下载字节位置
    val isCompleted: Boolean = false // 是否完成
)

// 计算分片进度
fun DownloadPartInfo.getPartProgress(): Float {
    val totalSize = endByte - startByte + 1
    val downloaded = downloadedByte - startByte
    return if (totalSize > 0) {
        (downloaded.toFloat() / totalSize.toFloat() * 100).coerceIn(0f, 100f)
    } else {
        0f
    }
}
