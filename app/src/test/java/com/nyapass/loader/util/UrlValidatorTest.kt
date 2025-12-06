package com.nyapass.loader.util

import org.junit.Test
import org.junit.Assert.*

/**
 * URL 验证工具测试
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
class UrlValidatorTest {

    @Test
    fun `valid http url should return true`() {
        val urls = listOf(
            "http://example.com/file.zip",
            "http://192.168.1.1/download.exe",
            "http://localhost:8080/test.pdf"
        )

        urls.forEach { url ->
            assertTrue("Expected $url to be valid", isValidDownloadUrl(url))
        }
    }

    @Test
    fun `valid https url should return true`() {
        val urls = listOf(
            "https://example.com/file.zip",
            "https://cdn.example.org/path/to/file.tar.gz",
            "https://files.example.net:443/download?id=123"
        )

        urls.forEach { url ->
            assertTrue("Expected $url to be valid", isValidDownloadUrl(url))
        }
    }

    @Test
    fun `invalid url should return false`() {
        val urls = listOf(
            "",
            "   ",
            "not-a-url",
            "ftp://example.com/file.zip",
            "file:///local/path",
            "javascript:alert(1)",
            "data:text/html,<script>alert(1)</script>"
        )

        urls.forEach { url ->
            assertFalse("Expected $url to be invalid", isValidDownloadUrl(url))
        }
    }

    @Test
    fun `url with whitespace should be trimmed and validated`() {
        val url = "  https://example.com/file.zip  "
        assertTrue(isValidDownloadUrl(url.trim()))
    }

    @Test
    fun `case insensitive protocol check`() {
        val urls = listOf(
            "HTTP://example.com/file.zip",
            "HTTPS://example.com/file.zip",
            "Http://example.com/file.zip",
            "Https://example.com/file.zip"
        )

        urls.forEach { url ->
            assertTrue("Expected $url to be valid", isValidDownloadUrl(url))
        }
    }

    /**
     * 验证是否为有效的下载 URL
     */
    private fun isValidDownloadUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val trimmed = url.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
               trimmed.startsWith("https://", ignoreCase = true)
    }
}
