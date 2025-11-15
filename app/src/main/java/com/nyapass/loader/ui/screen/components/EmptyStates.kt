package com.nyapass.loader.ui.screen.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
 * 空状态组件
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    filter: TaskFilter
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (filter) {
                TaskFilter.ALL -> Icons.Default.CloudDownload
                TaskFilter.DOWNLOADING -> Icons.Default.Download
                TaskFilter.COMPLETED -> Icons.Default.CheckCircle
                TaskFilter.PAUSED -> Icons.Default.Pause
                TaskFilter.FAILED -> Icons.Default.Error
            },
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(
                when (filter) {
                    TaskFilter.ALL -> R.string.no_tasks
                    TaskFilter.DOWNLOADING -> R.string.no_downloading_tasks
                    TaskFilter.COMPLETED -> R.string.no_completed_tasks
                    TaskFilter.PAUSED -> R.string.no_paused_tasks
                    TaskFilter.FAILED -> R.string.no_failed_tasks
                }
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.tap_to_create),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * 搜索无结果状态
 */
@Composable
fun SearchEmptyState(
    modifier: Modifier = Modifier,
    searchQuery: String
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.no_search_results),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "「$searchQuery」",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

