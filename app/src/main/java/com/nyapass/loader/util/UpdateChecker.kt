package com.nyapass.loader.util

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * GitHub 更新检测工具类
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
@Immutable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String? = null,  // 兼容旧格式
    val apkUrls: Map<String, String>? = null,  // 新格式：多架构支持
    val updateTime: String? = null,
    val forceUpdate: Boolean = false,
    val msg: String
) {
    /**
     * 根据设备架构获取最佳的 APK 下载地址
     */
    fun getBestApkUrl(): String {
        // 如果有新格式的 apkUrls，根据架构选择
        if (apkUrls != null && apkUrls.isNotEmpty()) {
            val abi = getDeviceAbi()
            
            // 优先匹配设备主架构
            apkUrls[abi]?.let { return it }
            
            // 回退到通用版本
            apkUrls["universal"]?.let { return it }
            
            // 如果都没有，返回第一个可用的
            return apkUrls.values.first()
        }
        
        // 兼容旧格式，直接返回 apkUrl
        return apkUrl ?: ""
    }
    
    /**
     * 获取设备主架构
     */
    private fun getDeviceAbi(): String {
        val abis = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.os.Build.SUPPORTED_ABIS
        } else {
            @Suppress("DEPRECATION")
            arrayOf(android.os.Build.CPU_ABI, android.os.Build.CPU_ABI2)
        }
        
        // 返回第一个支持的架构
        return when {
            abis.any { it.startsWith("arm64") } -> "arm64-v8a"
            abis.any { it.startsWith("armeabi-v7a") } -> "armeabi-v7a"
            abis.any { it.startsWith("x86_64") } -> "x86_64"
            abis.any { it.startsWith("x86") } -> "x86"
            else -> "universal"  // 默认使用通用版本
        }
    }
}

object UpdateChecker {
    
    private const val TAG = "UpdateChecker"
    
    // GitHub 原始文件链接（请替换为您的仓库信息）
    private const val VERSION_JSON_URL = 
        "https://raw.githubusercontent.com/Jinsens/NyaLoader/main/version.json"
    
    // SharedPreferences 相关
    private const val PREF_NAME = "update_checker"
    private const val KEY_IGNORED_VERSION_CODE = "ignored_version_code"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    
    // 检查间隔（24小时，单位：毫秒）
    private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L
    
    /**
     * 检查更新
     * 
     * @param context 上下文
     * @param currentVersionCode 当前应用的 versionCode
     * @param okHttpClient OkHttp 客户端
     * @return VersionInfo 如果有更新返回版本信息，否则返回 null
     */
    suspend fun checkForUpdate(
        context: Context,
        currentVersionCode: Int,
        okHttpClient: OkHttpClient
    ): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            // 检查是否需要检查更新（避免频繁请求）
            if (!shouldCheckUpdate(context)) {
                Log.d(TAG, "距离上次检查时间过短，跳过本次检查")
                return@withContext null
            }
            
            // 从 GitHub 拉取 version.json
            val request = Request.Builder()
                .url(VERSION_JSON_URL)
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "获取版本信息失败: HTTP ${response.code}")
                return@withContext null
            }

            val jsonString = response.body.string()
            if (jsonString.isBlank()) {
                Log.e(TAG, "版本信息为空")
                return@withContext null
            }
            
            // 解析 JSON
            val versionInfo = try {
                Gson().fromJson(jsonString, VersionInfo::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "解析版本信息失败", e)
                return@withContext null
            }
            
            Log.d(TAG, "远程版本: ${versionInfo.versionCode}, 当前版本: $currentVersionCode")
            
            // 更新最后检查时间
            saveLastCheckTime(context)
            
            // 比较版本号
            if (versionInfo.versionCode > currentVersionCode) {
                // 检查是否被用户忽略
                val ignoredVersion = getIgnoredVersionCode(context)
                if (!versionInfo.forceUpdate && versionInfo.versionCode <= ignoredVersion) {
                    Log.d(TAG, "用户已忽略版本 ${versionInfo.versionCode}")
                    return@withContext null
                }
                
                Log.d(TAG, "发现新版本: ${versionInfo.versionName} (${versionInfo.versionCode})")
                return@withContext versionInfo
            } else {
                Log.d(TAG, "当前已是最新版本")
                return@withContext null
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "网络请求失败", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "检查更新时发生错误", e)
            null
        }
    }
    
    /**
     * 判断是否需要检查更新
     * 避免频繁请求，每24小时最多检查一次
     */
    private fun shouldCheckUpdate(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastCheckTime) >= CHECK_INTERVAL
    }
    
    /**
     * 保存最后检查时间
     */
    private fun saveLastCheckTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
    }
    
    /**
     * 获取用户忽略的版本号
     */
    private fun getIgnoredVersionCode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_IGNORED_VERSION_CODE, 0)
    }
    
    /**
     * 保存用户忽略的版本号
     */
    fun ignoreVersion(context: Context, versionCode: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_IGNORED_VERSION_CODE, versionCode).apply()
        Log.d(TAG, "用户忽略版本: $versionCode")
    }
    
    /**
     * 清除忽略的版本（用于测试或用户主动检查更新时）
     */
    fun clearIgnoredVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_IGNORED_VERSION_CODE).apply()
    }
    
    /**
     * 手动检查更新（忽略时间间隔限制）
     * 用于用户主动点击"检查更新"按钮
     */
    suspend fun checkForUpdateManually(
        context: Context,
        currentVersionCode: Int,
        okHttpClient: OkHttpClient
    ): VersionInfo? = withContext(Dispatchers.IO) {
        // 清除忽略的版本和时间限制
        clearIgnoredVersion(context)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, 0).apply()
        
        // 调用正常的检查更新流程
        checkForUpdate(context, currentVersionCode, okHttpClient)
    }
}

