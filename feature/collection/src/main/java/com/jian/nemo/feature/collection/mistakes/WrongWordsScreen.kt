package com.jian.nemo.feature.collection.mistakes

import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.collection.R
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.NemoDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton

/**
 * 错误单词列表界面
 *
 * UI/UX Pro Max V2: Custom Premium Colors, Tinted Squircle Tags, High-Quality Surfaces
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongWordsScreen(
    viewModel: WrongWordsViewModel = hiltViewModel(),
    onWordClick: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val iconStyle by viewModel.iconStyle.collectAsState()
    val useDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Premium Aesthetics
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val accentColor = SystemColors.Blue
    val deleteColor = SystemColors.Red
    val emptyIconColor = SystemColors.Green

    // 多选状态
    var selectedWordIds by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val isSelectionMode = selectedWordIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 拦截物理返回键
    BackHandler(enabled = isSelectionMode) {
        selectedWordIds = emptySet()
    }

    if (showDeleteDialog) {
        NemoDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.collection_dialog_remove_mistake_title),
            text = stringResource(R.string.collection_dialog_remove_words_format, selectedWordIds.size),
            confirmText = stringResource(R.string.collection_btn_confirm_remove_mistake),
            dismissText = stringResource(R.string.collection_btn_cancel),
            isDangerous = true,
            onConfirm = {
                viewModel.deleteWrongWords(selectedWordIds)
                selectedWordIds = emptySet()
                showDeleteDialog = false
            }
        )
    }

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 1. 底层滚动内容（被 backdrop 采样源录制）
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarTop + 60.dp, bottom = navBarBottom),
                        contentAlignment = Alignment.Center
                    ) {
                        NemoChasingDotsLoader()
                    }
                }
                uiState.words.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarTop + 60.dp, bottom = navBarBottom),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val emptyBgColor = if (isSoftTranslucent) emptyIconColor.copy(alpha = 0.15f) else emptyIconColor
                            val emptyTint = if (isSoftTranslucent) emptyIconColor else Color.White
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(emptyBgColor, RoundedRectangle(28.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = emptyTint,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.collection_empty_mistakes_words_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.collection_empty_mistakes_words_desc),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = statusBarTop + 60.dp + 16.dp,
                            bottom = navBarBottom + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.words,
                            key = { "wrong_word_${it.id}" }
                        ) { word ->
                            val isSelected = selectedWordIds.contains(word.id)
                            Box(modifier = Modifier.animateListItem()) {
                                WrongWordItem(
                                    word = word,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onSelectedChange = { checked ->
                                        selectedWordIds = if (checked) {
                                            selectedWordIds + word.id
                                        } else {
                                            selectedWordIds - word.id
                                        }
                                    },
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedWordIds = if (isSelected) {
                                                selectedWordIds - word.id
                                            } else {
                                                selectedWordIds + word.id
                                            }
                                        } else {
                                            onWordClick(word.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedWordIds = selectedWordIds + word.id
                                        }
                                    },
                                    accentColor = accentColor,
                                    tagColor = getLevelColor(word.level)
                                )
                            }
                        }
                    }
                }
            }

            // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.95f),
                                backgroundColor.copy(alpha = 0.75f),
                                backgroundColor.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(bottom = 8.dp)
            ) {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FrostedLiquidGlassButton(
                            onClick = { selectedWordIds = emptySet() },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.collection_cd_cancel_selection),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.collection_selected_items_format, selectedWordIds.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        val allWordIds = uiState.words.map { it.id }.toSet()
                        val isAllSelected = selectedWordIds.size == allWordIds.size
                        TextButton(
                            onClick = {
                                selectedWordIds = if (isAllSelected) emptySet() else allWordIds
                            }
                        ) {
                            Text(
                                text = if (isAllSelected) stringResource(R.string.collection_btn_deselect_all) else stringResource(R.string.collection_btn_select_all),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FrostedLiquidGlassButton(
                            onClick = { showDeleteDialog = true },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.collection_cd_batch_delete),
                                tint = deleteColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    com.jian.nemo.core.ui.component.common.CommonHeader(
                        title = stringResource(R.string.collection_mistakes_words_title),
                        onBack = onNavigateBack,
                        backgroundColor = Color.Transparent,
                        backdrop = backdrop,
                        backButtonSize = 48.dp
                    )
                }
            }
        }
    }
}

/**
 * 获取 JLPT 等级对应语义颜色
 */
private fun getLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "N5" -> SystemColors.Green
        "N4" -> SystemColors.Cyan
        "N3" -> SystemColors.Blue
        "N2" -> SystemColors.Orange
        "N1" -> SystemColors.Pink
        else -> SystemColors.Indigo
    }
}

/**
 * 错误单词列表项 - Premium Card Style V2
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WrongWordItem(
    word: Word,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    accentColor: Color,
    tagColor: Color
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 20.dp, isDark = isDark)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedRectangle(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Clean surface
        ),
        border = if (isSelected) BorderStroke(2.dp, accentColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp) // More padding for premium feel
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox 平滑滑出动效
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NemoRoundCheckbox(
                        checked = isSelected,
                        checkedColor = accentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Japanese Word
                Text(
                    text = word.japanese,
                    style = MaterialTheme.typography.headlineSmall, // Larger
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Meaning and Kana
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Kana (Subtle)
                    Text(
                        text = word.hiragana,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Meaning (Primary Content)
                    Text(
                        text = word.chinese,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // JLPT Level Tag: G2 Rounded Squircle
            Surface(
                shape = RoundedRectangle(8.dp),
                color = tagColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = word.level.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tagColor
                )
            }
        }
    }
}

@Composable
private fun NemoRoundCheckbox(
    checked: Boolean,
    checkedColor: Color,
    modifier: Modifier = Modifier
) {
    val uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .size(22.dp)
            .background(
                color = if (checked) checkedColor else Color.Transparent,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .border(
                width = if (checked) 0.dp else 2.dp,
                color = if (checked) Color.Transparent else uncheckedColor,
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

