
package com.nyapass.loader.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.viewmodel.TaskWithProgress
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.model.getDisplayLocation
import com.nyapass.loader.util.ShareHelper
import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * 下载任务卡片组件
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadTaskItem(
    taskWithProgress: TaskWithProgress,
    onStart: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onOpenFile: ((DownloadTask) -> Unit)? = null,
    onUpdateTags: ((Long, List<Long>) -> Unit)? = null,
    onCreateTag: (() -> Unit)? = null,
    availableTags: List<DownloadTag> = emptyList(),
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {}
) {
    val task = taskWithProgress.task
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // 使用key确保状态与任务ID关联，避免复用导致的问题
    // 完成的任务和失败的任务默认展开
    var expanded by rememberSaveable(task.id) {
        mutableStateOf(task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.FAILED)
    }
    var showDeleteDialog by remember(task.id) { mutableStateOf(false) }
    var showContextMenu by remember(task.id) { mutableStateOf(false) }
    var showTagSelectionDialog by remember(task.id) { mutableStateOf(false) }

    // 下载完成动画状态
    var showCompletionAnimation by remember(task.id) { mutableStateOf(false) }
    var previousStatus by remember(task.id) { mutableStateOf(task.status) }

    // 检测状态从 RUNNING 变为 COMPLETED 时触发动画
    LaunchedEffect(task.status) {
        if (previousStatus == DownloadStatus.RUNNING && task.status == DownloadStatus.COMPLETED) {
            showCompletionAnimation = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2000) // 动画显示2秒
            showCompletionAnimation = false
        }
        previousStatus = task.status
    }

    // 使用rememberUpdatedState避免回调闭包问题
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnResume by rememberUpdatedState(onResume)
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnRetry by rememberUpdatedState(onRetry)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) {
                        onToggleSelection(task.id)
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!isMultiSelectMode) {
                        onLongPress(task.id)
                    }
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        } else {
            null
        }
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
                // 多选模式下显示选择圆点
                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(16.dp)
                            .then(
                                if (!isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onToggleSelection(task.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (task.status == DownloadStatus.COMPLETED && onOpenFile != null && !isMultiSelectMode) {
                                Modifier.clickable { onOpenFile(task) }
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
                                contentDescription = stringResource(R.string.download_open_file),
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
                    // 状态控制按钮（多选模式下不显示）
                    if (!isMultiSelectMode) {
                        when (task.status) {
                            DownloadStatus.PENDING -> {
                                IconButton(onClick = { currentOnStart(task.id) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.download_status_pending)
                                    )
                                }
                            }
                            DownloadStatus.RUNNING -> {
                                IconButton(onClick = { currentOnPause(task.id) }) {
                                    Icon(
                                        Icons.Default.Pause,
                                        contentDescription = stringResource(R.string.download_status_running)
                                    )
                                }
                            }
                            DownloadStatus.PAUSED -> {
                                IconButton(onClick = { currentOnResume(task.id) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.download_status_paused)
                                    )
                                }
                            }
                            DownloadStatus.FAILED -> {
                                IconButton(onClick = { currentOnRetry(task.id) }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.download_status_failed)
                                    )
                                }
                            }
                            DownloadStatus.COMPLETED -> {
                                // 完成动画
                                Box(contentAlignment = Alignment.Center) {
                                    // 普通完成图标
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.download_status_completed),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    // 完成时的庆祝动画 - 使用显式调用避免 RowScope 版本
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showCompletionAnimation,
                                        enter = scaleIn(
                                            initialScale = 0.3f,
                                            animationSpec = tween(300)
                                        ) + fadeIn(animationSpec = tween(300)),
                                        exit = scaleOut(
                                            targetScale = 1.5f,
                                            animationSpec = tween(500)
                                        ) + fadeOut(animationSpec = tween(500))
                                    ) {
                                        Icon(
                                            Icons.Default.Celebration,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            DownloadStatus.CANCELLED -> {
                                // 不显示按钮
                            }
                        }
                    }
                    
                    // 展开/收起按钮（始终显示）
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
            }
            
            if (taskWithProgress.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    taskWithProgress.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag.name) },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(tag.color))
                                )
                            }
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
                                        contentDescription = stringResource(R.string.download_error_label),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.download_error_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.errorMessage ?: stringResource(R.string.download_error_unknown),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    CopyableDetailRow(
                        label = stringResource(R.string.download_url_label),
                        value = task.url,
                        context = context,
                        onCopied = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.download_url_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    CopyableDetailRow(
                        label = stringResource(R.string.download_save_path_label),
                        value = task.getDisplayLocation(),
                        context = context,
                        onCopied = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.download_path_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    DetailRow(
                        label = stringResource(R.string.download_location_label),
                        value = if (task.saveToPublicDir) {
                            stringResource(R.string.save_location_public)
                        } else {
                            stringResource(R.string.save_location_private)
                        }
                    )
                    DetailRow(
                        label = stringResource(R.string.download_thread_count_label),
                        value = "${task.threadCount}"
                    )
                    DetailRow(
                        label = stringResource(R.string.download_support_range_label),
                        value = if (task.supportRange) {
                            stringResource(R.string.yes)
                        } else {
                            stringResource(R.string.no)
                        }
                    )
                }
                
                // 完成后显示操作按钮
                if (task.status == DownloadStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 第一行：分享按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 分享链接
                            OutlinedButton(
                                onClick = {
                                    ShareHelper.shareDownloadLink(context, task.url, task.fileName)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.share_link))
                            }

                            // 分享文件
                            OutlinedButton(
                                onClick = {
                                    val success = ShareHelper.shareFile(context, task)
                                    if (!success) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.share_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.share_file))
                            }
                        }

                        // 第二行：编辑标签按钮
                        if (onUpdateTags != null) {
                            Button(
                                onClick = { showTagSelectionDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.edit_tags))
                            }
                        }

                        // 第三行：删除和重新下载
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
                                Text(stringResource(R.string.delete))
                            }

                            Button(
                                onClick = { currentOnRetry(task.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.download_redownload))
                            }
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
                        Text(
                            text = if (task.status == DownloadStatus.RUNNING) {
                                stringResource(R.string.download_stop_and_delete)
                            } else {
                                stringResource(R.string.delete)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 长按上下文菜单
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false }
    ) {
        // 重新下载选项（所有状态都可用）
        DropdownMenuItem(
            text = { 
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(
                        if (task.status == DownloadStatus.COMPLETED) {
                            stringResource(R.string.download_redownload)
                        } else {
                            stringResource(R.string.retry)
                        }
                    )
                }
            },
            onClick = {
                currentOnRetry(task.id)
                showContextMenu = false
            }
        )
        
        // 删除选项
        DropdownMenuItem(
            text = { 
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (task.status == DownloadStatus.RUNNING) {
                            stringResource(R.string.download_stop_and_delete)
                        } else {
                            stringResource(R.string.delete)
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            onClick = {
                showContextMenu = false
                showDeleteDialog = true
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.download_delete_task_title)) },
            text = { Text(stringResource(R.string.download_delete_task_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentOnDelete(task.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 标签选择对话框
    if (showTagSelectionDialog && onUpdateTags != null) {
        TaskTagSelectionDialog(
            tags = availableTags,
            selectedTagIds = taskWithProgress.tags.map { it.id },
            onDismiss = { showTagSelectionDialog = false },
            onConfirm = { selectedTagIds ->
                onUpdateTags(task.id, selectedTagIds)
                showTagSelectionDialog = false
            },
            onCreateTag = onCreateTag
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
                contentDescription = stringResource(R.string.copy_long_press_hint),
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

fun formatSpeed(bytesPerSecond: Long): String {
    return formatFileSize(bytesPerSecond)
}

/**
 * 可滑动操作的下载任务卡片包装组件
 * 支持两阶段滑动确认：
 * 1. 第一次滑动 → 显示操作按钮（删除、重新下载）
 * 2. 点击按钮 → 执行真正的操作
 *
 * 注意：展开状态下禁用滑动，避免与展开内容交互冲突
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeableDownloadTaskItem(
    taskWithProgress: TaskWithProgress,
    onStart: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onOpenFile: ((DownloadTask) -> Unit)? = null,
    onUpdateTags: ((Long, List<Long>) -> Unit)? = null,
    onCreateTag: (() -> Unit)? = null,
    availableTags: List<DownloadTag> = emptyList(),
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {}
) {
    val task = taskWithProgress.task
    val haptic = LocalHapticFeedback.current

    // 跟踪展开状态 - 与 DownloadTaskItem 内部的 expanded 状态同步
    // 完成和失败的任务默认展开
    var isExpanded by rememberSaveable(task.id) {
        mutableStateOf(task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.FAILED)
    }

    // 滑动状态：是否显示操作按钮
    var isSwipeRevealed by rememberSaveable(task.id) { mutableStateOf(false) }

    // 删除确认对话框状态
    var showDeleteConfirmDialog by remember(task.id) { mutableStateOf(false) }

    // 删除确认对话框
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.download_delete_task_title)) },
            text = { Text(stringResource(R.string.download_delete_task_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(task.id)
                        showDeleteConfirmDialog = false
                        isSwipeRevealed = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 多选模式或展开状态下禁用滑动，恢复正常显示
    if (isMultiSelectMode || isExpanded) {
        // 重置滑动状态
        LaunchedEffect(isMultiSelectMode, isExpanded) {
            isSwipeRevealed = false
        }

        DownloadTaskItemWithExpandCallback(
            taskWithProgress = taskWithProgress,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onDelete = onDelete,
            onRetry = onRetry,
            onOpenFile = onOpenFile,
            onUpdateTags = onUpdateTags,
            onCreateTag = onCreateTag,
            availableTags = availableTags,
            isMultiSelectMode = isMultiSelectMode,
            isSelected = isSelected,
            onLongPress = onLongPress,
            onToggleSelection = onToggleSelection,
            isExpanded = isExpanded,
            onExpandChanged = { isExpanded = it }
        )
    } else {
        // 使用自定义的两阶段滑动逻辑
        TwoStageSwipeableCard(
            isRevealed = isSwipeRevealed,
            onRevealChanged = { revealed ->
                if (revealed && !isSwipeRevealed) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                isSwipeRevealed = revealed
            },
            actionButtons = {
                // 操作按钮区域
                SwipeActionButtons(
                    task = task,
                    onDelete = {
                        showDeleteConfirmDialog = true
                    },
                    onRetry = {
                        onRetry(task.id)
                        isSwipeRevealed = false
                    }
                )
            }
        ) {
            DownloadTaskItemWithExpandCallback(
                taskWithProgress = taskWithProgress,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onDelete = onDelete,
                onRetry = onRetry,
                onOpenFile = onOpenFile,
                onUpdateTags = onUpdateTags,
                onCreateTag = onCreateTag,
                availableTags = availableTags,
                isMultiSelectMode = isMultiSelectMode,
                isSelected = isSelected,
                onLongPress = onLongPress,
                onToggleSelection = onToggleSelection,
                isExpanded = isExpanded,
                onExpandChanged = { isExpanded = it }
            )
        }
    }
}

/**
 * 两阶段滑动卡片组件
 * 第一次滑动显示操作按钮，点击按钮执行操作
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TwoStageSwipeableCard(
    isRevealed: Boolean,
    onRevealChanged: (Boolean) -> Unit,
    actionButtons: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val actionButtonsWidth = 160.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionButtonsWidthPx = with(density) { actionButtonsWidth.toPx() }

    // 使用 Animatable 控制滑动偏移
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }

    // 当 isRevealed 变化时，动画到目标位置
    LaunchedEffect(isRevealed) {
        val targetOffset = if (isRevealed) -actionButtonsWidthPx else 0f
        offsetX.animateTo(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = 200)
        )
    }

    // 拖拽状态
    val draggableState = rememberDraggableState { delta ->
        kotlinx.coroutines.runBlocking {
            val newOffset = (offsetX.value + delta).coerceIn(-actionButtonsWidthPx, 0f)
            offsetX.snapTo(newOffset)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 操作按钮背景层（始终在底部）
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
            // 只有在滑动时才显示操作按钮
            if (offsetX.value < 0) {
                actionButtons()
            }
        }

        // 前景卡片层（可滑动）
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        // 根据滑动距离和速度决定最终状态
                        val threshold = actionButtonsWidthPx * 0.4f
                        val shouldReveal = if (kotlin.math.abs(velocity) > 500f) {
                            velocity < 0 // 快速左滑则显示
                        } else {
                            kotlin.math.abs(offsetX.value) > threshold
                        }
                        onRevealChanged(shouldReveal)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // 点击卡片时收起操作按钮
                    if (isRevealed) {
                        onRevealChanged(false)
                    }
                }
        ) {
            content()
        }
    }
}

/**
 * 滑动操作按钮组件
 */
@Composable
private fun SwipeActionButtons(
    task: DownloadTask,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
        horizontalArrangement = Arrangement.Start
    ) {
        // 重新下载按钮
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onRetry),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(
                        if (task.status == DownloadStatus.COMPLETED) R.string.download_redownload
                        else R.string.retry
                    ),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (task.status == DownloadStatus.COMPLETED) R.string.download_redownload
                        else R.string.retry
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
            }
        }

        // 删除按钮
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.delete),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 带展开回调的下载任务卡片 - 内部组件，用于同步展开状态
 * 展开状态由外部控制，避免状态不同步问题
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DownloadTaskItemWithExpandCallback(
    taskWithProgress: TaskWithProgress,
    onStart: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onOpenFile: ((DownloadTask) -> Unit)? = null,
    onUpdateTags: ((Long, List<Long>) -> Unit)? = null,
    onCreateTag: (() -> Unit)? = null,
    availableTags: List<DownloadTag> = emptyList(),
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    isExpanded: Boolean,
    onExpandChanged: (Boolean) -> Unit
) {
    val task = taskWithProgress.task
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // 使用外部传入的展开状态
    val expanded = isExpanded

    var showDeleteDialog by remember(task.id) { mutableStateOf(false) }
    var showContextMenu by remember(task.id) { mutableStateOf(false) }
    var showTagSelectionDialog by remember(task.id) { mutableStateOf(false) }

    // 下载完成动画状态
    var showCompletionAnimation by remember(task.id) { mutableStateOf(false) }
    var previousStatus by remember(task.id) { mutableStateOf(task.status) }

    // 检测状态从 RUNNING 变为 COMPLETED 时触发动画
    LaunchedEffect(task.status) {
        if (previousStatus == DownloadStatus.RUNNING && task.status == DownloadStatus.COMPLETED) {
            showCompletionAnimation = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2000) // 动画显示2秒
            showCompletionAnimation = false
        }
        previousStatus = task.status
    }

    // 使用rememberUpdatedState避免回调闭包问题
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnResume by rememberUpdatedState(onResume)
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnRetry by rememberUpdatedState(onRetry)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) {
                        onToggleSelection(task.id)
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!isMultiSelectMode) {
                        onLongPress(task.id)
                    }
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        } else {
            null
        }
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
                // 多选模式下显示选择圆点
                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(16.dp)
                            .then(
                                if (!isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onToggleSelection(task.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (task.status == DownloadStatus.COMPLETED && onOpenFile != null && !isMultiSelectMode) {
                                Modifier.clickable { onOpenFile(task) }
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
                                contentDescription = stringResource(R.string.download_open_file),
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
                    // 状态控制按钮（多选模式下不显示）
                    if (!isMultiSelectMode) {
                        when (task.status) {
                            DownloadStatus.PENDING -> {
                                IconButton(onClick = { currentOnStart(task.id) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.download_status_pending)
                                    )
                                }
                            }
                            DownloadStatus.RUNNING -> {
                                IconButton(onClick = { currentOnPause(task.id) }) {
                                    Icon(
                                        Icons.Default.Pause,
                                        contentDescription = stringResource(R.string.download_status_running)
                                    )
                                }
                            }
                            DownloadStatus.PAUSED -> {
                                IconButton(onClick = { currentOnResume(task.id) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.download_status_paused)
                                    )
                                }
                            }
                            DownloadStatus.FAILED -> {
                                IconButton(onClick = { currentOnRetry(task.id) }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.download_status_failed)
                                    )
                                }
                            }
                            DownloadStatus.COMPLETED -> {
                                // 完成动画
                                Box(contentAlignment = Alignment.Center) {
                                    // 普通完成图标
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.download_status_completed),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    // 完成时的庆祝动画
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showCompletionAnimation,
                                        enter = scaleIn(
                                            initialScale = 0.3f,
                                            animationSpec = tween(300)
                                        ) + fadeIn(animationSpec = tween(300)),
                                        exit = scaleOut(
                                            targetScale = 1.5f,
                                            animationSpec = tween(500)
                                        ) + fadeOut(animationSpec = tween(500))
                                    ) {
                                        Icon(
                                            Icons.Default.Celebration,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            DownloadStatus.CANCELLED -> {
                                // 不显示按钮
                            }
                        }
                    }

                    // 展开/收起按钮（始终显示）
                    IconButton(onClick = { onExpandChanged(!expanded) }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
            }

            if (taskWithProgress.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    taskWithProgress.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag.name) },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(tag.color))
                                )
                            }
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
                                        contentDescription = stringResource(R.string.download_error_label),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.download_error_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.errorMessage ?: stringResource(R.string.download_error_unknown),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    CopyableDetailRow(
                        label = stringResource(R.string.download_url_label),
                        value = task.url,
                        context = context,
                        onCopied = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.download_url_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    CopyableDetailRow(
                        label = stringResource(R.string.download_save_path_label),
                        value = task.getDisplayLocation(),
                        context = context,
                        onCopied = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.download_path_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    DetailRow(
                        label = stringResource(R.string.download_location_label),
                        value = if (task.saveToPublicDir) {
                            stringResource(R.string.save_location_public)
                        } else {
                            stringResource(R.string.save_location_private)
                        }
                    )
                    DetailRow(
                        label = stringResource(R.string.download_thread_count_label),
                        value = "${task.threadCount}"
                    )
                    DetailRow(
                        label = stringResource(R.string.download_support_range_label),
                        value = if (task.supportRange) {
                            stringResource(R.string.yes)
                        } else {
                            stringResource(R.string.no)
                        }
                    )
                }

                // 完成后显示操作按钮
                if (task.status == DownloadStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 第一行：分享按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 分享链接
                            OutlinedButton(
                                onClick = {
                                    ShareHelper.shareDownloadLink(context, task.url, task.fileName)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.share_link))
                            }

                            // 分享文件
                            OutlinedButton(
                                onClick = {
                                    val success = ShareHelper.shareFile(context, task)
                                    if (!success) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.share_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.share_file))
                            }
                        }

                        // 第二行：编辑标签按钮
                        if (onUpdateTags != null) {
                            Button(
                                onClick = { showTagSelectionDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.edit_tags))
                            }
                        }

                        // 第三行：删除和重新下载
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
                                Text(stringResource(R.string.delete))
                            }

                            Button(
                                onClick = { currentOnRetry(task.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.download_redownload))
                            }
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
                        Text(
                            text = if (task.status == DownloadStatus.RUNNING) {
                                stringResource(R.string.download_stop_and_delete)
                            } else {
                                stringResource(R.string.delete)
                            }
                        )
                    }
                }
            }
        }
    }

    // 长按上下文菜单
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false }
    ) {
        // 重新下载选项（所有状态都可用）
        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(
                        if (task.status == DownloadStatus.COMPLETED) {
                            stringResource(R.string.download_redownload)
                        } else {
                            stringResource(R.string.retry)
                        }
                    )
                }
            },
            onClick = {
                currentOnRetry(task.id)
                showContextMenu = false
            }
        )

        // 删除选项
        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (task.status == DownloadStatus.RUNNING) {
                            stringResource(R.string.download_stop_and_delete)
                        } else {
                            stringResource(R.string.delete)
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            onClick = {
                showContextMenu = false
                showDeleteDialog = true
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.download_delete_task_title)) },
            text = { Text(stringResource(R.string.download_delete_task_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentOnDelete(task.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 标签选择对话框
    if (showTagSelectionDialog && onUpdateTags != null) {
        TaskTagSelectionDialog(
            tags = availableTags,
            selectedTagIds = taskWithProgress.tags.map { it.id },
            onDismiss = { showTagSelectionDialog = false },
            onConfirm = { selectedTagIds ->
                onUpdateTags(task.id, selectedTagIds)
                showTagSelectionDialog = false
            },
            onCreateTag = onCreateTag
        )
    }
}


