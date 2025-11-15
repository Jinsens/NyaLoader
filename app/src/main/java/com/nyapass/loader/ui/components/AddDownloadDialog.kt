package com.nyapass.loader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.ui.components.dialog.*

/**
 * 添加下载对话框
 * 重构后使用模块化的子组件
 *
 * @author 小花生FMR
 * @version 2.0.0 (重构版)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDownloadDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, fileName: String?, threadCount: Int, saveToPublicDir: Boolean, customPath: String?, userAgent: String?, tagIds: List<Long>) -> Unit,
    onSelectFolder: (() -> Unit)? = null,
    currentTempPath: String? = null,
    availableTags: List<DownloadTag> = emptyList(),
    onCreateTag: (() -> Unit)? = null
) {
    // 使用状态管理类
    val state = rememberAddDownloadState()

    // 使用 rememberUpdatedState 避免闭包捕获旧值
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnSelectFolder by rememberUpdatedState(onSelectFolder)

    // 清理无效的标签选择
    LaunchedEffect(availableTags) {
        val validIds = availableTags.map { it.id }.toSet()
        state.cleanupInvalidTags(validIds)
    }

    AlertDialog(
        onDismissRequest = currentOnDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight(),
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.new_download_task)) },
        text = {
            AddDownloadDialogContent(
                state = state,
                currentTempPath = currentTempPath,
                availableTags = availableTags,
                onCreateTag = onCreateTag,
                onSelectFolder = currentOnSelectFolder
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val (hasError, threads) = state.validate()

                    if (!hasError && threads != null) {
                        // 为每个有效的 URL 创建下载任务
                        val validUrls = state.getValidUrls()
                        validUrls.forEachIndexed { index, url ->
                            // 批量下载时，文件名只对第一个 URL 有效
                            val actualFileName = if (index == 0 && state.fileName.isNotBlank()) {
                                state.fileName.trim()
                            } else {
                                null
                            }

                            currentOnConfirm(
                                url,
                                actualFileName,
                                threads,
                                true, // 始终使用默认设置
                                currentTempPath,
                                state.userAgent.trim().ifBlank { null },
                                state.selectedTagIds.toList()
                            )
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
 * 对话框内容区域
 */
@Composable
private fun AddDownloadDialogContent(
    state: AddDownloadState,
    currentTempPath: String?,
    availableTags: List<DownloadTag>,
    onCreateTag: (() -> Unit)?,
    onSelectFolder: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // URL 输入区域
        UrlInputSection(
            urls = state.urls,
            urlErrors = state.urlErrors,
            onUrlChange = state::updateUrl,
            onAddUrl = state::addUrl,
            onRemoveUrl = state::removeUrl
        )

        // 文件名输入
        FileNameInput(
            fileName = state.fileName,
            onFileNameChange = { state.fileName = it },
            isBatchMode = state.urls.size > 1
        )

        // 线程数选择
        ThreadCountSection(
            threadCount = state.threadCount,
            onThreadCountChange = {
                state.threadCount = it
                state.threadCountError = false
            },
            threadCountError = state.threadCountError
        )

        // 标签选择
        TagSelectionSection(
            availableTags = availableTags,
            selectedTagIds = state.selectedTagIds,
            onTagToggle = state::toggleTag,
            onCreateTag = onCreateTag
        )

        // 高级选项
        AdvancedOptionsSection(
            expanded = state.showAdvancedOptions,
            onExpandedChange = { state.showAdvancedOptions = it },
            userAgent = state.userAgent,
            onUserAgentChange = { state.userAgent = it },
            showUAPresets = state.showUAPresets,
            onShowUAPresetsChange = { state.showUAPresets = it },
            webViewUA = state.webViewUA,
            defaultUserAgent = state.defaultUserAgent,
            customUserAgentPresets = state.customUserAgentPresets
        )

        // 自定义保存目录
        if (onSelectFolder != null) {
            SaveDirectorySection(
                customPath = currentTempPath,
                onSelectFolder = onSelectFolder
            )
        }

        // 提示信息
        DownloadHintsSection(
            hasCustomPath = currentTempPath != null
        )
    }
}

/**
 * 简化版 UA 预设项（保留向后兼容）
 * @deprecated 使用 [com.nyapass.loader.ui.components.dialog.UAPresetItem] 替代
 */
@Composable
fun UAPresetItemSimple(
    name: String,
    ua: String,
    isHighlighted: Boolean = false,
    onClick: (String) -> Unit
) {
    UAPresetItem(
        name = name,
        ua = ua,
        isHighlighted = isHighlighted,
        onClick = onClick
    )
}
