package com.nyapass.loader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.util.VersionInfo

/**
 * 更新提示对话框
 * 
 * @param versionInfo 版本信息
 * @param onDismiss 忽略更新回调
 * @param onConfirm 确认更新回调
 */
@Composable
fun UpdateDialog(
    versionInfo: VersionInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // 使用 rememberUpdatedState 避免闭包捕获旧值
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    
    AlertDialog(
        onDismissRequest = {
            if (!versionInfo.forceUpdate) {
                currentOnDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = versionInfo.versionName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 如果是强制更新，显示提示
                if (versionInfo.forceUpdate) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.update_force_update_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // 更新内容
                Text(
                    text = versionInfo.msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 更新时间（如果有）
                versionInfo.updateTime?.let { time ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${stringResource(R.string.update_dialog_update_time)}: $time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = currentOnConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.update_dialog_download_now))
            }
        },
        dismissButton = {
            // 如果不是强制更新，显示"稍后再说"按钮
            if (!versionInfo.forceUpdate) {
                TextButton(onClick = currentOnDismiss) {
                    Text(stringResource(R.string.update_dialog_later))
                }
            }
        }
    )
}

