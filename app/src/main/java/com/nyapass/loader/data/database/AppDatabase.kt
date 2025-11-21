package com.nyapass.loader.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadTagDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.model.DownloadPartInfo
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.data.model.DownloadTask
import com.nyapass.loader.data.model.TaskTagCrossRef

@Database(
    entities = [
        DownloadTask::class,
        DownloadPartInfo::class,
        DownloadTag::class,
        TaskTagCrossRef::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun downloadPartDao(): DownloadPartDao
    abstract fun downloadTagDao(): DownloadTagDao
    
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
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gdownload_database"
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

