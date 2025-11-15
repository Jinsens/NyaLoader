package com.nyapass.loader.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.model.getProgress
import com.nyapass.loader.download.DownloadProgress
import com.nyapass.loader.repository.DownloadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 下载ViewModel
 */
class DownloadViewModel(
    private val repository: DownloadRepository
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
    
    // UI状态
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()
    
    // 组合任务和进度
    val tasksWithProgress: StateFlow<List<TaskWithProgress>> = combine(
        allTasks,
        downloadProgress
    ) { tasks, progress ->
        tasks.map { task ->
            val progressInfo = progress[task.id]
            TaskWithProgress(
                task = task,
                progress = progressInfo?.progress ?: task.getProgress(),
                speed = progressInfo?.speed ?: task.speed
            )
        }
    }.stateIn(
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
        userAgent: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
                
                if (url.isBlank()) {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(error = "URL不能为空", isLoading = false) }
                    }
                    return@launch
                }
                
                Log.i(TAG, "创建下载任务: url=$url, fileName=$fileName, threadCount=$threadCount")
                val taskId = repository.createDownloadTask(url, fileName, threadCount, saveToPublicDir, customPath, userAgent)
                Log.i(TAG, "任务创建成功: taskId=$taskId")
                
                // 等待一小段时间确保任务已写入数据库
                kotlinx.coroutines.delay(100)
                
                // 立即启动下载
                Log.i(TAG, "开始启动下载: taskId=$taskId")
                try {
                    repository.startDownload(taskId)
                    Log.i(TAG, "✅ 下载启动完成: taskId=$taskId")
                    
                    // 等待一小段时间确保下载Job已开始执行
                    kotlinx.coroutines.delay(100)
                    Log.i(TAG, "下载Job应已开始执行: taskId=$taskId")
                } catch (startError: Exception) {
                    Log.e(TAG, "❌ 启动下载时出错: taskId=$taskId, error=${startError.message}", startError)
                    throw startError
                }
                
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false, showAddDialog = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建任务失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { 
                        it.copy(error = "创建任务失败: ${e.message}", isLoading = false) 
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
                    _uiState.update { it.copy(error = "启动下载失败: ${e.message}") }
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
                    _uiState.update { it.copy(error = "暂停下载失败: ${e.message}") }
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
                    _uiState.update { it.copy(error = "恢复下载失败: ${e.message}") }
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
                    _uiState.update { it.copy(error = "取消下载失败: ${e.message}") }
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
                    _uiState.update { it.copy(error = "删除任务失败: ${e.message}") }
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
                    _uiState.update { it.copy(error = "重试失败: ${e.message}") }
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
                _uiState.update { it.copy(error = "清除失败: ${e.message}") }
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
                _uiState.update { it.copy(error = "清除失败: ${e.message}") }
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
     * 打开文件
     */
    fun openFile(context: android.content.Context, filePath: String) {
        viewModelScope.launch {
            try {
                val chooserTitle = context.getString(com.nyapass.loader.R.string.choose_app_to_open)
                val success = repository.openFile(context, filePath, chooserTitle)
                if (!success) {
                    // 文件不存在或没有应用可以打开
                    val file = java.io.File(filePath)
                    val errorMsg = when {
                        !file.exists() -> context.getString(com.nyapass.loader.R.string.file_not_found)
                        else -> context.getString(com.nyapass.loader.R.string.no_app_to_open_file)
                    }
                    _uiState.update { it.copy(error = errorMsg) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "打开文件失败: ${e.message}", e)
                val errorMsg = context.getString(com.nyapass.loader.R.string.open_file_failed)
                _uiState.update { it.copy(error = "$errorMsg: ${e.message}") }
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
        
        // 再按搜索关键词过滤
        if (state.searchQuery.isBlank()) {
            statusFiltered
        } else {
            val query = state.searchQuery.lowercase()
            statusFiltered.filter { taskWithProgress ->
                taskWithProgress.task.fileName.lowercase().contains(query) ||
                taskWithProgress.task.url.lowercase().contains(query) ||
                taskWithProgress.task.filePath.lowercase().contains(query)
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
    
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

/**
 * UI状态
 */
data class DownloadUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val filter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val isSearching: Boolean = false
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
data class TaskWithProgress(
    val task: DownloadTask,
    val progress: Float,
    val speed: Long
)

/**
 * 总下载进度
 */
data class TotalProgress(
    val showProgress: Boolean = false,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val progress: Float = 0f,
    val activeTaskCount: Int = 0,
    val totalSpeed: Long = 0L
)

