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

import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp.Companion.Hairline
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.designsystem.theme.UiPalette
import com.jian.nemo.core.ui.modifier.softCardShadow

/**
 * 通用纯色液态按钮 (Unified Solid Color Liquid Button)
 * 完美融合物理弹簧形变、按压缩放与触摸滑动拉伸效果。
 * 内置项目通用 softCardShadow 极简高质感弥散阴影，支持通过 [shape] 动态配置形状。
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = UiPalette.LiquidBrandBlue,
    shape: Shape = CircleShape,
    border: BorderStroke? = null,
    elevation: Dp = 6.dp,
    useSoftShadow: Boolean = true,
    isInteractive: Boolean = true,
    clipToShape: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val interactiveHighlight = remember(isInteractive, animationScope) {
        if (isInteractive) {
            InteractiveHighlight(animationScope = animationScope)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                if (interactiveHighlight != null) {
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
                val shouldClip = clipToShape || (interactiveHighlight != null && interactiveHighlight.pressProgress > 0f)
                clip = shouldClip
                if (shouldClip) {
                    this.shape = shape
                }
            }
            .then(
                if (elevation > 0.dp) {
                    if (useSoftShadow) {
                        Modifier.softCardShadow(borderRadius = 22.dp, isDark = isDarkTheme)
                    } else {
                        Modifier.shadow(elevation, shape)
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (shape != CircleShape && shape != RectangleShape) {
                    Modifier.vectorShapeSurface(
                        backgroundColor = backgroundColor,
                        border = border,
                        shape = shape
                    )
                } else {
                    Modifier
                        .then(
                            if (border != null) {
                                Modifier.vectorShapeBorder(border = border, shape = shape)
                            } else {
                                Modifier
                            }
                        )
                        .background(color = backgroundColor, shape = shape)
                }
            )
            .then(
                if (interactiveHighlight != null) {
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
                role = null,
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
    backgroundColor: Color = UiPalette.LiquidBrandBlue,
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
    backgroundColor: Color = UiPalette.LiquidPurple,
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

private data class BorderMaskKey(
    val shape: Shape,
    val widthPx: Int,
    val heightPx: Int,
    val strokeWidthBits: Int,
    val layoutDirection: LayoutDirection,
    val densityBits: Int
)

private object BorderMaskCache {
    private val cache = LruCache<BorderMaskKey, ImageBitmap>(32)

    fun getOrCreate(
        shape: Shape,
        widthPx: Int,
        heightPx: Int,
        strokeWidthPx: Float,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): ImageBitmap {
        val key = BorderMaskKey(
            shape = shape,
            widthPx = widthPx,
            heightPx = heightPx,
            strokeWidthBits = strokeWidthPx.toRawBits(),
            layoutDirection = layoutDirection,
            densityBits = density.density.toRawBits()
        )
        cache.get(key)?.let { return it }

        val bitmap = ImageBitmap(widthPx, heightPx, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        val halfStroke = strokeWidthPx / 2f
        val insetSize = Size(
            width = (widthPx - strokeWidthPx).coerceAtLeast(0f),
            height = (heightPx - strokeWidthPx).coerceAtLeast(0f)
        )
        val outline = shape.createOutline(insetSize, layoutDirection, density)
        drawScope.draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = Size(widthPx.toFloat(), heightPx.toFloat())
        ) {
            translate(left = halfStroke, top = halfStroke) {
                drawOutline(
                    outline = outline,
                    color = Color.White,
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
        bitmap.prepareToDraw()
        cache.put(key, bitmap)
        return bitmap
    }
}

private data class FillMaskKey(
    val shape: Shape,
    val widthPx: Int,
    val heightPx: Int,
    val layoutDirection: LayoutDirection,
    val densityBits: Int
)

private object FillMaskCache {
    private val cache = LruCache<FillMaskKey, ImageBitmap>(32)

    fun getOrCreate(
        shape: Shape,
        widthPx: Int,
        heightPx: Int,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): ImageBitmap {
        val key = FillMaskKey(
            shape = shape,
            widthPx = widthPx,
            heightPx = heightPx,
            layoutDirection = layoutDirection,
            densityBits = density.density.toRawBits()
        )
        cache.get(key)?.let { return it }

        val bitmap = ImageBitmap(widthPx, heightPx, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        val fullSize = Size(widthPx.toFloat(), heightPx.toFloat())
        val fillOutline = shape.createOutline(fullSize, layoutDirection, density)

        drawScope.draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = fullSize
        ) {
            drawOutline(outline = fillOutline, color = Color.White)
        }
        bitmap.prepareToDraw()
        cache.put(key, bitmap)
        return bitmap
    }
}

fun resolveShapeBorderMask(
    shape: Shape,
    widthPx: Int,
    heightPx: Int,
    strokeWidthPx: Float,
    layoutDirection: LayoutDirection,
    density: androidx.compose.ui.unit.Density
): ImageBitmap = BorderMaskCache.getOrCreate(
    shape = shape,
    widthPx = widthPx,
    heightPx = heightPx,
    strokeWidthPx = strokeWidthPx,
    layoutDirection = layoutDirection,
    density = density
)

fun resolveShapeFillMask(
    shape: Shape,
    widthPx: Int,
    heightPx: Int,
    layoutDirection: LayoutDirection,
    density: androidx.compose.ui.unit.Density
): ImageBitmap = FillMaskCache.getOrCreate(
    shape = shape,
    widthPx = widthPx,
    heightPx = heightPx,
    layoutDirection = layoutDirection,
    density = density
)

/**
 * 合并矢量形状的背景填充与描边，按几何尺寸复用纯白形状蒙版并通过硬件着色器着色，消除按色值重复光栅化开销。
 */
