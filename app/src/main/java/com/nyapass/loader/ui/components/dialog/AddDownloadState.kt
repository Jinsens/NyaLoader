package com.nyapass.loader.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyapass.loader.LoaderApplication
import com.nyapass.loader.data.preferences.UserAgentPreset
import com.nyapass.loader.util.UserAgentHelper

/**
 * AddDownloadDialog 的状态管理类
 * 将所有状态集中管理，便于维护和测试
 *
 * @author 小花生FMR
 * @version 2.5.0
 */
@Stable
class AddDownloadState(
    val webViewUA: String,
    val defaultUserAgent: String?,
    val customUserAgentPresets: List<UserAgentPreset>,
    val defaultThreadCount: Int
) {
    // URL 列表（支持批量下载）
    val urls: SnapshotStateList<String> = mutableStateListOf("")
    val urlErrors: SnapshotStateList<Boolean> = mutableStateListOf(false)

    // 文件名
    var fileName by mutableStateOf("")

    // 线程数
    var threadCount by mutableStateOf(defaultThreadCount.toString())
    var threadCountError by mutableStateOf(false)

    // User-Agent
    var userAgent by mutableStateOf(defaultUserAgent ?: webViewUA)

    // UI 状态
    var showAdvancedOptions by mutableStateOf(false)
    var showUAPresets by mutableStateOf(false)

    // 标签选择
    val selectedTagIds: SnapshotStateList<Long> = mutableStateListOf()

    /**
     * 添加新的 URL 输入框
     */
    fun addUrl() {
        urls.add("")
        urlErrors.add(false)
    }

    /**
     * 移除指定的 URL 输入框
     */
    fun removeUrl(index: Int) {
        if (urls.size > 1 && index < urls.size) {
            urls.removeAt(index)
            if (index < urlErrors.size) {
                urlErrors.removeAt(index)
            }
        }
    }

    /**
     * 更新指定位置的 URL
     */
    fun updateUrl(index: Int, value: String) {
        if (index < urls.size) {
            urls[index] = value
            if (index < urlErrors.size) {
                urlErrors[index] = false
            }
        }
    }

    /**
     * 切换标签选择
     */
    fun toggleTag(tagId: Long) {
        if (tagId in selectedTagIds) {
            selectedTagIds.remove(tagId)
        } else {
            selectedTagIds.add(tagId)
        }
    }

    /**
     * 清理无效的标签选择
     */
    fun cleanupInvalidTags(validTagIds: Set<Long>) {
        val iterator = selectedTagIds.iterator()
        while (iterator.hasNext()) {
            if (!validTagIds.contains(iterator.next())) {
                iterator.remove()
            }
        }
    }

    /**
     * 验证所有输入
     * @return Pair(hasError, validThreadCount)
     */
    fun validate(): Pair<Boolean, Int?> {
        var hasError = false

        // 验证所有 URL
        urlErrors.clear()
        urls.forEachIndexed { _, url ->
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

        return hasError to threads
    }

    /**
     * 获取有效的 URL 列表
     */
    fun getValidUrls(): List<String> {
        return urls.filter { it.isNotBlank() && it.trim().startsWith("http", ignoreCase = true) }
            .map { it.trim() }
    }
}

/**
 * 创建并记住 AddDownloadState
 */
@Composable
fun rememberAddDownloadState(): AddDownloadState {
    val context = LocalContext.current
    val app = context.applicationContext as LoaderApplication

    val defaultUserAgent by app.appPreferences.defaultUserAgent.collectAsStateWithLifecycle()
    val customUserAgentPresets by app.appPreferences.customUserAgentPresets.collectAsStateWithLifecycle()
    val defaultThreadCount by app.appPreferences.defaultThreadCount.collectAsStateWithLifecycle()
    val webViewUA = remember { UserAgentHelper.getDefaultUserAgent(context) }

    return remember(defaultUserAgent, defaultThreadCount, customUserAgentPresets) {
        AddDownloadState(
            webViewUA = webViewUA,
            defaultUserAgent = defaultUserAgent,
            customUserAgentPresets = customUserAgentPresets,
            defaultThreadCount = defaultThreadCount
        )
    }
}
