package com.jian.nemo.core.ui.component.popup

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

sealed interface NemoPopupMenuItem

/**
 * 具有单选/多选选中指示器的菜单项
 *
 * @param text 展示文本
 * @param isSelected 当前是否被选中
 * @param dismissOnClick 点击后是否立即关闭菜单（单选项为 true，等级多选为 false）
 * @param onClick 点击事件回调
 */
data class NemoSelectivePopupMenuItem(
    val text: String,
    val isSelected: () -> Boolean,
    val dismissOnClick: Boolean = true,
    val onClick: () -> Unit
) : NemoPopupMenuItem

/**
 * 普通动作菜单项（如“自定义…”）
 */
data class NemoActionPopupMenuItem(
    val text: String,
    val onClick: () -> Unit
) : NemoPopupMenuItem

/**
 * 菜单分割线
 */
object NemoPopupMenuDivider : NemoPopupMenuItem

/**
 * 气泡菜单状态类
 */
data class NemoPopupMenuState(
    val isVisible: Boolean = false,
    val anchorBounds: Rect = Rect.Zero,
    val items: List<NemoPopupMenuItem> = emptyList()
)

private enum class PopupCorner { RightTop, RightBottom, LeftTop, LeftBottom }

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
 * 根据点击锚点与视口安全边界智能计算停靠位置
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

    // 设置项的值在右侧，优先靠右展开 (RightTop, RightBottom)
    val candidates = listOf(
        PopupCorner.RightTop to Offset(
            anchorBounds.right - menuSize.width,
            anchorBounds.bottom + gapPx
        ),
        PopupCorner.RightBottom to Offset(
            anchorBounds.right - menuSize.width,
            anchorBounds.top - menuSize.height - gapPx
        ),
        PopupCorner.LeftTop to Offset(anchorBounds.left, anchorBounds.bottom + gapPx),
        PopupCorner.LeftBottom to Offset(
            anchorBounds.left,
            anchorBounds.top - menuSize.height - gapPx
        ),
    )

    val chosen = candidates.firstOrNull { (_, p) -> overflow(p.x, p.y) == 0f }
        ?: candidates.minByOrNull { (_, p) -> overflow(p.x, p.y) }
        ?: candidates.first()

    val clampedX = chosen.second.x.coerceIn(minX, maxX)
    val clampedY = chosen.second.y.coerceIn(minY, maxY)

    return PopupPlacement(clampedX, clampedY, cornerToOrigin(chosen.first))
}

/**
 * Nemo 液态气泡悬浮菜单组件 - 原生物理果冻弹性液态毛玻璃版
 *
 * 采用 G2 连续平滑曲率 [RoundedRectangle]、真实物理折射 [Backdrop]、非对称果冻弹性（Jelly Spring）展开微晃，
 * 结合双层复合物理阴影与双阶高光渐变边框，确保稳健即时响应并呈现通透流体质感。
 */
