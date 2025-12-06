package com.nyapass.loader.repository

import org.junit.Test
import org.junit.Assert.*

/**
 * 磁盘空间检查测试
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
class DiskSpaceCheckerTest {

    @Test
    fun `sufficient space should return Sufficient result`() {
        val available = 10 * 1024 * 1024 * 1024L // 10GB
        val required = 1 * 1024 * 1024 * 1024L // 1GB

        val result = checkDiskSpace(available, required)
        assertTrue(result is DiskSpaceResult.Sufficient)
    }

    @Test
    fun `insufficient space should return InsufficientSpace result`() {
        val available = 500 * 1024 * 1024L // 500MB
        val required = 1 * 1024 * 1024 * 1024L // 1GB

        val result = checkDiskSpace(available, required)
        assertTrue(result is DiskSpaceResult.InsufficientSpace)

        val insufficientResult = result as DiskSpaceResult.InsufficientSpace
        assertEquals(available, insufficientResult.availableBytes)
        assertEquals(required, insufficientResult.requiredBytes)
    }

    @Test
    fun `low space should return LowSpace result`() {
        val available = 1100 * 1024 * 1024L // 1.1GB
        val required = 1 * 1024 * 1024 * 1024L // 1GB (available is less than 110% of required)

        val result = checkDiskSpace(available, required)
        assertTrue(
            "Should be LowSpace when available is less than 110% of required",
            result is DiskSpaceResult.LowSpace
        )
    }

    @Test
    fun `zero required bytes should always be sufficient`() {
        val available = 100L
        val required = 0L

        val result = checkDiskSpace(available, required)
        assertTrue(result is DiskSpaceResult.Sufficient)
    }

    @Test
    fun `zero available bytes should be insufficient`() {
        val available = 0L
        val required = 1024L

        val result = checkDiskSpace(available, required)
        assertTrue(result is DiskSpaceResult.InsufficientSpace)
    }

    @Test
    fun `exact match should be LowSpace`() {
        val bytes = 1 * 1024 * 1024 * 1024L // 1GB
        val result = checkDiskSpace(bytes, bytes)

        // 刚好等于需求时，应该警告空间紧张
        assertTrue(result is DiskSpaceResult.LowSpace)
    }

    /**
     * 检查磁盘空间
     */
    private fun checkDiskSpace(availableBytes: Long, requiredBytes: Long): DiskSpaceResult {
        return when {
            availableBytes < requiredBytes ->
                DiskSpaceResult.InsufficientSpace(availableBytes, requiredBytes)
            availableBytes < requiredBytes * 1.1 ->
                DiskSpaceResult.LowSpace(availableBytes)
            else ->
                DiskSpaceResult.Sufficient
        }
    }

    /**
     * 磁盘空间检查结果
     */
    sealed class DiskSpaceResult {
        data object Sufficient : DiskSpaceResult()
        data class LowSpace(val availableBytes: Long) : DiskSpaceResult()
        data class InsufficientSpace(
            val availableBytes: Long,
            val requiredBytes: Long
        ) : DiskSpaceResult()
    }
}
