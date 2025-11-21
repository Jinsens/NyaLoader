/**
 * NyaLoader - 多线程下载器
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
package com.nyapass.loader

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nyapass.loader.data.preferences.AppPreferences
import com.nyapass.loader.data.preferences.Language
import com.nyapass.loader.ui.components.UpdateDialog
import com.nyapass.loader.ui.screen.DownloadScreen
import com.nyapass.loader.ui.screen.LicensesScreen
import com.nyapass.loader.ui.screen.SettingsScreen
import com.nyapass.loader.ui.screen.WebViewScreen
import com.nyapass.loader.ui.theme.getColorScheme
import com.nyapass.loader.util.*
import com.nyapass.loader.viewmodel.DownloadViewModel
import com.nyapass.loader.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // 使用 Hilt 注入依赖
    @Inject
    lateinit var appPreferences: AppPreferences
    
    @Inject
    lateinit var okHttpClient: OkHttpClient
    
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var updateHandler: UpdateHandler
    private lateinit var clipboardHandler: ClipboardHandler
    
    private var selectedFolderPath by mutableStateOf<String?>(null) // 设置界面的持久化路径
    private var tempFolderPath by mutableStateOf<String?>(null) // 创建下载的临时路径
    
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
            
            val rawUri = it.toString()
            selectedFolderPath = rawUri
            
            appPreferences.saveCustomSavePath(rawUri)
            
            Toast.makeText(
                this,
                getString(R.string.directory_saved_toast, PathFormatter.formatForDisplay(this, rawUri) ?: rawUri),
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
            
            val rawUri = it.toString()
            tempFolderPath = rawUri
            
            Toast.makeText(
                this,
                getString(R.string.directory_selected_toast, PathFormatter.formatForDisplay(this, rawUri) ?: rawUri),
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
        
        // 设置沉浸式状态栏和导航栏
        setupEdgeToEdge()
        
        // 初始化Handler
        permissionHandler = PermissionHandler(this)
        updateHandler = UpdateHandler(this)
        clipboardHandler = ClipboardHandler(this)
        
        permissionHandler.initialize()
        
        // 从设置中恢复最近一次处理的剪贴板URL，避免重启应用后重复弹窗
        clipboardHandler.setLastProcessedUrl(appPreferences.getLastClipboardUrl() ?: "")
        
        // 请求权限
        permissionHandler.requestPermissionsIfNeeded()
        
        setContent {
            MainContent()
        }
    }
    
    /**
     * 设置沉浸式状态栏和导航栏
     */
    private fun setupEdgeToEdge() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
    
    @Composable
    private fun MainContent() {
        // 使用 Hilt 注入 ViewModel
        val downloadViewModel: DownloadViewModel = hiltViewModel()
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        
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
                Toast.makeText(this@MainActivity, getString(R.string.language_changed_toast), Toast.LENGTH_SHORT).show()
                kotlinx.coroutines.delay(500)
                recreate()
            }
            previousLanguage = language
        }
        
        // Firebase 设置
        val hasShownFirebaseTip by appPreferences.hasShownFirebaseTip.collectAsStateWithLifecycle()
        var showFirebaseDialog by remember { mutableStateOf(!hasShownFirebaseTip) }
        
        // 更新检测状态
        var updateInfo by remember { mutableStateOf<VersionInfo?>(null) }
        var showUpdateDialog by remember { mutableStateOf(false) }
        
        // 显示 Firebase 首次提醒对话框
        if (showFirebaseDialog) {
            FirebasePermissionDialog(
                onAccept = {
                    appPreferences.saveFirebaseEnabled(true)
                    appPreferences.saveHasShownFirebaseTip(true)
                    showFirebaseDialog = false
                    Toast.makeText(this, "Firebase 已启用，重启应用后生效", Toast.LENGTH_LONG).show()
                },
                onDecline = {
                    appPreferences.saveFirebaseEnabled(false)
                    appPreferences.saveHasShownFirebaseTip(true)
                    showFirebaseDialog = false
                }
            )
        }
        
        // 更新检测（在 Firebase 对话框关闭后）
        LaunchedEffect(showFirebaseDialog) {
            if (!showFirebaseDialog) {
                kotlinx.coroutines.delay(1000)
                
                try {
                    val currentVersionCode = getCurrentVersionCode()
                    updateHandler.checkUpdateAutomatically(
                        currentVersionCode = currentVersionCode,
                        okHttpClient = okHttpClient
                    ) { versionInfo ->
                        updateInfo = versionInfo
                        showUpdateDialog = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "更新检查失败", e)
                }
            }
        }
        
        // 显示更新对话框
        if (showUpdateDialog && updateInfo != null) {
            UpdateDialog(
                versionInfo = updateInfo!!,
                onDismiss = {
                    UpdateChecker.ignoreVersion(this, updateInfo!!.versionCode)
                    showUpdateDialog = false
                    updateInfo = null
                },
                onConfirm = {
                    showUpdateDialog = false
                    updateHandler.downloadAndInstallApk(updateInfo!!.getBestApkUrl(), updateInfo!!.versionName)
                    updateInfo = null
                }
            )
        }
        
        // 当默认保存位置改为自定义且需要选择路径时
        LaunchedEffect(defaultSaveLocation) {
            if (defaultSaveLocation == com.nyapass.loader.data.preferences.SaveLocation.CUSTOM && customSavePath == null) {
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
                    persistentFolderPath = selectedFolderPath,
                    tempFolderPath = tempFolderPath,
                    onSelectPersistentFolder = { settingsFolderPickerLauncher.launch(null) },
                    onSelectTempFolder = { tempFolderPickerLauncher.launch(null) },
                    onResetTempFolder = { tempFolderPath = null },
                    onCheckUpdate = {
                        val currentVersionCode = getCurrentVersionCode()
                        updateHandler.checkUpdateManually(
                            currentVersionCode = currentVersionCode,
                            okHttpClient = okHttpClient
                        )
                    },
                    clipboardHandler = clipboardHandler,
                    appPreferences = appPreferences
                )
            }
        }
    }
    
    /**
     * 获取当前版本号
     */
    private fun getCurrentVersionCode(): Int {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionCode
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
    persistentFolderPath: String?,
    tempFolderPath: String?,
    onSelectPersistentFolder: () -> Unit,
    onSelectTempFolder: () -> Unit,
    onResetTempFolder: () -> Unit,
    onCheckUpdate: () -> Unit,
    clipboardHandler: ClipboardHandler,
    appPreferences: com.nyapass.loader.data.preferences.AppPreferences
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
                clipboardHandler.checkClipboardForUrl()?.let { url ->
                    withContext(Dispatchers.Main) {
                        // 持久化保存
                        app.appPreferences.saveLastClipboardUrl(clipboardHandler.getLastProcessedUrl())
                        // 显示对话框
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
                Toast.makeText(context, context.getString(R.string.clipboard_enabled_toast), Toast.LENGTH_SHORT).show()
            },
            onDisable = {
                app.appPreferences.saveClipboardMonitorEnabled(false)
                app.appPreferences.saveHasShownClipboardTip(true)
                showClipboardTip = false
                Toast.makeText(context, context.getString(R.string.clipboard_can_enable_later), Toast.LENGTH_SHORT).show()
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
            },
            onConfirm = { url, fileName ->
                downloadViewModel.createDownloadTask(
                    url = url,
                    fileName = fileName,
                    threadCount = 32,
                    saveToPublicDir = true,
                    customPath = persistentFolderPath,
                    userAgent = null
                )
                showSimpleDialog = false
                clipboardUrl = ""
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
                onSelectTempFolder = onSelectTempFolder,
                tempFolderPath = tempFolderPath,
                onResetTempFolder = onResetTempFolder,
                onOpenSettings = { navController.navigate("settings") },
                onOpenLicenses = { },
                onOpenWebView = { navController.navigate("webview") }
            )
        }

        composable(
            route = "webview",
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
            WebViewScreen(
                viewModel = downloadViewModel,
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() },
                onSelectFolder = onSelectPersistentFolder,
                onOpenLicenses = { navController.navigate("licenses") },
                onCheckUpdate = onCheckUpdate
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Firebase 权限对话框
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
            Button(onClick = onAccept) {
                Text(stringResource(R.string.firebase_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.firebase_decline))
            }
        }
    )
}
