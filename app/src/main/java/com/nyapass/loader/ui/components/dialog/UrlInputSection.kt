package com.nyapass.loader.ui.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R

/**
 * URL 输入区域组件
 * 支持批量添加多个下载链接
 *
 * @author 小花生FMR
 * @version 2.0.0
 */
@Composable
fun UrlInputSection(
    urls: List<String>,
    urlErrors: List<Boolean>,
    onUrlChange: (Int, String) -> Unit,
    onAddUrl: () -> Unit,
    onRemoveUrl: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // URL 输入框列表
        urls.forEachIndexed { index, url ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { onUrlChange(index, it) },
                    label = {
                        Text(
                            if (urls.size > 1)
                                "${stringResource(R.string.download_link)} ${index + 1}"
                            else
                                stringResource(R.string.download_link)
                        )
                    },
                    placeholder = { Text(stringResource(R.string.enter_url)) },
                    leadingIcon = { Icon(Icons.Default.Link, null) },
                    trailingIcon = if (urls.size > 1) {
                        {
                            IconButton(onClick = { onRemoveUrl(index) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else null,
                    isError = index < urlErrors.size && urlErrors[index],
                    supportingText = if (index < urlErrors.size && urlErrors[index]) {
                        { Text(stringResource(R.string.enter_valid_url)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        // 添加更多链接按钮
        OutlinedButton(
            onClick = onAddUrl,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_more_links))
        }

        // 批量下载提示
        if (urls.size > 1) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.batch_download_hint, urls.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 文件名输入组件
 */
@Composable
fun FileNameInput(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    isBatchMode: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = fileName,
        onValueChange = onFileNameChange,
        label = { Text(stringResource(R.string.filename_optional)) },
        placeholder = { Text(stringResource(R.string.auto_extract_from_url)) },
        leadingIcon = { Icon(Icons.Default.Description, null) },
        supportingText = if (isBatchMode) {
            { Text(stringResource(R.string.filename_only_first)) }
        } else null,
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}
