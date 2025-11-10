package com.nyapass.loader.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
/**
 * 开源许可界面
 * 
 * @author 小花生FMR
 * @version 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // 获取应用版本信息
    val versionInfo = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "1.0.0 (1)"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于与许可") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 应用信息卡片
            item {
                AppInfoCard(versionInfo = versionInfo)
            }
            
            // 分隔线
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    text = "开源许可",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Text(
                    text = "本应用使用了以下开源项目",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            items(openSourceLibraries) { library ->
                LicenseCard(library = library)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "感谢所有开源项目的贡献者！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 应用信息卡片
 */
@Composable
fun AppInfoCard(versionInfo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 应用图标
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "应用图标",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // 应用名称
            Text(
                text = "NyaLoader",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // 副标题
            Text(
                text = "多线程下载管理器",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 版本信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "版本",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "版本 $versionInfo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // 作者信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "作者",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "作者：小花生FMR",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 版权信息
            Text(
                text = "© 2024 小花生FMR",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "基于 AGPL-3.0 许可证开源",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 单个开源库卡片
 */
@Composable
fun LicenseCard(library: OpenSourceLibrary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = library.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(library.license) }
                )
                AssistChip(
                    onClick = { },
                    label = { Text("版本 ${library.version}") }
                )
            }
            
            if (library.url.isNotEmpty()) {
                Text(
                    text = library.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 开源库数据类
 */
data class OpenSourceLibrary(
    val name: String,
    val description: String,
    val license: String,
    val version: String,
    val url: String = ""
)

/**
 * 项目使用的开源库列表
 */
val openSourceLibraries = listOf(
    OpenSourceLibrary(
        name = "Kotlin",
        description = "现代化的 JVM 编程语言",
        license = "Apache 2.0",
        version = "2.2.21",
        url = "https://kotlinlang.org"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose",
        description = "Android 现代化声明式 UI 框架",
        license = "Apache 2.0",
        version = "2025.11.00",
        url = "https://developer.android.com/compose"
    ),
    OpenSourceLibrary(
        name = "Material 3",
        description = "Material Design 3 组件库",
        license = "Apache 2.0",
        version = "Latest",
        url = "https://m3.material.io"
    ),
    OpenSourceLibrary(
        name = "Material Icons Extended",
        description = "Material Design 扩展图标库",
        license = "Apache 2.0",
        version = "Latest",
        url = "https://fonts.google.com/icons"
    ),
    OpenSourceLibrary(
        name = "AndroidX Core KTX",
        description = "Android 核心库的 Kotlin 扩展",
        license = "Apache 2.0",
        version = "1.17.0",
        url = "https://developer.android.com/kotlin/ktx"
    ),
    OpenSourceLibrary(
        name = "Lifecycle Runtime KTX",
        description = "Android 生命周期感知组件",
        license = "Apache 2.0",
        version = "2.9.4",
        url = "https://developer.android.com/topic/libraries/architecture/lifecycle"
    ),
    OpenSourceLibrary(
        name = "Activity Compose",
        description = "Activity 与 Compose 的集成库",
        license = "Apache 2.0",
        version = "1.11.0",
        url = "https://developer.android.com/jetpack/androidx/releases/activity"
    ),
    OpenSourceLibrary(
        name = "ViewModel Compose",
        description = "ViewModel 与 Compose 的集成",
        license = "Apache 2.0",
        version = "2.9.4",
        url = "https://developer.android.com/topic/libraries/architecture/viewmodel"
    ),
    OpenSourceLibrary(
        name = "Room Database",
        description = "Android SQLite 数据库抽象层",
        license = "Apache 2.0",
        version = "2.8.3",
        url = "https://developer.android.com/training/data-storage/room"
    ),
    OpenSourceLibrary(
        name = "Navigation Compose",
        description = "Jetpack Compose 导航组件",
        license = "Apache 2.0",
        version = "2.9.6",
        url = "https://developer.android.com/jetpack/compose/navigation"
    ),
    OpenSourceLibrary(
        name = "OkHttp",
        description = "Square 开发的高效 HTTP 客户端库",
        license = "Apache 2.0",
        version = "5.3.0",
        url = "https://square.github.io/okhttp/"
    ),
    OpenSourceLibrary(
        name = "Kotlin Coroutines",
        description = "Kotlin 协程库，用于异步编程",
        license = "Apache 2.0",
        version = "1.10.2",
        url = "https://kotlinlang.org/docs/coroutines-overview.html"
    ),
    OpenSourceLibrary(
        name = "Kotlinx Serialization",
        description = "Kotlin 多平台序列化库",
        license = "Apache 2.0",
        version = "1.9.0",
        url = "https://github.com/Kotlin/kotlinx.serialization"
    ),
    OpenSourceLibrary(
        name = "WorkManager",
        description = "Android 后台任务调度库",
        license = "Apache 2.0",
        version = "2.11.0",
        url = "https://developer.android.com/topic/libraries/architecture/workmanager"
    ),
    OpenSourceLibrary(
        name = "Gson",
        description = "Google 的 JSON 序列化/反序列化库",
        license = "Apache 2.0",
        version = "2.13.2",
        url = "https://github.com/google/gson"
    )
)

