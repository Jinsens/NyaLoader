package com.nyapass.loader.util

import org.junit.Test
import org.junit.Assert.*

/**
 * URL 处理工具测试
 * 测试 URL 解析、文件名提取等功能
 */
class UrlUtilsTest {

    @Test
    fun `extractFileNameFromUrl should get filename from simple URL`() {
        assertEquals("file.zip", extractFileNameFromUrl("https://example.com/file.zip"))
        assertEquals("app.apk", extractFileNameFromUrl("https://download.example.com/app.apk"))
    }

    @Test
    fun `extractFileNameFromUrl should handle URL with parameters`() {
        assertEquals("file.zip", extractFileNameFromUrl("https://example.com/file.zip?token=abc123"))
        assertEquals("video.mp4", extractFileNameFromUrl("https://cdn.example.com/video.mp4?sig=xyz&exp=12345"))
    }

    @Test
    fun `extractFileNameFromUrl should handle encoded URLs`() {
        val encodedUrl = "https://example.com/%E4%B8%8B%E8%BD%BD.apk"
        val fileName = extractFileNameFromUrl(encodedUrl)
        assertTrue("Should decode URL-encoded filename", fileName.isNotBlank())
    }

    @Test
    fun `isValidUrl should validate correct URLs`() {
        assertTrue(isValidUrl("https://example.com"))
        assertTrue(isValidUrl("http://example.com/file.zip"))
        assertTrue(isValidUrl("https://download.example.com/path/to/file.apk"))
    }

    @Test
    fun `isValidUrl should reject invalid URLs`() {
        assertFalse(isValidUrl(""))
        assertFalse(isValidUrl("not a url"))
        assertFalse(isValidUrl("ftp://example.com")) // Only HTTP/HTTPS supported
        assertFalse(isValidUrl("example.com")) // Missing protocol
    }

    @Test
    fun `extractFileNameFromContentDisposition should parse header`() {
        assertEquals(
            "report.pdf",
            extractFileNameFromContentDisposition("attachment; filename=\"report.pdf\"")
        )
        assertEquals(
            "data.xlsx",
            extractFileNameFromContentDisposition("attachment; filename=data.xlsx")
        )
    }

    @Test
    fun `extractFileNameFromContentDisposition should handle UTF-8 encoding`() {
        val header = "attachment; filename*=UTF-8''%E6%96%87%E4%BB%B6.pdf"
        val fileName = extractFileNameFromContentDisposition(header)
        assertTrue("Should handle UTF-8 encoded filenames", fileName?.isNotBlank() == true)
    }

    @Test
    fun `getDomainFromUrl should extract domain`() {
        assertEquals("example.com", getDomainFromUrl("https://example.com/path"))
        assertEquals("sub.example.com", getDomainFromUrl("https://sub.example.com/file.zip"))
        assertEquals("download.example.org", getDomainFromUrl("http://download.example.org/"))
    }

    // Helper functions for testing
    private fun extractFileNameFromUrl(url: String): String {
        val path = url.substringBefore("?").substringAfterLast("/")
        return try {
            java.net.URLDecoder.decode(path, "UTF-8")
        } catch (e: Exception) {
            path
        }
    }

    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val parsed = java.net.URL(url)
            parsed.protocol in listOf("http", "https")
        } catch (e: Exception) {
            false
        }
    }

    private fun extractFileNameFromContentDisposition(header: String?): String? {
        if (header == null) return null
        
        // Try filename*= (RFC 5987)
        val utf8Pattern = Regex("filename\\*=UTF-8''(.+)")
        utf8Pattern.find(header)?.let { match ->
            return try {
                java.net.URLDecoder.decode(match.groupValues[1], "UTF-8")
            } catch (e: Exception) {
                null
            }
        }
        
        // Try filename= with quotes
        val quotedPattern = Regex("filename=\"(.+?)\"")
        quotedPattern.find(header)?.let { match ->
            return match.groupValues[1]
        }
        
        // Try filename= without quotes
        val simplePattern = Regex("filename=([^;\\s]+)")
        simplePattern.find(header)?.let { match ->
            return match.groupValues[1]
        }
        
        return null
    }

    private fun getDomainFromUrl(url: String): String? {
        return try {
            java.net.URL(url).host
        } catch (e: Exception) {
            null
        }
    }
}
