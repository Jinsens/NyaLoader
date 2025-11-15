package com.nyapass.loader.ui.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.util.PathFormatter

/**
 * 保存目录选择区域组件
 * 支持选择临时保存目录
 *
 * @author 小花生FMR
 * @version 2.0.0
 */
@Composable
fun SaveDirectorySection(
    customPath: String?,
    onSelectFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayPath = remember(customPath) {
        PathFormatter.formatForDisplay(context, customPath)
    }

    OutlinedCard(
        onClick = onSelectFolder,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.temp_save_directory),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = displayPath ?: customPath ?: stringResource(R.string.click_to_select_temp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (customPath != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 下载提示信息区域
 */
@Composable
fun DownloadHintsSection(
    hasCustomPath: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 多线程下载提示
            HintRow(
                text = stringResource(R.string.multithread_hint),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // 临时目录提示
            if (hasCustomPath) {
                HintRow(
                    text = stringResource(R.string.temp_dir_hint),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 单行提示
 */
@Composable
private fun HintRow(
    text: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tint
        )
    }
}
