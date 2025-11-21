package com.nyapass.loader.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 标签实体
 */
@Immutable
@Entity(
    tableName = "download_tags",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class DownloadTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long = DEFAULT_COLOR,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Long = createdAt
) {
    companion object {
        const val DEFAULT_COLOR: Long = 0xFF5C6BC0
    }
}

/**
 * 任务与标签关联表
 */
@Immutable
@Entity(
    tableName = "task_tag_cross_ref",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = DownloadTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DownloadTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tagId"]),
        Index(value = ["taskId"])
    ]
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)

/**
 * 任务标签映射数据
 */
@Immutable
data class TaskTagRelation(
    val taskId: Long,
    @Embedded(prefix = "tag_")
    val tag: DownloadTag
)

/**
 * 标签统计信息
 */
@Immutable
data class TagStatistics(
    val id: Long,
    val name: String,
    val color: Long,
    val createdAt: Long,
    val taskCount: Int,
    val totalSize: Long
)
