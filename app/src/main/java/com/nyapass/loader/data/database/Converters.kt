package com.nyapass.loader.data.database

import androidx.room.TypeConverter
import com.nyapass.loader.data.model.DownloadStatus

class Converters {
    
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            DownloadStatus.PENDING
        }
    }
}
