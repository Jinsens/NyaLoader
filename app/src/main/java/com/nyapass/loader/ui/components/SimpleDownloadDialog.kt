package com.nyapass.loader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 简化的下载对话框
 * 只显示下载地址、文件名和开始下载按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDownloadDialog(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (url: String, fileName: String?) -> Unit
) {
    // 使用rememberUpdatedState避免闭包捕获旧值
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    
    // 使用key确保当initialUrl变化时状态会更新
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var fileName by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = currentOnDismiss,
        icon = { 
            Icon(
                Icons.Default.CloudDownload, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        title = { 
            Text(
                "新建下载",
                style = MaterialTheme.typography.headlineSmall
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 下载链接
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = false
                    },
                    label = { Text("下载地址") },
                    placeholder = { Text("https://example.com/file.zip") },
                    leadingIcon = { 
                        Icon(Icons.Default.Link, null) 
                    },
                    isError = urlError,
                    supportingText = if (urlError) {
                        { Text("请输入有效的下载链接") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                
                // 文件名
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名（可选）") },
                    placeholder = { Text("不填则自动从URL中提取") },
                    leadingIcon = { 
                        Icon(Icons.Default.Description, null) 
                    },
                    supportingText = {
                        Text("留空将自动识别文件名")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 提示信息
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "将使用默认设置进行下载\n如需更多选项，请使用右下角的新建下载按钮",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 验证URL
                    if (url.isBlank() || !url.trim().startsWith("http", ignoreCase = true)) {
                        urlError = true
                        return@Button
                    }
                    
                    currentOnConfirm(
                        url.trim(),
                        fileName.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始下载")
            }
        },
        dismissButton = {
            TextButton(onClick = currentOnDismiss) {
                Text("取消")
            }
        }
    )
}

