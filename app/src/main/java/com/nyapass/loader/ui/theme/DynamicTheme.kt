package com.nyapass.loader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nyapass.loader.data.preferences.DarkMode
import com.nyapass.loader.data.preferences.ThemeColor

/**
 * 蓝色主题
 */
private val BlueLightScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1)
)

private val BlueDarkScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFBBDEFB)
)

/**
 * 绿色主题
 */
private val GreenLightScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20)
)

private val GreenDarkScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF388E3C),
    onPrimaryContainer = Color(0xFFC8E6C9)
)

/**
 * 紫色主题
 */
private val PurpleLightScheme = lightColorScheme(
    primary = Color(0xFF9C27B0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF4A148C)
)

private val PurpleDarkScheme = darkColorScheme(
    primary = Color(0xFFBA68C8),
    onPrimary = Color(0xFF4A148C),
    primaryContainer = Color(0xFF7B1FA2),
    onPrimaryContainer = Color(0xFFE1BEE7)
)

/**
 * 橙色主题
 */
private val OrangeLightScheme = lightColorScheme(
    primary = Color(0xFFFF9800),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFFE65100)
)

private val OrangeDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFFE65100),
    primaryContainer = Color(0xFFF57C00),
    onPrimaryContainer = Color(0xFFFFE0B2)
)

/**
 * 红色主题
 */
private val RedLightScheme = lightColorScheme(
    primary = Color(0xFFF44336),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = Color(0xFFB71C1C)
)

private val RedDarkScheme = darkColorScheme(
    primary = Color(0xFFE57373),
    onPrimary = Color(0xFFB71C1C),
    primaryContainer = Color(0xFFD32F2F),
    onPrimaryContainer = Color(0xFFFFCDD2)
)

/**
 * 粉色主题
 */
private val PinkLightScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8BBD0),
    onPrimaryContainer = Color(0xFF880E4F)
)

private val PinkDarkScheme = darkColorScheme(
    primary = Color(0xFFF06292),
    onPrimary = Color(0xFF880E4F),
    primaryContainer = Color(0xFFC2185B),
    onPrimaryContainer = Color(0xFFF8BBD0)
)

/**
 * 青色主题
 */
private val TealLightScheme = lightColorScheme(
    primary = Color(0xFF009688),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF004D40)
)

private val TealDarkScheme = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF004D40),
    primaryContainer = Color(0xFF00796B),
    onPrimaryContainer = Color(0xFFB2DFDB)
)

/**
 * 根据主题颜色获取ColorScheme
 */
@Composable
fun getColorScheme(themeColor: ThemeColor, darkMode: DarkMode, customColorHex: String?) =
    when {
        // 如果选择了动态取色且Android 12+，使用系统动态颜色
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeColor == ThemeColor.DYNAMIC -> {
            val context = LocalContext.current
            val useDark = when (darkMode) {
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
                DarkMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            if (useDark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        else -> {
            val useDark = when (darkMode) {
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
                DarkMode.SYSTEM -> isSystemInDarkTheme()
            }
            getStaticColorScheme(themeColor, useDark, customColorHex)
        }
    }

/**
 * 获取静态颜色方案
 */
@Composable
private fun getStaticColorScheme(themeColor: ThemeColor, isDark: Boolean, customColorHex: String?) =
    when (themeColor) {
        ThemeColor.DYNAMIC -> if (isDark) BlueDarkScheme else BlueLightScheme // 降级方案
        ThemeColor.BLUE -> if (isDark) BlueDarkScheme else BlueLightScheme
        ThemeColor.GREEN -> if (isDark) GreenDarkScheme else GreenLightScheme
        ThemeColor.PURPLE -> if (isDark) PurpleDarkScheme else PurpleLightScheme
        ThemeColor.ORANGE -> if (isDark) OrangeDarkScheme else OrangeLightScheme
        ThemeColor.RED -> if (isDark) RedDarkScheme else RedLightScheme
        ThemeColor.PINK -> if (isDark) PinkDarkScheme else PinkLightScheme
        ThemeColor.TEAL -> if (isDark) TealDarkScheme else TealLightScheme
        ThemeColor.CUSTOM -> {
            val color = try {
                Color(android.graphics.Color.parseColor(customColorHex ?: "#2196F3"))
            } catch (e: Exception) {
                Color(0xFF2196F3)
            }
            
            if (isDark) {
                darkColorScheme(
                    primary = color,
                    primaryContainer = color.copy(alpha = 0.7f)
                )
            } else {
                lightColorScheme(
                    primary = color,
                    primaryContainer = color.copy(alpha = 0.3f)
                )
            }
        }
    }

