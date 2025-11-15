package com.nyapass.loader.di

import android.content.Context
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.download.DownloadEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 下载模块
 * 提供下载引擎相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    
    /**
     * 提供下载引擎
     */
    @Provides
    @Singleton
    fun provideDownloadEngine(
        @ApplicationContext context: Context,
        taskDao: DownloadTaskDao,
        partDao: DownloadPartDao,
        okHttpClient: OkHttpClient
    ): DownloadEngine {
        return DownloadEngine(
            context = context,
            taskDao = taskDao,
            partDao = partDao,
            okHttpClient = okHttpClient
        )
    }
}

