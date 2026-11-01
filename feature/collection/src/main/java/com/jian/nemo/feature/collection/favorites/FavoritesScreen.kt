package com.jian.nemo.feature.collection.favorites

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.collection.R
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.animation.containerTransform
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop


/**
 * 我的收藏界面 - UI/UX Pro Max 风格
 *
 * 风格统一：
 * - Background: Solid
 * - Card: Premium Card (26dp Rounded)
 * - Icon: Squircle (Rounded 14dp)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateToWordFavorites: () -> Unit = {},
    onNavigateToGrammarFavorites: () -> Unit = {},
    onWordClick: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val iconStyle by viewModel.iconStyle.collectAsState()

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    var isMenuExpanded by remember { mutableStateOf(false) }

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 动态高斯模糊半径 (0dp -> 14dp 平滑过渡)
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (isMenuExpanded) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "favoritesBlur"
    )

    // 图标风格双自适应（MD3 / Plump Duo）下发给整页子组件
    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .containerTransform(
                    key = "container_favorites",
                    shape = RoundedRectangle(0.dp)
                )
        ) {
        // 1. 底层滚动内容（被 backdrop 采样源录制）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (animatedBlurRadius > 0.dp) Modifier.blur(radius = animatedBlurRadius)
                    else Modifier
                )
                .layerBackdrop(backdrop)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                )
        ) {
            // 收藏列表
            FavoritesList(
                favoriteWordsCount = uiState.favoriteWordsCount,
                favoriteGrammarsCount = uiState.favoriteGrammarsCount,
                onWordFavoritesClick = onNavigateToWordFavorites,
                onGrammarFavoritesClick = onNavigateToGrammarFavorites
            )

            Spacer(modifier = Modifier.height(30.dp))
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
            com.jian.nemo.core.ui.component.common.CommonHeader(
                title = stringResource(R.string.collection_favorites_title_screen),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    com.jian.nemo.feature.collection.components.CollectionActionMenu(
                        wordCount = uiState.favoriteWordsCount,
                        grammarCount = uiState.favoriteGrammarsCount,
                        titleSuffix = stringResource(R.string.collection_suffix_favorites),
                        onClearAll = viewModel::clearAll,
                        onClearWords = viewModel::clearAllWordFavorites,
                        onClearGrammars = viewModel::clearAllGrammarFavorites,
                        isExpanded = isMenuExpanded,
                        onToggleMenu = { isMenuExpanded = it },
                        backdrop = backdrop
                    )
                }
            )
        }

        // 3. 页面全屏高斯模糊景深遮罩 (12% 柔光暗化 + 200ms 淡入淡出)
        androidx.compose.animation.AnimatedVisibility(
            visible = isMenuExpanded,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { isMenuExpanded = false }
                    )
            )
        }
        }
    }
}

@Composable
fun FavoritesList(
    favoriteWordsCount: Int,
    favoriteGrammarsCount: Int,
    onWordFavoritesClick: () -> Unit,
    onGrammarFavoritesClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FavoriteCategoryCard(
            title = stringResource(R.string.collection_favorites_words_title),
            subtitle = stringResource(R.string.collection_favorites_words_subtitle),
            count = favoriteWordsCount,
            icon = Icons.Rounded.Translate,
            iconColor = IosColors.Yellow,
            onClick = onWordFavoritesClick
        )

        FavoriteCategoryCard(
            title = stringResource(R.string.collection_favorites_questions_title),
            subtitle = stringResource(R.string.collection_favorites_questions_subtitle),
            count = favoriteGrammarsCount,
            icon = Icons.Rounded.Checklist,
            iconColor = IosColors.Green,
            onClick = onGrammarFavoritesClick
        )
    }
}

@Composable
fun FavoriteCategoryCard(
    title: String,
    subtitle: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val containerBgColor = if (isSoftTranslucent) iconColor.copy(alpha = 0.15f) else iconColor
    val iconTint = if (isSoftTranslucent) iconColor else Color.White

    PremiumCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器：严格遵守 2:3 黄金尺寸比例公理 (42dp 底框 x 28dp 图标，四周 7dp 留白)
            // G2 连续平滑曲率 RoundedRectangle(12.dp)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(containerBgColor, RoundedRectangle(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by if(onClick != null) {
         val isPressed by interactionSource.collectIsPressedAsState()
         animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            label = "cardScale",
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = MaterialTheme.colorScheme.surface


    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedRectangle(26.dp),
        color = containerColor,

        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 26.dp, isDark = isDark)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        content = { Column(modifier = Modifier.padding(24.dp), content = content) }
    )
}
