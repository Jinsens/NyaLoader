package com.nyapass.loader.ui.components.webview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * User-Agent 设置对话框
 * 
 * 允许用户选择预设的 User-Agent 或自定义
 */
@Composable
fun UserAgentDialog(
    currentUA: String,
    customUA: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedUA by remember { mutableStateOf(customUA.ifEmpty { currentUA }) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf(customUA) }
    
    // 预设的User-Agent列表
    val presetUAs = listOf(
        "Chrome Android (移动)" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Chrome Windows (桌面)" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Safari iPhone" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
        "Safari Mac" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
        "Firefox Android" to "Mozilla/5.0 (Android 13; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0",
        "Edge Windows" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
        "自定义 UA" to "custom"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User-Agent 设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "选择或自定义浏览器标识",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 预设选项
                presetUAs.forEach { (name, ua) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (ua == "custom") {
                                    showCustomInput = true
                                    selectedUA = customInput
                                } else {
                                    showCustomInput = false
                                    selectedUA = ua
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = if (ua == "custom") {
                                showCustomInput
                            } else {
                                selectedUA == ua && !showCustomInput
                            },
                            onClick = {
                                if (ua == "custom") {
                                    showCustomInput = true
                                    selectedUA = customInput
                                } else {
                                    showCustomInput = false
                                    selectedUA = ua
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // 自定义输入框
                if (showCustomInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { 
                            customInput = it
                            selectedUA = it
                        },
                        label = { Text("自定义 User-Agent") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
                
                // 显示当前UA
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "当前 UA:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentUA,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedUA) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
