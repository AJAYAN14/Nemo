package com.jian.nemo.feature.collection.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuButton
import com.jian.nemo.core.ui.component.common.NemoNativeLiquidMenuTrigger
import com.jian.nemo.core.ui.component.liquid.menu.records.GlassMenuItemRecord
import com.jian.nemo.feature.collection.R
import com.kyant.backdrop.Backdrop

/**
 * 全局共享的活跃原生收藏/错题菜单项清单
 */
internal val ActiveNativeCollectionMenuItems = mutableStateOf<List<GlassMenuItemRecord>>(emptyList())

/**
 * 全局共享的活跃原生收藏/错题菜单选项动作分发回调
 */
internal val ActiveNativeCollectionMenuActionHandler = mutableStateOf<((String) -> Unit)?>(null)

/**
 * 全局共享的原生收藏/错题菜单展开状态
 */
internal val ActiveNativeCollectionMenuOpenState = mutableStateOf(false)

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

    val titleClearAll = stringResource(R.string.collection_menu_clear_all_format, titleSuffix)
    val titleClearWords = stringResource(R.string.collection_menu_clear_words_format, titleSuffix)
    val titleClearGrammars = stringResource(R.string.collection_menu_clear_grammars_format, titleSuffix)

    val nativeItems = remember(wordCount, grammarCount, titleSuffix, titleClearAll, titleClearWords, titleClearGrammars) {
        listOf(
            GlassMenuItemRecord(
                id = "clear_all",
                title = titleClearAll,
                icon = "trash",
                destructive = true
            ),
            GlassMenuItemRecord(separator = true),
            GlassMenuItemRecord(
                id = "clear_words",
                title = titleClearWords,
                icon = "trash",
                destructive = true,
                disabled = wordCount <= 0
            ),
            GlassMenuItemRecord(
                id = "clear_grammars",
                title = titleClearGrammars,
                icon = "folder",
                destructive = true,
                disabled = grammarCount <= 0
            )
        )
    }

    SideEffect {
        ActiveNativeCollectionMenuItems.value = nativeItems
        ActiveNativeCollectionMenuActionHandler.value = { id ->
            onDismiss()
            when (id) {
                "clear_all" -> showClearAllDialog = true
                "clear_words" -> showClearWordsDialog = true
                "clear_grammars" -> showClearGrammarsDialog = true
            }
        }
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
    buttonSize: Dp = 48.dp,
    content: (@Composable () -> Unit)? = null
) {
    val items = ActiveNativeCollectionMenuItems.value
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (items.isNotEmpty()) {
        NemoNativeLiquidMenuTrigger(
            items = items,
            modifier = modifier,
            buttonSize = buttonSize,
            colorScheme = if (isDarkTheme) "dark" else "light",
            onSelect = { item, _ ->
                ActiveNativeCollectionMenuActionHandler.value?.invoke(item.id)
            },
            onOpenChange = { open ->
                ActiveNativeCollectionMenuOpenState.value = open
                if (open != isExpanded) {
                    onClick()
                }
            },
            content = content
        )
    } else {
        NemoLiquidMenuButton(
            isExpanded = isExpanded,
            onClick = onClick,
            modifier = modifier,
            backdrop = backdrop,
            buttonSize = buttonSize,
            content = content
        )
    }
}
