package com.jian.nemo.core.ui.component.liquid

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 通用纯色液态按钮 (Unified Solid Color Liquid Button)
 * 完美融合物理弹簧形变、按压缩放与触摸滑动拉伸效果。
 * 支持通过 [shape] 参数动态配置胶囊形、正圆形、圆角矩形等任意形状，并可由 [backgroundColor] 自定义背景颜色。
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0088FF),
    shape: Shape = CircleShape,
    elevation: Dp = 6.dp,
    isInteractive: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                if (isInteractive) {
                    val width = size.width
                    val height = size.height

                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.05f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                    val maxDragScale = 4f.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX =
                        scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                    scaleY =
                        scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                }
                clip = false
                this.shape = shape
            }
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation, shape)
                } else {
                    Modifier
                }
            )
            .background(color = backgroundColor, shape = shape)
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * 纯色果冻液态按钮 (Pure Solid Color Liquid Button - 快捷调用的胶囊/长条模式)
 * 内部调用通用的 [LiquidButton]，默认为 48dp 高度的长条/胶囊/圆角按钮布局。
 */
@Composable
fun PureLiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0088FF),
    shape: Shape = CircleShape,
    isInteractive: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    LiquidButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 24.dp),
        backgroundColor = backgroundColor,
        shape = shape,
        isInteractive = isInteractive,
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

/**
 * 纯色正圆形液态按钮 / 图标按钮 (Pure Solid Color Circular Liquid Icon Button - 快捷调用的正圆模式)
 * 内部调用通用的 [LiquidButton]，专用于正圆形按钮/图标按钮/FAB。
 */
@Composable
fun PureLiquidIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF8B5CF6),
    size: Dp = 56.dp,
    isInteractive: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidButton(
        onClick = onClick,
        modifier = modifier.size(size),
        backgroundColor = backgroundColor,
        shape = CircleShape,
        isInteractive = isInteractive,
        contentAlignment = Alignment.Center,
        content = content
    )
}
