package com.jian.nemo.core.ui.component.common

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jian.nemo.core.ui.component.RoundedRectangle
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 菜单智能吸附角落
 */
private enum class PopupCorner { LeftTop, RightTop, RightBottom, LeftBottom }

private data class PopupPlacement(
    val x: Float,
    val y: Float,
    val origin: TransformOrigin
)

private fun cornerToOrigin(corner: PopupCorner): TransformOrigin = when (corner) {
    PopupCorner.LeftTop -> TransformOrigin(0f, 0f)
    PopupCorner.RightTop -> TransformOrigin(1f, 0f)
    PopupCorner.RightBottom -> TransformOrigin(1f, 1f)
    PopupCorner.LeftBottom -> TransformOrigin(0f, 1f)
}

/**
 * 借鉴 Cresto Demo 的 pickPlacement 算法：
 * 根据触发按钮的全局位置与屏幕可用安全边界，智能吸附在最佳方位展开，避免被遮挡或溢出屏幕
 */
private fun pickPlacement(
    anchorBounds: Rect,
    menuSize: IntSize,
    availableBounds: Rect,
    marginPx: Float,
    gapPx: Float
): PopupPlacement {
    val minX = availableBounds.left + marginPx
    val minY = availableBounds.top + marginPx
    val maxX = (availableBounds.right - menuSize.width - marginPx).coerceAtLeast(minX)
    val maxY = (availableBounds.bottom - menuSize.height - marginPx).coerceAtLeast(minY)

    fun overflow(x: Float, y: Float): Float {
        val left = (minX - x).coerceAtLeast(0f)
        val top = (minY - y).coerceAtLeast(0f)
        val right = (x - maxX).coerceAtLeast(0f)
        val bottom = (y - maxY).coerceAtLeast(0f)
        return left + top + right + bottom
    }

    val candidates = listOf(
        PopupCorner.RightTop to Offset(
            anchorBounds.right - menuSize.width,
            anchorBounds.bottom + gapPx
        ),
        PopupCorner.LeftTop to Offset(anchorBounds.left, anchorBounds.bottom + gapPx),
        PopupCorner.RightBottom to Offset(
            anchorBounds.right - menuSize.width,
            anchorBounds.top - menuSize.height - gapPx
        ),
        PopupCorner.LeftBottom to Offset(
            anchorBounds.left,
            anchorBounds.top - menuSize.height - gapPx
        )
    )

    val chosen = candidates.firstOrNull { (_, p) -> overflow(p.x, p.y) == 0f }
        ?: candidates.minBy { (_, p) -> overflow(p.x, p.y) }

    val clampedX = chosen.second.x.coerceIn(minX, maxX)
    val clampedY = chosen.second.y.coerceIn(minY, maxY)

    return PopupPlacement(clampedX, clampedY, cornerToOrigin(chosen.first))
}

/**
 * 苹果风格高光微光描边（对齐 Cresto 的 glasenseHighlight）
 */
fun Modifier.appleMenuHighlight(
    shape: Shape = RoundedRectangle(16.dp),
    strokeWidth: Dp = 1.2.dp
): Modifier = this.then(AppleMenuHighlightElement(shape, strokeWidth))

private data class AppleMenuHighlightElement(
    val shape: Shape,
    val strokeWidth: Dp
) : ModifierNodeElement<AppleMenuHighlightNode>() {
    override fun create(): AppleMenuHighlightNode = AppleMenuHighlightNode(shape, strokeWidth)
    override fun update(node: AppleMenuHighlightNode) { node.update(shape, strokeWidth) }
    override fun InspectorInfo.inspectableProperties() {
        name = "appleMenuHighlight"
        properties["shape"] = shape
        properties["strokeWidth"] = strokeWidth
    }
}

private class AppleMenuHighlightNode(
    var shape: Shape,
    var strokeWidth: Dp
) : Modifier.Node(), DrawModifierNode {

    private val gradientBrush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.22f),
        1.0f to Color.White.copy(alpha = 0.02f)
    )

    private var cachedOutline: Outline? = null
    private var cachedClipPath: Path? = null
    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null

    fun update(newShape: Shape, newStrokeWidth: Dp) {
        var needsInvalidate = false
        if (shape != newShape) {
            shape = newShape
            cachedOutline = null
            cachedClipPath = null
            needsInvalidate = true
        }
        if (strokeWidth != newStrokeWidth) {
            strokeWidth = newStrokeWidth
            needsInvalidate = true
        }
        if (needsInvalidate) {
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        if (cachedOutline == null || size != lastSize || layoutDirection != lastLayoutDirection) {
            cachedOutline = shape.createOutline(size, layoutDirection, this)
            cachedClipPath = when (val outline = cachedOutline!!) {
                is Outline.Generic -> outline.path
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            }
            lastSize = size
            lastLayoutDirection = layoutDirection
        }

        val outline = cachedOutline!!
        val clipPath = cachedClipPath!!

        drawContent()

        clipPath(path = clipPath) {
            drawOutline(
                outline = outline,
                brush = gradientBrush,
                style = Stroke(width = strokeWidth.toPx() * 2),
                blendMode = BlendMode.Plus
            )
        }
    }
}

