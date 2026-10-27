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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
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
    modifier: Modifier = Modifier,
    buttonSize: Dp = 56.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 200
        }
    }

    AnimatedVisibility(
        visible = showScrollToTop && !isMenuExpanded,
        enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f)),
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
            modifier = Modifier.size(buttonSize),
            shape = CircleShape,
            backdrop = backdrop,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(id = com.jian.nemo.core.ui.R.drawable.ic_scroll_to_top),
                contentDescription = "回到顶部",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(buttonSize * (30f / 48f))
            )
        }
    }
}

/**
 * 词库列表通用 - 等级筛选 NemoMorphMenu
 *
 * 48dp 液态玻璃透镜按钮，专属几何图标，全部等级与 N5~N1 纯文字对齐、选中打勾、无震动
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
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        buttonSize = 48.dp,
        menuWidth = if (isPlumpDuo) 220.dp else 200.dp,
        enableHaptic = false,
        backdrop = backdrop,
        iconPainter = painterResource(id = com.jian.nemo.core.ui.R.drawable.ic_level_all),
        contentDescription = "选择等级"
    ) {
        val allCount = countsByLevel.values.sum()
        val isAllSelected = selectedLevel == "ALL"

        NemoMorphMenuItem(
            text = "全部等级 ($allCount)",
            plumpIcon = PlumpIcon.GraduationCap,
            iconColor = if (isPlumpDuo) IsoluminantPalette.OceanBlue else null,
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
            "N5" to IsoluminantPalette.EmeraldGreen,
            "N4" to IsoluminantPalette.LakesideCyan,
            "N3" to IsoluminantPalette.SkyIndigo,
            "N2" to IsoluminantPalette.LilacViolet,
            "N1" to IsoluminantPalette.CoralFlame
        )
        levelsList.forEach { (level, levelColor) ->
            val count = countsByLevel[level] ?: 0
            val isSelected = selectedLevel.equals(level, ignoreCase = true)
            NemoMorphMenuItem(
                text = "$level 等级 ($count)",
                plumpIcon = PlumpIcon.GraduationCap,
                iconColor = if (isPlumpDuo) levelColor else null,
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
