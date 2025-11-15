package com.nyapass.loader.ui.components.task

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadStatus

/**
 * 下载任务工具函数
 */

/**
 * 获取下载状态对应的颜色
 */
@Composable
fun getStatusColor(status: DownloadStatus): Color {
    return when (status) {
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.outline
        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }
}

/**
 * 获取下载状态对应的文字
 */
@Composable
fun getStatusText(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.PENDING -> stringResource(R.string.download_status_pending)
        DownloadStatus.RUNNING -> stringResource(R.string.download_status_running)
        DownloadStatus.PAUSED -> stringResource(R.string.download_status_paused)
        DownloadStatus.COMPLETED -> stringResource(R.string.download_status_completed)
        DownloadStatus.FAILED -> stringResource(R.string.download_status_failed)
        DownloadStatus.CANCELLED -> stringResource(R.string.download_status_cancelled)
    }
}

/**
 * 格式化文件大小
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val unit = 1024.0
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    
    while (value >= unit && index < units.lastIndex) {
        value /= unit
        index++
    }
    
    return String.format("%.2f %s", value, units[index])
}

/**
 * 格式化下载速度
 */
fun formatSpeed(bytesPerSecond: Long): String {
    return formatFileSize(bytesPerSecond)
}
