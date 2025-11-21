package com.nyapass.loader.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.nyapass.loader.R
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadTagDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.model.TagStatistics
import com.nyapass.loader.download.DownloadEngine
import com.nyapass.loader.download.DownloadProgress
import com.nyapass.loader.util.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * 下载仓库层
 * 负责协调数据层和下载引擎
 */
class DownloadRepository(
    private val context: Context,
    private val taskDao: DownloadTaskDao,
    private val partDao: DownloadPartDao,
    private val tagDao: DownloadTagDao,
    private val downloadEngine: DownloadEngine,
    private val okHttpClient: OkHttpClient
) {
    
    private val TAG = "DownloadRepository"
    private val remoteFileInfoCache = ConcurrentHashMap<String, RemoteFileInfo>()
    
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
     * 获取全部标签
     */
    fun getAllTags(): Flow<List<DownloadTag>> {
        return tagDao.getAllTags()
    }

    /**
     * 获取任务标签映射
     */
    fun getTaskTags(): Flow<Map<Long, List<DownloadTag>>> {
        return tagDao.getTaskTags().map { relations ->
            relations.groupBy { it.taskId }
                .mapValues { entry -> entry.value.map { it.tag } }
        }
    }

    /**
     * 获取标签统计
     */
    fun getTagStatistics(): Flow<List<TagStatistics>> {
        return tagDao.getTagStatistics()
    }

    /**
     * 创建新标签
     */
    suspend fun createTag(name: String, color: Long): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { context.getString(R.string.error_tag_name_empty) }
        val nextOrder = tagDao.getMaxSortOrder() + 1
        return tagDao.insertTag(
            DownloadTag(
                name = trimmed,
                color = color,
                sortOrder = nextOrder
            )
        )
    }

    suspend fun updateTag(tagId: Long, name: String, color: Long) {
        val existing = tagDao.getTagById(tagId) ?: return
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { context.getString(R.string.error_tag_name_empty) }
        tagDao.updateTag(
            existing.copy(
                name = trimmed,
                color = color
            )
        )
    }

    /**
     * 删除标签
     */
    suspend fun deleteTag(tagId: Long) {
        tagDao.deleteTag(tagId)
    }

    suspend fun swapTagOrder(firstId: Long, secondId: Long) {
        if (firstId == secondId) return
        val first = tagDao.getTagById(firstId)
        val second = tagDao.getTagById(secondId)
        if (first != null && second != null) {
            tagDao.swapTagOrder(firstId, first.sortOrder, secondId, second.sortOrder)
        }
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
        userAgent: String? = null,
        tagIds: List<Long> = emptyList()
    ): Long {
        val remoteFileInfo = resolveFileInfoFromServer(url, userAgent)
        val safeFileName = buildSafeFileName(fileName ?: remoteFileInfo.fileName, remoteFileInfo.mimeType)
        val useSaf = isSafPath(customPath)
        val workingDirectory = if (useSaf) {
            FileUtils.getCacheDirectory(context)
        } else {
            determineTargetDirectory(customPath, saveToPublicDir)
        }
        val uniqueFileName = FileUtils.generateUniqueFileName(workingDirectory, safeFileName)
        val filePath = File(workingDirectory, uniqueFileName).absolutePath
        
        val task = DownloadTask(
            url = url,
            fileName = uniqueFileName,
            filePath = filePath,
            mimeType = remoteFileInfo.mimeType,
            destinationUri = if (useSaf) customPath else null,
            threadCount = threadCount,
            status = DownloadStatus.PENDING,
            saveToPublicDir = saveToPublicDir,
            userAgent = userAgent
        )
        
        val taskId = taskDao.insertTask(task)
        tagDao.replaceTagsForTask(taskId, tagIds.distinct())
        return taskId
    }
    /**
     * 获取文件名和 MIME 信息，带有简单缓存与 Range 兜底
     */
    private fun resolveFileInfoFromServer(url: String, userAgent: String?): RemoteFileInfo {
        val cacheKey = buildCacheKey(url, userAgent)
        remoteFileInfoCache[cacheKey]?.let { return it }
        
        val fallbackName = extractFileNameFromUrl(url).ifBlank { "download.bin" }
        val info = fetchFileInfoViaHead(url, userAgent, fallbackName)
            ?: fetchFileInfoViaRange(url, userAgent, fallbackName)
            ?: RemoteFileInfo(
                fileName = fallbackName,
                mimeType = guessMimeTypeFromName(fallbackName)
            )
        
        remoteFileInfoCache[cacheKey] = info
        return info
    }
    
    private fun buildCacheKey(url: String, userAgent: String?): String {
        return "$url|${userAgent ?: ""}"
    }
    
    private fun fetchFileInfoViaHead(
        url: String,
        userAgent: String?,
        fallbackName: String
    ): RemoteFileInfo? {
        val requestBuilder = Request.Builder().url(url).head()
        userAgent?.let { requestBuilder.header("User-Agent", it) }
        
        return try {
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return null
                buildRemoteFileInfo(response, fallbackName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "HEAD 请求文件信息失败: ${e.message}")
            null
        }
    }
    
    private fun fetchFileInfoViaRange(
        url: String,
        userAgent: String?,
        fallbackName: String
    ): RemoteFileInfo? {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .addHeader("Range", "bytes=0-0")
        userAgent?.let { requestBuilder.header("User-Agent", it) }
        
        return try {
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!(response.code == 206 || response.isSuccessful)) return null
                buildRemoteFileInfo(response, fallbackName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Range 请求文件信息失败: ${e.message}")
            null
        }
    }
    
    private fun buildRemoteFileInfo(response: okhttp3.Response, fallbackName: String): RemoteFileInfo {
        val disposition = response.header("Content-Disposition") ?: response.header("content-disposition")
        val cdFileName = disposition?.let { parseFileNameFromContentDisposition(it) }
        val redirectedUrl = response.request.url.toString()
        
        val resolvedName = when {
            !cdFileName.isNullOrBlank() -> cdFileName
            else -> extractFileNameFromUrl(redirectedUrl)
        }.ifBlank { fallbackName }
        
        val mimeType = response.header("Content-Type")
            ?.substringBefore(";")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: guessMimeTypeFromName(resolvedName)
        
        return RemoteFileInfo(
            fileName = resolvedName,
            mimeType = mimeType
        )
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
     * 构建安全的文件名
     */
    private fun buildSafeFileName(preferredName: String?, mimeType: String?): String {
        var candidate = preferredName?.trim().takeUnless { it.isNullOrEmpty() }
            ?: "download_${System.currentTimeMillis()}"
        
        candidate = FileUtils.sanitizeFileName(candidate).ifBlank {
            "download_${System.currentTimeMillis()}"
        }
        
        candidate = candidate.replace(Regex("[/\\\\]+"), "_")
        
        if (!candidate.contains('.') || candidate.endsWith(".")) {
            val extension = guessExtensionFromMimeType(mimeType)
            candidate = if (!extension.isNullOrBlank()) {
                candidate.trimEnd('.') + "." + extension
            } else {
                if (candidate.endsWith(".bin")) candidate else "$candidate.bin"
            }
        }
        
        val maxLength = 128
        if (candidate.length > maxLength) {
            val ext = candidate.substringAfterLast('.', "")
            val nameWithoutExt = if (ext.isNotEmpty()) candidate.substringBeforeLast('.') else candidate
            val trimmed = nameWithoutExt.take(maxLength - if (ext.isNotEmpty()) ext.length + 1 else 0)
                .ifBlank { "download_${System.currentTimeMillis()}" }
            candidate = if (ext.isNotEmpty()) "$trimmed.$ext" else trimmed
        }
        
        return candidate
    }
    
    /**
     * 确认存储目录
     */
    private fun determineTargetDirectory(customPath: String?, saveToPublicDir: Boolean): File {
        customPath?.trim()?.takeIf { it.isNotEmpty() }?.let { rawPath ->
            if (!isSafPath(rawPath)) {
                val customDir = File(rawPath)
                if (!customDir.exists()) {
                    runCatching { customDir.mkdirs() }.onFailure {
                        Log.w(TAG, "无法创建自定义目录: $rawPath - ${it.message}")
                    }
                }
                
                if (customDir.isDirectory && customDir.canWrite()) {
                    return customDir
                } else {
                    Log.w(TAG, "自定义目录无法写入, 回退至默认路径: $rawPath")
                }
            } else {
                Log.w(TAG, "content:// 路径无法通过 File API 直接访问, 回退至默认目录: $rawPath")
            }
        }
        
        val defaultDir = if (saveToPublicDir) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        } ?: context.filesDir
        
        if (!defaultDir.exists()) {
            defaultDir.mkdirs()
        }
        
        return defaultDir
    }
    
    private fun isSafPath(path: String?): Boolean {
        return path?.startsWith("content://") == true
    }
    
    private fun guessMimeTypeFromName(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    
    private fun guessExtensionFromMimeType(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        val normalized = mimeType.substringBefore(";").trim().lowercase()
        if (normalized.isEmpty()) return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(normalized)
    }
    
    private data class RemoteFileInfo(
        val fileName: String,
        val mimeType: String?
    )
    
    private fun deleteTaskFile(task: DownloadTask) {
        val safUri = task.finalContentUri ?: task.filePath.takeIf { isSafPath(it) }
        if (safUri != null && safUri.startsWith("content://")) {
            DocumentFile.fromSingleUri(context, Uri.parse(safUri))?.let { document ->
                if (document.exists()) {
                    document.delete()
                }
            }
        } else {
            val file = File(task.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }
    
    private fun resolveWorkingFilePath(task: DownloadTask): String {
        val shouldUseCache = task.finalContentUri != null || isSafPath(task.filePath)
        val directory = if (shouldUseCache) {
            FileUtils.getCacheDirectory(context)
        } else {
            File(task.filePath).parentFile ?: FileUtils.getDownloadDirectory(context)
        }
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val uniqueName = FileUtils.generateUniqueFileName(directory, task.fileName)
        return File(directory, uniqueName).absolutePath
    }
    
    /**
     * 开始下载
     */
    suspend fun startDownload(taskId: Long) {
        try {
            downloadEngine.startDownload(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "启动下载失败: $taskId - ${e.message}")
            throw e
        }
    }
    
    /**
     * 暂停下载
     */
    suspend fun pauseDownload(taskId: Long) {
        downloadEngine.pauseDownload(taskId)
    }
    
    /**
     * 恢复下载
     */
    suspend fun resumeDownload(taskId: Long) {
        downloadEngine.startDownload(taskId)
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
        val task = taskDao.getTaskById(taskId)
        
        // 先取消下载（如果正在运行）
        try {
            downloadEngine.cancelDownload(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "取消下载失败: ${e.message}")
        }
        
        // 删除文件
        task?.let { deleteTaskFile(it) }
        
        // 删除数据库记录
        partDao.deletePartsByTaskId(taskId)
        tagDao.deleteTagsForTask(taskId)
        taskDao.deleteTaskById(taskId)
    }
    
    /**
     * 清除已完成的任务
     */
    suspend fun clearCompletedTasks() {
        val taskIds = taskDao.getTaskIdsByStatus(DownloadStatus.COMPLETED)
        if (taskIds.isNotEmpty()) {
            tagDao.deleteTagsForTasks(taskIds)
        }
        taskDao.deleteTasksByStatus(DownloadStatus.COMPLETED)
    }
    
    /**
     * 清除失败的任务
     */
    suspend fun clearFailedTasks() {
        val taskIds = taskDao.getTaskIdsByStatus(DownloadStatus.FAILED)
        if (taskIds.isNotEmpty()) {
            tagDao.deleteTagsForTasks(taskIds)
        }
        taskDao.deleteTasksByStatus(DownloadStatus.FAILED)
    }
    
    /**
     * 重试失败的任务或重新下载已完成的任务
     */
    suspend fun retryFailedTask(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        
        // 如果是已完成的任务,需要重置下载数据
        if (task.status == DownloadStatus.COMPLETED) {
            try {
                downloadEngine.cancelDownload(taskId)
            } catch (_: Exception) {
            }
            
            deleteTaskFile(task)
            partDao.deletePartsByTaskId(taskId)
            
            val resetTask = task.copy(
                downloadedSize = 0L,
                totalSize = 0L,
                status = DownloadStatus.PENDING,
                speed = 0L,
                errorMessage = null,
                finalContentUri = null,
                filePath = resolveWorkingFilePath(task),
                updatedAt = System.currentTimeMillis()
            )
            taskDao.updateTask(resetTask)
        } else {
            // 对于失败或其他状态的任务,只需清除错误消息并更新状态
            taskDao.clearErrorMessage(taskId)
            taskDao.updateStatus(taskId, DownloadStatus.PENDING)
        }
        
        // 启动下载
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
     * 
     * @param context 上下文
     * @param filePath 文件路径
     * @param chooserTitle 选择器标题（可选）
     */
    fun openFile(context: android.content.Context, task: DownloadTask, chooserTitle: String? = null): Boolean {
        val safUri = task.finalContentUri ?: task.filePath.takeIf { isSafPath(it) }
        return if (safUri != null && safUri.startsWith("content://")) {
            openSafFile(context, Uri.parse(safUri), chooserTitle)
        } else {
            openLocalFile(context, task.filePath, chooserTitle)
        }
    }
    
    private fun openLocalFile(context: android.content.Context, filePath: String, chooserTitle: String?): Boolean {
        return try {
            val file = File(filePath)
            Log.d(TAG, "尝试打开文件: $filePath")
            Log.d(TAG, "文件存在: ${file.exists()}, 可读: ${file.canRead()}, 大小: ${file.length()} bytes")
            
            if (!file.exists()) {
                Log.e(TAG, "文件不存在: $filePath")
                return false
            }
            
            if (!file.canRead()) {
                Log.e(TAG, "文件无法读取: $filePath")
                return false
            }
            
            // 尝试使用 FileProvider 获取 URI
            val uri = try {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "FileProvider 获取 URI 失败: ${e.message}", e)
                Log.e(TAG, "文件路径可能不在 FileProvider 配置的路径中")
                throw e
            }
            
            // 获取 MIME 类型
            val extension = file.extension.lowercase()
            val mimeType = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension) ?: "*/*"
            
            Log.d(TAG, "文件信息:")
            Log.d(TAG, "  - 路径: $filePath")
            Log.d(TAG, "  - URI: $uri")
            Log.d(TAG, "  - 扩展名: $extension")
            Log.d(TAG, "  - MIME类型: $mimeType")
            
            // 创建打开文件的 Intent
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // 检查是否有应用可以处理这个 Intent
            val packageManager = context.packageManager
            val activities = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            }
            
            Log.d(TAG, "可以打开此文件的应用数量: ${activities.size}")
            activities.forEach { resolveInfo ->
                Log.d(TAG, "  - ${resolveInfo.activityInfo.packageName}")
            }
            
            if (activities.isEmpty()) {
                Log.e(TAG, "没有应用可以打开此文件类型: $mimeType")
                // 尝试使用通用 MIME 类型
                if (mimeType != "*/*") {
                    Log.d(TAG, "尝试使用通用 MIME 类型 */*")
                    intent.setDataAndType(uri, "*/*")
                    
                    val genericActivities = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        packageManager.queryIntentActivities(
                            intent,
                            android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong())
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                    }
                    
                    if (genericActivities.isEmpty()) {
                        return false
                    }
                } else {
                    return false
                }
            }
            
            // 使用选择器让用户选择打开方式
            val chooser = android.content.Intent.createChooser(
                intent, 
                chooserTitle ?: context.getString(com.nyapass.loader.R.string.choose_app_to_open)
            )
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 对于 APK 文件，检查是否有安装权限
            if (mimeType == "application/vnd.android.package-archive") {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                Log.d(TAG, "APK 文件 - 是否有安装权限: $canInstall")
                
                if (!canInstall) {
                    Log.w(TAG, "没有安装未知应用权限，需要用户授权")
                    // 仍然尝试打开，让系统引导用户授权
                }
            }
            
            Log.d(TAG, "启动应用选择器")
            context.startActivity(chooser)
            Log.d(TAG, "文件打开成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "打开文件失败: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }
    
    private fun openSafFile(context: android.content.Context, uri: Uri, chooserTitle: String?): Boolean {
        return try {
            val document = DocumentFile.fromSingleUri(context, uri)
            if (document == null || !document.exists()) {
                Log.e(TAG, "SAF 文件不存在: $uri")
                return false
            }
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = android.content.Intent.createChooser(
                intent,
                chooserTitle ?: context.getString(com.nyapass.loader.R.string.choose_app_to_open)
            )
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "通过 SAF 打开文件失败: ${e.message}", e)
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

