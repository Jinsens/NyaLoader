package com.nyapass.loader.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.ui.components.AddDownloadDialog
import com.nyapass.loader.ui.components.DownloadTaskItem
import com.nyapass.loader.ui.components.TagManagementDialog
import com.nyapass.loader.ui.screen.components.*
import com.nyapass.loader.viewmodel.DownloadViewModel
import com.nyapass.loader.viewmodel.TaskFilter
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
    onOpenLicenses: () -> Unit = {},
    onOpenWebView: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val totalProgress by viewModel.totalProgress.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val tagStats by viewModel.tagStatistics.collectAsStateWithLifecycle()
    
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    var showTagManagerDialog by remember { mutableStateOf(false) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showClearFailedDialog by remember { mutableStateOf(false) }
    
    // 记录滚动状态
    val listState = rememberLazyListState()
    
    // 根据滚动状态决定FAB是否展开
    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    
    // 处理返回键的优先级：搜索 > 多选模式 > 侧边栏 > 过滤器
    // 1. 如果正在搜索，返回键关闭搜索
    if (uiState.isSearching) {
        androidx.activity.compose.BackHandler {
            viewModel.stopSearch()
        }
    }
    // 2. 如果在多选模式，返回键退出多选模式
    else if (uiState.isMultiSelectMode) {
        androidx.activity.compose.BackHandler {
            viewModel.exitMultiSelectMode()
        }
    }
    // 3. 如果侧边栏打开，返回键关闭侧边栏
    else if (drawerState.isOpen) {
        androidx.activity.compose.BackHandler {
            scope.launch { drawerState.close() }
        }
    }
    // 4. 如果过滤器不是"全部"，返回键重置为"全部"
    else if (uiState.filter != TaskFilter.ALL) {
        androidx.activity.compose.BackHandler {
            viewModel.updateFilter(TaskFilter.ALL)
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
                    scope.launch { drawerState.close() }
                    showClearCompletedDialog = true
                },
                onClearFailed = {
                    scope.launch { drawerState.close() }
                    showClearFailedDialog = true
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                onOpenTagManager = {
                    scope.launch { drawerState.close() }
                    showTagManagerDialog = true
                },
                onOpenWebView = {
                    scope.launch { drawerState.close() }
                    onOpenWebView()
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                            }
                        },
                        actions = {
                            // 搜索按钮
                            IconButton(onClick = { viewModel.startSearch() }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        windowInsets = TopAppBarDefaults.windowInsets
                    )
                }
            },
            floatingActionButton = {
                // 根据滚动状态展开或收起FAB，显示总进度条时向上偏移
                if (uiState.isMultiSelectMode) {
                    // 多选模式：显示批量操作按钮
                    Row(
                        modifier = Modifier.padding(
                            bottom = if (totalProgress.showProgress) 110.dp else 0.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 批量重试按钮
                        FloatingActionButton(
                            onClick = { 
                                viewModel.retrySelectedTasks()
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.retry)
                            )
                        }
                        
                        // 批量删除按钮
                        FloatingActionButton(
                            onClick = {
                                viewModel.deleteSelectedTasks()
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    // 正常模式：显示新建下载按钮
                    Box(
                        modifier = Modifier.padding(
                            bottom = if (totalProgress.showProgress) 110.dp else 0.dp
                        )
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.showAddDialog() },
                            expanded = expandedFab,
                            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_download)) },
                            text = { Text(stringResource(R.string.new_download)) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TagFilterBar(
                    tags = tags,
                    selectedTagId = uiState.selectedTagId,
                    showOnlyUntagged = uiState.showOnlyUntagged,
                    onSelectAll = { viewModel.selectAllTags() },
                    onSelectTag = { tagId -> viewModel.selectTag(tagId) },
                    onSelectUntagged = { viewModel.selectUntagged() }
                )
                
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
                        // 性能优化：使用 key 参数、contentType，减少不必要的重组
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
                                key = { it.task.id },
                                contentType = { "DownloadTaskItem" }
                            ) { taskWithProgress ->
                                DownloadTaskItem(
                                    taskWithProgress = taskWithProgress,
                                    onStart = { viewModel.startDownload(it) },
                                    onPause = { viewModel.pauseDownload(it) },
                                    onResume = { viewModel.resumeDownload(it) },
                                    onDelete = { viewModel.deleteTask(it) },
                                    onRetry = { viewModel.retryTask(it) },
                                    onOpenFile = { selectedTask -> viewModel.openFile(context, selectedTask) },
                                    isMultiSelectMode = uiState.isMultiSelectMode,
                                    isSelected = taskWithProgress.task.id in uiState.selectedTaskIds,
                                    onLongPress = { taskId -> viewModel.enterMultiSelectMode(taskId) },
                                    onToggleSelection = { taskId -> viewModel.toggleTaskSelection(taskId) }
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
                onConfirm = { url, fileName, threadCount, saveToPublicDir, customPath, userAgent, tagIds ->
                    // customPath是对话框内部的临时路径，传递给ViewModel
                    viewModel.createDownloadTask(
                        url = url,
                        fileName = fileName,
                        threadCount = threadCount,
                        saveToPublicDir = saveToPublicDir,
                        customPath = customPath,
                        userAgent = userAgent,
                        tagIds = tagIds
                    )
                    // 下载开始后重置临时路径
                    onResetTempFolder?.invoke()
                },
                onSelectFolder = onSelectTempFolder,  // 使用临时目录选择
                currentTempPath = tempFolderPath,  // 传递当前临时路径
                availableTags = tags
            )
        }

        if (showTagManagerDialog) {
            TagManagementDialog(
                tags = tags,
                tagStats = tagStats,
                onDismiss = { showTagManagerDialog = false },
                onCreateTag = { name, color ->
                    viewModel.createTag(name, color)
                },
                onUpdateTag = { tagId, name, color ->
                    viewModel.updateTag(tagId, name, color)
                },
                onDeleteTag = { tagId ->
                    viewModel.deleteTag(tagId)
                },
                onMoveTag = { firstId, secondId ->
                    viewModel.swapTagOrder(firstId, secondId)
                }
            )
        }
        
        // 清除已完成任务确认对话框
        if (showClearCompletedDialog) {
            AlertDialog(
                onDismissRequest = { showClearCompletedDialog = false },
                title = { Text(stringResource(R.string.clear_completed_dialog_title)) },
                text = { Text(stringResource(R.string.clear_completed_dialog_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearCompletedTasks()
                            showClearCompletedDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCompletedDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        
        // 清除失败任务确认对话框
        if (showClearFailedDialog) {
            AlertDialog(
                onDismissRequest = { showClearFailedDialog = false },
                title = { Text(stringResource(R.string.clear_failed_dialog_title)) },
                text = { Text(stringResource(R.string.clear_failed_dialog_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearFailedTasks()
                            showClearFailedDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearFailedDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
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

@Composable
private fun TagFilterBar(
    tags: List<DownloadTag>,
    selectedTagId: Long?,
    showOnlyUntagged: Boolean,
    onSelectAll: () -> Unit,
    onSelectTag: (Long) -> Unit,
    onSelectUntagged: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !showOnlyUntagged && selectedTagId == null,
            onClick = onSelectAll,
            label = { Text(stringResource(R.string.tag_filter_all)) }
        )
        FilterChip(
            selected = showOnlyUntagged,
            onClick = onSelectUntagged,
            label = { Text(stringResource(R.string.tag_filter_untagged)) }
        )
        tags.forEach { tag ->
            val color = Color(tag.color)
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelectTag(tag.id) },
                label = { Text(tag.name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, shape = CircleShape)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f)
                )
            )
        }
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
                        stringResource(R.string.search_placeholder),
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
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}
