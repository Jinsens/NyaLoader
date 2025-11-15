package com.nyapass.loader.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadStatsSummary
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.model.FileTypeCategory
import com.nyapass.loader.data.model.FileTypeStats
import com.nyapass.loader.data.model.TagStatistics
import com.nyapass.loader.data.model.getProgress
import com.nyapass.loader.download.DownloadProgress
import com.nyapass.loader.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 下载ViewModel
 * 使用 Hilt 进行依赖注入
 */
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadRepository,
    private val application: Application
) : ViewModel() {
    
    private val TAG = "DownloadViewModel"
    
    // 所有下载任务
    val allTasks: StateFlow<List<DownloadTask>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 下载进度
    val downloadProgress: StateFlow<Map<Long, DownloadProgress>> = repository.getDownloadProgress()

    // 标签列表
    val tags: StateFlow<List<DownloadTag>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 标签统计
    val tagStatistics: StateFlow<List<TagStatistics>> = repository.getTagStatistics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 任务对应的标签
    private val taskTags: StateFlow<Map<Long, List<DownloadTag>>> = repository.getTaskTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    // UI状态
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()
    
    // 组合任务和进度（带去重优化）
    val tasksWithProgress: StateFlow<List<TaskWithProgress>> = combine(
        allTasks,
        downloadProgress,
        taskTags
    ) { tasks, progress, tags ->
        tasks.map { task ->
            val progressInfo = progress[task.id]
            TaskWithProgress(
                task = task,
                progress = progressInfo?.progress ?: task.getProgress(),
                speed = progressInfo?.speed ?: task.speed,
                tags = tags[task.id].orEmpty()
            )
        }
    }
        // 进度去重优化：只有当进度变化 >= 1% 或状态变化时才更新 UI
        .distinctUntilChanged { old, new ->
            if (old.size != new.size) return@distinctUntilChanged false
            old.zip(new).all { (oldTask, newTask) ->
                // 任务ID不同，认为有变化
                if (oldTask.task.id != newTask.task.id) return@all false
                // 状态变化，必须更新
                if (oldTask.task.status != newTask.task.status) return@all false
                // 标签变化，必须更新
                if (oldTask.tags != newTask.tags) return@all false
                // 进度变化小于 1%，认为没有变化
                val progressDiff = kotlin.math.abs(oldTask.progress - newTask.progress)
                progressDiff < 1f
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * 创建新下载任务
     */
    fun createDownloadTask(
        url: String,
        fileName: String? = null,
        threadCount: Int = 4,
        saveToPublicDir: Boolean = true,
        customPath: String? = null,
        userAgent: String? = null,
        cookie: String? = null,
        referer: String? = null,
        customHeaders: String? = null,
        tagIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
                
            if (url.isBlank()) {
                _uiState.update { it.copy(error = application.getString(R.string.error_url_empty), isLoading = false) }
                return@launch
            }
            
            val taskId = try {
                Log.i(TAG, "创建下载任务: url=$url, fileName=$fileName, threadCount=$threadCount")
                withContext(Dispatchers.IO) {
                    repository.createDownloadTask(
                        url = url,
                        fileName = fileName,
                        threadCount = threadCount,
                        saveToPublicDir = saveToPublicDir,
                        customPath = customPath,
                        userAgent = userAgent,
                        cookie = cookie,
                        referer = referer,
                        customHeaders = customHeaders,
                        tagIds = tagIds
                    )
                }.also { Log.i(TAG, "任务创建成功: taskId=$it") }
            } catch (e: Exception) {
                Log.e(TAG, "创建任务失败: ${e.message}", e)
                _uiState.update { 
                    it.copy(error = application.getString(R.string.error_create_task_failed, e.message ?: ""), isLoading = false) 
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = false, showAddDialog = false) }
            
            startTaskAsync(taskId)
        }
    }
    
    private fun startTaskAsync(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "开始启动下载: taskId=$taskId")
            try {
                repository.startDownload(taskId)
                Log.i(TAG, "下载启动完成: taskId=$taskId")
            } catch (startError: Exception) {
                Log.e(TAG, "启动下载时出错: taskId=$taskId, error=${startError.message}", startError)
                withContext(Dispatchers.Main) {
                    _uiState.update { 
                        it.copy(error = application.getString(R.string.error_start_download_failed, startError.message ?: "")) 
                    }
                }
            }
        }
    }
    
    /**
     * 开始下载
     */
    fun startDownload(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "开始下载: taskId=$taskId")
                repository.startDownload(taskId)
                Log.i(TAG, "开始下载成功: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "启动下载失败: taskId=$taskId, error=${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_start_download_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload(taskId: Long) {
        // 使用 Default 调度器，避免阻塞 IO 线程池
        viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.i(TAG, "暂停下载: taskId=$taskId")
                repository.pauseDownload(taskId)
                Log.i(TAG, "暂停下载成功: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "暂停下载失败: taskId=$taskId, error=${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_pause_download_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 恢复下载
     */
    fun resumeDownload(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "恢复下载: taskId=$taskId")
                repository.resumeDownload(taskId)
                Log.i(TAG, "恢复下载成功: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "恢复下载失败: taskId=$taskId, error=${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_resume_download_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "取消下载: taskId=$taskId")
                repository.cancelDownload(taskId)
                Log.i(TAG, "取消下载成功: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "取消下载失败: taskId=$taskId, error=${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_cancel_download_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 删除任务
     */
    fun deleteTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "删除任务: taskId=$taskId")
                repository.deleteTask(taskId)
                Log.i(TAG, "删除任务成功: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "删除任务失败: taskId=$taskId, error=${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_delete_task_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 重试失败的任务
     */
    fun retryTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "重试任务: taskId=$taskId")
                repository.retryFailedTask(taskId)
                Log.i(TAG, "重试任务成功: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "重试任务失败: taskId=$taskId, error=${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_retry_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 清除已完成的任务
     */
    fun clearCompletedTasks() {
        viewModelScope.launch {
            try {
                repository.clearCompletedTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = application.getString(R.string.error_clear_failed, e.message ?: "")) }
            }
        }
    }
    
    /**
     * 清除失败的任务
     */
    fun clearFailedTasks() {
        viewModelScope.launch {
            try {
                repository.clearFailedTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = application.getString(R.string.error_clear_failed, e.message ?: "")) }
            }
        }
    }
    
    /**
     * 显示添加对话框
     */
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }
    
    /**
     * 隐藏添加对话框
     */
    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * 更新过滤器
     */
    fun updateFilter(filter: TaskFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
    
    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    /**
     * 开启搜索
     */
    fun startSearch() {
        _uiState.update { it.copy(isSearching = true) }
    }
    
    /**
     * 关闭搜索
     */
    fun stopSearch() {
        _uiState.update { it.copy(isSearching = false, searchQuery = "") }
    }

    /**
     * 重置标签筛选
     */
    fun selectAllTags() {
        _uiState.update { it.copy(selectedTagId = null, showOnlyUntagged = false) }
    }

    /**
     * 选择指定标签
     */
    fun selectTag(tagId: Long) {
        _uiState.update { it.copy(selectedTagId = tagId, showOnlyUntagged = false) }
    }

    /**
     * 筛选未打标签的任务
     */
    fun selectUntagged() {
        _uiState.update { it.copy(selectedTagId = null, showOnlyUntagged = true) }
    }

    /**
     * 创建新标签
     */
    fun createTag(name: String, color: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.createTag(name, color)
            } catch (e: Exception) {
                Log.e(TAG, "创建标签失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_create_tag_failed, e.message ?: "")) }
                }
            }
        }
    }

    /**
     * 删除标签
     */
    fun deleteTag(tagId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteTag(tagId)
                withContext(Dispatchers.Main) {
                    if (_uiState.value.selectedTagId == tagId) {
                        selectAllTags()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除标签失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_delete_tag_failed, e.message ?: "")) }
                }
            }
        }
    }

    fun updateTag(tagId: Long, name: String, color: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTag(tagId, name, color)
            } catch (e: Exception) {
                Log.e(TAG, "更新标签失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_update_tag_failed, e.message ?: "")) }
                }
            }
        }
    }

    fun swapTagOrder(firstTagId: Long, secondTagId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.swapTagOrder(firstTagId, secondTagId)
            } catch (e: Exception) {
                Log.e(TAG, "调整标签顺序失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_reorder_tag_failed, e.message ?: "")) }
                }
            }
        }
    }

    /**
     * 更新任务的标签
     */
    fun updateTaskTags(taskId: Long, tagIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTaskTags(taskId, tagIds)
                Log.i(TAG, "更新任务标签成功: taskId=$taskId, tagIds=$tagIds")
            } catch (e: Exception) {
                Log.e(TAG, "更新任务标签失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_update_task_tags_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 打开文件
     */
    fun openFile(context: android.content.Context, task: DownloadTask) {
        viewModelScope.launch {
            try {
                val chooserTitle = context.getString(com.nyapass.loader.R.string.choose_app_to_open)
                val success = repository.openFile(context, task, chooserTitle)
                if (!success) {
                    val errorMsg = context.getString(com.nyapass.loader.R.string.open_file_failed)
                    _uiState.update { it.copy(error = errorMsg) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "打开文件失败: ${e.message}", e)
                val errorMsg = context.getString(com.nyapass.loader.R.string.open_file_failed)
                _uiState.update { it.copy(error = application.getString(R.string.open_file_failed)) }
            }
        }
    }
    
    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode(initialTaskId: Long) {
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                selectedTaskIds = setOf(initialTaskId)
            )
        }
    }
    
    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = false,
                selectedTaskIds = emptySet()
            )
        }
    }
    
    /**
     * 切换任务选中状态
     */
    fun toggleTaskSelection(taskId: Long) {
        _uiState.update { state ->
            val newSelectedIds = if (taskId in state.selectedTaskIds) {
                state.selectedTaskIds - taskId
            } else {
                state.selectedTaskIds + taskId
            }
            
            // 如果没有选中的任务，自动退出多选模式
            if (newSelectedIds.isEmpty()) {
                state.copy(
                    isMultiSelectMode = false,
                    selectedTaskIds = emptySet()
                )
            } else {
                state.copy(selectedTaskIds = newSelectedIds)
            }
        }
    }
    
    /**
     * 批量删除选中的任务
     */
    fun deleteSelectedTasks() {
        val selectedIds = _uiState.value.selectedTaskIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                selectedIds.forEach { taskId ->
                    repository.deleteTask(taskId)
                }
                withContext(Dispatchers.Main) {
                    exitMultiSelectMode()
                }
            } catch (e: Exception) {
                Log.e(TAG, "批量删除失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_batch_delete_failed, e.message ?: "")) }
                }
            }
        }
    }
    
    /**
     * 批量重试选中的任务
     */
    fun retrySelectedTasks() {
        val selectedIds = _uiState.value.selectedTaskIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                selectedIds.forEach { taskId ->
                    repository.retryFailedTask(taskId)
                }
                withContext(Dispatchers.Main) {
                    exitMultiSelectMode()
                }
            } catch (e: Exception) {
                Log.e(TAG, "批量重试失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_batch_retry_failed, e.message ?: "")) }
                }
            }
        }
    }

    /**
     * 全选当前过滤后的任务
     */
    fun selectAllTasks() {
        val currentTasks = filteredTasks.value
        val allTaskIds = currentTasks.map { it.task.id }.toSet()
        _uiState.update { state ->
            state.copy(
                isMultiSelectMode = allTaskIds.isNotEmpty(),
                selectedTaskIds = allTaskIds
            )
        }
    }

    /**
     * 反选当前过滤后的任务
     */
    fun invertSelection() {
        val currentTasks = filteredTasks.value
        val allTaskIds = currentTasks.map { it.task.id }.toSet()
        val currentSelected = _uiState.value.selectedTaskIds
        val invertedSelection = allTaskIds - currentSelected

        _uiState.update { state ->
            if (invertedSelection.isEmpty()) {
                state.copy(
                    isMultiSelectMode = false,
                    selectedTaskIds = emptySet()
                )
            } else {
                state.copy(selectedTaskIds = invertedSelection)
            }
        }
    }

    /**
     * 刷新任务列表
     * 重新触发数据流收集，用于下拉刷新
     */
    fun refreshTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                // 短暂延迟以显示刷新动画
                kotlinx.coroutines.delay(500)
                // 数据会通过 Flow 自动更新，这里只需要等待一下
                Log.i(TAG, "任务列表刷新完成")
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * 获取过滤后的任务
     */
    val filteredTasks: StateFlow<List<TaskWithProgress>> = combine(
        tasksWithProgress,
        uiState
    ) { tasks, state ->
        // 先按状态过滤
        val statusFiltered = when (state.filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.DOWNLOADING -> tasks.filter { 
                it.task.status == DownloadStatus.RUNNING 
            }
            TaskFilter.COMPLETED -> tasks.filter { 
                it.task.status == DownloadStatus.COMPLETED 
            }
            TaskFilter.PAUSED -> tasks.filter { 
                it.task.status == DownloadStatus.PAUSED 
            }
            TaskFilter.FAILED -> tasks.filter { 
                it.task.status == DownloadStatus.FAILED 
            }
        }
        
        // 标签过滤
        val tagFiltered = when {
            state.showOnlyUntagged -> statusFiltered.filter { it.tags.isEmpty() }
            state.selectedTagId != null -> statusFiltered.filter { task ->
                task.tags.any { tag -> tag.id == state.selectedTagId }
            }
            else -> statusFiltered
        }
        
        // 再按搜索关键词过滤
        if (state.searchQuery.isBlank()) {
            tagFiltered
        } else {
            val query = state.searchQuery.lowercase()
            tagFiltered.filter { taskWithProgress ->
                taskWithProgress.task.fileName.lowercase().contains(query) ||
                taskWithProgress.task.url.lowercase().contains(query) ||
                taskWithProgress.task.filePath.lowercase().contains(query) ||
                (taskWithProgress.task.finalContentUri?.lowercase()?.contains(query) == true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * 总下载进度
     */
    val totalProgress: StateFlow<TotalProgress> = tasksWithProgress.map { tasks ->
        // 只统计未完成的任务
        val activeTasks = tasks.filter { 
            it.task.status == DownloadStatus.RUNNING || 
            it.task.status == DownloadStatus.PAUSED ||
            it.task.status == DownloadStatus.PENDING
        }
        
        // 当活动任务数量大于2时显示总进度
        if (activeTasks.size > 1) {
            val totalSize = activeTasks.sumOf { it.task.totalSize }
            val downloadedSize = activeTasks.sumOf { it.task.downloadedSize }
            val totalSpeed = activeTasks
                .filter { it.task.status == DownloadStatus.RUNNING }
                .sumOf { it.speed }
            val progress = if (totalSize > 0) {
                (downloadedSize.toFloat() / totalSize.toFloat() * 100).coerceIn(0f, 100f)
            } else {
                0f
            }
            
            TotalProgress(
                showProgress = true,
                totalSize = totalSize,
                downloadedSize = downloadedSize,
                progress = progress,
                activeTaskCount = activeTasks.size,
                totalSpeed = totalSpeed
            )
        } else {
            TotalProgress(
                showProgress = false,
                totalSize = 0L,
                downloadedSize = 0L,
                progress = 0f,
                activeTaskCount = activeTasks.size,
                totalSpeed = 0L
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TotalProgress()
    )
    
    // ==================== 下载统计 ====================

    private val _statisticsState = MutableStateFlow<StatisticsState>(StatisticsState.Loading)
    val statisticsState: StateFlow<StatisticsState> = _statisticsState.asStateFlow()

    /**
     * 加载下载统计数据（使用独立的统计表）
     */
    fun loadStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            _statisticsState.value = StatisticsState.Loading
            try {
                // 从独立统计表获取持久化的统计数据
                val statsCount = repository.getStatsCount()
                val statsTotalSize = repository.getStatsTotalSize()
                val statsByType = repository.getStatsByFileType()

                // 当前活跃任务中的失败数（用于显示）
                val activeFailedCount = repository.getActiveFailedCount()

                // 转换为 UI 模型
                val fileTypeStats = statsByType.map { result ->
                    FileTypeStats(
                        type = result.fileType,
                        count = result.count,
                        totalSize = result.totalSize
                    )
                }

                val summary = DownloadStatsSummary(
                    totalDownloads = statsCount,
                    completedDownloads = statsCount, // 统计表只记录已完成的
                    failedDownloads = activeFailedCount,
                    totalDownloadedSize = statsTotalSize,
                    fileTypeStats = fileTypeStats
                )

                withContext(Dispatchers.Main) {
                    _statisticsState.value = StatisticsState.Success(summary)
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载统计数据失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _statisticsState.value = StatisticsState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * 清除所有统计数据
     */
    fun clearAllStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearAllStats()
                // 重新加载统计数据
                loadStatistics()
                Log.i(TAG, "已清除所有下载统计数据")
            } catch (e: Exception) {
                Log.e(TAG, "清除统计数据失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = application.getString(R.string.error_clear_failed, e.message ?: "")) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

/**
 * 统计页面状态
 */
sealed class StatisticsState {
    data object Loading : StatisticsState()
    data class Success(val data: DownloadStatsSummary) : StatisticsState()
    data class Error(val message: String) : StatisticsState()
}

/**
 * UI状态
 */
@Immutable
data class DownloadUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val filter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedTagId: Long? = null,
    val showOnlyUntagged: Boolean = false,
    val isMultiSelectMode: Boolean = false,
    val selectedTaskIds: Set<Long> = emptySet()
)

/**
 * 任务过滤器
 */
enum class TaskFilter {
    ALL,
    DOWNLOADING,
    COMPLETED,
    PAUSED,
    FAILED
}

/**
 * 带进度的任务
 */
@Immutable
data class TaskWithProgress(
    val task: DownloadTask,
    val progress: Float,
    val speed: Long,
    val tags: List<DownloadTag> = emptyList()
)

/**
 * 总下载进度
 */
@Immutable
data class TotalProgress(
    val showProgress: Boolean = false,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val progress: Float = 0f,
    val activeTaskCount: Int = 0,
    val totalSpeed: Long = 0L
)
