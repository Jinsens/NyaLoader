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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import android.content.Intent
import android.net.Uri
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
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            "1.2.5 (4)"
        }
    }
    
    val openSourceLibraries = getOpenSourceLibraries()
    
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_and_licenses)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
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
                    text = stringResource(R.string.open_source_licenses),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Text(
                    text = stringResource(R.string.using_open_source_projects),
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
                    text = stringResource(R.string.thanks_to_contributors),
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
    val context = LocalContext.current
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
                contentDescription = stringResource(R.string.app_icon),
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
                text = stringResource(R.string.multi_thread_downloader),
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
                    contentDescription = stringResource(R.string.version),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.version_format, versionInfo),
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
                    contentDescription = stringResource(R.string.author),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.author_name),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 版权信息
            Text(
                text = stringResource(R.string.copyright),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stringResource(R.string.open_source_license_agpl),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // GitHub 项目按钮
            FilledTonalButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Jinsens/NyaLoader"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.view_source_code))
            }
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
                    label = { Text(stringResource(R.string.version_format, library.version)) }
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
@androidx.compose.runtime.Immutable
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
@Composable
fun getOpenSourceLibraries() = listOf(
    OpenSourceLibrary(
        name = "Kotlin",
        description = stringResource(R.string.lib_kotlin_desc),
        license = "Apache 2.0",
        version = "2.3.10",
        url = "https://kotlinlang.org"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose",
        description = stringResource(R.string.lib_jetpack_compose_desc),
        license = "Apache 2.0",
        version = "2026.01.01",
        url = "https://developer.android.com/compose"
    ),
    OpenSourceLibrary(
        name = "Material 3",
        description = stringResource(R.string.lib_material3_desc),
        license = "Apache 2.0",
        version = "BOM 2026.01.01",
        url = "https://m3.material.io"
    ),
    OpenSourceLibrary(
        name = "Material Icons Extended",
        description = stringResource(R.string.lib_material_icons_desc),
        license = "Apache 2.0",
        version = "BOM 2026.01.01",
        url = "https://fonts.google.com/icons"
    ),
    OpenSourceLibrary(
        name = "AndroidX Core KTX",
        description = stringResource(R.string.lib_androidx_core_desc),
        license = "Apache 2.0",
        version = "1.17.0",
        url = "https://developer.android.com/kotlin/ktx"
    ),
    OpenSourceLibrary(
        name = "Lifecycle Runtime KTX",
        description = stringResource(R.string.lib_lifecycle_desc),
        license = "Apache 2.0",
        version = "2.10.0",
        url = "https://developer.android.com/topic/libraries/architecture/lifecycle"
    ),
    OpenSourceLibrary(
        name = "Activity Compose",
        description = stringResource(R.string.lib_activity_compose_desc),
        license = "Apache 2.0",
        version = "1.12.3",
        url = "https://developer.android.com/jetpack/androidx/releases/activity"
    ),
    OpenSourceLibrary(
        name = "ViewModel Compose",
        description = stringResource(R.string.lib_viewmodel_compose_desc),
        license = "Apache 2.0",
        version = "2.10.0",
        url = "https://developer.android.com/topic/libraries/architecture/viewmodel"
    ),
    OpenSourceLibrary(
        name = "Room Database",
        description = stringResource(R.string.lib_room_desc),
        license = "Apache 2.0",
        version = "2.8.4",
        url = "https://developer.android.com/training/data-storage/room"
    ),
    OpenSourceLibrary(
        name = "Navigation Compose",
        description = stringResource(R.string.lib_navigation_desc),
        license = "Apache 2.0",
        version = "2.9.7",
        url = "https://developer.android.com/jetpack/compose/navigation"
    ),
    OpenSourceLibrary(
        name = "OkHttp",
        description = stringResource(R.string.lib_okhttp_desc),
        license = "Apache 2.0",
        version = "5.3.2",
        url = "https://square.github.io/okhttp/"
    ),
    OpenSourceLibrary(
        name = "Kotlin Coroutines",
        description = stringResource(R.string.lib_coroutines_desc),
        license = "Apache 2.0",
        version = "1.10.2",
        url = "https://kotlinlang.org/docs/coroutines-overview.html"
    ),
    OpenSourceLibrary(
        name = "Kotlinx Serialization",
        description = stringResource(R.string.lib_serialization_desc),
        license = "Apache 2.0",
        version = "1.10.0",
        url = "https://github.com/Kotlin/kotlinx.serialization"
    ),
    OpenSourceLibrary(
        name = "WorkManager",
        description = stringResource(R.string.lib_workmanager_desc),
        license = "Apache 2.0",
        version = "2.11.1",
        url = "https://developer.android.com/topic/libraries/architecture/workmanager"
    ),
    OpenSourceLibrary(
        name = "Gson",
        description = stringResource(R.string.lib_gson_desc),
        license = "Apache 2.0",
        version = "2.13.2",
        url = "https://github.com/google/gson"
    ),
    OpenSourceLibrary(
        name = "Hilt",
        description = stringResource(R.string.lib_hilt_desc),
        license = "Apache 2.0",
        version = "2.59.1",
        url = "https://dagger.dev/hilt/"
    ),
    OpenSourceLibrary(
        name = "Firebase",
        description = stringResource(R.string.lib_firebase_desc),
        license = "Apache 2.0",
        version = "BOM 34.9.0",
        url = "https://firebase.google.com"
    ),
    OpenSourceLibrary(
        name = "Guava",
        description = stringResource(R.string.lib_guava_desc),
        license = "Apache 2.0",
        version = "33.5.0-android",
        url = "https://github.com/google/guava"
    )
)

