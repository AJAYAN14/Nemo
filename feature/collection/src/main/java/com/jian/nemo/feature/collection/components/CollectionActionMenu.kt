package com.jian.nemo.feature.collection.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.collection.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuButton
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuDivider
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuItem
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuOverlay
import com.kyant.backdrop.Backdrop

/**
 * 收藏与错题操作菜单全屏悬浮组件
 */
@Composable
fun CollectionActionOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    wordCount: Int,
    grammarCount: Int,
    titleSuffix: String,
    onClearAll: () -> Unit,
    onClearWords: () -> Unit,
    onClearGrammars: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    blurRadius: Dp = 12.dp,
    refractionHeightRatio: Float = 0.10f,
    refractionAmountRatio: Float = 0.20f
) {
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showClearWordsDialog by remember { mutableStateOf(false) }
    var showClearGrammarsDialog by remember { mutableStateOf(false) }

    NemoLiquidMenuOverlay(
        isVisible = isVisible,
        onDismiss = onDismiss,
        backdrop = backdrop,
        menuWidth = 260.dp,
        blurRadius = blurRadius,
        refractionHeightRatio = refractionHeightRatio,
        refractionAmountRatio = refractionAmountRatio,
        modifier = modifier
    ) {
        NemoLiquidMenuItem(
            text = stringResource(R.string.collection_menu_clear_all_format, titleSuffix),
            leadingIcon = Icons.Rounded.DeleteSweep,
            iconColor = IosColors.Red,
            isDestructive = true,
            onClick = {
                onDismiss()
                showClearAllDialog = true
            }
        )

        NemoLiquidMenuDivider()

        NemoLiquidMenuItem(
            text = stringResource(R.string.collection_menu_clear_words_format, titleSuffix),
            leadingIcon = Icons.Rounded.Delete,
            iconColor = IosColors.Orange,
            isDestructive = true,
            enabled = wordCount > 0,
            onClick = {
                onDismiss()
                showClearWordsDialog = true
            }
        )

        NemoLiquidMenuItem(
            text = stringResource(R.string.collection_menu_clear_grammars_format, titleSuffix),
            leadingIcon = Icons.Rounded.FolderDelete,
            iconColor = IosColors.Green,
            isDestructive = true,
            enabled = grammarCount > 0,
            onClick = {
                onDismiss()
                showClearGrammarsDialog = true
            }
        )
    }

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

/**
 * 收藏/错题操作按钮
 */
@Composable
fun CollectionActionButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    buttonSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    NemoLiquidMenuButton(
        isExpanded = isExpanded,
        onClick = onClick,
        backdrop = backdrop,
        buttonSize = buttonSize
    )
}

