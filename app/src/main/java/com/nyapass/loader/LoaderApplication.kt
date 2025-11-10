package com.nyapass.loader

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.nyapass.loader.data.database.AppDatabase
import com.nyapass.loader.data.preferences.AppPreferences
import com.nyapass.loader.download.DownloadEngine
import com.nyapass.loader.repository.DownloadRepository
import com.nyapass.loader.util.LocaleHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Application类
 * 初始化全局依赖
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
class LoaderApplication : Application() {
    
    // 数据库实例
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
    
    // 应用设置
    val appPreferences: AppPreferences by lazy {
        AppPreferences(this)
    }
    
    // OkHttp客户端
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    // 下载引擎
    val downloadEngine: DownloadEngine by lazy {
        DownloadEngine(
            context = this,
            taskDao = database.downloadTaskDao(),
            partDao = database.downloadPartDao(),
            okHttpClient = okHttpClient
        )
    }
    
    // Repository
    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(
            context = this,
            taskDao = database.downloadTaskDao(),
            partDao = database.downloadPartDao(),
            downloadEngine = downloadEngine
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 应用语言设置
        applyLanguageSettings()
        
        // 根据用户设置初始化 Firebase
        initFirebaseIfEnabled()
    }
    
    override fun attachBaseContext(base: Context) {
        // 在附加基础Context时应用语言设置
        val preferences = AppPreferences(base)
        val language = preferences.language.value
        val context = LocaleHelper.applyLanguage(base, language)
        super.attachBaseContext(context)
    }
    
    /**
     * 应用语言设置
     */
    private fun applyLanguageSettings() {
        val language = appPreferences.language.value
        LocaleHelper.applyLanguage(this, language)
    }
    
    /**
     * 根据用户设置初始化 Firebase
     * 仅在用户同意时初始化
     */
    private fun initFirebaseIfEnabled() {
        try {
            val firebaseEnabled = appPreferences.firebaseEnabled.value
            
            if (firebaseEnabled) {
                // 初始化 Firebase
                FirebaseApp.initializeApp(this)
                
                // 启用 Analytics 数据收集
                FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
                
                Log.i(TAG, "✅ Firebase 已启用并初始化成功")
            } else {
                // 禁用 Analytics 数据收集
                FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false)
                
                Log.i(TAG, "ℹ️ Firebase 分析已禁用")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase 初始化失败: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "LoaderApplication"
        
        lateinit var instance: LoaderApplication
            private set
    }
}

