/**
 * NyaLoader - 多线程下载器
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
package com.nyapass.loader

import android.content.ClipboardManager
import android.content.Context
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nyapass.loader.ui.screen.DownloadScreen
import com.nyapass.loader.ui.screen.LicensesScreen
import com.nyapass.loader.ui.screen.SettingsScreen
import com.nyapass.loader.ui.theme.getColorScheme
import com.nyapass.loader.util.PermissionUtils
import com.nyapass.loader.util.UrlValidator
import com.nyapass.loader.viewmodel.DownloadViewModel
import com.nyapass.loader.viewmodel.SettingsViewModel
import com.nyapass.loader.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var selectedFolderPath by mutableStateOf<String?>(null) // 设置界面的持久化路径
    private var tempFolderPath by mutableStateOf<String?>(null) // 创建下载的临时路径
    private var lastProcessedUrl = "" // 记录最后处理的URL，避免重复弹窗
    
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
            
            // ✅ 持久化保存到设置
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
            
            // ⚠️ 仅设置临时路径，不持久化
            tempFolderPath = path
            
            Toast.makeText(
                this,
                "已选择临时目录: $path",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化ViewModel
        val application = application as LoaderApplication
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
            
            // Firebase 设置
            val hasShownFirebaseTip by application.appPreferences.hasShownFirebaseTip.collectAsStateWithLifecycle()
            var showFirebaseDialog by remember { mutableStateOf(!hasShownFirebaseTip) }
            
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
                val url = extractUrlFromText(text)
                if (url != null && UrlValidator.isValidUrlFormat(url)) {
                    // 检查是否与上次处理的URL相同，避免重复弹窗
                    if (url != lastProcessedUrl) {
                        lastProcessedUrl = url
                        return url
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * 重置已处理的URL（用于对话框关闭后）
     */
    fun resetProcessedUrl() {
        lastProcessedUrl = ""
    }
    
    /**
     * 从文本中提取URL（优化版）
     * 支持复杂的URL（包含中文、特殊字符、URL编码等）
     */
    private fun extractUrlFromText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        
        // 快速路径：如果以http开头，直接处理
        val startsWithHttp = trimmed.startsWith("http://", ignoreCase = true)
        val startsWithHttps = trimmed.startsWith("https://", ignoreCase = true)
        
        if (startsWithHttp || startsWithHttps) {
            // 找到第一个空白字符的位置
            var endIndex = trimmed.length
            for (i in trimmed.indices) {
                val c = trimmed[i]
                if (c.isWhitespace() || c == '\n' || c == '\r') {
                    endIndex = i
                    break
                }
            }
            return trimmed.substring(0, endIndex)
        }
        
        // 从文本中查找URL（使用indexOf代替正则表达式，更快）
        val httpIndex = trimmed.indexOf("http://", ignoreCase = true)
        val httpsIndex = trimmed.indexOf("https://", ignoreCase = true)
        
        val startIndex = when {
            httpIndex >= 0 && httpsIndex >= 0 -> minOf(httpIndex, httpsIndex)
            httpIndex >= 0 -> httpIndex
            httpsIndex >= 0 -> httpsIndex
            else -> return null
        }
        
        // 从找到的位置开始提取URL
        var endIndex = trimmed.length
        for (i in startIndex until trimmed.length) {
            val c = trimmed[i]
            if (c.isWhitespace() || c == '\n' || c == '\r') {
                endIndex = i
                break
            }
        }
        
        return trimmed.substring(startIndex, endIndex)
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
    onResetTempFolder: () -> Unit  // 重置临时路径
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
                // 对话框关闭后重置，允许下次再弹
                activity?.resetProcessedUrl()
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
    
    NavHost(navController = navController, startDestination = "download") {
        composable("download") {
            DownloadScreen(
                viewModel = downloadViewModel,
                onSelectTempFolder = onSelectTempFolder,  // 临时目录选择
                tempFolderPath = tempFolderPath,  // 临时目录路径
                onResetTempFolder = onResetTempFolder,  // 重置临时路径
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenLicenses = {
                    navController.navigate("licenses")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectFolder = onSelectPersistentFolder,  // 持久化目录选择
                onOpenLicenses = {
                    navController.navigate("licenses")
                }
            )
        }
        
        composable("licenses") {
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
                text = "数据分析服务",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "我们使用 Google Firebase 来收集应用使用数据，帮助我们：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• 了解应用崩溃情况并修复问题\n• 分析用户行为以改进功能\n• 提供更好的用户体验",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "您可以随时在设置中更改此选项。",
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
                Text("同意并开启")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline
            ) {
                Text("暂不开启")
            }
        }
    )
}

