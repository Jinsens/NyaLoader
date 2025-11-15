package com.nyapass.loader.data.dao

import androidx.room.*
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {
    
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<DownloadTask>>
    
    @Query("SELECT * FROM download_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): DownloadTask?
    
    @Query("SELECT * FROM download_tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: Long): Flow<DownloadTask?>
    
    @Query("SELECT * FROM download_tasks WHERE status = :status")
    fun getTasksByStatus(status: DownloadStatus): Flow<List<DownloadTask>>

    @Query("SELECT id FROM download_tasks WHERE status = :status")
    suspend fun getTaskIdsByStatus(status: DownloadStatus): List<Long>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTask): Long
    
    @Update
    suspend fun updateTask(task: DownloadTask)
    
    @Delete
    suspend fun deleteTask(task: DownloadTask)
    
    @Query("DELETE FROM download_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
    
    @Query("UPDATE download_tasks SET downloadedSize = :downloadedSize, speed = :speed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateProgress(taskId: Long, downloadedSize: Long, speed: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE download_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: DownloadStatus, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE download_tasks SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateStatusWithError(taskId: Long, status: DownloadStatus, errorMessage: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE download_tasks SET filePath = :filePath, finalContentUri = :finalContentUri, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateFileLocation(taskId: Long, filePath: String, finalContentUri: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE download_tasks SET fileName = :fileName, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateFileName(taskId: Long, fileName: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE download_tasks SET errorMessage = NULL WHERE id = :taskId")
    suspend fun clearErrorMessage(taskId: Long)
    
    @Query("DELETE FROM download_tasks WHERE status = :status")
    suspend fun deleteTasksByStatus(status: DownloadStatus)

    /**
     * 根据 URL 查找已存在的任务
     */
    @Query("SELECT * FROM download_tasks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): DownloadTask?

    /**
     * 根据文件名查找已存在的任务
     */
    @Query("SELECT * FROM download_tasks WHERE fileName = :fileName LIMIT 1")
    suspend fun findByFileName(fileName: String): DownloadTask?

    /**
     * 根据 URL 查找所有匹配的任务（可能有多个）
     */
    @Query("SELECT * FROM download_tasks WHERE url = :url")
    suspend fun findAllByUrl(url: String): List<DownloadTask>

    /**
     * 根据文件名查找所有匹配的任务（可能有多个）
     */
    @Query("SELECT * FROM download_tasks WHERE fileName = :fileName")
    suspend fun findAllByFileName(fileName: String): List<DownloadTask>

    /**
     * 获取所有任务（按优先级降序、创建时间升序排列）
     * 高优先级的任务会排在前面，同优先级按创建时间排序
     */
    @Query("SELECT * FROM download_tasks ORDER BY priority DESC, createdAt ASC")
    fun getAllTasksByPriority(): Flow<List<DownloadTask>>

    /**
     * 获取指定状态的任务（按优先级排序）
     */
    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun getTasksByStatusWithPriority(status: DownloadStatus): Flow<List<DownloadTask>>

    /**
     * 获取待处理任务（PENDING 状态，按优先级排序）
     */
    @Query("SELECT * FROM download_tasks WHERE status = 'PENDING' ORDER BY priority DESC, createdAt ASC")
    suspend fun getPendingTasksByPriority(): List<DownloadTask>

    /**
     * 更新任务优先级
     */
    @Query("UPDATE download_tasks SET priority = :priority, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updatePriority(taskId: Long, priority: Int, updatedAt: Long = System.currentTimeMillis())

    // ==================== 统计查询 ====================

    /**
     * 获取总下载任务数
     */
    @Query("SELECT COUNT(*) FROM download_tasks")
    suspend fun getTotalDownloadCount(): Int

    /**
     * 获取已完成任务数
     */
    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun getCompletedDownloadCount(): Int

    /**
     * 获取失败任务数
     */
    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = 'FAILED'")
    suspend fun getFailedDownloadCount(): Int

    /**
     * 获取总下载大小（已完成的任务）
     */
    @Query("SELECT COALESCE(SUM(totalSize), 0) FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun getTotalDownloadedSize(): Long

    /**
     * 获取所有已完成任务（用于统计文件类型）
     */
    @Query("SELECT * FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun getCompletedTasks(): List<DownloadTask>

    /**
     * 获取所有任务（用于完整统计）
     */
    @Query("SELECT * FROM download_tasks")
    suspend fun getAllTasksSync(): List<DownloadTask>
}
