package com.nyapass.loader.di

import android.content.Context
import com.nyapass.loader.data.dao.DownloadPartDao
import com.nyapass.loader.data.dao.DownloadTagDao
import com.nyapass.loader.data.dao.DownloadTaskDao
import com.nyapass.loader.download.DownloadEngine
import com.nyapass.loader.repository.DownloadRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Repository 模块
 * 提供数据仓库相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * 提供下载 Repository
     */
    @Provides
    @Singleton
    fun provideDownloadRepository(
        @ApplicationContext context: Context,
        taskDao: DownloadTaskDao,
        partDao: DownloadPartDao,
        tagDao: DownloadTagDao,
        downloadEngine: DownloadEngine,
        okHttpClient: OkHttpClient
    ): DownloadRepository {
        return DownloadRepository(
            context = context,
            taskDao = taskDao,
            partDao = partDao,
            downloadEngine = downloadEngine,
            tagDao = tagDao,
            okHttpClient = okHttpClient
        )
    }
}

