package com.nyapass.loader.ui.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.data.preferences.UserAgentPreset
import com.nyapass.loader.util.UserAgentHelper

/**
 * 高级选项区域组件
 * 包含 User-Agent 设置等高级功能
 *
 * @author 小花生FMR
 * @version 2.0.0
 */
@Composable
fun AdvancedOptionsSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    userAgent: String,
    onUserAgentChange: (String) -> Unit,
    showUAPresets: Boolean,
    onShowUAPresetsChange: (Boolean) -> Unit,
    webViewUA: String,
    defaultUserAgent: String?,
    customUserAgentPresets: List<UserAgentPreset>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 展开/收起按钮
        AdvancedOptionsHeader(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        )

        // 展开的内容
        if (expanded) {
            // User-Agent 输入
            UserAgentInput(
                userAgent = userAgent,
                onUserAgentChange = onUserAgentChange,
                showPresets = showUAPresets,
                onShowPresetsChange = onShowUAPresetsChange
            )

            // UA 预设列表
            if (showUAPresets) {
                UserAgentPresetList(
                    webViewUA = webViewUA,
                    defaultUserAgent = defaultUserAgent,
                    customPresets = customUserAgentPresets,
                    onSelect = { ua ->
                        onUserAgentChange(ua)
                        onShowUAPresetsChange(false)
                    }
                )
            }
        }
    }
}

/**
 * 高级选项头部（展开/收起按钮）
 */
@Composable
private fun AdvancedOptionsHeader(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    OutlinedCard(
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
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
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
    }
}

/**
 * User-Agent 输入框
 */
@Composable
private fun UserAgentInput(
    userAgent: String,
    onUserAgentChange: (String) -> Unit,
    showPresets: Boolean,
    onShowPresetsChange: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = userAgent,
        onValueChange = onUserAgentChange,
        label = { Text(stringResource(R.string.user_agent_label)) },
        placeholder = { Text(stringResource(R.string.browser_identity)) },
        leadingIcon = { Icon(Icons.Default.Computer, null) },
        trailingIcon = {
            IconButton(onClick = { onShowPresetsChange(!showPresets) }) {
                Icon(
                    if (showPresets) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    stringResource(R.string.select_preset_ua)
                )
            }
        },
        supportingText = {
            Text(
                if (showPresets) stringResource(R.string.click_below_to_select)
                else stringResource(R.string.click_arrow_to_select)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 3
    )
}

/**
 * User-Agent 预设列表
 */
@Composable
private fun UserAgentPresetList(
    webViewUA: String,
    defaultUserAgent: String?,
    customPresets: List<UserAgentPreset>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 本设备预设（放在最前面）
        UAPresetItem(
            name = stringResource(R.string.this_device),
            ua = webViewUA,
            isHighlighted = true,
            onClick = onSelect
        )

        // 默认设置中的 UA
        if (defaultUserAgent != null && defaultUserAgent != webViewUA) {
            UAPresetItem(
                name = stringResource(R.string.default_settings),
                ua = defaultUserAgent,
                onClick = onSelect
            )
        }

        // 自定义预设
        if (customPresets.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(R.string.user_agent_custom_presets),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            customPresets.forEach { preset ->
                UAPresetItem(
                    name = preset.name,
                    ua = preset.userAgent,
                    onClick = onSelect
                )
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
        UAPresetItem("Chrome Windows", UserAgentHelper.Presets.CHROME_WINDOWS, onClick = onSelect)
        UAPresetItem("Chrome Mac", UserAgentHelper.Presets.CHROME_MAC, onClick = onSelect)
        UAPresetItem("Firefox", UserAgentHelper.Presets.FIREFOX, onClick = onSelect)
        UAPresetItem("Android Chrome", UserAgentHelper.Presets.ANDROID_CHROME, onClick = onSelect)
    }
}

/**
 * UA 预设项
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
