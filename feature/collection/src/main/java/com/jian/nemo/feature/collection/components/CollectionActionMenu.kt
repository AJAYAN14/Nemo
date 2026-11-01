package com.jian.nemo.feature.collection.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.collection.R
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
import com.kyant.backdrop.Backdrop

/**
 * 通用收藏/错题操作菜单组件 - UI/UX Pro Max
 *
 * 遵循单一官方原生图标与调色盘标准规范：
 * - 触发图标: Google Fonts 原生实心 Icons.Rounded.MoreVert
 * - 菜单项图标: 统一采用实心 Rounded 图标 (DeleteSweep, Delete, FolderDelete)
 * - 配色体系: 严格符合项目设计调色盘 IosColors (Red, Orange, Green 单套基准色)
 */
@Composable
fun CollectionActionMenu(
    wordCount: Int,
    grammarCount: Int,
    titleSuffix: String,
    onClearAll: () -> Unit,
    onClearWords: () -> Unit,
    onClearGrammars: () -> Unit,
    isExpanded: Boolean = false,
    onToggleMenu: ((Boolean) -> Unit)? = null,
    backdrop: Backdrop? = null
) {
    if (wordCount <= 0 && grammarCount <= 0) return

    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = if (onToggleMenu != null) isExpanded else internalExpanded
    val setExpanded: (Boolean) -> Unit = { expandedValue ->
        internalExpanded = expandedValue
        onToggleMenu?.invoke(expandedValue)
    }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showClearWordsDialog by remember { mutableStateOf(false) }
    var showClearGrammarsDialog by remember { mutableStateOf(false) }

    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = setExpanded,
        icon = Icons.Rounded.MoreVert,
        contentDescription = stringResource(R.string.collection_cd_more_options),
        backdrop = backdrop,
        buttonSize = 48.dp
    ) {
        NemoMorphMenuItem(
            text = stringResource(R.string.collection_menu_clear_all_format, titleSuffix),
            leadingIcon = Icons.Rounded.DeleteSweep,
            iconColor = IosColors.Red,
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

        NemoMorphMenuItem(
            text = stringResource(R.string.collection_menu_clear_words_format, titleSuffix),
            leadingIcon = Icons.Rounded.Delete,
            iconColor = IosColors.Orange,
            isDestructive = true,
            enabled = wordCount > 0,
            onClick = {
                close()
                showClearWordsDialog = true
            }
        )

        NemoMorphMenuItem(
            text = stringResource(R.string.collection_menu_clear_grammars_format, titleSuffix),
            leadingIcon = Icons.Rounded.FolderDelete,
            iconColor = IosColors.Green,
            isDestructive = true,
            enabled = grammarCount > 0,
            onClick = {
                close()
                showClearGrammarsDialog = true
            }
        )
    }

    // Dialogs (统一使用项目通用 NemoDialog)
    if (showClearAllDialog) {
        NemoDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = stringResource(R.string.collection_menu_clear_all_format, titleSuffix),
            text = stringResource(R.string.collection_dialog_msg_clear_all_format, titleSuffix),
            confirmText = stringResource(R.string.collection_btn_clear_all),
            dismissText = stringResource(R.string.collection_btn_cancel),
            isDangerous = true,
            onConfirm = {
                onClearAll()
                showClearAllDialog = false
            }
        )
    }

    if (showClearWordsDialog) {
        NemoDialog(
            onDismissRequest = { showClearWordsDialog = false },
            title = stringResource(R.string.collection_menu_clear_words_format, titleSuffix),
            text = stringResource(R.string.collection_dialog_msg_clear_words_format, titleSuffix),
            confirmText = stringResource(R.string.collection_btn_clear),
            dismissText = stringResource(R.string.collection_btn_cancel),
            isDangerous = true,
            onConfirm = {
                onClearWords()
                showClearWordsDialog = false
            }
        )
    }

    if (showClearGrammarsDialog) {
        NemoDialog(
            onDismissRequest = { showClearGrammarsDialog = false },
            title = stringResource(R.string.collection_menu_clear_grammars_format, titleSuffix),
            text = stringResource(R.string.collection_dialog_msg_clear_grammars_format, titleSuffix),
            confirmText = stringResource(R.string.collection_btn_clear),
            dismissText = stringResource(R.string.collection_btn_cancel),
            isDangerous = true,
            onConfirm = {
                onClearGrammars()
                showClearGrammarsDialog = false
            }
        )
    }
}
