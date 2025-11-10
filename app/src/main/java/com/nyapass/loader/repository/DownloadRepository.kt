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

/**
 * 下载仓库层
 * 负责协调数据层和下载引擎
 */
class DownloadRepository(
    private val context: Context,
    private val taskDao: DownloadTaskDao,
    private val partDao: DownloadPartDao,
    private val downloadEngine: DownloadEngine
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
        // 从URL提取文件名
        val finalFileName = fileName ?: url.substringAfterLast("/").substringBefore("?")
        
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
     * 重试失败的任务
     */
    suspend fun retryFailedTask(taskId: Long) {
        taskDao.clearErrorMessage(taskId)
        taskDao.updateStatus(taskId, DownloadStatus.PENDING)
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
