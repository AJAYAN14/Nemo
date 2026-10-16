package com.jian.nemo.feature.collection.mistakes

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val useDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Premium Aesthetics
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Custom Premium Colors
    val premiumRed = Color(0xFFFF3B30) // Apple-style System Red
    val premiumBlue = Color(0xFF007AFF) // Apple-style System Blue
    val premiumOrange = Color(0xFFFF9500) // Apple-style System Orange
    val premiumGray = Color(0xFF8E8E93) // System Gray

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
            title = "移出错题本",
            text = "确定要将选中的 ${selectedWordIds.size} 个单词从错题本中移除吗？此操作不会删除单词本身，仅清除错题记录。",
            confirmText = "确认移除",
            dismissText = "取消",
            isDangerous = true,
            onConfirm = {
                viewModel.deleteWrongWords(selectedWordIds)
                selectedWordIds = emptySet()
                showDeleteDialog = false
            }
        )
    }

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
                // Premium Empty State
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
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = premiumGray.copy(alpha = 0.1f),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = premiumGray.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "暂无错词记录",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "太棒了！继续保持全对的状态。",
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
                                accentColor = premiumBlue,
                                tagColor = premiumBlue
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
                            contentDescription = "取消选择",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "已选择 ${selectedWordIds.size} 项",
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
                            text = if (isAllSelected) "取消全选" else "全选",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = premiumBlue
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
                            contentDescription = "批量删除",
                            tint = premiumRed,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            } else {
                com.jian.nemo.core.ui.component.common.CommonHeader(
                    title = "错误的单词",
                    onBack = onNavigateBack,
                    backgroundColor = Color.Transparent,
                    backdrop = backdrop,
                    backButtonSize = 48.dp
                )
            }
        }
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Clean surface
        ),
        // Subtle border for definition, highlighted when selected
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
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

            // Premium Tag: Tinted Squircle
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tagColor.copy(alpha = 0.1f) // Tinted background
            ) {
                Text(
                    text = word.level,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tagColor // Solid text color
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

