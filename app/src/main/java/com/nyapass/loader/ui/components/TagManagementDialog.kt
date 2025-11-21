package com.nyapass.loader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.nyapass.loader.R
import com.nyapass.loader.data.model.DownloadTag
import com.nyapass.loader.data.model.TagStatistics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementDialog(
    tags: List<DownloadTag>,
    tagStats: List<TagStatistics>,
    onDismiss: () -> Unit,
    onCreateTag: (String, Long) -> Unit,
    onUpdateTag: (Long, String, Long) -> Unit,
    onDeleteTag: (Long) -> Unit,
    onMoveTag: (Long, Long) -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf(TAG_COLOR_PALETTE.first()) }
    var editingTag by remember { mutableStateOf<DownloadTag?>(null) }
    var deletingTag by remember { mutableStateOf<DownloadTag?>(null) }

    val statsMap = remember(tagStats) {
        tagStats.associateBy { it.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
        title = { Text(text = stringResource(R.string.tag_manage_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tag_create_section_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text(stringResource(R.string.tag_name_label)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ColorPalette(
                        selectedColor = newTagColor,
                        onColorSelected = { newTagColor = it }
                    )
                    Button(
                        onClick = {
                            onCreateTag(newTagName, newTagColor)
                            newTagName = ""
                            newTagColor = TAG_COLOR_PALETTE.first()
                        },
                        enabled = newTagName.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.tag_create_button))
                    }
                }

                HorizontalDivider()

                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.tag_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEachIndexed { index, tag ->
                            val stats = statsMap[tag.id]
                            TagRow(
                                tag = tag,
                                stats = stats,
                                canMoveUp = index > 0,
                                canMoveDown = index < tags.lastIndex,
                                onMoveUp = {
                                    val otherId = tags[index - 1].id
                                    onMoveTag(tag.id, otherId)
                                },
                                onMoveDown = {
                                    val otherId = tags[index + 1].id
                                    onMoveTag(tag.id, otherId)
                                },
                                onEdit = { editingTag = tag },
                                onDelete = { deletingTag = tag }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )

    editingTag?.let { tag ->
        TagEditDialog(
            tag = tag,
            onDismiss = { editingTag = null },
            onConfirm = { name, color ->
                onUpdateTag(tag.id, name, color)
                editingTag = null
            }
        )
    }

    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.tag_delete_confirm_title)) },
            text = { Text(stringResource(R.string.tag_delete_confirm_message, tag.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTag(tag.id)
                        deletingTag = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TagRow(
    tag: DownloadTag,
    stats: TagStatistics?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(tag.color))
                    )
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                stats?.let {
                    Text(
                        text = stringResource(
                            R.string.tag_stats_format,
                            it.taskCount,
                            formatFileSize(it.totalSize)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.tag_move_up))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.tag_move_down))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.tag_edit_button))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.tag_delete_button))
                }
            }
        }
    }
}

@Composable
private fun TagEditDialog(
    tag: DownloadTag,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember(tag) { mutableStateOf(tag.name) }
    var color by remember(tag) { mutableStateOf(tag.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text(stringResource(R.string.tag_edit_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tag_name_label)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                ColorPalette(
                    selectedColor = color,
                    onColorSelected = { color = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, color) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPalette(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TAG_COLOR_PALETTE.forEach { colorValue ->
            val color = Color(colorValue)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selectedColor == colorValue) 3.dp else 1.dp,
                        color = if (selectedColor == colorValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorValue) }
            )
        }
    }
}

private val TAG_COLOR_PALETTE = listOf(
    0xFFE57373,
    0xFFF06292,
    0xFFBA68C8,
    0xFF9575CD,
    0xFF64B5F6,
    0xFF4DB6AC,
    0xFF81C784,
    0xFFFFB74D,
    0xFFA1887F
).map { it.toLong() }
