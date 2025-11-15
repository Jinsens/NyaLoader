package com.nyapass.loader.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.data.preferences.UserAgentPreset
import com.nyapass.loader.util.UserAgentHelper

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
        title = { Text(stringResource(R.string.custom_color_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text(stringResource(R.string.custom_color)) },
                    placeholder = { Text(stringResource(R.string.custom_color_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.custom_color_preview),
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
                                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(colorHex) }) {
                Text(stringResource(R.string.confirm))
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
        title = { Text(stringResource(R.string.user_agent_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text(stringResource(R.string.user_agent_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { showPresets = !showPresets },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showPresets) stringResource(R.string.user_agent_hide_presets) else stringResource(R.string.user_agent_show_presets))
                    Icon(
                        if (showPresets) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                
                if (showPresets) {
                    UAPresetsContent(
                        deviceUA = deviceUA,
                        customPresets = customPresets,
                        onSelectUA = {
                            userAgent = it
                            showPresets = false
                        },
                        onAddCustomPreset = onAddCustomPreset,
                        onRemoveCustomPreset = onRemoveCustomPreset
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(userAgent.ifBlank { null }) }) {
                Text(stringResource(R.string.confirm))
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
 * UA预设内容
 */
@Composable
private fun UAPresetsContent(
    deviceUA: String,
    customPresets: List<UserAgentPreset>,
    onSelectUA: (String) -> Unit,
    onAddCustomPreset: () -> Unit,
    onRemoveCustomPreset: (UserAgentPreset) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 本设备预设
        UAPresetItem(
            name = stringResource(R.string.user_agent_device), 
            ua = deviceUA,
            isHighlighted = true,
            onClick = onSelectUA
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // 自定义预设
        UACustomPresetsSection(
            customPresets = customPresets,
            onSelectUA = onSelectUA,
            onAddCustomPreset = onAddCustomPreset,
            onRemoveCustomPreset = onRemoveCustomPreset
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // 系统预设
        UASystemPresetsSection(onSelectUA = onSelectUA)
    }
}

/**
 * 自定义UA预设部分
 */
@Composable
private fun UACustomPresetsSection(
    customPresets: List<UserAgentPreset>,
    onSelectUA: (String) -> Unit,
    onAddCustomPreset: () -> Unit,
    onRemoveCustomPreset: (UserAgentPreset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.user_agent_custom_presets),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onAddCustomPreset) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (customPresets.isEmpty()) {
        Text(
            text = stringResource(R.string.user_agent_no_custom),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        customPresets.forEach { preset ->
            UAPresetItemWithDelete(
                name = preset.name,
                ua = preset.userAgent,
                onSelect = onSelectUA,
                onDelete = { onRemoveCustomPreset(preset) }
            )
        }
    }
}

/**
 * 系统UA预设部分
 */
@Composable
private fun UASystemPresetsSection(onSelectUA: (String) -> Unit) {
    Text(
        text = stringResource(R.string.user_agent_system_presets),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    
    UAPresetItem(stringResource(R.string.user_agent_chrome_windows), UserAgentHelper.Presets.CHROME_WINDOWS, onClick = onSelectUA)
    UAPresetItem(stringResource(R.string.user_agent_chrome_mac), UserAgentHelper.Presets.CHROME_MAC, onClick = onSelectUA)
    UAPresetItem(stringResource(R.string.user_agent_firefox), UserAgentHelper.Presets.FIREFOX, onClick = onSelectUA)
    UAPresetItem(stringResource(R.string.user_agent_android_chrome), UserAgentHelper.Presets.ANDROID_CHROME, onClick = onSelectUA)
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
                    contentDescription = stringResource(R.string.delete),
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
        title = { Text(stringResource(R.string.user_agent_add_preset)) },
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
                    label = { Text(stringResource(R.string.user_agent_preset_name)) },
                    placeholder = { Text(stringResource(R.string.user_agent_preset_name_hint)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.user_agent_preset_name_error)) }
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
                    label = { Text(stringResource(R.string.user_agent_label)) },
                    placeholder = { Text(stringResource(R.string.user_agent_preset_ua_hint)) },
                    isError = uaError,
                    supportingText = if (uaError) {
                        { Text(stringResource(R.string.user_agent_preset_ua_error)) }
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
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

