package com.jian.nemo.feature.collection.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem

/**
 * 通用收藏/错题操作菜单组件 - UI/UX Pro Max
 *
 * Design Spec:
 * - Menu Background: 
 *   - Light: Pure White (#FFFFFF) + High Elevation Shadow
 *   - Dark: Surface Container (#1C1C1E) + Subtle Border
 * - Shape: Rounded 16dp
 * - Typography: Inter/System Default (Clean)
 * - Icons: Rounded Material Icons
 * - Dialog: Premium Alert Dialog Style
 */
@Composable
fun CollectionActionMenu(
    wordCount: Int,
    grammarCount: Int,
    titleSuffix: String,
    onClearAll: () -> Unit,
    onClearWords: () -> Unit,
    onClearGrammars: () -> Unit
) {
    if (wordCount <= 0 && grammarCount <= 0) return

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showClearWordsDialog by remember { mutableStateOf(false) }
    var showClearGrammarsDialog by remember { mutableStateOf(false) }

    com.jian.nemo.core.ui.component.common.NemoMorphMenu(
        icon = Icons.Default.MoreVert,
        contentDescription = "更多选项"
    ) {
        com.jian.nemo.core.ui.component.common.NemoMorphMenuItem(
            text = "清除所有${titleSuffix}",
            leadingIcon = Icons.Rounded.DeleteSweep,
            isDestructive = true,
            onClick = {
                close()
                showClearAllDialog = true
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )

        com.jian.nemo.core.ui.component.common.NemoMorphMenuItem(
            text = "清除单词${titleSuffix}",
            leadingIcon = Icons.Rounded.Delete,
            enabled = wordCount > 0,
            onClick = {
                close()
                showClearWordsDialog = true
            }
        )

        com.jian.nemo.core.ui.component.common.NemoMorphMenuItem(
            text = "清除语法${titleSuffix}",
            leadingIcon = Icons.Rounded.FolderDelete,
            enabled = grammarCount > 0,
            onClick = {
                close()
                showClearGrammarsDialog = true
            }
        )
    }

    // Dialogs (统一使用项目通用 NemoDialog)
    if (showClearAllDialog) {
        com.jian.nemo.core.ui.component.NemoDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = "清除所有${titleSuffix}",
            text = "确定要清除所有${titleSuffix}吗？此操作无法撤销，所有记录将永久删除。",
            confirmText = "清除全部",
            dismissText = "取消",
            isDangerous = true,
            onConfirm = {
                onClearAll()
                showClearAllDialog = false
            }
        )
    }

    if (showClearWordsDialog) {
        com.jian.nemo.core.ui.component.NemoDialog(
            onDismissRequest = { showClearWordsDialog = false },
            title = "清除单词${titleSuffix}",
            text = "确定要清除所有单词${titleSuffix}吗？此操作无法撤销。",
            confirmText = "清除",
            dismissText = "取消",
            isDangerous = true,
            onConfirm = {
                onClearWords()
                showClearWordsDialog = false
            }
        )
    }

    if (showClearGrammarsDialog) {
        com.jian.nemo.core.ui.component.NemoDialog(
            onDismissRequest = { showClearGrammarsDialog = false },
            title = "清除语法${titleSuffix}",
            text = "确定要清除所有语法${titleSuffix}吗？此操作无法撤销。",
            confirmText = "清除",
            dismissText = "取消",
            isDangerous = true,
            onConfirm = {
                onClearGrammars()
                showClearGrammarsDialog = false
            }
        )
    }
}
