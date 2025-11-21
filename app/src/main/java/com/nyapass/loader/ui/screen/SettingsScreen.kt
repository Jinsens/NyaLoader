package com.nyapass.loader.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyapass.loader.R
import com.nyapass.loader.data.preferences.DarkMode
import com.nyapass.loader.data.preferences.Language
import com.nyapass.loader.data.preferences.SaveLocation
import com.nyapass.loader.data.preferences.ThemeColor
import com.nyapass.loader.util.UserAgentHelper
import com.nyapass.loader.util.PathFormatter
import com.nyapass.loader.util.getLocalizedName
import com.nyapass.loader.viewmodel.SettingsViewModel
import com.nyapass.loader.ui.screen.components.*

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSelectFolder: () -> Unit,
    onOpenLicenses: () -> Unit = {},
    onCheckUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val customColor by viewModel.customColor.collectAsStateWithLifecycle()
    val defaultUserAgent by viewModel.defaultUserAgent.collectAsStateWithLifecycle()
    val customUserAgentPresets by viewModel.customUserAgentPresets.collectAsStateWithLifecycle()
    val defaultSaveLocation by viewModel.defaultSaveLocation.collectAsStateWithLifecycle()
    val customSavePath by viewModel.customSavePath.collectAsStateWithLifecycle()
    val defaultThreadCount by viewModel.defaultThreadCount.collectAsStateWithLifecycle()
    val clipboardMonitorEnabled by viewModel.clipboardMonitorEnabled.collectAsStateWithLifecycle()
    val firebaseEnabled by viewModel.firebaseEnabled.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 当页面销毁时，清除所有对话框状态
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAllDialogs()
        }
    }
    
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = TopAppBarDefaults.windowInsets
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
                SettingsSectionHeader(stringResource(R.string.appearance))
            }
            
            // 语言设置
            item {
                SettingsItemWithContent(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    subtitle = language.getLocalizedName()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(Language.entries.toList()) { lang ->
                            FilterChip(
                                selected = lang == language,
                                onClick = { viewModel.updateLanguage(lang) },
                                label = { Text(lang.getLocalizedName()) }
                            )
                        }
                    }
                }
            }
            
            // 主题颜色
            item {
                SettingsItemWithContent(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.theme_color),
                    subtitle = themeColor.getLocalizedName()
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
                        title = stringResource(R.string.custom_color),
                        subtitle = customColor ?: stringResource(R.string.click_to_set),
                        onClick = { viewModel.showColorPicker() }
                    )
                }
            }
            
            // 暗色模式
            item {
                SettingsItemWithSegmented(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.dark_mode),
                    options = DarkMode.entries.toList(),
                    selectedOption = darkMode,
                    onOptionSelected = { viewModel.updateDarkMode(it) },
                    optionLabel = { it.getLocalizedName() }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // 下载设置
            item {
                SettingsSectionHeader(stringResource(R.string.download_settings))
            }
            
            // 默认保存位置
            item {
                SettingsItemWithSegmented(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.default_save_location),
                    options = SaveLocation.entries.toList(),
                    selectedOption = defaultSaveLocation,
                    onOptionSelected = { viewModel.updateDefaultSaveLocation(it) },
                    optionLabel = { it.getLocalizedName() }
                )
            }
            
            // 自定义目录（仅在选择自定义时显示）
            if (defaultSaveLocation == SaveLocation.CUSTOM) {
                item {
                    SettingsItem(
                        icon = Icons.Default.FolderOpen,
                        title = stringResource(R.string.custom_directory),
                        subtitle = PathFormatter.formatForDisplay(context, customSavePath) ?: stringResource(R.string.click_to_select),
                        onClick = onSelectFolder
                    )
                }
            }
            
            // 默认线程数
            item {
                SettingsItemWithSlider(
                    icon = Icons.Default.Speed,
                    title = stringResource(R.string.default_thread_count),
                    value = defaultThreadCount,
                    valueRange = 1f..256f,
                    onValueChange = { viewModel.updateDefaultThreadCount(it.toInt()) }
                )
            }
            
            // 默认User-Agent
            item {
                SettingsItem(
                    icon = Icons.Default.Computer,
                    title = stringResource(R.string.default_user_agent),
                    subtitle = defaultUserAgent?.take(50) ?: stringResource(R.string.use_webview_ua),
                    onClick = { viewModel.showUserAgentDialog() }
                )
            }
            
            // 剪贴板监听开关
            item {
                SettingsItemWithSwitch(
                    icon = Icons.Default.ContentPaste,
                    title = stringResource(R.string.clipboard_monitor),
                    subtitle = stringResource(R.string.clipboard_monitor_description),
                    checked = clipboardMonitorEnabled,
                    onCheckedChange = { viewModel.updateClipboardMonitorEnabled(it) }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // 数据分析
            item {
                SettingsSectionHeader(stringResource(R.string.data_analytics))
            }
            
            // Firebase 开关
            item {
                SettingsItemWithSwitch(
                    icon = Icons.Default.Analytics,
                    title = stringResource(R.string.firebase_analytics),
                    subtitle = stringResource(R.string.firebase_description),
                    checked = firebaseEnabled,
                    onCheckedChange = { viewModel.updateFirebaseEnabled(it) }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // 关于与许可
            item {
                SettingsSectionHeader(stringResource(R.string.other))
            }
            
            // 检查更新
            item {
                SettingsItem(
                    icon = Icons.Default.SystemUpdate,
                    title = stringResource(R.string.check_for_updates),
                    subtitle = stringResource(R.string.check_for_updates_description),
                    onClick = onCheckUpdate
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about_and_licenses),
                    subtitle = stringResource(R.string.about_and_licenses_description),
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
