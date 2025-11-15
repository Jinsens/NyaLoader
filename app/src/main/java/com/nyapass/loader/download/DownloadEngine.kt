package com.nyapass.loader.download

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.model.DownloadPartInfo
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.preferences.AppPreferences
import com.nyapass.loader.service.DownloadNotificationService
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import android.webkit.MimeTypeMap
import kotlin.math.min

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
    private val okHttpClient: OkHttpClient,
    private val preferences: AppPreferences? = null  // 可选，用于限速功能
) {
    private val TAG = "DownloadEngine"
    private val maxParallelPartsPerTask = 16
    private val maxPartRetryCount = 3
    private val baseRetryDelayMs = 300L

    // 限速相关
    private val speedLimitBytesPerSecond: Long
        get() = preferences?.downloadSpeedLimit?.value ?: 0L
    
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
        if (file.exists() && file.isDirectory) {
            file.deleteRecursively()
        }
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
        try {
            var contentLength = 0L
            var acceptRanges = false
            var headRequestFailed = false

            // 首先尝试 HEAD 请求
            val headRequestBuilder = Request.Builder()
                .url(task.url)
                .head()

            // 添加User-Agent头
            task.userAgent?.let { ua ->
                headRequestBuilder.addHeader("User-Agent", ua)
            }
            
            // 添加Cookie头
            task.cookie?.let { cookie ->
                if (cookie.isNotBlank()) {
                    headRequestBuilder.addHeader("Cookie", cookie)
                }
            }
            
            // 添加Referer头
            task.referer?.let { referer ->
                if (referer.isNotBlank()) {
                    headRequestBuilder.addHeader("Referer", referer)
                }
            }
            
            // 添加自定义请求头
            task.customHeaders?.let { headersJson ->
                if (headersJson.isNotBlank()) {
                    try {
                        val jsonObject = org.json.JSONObject(headersJson)
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObject.getString(key)
                            headRequestBuilder.addHeader(key, value)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析自定义请求头失败: ${e.message}")
                    }
                }
            }

            try {
                okHttpClient.newCall(headRequestBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        contentLength = response.header("Content-Length")?.toLongOrNull()
                            ?: response.body.contentLength()
                        acceptRanges = response.header("Accept-Ranges")?.contains("bytes", true) == true
                    } else {
                        // HEAD 请求失败（如 403），标记需要回退
                        Log.w(TAG, "HEAD 请求失败: ${response.code}，将尝试 GET 请求")
                        headRequestFailed = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "HEAD 请求异常: ${e.message}，将尝试 GET 请求")
                headRequestFailed = true
            }

            // 如果 HEAD 失败或未获取到文件大小，尝试使用 Range GET 请求
            if (headRequestFailed || contentLength <= 0L) {
                fetchContentLengthWithRange(task)?.let { fallback ->
                    contentLength = fallback.first
                    acceptRanges = acceptRanges || fallback.second
                }
            }

            // 如果还是获取不到，尝试普通 GET 请求（不带 Range）
            if (contentLength <= 0L) {
                fetchContentLengthWithGet(task)?.let { fallback ->
                    contentLength = fallback.first
                    // 普通 GET 请求无法确定是否支持 Range，保持之前的值
                }
            }

            if (contentLength <= 0L) {
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
        
        val parallelism = min(task.threadCount, maxParallelPartsPerTask).coerceAtLeast(1)
        val semaphore = Semaphore(parallelism)
        val jobs = incompleteParts.map { part ->
            async {
                semaphore.withPermit {
                    downloadPart(task.id, part, task.url, file)
                }
            }
        }
        
        // 启动进度监控（自适应更新频率）
        val progressJob = launch {
            var currentUpdateInterval = 500L  // 初始 500ms
            var consecutiveSlowUpdates = 0     // 连续慢速更新计数

            while (isActive) {
                delay(currentUpdateInterval)

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

                // 自适应更新频率：根据下载速度动态调整
                // 速度越快，更新频率可以越低（节省资源）
                // 速度越慢，更新频率保持较高（用户体验）
                val newInterval = when {
                    speed > 50 * 1024 * 1024 -> 1000L   // >50MB/s → 1秒更新
                    speed > 10 * 1024 * 1024 -> 750L    // >10MB/s → 750ms
                    speed > 5 * 1024 * 1024 -> 500L     // >5MB/s → 500ms
                    speed > 1 * 1024 * 1024 -> 400L     // >1MB/s → 400ms
                    speed > 100 * 1024 -> 300L          // >100KB/s → 300ms
                    else -> 200L                         // <100KB/s → 200ms（慢速时更频繁更新）
                }

                // 平滑过渡：避免频率突变
                currentUpdateInterval = if (newInterval > currentUpdateInterval) {
                    // 速度提升，逐渐降低更新频率
                    consecutiveSlowUpdates = 0
                    min(currentUpdateInterval + 50, newInterval)
                } else if (newInterval < currentUpdateInterval) {
                    // 速度下降，快速提高更新频率
                    consecutiveSlowUpdates++
                    if (consecutiveSlowUpdates >= 3) {
                        // 连续3次慢速，直接切换
                        newInterval
                    } else {
                        // 逐渐过渡
                        kotlin.math.max(currentUpdateInterval - 100, newInterval)
                    }
                } else {
                    currentUpdateInterval
                }
            }
        }
        
        // 等待所有分片下载完成
        try {
            jobs.awaitAll()
            progressJob.cancel()
            
            var finalDocument: DocumentFile? = null
            if (!task.destinationUri.isNullOrBlank()) {
                finalDocument = moveFileToSafDestination(task, file)
                taskDao.updateFileLocation(
                    taskId = task.id,
                    filePath = finalDocument.uri.toString(),
                    finalContentUri = finalDocument.uri.toString()
                )
                val documentName = finalDocument.name
                if (!documentName.isNullOrBlank() && documentName != task.fileName) {
                    taskDao.updateFileName(task.id, documentName)
                }
            }
            
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
    
    private suspend fun downloadPart(
        taskId: Long,
        part: DownloadPartInfo,
        url: String,
        file: File
    ) {
        var attempt = 0
        var currentDelay = baseRetryDelayMs
        var lastError: Exception? = null
        
        while (attempt < maxPartRetryCount) {
            try {
                performDownloadPart(taskId, part, url, file)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                attempt++
                if (attempt < maxPartRetryCount) {
                    Log.w(TAG, "分片${part.partIndex}重试($attempt/$maxPartRetryCount): ${e.message}")
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(2000L)
                }
            }
        }
        
        throw lastError ?: IllegalStateException("分片${part.partIndex}下载失败")
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
    private suspend fun performDownloadPart(
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
            
            // 添加Cookie头
            task?.cookie?.let { cookie ->
                if (cookie.isNotBlank()) {
                    requestBuilder.addHeader("Cookie", cookie)
                }
            }
            
            // 添加Referer头
            task?.referer?.let { referer ->
                if (referer.isNotBlank()) {
                    requestBuilder.addHeader("Referer", referer)
                }
            }
            
            // 添加自定义请求头 (JSON格式)
            task?.customHeaders?.let { headersJson ->
                if (headersJson.isNotBlank()) {
                    try {
                        val jsonObject = org.json.JSONObject(headersJson)
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObject.getString(key)
                            requestBuilder.addHeader(key, value)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析自定义请求头失败: ${e.message}")
                    }
                }
            }
            
            val request = requestBuilder.build()
            response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful && response.code != 206) {
                throw Exception("分片下载失败: ${response.code}")
            }
            

            val BUFFER_SIZE = 256 * 1024        // 256KB 缓冲区
            val DB_UPDATE_INTERVAL = 1024 * 1024 // 每 1MB 更新数据库
            val CANCEL_CHECK_INTERVAL = 100      // 每 100 次循环检查取消
            val THROTTLE_CHECK_INTERVAL = 50     // 每 50 次循环检查限速

            val buffer = ByteArray(BUFFER_SIZE)
            var currentPosition = part.downloadedByte
            var bytesReadSinceLastUpdate = 0L
            var loopCount = 0

            // 限速相关变量
            var throttleStartTime = System.currentTimeMillis()
            var bytesDownloadedSinceThrottleCheck = 0L

            // 使用 FileChannel 替代 RandomAccessFile 以获得更好的 I/O 性能
            // FileChannel 优势：
            // 1. 直接缓冲区写入，无需中间缓冲
            // 2. 更好的内存管理 (ByteBuffer)
            // 3. 原生 OS 级别优化
            java.nio.channels.FileChannel.open(
                file.toPath(),
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.READ
            ).use { channel ->
                channel.position(currentPosition)

                // 使用直接缓冲区（堆外内存）减少数据拷贝
                val byteBuffer = java.nio.ByteBuffer.allocateDirect(BUFFER_SIZE)

                response.body.byteStream().use { input ->
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // 写入数据到 FileChannel
                        byteBuffer.clear()
                        byteBuffer.put(buffer, 0, bytesRead)
                        byteBuffer.flip()
                        
                        while (byteBuffer.hasRemaining()) {
                            channel.write(byteBuffer)
                        }

                        currentPosition += bytesRead
                        bytesReadSinceLastUpdate += bytesRead
                        bytesDownloadedSinceThrottleCheck += bytesRead
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

                        // 限速检查（每50次循环，约12.5MB）
                        if (loopCount % THROTTLE_CHECK_INTERVAL == 0) {
                            val currentSpeedLimit = speedLimitBytesPerSecond
                            if (currentSpeedLimit > 0) {
                                val elapsedMs = System.currentTimeMillis() - throttleStartTime
                                if (elapsedMs > 0) {
                                    // 计算当前分片的允许速度（总限速 / 活动分片数）
                                    val activeParts = partDao.getPartsByTaskId(taskId).count { !it.isCompleted }
                                    val perPartLimit = if (activeParts > 0) {
                                        currentSpeedLimit / activeParts
                                    } else {
                                        currentSpeedLimit
                                    }

                                    // 计算应该花费的时间
                                    val expectedTimeMs = (bytesDownloadedSinceThrottleCheck.toDouble() / perPartLimit * 1000).toLong()
                                    val delayMs = expectedTimeMs - elapsedMs

                                    if (delayMs > 10) {
                                        delay(delayMs.coerceAtMost(1000)) // 最多延迟1秒
                                    }
                                }

                                // 重置限速计时器
                                throttleStartTime = System.currentTimeMillis()
                                bytesDownloadedSinceThrottleCheck = 0
                            }
                        }
                    }

                    // 强制刷新到磁盘
                    channel.force(false)
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
    
    private fun moveFileToSafDestination(task: DownloadTask, localFile: File): DocumentFile {
        val destinationUri = task.destinationUri ?: throw IllegalStateException("destinationUri 为空")
        val treeUri = Uri.parse(destinationUri)
        val parentDocument = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("无法访问所选目录")
        
        val mimeType = task.mimeType ?: guessMimeType(task.fileName)
        val targetDocument = createUniqueDocumentFile(parentDocument, mimeType, task.fileName)
            ?: throw IllegalStateException("无法在目标目录创建文件")
        
        context.contentResolver.openOutputStream(targetDocument.uri, "w")
            ?.use { output ->
                localFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("无法写入目标文件")
        
        if (localFile.exists()) {
            localFile.delete()
        }
        
        return targetDocument
    }
    
    private fun createUniqueDocumentFile(
        parent: DocumentFile,
        mimeType: String,
        baseName: String
    ): DocumentFile? {
        var name = baseName
        var counter = 1
        while (parent.findFile(name) != null) {
            val ext = baseName.substringAfterLast('.', "")
            val nameWithoutExt = if (ext.isNotEmpty()) baseName.substringBeforeLast('.') else baseName
            name = if (ext.isNotEmpty()) {
                "${nameWithoutExt}_$counter.$ext"
            } else {
                "${nameWithoutExt}_$counter"
            }
            counter++
        }
        return parent.createFile(mimeType, name)
    }
    
    private fun guessMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
    
    private fun fetchContentLengthWithRange(task: DownloadTask): Pair<Long, Boolean>? {
        val requestBuilder = Request.Builder()
            .url(task.url)
            .get()
            .addHeader("Range", "bytes=0-0")
        task.userAgent?.let { ua ->
            requestBuilder.addHeader("User-Agent", ua)
        }

        return try {
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                Log.d(TAG, "Range GET 请求响应码: ${response.code}")
                if (response.code == 206 || response.isSuccessful) {
                    // Content-Range 格式: bytes 0-0/12345678
                    // 对于 206 响应，Content-Length 是分片大小，需要从 Content-Range 获取总大小
                    val contentRange = response.header("Content-Range")
                    Log.d(TAG, "Content-Range: $contentRange")

                    val totalFromRange = contentRange?.substringAfterLast("/")?.toLongOrNull()

                    // 优先使用 Content-Range 中的总大小，只有在没有 Content-Range 时才使用 Content-Length
                    val length = if (response.code == 206 && totalFromRange != null && totalFromRange > 0) {
                        totalFromRange
                    } else {
                        // 200 响应（服务器忽略了 Range 头），使用 Content-Length
                        response.header("Content-Length")?.toLongOrNull()
                            ?: response.body.contentLength()
                    }

                    if (length > 0L) {
                        val supportsRange = response.code == 206 ||
                            response.header("Accept-Ranges")?.contains("bytes", true) == true
                        Log.d(TAG, "Range GET 成功获取文件大小: $length, supportsRange: $supportsRange")
                        return length to supportsRange
                    } else {
                        Log.w(TAG, "Range GET 无法从响应中获取文件大小，将尝试普通 GET 请求")
                    }
                } else {
                    Log.w(TAG, "Range GET 请求失败: ${response.code}，将尝试普通 GET 请求")
                }
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Range 检测文件大小失败: ${e.message}")
            null
        }
    }

    /**
     * 使用普通 GET 请求获取文件大小（不带 Range 头）
     * 用于某些不支持 HEAD 和 Range 请求的服务器（如 AWS S3 预签名 URL）
     * 注意：此方法不下载实际内容，只获取响应头
     */
    private fun fetchContentLengthWithGet(task: DownloadTask): Pair<Long, Boolean>? {
        val requestBuilder = Request.Builder()
            .url(task.url)
            .get()
        task.userAgent?.let { ua ->
            requestBuilder.addHeader("User-Agent", ua)
        }

        return try {
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val length = response.header("Content-Length")?.toLongOrNull()
                        ?: response.body.contentLength()

                    if (length > 0L) {
                        // 普通 GET 请求无法确定是否支持 Range，默认设为 false
                        // 后续下载会使用单线程模式
                        val supportsRange = response.header("Accept-Ranges")?.contains("bytes", true) == true
                        Log.d(TAG, "GET 请求成功获取文件大小: $length, supportsRange: $supportsRange")
                        return length to supportsRange
                    }
                } else {
                    Log.w(TAG, "GET 请求失败: ${response.code}")
                }
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET 检测文件大小失败: ${e.message}")
            null
        }
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

