package com.nyapass.loader.data.dao

import androidx.room.*
import com.nyapass.loader.data.model.DownloadStatsRecord

/**
 * 下载统计数据访问对象
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
@Dao
interface DownloadStatsDao {

    /**
     * 插入统计记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(record: DownloadStatsRecord): Long

    /**
     * 获取所有统计记录
     */
    @Query("SELECT * FROM download_stats ORDER BY completedAt DESC")
    suspend fun getAllStats(): List<DownloadStatsRecord>

    /**
     * 获取总下载数量
     */
    @Query("SELECT COUNT(*) FROM download_stats")
    suspend fun getTotalCount(): Int

    /**
     * 获取总下载大小
     */
    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM download_stats")
    suspend fun getTotalSize(): Long

    /**
     * 按文件类型分组统计
     */
    @Query("""
        SELECT fileType, COUNT(*) as count, COALESCE(SUM(fileSize), 0) as totalSize
        FROM download_stats
        GROUP BY fileType
        ORDER BY count DESC
    """)
    suspend fun getStatsByFileType(): List<FileTypeStatsResult>

    /**
     * 清除所有统计数据
     */
    @Query("DELETE FROM download_stats")
    suspend fun clearAllStats()

    /**
     * 检查是否存在指定文件名和完成时间的记录（用于防止重复记录）
     */
    @Query("SELECT COUNT(*) FROM download_stats WHERE fileName = :fileName AND fileSize = :fileSize")
    suspend fun countByFileNameAndSize(fileName: String, fileSize: Long): Int
}

/**
 * 文件类型统计查询结果
 */
data class FileTypeStatsResult(
    val fileType: String,
    val count: Int,
    val totalSize: Long
)
