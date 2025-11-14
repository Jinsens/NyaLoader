package com.nyapass.loader.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.download.DownloadEngine
import com.nyapass.loader.download.DownloadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * 下载仓库层
 * 负责协调数据层和下载引擎
 */
class DownloadRepository(
    private val context: Context,
    private val taskDao: DownloadTaskDao,
    private val partDao: DownloadPartDao,
    private val downloadEngine: DownloadEngine,
    private val okHttpClient: OkHttpClient
) {
    
    private val TAG = "DownloadRepository"
    
    /**
     * 获取所有下载任务
     */
    fun getAllTasks(): Flow<List<DownloadTask>> {
        return taskDao.getAllTasks()
    }
    
    /**
     * 根据ID获取任务
     */
    suspend fun getTaskById(taskId: Long): DownloadTask? {
        return taskDao.getTaskById(taskId)
    }
    
    /**
     * 根据ID获取任务Flow
     */
    fun getTaskByIdFlow(taskId: Long): Flow<DownloadTask?> {
        return taskDao.getTaskByIdFlow(taskId)
    }
    
    /**
     * 根据状态获取任务
     */
    fun getTasksByStatus(status: DownloadStatus): Flow<List<DownloadTask>> {
        return taskDao.getTasksByStatus(status)
    }
    
    /**
     * 获取下载进度
     */
    fun getDownloadProgress(): StateFlow<Map<Long, DownloadProgress>> {
        return downloadEngine.downloadProgress
    }
    
    /**
     * 创建新的下载任务
     */
    suspend fun createDownloadTask(
        url: String,
        fileName: String? = null,
        threadCount: Int = 4,
        saveToPublicDir: Boolean = true,
        customPath: String? = null,
        userAgent: String? = null
    ): Long {
        // 优先使用服务器提供的文件名（Content-Disposition / 重定向后的URL），否则再回退到URL路径
        val finalFileName = fileName ?: resolveFileNameFromServer(url)
        
        // 构建文件路径
        val filePath = if (customPath != null) {
            File(customPath, finalFileName).absolutePath
        } else if (saveToPublicDir) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val loaderDir = File(downloadDir, "NyaLoader")
            loaderDir.mkdirs()
            File(loaderDir, finalFileName).absolutePath
        } else {
            val downloadDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "NyaLoader"
            )
            downloadDir.mkdirs()
            File(downloadDir, finalFileName).absolutePath
        }
        
        // 创建任务
        val task = DownloadTask(
            url = url,
            fileName = finalFileName,
            filePath = filePath,
            threadCount = threadCount,
            status = DownloadStatus.PENDING,
            saveToPublicDir = saveToPublicDir,
            userAgent = userAgent
        )
        
        return taskDao.insertTask(task)
    }

    /**
     * 从服务器获取更准确的文件名
     * 1. 先尝试读取 Content-Disposition 的 filename / filename*
     * 2. 再尝试使用最终 URL 的路径名
     * 3. 最后回退到原始 URL 的最后一段
     */
    private fun resolveFileNameFromServer(url: String): String {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    // 1. Content-Disposition: attachment; filename="xxx.ext"
                    val disposition = response.header("Content-Disposition") ?: response.header("content-disposition")
                    val cdFileName = disposition?.let { parseFileNameFromContentDisposition(it) }
                    if (!cdFileName.isNullOrBlank()) {
                        return@use cdFileName
                    }

                    // 2. 最终 URL 的路径
                    val finalUrl = response.request.url.toString()
                    val pathName = extractFileNameFromUrl(finalUrl)
                    if (pathName.isNotBlank()) {
                        return@use pathName
                    }
                }

                // 3. 回退到原始 URL
                extractFileNameFromUrl(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "从服务器解析文件名失败: ${e.message}")
            extractFileNameFromUrl(url)
        }
    }

    /**
     * 从 Content-Disposition 中解析文件名
     */
    private fun parseFileNameFromContentDisposition(header: String): String? {
        // 处理 filename*（RFC5987，例如：filename*=UTF-8''%E4%B8%AD%E6%96%87.txt）
        val filenameStar = Regex("filename\\*=(?:UTF-8'')?([^;]+)", RegexOption.IGNORE_CASE)
            .find(header)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim(' ', '"')

        if (!filenameStar.isNullOrBlank()) {
            return try {
                URLDecoder.decode(filenameStar, StandardCharsets.UTF_8.name())
            } catch (_: Exception) {
                filenameStar
            }
        }

        // 处理普通 filename=xxx.ext
        val filename = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
            .find(header)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

        return filename
    }

    /**
     * 从 URL 中提取文件名
     * 优先从常见的 query 参数中获取（如 fin / filename / name）
     * 再退回到路径最后一段
     */
    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)

            // 1. 先尝试从 query 参数中取更加语义化的文件名
            val queryKeys = listOf("fin", "filename", "file", "name")
            for (key in queryKeys) {
                val value = uri.getQueryParameter(key)
                if (!value.isNullOrBlank()) {
                    val decoded = try {
                        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                    } catch (_: Exception) {
                        value
                    }
                    if (decoded.isNotBlank()) return decoded
                }
            }

            // 2. 再尝试用路径最后一段
            val raw = uri.path?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
                ?: url.substringAfterLast("/").substringBefore("?")

            if (raw.isBlank()) return "download.bin"

            try {
                URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
            } catch (_: Exception) {
                raw
            }
        } catch (e: Exception) {
            Log.e(TAG, "从URL提取文件名失败: ${e.message}")
            "download.bin"
        }
    }
    
    /**
     * 开始下载
     */
    suspend fun startDownload(taskId: Long) {
        try {
            Log.i(TAG, "Repository开始下载: taskId=$taskId")
            downloadEngine.startDownload(taskId)
            Log.i(TAG, "Repository下载引擎已启动: taskId=$taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Repository启动下载失败: taskId=$taskId, error=${e.message}", e)
            throw e
        }
    }
    
    /**
     * 暂停下载
     */
    suspend fun pauseDownload(taskId: Long) {
        Log.i(TAG, "Repository暂停下载: taskId=$taskId")
        downloadEngine.pauseDownload(taskId)
        Log.i(TAG, "Repository暂停下载完成: taskId=$taskId")
    }
    
    /**
     * 恢复下载
     */
    suspend fun resumeDownload(taskId: Long) {
        Log.i(TAG, "Repository恢复下载: taskId=$taskId")
        downloadEngine.startDownload(taskId)
        Log.i(TAG, "Repository恢复下载完成: taskId=$taskId")
    }
    
    /**
     * 取消下载
     */
    suspend fun cancelDownload(taskId: Long) {
        downloadEngine.cancelDownload(taskId)
    }
    
    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: Long) {
        Log.i(TAG, "Repository删除任务开始: taskId=$taskId")
        val task = taskDao.getTaskById(taskId)
        
        // 先取消下载（如果正在运行）
        try {
            Log.i(TAG, "Repository取消下载: taskId=$taskId")
            downloadEngine.cancelDownload(taskId)
            Log.i(TAG, "Repository下载已取消: taskId=$taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Repository取消下载失败: taskId=$taskId, error=${e.message}")
            // 即使取消失败，仍继续删除流程
        }
        
        // 删除文件
        task?.let {
            val file = File(it.filePath)
            if (file.exists()) {
                Log.i(TAG, "Repository删除文件: ${file.absolutePath}")
                file.delete()
            }
        }
        
        // 删除数据库记录
        Log.i(TAG, "Repository删除数据库记录: taskId=$taskId")
        partDao.deletePartsByTaskId(taskId)
        taskDao.deleteTaskById(taskId)
        Log.i(TAG, "Repository删除任务完成: taskId=$taskId")
    }
    
    /**
     * 清除已完成的任务
     */
    suspend fun clearCompletedTasks() {
        taskDao.deleteTasksByStatus(DownloadStatus.COMPLETED)
    }
    
    /**
     * 清除失败的任务
     */
    suspend fun clearFailedTasks() {
        taskDao.deleteTasksByStatus(DownloadStatus.FAILED)
    }
    
    /**
     * 重试失败的任务或重新下载已完成的任务
     */
    suspend fun retryFailedTask(taskId: Long) {
        Log.i(TAG, "重试/重新下载任务: taskId=$taskId")
        val task = taskDao.getTaskById(taskId)
        
        if (task == null) {
            Log.e(TAG, "任务不存在: taskId=$taskId")
            return
        }
        
        // 如果是已完成的任务,需要重置下载数据
        if (task.status == DownloadStatus.COMPLETED) {
            Log.i(TAG, "重新下载已完成的任务: taskId=$taskId")
            
            // 先取消可能存在的下载任务
            try {
                downloadEngine.cancelDownload(taskId)
            } catch (e: Exception) {
                // 忽略取消失败的错误
            }
            
            // 删除已下载的文件
            val file = File(task.filePath)
            if (file.exists()) {
                Log.i(TAG, "删除已存在的文件: ${file.absolutePath}")
                file.delete()
            }
            
            // 删除所有分片记录
            Log.i(TAG, "删除分片记录: taskId=$taskId")
            partDao.deletePartsByTaskId(taskId)
            
            // 重置任务的下载数据
            val resetTask = task.copy(
                downloadedSize = 0L,
                totalSize = 0L,
                status = DownloadStatus.PENDING,
                speed = 0L,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            Log.i(TAG, "重置任务数据: taskId=$taskId")
            taskDao.updateTask(resetTask)
        } else {
            // 对于失败或其他状态的任务,只需清除错误消息并更新状态
            Log.i(TAG, "重试失败的任务: taskId=$taskId, status=${task.status}")
            taskDao.clearErrorMessage(taskId)
            taskDao.updateStatus(taskId, DownloadStatus.PENDING)
        }
        
        // 启动下载
        Log.i(TAG, "启动下载: taskId=$taskId")
        downloadEngine.startDownload(taskId)
    }
    
    /**
     * 更新任务
     */
    suspend fun updateTask(task: DownloadTask) {
        taskDao.updateTask(task)
    }
    
    /**
     * 打开已下载的文件
     */
    fun openFile(context: android.content.Context, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return false
            }
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val mimeType = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension) ?: "*/*"
            
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "打开文件失败: ${e.message}")
            false
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        downloadEngine.cleanup()
    }
}
