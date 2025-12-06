package com.nyapass.loader.di

import android.content.Context
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadStatsDao
import com.nyapass.loader.data.dao.DownloadTagDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块
 * 提供数据库相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * 提供数据库实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    /**
     * 提供 DownloadTaskDao
     */
    @Provides
    @Singleton
    fun provideDownloadTaskDao(database: AppDatabase): DownloadTaskDao {
        return database.downloadTaskDao()
    }
    
    /**
     * 提供 DownloadPartDao
     */
    @Provides
    @Singleton
    fun provideDownloadPartDao(database: AppDatabase): DownloadPartDao {
        return database.downloadPartDao()
    }

    /**
     * 提供 DownloadTagDao
     */
    @Provides
    @Singleton
    fun provideDownloadTagDao(database: AppDatabase): DownloadTagDao {
        return database.downloadTagDao()
    }

    /**
     * 提供 DownloadStatsDao
     */
    @Provides
    @Singleton
    fun provideDownloadStatsDao(database: AppDatabase): DownloadStatsDao {
        return database.downloadStatsDao()
    }
}

