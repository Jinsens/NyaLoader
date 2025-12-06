package com.nyapass.loader.ui.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.nyapass.loader.R

/**
 * 线程数选择区域组件
 * 简化版本，仅支持手动输入
 *
 * @author 小花生FMR
 * @version 2.5.0
 */
@Composable
fun ThreadCountSection(
    threadCount: String,
    onThreadCountChange: (String) -> Unit,
    threadCountError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = threadCount,
        onValueChange = onThreadCountChange,
        label = { Text(stringResource(R.string.thread_count)) },
        leadingIcon = { Icon(Icons.Default.Speed, null) },
        isError = threadCountError,
        supportingText = if (threadCountError) {
            { Text(stringResource(R.string.thread_count_error)) }
        } else {
            { Text(stringResource(R.string.thread_count_hint)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}
