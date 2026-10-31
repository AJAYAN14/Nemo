package com.jian.nemo.feature.collection.favorites

import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.NemoDangerDark
import com.jian.nemo.core.designsystem.theme.NemoOrange
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
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
 * 收藏单词列表界面
 *
 * UI/UX Pro Max V2: Custom Premium Colors, Tinted Squircle Tags, High-Quality Surfaces
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteWordsScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
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

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        val isPlumpDuo = iconStyle == SettingIconStyle.PLUMP_DUO

    // 标准语义色彩分配
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val premiumRed = if (isDark) NemoDangerDark else NemoDanger
    val premiumBlue = MaterialTheme.colorScheme.primary
    val premiumOrange = NemoOrange
    val premiumGray = MaterialTheme.colorScheme.onSurfaceVariant

    // 双方案色彩分流：Plump Duo 切换为 IsoluminantPalette 等亮度色，MD3 100% 保持原色
    val heartColor = if (isPlumpDuo) IsoluminantPalette.RoseRuby else premiumRed
    val accentColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else premiumBlue
    val deleteColor = if (isPlumpDuo) IsoluminantPalette.CoralFlame else premiumRed

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
            title = "取消收藏",
            text = "确定要将选中的 ${selectedWordIds.size} 个单词取消收藏吗？",
            confirmText = "确认移出",
            dismissText = "取消",
            isDangerous = true,
            onConfirm = {
                viewModel.deleteWordFavorites(selectedWordIds)
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
                uiState.favoriteWords.isEmpty() -> {
                    // Empty State: Plump Duo 取消外层纯色底块，使用纯透明对齐容器
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
                            if (isPlumpDuo) {
                                Box(
                                    modifier = Modifier.size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PlumpDuoIcon(
                                        icon = PlumpIcon.Heart,
                                        color = heartColor,
                                        size = 64.dp
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(32.dp),
                                    color = premiumRed.copy(alpha = 0.1f), // Red tint for Favorites
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = premiumRed.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "暂无收藏单词",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "在学习过程中遇到喜欢的单词可以收藏哦",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 2,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            items = uiState.favoriteWords,
                            key = { "favorite_${it.id}" }
                        ) { word ->
                            val isSelected = selectedWordIds.contains(word.id)
                            Box(modifier = Modifier.animateListItem()) {
                                FavoriteWordItem(
                                    word = word,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
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
                                    tagColor = getLevelColor(word.level, isPlumpDuo),
                                    heartColor = heartColor,
                                    isPlumpDuo = isPlumpDuo
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

                        val allWordIds = uiState.favoriteWords.map { it.id }.toSet()
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
                                contentDescription = "批量取消收藏",
                                tint = deleteColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    com.jian.nemo.core.ui.component.common.CommonHeader(
                        title = "收藏单词",
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
}

/**
 * 获取 JLPT 等级对应颜色（双风格自适应：Plump Duo 下对应等亮度色彩，MD3 保持原经典蓝）
 */
private fun getLevelColor(level: String, isPlumpDuo: Boolean): Color {
    return if (isPlumpDuo) {
        when (level.uppercase()) {
            "N5" -> IsoluminantPalette.EmeraldGreen
            "N4" -> IsoluminantPalette.LakesideCyan
            "N3" -> IsoluminantPalette.SkyIndigo
            "N2" -> IsoluminantPalette.WarmAmber
            "N1" -> IsoluminantPalette.RoseRuby
            else -> IsoluminantPalette.SkyIndigo
        }
    } else {
        NemoPrimary
    }
}

/**
 * 收藏单词列表项 - Premium Card Style V2
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteWordItem(
    word: Word,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    accentColor: Color,
    tagColor: Color,
    heartColor: Color,
    isPlumpDuo: Boolean
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
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
                    style = MaterialTheme.typography.headlineSmall,
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

            // Stacked Icons/Tags
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Heart Icon：Plump Duo 取消方块背景，使用透明对齐容器
                if (isPlumpDuo) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlumpDuoIcon(
                            icon = PlumpIcon.Heart,
                            color = heartColor,
                            size = 18.dp
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = heartColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Favorited",
                                tint = heartColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // JLPT Squircle Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tagColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = word.level,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tagColor
                    )
                }
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

