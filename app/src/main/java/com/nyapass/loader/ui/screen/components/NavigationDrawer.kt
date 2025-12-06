package com.nyapass.loader.ui.screen.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.viewmodel.TaskFilter

/**
 * 侧边导航内容
 */
@Composable
fun NavigationDrawerContent(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
    onClearCompleted: () -> Unit,
    onClearFailed: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTagManager: () -> Unit,
    onOpenWebView: () -> Unit,
    onOpenStatistics: () -> Unit = {}
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            DrawerHeader()

            HorizontalDivider()

            FilterSection(
                currentFilter = currentFilter,
                onFilterSelected = onFilterSelected
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ManagementSection(
                onClearCompleted = onClearCompleted,
                onClearFailed = onClearFailed,
                onOpenTagManager = onOpenTagManager
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ToolsSection(
                onOpenWebView = onOpenWebView,
                onOpenStatistics = onOpenStatistics
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MoreOptionsSection(
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun DrawerHeader() {
    androidx.compose.foundation.layout.Box(
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
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun ManagementSection(
    onClearCompleted: () -> Unit,
    onClearFailed: () -> Unit,
    onOpenTagManager: () -> Unit
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

    NavigationDrawerItem(
        icon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
        label = { Text(stringResource(R.string.tag_management_title)) },
        selected = false,
        onClick = onOpenTagManager,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun ToolsSection(
    onOpenWebView: () -> Unit,
    onOpenStatistics: () -> Unit
) {
    Text(
        text = stringResource(R.string.tools),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Language, null) },
        label = { Text(stringResource(R.string.webview_title)) },
        selected = false,
        onClick = onOpenWebView,
        modifier = Modifier.padding(horizontal = 12.dp)
    )

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.BarChart, null) },
        label = { Text(stringResource(R.string.statistics_title)) },
        selected = false,
        onClick = onOpenStatistics,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun MoreOptionsSection(
    onOpenSettings: () -> Unit
) {
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
