package com.nyapass.loader.ui.screen.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.viewmodel.TaskFilter

/**
 * 侧滑导航抽屉内容
 */
@Composable
fun NavigationDrawerContent(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
    onClearCompleted: () -> Unit,
    onClearFailed: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicenses: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .statusBarsPadding()  // 处理状态栏沉浸
        ) {
            // 抽屉头部
            DrawerHeader()
            
            HorizontalDivider()
            
            // 过滤选项
            FilterSection(
                currentFilter = currentFilter,
                onFilterSelected = onFilterSelected
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 清理操作
            ManagementSection(
                onClearCompleted = onClearCompleted,
                onClearFailed = onClearFailed
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 更多选项
            MoreOptionsSection(onOpenSettings = onOpenSettings)
        }
    }
}

/**
 * 抽屉头部
 */
@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.multithreaded_downloader),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 过滤选项部分
 */
@Composable
private fun FilterSection(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    Text(
        text = stringResource(R.string.task_filter),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
        label = { Text(stringResource(R.string.all)) },
        selected = currentFilter == TaskFilter.ALL,
        onClick = { onFilterSelected(TaskFilter.ALL) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Download, null) },
        label = { Text(stringResource(R.string.downloading)) },
        selected = currentFilter == TaskFilter.DOWNLOADING,
        onClick = { onFilterSelected(TaskFilter.DOWNLOADING) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.CheckCircle, null) },
        label = { Text(stringResource(R.string.completed)) },
        selected = currentFilter == TaskFilter.COMPLETED,
        onClick = { onFilterSelected(TaskFilter.COMPLETED) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Pause, null) },
        label = { Text(stringResource(R.string.paused)) },
        selected = currentFilter == TaskFilter.PAUSED,
        onClick = { onFilterSelected(TaskFilter.PAUSED) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Error, null) },
        label = { Text(stringResource(R.string.failed)) },
        selected = currentFilter == TaskFilter.FAILED,
        onClick = { onFilterSelected(TaskFilter.FAILED) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

/**
 * 任务管理部分
 */
@Composable
private fun ManagementSection(
    onClearCompleted: () -> Unit,
    onClearFailed: () -> Unit
) {
    Text(
        text = stringResource(R.string.task_management),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Clear, null) },
        label = { Text(stringResource(R.string.clear_completed)) },
        selected = false,
        onClick = onClearCompleted,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Clear, null) },
        label = { Text(stringResource(R.string.clear_failed)) },
        selected = false,
        onClick = onClearFailed,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

/**
 * 更多选项部分
 */
@Composable
private fun MoreOptionsSection(onOpenSettings: () -> Unit) {
    Text(
        text = stringResource(R.string.more),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Settings, null) },
        label = { Text(stringResource(R.string.settings)) },
        selected = false,
        onClick = onOpenSettings,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

