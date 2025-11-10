package com.nyapass.loader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 蓝色主题
private val BlueLightScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF03A9F4),
    tertiary = Color(0xFF00BCD4)
)

private val BlueDarkScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF42A5F5),
    tertiary = Color(0xFF4DD0E1)
)

// 红色主题
private val RedLightScheme = lightColorScheme(
    primary = Color(0xFFF44336),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = Color(0xFFE91E63),
    tertiary = Color(0xFFFF5722)
)

private val RedDarkScheme = darkColorScheme(
    primary = Color(0xFFEF5350),
    onPrimary = Color(0xFFB71C1C),
    primaryContainer = Color(0xFFC62828),
    onPrimaryContainer = Color(0xFFFFEBEE),
    secondary = Color(0xFFF06292),
    tertiary = Color(0xFFFF7043)
)

// 绿色主题
private val GreenLightScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF8BC34A),
    tertiary = Color(0xFF009688)
)

private val GreenDarkScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF388E3C),
    onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF9CCC65),
    tertiary = Color(0xFF26A69A)
)

// 紫色主题
private val PurpleLightScheme = lightColorScheme(
    primary = Color(0xFF9C27B0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF673AB7),
    tertiary = Color(0xFF3F51B5)
)

private val PurpleDarkScheme = darkColorScheme(
    primary = Color(0xFFBA68C8),
    onPrimary = Color(0xFF4A148C),
    primaryContainer = Color(0xFF7B1FA2),
    onPrimaryContainer = Color(0xFFF3E5F5),
    secondary = Color(0xFF9575CD),
    tertiary = Color(0xFF7986CB)
)

// 橙色主题
private val OrangeLightScheme = lightColorScheme(
    primary = Color(0xFFFF9800),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFFE65100),
    secondary = Color(0xFFFF5722),
    tertiary = Color(0xFFFFEB3B)
)

private val OrangeDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFFE65100),
    primaryContainer = Color(0xFFF57C00),
    onPrimaryContainer = Color(0xFFFFF3E0),
    secondary = Color(0xFFFF8A65),
    tertiary = Color(0xFFFFF176)
)

@Composable
fun LoaderTheme(
    themeMode: String = "system",  // system, light, dark
    themeColor: String = "blue",   // blue, red, green, purple, orange
    @Suppress("UNUSED_PARAMETER")
    customColor: Int? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = when (themeColor) {
        "red" -> if (darkTheme) RedDarkScheme else RedLightScheme
        "green" -> if (darkTheme) GreenDarkScheme else GreenLightScheme
        "purple" -> if (darkTheme) PurpleDarkScheme else PurpleLightScheme
        "orange" -> if (darkTheme) OrangeDarkScheme else OrangeLightScheme
        else -> if (darkTheme) BlueDarkScheme else BlueLightScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 使用WindowInsetsController设置状态栏样式（statusBarColor已废弃）
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

