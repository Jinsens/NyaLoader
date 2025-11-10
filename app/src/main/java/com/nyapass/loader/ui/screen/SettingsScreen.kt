package com.nyapass.loader.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nyapass.loader.data.preferences.DarkMode
import com.nyapass.loader.data.preferences.SaveLocation
import com.nyapass.loader.data.preferences.ThemeColor
import com.nyapass.loader.data.preferences.UserAgentPreset
import com.nyapass.loader.util.UserAgentHelper
import com.nyapass.loader.viewmodel.SettingsViewModel

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSelectFolder: () -> Unit,
    onOpenLicenses: () -> Unit = {}
) {
    val context = LocalContext.current
    
    val themeColor by viewModel.themeColor.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val customColor by viewModel.customColor.collectAsState()
    val defaultUserAgent by viewModel.defaultUserAgent.collectAsState()
    val customUserAgentPresets by viewModel.customUserAgentPresets.collectAsState()
    val defaultSaveLocation by viewModel.defaultSaveLocation.collectAsState()
    val customSavePath by viewModel.customSavePath.collectAsState()
    val defaultThreadCount by viewModel.defaultThreadCount.collectAsState()
    val clipboardMonitorEnabled by viewModel.clipboardMonitorEnabled.collectAsState()
    val firebaseEnabled by viewModel.firebaseEnabled.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // 当页面销毁时，清除所有对话框状态
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAllDialogs()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 外观设置
            item {
                SettingsSectionHeader("外观")
            }
            
            // 主题颜色
            item {
                SettingsItemWithContent(
                    icon = Icons.Default.Palette,
                    title = "主题颜色",
                    subtitle = themeColor.displayName
                ) {
                    ColorSelector(
                        selectedColor = themeColor,
                        onColorSelected = { viewModel.updateThemeColor(it) }
                    )
                }
            }
            
            // 自定义颜色（仅在选择自定义时显示）
            if (themeColor == ThemeColor.CUSTOM) {
                item {
                    SettingsItem(
                        icon = Icons.Default.ColorLens,
                        title = "自定义颜色",
                        subtitle = customColor ?: "点击设置",
                        onClick = { viewModel.showColorPicker() }
                    )
                }
            }
            
            // 暗色模式
            item {
                SettingsItemWithSegmented(
                    icon = Icons.Default.DarkMode,
                    title = "暗色模式",
                    options = DarkMode.entries.toList(),
                    selectedOption = darkMode,
                    onOptionSelected = { viewModel.updateDarkMode(it) },
                    optionLabel = { it.displayName }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // 下载设置
            item {
                SettingsSectionHeader("下载设置")
            }
            
            // 默认保存位置
            item {
                SettingsItemWithSegmented(
                    icon = Icons.Default.Folder,
                    title = "默认保存位置",
                    options = SaveLocation.entries.toList(),
                    selectedOption = defaultSaveLocation,
                    onOptionSelected = { viewModel.updateDefaultSaveLocation(it) },
                    optionLabel = { it.displayName }
                )
            }
            
            // 自定义目录（仅在选择自定义时显示）
            if (defaultSaveLocation == SaveLocation.CUSTOM) {
                item {
                    SettingsItem(
                        icon = Icons.Default.FolderOpen,
                        title = "自定义目录",
                        subtitle = customSavePath ?: "点击选择",
                        onClick = onSelectFolder
                    )
                }
            }
            
            // 默认线程数
            item {
                SettingsItemWithSlider(
                    icon = Icons.Default.Speed,
                    title = "默认线程数",
                    value = defaultThreadCount,
                    valueRange = 1f..256f,
                    onValueChange = { viewModel.updateDefaultThreadCount(it.toInt()) }
                )
            }
            
            // 默认User-Agent
            item {
                SettingsItem(
                    icon = Icons.Default.Computer,
                    title = "默认User-Agent",
                    subtitle = defaultUserAgent?.take(50) ?: "使用WebView默认UA",
                    onClick = { viewModel.showUserAgentDialog() }
                )
            }
            
            // 剪贴板监听开关
            item {
                SettingsItemWithSwitch(
                    icon = Icons.Default.ContentPaste,
                    title = "剪贴板监听",
                    subtitle = "复制链接后自动弹窗，快速开始下载",
                    checked = clipboardMonitorEnabled,
                    onCheckedChange = { viewModel.updateClipboardMonitorEnabled(it) }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // 数据分析
            item {
                SettingsSectionHeader("数据分析")
            }
            
            // Firebase 开关
            item {
                SettingsItemWithSwitch(
                    icon = Icons.Default.Analytics,
                    title = "Google Firebase 分析",
                    subtitle = "帮助我们改进应用体验（需要重启应用生效）",
                    checked = firebaseEnabled,
                    onCheckedChange = { viewModel.updateFirebaseEnabled(it) }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // 关于与许可
            item {
                SettingsSectionHeader("其他")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于与许可",
                    subtitle = "查看应用信息和开源许可",
                    onClick = onOpenLicenses
                )
            }
            
            // 底部间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // 自定义颜色选择对话框
    if (uiState.showColorPicker) {
        CustomColorPickerDialog(
            currentColor = customColor,
            onDismiss = { viewModel.hideColorPicker() },
            onConfirm = { color ->
                viewModel.updateCustomColor(color)
                viewModel.hideColorPicker()
            }
        )
    }
    
    // User-Agent编辑对话框
    if (uiState.showUserAgentDialog) {
        UserAgentDialog(
            currentUA = defaultUserAgent ?: UserAgentHelper.getDefaultUserAgent(context),
            customPresets = customUserAgentPresets,
            onDismiss = { viewModel.hideUserAgentDialog() },
            onConfirm = { ua ->
                viewModel.updateDefaultUserAgent(ua)
                viewModel.hideUserAgentDialog()
            },
            onAddCustomPreset = { viewModel.showAddCustomUADialog() },
            onRemoveCustomPreset = { preset ->
                viewModel.removeCustomUserAgentPreset(preset)
            }
        )
    }
    
    // 添加自定义UA预设对话框
    if (uiState.showAddCustomUADialog) {
        AddCustomUAPresetDialog(
            onDismiss = { viewModel.hideAddCustomUADialog() },
            onConfirm = { name, ua ->
                viewModel.addCustomUserAgentPreset(name, ua)
                viewModel.hideAddCustomUADialog()
            }
        )
    }
}

/**
 * 设置分组标题
 */
@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * 设置项
 */
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 带自定义内容的设置项
 */
@Composable
fun SettingsItemWithContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        content()
    }
}

/**
 * 带分段选择器的设置项
 */
@Composable
fun <T> SettingsItemWithSegmented(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    label = { Text(optionLabel(option)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 带滑块的设置项
 */
@Composable
fun SettingsItemWithSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 带开关的设置项
 */
@Composable
fun SettingsItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

/**
 * 颜色选择器
 */
@Composable
fun ColorSelector(
    selectedColor: ThemeColor,
    onColorSelected: (ThemeColor) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(ThemeColor.entries.toList()) { color ->
            ColorOption(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

/**
 * 颜色选项
 */
@Composable
fun ColorOption(
    color: ThemeColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(getColorForTheme(color))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                            CircleShape
                        )
                    } else {
                        Modifier.border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            CircleShape
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // 动态取色选项显示特殊图标
            if (color == ThemeColor.DYNAMIC) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = color.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 获取主题颜色的Color值
 */
@Composable
fun getColorForTheme(themeColor: ThemeColor): Color {
    return when (themeColor) {
        ThemeColor.DYNAMIC -> MaterialTheme.colorScheme.primary // 使用当前主题色
        ThemeColor.BLUE -> Color(0xFF2196F3)
        ThemeColor.GREEN -> Color(0xFF4CAF50)
        ThemeColor.PURPLE -> Color(0xFF9C27B0)
        ThemeColor.ORANGE -> Color(0xFFFF9800)
        ThemeColor.RED -> Color(0xFFF44336)
        ThemeColor.PINK -> Color(0xFFE91E63)
        ThemeColor.TEAL -> Color(0xFF009688)
        ThemeColor.CUSTOM -> MaterialTheme.colorScheme.primary
    }
}

/**
 * 自定义颜色选择对话框
 */
@Composable
fun CustomColorPickerDialog(
    currentColor: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var colorHex by remember { mutableStateOf(currentColor ?: "#2196F3") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义颜色") },
        text = {
            Column {
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("十六进制颜色") },
                    placeholder = { Text("#RRGGBB") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "预览",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(colorHex) }) {
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

/**
 * User-Agent编辑对话框
 */
@Composable
fun UserAgentDialog(
    currentUA: String,
    customPresets: List<UserAgentPreset>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onAddCustomPreset: () -> Unit,
    onRemoveCustomPreset: (UserAgentPreset) -> Unit
) {
    val context = LocalContext.current
    val deviceUA = remember { UserAgentHelper.getDefaultUserAgent(context) }
    
    var userAgent by remember { mutableStateOf(currentUA) }
    var showPresets by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("默认User-Agent") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text("User-Agent") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { showPresets = !showPresets },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showPresets) "隐藏预设" else "选择预设")
                    Icon(
                        if (showPresets) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                
                if (showPresets) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 本设备预设（放在最前面）
                        UAPresetItem(
                            name = "本设备", 
                            ua = deviceUA,
                            isHighlighted = true
                        ) {
                            userAgent = it
                            showPresets = false
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // 自定义预设标题和添加按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "自定义预设",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = onAddCustomPreset) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "添加自定义预设",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // 自定义预设列表
                        if (customPresets.isEmpty()) {
                            Text(
                                text = "暂无自定义预设，点击上方+号添加",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            customPresets.forEach { preset ->
                                UAPresetItemWithDelete(
                                    name = preset.name,
                                    ua = preset.userAgent,
                                    onSelect = {
                                        userAgent = it
                                        showPresets = false
                                    },
                                    onDelete = { onRemoveCustomPreset(preset) }
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // 系统预设
                        Text(
                            text = "系统预设",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        UAPresetItem("Chrome Windows", UserAgentHelper.Presets.CHROME_WINDOWS) {
                            userAgent = it
                            showPresets = false
                        }
                        UAPresetItem("Chrome Mac", UserAgentHelper.Presets.CHROME_MAC) {
                            userAgent = it
                            showPresets = false
                        }
                        UAPresetItem("Firefox", UserAgentHelper.Presets.FIREFOX) {
                            userAgent = it
                            showPresets = false
                        }
                        UAPresetItem("Android Chrome", UserAgentHelper.Presets.ANDROID_CHROME) {
                            userAgent = it
                            showPresets = false
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(userAgent.ifBlank { null }) }) {
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

/**
 * UA预设项
 */
@Composable
fun UAPresetItem(
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

/**
 * 带删除按钮的UA预设项
 */
@Composable
fun UAPresetItemWithDelete(
    name: String,
    ua: String,
    onSelect: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = { onSelect(ua) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 添加自定义UA预设对话框
 */
@Composable
fun AddCustomUAPresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, userAgent: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var uaError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自定义UA预设") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = false
                    },
                    label = { Text("预设名称") },
                    placeholder = { Text("例如：我的浏览器") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("请输入预设名称") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { 
                        userAgent = it
                        uaError = false
                    },
                    label = { Text("User-Agent") },
                    placeholder = { Text("输入User-Agent字符串") },
                    isError = uaError,
                    supportingText = if (uaError) {
                        { Text("请输入User-Agent") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                var hasError = false
                
                if (name.isBlank()) {
                    nameError = true
                    hasError = true
                }
                
                if (userAgent.isBlank()) {
                    uaError = true
                    hasError = true
                }
                
                if (!hasError) {
                    onConfirm(name.trim(), userAgent.trim())
                }
            }) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
