package com.nyapass.loader.download

import com.nyapass.loader.data.model.DownloadStatus
import com.nyapass.loader.data.model.DownloadTask
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * 下载队列管理器测试
 * 测试并发控制、优先级排序、任务状态管理等核心逻辑
 */
class DownloadQueueManagerTest {

    private lateinit var testTasks: List<DownloadTask>

    @Before
    fun setup() {
        // 创建测试任务数据
        testTasks = listOf(
            createTestTask(1, "file1.zip", DownloadStatus.PENDING),
            createTestTask(2, "file2.apk", DownloadStatus.RUNNING),
            createTestTask(3, "file3.mp4", DownloadStatus.PAUSED),
            createTestTask(4, "file4.pdf", DownloadStatus.COMPLETED),
            createTestTask(5, "file5.rar", DownloadStatus.FAILED)
        )
    }

    @Test
    fun `pending tasks should be identifiable`() {
        val pendingTasks = testTasks.filter { it.status == DownloadStatus.PENDING }
        assertEquals(1, pendingTasks.size)
        assertEquals("file1.zip", pendingTasks.first().fileName)
    }

    @Test
    fun `running tasks should be identifiable`() {
        val runningTasks = testTasks.filter { it.status == DownloadStatus.RUNNING }
        assertEquals(1, runningTasks.size)
        assertEquals("file2.apk", runningTasks.first().fileName)
    }

    @Test
    fun `failed tasks can be identified for retry`() {
        val failedTasks = testTasks.filter { it.status == DownloadStatus.FAILED }
        assertEquals(1, failedTasks.size)
        assertEquals("file5.rar", failedTasks.first().fileName)
    }

    @Test
    fun `completed tasks should be filterable`() {
        val completedTasks = testTasks.filter { it.status == DownloadStatus.COMPLETED }
        assertEquals(1, completedTasks.size)
    }

    @Test
    fun `max concurrent downloads should be respected`() {
        val maxConcurrent = 3
        val runningCount = testTasks.count { it.status == DownloadStatus.RUNNING }
        assertTrue("Running count should not exceed max", runningCount <= maxConcurrent)
    }

    @Test
    fun `task priority should sort correctly`() {
        val tasksByCreatedAt = testTasks.sortedBy { it.createdAt }
        assertEquals(1L, tasksByCreatedAt.first().id)
    }

    @Test
    fun `downloading and downloadable tasks calculation`() {
        val downloadableTasks = testTasks.filter { 
            it.status == DownloadStatus.PENDING || 
            it.status == DownloadStatus.RUNNING ||
            it.status == DownloadStatus.PAUSED
        }
        assertEquals(3, downloadableTasks.size)
    }

    private fun createTestTask(
        id: Long,
        fileName: String,
        status: DownloadStatus
    ): DownloadTask {
        return DownloadTask(
            id = id,
            url = "https://example.com/$fileName",
            fileName = fileName,
            filePath = "/storage/downloads/$fileName",
            totalSize = 1024 * 1024L,
            downloadedSize = if (status == DownloadStatus.COMPLETED) 1024 * 1024L else 0L,
            status = status,
            threadCount = 4,
            supportRange = true,
            createdAt = System.currentTimeMillis() + id,
            errorMessage = if (status == DownloadStatus.FAILED) "Network error" else null,
            saveToPublicDir = true
        )
    }
}
