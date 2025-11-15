package com.nyapass.loader.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadTag

/**
 * 标签选择区域组件
 * 支持多选标签和创建新标签
 *
 * @author 小花生FMR
 * @version 2.0.0
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelectionSection(
    availableTags: List<DownloadTag>,
    selectedTagIds: List<Long>,
    onTagToggle: (Long) -> Unit,
    onCreateTag: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 标题行
        TagSectionHeader(onCreateTag = onCreateTag)

        // 标签内容
        if (availableTags.isEmpty()) {
            TagEmptyHint()
        } else {
            TagFlowRow(
                tags = availableTags,
                selectedTagIds = selectedTagIds,
                onTagToggle = onTagToggle
            )

            // 未选择提示
            if (selectedTagIds.isEmpty()) {
                Text(
                    text = stringResource(R.string.tag_select_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 标签区域标题
 */
@Composable
private fun TagSectionHeader(
    onCreateTag: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.tag_section_title),
            style = MaterialTheme.typography.titleSmall
        )
        if (onCreateTag != null) {
            TextButton(onClick = onCreateTag) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add))
            }
        }
    }
}

/**
 * 标签为空时的提示
 */
@Composable
private fun TagEmptyHint() {
    Text(
        text = stringResource(R.string.tag_empty_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * 标签流式布局
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFlowRow(
    tags: List<DownloadTag>,
    selectedTagIds: List<Long>,
    onTagToggle: (Long) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            TagChip(
                tag = tag,
                isSelected = tag.id in selectedTagIds,
                onToggle = { onTagToggle(tag.id) }
            )
        }
    }
}

/**
 * 单个标签 Chip
 */
@Composable
private fun TagChip(
    tag: DownloadTag,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val tagColor = Color(tag.color)

    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = { Text(tag.name) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(tagColor, CircleShape)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = tagColor.copy(alpha = 0.2f)
        )
    )
}
