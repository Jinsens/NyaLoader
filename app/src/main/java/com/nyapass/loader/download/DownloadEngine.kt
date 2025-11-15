package com.nyapass.loader.download

import android.content.Context
import android.util.Log
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.model.DownloadPartInfo
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.service.DownloadNotificationService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 核心下载引擎
 * 支持多线程下载和断点续传
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
class DownloadEngine(
    private val context: Context,
    private val taskDao: DownloadTaskDao,
    private val partDao: DownloadPartDao,
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "DownloadEngine"
    
    // 是否已启动前台服务
    private var foregroundServiceStarted = false
    
    // 存储正在运行的下载任务
    private val activeDownloads = ConcurrentHashMap<Long, Job>()
    
    // 下载进度流
    private val _downloadProgress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<Long, DownloadProgress>> = _downloadProgress
    
    // 下载作用域
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 开始下载任务
     */
    suspend fun startDownload(taskId: Long) {
        if (activeDownloads.containsKey(taskId)) {
            return
        }
        
        // 启动前台服务
        if (!foregroundServiceStarted) {
            DownloadNotificationService.startService(context)
            foregroundServiceStarted = true
        }
        
        val task = taskDao.getTaskById(taskId) ?: run {
            Log.e(TAG, "任务不存在: $taskId")
            return
        }
        
        Log.i(TAG, "开始下载: ${task.fileName}")
        
        // 更新状态为运行中
        taskDao.updateStatus(taskId, DownloadStatus.RUNNING)
        
        // 重新获取任务以确保使用最新状态
        val updatedTask = taskDao.getTaskById(taskId) ?: return
        
        val job = downloadScope.launch(start = CoroutineStart.DEFAULT) {
            try {
                ensureActive()
                performDownload(updatedTask)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "下载失败: ${task.fileName} - ${e.message}", e)
                val errorMessage = e.message ?: "未知错误"
                try {
                    taskDao.updateStatusWithError(taskId, DownloadStatus.FAILED, errorMessage)
                    DownloadNotificationService.notifyFailed(context, taskId, task.fileName, errorMessage)
                } catch (dbError: Exception) {
                    Log.e(TAG, "更新失败状态时出错: ${dbError.message}")
                }
            } finally {
                activeDownloads.remove(taskId)
            }
        }
        
        activeDownloads[taskId] = job
        withContext(Dispatchers.Default) { delay(50) }
    }
    
    /**
     * 暂停下载
     */
    suspend fun pauseDownload(taskId: Long) {
        val job = activeDownloads[taskId]
        if (job != null) {
            job.cancel()
            activeDownloads.remove(taskId)
        }
        
        // 在后台更新状态
        downloadScope.launch {
            try {
                taskDao.updateStatus(taskId, DownloadStatus.PAUSED)
                job?.join()
            } catch (e: Exception) {
                Log.e(TAG, "暂停失败: $taskId - ${e.message}")
            }
        }
    }
    
    /**
     * 取消下载
     */
    suspend fun cancelDownload(taskId: Long) {
        val job = activeDownloads[taskId]
        if (job != null) {
            job.cancel()
            downloadScope.launch {
                try {
                    job.join()
                } catch (e: Exception) {
                    Log.e(TAG, "取消失败: $taskId - ${e.message}")
                }
            }
        }
        activeDownloads.remove(taskId)
    }
    
    /**
     * 执行下载
     */
    private suspend fun performDownload(task: DownloadTask) {
        val file = File(task.filePath)
        
        // 确保目录存在
        file.parentFile?.mkdirs()
        
        // 如果是新任务，需要初始化
        if (task.totalSize == 0L) {
            initializeDownload(task, file)
            val updatedTask = taskDao.getTaskById(task.id) ?: return
            return performDownload(updatedTask)
        }
        
        // 获取分片信息
        val parts = partDao.getPartsByTaskId(task.id)
        
        if (parts.isEmpty()) {
            createDownloadParts(task)
            val updatedTask = taskDao.getTaskById(task.id) ?: return
            return performDownload(updatedTask)
        }
        
        // 开始多线程下载
        downloadWithMultiThread(task, parts, file)
    }
    
    /**
     * 初始化下载任务
     */
    private suspend fun initializeDownload(task: DownloadTask, file: File) {
        val requestBuilder = Request.Builder()
            .url(task.url)
            .head()
        
        // 添加User-Agent头
        task.userAgent?.let { ua ->
            requestBuilder.addHeader("User-Agent", ua)
        }
        
        val request = requestBuilder.build()
        
        try {
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                response.close()
                throw Exception("服务器响应错误: ${response.code}")
            }
            
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
            val acceptRanges = response.header("Accept-Ranges") == "bytes"
            
            response.close()
            
            if (contentLength == 0L) {
                throw Exception("无法获取文件大小")
            }
            
            // 更新任务信息（保持RUNNING状态）
            val updatedTask = task.copy(
                totalSize = contentLength,
                supportRange = acceptRanges,
                status = DownloadStatus.RUNNING  // 明确保持RUNNING状态
            )
            taskDao.updateTask(updatedTask)
            
            // 创建空文件
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(contentLength)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败: ${task.fileName} - ${e.message}")
            throw e
        }
    }
    
    /**
     * 创建下载分片
     */
    private suspend fun createDownloadParts(task: DownloadTask) {
        val threadCount = if (task.supportRange) task.threadCount else 1
        val partSize = task.totalSize / threadCount
        
        val parts = (0 until threadCount).map { index ->
            val startByte = index * partSize
            val endByte = if (index == threadCount - 1) {
                task.totalSize - 1
            } else {
                startByte + partSize - 1
            }
            
            DownloadPartInfo(
                taskId = task.id,
                partIndex = index,
                startByte = startByte,
                endByte = endByte,
                downloadedByte = startByte
            )
        }
        
        partDao.insertParts(parts)
    }
    
    /**
     * 多线程下载
     */
    private suspend fun downloadWithMultiThread(
        task: DownloadTask,
        parts: List<DownloadPartInfo>,
        file: File
    ) = coroutineScope {
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime
        var lastDownloadedSize = task.downloadedSize
        
        // 为每个未完成的分片创建下载协程
        val incompleteParts = parts.filter { !it.isCompleted }
        
        val jobs = incompleteParts.map { part ->
            async {
                downloadPart(task.id, part, task.url, file)
            }
        }
        
        // 启动进度监控
        val progressJob = launch {
            while (isActive) {
                delay(500)
                
                val currentParts = partDao.getPartsByTaskId(task.id)
                val totalDownloaded = currentParts.sumOf { it.downloadedByte - it.startByte }
                
                val currentTime = System.currentTimeMillis()
                val timeDiff = (currentTime - lastUpdateTime) / 1000.0
                
                val speed = if (timeDiff > 0) {
                    ((totalDownloaded - lastDownloadedSize) / timeDiff).toLong()
                } else {
                    0L
                }
                
                taskDao.updateProgress(task.id, totalDownloaded, speed)
                
                lastUpdateTime = currentTime
                lastDownloadedSize = totalDownloaded
                
                // 计算进度百分比
                val progress = if (task.totalSize > 0) {
                    ((totalDownloaded.toFloat() / task.totalSize) * 100).toInt()
                } else {
                    0
                }
                
                // 更新系统通知
                DownloadNotificationService.updateProgress(
                    context,
                    task.id,
                    task.fileName,
                    progress,
                    totalDownloaded,
                    task.totalSize,
                    speed
                )
                
                // 更新进度流
                _downloadProgress.value = _downloadProgress.value + (task.id to DownloadProgress(
                    taskId = task.id,
                    downloadedSize = totalDownloaded,
                    totalSize = task.totalSize,
                    speed = speed
                ))
            }
        }
        
        // 等待所有分片下载完成
        try {
            jobs.awaitAll()
            progressJob.cancel()
            
            // 标记任务完成
            taskDao.updateStatus(task.id, DownloadStatus.COMPLETED)
            taskDao.updateProgress(task.id, task.totalSize, 0)
            
            // 发送完成通知
            DownloadNotificationService.notifyComplete(context, task.id, task.fileName)
            
            Log.i(TAG, "下载完成: ${task.fileName}")
        } catch (e: Exception) {
            Log.e(TAG, "下载出错: ${task.fileName} - ${e.message}")
            progressJob.cancel()
            throw e
        }
    }
    
    /**
     * 下载单个分片
     * 
     * 性能优化：
     * 1. 256KB 大缓冲区
     * 2. 批量数据库更新（每1MB）
     * 3. BufferedOutputStream
     * 4. 降低取消检查频率
     */
    private suspend fun downloadPart(
        taskId: Long,
        part: DownloadPartInfo,
        url: String,
        file: File
    ) = withContext(Dispatchers.IO) {
        var response: okhttp3.Response? = null
        try {
            // 提前检查取消状态
            ensureActive()
            
            // 获取任务信息以使用自定义UA
            val task = taskDao.getTaskById(taskId)
            
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=${part.downloadedByte}-${part.endByte}")
            
            // 添加User-Agent头
            task?.userAgent?.let { ua ->
                requestBuilder.addHeader("User-Agent", ua)
            }
            
            val request = requestBuilder.build()
            response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful && response.code != 206) {
                throw Exception("分片下载失败: ${response.code}")
            }
            
           
            val BUFFER_SIZE = 256 * 1024        // 256KB 缓冲区
            val DB_UPDATE_INTERVAL = 1024 * 1024 // 每 1MB 更新数据库
            val CANCEL_CHECK_INTERVAL = 100      // 每 100 次循环检查取消
            
            val buffer = ByteArray(BUFFER_SIZE)
            var currentPosition = part.downloadedByte
            var bytesReadSinceLastUpdate = 0L
            var loopCount = 0
            
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(currentPosition)
                
                // 使用 BufferedOutputStream 进一步优化写入性能
                java.io.BufferedOutputStream(
                    object : java.io.OutputStream() {
                        override fun write(b: Int) = raf.write(b)
                        override fun write(b: ByteArray, off: Int, len: Int) = raf.write(b, off, len)
                    },
                    BUFFER_SIZE  // 256KB 内核缓冲区
                ).use { output ->
                    response.body?.byteStream()?.use { input ->
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // 写入数据
                            output.write(buffer, 0, bytesRead)
                            currentPosition += bytesRead
                            bytesReadSinceLastUpdate += bytesRead
                            loopCount++
                            
                            // 批量更新数据库（每1MB或每100次循环）
                            if (bytesReadSinceLastUpdate >= DB_UPDATE_INTERVAL) {
                                partDao.updatePartProgress(part.id, currentPosition)
                                bytesReadSinceLastUpdate = 0
                            }
                            
                            // 降低取消检查频率（每100次循环，约25MB）
                            if (loopCount % CANCEL_CHECK_INTERVAL == 0) {
                                ensureActive()
                            }
                        }
                        
                        // 强制刷新缓冲区到磁盘
                        output.flush()
                    }
                }
            }
            
            // 最后一次更新数据库
            partDao.updatePartProgress(part.id, currentPosition)
            
            // 标记分片完成
            partDao.markPartCompleted(part.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "分片${part.partIndex}下载失败: ${e.message}")
            throw e
        } finally {
            // 确保response被关闭
            response?.close()
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        downloadScope.cancel()
    }
}

/**
 * 下载进度数据类
 */
@androidx.compose.runtime.Immutable
data class DownloadProgress(
    val taskId: Long,
    val downloadedSize: Long,
    val totalSize: Long,
    val speed: Long
) {
    val progress: Float
        get() = if (totalSize > 0) {
            (downloadedSize.toFloat() / totalSize.toFloat() * 100).coerceIn(0f, 100f)
        } else {
            0f
        }
}

