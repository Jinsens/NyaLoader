package com.nyapass.loader.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.nyapass.loader.R
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
    var urlInput by remember { mutableStateOf("https://bing.com") }
    var currentUrl by remember { mutableStateOf(urlInput) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showUrlBar by remember { mutableStateOf(false) }

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
                        
                        IconButton(onClick = { 
                            webView?.loadUrl("https://ys.mihoyo.com/cloud/#/")
                        }) {
                            Icon(Icons.Default.CloudQueue, contentDescription = stringResource(R.string.webview_cloud_game))
                        }
                        
                        IconButton(onClick = { showUrlBar = !showUrlBar }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.webview_edit_url))
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
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    
                    Toast.makeText(
                        context,
                        context.getString(R.string.webview_download_captured, fileName),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 添加到下载管理器
                    viewModel.createDownloadTask(
                        url = url,
                        fileName = fileName,
                        threadCount = 32,
                        saveToPublicDir = true,
                        customPath = null,
                        userAgent = userAgent
                    )
                }
            )
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

                // WebViewClient - 处理页面导航
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

