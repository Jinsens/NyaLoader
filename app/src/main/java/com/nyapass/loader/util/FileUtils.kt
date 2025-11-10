package com.nyapass.loader.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * 文件工具类
 */
object FileUtils {
    
    /**
     * 获取下载目录
     */
    fun getDownloadDirectory(context: Context): File {
        val downloadDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "NyaLoader"
        )
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return downloadDir
    }
    
    /**
     * 获取缓存目录
     */
    fun getCacheDirectory(context: Context): File {
        val cacheDir = File(context.cacheDir, "downloads")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }
    
    /**
     * 检查文件名是否合法
     */
    fun isValidFileName(fileName: String): Boolean {
        if (fileName.isBlank()) return false
        val invalidChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return !fileName.any { it in invalidChars }
    }
    
    /**
     * 清理文件名
     */
    fun sanitizeFileName(fileName: String): String {
        val invalidChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        var cleaned = fileName
        invalidChars.forEach { cleaned = cleaned.replace(it, '_') }
        return cleaned.trim()
    }
    
    /**
     * 生成唯一文件名
     */
    fun generateUniqueFileName(directory: File, baseName: String): String {
        val extension = baseName.substringAfterLast('.', "")
        val nameWithoutExt = if (extension.isNotEmpty()) {
            baseName.substringBeforeLast('.')
        } else {
            baseName
        }
        
        var counter = 1
        var fileName = baseName
        var file = File(directory, fileName)
        
        while (file.exists()) {
            fileName = if (extension.isNotEmpty()) {
                "${nameWithoutExt}_$counter.$extension"
            } else {
                "${nameWithoutExt}_$counter"
            }
            file = File(directory, fileName)
            counter++
        }
        
        return fileName
    }
    
    /**
     * 删除目录及其内容
     */
    fun deleteDirectory(directory: File): Boolean {
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }
        return directory.delete()
    }
    
    /**
     * 获取目录大小
     */
    fun getDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }
}
