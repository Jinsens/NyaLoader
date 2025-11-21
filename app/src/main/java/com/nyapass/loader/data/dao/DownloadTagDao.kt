package com.nyapass.loader.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.data.model.TagStatistics
import com.nyapass.loader.data.model.TaskTagCrossRef
import com.nyapass.loader.data.model.TaskTagRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTagDao {

    @Query("SELECT * FROM download_tags ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllTags(): Flow<List<DownloadTag>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: DownloadTag): Long

    @Update
    suspend fun updateTag(tag: DownloadTag)

    @Query("SELECT * FROM download_tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): DownloadTag?

    @Query("DELETE FROM download_tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTags(refs: List<TaskTagCrossRef>)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    suspend fun deleteTagsForTask(taskId: Long)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId IN (:taskIds)")
    suspend fun deleteTagsForTasks(taskIds: List<Long>)

    @Transaction
    suspend fun replaceTagsForTask(taskId: Long, tagIds: List<Long>) {
        deleteTagsForTask(taskId)
        if (tagIds.isNotEmpty()) {
            val refs = tagIds.map { TaskTagCrossRef(taskId = taskId, tagId = it) }
            insertTaskTags(refs)
        }
    }

    @Query("SELECT IFNULL(MAX(sortOrder), 0) FROM download_tags")
    suspend fun getMaxSortOrder(): Long

    @Query("UPDATE download_tags SET sortOrder = :sortOrder WHERE id = :tagId")
    suspend fun updateTagSortOrder(tagId: Long, sortOrder: Long)

    @Transaction
    suspend fun swapTagOrder(firstId: Long, firstOrder: Long, secondId: Long, secondOrder: Long) {
        updateTagSortOrder(firstId, secondOrder)
        updateTagSortOrder(secondId, firstOrder)
    }

    @Query(
        """
        SELECT 
            ref.taskId as taskId,
            tag.id as tag_id,
            tag.name as tag_name,
            tag.color as tag_color,
            tag.createdAt as tag_createdAt,
            tag.sortOrder as tag_sortOrder
        FROM task_tag_cross_ref ref
        INNER JOIN download_tags tag ON tag.id = ref.tagId
        """
    )
    fun getTaskTags(): Flow<List<TaskTagRelation>>

    @Query(
        """
        SELECT 
            tag.id as id,
            tag.name as name,
            tag.color as color,
            tag.createdAt as createdAt,
            COUNT(ref.taskId) as taskCount,
            IFNULL(SUM(task.totalSize), 0) as totalSize
        FROM download_tags tag
        LEFT JOIN task_tag_cross_ref ref ON tag.id = ref.tagId
        LEFT JOIN download_tasks task ON ref.taskId = task.id
        GROUP BY tag.id
        ORDER BY tag.sortOrder ASC, tag.createdAt DESC
        """
    )
    fun getTagStatistics(): Flow<List<TagStatistics>>
}
