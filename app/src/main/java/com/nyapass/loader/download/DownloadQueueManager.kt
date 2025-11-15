package com.nyapass.loader.download

import android.util.Log
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载队列管理器
 * 负责管理并发下载数量和任务排队
 *
 * @author 小花生FMR
 * @version 1.0.0
 */
@Singleton
class DownloadQueueManager @Inject constructor(
    private val taskDao: DownloadTaskDao,
    private val preferences: AppPreferences,
    private val downloadEngine: DownloadEngine
) {
    private val TAG = "DownloadQueueManager"

    // 队列管理作用域
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 当前正在下载的任务ID集合
    private val _activeDownloads = MutableStateFlow<Set<Long>>(emptySet())
    val activeDownloads: StateFlow<Set<Long>> = _activeDownloads.asStateFlow()

    // 等待中的任务队列
    private val _pendingQueue = MutableStateFlow<List<Long>>(emptyList())
    val pendingQueue: StateFlow<List<Long>> = _pendingQueue.asStateFlow()

    // 互斥锁，确保队列操作的线程安全
    private val queueMutex = Mutex()

    // 最大并发下载数
    private val maxConcurrentDownloads: Int
        get() = preferences.maxConcurrentDownloads.value

    init {
        // 监听任务状态变化
        scope.launch {
            taskDao.getAllTasks().collect { tasks ->
                processTaskStatusChanges(tasks)
            }
        }
    }

    /**
     * 将任务加入下载队列
     * 如果当前下载数未达上限，立即开始下载
     * 否则加入等待队列
     */
    suspend fun enqueue(taskId: Long): Boolean = queueMutex.withLock {
        val task = taskDao.getTaskById(taskId) ?: return@withLock false

        // 如果任务已经在活动列表中，忽略
        if (taskId in _activeDownloads.value) {
            Log.d(TAG, "任务 $taskId 已在下载中")
            return@withLock true
        }

        // 如果当前下载数未达上限，直接开始
        if (_activeDownloads.value.size < maxConcurrentDownloads) {
            startDownloadInternal(taskId)
            return@withLock true
        }

        // 否则加入等待队列
        if (taskId !in _pendingQueue.value) {
            _pendingQueue.value = _pendingQueue.value + taskId
            taskDao.updateStatus(taskId, DownloadStatus.PENDING)
            Log.i(TAG, "任务 $taskId 已加入等待队列，当前队列长度: ${_pendingQueue.value.size}")
        }

        return@withLock true
    }

    /**
     * 任务完成或失败时调用
     * 自动开始下一个等待中的任务
     */
    suspend fun onTaskCompleted(taskId: Long) = queueMutex.withLock {
        // 从活动列表移除
        _activeDownloads.value = _activeDownloads.value - taskId
        Log.i(TAG, "任务 $taskId 已完成/失败，当前活动下载数: ${_activeDownloads.value.size}")

        // 尝试开始下一个等待任务
        startNextPendingTask()
    }

    /**
     * 暂停任务
     */
    suspend fun pauseTask(taskId: Long) = queueMutex.withLock {
        // 从活动列表移除
        if (taskId in _activeDownloads.value) {
            downloadEngine.pauseDownload(taskId)
            _activeDownloads.value = _activeDownloads.value - taskId
        }

        // 从等待队列移除
        _pendingQueue.value = _pendingQueue.value - taskId

        // 尝试开始下一个任务
        startNextPendingTask()
    }

    /**
     * 取消任务
     */
    suspend fun cancelTask(taskId: Long) = queueMutex.withLock {
        // 从活动列表移除
        if (taskId in _activeDownloads.value) {
            downloadEngine.cancelDownload(taskId)
            _activeDownloads.value = _activeDownloads.value - taskId
        }

        // 从等待队列移除
        _pendingQueue.value = _pendingQueue.value - taskId

        // 尝试开始下一个任务
        startNextPendingTask()
    }

    /**
     * 获取任务在队列中的位置
     * 返回 -1 表示正在下载或不在队列中
     */
    fun getQueuePosition(taskId: Long): Int {
        if (taskId in _activeDownloads.value) return -1
        return _pendingQueue.value.indexOf(taskId)
    }

    /**
     * 获取当前活动下载数
     */
    fun getActiveDownloadCount(): Int = _activeDownloads.value.size

    /**
     * 获取等待队列长度
     */
    fun getPendingQueueSize(): Int = _pendingQueue.value.size

    /**
     * 检查是否可以开始新下载
     */
    fun canStartNewDownload(): Boolean = _activeDownloads.value.size < maxConcurrentDownloads

    /**
     * 内部方法：开始下载任务
     */
    private suspend fun startDownloadInternal(taskId: Long) {
        _activeDownloads.value = _activeDownloads.value + taskId
        scope.launch(Dispatchers.IO) {
            try {
                downloadEngine.startDownload(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "启动下载失败: $taskId - ${e.message}")
                queueMutex.withLock {
                    _activeDownloads.value = _activeDownloads.value - taskId
                    startNextPendingTask()
                }
            }
        }
        Log.i(TAG, "开始下载任务 $taskId，当前活动下载数: ${_activeDownloads.value.size}")
    }

    /**
     * 开始下一个等待中的任务
     * 按优先级顺序选择任务
     */
    private suspend fun startNextPendingTask() {
        while (_activeDownloads.value.size < maxConcurrentDownloads && _pendingQueue.value.isNotEmpty()) {
            // 按优先级排序，选择最高优先级的任务
            val pendingTasks = _pendingQueue.value.mapNotNull { taskDao.getTaskById(it) }
                .filter { it.status == DownloadStatus.PENDING }
                .sortedByDescending { it.priority }

            if (pendingTasks.isEmpty()) {
                _pendingQueue.value = emptyList()
                break
            }

            val nextTask = pendingTasks.first()
            _pendingQueue.value = _pendingQueue.value - nextTask.id

            startDownloadInternal(nextTask.id)
            Log.i(TAG, "自动开始下一个任务 ${nextTask.id} (优先级: ${nextTask.priority})")
        }
    }

    /**
     * 更新任务优先级
     * 如果任务在等待队列中，会重新排序
     */
    suspend fun updateTaskPriority(taskId: Long, priority: Int) = queueMutex.withLock {
        taskDao.updatePriority(taskId, priority)
        Log.i(TAG, "任务 $taskId 优先级已更新为 $priority")
    }

    /**
     * 处理任务状态变化
     * 用于同步队列状态
     */
    private suspend fun processTaskStatusChanges(tasks: List<DownloadTask>) = queueMutex.withLock {
        // 找出已完成或失败的任务
        val completedOrFailed = tasks.filter {
            it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED
        }.map { it.id }

        // 从活动列表中移除
        val wasActive = _activeDownloads.value.intersect(completedOrFailed.toSet())
        if (wasActive.isNotEmpty()) {
            _activeDownloads.value = _activeDownloads.value - wasActive
            Log.d(TAG, "移除已完成/失败的任务: $wasActive")

            // 尝试开始下一个任务
            startNextPendingTask()
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        _activeDownloads.value = emptySet()
        _pendingQueue.value = emptyList()
    }
}
