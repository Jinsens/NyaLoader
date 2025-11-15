package com.nyapass.loader.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.nyapass.loader.LoaderApplication
import com.nyapass.loader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * 更新处理器
 * 统一管理应用更新检查和下载
 */
class UpdateHandler(private val activity: ComponentActivity) {
    
    /**
     * 自动检查更新（启动时）
     * 
     * @param currentVersionCode 当前版本号
     * @param okHttpClient HTTP客户端
     * @param onUpdateAvailable 有更新时的回调
     */
    fun checkUpdateAutomatically(
        currentVersionCode: Int,
        okHttpClient: OkHttpClient,
        onUpdateAvailable: (VersionInfo) -> Unit
    ) {
        activity.lifecycleScope.launch {
            try {
                val versionInfo = UpdateChecker.checkForUpdate(
                    context = activity,
                    currentVersionCode = currentVersionCode,
                    okHttpClient = okHttpClient
                )
                
                if (versionInfo != null) {
                    onUpdateAvailable(versionInfo)
                }
            } catch (e: Exception) {
                // 更新检查失败，静默处理
                android.util.Log.e("UpdateHandler", "自动更新检查失败", e)
            }
        }
    }
    
    /**
     * 手动检查更新
     * 用于用户主动点击"检查更新"按钮
     */
    fun checkUpdateManually(
        currentVersionCode: Int,
        okHttpClient: OkHttpClient
    ) {
        // 显示检查中的提示
        Toast.makeText(
            activity,
            activity.getString(R.string.update_checking),
            Toast.LENGTH_SHORT
        ).show()
        
        // 在协程中执行检查
        activity.lifecycleScope.launch {
            try {
                // 手动检查更新（忽略时间限制和已忽略版本）
                val versionInfo = UpdateChecker.checkForUpdateManually(
                    context = activity,
                    currentVersionCode = currentVersionCode,
                    okHttpClient = okHttpClient
                )
                
                withContext(Dispatchers.Main) {
                    if (versionInfo != null) {
                        // 有新版本，显示更新对话框
                        showUpdateDialogManual(versionInfo)
                    } else {
                        // 没有新版本
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.update_no_update),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // 检查失败
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.update_check_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                android.util.Log.e("UpdateHandler", "手动检查更新失败", e)
            }
        }
    }
    
    /**
     * 下载并安装 APK
     * 使用系统的 DownloadManager 下载 APK 文件
     * 
     * @param apkUrl APK 下载地址
     * @param versionName 版本名称
     */
    fun downloadAndInstallApk(apkUrl: String, versionName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("NyaLoader 更新")
                .setDescription("正在下载 v$versionName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "NyaLoader_${versionName}.apk"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            
            Toast.makeText(
                activity,
                activity.getString(R.string.update_downloading),
                Toast.LENGTH_LONG
            ).show()
            
            // 监听下载完成，自动打开安装界面
            val onDownloadComplete = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        // 下载完成，安装 APK
                        installApk(downloadManager, downloadId)
                        // 取消注册
                        try {
                            activity.unregisterReceiver(this)
                        } catch (e: Exception) {
                            // 忽略
                        }
                    }
                }
            }
            
            // 注册广播接收器 (Android 8.0+)
            activity.registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
            
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "下载失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("UpdateHandler", "下载 APK 失败", e)
        }
    }
    
    /**
     * 安装 APK
     * 
     * @param downloadManager 下载管理器
     * @param downloadId 下载 ID
     */
    private fun installApk(downloadManager: DownloadManager, downloadId: Long) {
        try {
            val uri = downloadManager.getUriForDownloadedFile(downloadId)
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                // Android 7.0+ 使用 FileProvider
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                activity.startActivity(intent)
                
                Toast.makeText(
                    activity,
                    activity.getString(R.string.update_download_success),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    activity,
                    "下载的文件不存在",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "安装失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("UpdateHandler", "安装 APK 失败", e)
        }
    }
    
    /**
     * 显示手动检查更新的对话框
     */
    private fun showUpdateDialogManual(versionInfo: VersionInfo) {
        // 使用 Android 原生 AlertDialog
        android.app.AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.update_dialog_title))
            .setMessage("${versionInfo.versionName}\n\n${versionInfo.msg}")
            .setPositiveButton(activity.getString(R.string.update_dialog_download_now)) { dialog: DialogInterface, _: Int ->
                downloadAndInstallApk(versionInfo.getBestApkUrl(), versionInfo.versionName)
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(R.string.update_dialog_later)) { dialog: DialogInterface, _: Int ->
                if (!versionInfo.forceUpdate) {
                    UpdateChecker.ignoreVersion(activity, versionInfo.versionCode)
                }
                dialog.dismiss()
            }
            .setCancelable(!versionInfo.forceUpdate)
            .show()
    }
}