fun Modifier.vectorShapeSurface(
    backgroundColor: Color,
    border: BorderStroke?,
    shape: Shape
): Modifier = drawWithCache {
    val widthPx = size.width.roundToInt()
    val heightPx = size.height.roundToInt()
    val strokeWidthPx = if (border == null) 0f else if (border.width == Hairline) 1f else border.width.toPx()
    val solidBorderBrush = border?.brush as? SolidColor

    if (
        shape != CircleShape &&
        shape != RectangleShape &&
        widthPx in 1..480 &&
        heightPx in 1..480 &&
        (border == null || solidBorderBrush != null)
    ) {
        val fillMask = if (backgroundColor != Color.Transparent && backgroundColor.alpha > 0f) {
            FillMaskCache.getOrCreate(
                shape = shape,
                widthPx = widthPx,
                heightPx = heightPx,
                layoutDirection = layoutDirection,
                density = this
            )
        } else null
        val fillColorFilter = if (fillMask != null) ColorFilter.tint(backgroundColor) else null

        val borderColor = solidBorderBrush?.value
        val borderMask = if (borderColor != null && borderColor.alpha > 0f && strokeWidthPx > 0f) {
            BorderMaskCache.getOrCreate(
                shape = shape,
                widthPx = widthPx,
                heightPx = heightPx,
                strokeWidthPx = strokeWidthPx,
                layoutDirection = layoutDirection,
                density = this
            )
        } else null
        val borderColorFilter = if (borderMask != null && borderColor != null) ColorFilter.tint(borderColor) else null

        val dstSize = IntSize(widthPx, heightPx)
        onDrawBehind {
            if (fillMask != null && fillColorFilter != null) {
                drawImage(image = fillMask, dstSize = dstSize, colorFilter = fillColorFilter)
            }
            if (borderMask != null && borderColorFilter != null) {
                drawImage(image = borderMask, dstSize = dstSize, colorFilter = borderColorFilter)
            }
        }
    } else {
        val fillOutline = shape.createOutline(size, layoutDirection, this)
        val strokeOutline = if (border != null && strokeWidthPx > 0f) {
            val insetSize = Size(
                width = (size.width - strokeWidthPx).coerceAtLeast(0f),
                height = (size.height - strokeWidthPx).coerceAtLeast(0f)
            )
            shape.createOutline(insetSize, layoutDirection, this)
        } else null
        val halfStroke = strokeWidthPx / 2f
        val stroke = if (strokeOutline != null) Stroke(width = strokeWidthPx) else null
        onDrawBehind {
            if (backgroundColor != Color.Transparent) {
                drawOutline(outline = fillOutline, color = backgroundColor)
            }
            if (strokeOutline != null && border != null && stroke != null) {
                translate(left = halfStroke, top = halfStroke) {
                    drawOutline(outline = strokeOutline, brush = border.brush, style = stroke)
                }
            }
        }
    }
}

/**
 * 基于几何尺寸复用的纯白边框蒙版与硬件着色描边修饰符。
 */
fun Modifier.vectorShapeBorder(
    border: BorderStroke,
    shape: Shape
): Modifier = drawWithCache {
    val strokeWidthPx = if (border.width == Hairline) 1f else border.width.toPx()
    val widthPx = size.width.roundToInt()
    val heightPx = size.height.roundToInt()
    val solidBrush = border.brush as? SolidColor

    if (solidBrush != null && strokeWidthPx > 0f && widthPx in 1..1024 && heightPx in 1..1024) {
        val cachedBitmap = BorderMaskCache.getOrCreate(
            shape = shape,
            widthPx = widthPx,
            heightPx = heightPx,
            strokeWidthPx = strokeWidthPx,
            layoutDirection = layoutDirection,
            density = this
        )
        val borderColorFilter = ColorFilter.tint(solidBrush.value)
        val dstSize = IntSize(widthPx, heightPx)
        onDrawWithContent {
            drawContent()
            drawImage(
                image = cachedBitmap,
                dstSize = dstSize,
                colorFilter = borderColorFilter
            )
        }
    } else {
        val halfStroke = strokeWidthPx / 2f
        val insetSize = Size(
            width = (size.width - strokeWidthPx).coerceAtLeast(0f),
            height = (size.height - strokeWidthPx).coerceAtLeast(0f)
        )
        val outline = shape.createOutline(insetSize, layoutDirection, this)
        val stroke = Stroke(width = strokeWidthPx)
        onDrawWithContent {
            drawContent()
            if (strokeWidthPx > 0f) {
                translate(left = halfStroke, top = halfStroke) {
                    drawOutline(
                        outline = outline,
                        brush = border.brush,
                        style = stroke
                    )
                }
            }
        }
    }
}
