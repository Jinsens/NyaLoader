package com.nyapass.loader.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadStatsSummary
import com.nyapass.loader.data.model.FileTypeStats

/**
 * 下载统计页面
 * 显示文件类型分布饼状图和下载大小统计
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    statistics: DownloadStatsSummary?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onClearStats: () -> Unit = {}
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // 清除确认对话框
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.clear_statistics_title)) },
            text = { Text(stringResource(R.string.clear_statistics_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearStats()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // 清除统计按钮
                    if (statistics != null && statistics.totalDownloads > 0) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_statistics)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (statistics == null || statistics.totalDownloads == 0) {
            EmptyStatisticsView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            StatisticsContent(
                statistics = statistics,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun EmptyStatisticsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_statistics_data),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatisticsContent(
    statistics: DownloadStatsSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 总览卡片
        SummaryCard(statistics)

        // 文件类型饼状图
        if (statistics.fileTypeStats.isNotEmpty()) {
            FileTypePieChartCard(statistics.fileTypeStats)
        }

        // 文件类型详情列表
        if (statistics.fileTypeStats.isNotEmpty()) {
            FileTypeDetailsCard(statistics.fileTypeStats)
        }
    }
}

@Composable
private fun SummaryCard(statistics: DownloadStatsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.statistics_overview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Download,
                    label = stringResource(R.string.total_downloads),
                    value = statistics.totalDownloads.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    label = stringResource(R.string.completed_downloads),
                    value = statistics.completedDownloads.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Default.Error,
                    label = stringResource(R.string.failed_downloads),
                    value = statistics.failedDownloads.toString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // 总下载大小
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.total_downloaded_size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatFileSize(statistics.totalDownloadedSize),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FileTypePieChartCard(fileTypeStats: List<FileTypeStats>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.file_type_distribution),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 饼状图
                PieChart(
                    data = fileTypeStats,
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.width(24.dp))

                // 图例
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fileTypeStats.forEachIndexed { index, stat ->
                        LegendItem(
                            color = getPieChartColor(index),
                            label = getFileTypeDisplayName(stat.type),
                            count = stat.count
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<FileTypeStats>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.count }.toFloat()
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "pie_animation"
    )

    LaunchedEffect(data) {
        animationProgress = 1f
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 40.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        data.forEachIndexed { index, stat ->
            val sweepAngle = (stat.count / total) * 360f * animatedProgress
            drawArc(
                color = getPieChartColor(index),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileTypeDetailsCard(fileTypeStats: List<FileTypeStats>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.file_type_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            fileTypeStats.forEachIndexed { index, stat ->
                FileTypeDetailItem(
                    stat = stat,
                    color = getPieChartColor(index),
                    totalCount = fileTypeStats.sumOf { it.count }
                )
            }
        }
    }
}

@Composable
private fun FileTypeDetailItem(
    stat: FileTypeStats,
    color: Color,
    totalCount: Int
) {
    val percentage = (stat.count.toFloat() / totalCount * 100).let {
        String.format("%.1f%%", it)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getFileTypeIcon(stat.type),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = getFileTypeDisplayName(stat.type),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${stat.count} ${stringResource(R.string.files_count_unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatFileSize(stat.totalSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 进度条
        LinearProgressIndicator(
            progress = { stat.count.toFloat() / totalCount },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = percentage,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 辅助函数 ====================

private fun getPieChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF4285F4), // 蓝色 - 视频
        Color(0xFF34A853), // 绿色 - 音频
        Color(0xFFFBBC05), // 黄色 - 图片
        Color(0xFFEA4335), // 红色 - 文档
        Color(0xFF9C27B0), // 紫色 - 压缩包
        Color(0xFF00BCD4), // 青色 - 应用
        Color(0xFF607D8B)  // 灰色 - 其他
    )
    return colors[index % colors.size]
}

@Composable
private fun getFileTypeDisplayName(type: String): String {
    return when (type) {
        "video" -> stringResource(R.string.file_type_video)
        "audio" -> stringResource(R.string.file_type_audio)
        "image" -> stringResource(R.string.file_type_image)
        "document" -> stringResource(R.string.file_type_document)
        "archive" -> stringResource(R.string.file_type_archive)
        "application" -> stringResource(R.string.file_type_application)
        else -> stringResource(R.string.file_type_other)
    }
}

private fun getFileTypeIcon(type: String): ImageVector {
    return when (type) {
        "video" -> Icons.Default.VideoFile
        "audio" -> Icons.Default.AudioFile
        "image" -> Icons.Default.Image
        "document" -> Icons.Default.Description
        "archive" -> Icons.Default.FolderZip
        "application" -> Icons.Default.Android
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val safeIndex = digitGroups.coerceIn(0, units.size - 1)
    return String.format("%.2f %s", bytes / Math.pow(1024.0, safeIndex.toDouble()), units[safeIndex])
}
