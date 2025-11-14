package com.nyapass.loader.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nyapass.loader.R
import com.nyapass.loader.data.preferences.ThemeColor
import com.nyapass.loader.data.preferences.DarkMode
import com.nyapass.loader.data.preferences.Language
import com.nyapass.loader.data.preferences.SaveLocation

/**
 * 获取主题颜色的本地化显示名称
 */
@Composable
fun ThemeColor.getLocalizedName(): String {
    return when (this) {
        ThemeColor.DYNAMIC -> stringResource(R.string.theme_color_dynamic)
        ThemeColor.BLUE -> stringResource(R.string.theme_color_blue)
        ThemeColor.GREEN -> stringResource(R.string.theme_color_green)
        ThemeColor.PURPLE -> stringResource(R.string.theme_color_purple)
        ThemeColor.ORANGE -> stringResource(R.string.theme_color_orange)
        ThemeColor.RED -> stringResource(R.string.theme_color_red)
        ThemeColor.PINK -> stringResource(R.string.theme_color_pink)
        ThemeColor.TEAL -> stringResource(R.string.theme_color_teal)
        ThemeColor.CUSTOM -> stringResource(R.string.theme_color_custom)
    }
}

/**
 * 获取暗色模式的本地化显示名称
 */
@Composable
fun DarkMode.getLocalizedName(): String {
    return when (this) {
        DarkMode.LIGHT -> stringResource(R.string.dark_mode_light)
        DarkMode.DARK -> stringResource(R.string.dark_mode_dark)
        DarkMode.SYSTEM -> stringResource(R.string.dark_mode_system)
    }
}

/**
 * 获取语言设置的本地化显示名称
 */
@Composable
fun Language.getLocalizedName(): String {
    return when (this) {
        Language.SYSTEM -> stringResource(R.string.language_system)
        Language.CHINESE_SIMPLIFIED -> stringResource(R.string.language_chinese_simplified)
        Language.CHINESE_TRADITIONAL -> stringResource(R.string.language_chinese_traditional)
        Language.ENGLISH -> stringResource(R.string.language_english)
        Language.JAPANESE -> stringResource(R.string.language_japanese)
    }
}

/**
 * 获取保存位置的本地化显示名称
 */
@Composable
fun SaveLocation.getLocalizedName(): String {
    return when (this) {
        SaveLocation.PUBLIC_DOWNLOAD -> stringResource(R.string.save_location_public)
        SaveLocation.PRIVATE_STORAGE -> stringResource(R.string.save_location_private)
        SaveLocation.CUSTOM -> stringResource(R.string.save_location_custom)
    }
}

