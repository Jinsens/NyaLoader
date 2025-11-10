package com.nyapass.loader.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nyapass.loader.ui.components.AddDownloadDialog
import com.nyapass.loader.ui.components.DownloadTaskItem
import com.nyapass.loader.ui.components.formatFileSize
import com.nyapass.loader.ui.components.formatSpeed
import com.nyapass.loader.viewmodel.DownloadViewModel
import com.nyapass.loader.viewmodel.TaskFilter
import com.nyapass.loader.viewmodel.TotalProgress
import kotlinx.coroutines.launch

/**
 * 主下载界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    onSelectTempFolder: (() -> Unit)? = null,  // 临时目录选择（仅用于当前下载）
    tempFolderPath: String? = null,  // 临时目录路径
    onResetTempFolder: (() -> Unit)? = null,  // 重置临时路径
    onOpenSettings: () -> Unit = {},
    onOpenLicenses: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    val tasks by viewModel.filteredTasks.collectAsState()
    val totalProgress by viewModel.totalProgress.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 记录滚动状态
    val listState = rememberLazyListState()
    
    // 根据滚动状态决定FAB是否展开
    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                currentFilter = uiState.filter,
                onFilterSelected = { filter ->
                    viewModel.updateFilter(filter)
                    scope.launch { drawerState.close() }
                },
                onClearCompleted = {
                    viewModel.clearCompletedTasks()
                    scope.launch { drawerState.close() }
                },
                onClearFailed = {
                    viewModel.clearFailedTasks()
                    scope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                onOpenLicenses = {
                    scope.launch { drawerState.close() }
                    onOpenLicenses()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (uiState.isSearching) {
                    SearchTopBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onCloseSearch = { viewModel.stopSearch() }
                    )
                } else {
                    TopAppBar(
                        title = { Text("NyaLoader") },
                        navigationIcon = {
                            IconButton(onClick = { 
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        },
                        actions = {
                            // 搜索按钮
                            IconButton(onClick = { viewModel.startSearch() }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            },
            floatingActionButton = {
                // 根据滚动状态展开或收起FAB，显示总进度条时向上偏移
                Box(
                    modifier = Modifier.padding(
                        bottom = if (totalProgress.showProgress) 110.dp else 0.dp
                    )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.showAddDialog() },
                        expanded = expandedFab,
                        icon = { Icon(Icons.Default.Add, contentDescription = "新建下载") },
                        text = { Text("新建下载") }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 主要内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (tasks.isEmpty()) {
                        // 空状态
                        if (uiState.isSearching && uiState.searchQuery.isNotEmpty()) {
                            // 搜索无结果
                            SearchEmptyState(
                                modifier = Modifier.align(Alignment.Center),
                                searchQuery = uiState.searchQuery
                            )
                        } else {
                            // 正常空状态
                            EmptyState(
                                modifier = Modifier.align(Alignment.Center),
                                filter = uiState.filter
                            )
                        }
                    } else {
                        // 任务列表
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = tasks,
                                key = { it.task.id }
                            ) { taskWithProgress ->
                                DownloadTaskItem(
                                    taskWithProgress = taskWithProgress,
                                    onStart = { viewModel.startDownload(it) },
                                    onPause = { viewModel.pauseDownload(it) },
                                    onResume = { viewModel.resumeDownload(it) },
                                    onDelete = { viewModel.deleteTask(it) },
                                    onRetry = { viewModel.retryTask(it) },
                                    onOpenFile = { filePath -> viewModel.openFile(context, filePath) }
                                )
                            }
                        }
                    }
                }
                
                // 总下载进度条 - 固定在底部
                if (totalProgress.showProgress) {
                    TotalProgressBar(totalProgress = totalProgress)
                }
            }
        
        // 添加下载对话框
        if (uiState.showAddDialog) {
            AddDownloadDialog(
                onDismiss = { 
                    viewModel.hideAddDialog()
                    // 对话框关闭时重置临时路径
                    onResetTempFolder?.invoke()
                },
                onConfirm = { url, fileName, threadCount, saveToPublicDir, customPath, userAgent ->
                    // customPath是对话框内部的临时路径，传递给ViewModel
                    viewModel.createDownloadTask(url, fileName, threadCount, saveToPublicDir, customPath, userAgent)
                    // 下载开始后重置临时路径
                    onResetTempFolder?.invoke()
                },
                onSelectFolder = onSelectTempFolder,  // 使用临时目录选择
                currentTempPath = tempFolderPath  // 传递当前临时路径
            )
        }
        
            // 错误提示
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // 可以在这里显示 Snackbar
                    viewModel.clearError()
                }
            }
        }
    }
}

/**
 * 侧滑导航抽屉内容
 */
