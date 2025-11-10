package com.nyapass.loader.data.dao

import androidx.room.*
import com.nyapass.loader.data.model.DownloadPartInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadPartDao {
    
    @Query("SELECT * FROM download_parts WHERE taskId = :taskId ORDER BY partIndex")
    suspend fun getPartsByTaskId(taskId: Long): List<DownloadPartInfo>
    
    @Query("SELECT * FROM download_parts WHERE taskId = :taskId ORDER BY partIndex")
    fun getPartsByTaskIdFlow(taskId: Long): Flow<List<DownloadPartInfo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: DownloadPartInfo): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<DownloadPartInfo>)
    
    @Update
    suspend fun updatePart(part: DownloadPartInfo)
    
    @Delete
    suspend fun deletePart(part: DownloadPartInfo)
    
    @Query("DELETE FROM download_parts WHERE taskId = :taskId")
    suspend fun deletePartsByTaskId(taskId: Long)
    
    @Query("UPDATE download_parts SET downloadedByte = :downloadedByte WHERE id = :partId")
    suspend fun updatePartProgress(partId: Long, downloadedByte: Long)
    
    @Query("UPDATE download_parts SET isCompleted = 1 WHERE id = :partId")
    suspend fun markPartCompleted(partId: Long)
}
