package com.nyapass.loader.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyapass.loader.R
import com.nyapass.loader.ui.components.webview.UserAgentDialog
import com.nyapass.loader.viewmodel.DownloadViewModel

/**
 * WebView 屏幕
 * 支持自动捕获下载请求并添加到下载管理器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    viewModel: DownloadViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.nyapass.loader.LoaderApplication

    // 收集用户配置的默认线程数
    val defaultThreadCount by app.appPreferences.defaultThreadCount.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("https://bing.com") }
    var currentUrl by remember { mutableStateOf(urlInput) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showUrlBar by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showUADialog by remember { mutableStateOf(false) }
    var currentUserAgent by remember { 
        mutableStateOf("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
    }
    var customUserAgent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.webview_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webView?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.webview_back)
                            )
                        }
                        
                        IconButton(
                            onClick = { webView?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.webview_forward)
                            )
                        }
                        
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.webview_refresh))
                        }
                        
                        IconButton(onClick = { showUrlBar = !showUrlBar }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.webview_edit_url))
                        }
                        
                        // 三点菜单
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                // 云游戏
                                DropdownMenuItem(
                                    text = { Text("☁️ 原神云游戏") },
                                    onClick = {
                                        webView?.loadUrl("https://ys.mihoyo.com/cloud/#/")
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.CloudQueue, contentDescription = null)
                                    }
                                )
                                
                                HorizontalDivider()
                                
                                // UA设置
                                DropdownMenuItem(
                                    text = { Text("User-Agent 设置") },
                                    onClick = {
                                        showUADialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                                    }
                                )
                                
                                // 桌面模式
                                DropdownMenuItem(
                                    text = { Text("切换桌面模式") },
                                    onClick = {
                                        webView?.settings?.apply {
                                            if (userAgentString?.contains("Mobile") == true) {
                                                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                                currentUserAgent = userAgentString ?: ""
                                                Toast.makeText(context, "已切换到桌面模式", Toast.LENGTH_SHORT).show()
                                            } else {
                                                userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                                currentUserAgent = userAgentString ?: ""
                                                Toast.makeText(context, "已切换到移动模式", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        webView?.reload()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Computer, contentDescription = null)
                                    }
                                )
                                
                                HorizontalDivider()
                                
                                // 清除缓存
                                DropdownMenuItem(
                                    text = { Text("清除缓存") },
                                    onClick = {
                                        webView?.clearCache(true)
                                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                                        Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
                                
                                // 清除历史
                                DropdownMenuItem(
                                    text = { Text("清除历史记录") },
                                    onClick = {
                                        webView?.clearHistory()
                                        Toast.makeText(context, "历史记录已清除", Toast.LENGTH_SHORT).show()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.History, contentDescription = null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // 进度条
                if (isLoading && loadProgress < 100) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // URL 输入栏
                if (showUrlBar) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.webview_url_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        var url = urlInput.trim()
                                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                            url = "https://$url"
                                        }
                                        webView?.loadUrl(url)
                                        showUrlBar = false
                                    }
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = {
                                    var url = urlInput.trim()
                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                        url = "https://$url"
                                    }
                                    webView?.loadUrl(url)
                                    showUrlBar = false
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.webview_go))
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // 当前 URL 显示
                if (!showUrlBar) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentUrl.startsWith("https://")) 
                                    Icons.Default.Lock 
                                else 
                                    Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            WebViewContent(
                initialUrl = urlInput,
                onWebViewCreated = { webView = it },
                onUrlChanged = { 
                    currentUrl = it
                    urlInput = it
                },
                onCanGoBackChanged = { canGoBack = it },
                onCanGoForwardChanged = { canGoForward = it },
                onLoadingChanged = { isLoading = it },
                onProgressChanged = { loadProgress = it },
                onDownloadStart = { url, userAgent, contentDisposition, mimeType, contentLength ->
                    // 自动捕获下载请求
                    val fileName = com.nyapass.loader.util.FileNameExtractor.extractFileName(
                        url = url,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType
                    )
                    val lowerFileName = fileName.lowercase()
                    val lowerUrl = url.lowercase()
                    
                    // 过滤掉页面文件和授权页面
                    val isPageFile = lowerFileName.endsWith(".aspx") ||
                                     lowerFileName.endsWith(".html") ||
                                     lowerFileName.endsWith(".htm") ||
                                     lowerFileName.endsWith(".php") ||
                                     lowerFileName.endsWith(".jsp") ||
                                     lowerFileName.endsWith(".asp")
                    
                    // 过滤掉SharePoint/OneDrive的授权链接
                    val isAuthUrl = lowerUrl.contains("sharepoint.com") && lowerUrl.contains("download.aspx") ||
                                   lowerUrl.contains("onedrive.live.com") && lowerUrl.contains("download") ||
                                   lowerUrl.contains("/_layouts/") ||
                                   lowerUrl.contains("/authorize")
                    
                    // 过滤掉小文件（授权页面通常很小，< 100KB）
                    val isTooSmall = contentLength > 0 && contentLength < 100 * 1024 && isPageFile
                    
                    // 只有不是页面文件且不是授权URL才下载
                    if (!isPageFile && !isAuthUrl && !isTooSmall) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.webview_download_captured, fileName),
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // 获取Cookie
                        val cookie = try {
                            android.webkit.CookieManager.getInstance().getCookie(url)
                        } catch (e: Exception) {
                            null
                        }
                        
                        // 添加到下载管理器，传递Cookie和Referer
                        viewModel.createDownloadTask(
                            url = url,
                            fileName = fileName,
                            threadCount = defaultThreadCount,
                            saveToPublicDir = true,
                            customPath = null,
                            userAgent = userAgent,
                            cookie = cookie,
                            referer = currentUrl  // 使用当前页面URL作为Referer
                        )
                    } else {
                        // 显示调试信息
                        android.util.Log.d("WebView", "已忽略下载: $fileName (${contentLength} bytes)")
                        android.util.Log.d("WebView", "原因: isPageFile=$isPageFile, isAuthUrl=$isAuthUrl, isTooSmall=$isTooSmall")
                    }
                }
            )
            
            // User-Agent 设置对话框
            if (showUADialog) {
                UserAgentDialog(
                    currentUA = currentUserAgent,
                    customUA = customUserAgent,
                    onDismiss = { showUADialog = false },
                    onConfirm = { newUA ->
                        currentUserAgent = newUA
                        customUserAgent = newUA
                        webView?.settings?.userAgentString = newUA
                        webView?.reload()
                        Toast.makeText(context, "User-Agent 已更新", Toast.LENGTH_SHORT).show()
                        showUADialog = false
                    }
                )
            }
        }
    }
}

/**
 * WebView 内容组件
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewContent(
    initialUrl: String,
    onWebViewCreated: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onCanGoForwardChanged: (Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onDownloadStart: (url: String, userAgent: String?, contentDisposition: String?, mimeType: String?, contentLength: Long) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // WebView 设置
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                // WebViewClient - 处理页面导航和下载拦截
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onUrlChanged(it) }
                        onLoadingChanged(true)
                        onProgressChanged(0)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { onUrlChanged(it) }
                        onLoadingChanged(false)
                        onProgressChanged(100)
                        view?.let {
                            onCanGoBackChanged(it.canGoBack())
                            onCanGoForwardChanged(it.canGoForward())
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }

                    // 增强的下载拦截机制 - 捕获所有HTTP请求并分析是否为下载
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        request?.let { req ->
                            val url = req.url.toString()
                            val headers = req.requestHeaders
                            
                            // 检查URL是否包含下载相关的参数或路径
                            if (isDownloadUrl(url)) {
                                // 发起HEAD请求来获取响应头信息
                                try {
                                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                    connection.requestMethod = "HEAD"
                                    connection.connectTimeout = 5000
                                    connection.readTimeout = 5000
                                    
                                    // 添加请求头
                                    headers?.forEach { (key, value) ->
                                        connection.setRequestProperty(key, value)
                                    }
                                    connection.setRequestProperty("User-Agent", settings.userAgentString)
                                    
                                    connection.connect()
                                    
                                    val contentType = connection.contentType
                                    val contentDisposition = connection.getHeaderField("Content-Disposition")
                                    val contentLength = connection.contentLength.toLong()
                                    
                                    // 判断是否为下载文件
                                    if (shouldTriggerDownload(url, contentType, contentDisposition)) {
                                        // 触发下载回调
                                        view?.post {
                                            onDownloadStart(
                                                url,
                                                settings.userAgentString,
                                                contentDisposition,
                                                contentType,
                                                contentLength
                                            )
                                        }
                                        
                                        connection.disconnect()
                                        // 返回空响应以阻止WebView加载
                                        return WebResourceResponse("text/html", "UTF-8", null)
                                    }
                                    
                                    connection.disconnect()
                                } catch (e: Exception) {
                                    // 网络请求失败，忽略错误，让WebView正常处理
                                    e.printStackTrace()
                                }
                            }
                        }
                        
                        return super.shouldInterceptRequest(view, request)
                    }
                    
                    // 判断URL是否为下载链接
                    private fun isDownloadUrl(url: String): Boolean {
                        val uri = try { Uri.parse(url) } catch (e: Exception) { null }
                        val path = uri?.path?.lowercase() ?: ""
                        
                        // 只检查明显的文件扩展名
                        // 不检查云存储链接，让WebView正常处理它们的授权流程
                        val downloadExtensions = listOf(
                            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2",
                            ".apk", ".exe", ".dmg", ".pkg", ".deb", ".rpm",
                            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
                            ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv",
                            ".mp3", ".wav", ".flac", ".aac", ".ogg",
                            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg", ".webp",
                            ".iso", ".img", ".bin", ".dat",
                            ".torrent"
                        )
                        
                        return downloadExtensions.any { ext -> 
                            path.endsWith(ext)
                        }
                    }
                    
                    // 判断响应是否应该触发下载
                    private fun shouldTriggerDownload(
                        url: String, 
                        contentType: String?, 
                        contentDisposition: String?
                    ): Boolean {
                        // 检查Content-Disposition是否包含attachment
                        if (contentDisposition != null && 
                            contentDisposition.lowercase().contains("attachment")) {
                            return true
                        }
                        
                        // 检查Content-Type是否为下载类型
                        if (contentType != null) {
                            val lowerContentType = contentType.lowercase()
                            val downloadMimeTypes = listOf(
                                "application/octet-stream",
                                "application/zip",
                                "application/x-zip",
                                "application/x-rar",
                                "application/x-7z-compressed",
                                "application/x-tar",
                                "application/gzip",
                                "application/vnd.android.package-archive",
                                "application/x-msdownload",
                                "application/x-apple-diskimage",
                                "application/vnd.debian.binary-package"
                            )
                            
                            if (downloadMimeTypes.any { lowerContentType.contains(it) }) {
                                return true
                            }
                        }
                        
                        // 如果URL明确包含download参数，也触发下载
                        if (url.lowercase().contains("download")) {
                            return true
                        }
                        
                        return false
                    }
                }

                // WebChromeClient - 处理进度和对话框
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                        if (newProgress == 100) {
                            onLoadingChanged(false)
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        // 可以在这里更新标题
                    }
                }

                // DownloadListener - 捕获下载请求
                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
                }

                // 加载初始 URL
                loadUrl(initialUrl)
                
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            // WebView 更新时的操作（如果需要）
        }
    )
}