@Composable
fun NavigationDrawerContent(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
    onClearCompleted: () -> Unit,
    onClearFailed: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicenses: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
        ) {
            // 抽屉头部
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NyaLoader",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "多线程下载器",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider()
            
            // 过滤选项
            Text(
                text = "任务筛选",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                label = { Text("全部") },
                selected = currentFilter == TaskFilter.ALL,
                onClick = { onFilterSelected(TaskFilter.ALL) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Download, null) },
                label = { Text("下载中") },
                selected = currentFilter == TaskFilter.DOWNLOADING,
                onClick = { onFilterSelected(TaskFilter.DOWNLOADING) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.CheckCircle, null) },
                label = { Text("已完成") },
                selected = currentFilter == TaskFilter.COMPLETED,
                onClick = { onFilterSelected(TaskFilter.COMPLETED) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Pause, null) },
                label = { Text("已暂停") },
                selected = currentFilter == TaskFilter.PAUSED,
                onClick = { onFilterSelected(TaskFilter.PAUSED) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Error, null) },
                label = { Text("失败") },
                selected = currentFilter == TaskFilter.FAILED,
                onClick = { onFilterSelected(TaskFilter.FAILED) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 清理操作
            Text(
                text = "任务管理",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Clear, null) },
                label = { Text("清除已完成") },
                selected = false,
                onClick = onClearCompleted,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Clear, null) },
                label = { Text("清除失败") },
                selected = false,
                onClick = onClearFailed,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 更多选项
            Text(
                text = "更多",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text("设置") },
                selected = false,
                onClick = onOpenSettings,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Code, null) },
                label = { Text("开源许可") },
                selected = false,
                onClick = onOpenLicenses,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/**
 * 空状态组件
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    filter: TaskFilter
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (filter) {
                TaskFilter.ALL -> Icons.Default.CloudDownload
                TaskFilter.DOWNLOADING -> Icons.Default.Download
                TaskFilter.COMPLETED -> Icons.Default.CheckCircle
                TaskFilter.PAUSED -> Icons.Default.Pause
                TaskFilter.FAILED -> Icons.Default.Error
            },
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "暂无下载任务"
                TaskFilter.DOWNLOADING -> "暂无正在下载的任务"
                TaskFilter.COMPLETED -> "暂无已完成的任务"
                TaskFilter.PAUSED -> "暂无已暂停的任务"
                TaskFilter.FAILED -> "暂无失败的任务"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "点击右下角按钮创建新任务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * 搜索无结果状态
 */
@Composable
fun SearchEmptyState(
    modifier: Modifier = Modifier,
    searchQuery: String
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "未找到匹配的任务",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "「$searchQuery」",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * 搜索顶栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { 
                    Text(
                        "搜索文件名、URL或路径",
                        style = MaterialTheme.typography.bodyLarge
                    ) 
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * 总下载进度条组件
 */
@Composable
fun TotalProgressBar(
    totalProgress: TotalProgress
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "总下载进度",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "${totalProgress.activeTaskCount} 个任务",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = { totalProgress.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 进度信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatFileSize(totalProgress.downloadedSize)} / ${formatFileSize(totalProgress.totalSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (totalProgress.totalSpeed > 0) {
                        Text(
                            text = "${formatSpeed(totalProgress.totalSpeed)}/s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = "${String.format("%.1f", totalProgress.progress)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

