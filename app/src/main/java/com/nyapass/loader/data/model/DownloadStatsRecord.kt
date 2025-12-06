package com.nyapass.loader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 下载统计记录实体
 * 用于持久化存储下载统计数据，与下载任务的删除独立
 *
 * @author 小花生FMR
 * @version 2.4.0
 */
@Entity(tableName = "download_stats")
data class DownloadStatsRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 文件名 */
    val fileName: String,

    /** 文件类型分类 (video, audio, image, document, archive, application, other) */
    val fileType: String,

    /** 文件大小（字节） */
    val fileSize: Long,

    /** 下载完成时间 */
    val completedAt: Long = System.currentTimeMillis()
)
