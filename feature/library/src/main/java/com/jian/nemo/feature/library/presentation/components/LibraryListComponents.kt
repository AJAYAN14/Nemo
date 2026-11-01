package com.jian.nemo.feature.library.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.library.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch

/**
 * 词库列表通用 - 56dp 纯净磨砂回顶悬浮按钮
 *
 * 严格锁定 30/48 黄金比例（56.dp 按钮对应 35.dp 图标），纯净无杂色磨砂底 + 透镜物理折射
 */
@Composable
fun BoxScope.LibraryScrollToTopButton(
    lazyListState: LazyListState,
    backdrop: Backdrop?,
    isMenuExpanded: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 6
        }
    }

    AnimatedVisibility(
        visible = showScrollToTop && !isMenuExpanded,
        enter = fadeIn(spring()) + scaleIn(spring(dampingRatio = 0.75f)),
        exit = fadeOut(tween(150)) + scaleOut(tween(150)),
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = bottomPadding)
    ) {
        FrostedLiquidGlassButton(
            onClick = {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
            shape = CircleShape,
            backdrop = backdrop,
            hasLens = backdrop != null,
            contentPadding = PaddingValues(0.dp),
            isInteractive = true,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = com.jian.nemo.core.ui.R.drawable.ic_scroll_to_top),
                contentDescription = stringResource(R.string.lib_components_scroll_to_top_cd),
                modifier = Modifier.size(35.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 词库列表通用 - 等级筛选 NemoMorphMenu
 *
 * 48dp 液态玻璃透镜按钮，专属几何图标，全部等级与 N5~N1 纯文字对齐、选中打勾、无震动
 * 遵循单一官方原生图标规范与标准设计调色盘 IosColors。
 */
@Composable
fun LibraryLevelSelectionMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    countsByLevel: Map<String, Int>,
    backdrop: Backdrop?,
    onScrollToTop: () -> Unit
) {
    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        buttonSize = 48.dp,
        menuWidth = 200.dp,
        enableHaptic = false,
        backdrop = backdrop,
        iconPainter = painterResource(id = com.jian.nemo.core.ui.R.drawable.ic_level_all),
        contentDescription = stringResource(R.string.lib_components_select_level_cd)
    ) {
        val allCount = countsByLevel.values.sum()
        val isAllSelected = selectedLevel == "ALL"

        NemoMorphMenuItem(
            text = stringResource(R.string.lib_components_all_levels_count_format, allCount),
            leadingIcon = Icons.Rounded.School,
            iconColor = IosColors.Blue,
            trailingIcon = if (isAllSelected) Icons.Rounded.Check else null,
            onClick = {
                onLevelSelected("ALL")
                close()
                onScrollToTop()
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        val levelsList = listOf(
            "N5" to IosColors.Green,
            "N4" to IosColors.Cyan,
            "N3" to IosColors.Indigo,
            "N2" to IosColors.Purple,
            "N1" to IosColors.Red
        )
        levelsList.forEach { (level, levelColor) ->
            val count = countsByLevel[level] ?: 0
            val isSelected = selectedLevel.equals(level, ignoreCase = true)
            NemoMorphMenuItem(
                text = stringResource(R.string.lib_components_level_count_format, level, count),
                leadingIcon = Icons.Rounded.School,
                iconColor = levelColor,
                trailingIcon = if (isSelected) Icons.Rounded.Check else null,
                onClick = {
                    onLevelSelected(level)
                    close()
                    onScrollToTop()
                }
            )
        }
    }
}


/**
 * 词库列表通用 - 页面全屏高斯模糊景深遮罩
 *
 * 12% 柔光暗化 + 200ms 淡入淡出，点击外部平滑收起菜单
 */
@Composable
fun LibraryMenuScrimOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}

/**
 * 词库列表通用 - 悬浮顶栏柔和垂直渐变保护遮罩容器
 */
@Composable
fun FloatingHeaderGradientContainer(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
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
            .padding(bottom = 8.dp),
        content = content
    )
}
