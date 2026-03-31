package com.jian.nemo.core.ui.component.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 高保真液态拉伸开关 (Gooey Stretch Toggle)
 * 
 * 该组件实现了滑块（Thumb）在开关切换过程中的横向液态拉伸形变物理动效，
 * 通过动态调整宽度与位移的同步关系，增强交互的流畅感。
 *
 * @param checked 当前开关是否开启
 * @param onCheckedChange 开关状态改变时的回调
 * @param modifier 修饰符
 * @param enabled 是否可用
 * @param activeColor 开启状态下的轨道背景颜色，默认使用 Nemo 主题蓝
 * @param inactiveColor 关闭状态下的轨道背景颜色
 */
@Composable
fun NemoGooeyToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Color(0xFF0E68FF), // 默认使用 Nemo 品牌蓝 (#0E68FF)
    inactiveColor: Color = Color(0xFFE5E7EB) // 默认使用浅灰色 (#E5E7EB)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val transition = updateTransition(checked, label = "GooeyToggleTransition")

    // 1. 背景颜色过渡动画
    val trackColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 300) },
        label = "trackColor"
    ) { target ->
        if (target) activeColor else inactiveColor
    }

    // 2. 滑块宽度动画：1:1 还原 CSS 的液态拉伸物理逻辑 (Stretch & Snap)
    // 在动画进行到 50% 时，宽度会拉伸至最大值 42dp，随后收缩回 24dp
    val thumbWidth by transition.animateDp(
        transitionSpec = {
            keyframes {
                durationMillis = 400
                42.dp at 200 with FastOutLinearInEasing 
                24.dp at 400 with LinearOutSlowInEasing
            }
        },
        label = "thumbWidth"
    ) { 24.dp }

    // 3. 滑块位移动画：配合宽度拉伸实现“弹射”感
    // 开启过程中：中途 Offset 仅小幅移动；后半段快速补齐，营造出先拉伸后弹射的效果
    val thumbOffset by transition.animateDp(
        transitionSpec = {
            keyframes {
                durationMillis = 400
                // 对应 CSS: 50% { transform: translateX(6px) } -> (4 + 6 = 10dp)
                if (checked) 10.dp at 200 else 22.dp at 200 
            }
        },
        label = "thumbOffset"
    ) { target ->
        if (target) 28.dp else 4.dp
    }

    // 轨迹容器
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 移除系统默认水波纹，使用自定义动效
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // 白色滑块 (Thumb)
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(width = thumbWidth, height = 24.dp)
                .background(Color.White, CircleShape)
                .padding(2.dp)
        )
    }
}