/**
 * 苹果菜单操作作用域
 */
interface NemoAppleMenuScope {
    fun close()
    val isDarkTheme: Boolean
}

/**
 * NemoAppleDropdownMenu - 完全对齐 Cresto Demo 的同窗口硬件毛玻璃苹果菜单
 *
 * 核心架构特色：
 * 1. 绝不使用 Android 独立子窗口 Popup，直接作为顶层组件运行在同一个 Window（同一 RenderNode 树）中，
 *    杜绝小米 MIUI / HyperOS 底层 MiBackgroundBlurBlend 跨 Window 导致的 SIGSEGV 致命崩溃；
 * 2. 物理连续曲率超椭圆圆角（RoundedRectangle 16.dp）；
 * 3. Cresto 同款 spring(0.8f, 450f, 0.001f) 原点超调弹性展开动效；
 * 4. 苹果级 50dp 硬件深度背景模糊采样（drawBackdrop 50.dp）；
 * 5. 双通道漫反射柔和软阴影（BlurMaskFilter 32.dp，向下偏移 16.dp）；
 * 6. 边缘垂直高光描边（appleMenuHighlight）；
 * 7. 严格跟随 App 内部主题深浅色切换。
 */
@Composable
fun NemoAppleDropdownMenu(
    expanded: Boolean,
    anchorBounds: Rect,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    menuWidth: Dp = 236.dp,
    content: @Composable NemoAppleMenuScope.() -> Unit
) {
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewport = IntSize(
        with(density) { configuration.screenWidthDp.dp.roundToPx() },
        with(density) { configuration.screenHeightDp.dp.roundToPx() }
    )

    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navigationBarLeftPx = WindowInsets.navigationBars.getLeft(density, layoutDirection).toFloat()
    val navigationBarRightPx = WindowInsets.navigationBars.getRight(density, layoutDirection).toFloat()
    val navigationBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()

    val liveAvailableBounds = Rect(
        left = navigationBarLeftPx,
        top = statusBarTopPx,
        right = (viewport.width - navigationBarRightPx).coerceAtLeast(navigationBarLeftPx),
        bottom = (viewport.height - max(navigationBarBottomPx, imeBottomPx)).coerceAtLeast(statusBarTopPx)
    )

    var placementAnchorBounds by remember { mutableStateOf(anchorBounds) }
    var placementAvailableBounds by remember { mutableStateOf(liveAvailableBounds) }

    LaunchedEffect(expanded) {
        if (expanded) {
            placementAnchorBounds = anchorBounds
            placementAvailableBounds = liveAvailableBounds
        }
    }

    val menuWidthPx = with(density) { menuWidth.roundToPx() }
    val fallbackHeightPx = with(density) { 260.dp.roundToPx() }
    val effectiveMenuSize = if (menuSize == IntSize.Zero) IntSize(menuWidthPx, fallbackHeightPx) else menuSize

    val placement = remember(placementAnchorBounds, effectiveMenuSize, placementAvailableBounds) {
        pickPlacement(
            anchorBounds = placementAnchorBounds,
            menuSize = effectiveMenuSize,
            availableBounds = placementAvailableBounds,
            marginPx = with(density) { 8.dp.toPx() },
            gapPx = with(density) { 8.dp.toPx() }
        )
    }

    var isReady by remember { mutableStateOf(false) }
    val scaleAni = remember { Animatable(0.4f) }
    val alphaAni = remember { Animatable(0f) }
    var isMenuInComposition by remember { mutableStateOf(false) }

    val hapticController = LocalHapticFeedback.current

    LaunchedEffect(isReady, isMenuInComposition, expanded) {
        if (isReady && isMenuInComposition) {
            coroutineScope {
                launch { scaleAni.animateTo(1f, spring(0.8f, 450f, 0.001f)) }
                launch { alphaAni.animateTo(1f) }
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            hapticController.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isMenuInComposition = true
        } else {
            coroutineScope {
                launch { scaleAni.animateTo(0.4f, spring(0.7f, 600f)) }
                launch { alphaAni.animateTo(0f) }
            }
            isMenuInComposition = false
            isReady = false
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedRectangle(16.dp)

    val shadowRadiusPx = with(density) { 32.dp.toPx() }
    val shadowDyPx = with(density) { 16.dp.toPx() }
    val shadowPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val shadowBaseColor = if (isDarkTheme) Color.Black.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.12f)

    val scope = remember(isDarkTheme, onDismiss) {
        object : NemoAppleMenuScope {
            override fun close() { onDismiss() }
            override val isDarkTheme: Boolean get() = isDarkTheme
        }
    }

    // 全屏透明交互遮罩（处理外部点击与返回拦截，无需 Popup）
    if (expanded) {
        BackHandler { onDismiss() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(98f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    // 顶层毛玻璃菜单卡片（同处于一个窗口渲染树内）
    if (isMenuInComposition) {
        Box(
            modifier = modifier
                .zIndex(99f)
                .graphicsLayer {
                    translationX = placement.x
                    translationY = placement.y
                    scaleX = scaleAni.value
                    scaleY = scaleAni.value
                    transformOrigin = placement.origin
                }
                .width(menuWidth)
                .onSizeChanged { menuSize = it }
                .drawBehind {
                    val currentAlpha = alphaAni.value
                    if (currentAlpha > 0f) {
                        val paintColor = shadowBaseColor.copy(alpha = shadowBaseColor.alpha * currentAlpha)
                        shadowPaint.color = paintColor.toArgb()

                        drawIntoCanvas { canvas ->
                            canvas.save()
                            canvas.translate(0f, shadowDyPx)
                            when (val outline = shape.createOutline(size, layoutDirection, this)) {
                                is Outline.Rectangle -> {
                                    canvas.nativeCanvas.drawRect(
                                        outline.rect.left,
                                        outline.rect.top,
                                        outline.rect.right,
                                        outline.rect.bottom,
                                        shadowPaint
                                    )
                                }
                                is Outline.Rounded -> {
                                    canvas.nativeCanvas.drawRoundRect(
                                        outline.roundRect.left,
                                        outline.roundRect.top,
                                        outline.roundRect.right,
                                        outline.roundRect.bottom,
                                        outline.roundRect.topLeftCornerRadius.x,
                                        outline.roundRect.topLeftCornerRadius.y,
                                        shadowPaint
                                    )
                                }
                                is Outline.Generic -> {
                                    canvas.nativeCanvas.drawPath(outline.path.asAndroidPath(), shadowPaint)
                                }
                            }
                            canvas.restore()
                        }
                    }
                }
                .clip(shape)
                .then(
                    if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            layerBlock = {
                                this.alpha = alphaAni.value
                            },
                            shadow = null,
                            highlight = null,
                            effects = {
                                padding = 50.dp.toPx() * 2
                                blur(50.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(if (isDarkTheme) Color(0x3818181C) else Color(0x6BFFFFFF))
                            }
                        )
                    } else {
                        Modifier
                            .graphicsLayer { this.alpha = alphaAni.value }
                            .background(if (isDarkTheme) Color(0xF21C1C20) else Color(0xF7FFFFFF))
                    }
                )
                .appleMenuHighlight(shape = shape)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                        if (!isReady) isReady = true
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                scope.content()
            }
        }
    }
}

/**
 * 苹果风格细分割线（1.dp）
 */
@Composable
fun NemoAppleMenuDivider(
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(dividerColor)
    )
}

/**
 * 苹果风格分组条（宽底槽 Section Divider）
 */
@Composable
fun NemoAppleMenuSectionDivider(
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(dividerColor)
    )
}

/**
 * 苹果风格菜单项（左文右图标/对勾，带点按变暗反馈）
 */
@Composable
fun NemoAppleMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    plumpIcon: PlumpIcon? = null,
    iconColor: Color? = null,
    isSelected: Boolean = false,
    isDestructive: Boolean = false,
    contentColor: Color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    Row(
        modifier = modifier
            .height(46.dp)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isPressed) 0.6f else 1.0f
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )

        if (isPlumpDuo && plumpIcon != null) {
            PlumpDuoIcon(
                icon = plumpIcon,
                color = iconColor ?: contentColor,
                size = 20.dp
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = iconColor ?: contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 苹果风格开关项（带 LiquidToggle）
 */
@Composable
fun NemoAppleMenuToggleItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .height(46.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(!checked)
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
        LiquidToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            accentColor = MaterialTheme.colorScheme.primary
        )
    }
}
