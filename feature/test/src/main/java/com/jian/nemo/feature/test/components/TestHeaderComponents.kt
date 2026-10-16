package com.jian.nemo.feature.test.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.test.presentation.theme.TestDanger
import com.kyant.backdrop.Backdrop

import androidx.compose.ui.graphics.lerp

/**
 * 测试头部中间内容：倒计时显示（液态按钮容器）
 *
 * 供 CommonHeader 的 centerContent 插槽使用。
 * 当时间比例进入最后 30% 时，背景色与文字颜色在同红色系中平滑渐变。
 */
@Composable
fun TestHeaderCenterContent(
    timeLimitSeconds: Int,
    timeRemainingSeconds: Int,
    backdrop: Backdrop? = null
) {
    if (timeLimitSeconds <= 0) return

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val defaultBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.35f)
    val defaultTextColor = MaterialTheme.colorScheme.onBackground
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.6f)

    // 计算剩余时间比例 (0.0f ~ 1.0f)
    val remainingRatio = (timeRemainingSeconds.toFloat() / timeLimitSeconds.toFloat()).coerceIn(0f, 1f)

    // 触发阈值：最后 30% (即 remainingRatio <= 0.3f 时启动渐变)
    val threshold = 0.3f
    val alertFraction = if (remainingRatio < threshold) {
        ((threshold - remainingRatio) / threshold).coerceIn(0f, 1f)
    } else {
        0f
    }

    // 同色系目标颜色 (Red Tonal Scale：浅红半透背景 + 深红文字，兼顾警报与宝石通透质感)
    val targetBg = if (isDarkTheme) Color(0xFF450A0A).copy(alpha = 0.45f) else Color(0xFFFEE2E2).copy(alpha = 0.65f)
    val targetTextColor = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B)

    // 动态平滑插值
    val animatedBgColor = lerp(defaultBg, targetBg, alertFraction)
    val animatedTextColor = lerp(defaultTextColor, targetTextColor, alertFraction)

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60

    FrostedLiquidGlassButton(
        onClick = { /* 倒计时按钮不触发操作 */ },
        backdrop = backdrop,
        surfaceColor = animatedBgColor,
        shape = CircleShape,
        border = BorderStroke(0.5.dp, borderColor),
        isInteractive = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            color = animatedTextColor,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * 测试头部右侧操作区：学习界面同款形变菜单按钮 (NemoMorphMenu)
 *
 * 供 CommonHeader 的 actions 插槽使用。
 * 严格锁定 30/48 黄金比例 (48dp 按钮配 30dp 图标)，具备触觉反馈与窗口模糊背景遮罩联动。
 */
@Composable
fun TestHeaderActions(
    word: Word?,
    grammar: Grammar? = null,
    onToggleFavorite: (Int, Boolean) -> Unit,
    onPause: () -> Unit,
    backdrop: Backdrop? = null
) {
    val isFavorite = word?.isFavorite == true || grammar?.isFavorite == true
    val itemId = word?.id ?: grammar?.id ?: 0

    val menuExpandedState = LocalTestMenuExpanded.current
    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = menuExpandedState?.value ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = { expandedValue ->
        if (menuExpandedState != null) {
            menuExpandedState.value = expandedValue
        } else {
            internalExpanded = expandedValue
        }
    }

    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = setExpanded,
        buttonSize = 48.dp,
        menuWidth = 192.dp,
        backdrop = backdrop,
        icon = Icons.Rounded.MoreVert,
        contentDescription = "更多选项"
    ) {
        // 收藏/取消收藏选项
        if (itemId != 0) {
            NemoMorphMenuItem(
                text = if (isFavorite) "取消收藏" else "收藏",
                leadingIcon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                onClick = {
                    close()
                    onToggleFavorite(itemId, !isFavorite)
                }
            )
        }

        // 暂停测试选项
        NemoMorphMenuItem(
            text = "暂停测试",
            leadingIcon = Icons.Rounded.Pause,
            onClick = {
                close()
                onPause()
            }
        )
    }
}
