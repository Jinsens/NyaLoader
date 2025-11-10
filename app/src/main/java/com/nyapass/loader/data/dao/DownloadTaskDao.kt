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
    
    @Query("UPDATE download_tasks SET errorMessage = NULL WHERE id = :taskId")
    suspend fun clearErrorMessage(taskId: Long)
    
    @Query("DELETE FROM download_tasks WHERE status = :status")
    suspend fun deleteTasksByStatus(status: DownloadStatus)
}
