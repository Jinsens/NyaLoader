package com.nyapass.loader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 剪贴板监听首次使用提示对话框
 */
@Composable
fun ClipboardTipDialog(
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit
) {
    // 使用rememberUpdatedState避免闭包捕获旧值
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnEnable by rememberUpdatedState(onEnable)
    val currentOnDisable by rememberUpdatedState(onDisable)
    
    AlertDialog(
        onDismissRequest = currentOnDismiss,
        icon = {
            Icon(
                Icons.Default.ContentPaste,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "智能剪贴板监听",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "NyaLoader 可以自动识别剪贴板中的下载链接",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // 功能说明
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Default.Link,
                        title = "自动识别",
                        description = "复制链接后打开应用立即识别"
                    )
                    
                    FeatureItem(
                        icon = Icons.Default.ContentPaste,
                        title = "智能提取",
                        description = "从长文本中自动提取下载链接"
                    )
                    
                    FeatureItem(
                        icon = Icons.Default.Speed,
                        title = "快速下载",
                        description = "一键开始下载，无需手动输入"
                    )
                }
                
                // 提示信息
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "可在设置中随时开启或关闭此功能",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = currentOnEnable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("启用智能监听")
            }
        },
        dismissButton = {
            TextButton(
                onClick = currentOnDisable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("暂不启用")
            }
        }
    )
}

/**
 * 功能说明项
 */
@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

