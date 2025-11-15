/**
 * NyaLoader - 多线程下载器
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
package com.nyapass.loader

import android.content.Context
import android.content.Intent
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
import com.nyapass.loader.ui.components.FirebasePermissionDialog
import com.nyapass.loader.ui.components.UpdateDialog
import com.nyapass.loader.ui.screen.DownloadScreen
import com.nyapass.loader.ui.screen.LicensesScreen
import com.nyapass.loader.ui.screen.SettingsScreen
import com.nyapass.loader.ui.screen.StatisticsScreen
import com.nyapass.loader.ui.screen.WebViewScreen
import com.nyapass.loader.viewmodel.StatisticsState
import com.nyapass.loader.ui.theme.getColorScheme
import com.nyapass.loader.util.*
import com.nyapass.loader.viewmodel.DownloadViewModel
import com.nyapass.loader.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Job
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
    private var intentUrl by mutableStateOf<String?>(null) // 从外部 Intent 接收的下载链接
    
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

        // 处理外部 Intent（系统下载器功能）
        handleIntent(intent)

        setContent {
            MainContent()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理新的 Intent（应用已在前台时）
        handleIntent(intent)
    }

    /**
     * 处理外部 Intent，提取下载链接
     */
    private fun handleIntent(intent: Intent?) {
        intent ?: return

        val url: String? = when (intent.action) {
            Intent.ACTION_VIEW -> {
                // 从浏览器或其他应用打开的链接
                intent.dataString
            }
            Intent.ACTION_SEND -> {
                // 通过分享功能发送的文本
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        // 提取文本中的 URL
                        extractUrlFromText(text)
                    }
                } else null
            }
            else -> null
        }

        if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
            intentUrl = url
            android.util.Log.i("MainActivity", "从 Intent 接收到下载链接: $url")
        }
    }

    /**
     * 从文本中提取 URL
     */
    private fun extractUrlFromText(text: String): String? {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value
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
        // 添加标志避免应用启动时自动触发
        var hasCheckedInitialPath by remember { mutableStateOf(false) }
        LaunchedEffect(defaultSaveLocation, customSavePath) {
            // 只有在用户主动切换到自定义位置且路径为空时才提示选择
            // 忽略应用启动时的首次检查
            if (hasCheckedInitialPath &&
                defaultSaveLocation == com.nyapass.loader.data.preferences.SaveLocation.CUSTOM &&
                customSavePath == null) {
                settingsFolderPickerLauncher.launch(null)
            }
            hasCheckedInitialPath = true
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
                    intentUrl = intentUrl,
                    onIntentUrlConsumed = { intentUrl = null },
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
    intentUrl: String?,
    onIntentUrlConsumed: () -> Unit,
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

    // 使用受管理的协程作用域，自动处理生命周期
    val coroutineScope = rememberCoroutineScope()

    // 收集剪贴板监听设置
    val clipboardEnabled by app.appPreferences.clipboardMonitorEnabled.collectAsStateWithLifecycle()
    val hasShownTip by app.appPreferences.hasShownClipboardTip.collectAsStateWithLifecycle()

    // 收集用户配置的默认线程数
    val defaultThreadCount by app.appPreferences.defaultThreadCount.collectAsStateWithLifecycle()

    // 对话框状态
    var showSimpleDialog by remember { mutableStateOf(false) }
    var showClipboardTip by remember { mutableStateOf(false) }
    var clipboardUrl by remember { mutableStateOf("") }

    // 处理外部 Intent 传入的 URL（系统下载器功能）
    LaunchedEffect(intentUrl) {
        if (!intentUrl.isNullOrBlank()) {
            clipboardUrl = intentUrl
            showSimpleDialog = true
            onIntentUrlConsumed()
        }
    }

    // 使用 rememberUpdatedState 捕获最新状态，避免闭包问题
    val currentHasShownTip by rememberUpdatedState(hasShownTip)
    val currentClipboardEnabled by rememberUpdatedState(clipboardEnabled)

    // 监听生命周期，当应用回到前台时检查剪贴板
    val lifecycleOwner = LocalLifecycleOwner.current

    // 使用 DisposableEffect 监听生命周期事件，只依赖 lifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            // 当生命周期变为 RESUMED（从后台切换到前台）时检查剪贴板
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // 首次使用时显示提示（使用 currentHasShownTip 获取最新值）
                if (!currentHasShownTip) {
                    showClipboardTip = true
                    return@LifecycleEventObserver
                }

                // 如果未启用剪贴板监听，跳过（使用 currentClipboardEnabled 获取最新值）
                if (!currentClipboardEnabled) {
                    return@LifecycleEventObserver
                }

                // 使用受管理的协程作用域执行剪贴板检测
                coroutineScope.launch(Dispatchers.IO) {
                    // 检查协程是否已被取消
                    if (currentCoroutineContext()[Job]?.isActive == false) return@launch

                    // 检查剪贴板（智能检测）
                    val result = clipboardHandler.checkClipboardForUrl()

                    // 再次检查协程状态
                    if (currentCoroutineContext()[Job]?.isActive == false) return@launch

                    when (result) {
                        is com.nyapass.loader.util.ClipboardCheckResult.DirectDownload -> {
                            // 有明显文件扩展名，直接弹窗
                            withContext(Dispatchers.Main) {
                                app.appPreferences.saveLastClipboardUrl(clipboardHandler.getLastProcessedUrl())
                                clipboardUrl = result.url
                                showSimpleDialog = true
                            }
                        }

                        is com.nyapass.loader.util.ClipboardCheckResult.NeedsValidation -> {
                            // 检查协程状态后再进行网络请求
                            if (currentCoroutineContext()[Job]?.isActive == false) return@launch

                            // 复杂参数链接，需要验证
                            val isDownload = clipboardHandler.validateDownloadUrl(result.url)

                            // 网络请求后再次检查协程状态
                            if (currentCoroutineContext()[Job]?.isActive == false) return@launch

                            if (isDownload) {
                                withContext(Dispatchers.Main) {
                                    app.appPreferences.saveLastClipboardUrl(clipboardHandler.getLastProcessedUrl())
                                    clipboardUrl = result.url
                                    showSimpleDialog = true
                                }
                            }
                            // 如果验证失败，不弹窗
                        }

                        is com.nyapass.loader.util.ClipboardCheckResult.NoUrl,
                        is com.nyapass.loader.util.ClipboardCheckResult.AlreadyProcessed -> {
                            // 没有URL或已处理过，不做任何操作
                        }
                    }
                }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                    threadCount = defaultThreadCount,
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
                onOpenWebView = { navController.navigate("webview") },
                onOpenStatistics = { navController.navigate("statistics") }
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

        composable(
            route = "statistics",
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
            val statisticsState by downloadViewModel.statisticsState.collectAsState()

            LaunchedEffect(Unit) {
                downloadViewModel.loadStatistics()
            }

            StatisticsScreen(
                statistics = when (val state = statisticsState) {
                    is StatisticsState.Success -> state.data
                    else -> null
                },
                isLoading = statisticsState is StatisticsState.Loading,
                onBackClick = { navController.popBackStack() },
                onClearStats = { downloadViewModel.clearAllStats() }
            )
        }
    }
}
