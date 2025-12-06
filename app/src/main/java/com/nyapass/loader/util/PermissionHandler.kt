package com.nyapass.loader.util

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 权限处理器
 * 统一管理应用的权限请求
 */
class PermissionHandler(private val activity: ComponentActivity) {

    // 权限请求启动器
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // 所有文件访问权限启动器（Android 11+）
    private lateinit var allFilesAccessLauncher: ActivityResultLauncher<Intent>

    // SharedPreferences 用于记录权限请求状态
    private val prefs by lazy {
        activity.getSharedPreferences("permission_prefs", android.content.Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_ALL_FILES_ACCESS_REQUESTED = "all_files_access_requested"
    }
    
    /**
     * 初始化权限启动器
     * 必须在 onCreate 中调用
     */
    fun initialize() {
        // 权限请求启动器
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (!allGranted) {
                Toast.makeText(
                    activity,
                    "需要存储和通知权限才能正常使用应用",
                    Toast.LENGTH_LONG
                ).show()
            }
            
            // 检查是否需要请求所有文件访问权限（Android 11+）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (!PermissionUtils.hasAllFilesAccess()) {
                    requestAllFilesAccess()
                }
            }
        }
        
        // 所有文件访问权限设置页启动器（Android 11+）
        allFilesAccessLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (PermissionUtils.hasAllFilesAccess()) {
                    Toast.makeText(
                        activity,
                        "已授予所有文件访问权限",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // 用户拒绝了权限，记录下来，不再每次启动都请求
                    prefs.edit().putBoolean(KEY_ALL_FILES_ACCESS_REQUESTED, true).apply()
                    Toast.makeText(
                        activity,
                        "需要所有文件访问权限才能下载到自定义目录",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    /**
     * 请求必要的权限
     */
    fun requestPermissionsIfNeeded() {
        // 1. 先请求常规权限
        val permissions = PermissionUtils.getRequiredPermissions()
        if (permissions.isNotEmpty()) {
            val needRequest = permissions.any { permission ->
                activity.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (needRequest) {
                permissionLauncher.launch(permissions)
                return
            }
        }

        // 2. 如果常规权限已授予，检查所有文件访问权限（Android 11+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // 只有在用户未曾拒绝过且当前没有权限时才请求
            val hasRequested = prefs.getBoolean(KEY_ALL_FILES_ACCESS_REQUESTED, false)
            if (!PermissionUtils.hasAllFilesAccess() && !hasRequested) {
                requestAllFilesAccess()
            }
        }
    }
    
    /**
     * 请求所有文件访问权限（Android 11+）
     * 需要跳转到系统设置页面
     */
    private fun requestAllFilesAccess() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    android.net.Uri.parse("package:${activity.packageName}")
                )
                
                // 显示提示
                Toast.makeText(
                    activity,
                    "请在设置页面中开启「所有文件访问权限」",
                    Toast.LENGTH_LONG
                ).show()
                
                // 延迟500ms后跳转到设置页面
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    allFilesAccessLauncher.launch(intent)
                }, 500)
            } catch (e: Exception) {
                // 如果无法打开设置页面，尝试打开通用设置
                Toast.makeText(
                    activity,
                    "请在设置中授予所有文件访问权限",
                    Toast.LENGTH_LONG
                ).show()
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                allFilesAccessLauncher.launch(intent)
            }
        }
    }
}

