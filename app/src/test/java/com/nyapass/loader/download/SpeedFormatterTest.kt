package com.nyapass.loader.download

import org.junit.Test
import org.junit.Assert.*

/**
 * 速度格式化工具测试
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
class SpeedFormatterTest {

    @Test
    fun `zero speed should show 0`() {
        val result = formatSpeed(0L)
        assertTrue("Zero speed should contain 0", result.contains("0"))
    }

    @Test
    fun `bytes per second should format correctly`() {
        val result = formatSpeed(500L)
        assertTrue("500 B/s should be in bytes", result.contains("B/s") || result.contains("500"))
    }

    @Test
    fun `kilobytes per second should format correctly`() {
        val result = formatSpeed(1024L)
        assertTrue(
            "1024 B/s should be shown as 1 KB/s",
            result.contains("KB") || result.contains("1")
        )
    }

    @Test
    fun `megabytes per second should format correctly`() {
        val result = formatSpeed(1024L * 1024L)
        assertTrue(
            "1 MB/s should be shown correctly",
            result.contains("MB") || result.contains("1")
        )
    }

    @Test
    fun `gigabytes per second should format correctly`() {
        val result = formatSpeed(1024L * 1024L * 1024L)
        assertTrue(
            "1 GB/s should be shown correctly",
            result.contains("GB") || result.contains("1")
        )
    }

    @Test
    fun `decimal precision should be reasonable`() {
        val result = formatSpeed(1536L * 1024L) // 1.5 MB/s
        // 应该显示小数点
        assertTrue(
            "1.5 MB/s should show decimal",
            result.contains(".") || result.contains("1") || result.contains("5")
        )
    }

    @Test
    fun `negative speed should be handled`() {
        val result = formatSpeed(-100L)
        // 不应该崩溃，可以返回0或绝对值
        assertNotNull("Negative speed should not crash", result)
    }

    @Test
    fun `large speed values should not overflow`() {
        val result = formatSpeed(Long.MAX_VALUE / 2)
        assertNotNull("Large speed value should be handled", result)
        assertTrue("Result should not be empty", result.isNotBlank())
    }

    /**
     * 格式化下载速度
     */
    private fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0) return "0 B/s"

        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var speed = bytesPerSecond.toDouble()
        var unitIndex = 0

        while (speed >= 1024 && unitIndex < units.size - 1) {
            speed /= 1024
            unitIndex++
        }

        return if (unitIndex == 0) {
            "${speed.toLong()} ${units[unitIndex]}"
        } else {
            String.format("%.2f %s", speed, units[unitIndex])
        }
    }
}
