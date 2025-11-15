package com.nyapass.loader.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络模块
 * 提供网络相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val TAG = "NetworkModule"
    
    /**
     * 提供高性能 OkHttpClient
     *
     * 
     * 性能优化策略（平衡模式）：
     * 1. 大型连接池（128个） - 支持高并发
     * 2. HTTP/2 支持 - 多路复用优化
     * 3. 优化调度器（512请求） - 合理并发
     * 4. 平衡超时 - 稳定性优先
     * 5. 智能重试机制
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // ========== 连接池配置 ==========
            // 最大空闲连接：128个
            // 保持时间：10分钟
            // 提供充足的连接复用，避免频繁建立连接
            .connectionPool(
                ConnectionPool(
                    maxIdleConnections = 128,       // 128个空闲连接
                    keepAliveDuration = 10,         // 保持10分钟
                    timeUnit = TimeUnit.MINUTES
                )
            )
            
            // ========== 调度器配置 ==========
            // 每个主机最大并发：64个
            // 避免过高并发导致服务器拒绝连接
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 512               // 平衡：512个并发请求
                    maxRequestsPerHost = 64         // 平衡：每主机64个并发
                }
            )
            
            // ========== 协议配置 ==========
            // HTTP/2 提供多路复用，减少连接开销
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            
            // ========== 超时配置 ==========
            // 连接超时：15秒（给服务器足够响应时间）
            .connectTimeout(15, TimeUnit.SECONDS)
            
            // 读取超时：90秒（支持慢速网络和大文件）
            .readTimeout(90, TimeUnit.SECONDS)
            
            // 写入超时：90秒（支持大文件上传）
            .writeTimeout(90, TimeUnit.SECONDS)
            
            // 完整调用超时：600秒（10分钟，支持超大文件）
            .callTimeout(600, TimeUnit.SECONDS)
            
            // ========== 连接配置 ==========
            // 连接失败时自动重试
            .retryOnConnectionFailure(true)
            
            // 自动跟随重定向
            .followRedirects(true)
            .followSslRedirects(true)
            
            // 禁用缓存以减少开销
            .cache(null)
            
            // ========== 智能重试拦截器 ==========
            .addInterceptor { chain ->
                val request = chain.request()
                var response: okhttp3.Response? = null
                var lastException: Exception? = null
                var attempt = 0
                val maxAttempts = 3
                
                while (attempt < maxAttempts) {
                    try {
                        response = chain.proceed(request)
                        
                        // 检查响应状态
                        if (response.isSuccessful || response.code == 206) {
                            return@addInterceptor response
                        } else if (response.code == 429) {
                            // 429 Too Many Requests - 等待后重试
                            response.close()
                            attempt++
                            if (attempt < maxAttempts) {
                                Log.w(TAG, "请求过多(429)，等待${attempt}秒后重试: ${request.url.host}")
                                Thread.sleep(attempt * 1000L)
                            }
                        } else if (response.code >= 500) {
                            // 5xx 服务器错误 - 重试
                            response.close()
                            attempt++
                            if (attempt < maxAttempts) {
                                Log.w(TAG, "服务器错误(${response.code})，重试${attempt}/${maxAttempts}: ${request.url.host}")
                                Thread.sleep(1000L)
                            }
                        } else {
                            // 其他错误不重试
                            return@addInterceptor response
                        }
                    } catch (e: Exception) {
                        lastException = e
                        attempt++
                        if (attempt < maxAttempts) {
                            Log.w(TAG, "连接失败，重试${attempt}/${maxAttempts}: ${e.message}")
                            Thread.sleep(attempt * 500L)
                        }
                    }
                }
                
                // 所有重试都失败
                if (response != null) {
                    return@addInterceptor response
                } else {
                    throw lastException ?: Exception("连接失败")
                }
            }
            
            // ========== 性能监控 ==========
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    val startTime = System.nanoTime()
                    val response = chain.proceed(request)
                    val duration = (System.nanoTime() - startTime) / 1_000_000
                    
                    // 只记录超慢请求（超过10秒）
                    if (duration > 10000) {
                        Log.w(TAG, "慢速请求: ${request.url.host} 耗时: ${duration}ms")
                    }
                    
                    response
                } else {
                    chain.proceed(request)
                }
            }
            
            .build()

    }
}
