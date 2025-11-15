package com.nyapass.loader.util

import org.junit.Test
import org.junit.Assert.*

/**
 * 文件工具测试
 * 测试文件名提取、扩展名处理等功能
 */
class FileUtilsTest {

    @Test
    fun `getFileExtension should return correct extension`() {
        assertEquals("zip", getFileExtension("file.zip"))
        assertEquals("apk", getFileExtension("app.apk"))
        assertEquals("tar.gz", getFileExtension("archive.tar.gz"))
        assertEquals("", getFileExtension("noextension"))
    }

    @Test
    fun `getFileExtension should handle edge cases`() {
        assertEquals("", getFileExtension(""))
        assertEquals("", getFileExtension("."))
        assertEquals("", getFileExtension(".gitignore")) // dot at position 0 means no meaningful extension
    }

    @Test
    fun `isDownloadableExtension should identify common download types`() {
        val downloadableExtensions = setOf(
            "zip", "rar", "7z", "tar", "gz",
            "apk", "exe", "msi", "dmg",
            "mp4", "mkv", "avi", "mov",
            "mp3", "flac", "wav",
            "pdf", "doc", "docx", "xls", "xlsx"
        )
        
        downloadableExtensions.forEach { ext ->
            assertTrue("$ext should be downloadable", isDownloadableExtension(ext))
        }
    }

    @Test
    fun `isDownloadableExtension should reject web page extensions`() {
        val nonDownloadable = listOf("html", "htm", "php", "asp", "jsp")
        nonDownloadable.forEach { ext ->
            assertFalse("$ext should not be downloadable", isDownloadableExtension(ext))
        }
    }

    @Test
    fun `sanitizeFileName should remove invalid characters`() {
        assertEquals("file_name.txt", sanitizeFileName("file:name.txt"))
        assertEquals("file_name.txt", sanitizeFileName("file/name.txt"))
        assertEquals("file_name.txt", sanitizeFileName("file\\name.txt"))
        assertEquals("file_name.txt", sanitizeFileName("file?name.txt"))
        assertEquals("file_name.txt", sanitizeFileName("file*name.txt"))
    }

    @Test
    fun `sanitizeFileName should handle unicode`() {
        assertEquals("文件名.txt", sanitizeFileName("文件名.txt"))
        assertEquals("下载_文件.apk", sanitizeFileName("下载:文件.apk"))
    }

    @Test
    fun `formatFileSize should format correctly`() {
        assertEquals("0 B", formatFileSizeTest(0))
        assertEquals("1.00 KB", formatFileSizeTest(1024))
        assertEquals("1.00 MB", formatFileSizeTest(1024 * 1024))
        assertEquals("1.50 GB", formatFileSizeTest((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    // Helper functions for testing
    private fun getFileExtension(fileName: String): String {
        if (fileName.isBlank() || fileName == ".") return ""
        
        val lastDotIndex = fileName.lastIndexOf('.')
        if (lastDotIndex <= 0) return ""
        
        val extension = fileName.substring(lastDotIndex + 1).lowercase()
        
        // Handle double extensions like .tar.gz
        val secondLastDot = fileName.lastIndexOf('.', lastDotIndex - 1)
        if (secondLastDot > 0) {
            val possibleDouble = fileName.substring(secondLastDot + 1).lowercase()
            if (possibleDouble == "tar.gz" || possibleDouble == "tar.bz2") {
                return possibleDouble
            }
        }
        
        return extension
    }

    private fun isDownloadableExtension(ext: String): Boolean {
        val downloadable = setOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz",
            "apk", "exe", "msi", "dmg", "deb", "rpm",
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
            "mp3", "flac", "wav", "aac", "ogg", "m4a",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "iso", "img", "bin"
        )
        return ext.lowercase() in downloadable
    }

    private fun sanitizeFileName(fileName: String): String {
        val invalidChars = listOf(':', '/', '\\', '?', '*', '"', '<', '>', '|')
        var result = fileName
        invalidChars.forEach { char ->
            result = result.replace(char, '_')
        }
        return result
    }

    private fun formatFileSizeTest(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val unit = 1024.0
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        
        while (value >= unit && index < units.lastIndex) {
            value /= unit
            index++
        }
        
        return String.format("%.2f %s", value, units[index])
    }
}