@Composable
fun NemoPopupMenu(
    state: NemoPopupMenuState,
    backdrop: Backdrop? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible && state.items.isEmpty()) return

    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val viewport = with(density) {
        IntSize(configuration.screenWidthDp.dp.roundToPx(), configuration.screenHeightDp.dp.roundToPx())
    }

    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBarLeftPx = WindowInsets.navigationBars.getLeft(density, layoutDirection).toFloat()
    val navBarRightPx = WindowInsets.navigationBars.getRight(density, layoutDirection).toFloat()
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()

    val liveAvailableBounds = Rect(
        left = navBarLeftPx,
        top = statusBarTopPx,
        right = (viewport.width - navBarRightPx).coerceAtLeast(navBarLeftPx),
        bottom = (viewport.height - max(navBarBottomPx, imeBottomPx)).coerceAtLeast(statusBarTopPx)
    )

    var placementAnchorBounds by remember { mutableStateOf(state.anchorBounds) }
    var placementAvailableBounds by remember { mutableStateOf(liveAvailableBounds) }

    LaunchedEffect(state.isVisible) {
        if (state.isVisible) {
            placementAnchorBounds = state.anchorBounds
            placementAvailableBounds = liveAvailableBounds
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val calculatedWidthDp = remember(state.items, density) {
        var maxTextWidthPx = 0
        state.items.forEach { item ->
            val text = when (item) {
                is NemoSelectivePopupMenuItem -> item.text
                is NemoActionPopupMenuItem -> item.text
                else -> ""
            }
            if (text.isNotEmpty()) {
                val measured = textMeasurer.measure(
                    text = text,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                ).size.width
                if (measured > maxTextWidthPx) maxTextWidthPx = measured
            }
        }
        with(density) {
            (maxTextWidthPx.toDp() + 20.dp + 10.dp + 32.dp + 16.dp).coerceIn(180.dp, 240.dp)
        }
    }
    val calculatedWidthPx = with(density) { calculatedWidthDp.roundToPx() }
    val fallbackHeightPx = with(density) { 200.dp.roundToPx() }
    val effectiveMenuSize =
        if (menuSize == IntSize.Zero) IntSize(calculatedWidthPx, fallbackHeightPx) else menuSize

    val placement = remember(placementAnchorBounds, effectiveMenuSize, placementAvailableBounds) {
        pickPlacement(
            anchorBounds = placementAnchorBounds,
            menuSize = effectiveMenuSize,
            availableBounds = placementAvailableBounds,
            marginPx = with(density) { 12.dp.toPx() },
            gapPx = with(density) { 6.dp.toPx() }
        )
    }

    var isReady by remember { mutableStateOf(false) }

    // 🌟 原生同款果冻弹性拉伸弹簧 (Jelly Spring 动力学：非对称 X/Y 阻尼与刚度)
    val scaleXAni = remember { Animatable(0.65f) }
    val scaleYAni = remember { Animatable(0.45f) }
    val alphaAni = remember { Animatable(0f) }
    var isMenuInComposition by remember { mutableStateOf(false) }
    val hapticController = LocalHapticFeedback.current

    LaunchedEffect(isReady, isMenuInComposition, state.isVisible) {
        if (isReady && isMenuInComposition && state.isVisible) {
            coroutineScope {
                // 非对称果冻弹性（X 与 Y 轴微差刚度，产生生动的流体物理微晃感）
                launch { scaleXAni.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 420f, visibilityThreshold = 0.001f)) }
                launch { scaleYAni.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 380f, visibilityThreshold = 0.001f)) }
                launch { alphaAni.animateTo(1f, tween(150)) }
            }
        }
    }

    LaunchedEffect(state.isVisible) {
        if (state.isVisible) {
            hapticController.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isMenuInComposition = true
        } else {
            coroutineScope {
                launch { scaleXAni.animateTo(0.65f, spring(0.75f, 550f)) }
                launch { scaleYAni.animateTo(0.45f, spring(0.75f, 600f)) }
                launch { alphaAni.animateTo(0f, tween(120)) }
            }
            isMenuInComposition = false
            isReady = false
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 双层复合物理阴影：
    // 1. 贴身环境光阴影 (Ambient) - 8dp 模糊 + 2dp Y 偏移，精密锁住卡片立体轮廓
    val ambientShadowRadiusPx = with(density) { 8.dp.toPx() }
    val ambientShadowDyPx = with(density) { 2.dp.toPx() }
    val ambientShadowPaint = remember(ambientShadowRadiusPx) {
        AndroidPaint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(ambientShadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val ambientShadowColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.04f)

    // 2. 扩散主漫反射投影 (Key) - 26dp 模糊 + 12dp Y 偏移，提供悬浮纵深空气感
    val keyShadowRadiusPx = with(density) { 26.dp.toPx() }
    val keyShadowDyPx = with(density) { 12.dp.toPx() }
    val keyShadowPaint = remember(keyShadowRadiusPx) {
        AndroidPaint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(keyShadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val keyShadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.08f)

    val shape = RoundedRectangle(18.dp)

    // 🌟 原生同款垂直物理入射光双阶高光渐变边框 (上边缘物理反光)
    val highlightBorderBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.35f),
                0.5f to Color.White.copy(alpha = 0.12f),
                1.0f to Color.White.copy(alpha = 0.03f)
            )
        } else {
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.70f),
                0.5f to Color.White.copy(alpha = 0.25f),
                1.0f to Color.White.copy(alpha = 0.08f)
            )
        }
    }

    // 晶莹通透的液态毛玻璃底色
    val containerColor = if (isDark) {
        Color(0xFF1E1E22).copy(alpha = 0.90f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }

    if (state.isVisible) {
        BackHandler { onDismiss() }
        // 全屏透明遮罩层拦截外部点击
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

    if (isMenuInComposition) {
        Box(
            modifier = modifier
                .zIndex(99f)
                .graphicsLayer {
                    translationX = placement.x
                    translationY = placement.y
                    scaleX = scaleXAni.value
                    scaleY = scaleYAni.value
                    alpha = alphaAni.value
                    transformOrigin = placement.origin
                }
                .width(calculatedWidthDp)
                .onSizeChanged { menuSize = it }
                .drawBehind {
                    val currentAlpha = alphaAni.value
                    if (currentAlpha > 0f) {
                        val outline = shape.createOutline(size, layoutDirection, this)

                        drawIntoCanvas { canvas ->
                            // 1. 绘制深远扩散主投影 (Key Shadow)
                            val keyColor = keyShadowColor.copy(alpha = keyShadowColor.alpha * currentAlpha)
                            keyShadowPaint.color = keyColor.toArgb()
                            canvas.save()
                            canvas.translate(0f, keyShadowDyPx)
                            when (outline) {
                                is Outline.Rectangle -> canvas.nativeCanvas.drawRect(outline.rect.left, outline.rect.top, outline.rect.right, outline.rect.bottom, keyShadowPaint)
                                is Outline.Rounded -> canvas.nativeCanvas.drawRoundRect(outline.roundRect.left, outline.roundRect.top, outline.roundRect.right, outline.roundRect.bottom, outline.roundRect.bottomLeftCornerRadius.x, outline.roundRect.bottomLeftCornerRadius.y, keyShadowPaint)
                                is Outline.Generic -> canvas.nativeCanvas.drawPath(outline.path.asAndroidPath(), keyShadowPaint)
                            }
                            canvas.restore()

                            // 2. 绘制贴身环境光立体阴影 (Ambient Shadow)
                            val ambientColor = ambientShadowColor.copy(alpha = ambientShadowColor.alpha * currentAlpha)
                            ambientShadowPaint.color = ambientColor.toArgb()
                            canvas.save()
                            canvas.translate(0f, ambientShadowDyPx)
                            when (outline) {
                                is Outline.Rectangle -> canvas.nativeCanvas.drawRect(outline.rect.left, outline.rect.top, outline.rect.right, outline.rect.bottom, ambientShadowPaint)
                                is Outline.Rounded -> canvas.nativeCanvas.drawRoundRect(outline.roundRect.left, outline.roundRect.top, outline.roundRect.right, outline.roundRect.bottom, outline.roundRect.bottomLeftCornerRadius.x, outline.roundRect.bottomLeftCornerRadius.y, ambientShadowPaint)
                                is Outline.Generic -> canvas.nativeCanvas.drawPath(outline.path.asAndroidPath(), ambientShadowPaint)
                            }
                            canvas.restore()
                        }
                    }
                }
                .clip(shape)
                .border(width = 1.dp, brush = highlightBorderBrush, shape = shape)
                .then(
                    if (backdrop != null) {
                        // 🌟 物理折射：vibrancy() 色彩提纯 + 16dp 黄金中值模糊 + lens(...) 凸透镜物理厚度折射 + Highlight.Plain
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { shape },
                                effects = {
                                    val minDimension = size.minDimension
                                    vibrancy()
                                    blur(16.dp.toPx())
                                    lens(
                                        refractionHeight = 0.2f * minDimension * 0.5f,
                                        refractionAmount = 0.2f * minDimension,
                                        depthEffect = true
                                    )
                                },
                                highlight = { Highlight.Plain },
                                layerBlock = {
                                    this.alpha = alphaAni.value
                                }
                            )
                            .background(containerColor)
                    } else {
                        Modifier.background(containerColor)
                    }
                )
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
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                state.items.forEachIndexed { index, item ->
                    when (item) {
                        is NemoSelectivePopupMenuItem -> {
                            val selected = item.isSelected()
                            val itemInteractionSource = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clickable(
                                        interactionSource = itemInteractionSource,
                                        indication = null
                                    ) {
                                        hapticController.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (item.dismissOnClick) {
                                            onDismiss()
                                        }
                                        item.onClick()
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier.size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "\u2714",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.text,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is NemoActionPopupMenuItem -> {
                            val actionInteractionSource = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clickable(
                                        interactionSource = actionInteractionSource,
                                        indication = null
                                    ) {
                                        hapticController.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDismiss()
                                        item.onClick()
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is NemoPopupMenuDivider -> {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                            )
                        }
                    }

                    // 项与项之间的微透分割线
                    if (index < state.items.size - 1 &&
                        state.items[index + 1] !is NemoPopupMenuDivider &&
                        item !is NemoPopupMenuDivider
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}
