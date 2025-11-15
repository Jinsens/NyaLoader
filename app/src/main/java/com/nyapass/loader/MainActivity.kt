/**
 * NyaLoader - 多线程下载器
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
package com.nyapass.loader

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import android.content.DialogInterface
import com.nyapass.loader.ui.screen.DownloadScreen
import com.nyapass.loader.ui.screen.LicensesScreen
import com.nyapass.loader.ui.screen.SettingsScreen
import com.nyapass.loader.ui.theme.getColorScheme
import com.nyapass.loader.util.LocaleHelper
import com.nyapass.loader.util.PermissionUtils
import com.nyapass.loader.util.UrlValidator
import com.nyapass.loader.util.UpdateChecker
import com.nyapass.loader.util.VersionInfo
import com.nyapass.loader.ui.components.UpdateDialog
import com.nyapass.loader.viewmodel.DownloadViewModel
import com.nyapass.loader.viewmodel.SettingsViewModel
import com.nyapass.loader.viewmodel.ViewModelFactory
import com.nyapass.loader.data.preferences.Language
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
    
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var selectedFolderPath by mutableStateOf<String?>(null) // 设置界面的持久化路径
    private var tempFolderPath by mutableStateOf<String?>(null) // 创建下载的临时路径
    private var lastProcessedUrl = "" // 记录最后处理的URL（规范化后），避免重复弹窗
    
    // 权限请求启动器
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
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
    private val allFilesAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (PermissionUtils.hasAllFilesAccess()) {
                Toast.makeText(
                    this,
                    "已授予所有文件访问权限",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "需要所有文件访问权限才能下载到自定义目录",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // 设置界面的文件夹选择器（持久化）
    private val settingsFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // 获取持久化权限
            contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            
            // 将Uri转换为路径（用于显示）
            val path = it.path?.replace("/tree/primary:", "/storage/emulated/0/") ?: it.toString()
            selectedFolderPath = path
            
            //  持久化保存到设置
            settingsViewModel.updateCustomSavePath(path)
            
            Toast.makeText(
                this,
                "已保存默认目录: $path",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // 创建下载对话框的文件夹选择器（临时）
    private val tempFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // 获取持久化权限
            contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            
            // 将Uri转换为路径（用于显示）
            val path = it.path?.replace("/tree/primary:", "/storage/emulated/0/") ?: it.toString()
            
            //  仅设置临时路径，不持久化
            tempFolderPath = path
            
            Toast.makeText(
                this,
                "已选择临时目录: $path",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // 在附加基础Context时应用语言设置
        val preferences = com.nyapass.loader.data.preferences.AppPreferences(newBase)
        val language = preferences.language.value
        val context = LocaleHelper.applyLanguage(newBase, language)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置沉浸式状态栏和导航栏 - Edge to Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置状态栏和导航栏为透明（虽然API已废弃，但仍是实现沉浸式的必要方法）
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Android 10+ 关闭导航栏对比度强制，实现更纯粹的沉浸式效果
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        // 启用预测性返回手势动画（Android 13+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // AndroidManifest 中已设置 enableOnBackInvokedCallback="true"
            // Compose 的 BackHandler 和 ModalNavigationDrawer 会自动支持预测性返回
        }
        
        // 初始化ViewModel
        val application = application as LoaderApplication
        // 从设置中恢复最近一次处理的剪贴板URL，避免重启应用后重复弹窗
        lastProcessedUrl = application.appPreferences.getLastClipboardUrl() ?: ""

        val downloadFactory = ViewModelFactory(application.downloadRepository, application.appPreferences)
        downloadViewModel = ViewModelProvider(this, downloadFactory)[DownloadViewModel::class.java]
        settingsViewModel = ViewModelProvider(this, downloadFactory)[SettingsViewModel::class.java]
        
        // 请求权限
        requestPermissionsIfNeeded()
        
        setContent {
            // 收集主题设置
            val themeColor by settingsViewModel.themeColor.collectAsStateWithLifecycle()
            val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle()
            val customColor by settingsViewModel.customColor.collectAsStateWithLifecycle()
            val defaultSaveLocation by settingsViewModel.defaultSaveLocation.collectAsStateWithLifecycle()
            val customSavePath by settingsViewModel.customSavePath.collectAsStateWithLifecycle()
            
            // 监听语言变化
            val language by settingsViewModel.language.collectAsStateWithLifecycle()
            var previousLanguage by remember { mutableStateOf<Language?>(null) }
            
            // 当语言变化时重启Activity
            LaunchedEffect(language) {
                if (previousLanguage != null && previousLanguage != language) {
                    // 语言已更改，重启Activity以应用新语言
                    Toast.makeText(
                        this@MainActivity,
                        "语言已更改，请稍等",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 延迟一下再重启，让Toast显示出来
                    kotlinx.coroutines.delay(500)
                    recreate()
                }
                previousLanguage = language
            }
            
            // Firebase 设置
            val hasShownFirebaseTip by application.appPreferences.hasShownFirebaseTip.collectAsStateWithLifecycle()
            var showFirebaseDialog by remember { mutableStateOf(!hasShownFirebaseTip) }
            
            // 更新检测状态
            var updateInfo by remember { mutableStateOf<VersionInfo?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            
            // 显示 Firebase 首次提醒对话框
            if (showFirebaseDialog) {
                FirebasePermissionDialog(
                    onAccept = {
                        application.appPreferences.saveFirebaseEnabled(true)
                        application.appPreferences.saveHasShownFirebaseTip(true)
                        showFirebaseDialog = false
                        // 提示用户需要重启应用
                        Toast.makeText(
                            this,
                            "Firebase 已启用，重启应用后生效",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onDecline = {
                        application.appPreferences.saveFirebaseEnabled(false)
                        application.appPreferences.saveHasShownFirebaseTip(true)
                        showFirebaseDialog = false
                    }
                )
            }
            
            // 更新检测（在 Firebase 对话框关闭后）
            LaunchedEffect(showFirebaseDialog) {
                if (!showFirebaseDialog) {
                    // 延迟 1 秒后检查更新，避免与 Firebase 对话框冲突
                    kotlinx.coroutines.delay(1000)
                    
                    try {
                        val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(packageName, 0).versionCode
                        }
                        
                        val versionInfo = UpdateChecker.checkForUpdate(
                            context = this@MainActivity,
                            currentVersionCode = currentVersionCode,
                            okHttpClient = application.okHttpClient
                        )
                        
                        if (versionInfo != null) {
                            updateInfo = versionInfo
                            showUpdateDialog = true
                        }
                    } catch (e: Exception) {
                        // 更新检查失败，静默处理
                        android.util.Log.e("MainActivity", "更新检查失败", e)
                    }
                }
            }
            
            // 显示更新对话框
            if (showUpdateDialog && updateInfo != null) {
                UpdateDialog(
                    versionInfo = updateInfo!!,
                    onDismiss = {
                        // 用户选择忽略更新
                        UpdateChecker.ignoreVersion(this@MainActivity, updateInfo!!.versionCode)
                        showUpdateDialog = false
                        updateInfo = null
                    },
                    onConfirm = {
                        // 用户选择更新，自动选择最佳架构的 APK
                        showUpdateDialog = false
                        downloadAndInstallApk(updateInfo!!.getBestApkUrl(), updateInfo!!.versionName)
                        updateInfo = null
                    }
                )
            }
            
            // 当默认保存位置改为自定义且需要选择路径时
            LaunchedEffect(defaultSaveLocation) {
                if (defaultSaveLocation == com.nyapass.loader.data.preferences.SaveLocation.CUSTOM && customSavePath == null) {
                    // 自动打开文件夹选择器（使用设置launcher，会持久化）
                    settingsFolderPickerLauncher.launch(null)
                }
            }
            
            // 更新selectedFolderPath为设置中的自定义路径
            LaunchedEffect(customSavePath) {
                if (customSavePath != null) {
                    selectedFolderPath = customSavePath
                }
            }
            
            // 应用动态主题
            MaterialTheme(
                colorScheme = getColorScheme(themeColor, darkMode, customColor)
            ) {
                // 状态栏和导航栏跟随深色模式
                val view = window.decorView
                val isDark = when (darkMode) {
                    com.nyapass.loader.data.preferences.DarkMode.LIGHT -> false
                    com.nyapass.loader.data.preferences.DarkMode.DARK -> true
                    com.nyapass.loader.data.preferences.DarkMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                
                LaunchedEffect(isDark) {
                    androidx.core.view.WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = !isDark
                    androidx.core.view.WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightNavigationBars = !isDark
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        downloadViewModel = downloadViewModel,
                        settingsViewModel = settingsViewModel,
                        persistentFolderPath = selectedFolderPath,  // 设置界面的持久化路径
                        tempFolderPath = tempFolderPath,  // 创建下载的临时路径
                        onSelectPersistentFolder = {
                            // 设置界面选择目录（持久化）
                            settingsFolderPickerLauncher.launch(null)
                        },
                        onSelectTempFolder = {
                            // 创建下载对话框选择目录（临时）
                            tempFolderPickerLauncher.launch(null)
                        },
                        onResetTempFolder = {
                            // 重置临时路径
                            tempFolderPath = null
                        },
                        onCheckUpdate = {
                            // 手动检查更新
                            checkUpdateManually()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * 请求必要的权限
     */
    private fun requestPermissionsIfNeeded() {
        // 1. 先请求常规权限
        val permissions = PermissionUtils.getRequiredPermissions()
        if (permissions.isNotEmpty()) {
            val needRequest = permissions.any { permission ->
                checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            
            if (needRequest) {
                permissionLauncher.launch(permissions)
                return
            }
        }
        
        // 2. 如果常规权限已授予，检查所有文件访问权限（Android 11+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!PermissionUtils.hasAllFilesAccess()) {
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
                    android.net.Uri.parse("package:$packageName")
                )
                
                // 显示提示
                Toast.makeText(
                    this,
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
                    this,
                    "请在设置中授予所有文件访问权限",
                    Toast.LENGTH_LONG
                ).show()
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                allFilesAccessLauncher.launch(intent)
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
    private fun downloadAndInstallApk(apkUrl: String, versionName: String) {
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
            
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            
            Toast.makeText(
                this,
                getString(R.string.update_downloading),
                Toast.LENGTH_LONG
            ).show()
            
            // 监听下载完成，自动打开安装界面
            val onDownloadComplete = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        // 下载完成，安装 APK
                        installApk(downloadManager, downloadId)
                        // 取消注册
                        try {
                            unregisterReceiver(this)
                        } catch (e: Exception) {
                            // 忽略
                        }
                    }
                }
            }
            
            // 注册广播接收器
            registerReceiver(
                onDownloadComplete,
                android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
            
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "下载失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("MainActivity", "下载 APK 失败", e)
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
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    // Android 7.0+ 需要使用 FileProvider
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                } else {
                    @Suppress("DEPRECATION")
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                startActivity(intent)
                
                Toast.makeText(
                    this,
                    getString(R.string.update_download_success),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "下载的文件不存在",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "安装失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("MainActivity", "安装 APK 失败", e)
        }
    }
    
    /**
     * 手动检查更新
     * 用于用户主动点击"检查更新"按钮
     */
    private fun checkUpdateManually() {
        val application = application as LoaderApplication
        
        // 显示检查中的提示
        Toast.makeText(
            this,
            getString(R.string.update_checking),
            Toast.LENGTH_SHORT
        ).show()
        
        // 在协程中执行检查
        lifecycleScope.launch {
            try {
                val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0).versionCode
                }
                
                // 手动检查更新（忽略时间限制和已忽略版本）
                val versionInfo = UpdateChecker.checkForUpdateManually(
                    context = this@MainActivity,
                    currentVersionCode = currentVersionCode,
                    okHttpClient = application.okHttpClient
                )
                
                withContext(Dispatchers.Main) {
                    if (versionInfo != null) {
                        // 有新版本，显示更新对话框
                        showUpdateDialogManual(versionInfo)
                    } else {
                        // 没有新版本
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.update_no_update),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // 检查失败
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.update_check_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                android.util.Log.e("MainActivity", "手动检查更新失败", e)
            }
        }
    }
    
    /**
     * 显示手动检查更新的对话框
     * 注意：这需要在 setContent 外部处理，所以使用传统的 Dialog 方式
     */
    private fun showUpdateDialogManual(versionInfo: VersionInfo) {
        // 使用 Android 原生 AlertDialog
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_dialog_title))
            .setMessage("${versionInfo.versionName}\n\n${versionInfo.msg}")
            .setPositiveButton(getString(R.string.update_dialog_download_now)) { dialog: DialogInterface, _: Int ->
                downloadAndInstallApk(versionInfo.getBestApkUrl(), versionInfo.versionName)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.update_dialog_later)) { dialog: DialogInterface, _: Int ->
                if (!versionInfo.forceUpdate) {
                    UpdateChecker.ignoreVersion(this, versionInfo.versionCode)
                }
                dialog.dismiss()
            }
            .setCancelable(!versionInfo.forceUpdate)
            .show()
    }
    
    /**
     * 检查剪贴板中的下载链接
     * 每次进入前台都会检查一次
     * 返回URL字符串，如果没有有效链接返回null
     */
    fun checkClipboardForUrl(): String? {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboardManager?.primaryClip
        
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString() ?: ""
            
            if (text.isNotBlank()) {
                // 尝试从文本中提取URL
                val url = extractUrlFromText(text) ?: return null
                
                if (UrlValidator.isValidUrlFormat(url)) {
                    // 使用规范化后的URL做去重比较
                    val normalized = normalizeUrlForCompare(url)
                    
                    // 检查是否与上次处理的URL相同，避免重复弹窗
                    if (normalized.isNotEmpty() && normalized != lastProcessedUrl) {
                        lastProcessedUrl = normalized
                        // 持久化保存，避免应用重启后重复弹窗
                        (application as? LoaderApplication)
                            ?.appPreferences
                            ?.saveLastClipboardUrl(normalized)
                        return url
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * 从文本中提取URL（增强版）
     * - 支持一段文本中包含多个URL
     * - 去掉中文/英文标点等尾部字符
     * - 优先返回看起来像真实资源文件的链接（带常见扩展名）
     */
    private fun extractUrlFromText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        
        // 使用正则提取文本中的所有 http/https 链接
        val regex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(trimmed).map { it.value }.toList()
        if (matches.isEmpty()) return null
        
        // 清理每个候选URL尾部的标点符号
        val cleanedCandidates = matches.map { raw ->
            raw.trim().trimEnd(')', ']', '}', '>', '，', '。', '！', '；', ';', ',', '、', '"', '\'')
        }.filter { it.isNotBlank() }
        
        if (cleanedCandidates.isEmpty()) return null
        
        // 优先选择看起来像真实资源文件的URL（带常见扩展名）
        val exts = listOf(
            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz",
            ".apk", ".exe", ".iso",
            ".mp4", ".mkv", ".avi", ".mov",
            ".mp3", ".flac", ".wav",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
        )
        
        val withExt = cleanedCandidates.firstOrNull { url ->
            val lower = url.lowercase()
            val path = lower.substringBefore('?').substringBefore('#')
            exts.any { ext -> path.endsWith(ext) }
        }
        
        return withExt ?: cleanedCandidates.first()
    }

    /**
     * 将URL规范化用于去重比较：去掉结尾的斜杠、忽略大小写和fragment
     */
    private fun normalizeUrlForCompare(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: return url.trim()
            val host = uri.host?.lowercase() ?: return url.trim()
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val path = (uri.path ?: "").trimEnd('/')
            val query = uri.query?.let { "?$it" } ?: ""
            "$scheme://$host$port$path$query"
        } catch (e: Exception) {
            url.trim()
        }
    }
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation(
    downloadViewModel: DownloadViewModel,
    settingsViewModel: SettingsViewModel,
    persistentFolderPath: String?,  // 设置界面的持久化路径
    tempFolderPath: String?,  // 创建下载的临时路径
    onSelectPersistentFolder: () -> Unit,  // 设置界面选择目录（持久化）
    onSelectTempFolder: () -> Unit,  // 创建下载对话框选择目录（临时）
    onResetTempFolder: () -> Unit,  // 重置临时路径
    onCheckUpdate: () -> Unit  // 手动检查更新
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? MainActivity
    val app = context.applicationContext as LoaderApplication
    
    // 收集剪贴板监听设置
    val clipboardEnabled by app.appPreferences.clipboardMonitorEnabled.collectAsStateWithLifecycle()
    val hasShownTip by app.appPreferences.hasShownClipboardTip.collectAsStateWithLifecycle()
    
    // 对话框状态
    var showSimpleDialog by remember { mutableStateOf(false) }
    var showClipboardTip by remember { mutableStateOf(false) }
    var clipboardUrl by remember { mutableStateOf("") }
    
    // 监听生命周期，当应用回到前台时检查剪贴板
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    
    // 使用 LaunchedEffect 立即响应生命周期变化
    LaunchedEffect(lifecycleState, hasShownTip, clipboardEnabled) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            // 首次使用时显示提示
            if (!hasShownTip) {
                showClipboardTip = true
                return@LaunchedEffect
            }
            
            // 如果未启用剪贴板监听，跳过
            if (!clipboardEnabled) {
                return@LaunchedEffect
            }
            
            // 检查剪贴板（在IO线程执行）
            withContext(Dispatchers.IO) {
                activity?.checkClipboardForUrl()?.let { url ->
                    withContext(Dispatchers.Main) {
                        // 直接显示对话框
                        clipboardUrl = url
                        showSimpleDialog = true
                    }
                }
            }
        }
    }
    
    // 首次使用提示对话框
    if (showClipboardTip) {
        com.nyapass.loader.ui.components.ClipboardTipDialog(
            onDismiss = {
                showClipboardTip = false
                app.appPreferences.saveHasShownClipboardTip(true)
            },
            onEnable = {
                app.appPreferences.saveClipboardMonitorEnabled(true)
                app.appPreferences.saveHasShownClipboardTip(true)
                showClipboardTip = false
                Toast.makeText(context, "已启用智能剪贴板监听", Toast.LENGTH_SHORT).show()
            },
            onDisable = {
                app.appPreferences.saveClipboardMonitorEnabled(false)
                app.appPreferences.saveHasShownClipboardTip(true)
                showClipboardTip = false
                Toast.makeText(context, "可在设置中随时开启", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // 简化下载对话框
    if (showSimpleDialog) {
        com.nyapass.loader.ui.components.SimpleDownloadDialog(
            initialUrl = clipboardUrl,
            onDismiss = { 
                showSimpleDialog = false
                clipboardUrl = ""
                // 不重置lastProcessedUrl，避免相同链接再次弹出
            },
            onConfirm = { url, fileName ->
                // 创建下载任务（使用设置中的持久化路径）
                downloadViewModel.createDownloadTask(
                    url = url,
                    fileName = fileName,
                    threadCount = 32, // 使用默认值
                    saveToPublicDir = true,
                    customPath = persistentFolderPath,  // 使用持久化路径
                    userAgent = null
                )
                
                showSimpleDialog = false
                clipboardUrl = ""
                // 下载开始后不重置，避免重复添加
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = "download"
    ) {
        composable(
            route = "download",
            popEnterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            }
        ) {
            DownloadScreen(
                viewModel = downloadViewModel,
                onSelectTempFolder = onSelectTempFolder,  // 临时目录选择
                tempFolderPath = tempFolderPath,  // 临时目录路径
                onResetTempFolder = onResetTempFolder,  // 重置临时路径
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenLicenses = {
                    // 当前设计下，关于与许可只从设置入口进入，这里保持空实现
                }
            )
        }

        composable(
            route = "settings",
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            exitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            popEnterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            }
        ) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectFolder = onSelectPersistentFolder,  // 持久化目录选择
                onOpenLicenses = {
                    navController.navigate("licenses")
                },
                onCheckUpdate = onCheckUpdate  // 手动检查更新
            )
        }

        composable(
            route = "licenses",
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            exitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            popEnterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            }
        ) {
            LicensesScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Firebase 权限对话框
 * 首次启动应用时询问用户是否开启 Firebase 分析
 */
@Composable
fun FirebasePermissionDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "Firebase",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.firebase_permission_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.firebase_permission_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.firebase_permission_features),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.firebase_permission_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept
            ) {
                Text(stringResource(R.string.firebase_accept))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline
            ) {
                Text(stringResource(R.string.firebase_decline))
            }
        }
    )
}

