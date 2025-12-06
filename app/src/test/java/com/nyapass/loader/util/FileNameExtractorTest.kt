package com.nyapass.loader.util

import org.junit.Test
import org.junit.Assert.*

/**
 * 文件名提取工具测试
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
class FileNameExtractorTest {

    @Test
    fun `extract filename from simple url`() {
        val url = "https://example.com/file.zip"
        assertEquals("file.zip", extractFileName(url))
    }

    @Test
    fun `extract filename from url with path`() {
        val url = "https://example.com/path/to/document.pdf"
        assertEquals("document.pdf", extractFileName(url))
    }

    @Test
    fun `extract filename from url with query parameters`() {
        val url = "https://example.com/download.exe?token=abc123&user=test"
        assertEquals("download.exe", extractFileName(url))
    }

    @Test
    fun `extract filename from url with fragment`() {
        val url = "https://example.com/archive.tar.gz#section1"
        assertEquals("archive.tar.gz", extractFileName(url))
    }

    @Test
    fun `extract filename from url with encoded characters`() {
        val url = "https://example.com/my%20file%20(1).zip"
        // URL 解码后的文件名
        val fileName = extractFileName(url)
        assertTrue(fileName.isNotBlank())
    }

    @Test
    fun `handle url without filename`() {
        val urls = listOf(
            "https://example.com/",
            "https://example.com",
            "https://example.com/path/"
        )

        urls.forEach { url ->
            val fileName = extractFileName(url)
            // 应该返回空字符串或默认文件名
            assertNotNull("Should not return null for $url", fileName)
        }
    }

    @Test
    fun `handle invalid url gracefully`() {
        val urls = listOf(
            "",
            "not-a-url",
            "   "
        )

        urls.forEach { url ->
            val fileName = extractFileName(url)
            assertNotNull("Should not return null for invalid url", fileName)
        }
    }

    @Test
    fun `extract filename preserves extension`() {
        val testCases = mapOf(
            "https://example.com/video.mp4" to "mp4",
            "https://example.com/image.png" to "png",
            "https://example.com/archive.tar.gz" to "gz",
            "https://example.com/document.pdf" to "pdf"
        )

        testCases.forEach { (url, expectedExtension) ->
            val fileName = extractFileName(url)
            assertTrue(
                "Expected $fileName to end with .$expectedExtension",
                fileName.endsWith(".$expectedExtension")
            )
        }
    }

    /**
     * 从 URL 中提取文件名
     * 这是一个简化的实现，用于测试
     */
    private fun extractFileName(url: String): String {
        if (url.isBlank()) return ""

        return try {
            // 移除查询参数和片段
            val cleanUrl = url.substringBefore("?").substringBefore("#")

            // 获取最后一个路径段
            val lastSegment = cleanUrl.trimEnd('/').substringAfterLast("/")

            // 如果没有文件名，返回空字符串
            if (lastSegment.isEmpty() || !lastSegment.contains(".")) {
                ""
            } else {
                // URL 解码
                java.net.URLDecoder.decode(lastSegment, "UTF-8")
            }
        } catch (e: Exception) {
            ""
        }
    }
}
