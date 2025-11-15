package com.nyapass.loader.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadStatsDao
import com.nyapass.loader.data.dao.DownloadTagDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.model.DownloadPartInfo
import com.nyapass.loader.data.model.DownloadStatsRecord
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.model.TaskTagCrossRef

@Database(
    entities = [
        DownloadTask::class,
        DownloadPartInfo::class,
        DownloadTag::class,
        TaskTagCrossRef::class,
        DownloadStatsRecord::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun downloadPartDao(): DownloadPartDao
    abstract fun downloadTagDao(): DownloadTagDao
    abstract fun downloadStatsDao(): DownloadStatsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_download_tasks_status ON download_tasks(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_download_tasks_createdAt ON download_tasks(createdAt)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_download_parts_taskId_partIndex ON download_parts(taskId, partIndex)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS download_tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_download_tags_name ON download_tags(name)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_tag_cross_ref (
                        taskId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY(taskId, tagId),
                        FOREIGN KEY(taskId) REFERENCES download_tasks(id) ON DELETE CASCADE,
                        FOREIGN KEY(tagId) REFERENCES download_tags(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_tag_cross_ref_tagId ON task_tag_cross_ref(tagId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_tag_cross_ref_taskId ON task_tag_cross_ref(taskId)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE download_tags ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE download_tags SET sortOrder = createdAt WHERE sortOrder = 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加优先级字段，默认为普通优先级(1)
                db.execSQL("ALTER TABLE download_tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_download_tasks_priority ON download_tasks(priority)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建下载统计表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS download_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fileName TEXT NOT NULL,
                        fileType TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_download_stats_fileType ON download_stats(fileType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_download_stats_completedAt ON download_stats(completedAt)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gdownload_database"
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

