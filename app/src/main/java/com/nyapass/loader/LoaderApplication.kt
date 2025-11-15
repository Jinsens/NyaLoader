package com.nyapass.loader

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.nyapass.loader.data.database.AppDatabase
import com.nyapass.loader.data.preferences.AppPreferences
import com.nyapass.loader.download.DownloadEngine
import com.nyapass.loader.repository.DownloadRepository
import com.nyapass.loader.util.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Application类
 * 初始化全局依赖
 * 使用 Hilt 进行依赖注入
 * 
 * 优化启动性能：
 * - 关键组件立即初始化
 * - 非关键组件延迟到后台线程初始化
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
@HiltAndroidApp
class LoaderApplication : Application() {
    
    // 使用 Hilt 注入依赖
    @Inject
    lateinit var database: AppDatabase
    
    @Inject
    lateinit var appPreferences: AppPreferences
    
    @Inject
    lateinit var okHttpClient: OkHttpClient
    
    @Inject
    lateinit var downloadEngine: DownloadEngine
    
    @Inject
    lateinit var downloadRepository: DownloadRepository
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 【关键组件】立即初始化 - 必须在主线程完成
        initCriticalComponents()
        
        // 【非关键组件】延迟到后台线程初始化 - 优化启动时间
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            initNonCriticalComponents()
        }
    }
    
    /**
     * 初始化关键组件（主线程）
     * 这些组件必须在应用启动时立即可用
     */
    private fun initCriticalComponents() {
        try {
            // 应用语言设置（必须在主线程，影响 UI）
        applyLanguageSettings()
        
            Log.i(TAG, "✅ 关键组件初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 关键组件初始化失败", e)
        }
    }
    
    /**
     * 初始化非关键组件（后台线程）
     * 这些组件可以延迟初始化，不影响应用启动
     */
    private fun initNonCriticalComponents() {
        try {
            // Firebase 初始化（可延迟，用于统计分析）
        initFirebaseIfEnabled()
            
            Log.i(TAG, "✅ 非关键组件初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 非关键组件初始化失败", e)
        }
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

