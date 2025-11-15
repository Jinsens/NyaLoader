package com.nyapass.loader.download

import com.nyapass.loader.data.model.DownloadPriority
import org.junit.Test
import org.junit.Assert.*

/**
 * 下载优先级测试
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
class DownloadPriorityTest {

    @Test
    fun `priority values should be in correct order`() {
        assertTrue(
            "HIGH should have higher value than NORMAL",
            DownloadPriority.HIGH.value > DownloadPriority.NORMAL.value
        )
        assertTrue(
            "NORMAL should have higher value than LOW",
            DownloadPriority.NORMAL.value > DownloadPriority.LOW.value
        )
    }

    @Test
    fun `fromValue should return correct priority`() {
        assertEquals(DownloadPriority.LOW, DownloadPriority.fromValue(0))
        assertEquals(DownloadPriority.NORMAL, DownloadPriority.fromValue(1))
        assertEquals(DownloadPriority.HIGH, DownloadPriority.fromValue(2))
    }

    @Test
    fun `fromValue with invalid value should return NORMAL`() {
        assertEquals(DownloadPriority.NORMAL, DownloadPriority.fromValue(-1))
        assertEquals(DownloadPriority.NORMAL, DownloadPriority.fromValue(100))
    }

    @Test
    fun `all priorities should have non-empty display names`() {
        DownloadPriority.entries.forEach { priority ->
            assertTrue(
                "Display name for $priority should not be empty",
                priority.displayName.isNotBlank()
            )
        }
    }

    @Test
    fun `priorities should be sortable by value`() {
        val priorities = listOf(
            DownloadPriority.NORMAL,
            DownloadPriority.HIGH,
            DownloadPriority.LOW
        )

        val sorted = priorities.sortedByDescending { it.value }

        assertEquals(DownloadPriority.HIGH, sorted[0])
        assertEquals(DownloadPriority.NORMAL, sorted[1])
        assertEquals(DownloadPriority.LOW, sorted[2])
    }
}
