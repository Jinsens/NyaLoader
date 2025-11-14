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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
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
    
    // 批量下载：使用URL列表
    val urls = remember { mutableStateListOf("") }
    val urlErrors = remember { mutableStateListOf(false) }
    
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
    var threadCountError by remember { mutableStateOf(false) }
    
    // 使用外部传入的临时路径
    val customPath = currentTempPath
    
    AlertDialog(
        onDismissRequest = currentOnDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false  // 允许自定义宽度
        ),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight(),
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.new_download_task)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // URL输入框列表（批量下载）
                urls.forEachIndexed { index, url ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = {
                                urls[index] = it
                                if (index < urlErrors.size) {
                                    urlErrors[index] = false
                                }
                            },
                            label = { 
                                Text(
                                    if (urls.size > 1) 
                                        "${stringResource(R.string.download_link)} ${index + 1}" 
                                    else 
                                        stringResource(R.string.download_link)
                                ) 
                            },
                            placeholder = { Text(stringResource(R.string.enter_url)) },
                            leadingIcon = { Icon(Icons.Default.Link, null) },
                            trailingIcon = if (urls.size > 1) {
                                {
                                    IconButton(
                                        onClick = { 
                                            urls.removeAt(index)
                                            if (index < urlErrors.size) {
                                                urlErrors.removeAt(index)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else null,
                            isError = index < urlErrors.size && urlErrors[index],
                            supportingText = if (index < urlErrors.size && urlErrors[index]) {
                                { Text(stringResource(R.string.enter_valid_url)) }
                            } else null,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                
                // 添加更多链接按钮
                OutlinedButton(
                    onClick = { 
                        urls.add("")
                        urlErrors.add(false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_more_links))
                }
                
                // 批量下载提示
                if (urls.size > 1) {
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
                                text = stringResource(R.string.batch_download_hint, urls.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // 文件名输入框（可选）
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(stringResource(R.string.filename_optional)) },
                    placeholder = { Text(stringResource(R.string.auto_extract_from_url)) },
                    leadingIcon = { Icon(Icons.Default.Description, null) },
                    supportingText = if (urls.size > 1) {
                        { Text(stringResource(R.string.filename_only_first)) }
                    } else null,
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
                    label = { Text(stringResource(R.string.thread_count)) },
                    leadingIcon = { Icon(Icons.Default.Speed, null) },
                    isError = threadCountError,
                    supportingText = if (threadCountError) {
                        { Text(stringResource(R.string.thread_count_error)) }
                    } else {
                        { Text(stringResource(R.string.thread_count_hint)) }
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
                                text = stringResource(R.string.advanced_options),
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
                        label = { Text(stringResource(R.string.user_agent_label)) },
                        placeholder = { Text(stringResource(R.string.browser_identity)) },
                        leadingIcon = { Icon(Icons.Default.Computer, null) },
                        trailingIcon = {
                            IconButton(onClick = { showUAPresets = !showUAPresets }) {
                                Icon(
                                    if (showUAPresets) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    stringResource(R.string.select_preset_ua)
                                )
                            }
                        },
                        supportingText = { 
                            Text(if (showUAPresets) stringResource(R.string.click_below_to_select) else stringResource(R.string.click_arrow_to_select))
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
                                name = stringResource(R.string.this_device), 
                                ua = webViewUA,
                                isHighlighted = true
                            ) {
                                userAgent = it
                                showUAPresets = false
                            }
                            
                            // 默认设置中的UA（如果有）
                            if (defaultUserAgentFromSettings != null && defaultUserAgentFromSettings != webViewUA) {
                                UAPresetItemSimple(
                                    name = stringResource(R.string.default_settings),
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
                                    text = stringResource(R.string.user_agent_custom_presets),
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
                                text = stringResource(R.string.user_agent_system_presets),
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
                                        text = stringResource(R.string.temp_save_directory),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = customPath ?: stringResource(R.string.click_to_select_temp),
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
                                text = stringResource(R.string.multithread_hint),
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
                                    text = stringResource(R.string.temp_dir_hint),
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
                    
                    // 验证所有URL
                    urlErrors.clear()
                    urls.forEachIndexed { index, url ->
                        val isValid = url.isNotBlank() && url.trim().startsWith("http", ignoreCase = true)
                        urlErrors.add(!isValid)
                        if (!isValid) {
                            hasError = true
                        }
                    }
                    
                    // 验证线程数
                    val threads = threadCount.toIntOrNull()
                    if (threads == null || threads !in 1..256) {
                        threadCountError = true
                        hasError = true
                    }
                    
                    if (!hasError && threads != null) {
                        // 为每个有效的URL创建下载任务
                        urls.forEachIndexed { index, url ->
                            if (url.isNotBlank() && url.trim().startsWith("http", ignoreCase = true)) {
                                // 批量下载时，文件名只对第一个URL有效，其他自动从URL提取
                                val actualFileName = if (index == 0 && fileName.isNotBlank()) {
                                    fileName.trim()
                                } else {
                                    null
                                }
                                
                                currentOnConfirm(
                                    url.trim(),
                                    actualFileName,
                                    threads,
                                    true, // 始终使用默认设置
                                    customPath, // 临时自定义路径
                                    userAgent.trim().ifBlank { null }
                                )
                            }
                        }
                        
                        // 关闭对话框
                        currentOnDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.start_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

