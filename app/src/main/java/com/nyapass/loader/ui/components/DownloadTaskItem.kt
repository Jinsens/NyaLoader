package com.nyapass.loader.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.viewmodel.TaskWithProgress
import kotlin.math.pow

/**
 * 下载任务卡片组件
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadTaskItem(
    taskWithProgress: TaskWithProgress,
    onStart: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onOpenFile: ((String) -> Unit)? = null
) {
    val task = taskWithProgress.task
    val context = LocalContext.current
    
    // 使用key确保状态与任务ID关联，避免复用导致的问题
    // 完成的任务和失败的任务默认展开
    var expanded by remember(task.id) { 
        mutableStateOf(task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.FAILED) 
    }
    var showDeleteDialog by remember(task.id) { mutableStateOf(false) }
    
    // 使用rememberUpdatedState避免回调闭包问题
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnResume by rememberUpdatedState(onResume)
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnRetry by rememberUpdatedState(onRetry)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题和状态行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (task.status == DownloadStatus.COMPLETED && onOpenFile != null) {
                                Modifier.clickable { onOpenFile(task.filePath) }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = task.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.status == DownloadStatus.COMPLETED && onOpenFile != null) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "打开",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 状态指示器
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getStatusColor(task.status))
                        )
                        
                        Text(
                            text = getStatusText(task.status),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (task.status) {
                        DownloadStatus.PENDING -> {
                            IconButton(onClick = { currentOnStart(task.id) }) {
                                Icon(Icons.Default.PlayArrow, "开始")
                            }
                        }
                        DownloadStatus.RUNNING -> {
                            IconButton(onClick = { currentOnPause(task.id) }) {
                                Icon(Icons.Default.Pause, "暂停")
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = { currentOnResume(task.id) }) {
                                Icon(Icons.Default.PlayArrow, "继续")
                            }
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = { currentOnRetry(task.id) }) {
                                Icon(Icons.Default.Refresh, "重试")
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                "完成",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DownloadStatus.CANCELLED -> {
                            // 不显示按钮
                        }
                    }
                    
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (expanded) "收起" else "展开"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 进度条
            if (task.status != DownloadStatus.COMPLETED) {
                LinearProgressIndicator(
                    progress = { taskWithProgress.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.small),
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 进度信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatFileSize(task.downloadedSize)} / ${formatFileSize(task.totalSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (task.status == DownloadStatus.RUNNING) {
                    Text(
                        text = "${formatSpeed(taskWithProgress.speed)}/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "${String.format("%.1f", taskWithProgress.progress)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 展开的详细信息
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 如果任务失败，优先显示错误信息
                    if (task.status == DownloadStatus.FAILED && !task.errorMessage.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "错误",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "错误信息",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.errorMessage ?: "未知错误",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    CopyableDetailRow(
                        label = "URL",
                        value = task.url,
                        context = context,
                        onCopied = { 
                            Toast.makeText(context, "URL已复制", Toast.LENGTH_SHORT).show()
                        }
                    )
                    CopyableDetailRow(
                        label = "保存路径",
                        value = task.filePath,
                        context = context,
                        onCopied = { 
                            Toast.makeText(context, "保存路径已复制", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DetailRow("保存位置", if (task.saveToPublicDir) "公共Download目录" else "应用私有目录")
                    DetailRow("线程数", "${task.threadCount}")
                    DetailRow("支持断点续传", if (task.supportRange) "是" else "否")
                }
                
                // 完成后显示操作按钮
                if (task.status == DownloadStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("删除")
                        }
                        
                        Button(
                            onClick = { currentOnRetry(task.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("重新下载")
                        }
                    }
                } else if (task.status != DownloadStatus.COMPLETED) {
                    // 所有非完成状态都可以删除（包括下载中）
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (task.status == DownloadStatus.RUNNING) "停止并删除" else "删除")
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个下载任务吗？文件也将被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentOnDelete(task.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CopyableDetailRow(
    label: String,
    value: String,
    context: Context,
    onCopied: () -> Unit
) {
    Column(
        modifier = Modifier.combinedClickable(
            onClick = { },
            onLongClick = {
                // 复制到剪贴板
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, value)
                clipboard.setPrimaryClip(clip)
                onCopied()
            }
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "长按复制",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun getStatusColor(status: DownloadStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.outline
        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }
}

fun getStatusText(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.PENDING -> "等待中"
        DownloadStatus.RUNNING -> "下载中"
        DownloadStatus.PAUSED -> "已暂停"
        DownloadStatus.COMPLETED -> "已完成"
        DownloadStatus.FAILED -> "失败"
        DownloadStatus.CANCELLED -> "已取消"
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        "%.2f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}

fun formatSpeed(bytesPerSecond: Long): String {
    return formatFileSize(bytesPerSecond)
}

