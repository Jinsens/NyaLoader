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
        Log.i(TAG, "【DownloadEngine】startDownload 被调用: taskId=$taskId")
        
        if (activeDownloads.containsKey(taskId)) {
            Log.i(TAG, "【DownloadEngine】任务已在下载中: taskId=$taskId")
            return
        }
        
        // 启动前台服务
        if (!foregroundServiceStarted) {
            Log.i(TAG, "【DownloadEngine】启动前台服务")
            DownloadNotificationService.startService(context)
            foregroundServiceStarted = true
        }
        
        Log.i(TAG, "【DownloadEngine】从数据库查询任务: taskId=$taskId")
        val task = taskDao.getTaskById(taskId) ?: run {
            Log.e(TAG, "【DownloadEngine】任务不存在: $taskId")
            return
        }
        
        Log.i(TAG, "【DownloadEngine】找到任务: fileName=${task.fileName}, status=${task.status}")
        
        // 更新状态为运行中
        Log.i(TAG, "【DownloadEngine】更新状态为RUNNING: taskId=$taskId")
        taskDao.updateStatus(taskId, DownloadStatus.RUNNING)
        Log.i(TAG, "【DownloadEngine】状态更新完成: taskId=$taskId")
        
        // 重新获取任务以确保使用最新状态
        val updatedTask = taskDao.getTaskById(taskId) ?: run {
            Log.e(TAG, "【DownloadEngine】重新获取任务失败: $taskId")
            return
        }
        
        Log.i(TAG, "【DownloadEngine】创建下载Job: taskId=$taskId")
        Log.i(TAG, "【DownloadEngine】下载作用域状态: isActive=${downloadScope.isActive}")
        
        val job = downloadScope.launch(start = CoroutineStart.DEFAULT) {
            Log.i(TAG, "【DownloadEngine】✅ Job开始执行: taskId=$taskId")
            Log.i(TAG, "【DownloadEngine】协程上下文: ${coroutineContext[Job]}")
            
            try {
                // 确保协程处于活跃状态
                ensureActive()
                Log.i(TAG, "【DownloadEngine】协程状态检查通过,开始下载: taskId=$taskId")
                
                performDownload(updatedTask)
                Log.i(TAG, "【DownloadEngine】Job执行完成: taskId=$taskId")
            } catch (e: CancellationException) {
                Log.i(TAG, "【DownloadEngine】Job被取消: taskId=$taskId")
                // 暂停由pauseDownload处理，不需要重复更新状态
                // 直接抛出，让Job结束
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "【DownloadEngine】下载失败: ${task.fileName} - ${e.message}", e)
                e.printStackTrace()
                val errorMessage = e.message ?: "未知错误"
                try {
                    taskDao.updateStatusWithError(taskId, DownloadStatus.FAILED, errorMessage)
                    DownloadNotificationService.notifyFailed(
                        context,
                        taskId,
                        task.fileName,
                        errorMessage
                    )
                } catch (dbError: Exception) {
                    Log.e(TAG, "【DownloadEngine】更新失败状态时出错: ${dbError.message}", dbError)
                }
            } finally {
                Log.i(TAG, "【DownloadEngine】Job清理: taskId=$taskId")
                activeDownloads.remove(taskId)
                Log.i(TAG, "【DownloadEngine】Job已从activeDownloads移除: taskId=$taskId")
            }
        }
        
        activeDownloads[taskId] = job
        Log.i(TAG, "【DownloadEngine】Job已加入activeDownloads: taskId=$taskId, 活动任务数=${activeDownloads.size}")
        Log.i(TAG, "【DownloadEngine】Job状态: isActive=${job.isActive}, isCompleted=${job.isCompleted}, isCancelled=${job.isCancelled}")
        
        // 等待一小段时间确保Job开始执行
        withContext(Dispatchers.Default) {
            delay(50)
            Log.i(TAG, "【DownloadEngine】延迟后Job状态: isActive=${job.isActive}, isCompleted=${job.isCompleted}")
        }
    }
    
    /**
     * 暂停下载
     */
    suspend fun pauseDownload(taskId: Long) {
        Log.i(TAG, "【DownloadEngine】暂停下载: taskId=$taskId")
        
        // 先取消Job，再更新状态（避免数据库操作阻塞）
        val job = activeDownloads[taskId]
        if (job != null) {
            Log.i(TAG, "【DownloadEngine】找到Job，立即取消: taskId=$taskId")
            job.cancel()  // 立即取消，不等待
            activeDownloads.remove(taskId)  // 立即从活动列表移除
        } else {
            Log.i(TAG, "【DownloadEngine】未找到活动Job: taskId=$taskId")
        }
        
        // 在后台更新状态，不阻塞调用方
        downloadScope.launch {
            try {
                taskDao.updateStatus(taskId, DownloadStatus.PAUSED)
                Log.i(TAG, "【DownloadEngine】状态已更新为PAUSED: taskId=$taskId")
                
                // 等待Job完全停止
                job?.join()
                Log.i(TAG, "【DownloadEngine】Job已完全停止: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "【DownloadEngine】暂停处理异常: taskId=$taskId, error=${e.message}")
            }
        }
    }
    
    /**
     * 取消下载（只停止Job，不删除文件和数据库）
     */
    suspend fun cancelDownload(taskId: Long) {
        Log.i(TAG, "【DownloadEngine】取消下载: taskId=$taskId")
        
        // 立即取消Job
        val job = activeDownloads[taskId]
        if (job != null) {
            Log.i(TAG, "【DownloadEngine】找到活动下载Job: taskId=$taskId")
            job.cancel()  // 立即取消
            Log.i(TAG, "【DownloadEngine】Job已取消（不等待）: taskId=$taskId")
            
            // 在后台等待Job完全停止
            downloadScope.launch {
                try {
                    job.join()
                    Log.i(TAG, "【DownloadEngine】Job已完全停止: taskId=$taskId")
                } catch (e: Exception) {
                    Log.e(TAG, "【DownloadEngine】等待Job停止时异常: taskId=$taskId, error=${e.message}")
                }
            }
        } else {
            Log.i(TAG, "【DownloadEngine】未找到活动下载Job: taskId=$taskId")
        }
        
        // finally块会自动移除activeDownloads，但为了确保，这里也移除一次
        activeDownloads.remove(taskId)
        Log.i(TAG, "【DownloadEngine】下载已取消: taskId=$taskId, 剩余活动任务数=${activeDownloads.size}")
    }
    
    /**
     * 执行下载
     */
    private suspend fun performDownload(task: DownloadTask) {
        Log.i(TAG, "【DownloadEngine】performDownload开始: taskId=${task.id}, url=${task.url}")
        val file = File(task.filePath)
        Log.i(TAG, "【DownloadEngine】目标文件路径: ${file.absolutePath}")
        
        // 确保目录存在
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        
        // 如果是新任务，需要初始化
        if (task.totalSize == 0L) {
            initializeDownload(task, file)
            // 重新获取更新后的任务并继续下载
            val updatedTask = taskDao.getTaskById(task.id) ?: return
            return performDownload(updatedTask)
        }
        
        // 获取分片信息
        val parts = partDao.getPartsByTaskId(task.id)
        
        if (parts.isEmpty()) {
            // 创建分片
            createDownloadParts(task)
            // 重新获取更新后的任务和分片
            val updatedTask = taskDao.getTaskById(task.id) ?: return
            return performDownload(updatedTask)
        }
        
        // 开始多线程下载
        Log.i(TAG, "【DownloadEngine】准备调用downloadWithMultiThread: taskId=${task.id}, 分片数=${parts.size}")
        try {
            downloadWithMultiThread(task, parts, file)
            Log.i(TAG, "【DownloadEngine】downloadWithMultiThread完成: taskId=${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "【DownloadEngine】downloadWithMultiThread异常: taskId=${task.id}, error=${e.message}", e)
            throw e
        }
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
        Log.i(TAG, "【DownloadEngine】downloadWithMultiThread开始: taskId=${task.id}")
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime
        var lastDownloadedSize = task.downloadedSize
        
        // 为每个未完成的分片创建下载协程
        val incompleteParts = parts.filter { !it.isCompleted }
        Log.i(TAG, "【DownloadEngine】未完成分片数: ${incompleteParts.size}, 总分片数: ${parts.size}")
        
        Log.i(TAG, "【DownloadEngine】创建${incompleteParts.size}个下载协程")
        val jobs = incompleteParts.map { part ->
            async {
                Log.i(TAG, "【DownloadEngine】开始下载分片: partIndex=${part.partIndex}, range=${part.downloadedByte}-${part.endByte}")
                try {
                    downloadPart(task.id, part, task.url, file)
                    Log.i(TAG, "【DownloadEngine】分片下载完成: partIndex=${part.partIndex}")
                } catch (e: Exception) {
                    Log.e(TAG, "【DownloadEngine】分片下载失败: partIndex=${part.partIndex}, error=${e.message}", e)
                    throw e
                }
            }
        }
        Log.i(TAG, "【DownloadEngine】所有下载协程已创建: ${jobs.size}个")
        
        // 启动进度监控
        Log.i(TAG, "【DownloadEngine】启动进度监控协程")
        val progressJob = launch {
            Log.i(TAG, "【DownloadEngine】进度监控协程已启动")
            while (isActive) {
                delay(500) // 每500ms更新一次
                
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
        Log.i(TAG, "【DownloadEngine】等待所有分片下载完成...")
        try {
            jobs.awaitAll()
            Log.i(TAG, "【DownloadEngine】所有分片下载已完成")
            progressJob.cancel()
            
            // 标记任务完成
            taskDao.updateStatus(task.id, DownloadStatus.COMPLETED)
            taskDao.updateProgress(task.id, task.totalSize, 0)
            
            // 发送完成通知
            DownloadNotificationService.notifyComplete(
                context,
                task.id,
                task.fileName
            )
            
            Log.i(TAG, "下载完成: ${task.fileName}")
        } catch (e: Exception) {
            Log.e(TAG, "下载出错: ${task.fileName} - ${e.message}")
            progressJob.cancel()
            throw e
        }
    }
    
    /**
     * 下载单个分片
     */
    private suspend fun downloadPart(
        taskId: Long,
        part: DownloadPartInfo,
        url: String,
        file: File
    ) = withContext(Dispatchers.IO) {
        var response: okhttp3.Response? = null
        try {
            Log.i(TAG, "【downloadPart】分片${part.partIndex}开始: 获取任务信息")
            
            // 提前检查取消状态
            ensureActive()
            
            // 获取任务信息以使用自定义UA
            val task = taskDao.getTaskById(taskId)
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: 任务信息获取完成")
            
            ensureActive()
            
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=${part.downloadedByte}-${part.endByte}")
            
            // 添加User-Agent头
            task?.userAgent?.let { ua ->
                requestBuilder.addHeader("User-Agent", ua)
            }
            
            val request = requestBuilder.build()
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: 准备发送HTTP请求, Range=${part.downloadedByte}-${part.endByte}")
            
            ensureActive()
            
            response = okHttpClient.newCall(request).execute()
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: 收到HTTP响应, code=${response.code}")
            
            ensureActive()
            
            if (!response.isSuccessful && response.code != 206) {
                Log.e(TAG, "【downloadPart】分片${part.partIndex}: HTTP状态码错误: ${response.code}")
                throw Exception("分片下载失败: ${response.code}")
            }
            
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: HTTP状态码正常, 准备读取数据")
            val buffer = ByteArray(8192)
            var currentPosition = part.downloadedByte
            var totalBytesRead = 0L
            
            ensureActive()
            
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: 打开RandomAccessFile")
            RandomAccessFile(file, "rw").use { raf ->
                ensureActive()
                
                Log.i(TAG, "【downloadPart】分片${part.partIndex}: seek到位置: $currentPosition")
                raf.seek(currentPosition)
                
                Log.i(TAG, "【downloadPart】分片${part.partIndex}: 开始读取body stream")
                response.body?.byteStream()?.use { input ->
                    Log.i(TAG, "【downloadPart】分片${part.partIndex}: stream已打开, 开始循环读取")
                    var bytesRead: Int
                    var loopCount = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        ensureActive() // 检查协程是否被取消
                        
                        raf.write(buffer, 0, bytesRead)
                        currentPosition += bytesRead
                        totalBytesRead += bytesRead
                        loopCount++
                        
                        // 每读取100次输出一次日志
                        if (loopCount % 100 == 0) {
                            Log.i(TAG, "【downloadPart】分片${part.partIndex}: 已读取${totalBytesRead}字节, 当前位置=$currentPosition")
                            ensureActive()
                        }
                        
                        // 更新分片进度
                        partDao.updatePartProgress(part.id, currentPosition)
                    }
                    Log.i(TAG, "【downloadPart】分片${part.partIndex}: 读取完成, 总共读取${totalBytesRead}字节")
                }
            }
            
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: response已关闭")
            
            // 标记分片完成
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: 标记为已完成")
            partDao.markPartCompleted(part.id)
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: ✅ 完成")
        } catch (e: CancellationException) {
            // 协程被取消，静默处理，不打印错误日志
            Log.i(TAG, "【downloadPart】分片${part.partIndex}: 已取消")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "【downloadPart】❌ 分片${part.partIndex}下载失败: ${e.message}", e)
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

