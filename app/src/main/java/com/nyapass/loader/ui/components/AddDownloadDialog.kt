package com.nyapass.loader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nyapass.loader.data.preferences.UserAgentPreset
import com.nyapass.loader.util.UserAgentHelper

/**
 * 添加下载对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDownloadDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, fileName: String?, threadCount: Int, saveToPublicDir: Boolean, customPath: String?, userAgent: String?) -> Unit,
    onSelectFolder: (() -> Unit)? = null,
    currentTempPath: String? = null  // 当前的临时路径（从外部传入）
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.nyapass.loader.LoaderApplication
    
    // 从设置中获取默认值
    val defaultUserAgentFromSettings by app.appPreferences.defaultUserAgent.collectAsState()
    val customUserAgentPresets by app.appPreferences.customUserAgentPresets.collectAsState()
    val defaultThreadCountFromSettings by app.appPreferences.defaultThreadCount.collectAsState()
    val webViewUA = remember { UserAgentHelper.getDefaultUserAgent(context) }
    
    // 使用rememberUpdatedState避免闭包捕获旧值
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnSelectFolder by rememberUpdatedState(onSelectFolder)
    
    // 使用key确保每次打开对话框时状态都重置
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    // 依赖defaultThreadCountFromSettings，当它变化时更新
    var threadCount by remember(defaultThreadCountFromSettings) { 
        mutableStateOf(defaultThreadCountFromSettings.toString()) 
    }
    // 依赖defaultUserAgentFromSettings，当它变化时更新
    var userAgent by remember(defaultUserAgentFromSettings, webViewUA) { 
        mutableStateOf(defaultUserAgentFromSettings ?: webViewUA) 
    }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var showUAPresets by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }
    var threadCountError by remember { mutableStateOf(false) }
    
    // 使用外部传入的临时路径
    val customPath = currentTempPath
    
    AlertDialog(
        onDismissRequest = currentOnDismiss,
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text("新建下载任务") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // URL输入框
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = false
                    },
                    label = { Text("下载链接") },
                    placeholder = { Text("请输入URL") },
                    leadingIcon = { Icon(Icons.Default.Link, null) },
                    isError = urlError,
                    supportingText = if (urlError) {
                        { Text("请输入有效的URL") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 文件名输入框（可选）
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名（可选）") },
                    placeholder = { Text("自动从URL提取") },
                    leadingIcon = { Icon(Icons.Default.Description, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 线程数选择
                OutlinedTextField(
                    value = threadCount,
                    onValueChange = {
                        threadCount = it
                        threadCountError = false
                    },
                    label = { Text("线程数") },
                    leadingIcon = { Icon(Icons.Default.Speed, null) },
                    isError = threadCountError,
                    supportingText = if (threadCountError) {
                        { Text("线程数必须在1-256之间") }
                    } else {
                        { Text("建议使用32个线程，最大支持256") }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 高级选项展开按钮
                OutlinedCard(
                    onClick = { showAdvancedOptions = !showAdvancedOptions },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "高级选项",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
                
                // 高级选项内容
                if (showAdvancedOptions) {
                    // User-Agent输入框
                    OutlinedTextField(
                        value = userAgent,
                        onValueChange = { userAgent = it },
                        label = { Text("User-Agent") },
                        placeholder = { Text("浏览器标识") },
                        leadingIcon = { Icon(Icons.Default.Computer, null) },
                        trailingIcon = {
                            IconButton(onClick = { showUAPresets = !showUAPresets }) {
                                Icon(
                                    if (showUAPresets) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    "选择预设UA"
                                )
                            }
                        },
                        supportingText = { 
                            Text(if (showUAPresets) "点击下方选择预设" else "点击右侧箭头选择预设")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                    
                    // UA预设选择列表
                    if (showUAPresets) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 本设备预设（放在最前面）
                            UAPresetItemSimple(
                                name = "本设备", 
                                ua = webViewUA,
                                isHighlighted = true
                            ) {
                                userAgent = it
                                showUAPresets = false
                            }
                            
                            // 默认设置中的UA（如果有）
                            if (defaultUserAgentFromSettings != null && defaultUserAgentFromSettings != webViewUA) {
                                UAPresetItemSimple(
                                    name = "默认设置",
                                    ua = defaultUserAgentFromSettings!!
                                ) {
                                    userAgent = it
                                    showUAPresets = false
                                }
                            }
                            
                            // 自定义预设
                            if (customUserAgentPresets.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "自定义预设",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                customUserAgentPresets.forEach { preset ->
                                    UAPresetItemSimple(
                                        name = preset.name,
                                        ua = preset.userAgent
                                    ) {
                                        userAgent = it
                                        showUAPresets = false
                                    }
                                }
                            }
                            
                            // 系统预设
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "系统预设",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            UAPresetItemSimple("Chrome Windows", UserAgentHelper.Presets.CHROME_WINDOWS) {
                                userAgent = it
                                showUAPresets = false
                            }
                            UAPresetItemSimple("Chrome Mac", UserAgentHelper.Presets.CHROME_MAC) {
                                userAgent = it
                                showUAPresets = false
                            }
                            UAPresetItemSimple("Firefox", UserAgentHelper.Presets.FIREFOX) {
                                userAgent = it
                                showUAPresets = false
                            }
                            UAPresetItemSimple("Android Chrome", UserAgentHelper.Presets.ANDROID_CHROME) {
                                userAgent = it
                                showUAPresets = false
                            }
                        }
                    }
                }
                
                // 自定义保存目录（可选）
                if (currentOnSelectFolder != null) {
                    OutlinedCard(
                        onClick = { currentOnSelectFolder?.invoke() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                                        text = "临时保存目录",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = customPath ?: "点击选择（仅本次下载）",
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
                
                // 提示信息
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "多线程下载可提高下载速度，但会占用更多系统资源",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (customPath != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "临时目录仅用于本次下载，不会保存到设置中",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 验证输入
                    var hasError = false
                    
                    if (url.isBlank() || !url.startsWith("http")) {
                        urlError = true
                        hasError = true
                    }
                    
                    val threads = threadCount.toIntOrNull()
                    if (threads == null || threads !in 1..256) {
                        threadCountError = true
                        hasError = true
                    }
                    
                    if (!hasError) {
                        currentOnConfirm(
                            url.trim(),
                            fileName.trim().ifBlank { null },
                            threads!!,
                            true, // 始终使用默认设置
                            customPath, // 临时自定义路径
                            userAgent.trim().ifBlank { null }
                        )
                    }
                }
            ) {
                Text("开始下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 简化版UA预设项（用于下载对话框）
 */
@Composable
fun UAPresetItemSimple(
    name: String,
    ua: String,
    isHighlighted: Boolean = false,
    onClick: (String) -> Unit
) {
    Surface(
        onClick = { onClick(ua) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlighted) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHighlighted) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHighlighted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

