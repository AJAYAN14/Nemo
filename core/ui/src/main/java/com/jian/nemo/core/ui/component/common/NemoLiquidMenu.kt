package com.jian.nemo.core.ui.component.common

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassRenderer
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassSurfaceState
import com.jian.nemo.core.ui.component.liquid.menu.render.rememberGlassRenderer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackground
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoSurfaceContainerLowDark
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.jian.nemo.core.ui.component.liquid.menu.LiquidMenuMotionState
import com.jian.nemo.core.ui.component.liquid.menu.rememberLiquidMenuMotionState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.boundsInParent
import com.jian.nemo.core.ui.component.liquid.menu.GlassPlate
import com.jian.nemo.core.ui.component.liquid.menu.LiquidMenuItemBounds

/**
 * 隐式传递触发按钮的绝对屏幕窗口坐标
 */
val LocalLiquidMenuAnchorBounds = compositionLocalOf<MutableState<Rect?>> {
    mutableStateOf(null)
}

/**
 * 隐式传递液态菜单动力学控制器
 */
val LocalLiquidMenuMotionState = compositionLocalOf<LiquidMenuMotionState?> {
    null
}

/**
 * 隐式传递液态菜单面板容器的坐标系
 */
val LocalLiquidMenuContainerCoordinates = compositionLocalOf<MutableState<LayoutCoordinates?>> {
    mutableStateOf(null)
}

/**
 * 全局共享的活跃液态菜单动力学控制器
 */
internal val ActiveLiquidMenuMotionState = mutableStateOf<LiquidMenuMotionState?>(null)

/**
 * 全局共享的活跃液态菜单触发按钮窗口边界
 */
internal val ActiveLiquidMenuAnchorBounds = mutableStateOf<Rect?>(null)


/**
 * 液态悬浮菜单触发按钮（圆润三横平滑形变变为 ✕）
 *
 * @param isExpanded 菜单当前是否处于展开状态
 * @param onClick 点击事件
 * @param modifier 修饰符
 * @param backdrop 背景采样层（同级非空时启用透镜折射）
 * @param buttonSize 按钮尺寸（默认 48.dp）
 * @param enableHaptic 是否触发触感振动
 * @param chromaticAberration 是否启用色差弥散
 * @param content 自定义按钮内部图标内容，为空时默认呈现画廊级水平三点操作图标
 */
@Composable
fun NemoLiquidMenuButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    buttonSize: Dp = 48.dp,
    enableHaptic: Boolean = true,
    chromaticAberration: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val anchorBoundsState = LocalLiquidMenuAnchorBounds.current
    val currentIsExpanded by rememberUpdatedState(isExpanded)
    val currentOnClick by rememberUpdatedState(onClick)
    val localMotionState = LocalLiquidMenuMotionState.current

    fun currentMotionState(): LiquidMenuMotionState? =
        localMotionState ?: ActiveLiquidMenuMotionState.value

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                if (anchorBoundsState.value != bounds) {
                    anchorBoundsState.value = bounds
                }
                if (ActiveLiquidMenuAnchorBounds.value != bounds) {
                    ActiveLiquidMenuAnchorBounds.value = bounds
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val startTime = System.currentTimeMillis()
                    val wasExpandedAtDown = currentIsExpanded
                    var hasEnteredDrag = false

                    if (enableHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    currentMotionState()?.onPointerDown(startPos.x, startPos.y)

                    val anchorAtDown = anchorBoundsState.value ?: ActiveLiquidMenuAnchorBounds.value
                    val originWindowX = anchorAtDown?.left ?: 0f
                    val originWindowY = anchorAtDown?.top ?: 0f

                    // 若按下瞬间菜单处于收起状态，立即触发展开动画
                    if (!wasExpandedAtDown) {
                        currentOnClick()
                    }

                    do {
                        val event = awaitPointerEvent()
                        val currentPointer = event.changes.firstOrNull() ?: break
                        if (currentPointer.pressed) {
                            val dx = currentPointer.position.x - startPos.x
                            val dy = currentPointer.position.y - startPos.y
                            val distSq = dx * dx + dy * dy
                            val touchSlopPx = 6.dp.toPx()

                            if (!hasEnteredDrag && distSq > touchSlopPx * touchSlopPx) {
                                hasEnteredDrag = true
                            }

                            val currentAnchor = anchorBoundsState.value ?: ActiveLiquidMenuAnchorBounds.value
                            val winX = (currentAnchor?.left ?: originWindowX) + currentPointer.position.x
                            val winY = (currentAnchor?.top ?: originWindowY) + currentPointer.position.y

                            currentMotionState()?.onPointerMove(currentPointer.position.x, currentPointer.position.y)

                            if (hasEnteredDrag) {
                                currentPointer.consume()
                                currentMotionState()?.handleDragMove(
                                    windowX = winX,
                                    windowY = winY,
                                    slackPx = 24.dp.toPx()
                                ) {
                                    if (enableHaptic) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        } else {
                            // 手指抬起（ACTION_UP）
                            currentPointer.consume()
                            currentMotionState()?.onPointerUp()

                            if (hasEnteredDrag) {
                                val handled = currentMotionState()?.handleDragRelease(
                                    onSelectDismiss = {
                                        if (currentIsExpanded) currentOnClick()
                                    }
                                ) ?: false
                                if (!handled && currentIsExpanded) {
                                    currentOnClick()
                                }
                            } else {
                                val duration = System.currentTimeMillis() - startTime
                                if (wasExpandedAtDown && duration < 350L) {
                                    currentOnClick()
                                }
                            }
                            break
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                val state = currentMotionState()
                if (state != null && state.isShowing) {
                    scaleX = state.sourceScale
                    scaleY = state.sourceScale
                    alpha = state.sourceAlpha
                    translationY = state.sourceOffsetY
                }
            }
    ) {
        if (!isExpanded) {
            FrostedLiquidGlassButton(
                onClick = { /* 由外层 pointerInput 全权接管连续手势闭环 */ },
                shape = CircleShape,
                backdrop = backdrop,
                hasLens = backdrop != null,
                chromaticAberration = chromaticAberration,
                contentPadding = PaddingValues(0.dp),
                isInteractive = false,
                modifier = Modifier.fillMaxSize()
            ) {}
        }
        if (content != null) {
            content()
        } else {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 页面同级液态悬浮菜单浮层（基于多通道弹簧动力学与流体形变）
 *
 * @param isVisible 浮层是否可见
 * @param onDismiss 关闭回调
 * @param modifier 修饰符
 * @param backdrop 背景采样源
 * @param menuWidth 菜单面板宽度（默认 260.dp）
 * @param blurRadius 模糊半径（默认 12.dp）
 * @param refractionHeightRatio 动态折射高度比例（默认 0.10f）
 * @param refractionAmountRatio 动态折射强度比例（默认 0.20f）
 * @param anchorBounds 可选的触发按钮窗口位置（为空时从 LocalLiquidMenuAnchorBounds 获取或智能推导）
 * @param content 菜单列表内容
 */
@Composable
fun NemoLiquidMenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    menuWidth: Dp = 260.dp,
    estimatedHeight: Dp = 320.dp,
    blurRadius: Dp = 12.dp,
    refractionHeightRatio: Float = 0.10f,
    refractionAmountRatio: Float = 0.20f,
    anchorBounds: Rect? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val motionState = LocalLiquidMenuMotionState.current ?: rememberLiquidMenuMotionState().also {
        ActiveLiquidMenuMotionState.value = it
    }
    val localAnchor = LocalLiquidMenuAnchorBounds.current.value ?: ActiveLiquidMenuAnchorBounds.value
    val effectiveAnchor = anchorBounds ?: localAnchor

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val windowWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val windowHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val statusBarTopPx = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let {
        with(density) { it.toPx() }
    }
    val targetCornerRadiusPx = with(density) { 34.dp.toPx() }

    val initialEstimatedSize = remember(menuWidth, estimatedHeight, density) {
        IntSize(with(density) { menuWidth.roundToPx() }, with(density) { estimatedHeight.roundToPx() })
    }

    val containerCoordsState = remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            val fallbackAnchor = effectiveAnchor ?: Rect(
                offset = Offset(windowWidthPx - with(density) { 64.dp.toPx() }, statusBarTopPx + with(density) { 16.dp.toPx() }),
                size = androidx.compose.ui.geometry.Size(with(density) { 48.dp.toPx() }, with(density) { 48.dp.toPx() })
            )
            motionState.open(
                anchorRect = fallbackAnchor,
                menuSize = initialEstimatedSize,
                windowWidth = windowWidthPx,
                windowHeight = windowHeightPx,
                statusBarTopPx = statusBarTopPx,
                targetCornerRadiusPx = targetCornerRadiusPx
            )
        } else {
            motionState.close()
        }
    }

    LaunchedEffect(effectiveAnchor) {
        if (isVisible && effectiveAnchor != null) {
            motionState.updateAnchorBounds(effectiveAnchor)
        }
    }

    if (motionState.isShowing) {
        BackHandler(enabled = isVisible, onBack = onDismiss)

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

            // 动态圆角：从水滴胶囊平滑校准为 34dp 超椭圆
            val currentRadiusPx = max(motionState.radiusX, with(density) { 16.dp.toPx() })
            val currentCornerRadiusDp = with(density) { currentRadiusPx.toDp() }
            val menuShape = RoundedRectangle(currentCornerRadiusDp)
            val effectiveBackdrop = backdrop ?: remember { emptyBackdrop() }

            val glassRenderer = rememberGlassRenderer()
            val surfaceState = remember { GlassSurfaceState() }
            val canvasDrawScope = remember { CanvasDrawScope() }
            val isGlassActive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassRenderer != null

            var overlayCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

            // 1. 统一纯 GPU 解析式有向距离场流体渲染画布
            if (isGlassActive) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { overlayCoordinates = it }
                ) {
                    motionState.renderTick
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    if (!nativeCanvas.isHardwareAccelerated) return@Canvas

                    motionState.syncToSurfaceState(surfaceState, isDark, effectiveAnchor != null)
                    glassRenderer.prepare(surfaceState)

                    val limits = android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt())
                    glassRenderer.drawGlass(nativeCanvas, surfaceState, limits) { recordingCanvas ->
                        val composeCanvas = androidx.compose.ui.graphics.Canvas(recordingCanvas)
                        canvasDrawScope.draw(
                            density = this,
                            layoutDirection = layoutDirection,
                            canvas = composeCanvas,
                            size = size
                        ) {
                            if (backdrop is LayerBackdrop) {
                                drawLayer(backdrop.graphicsLayer)
                            } else if (backdrop != null) {
                                with(backdrop) {
                                    drawBackdrop(density = this@draw, coordinates = overlayCoordinates, layerBlock = null)
                                }
                            } else {
                                drawRect(if (isDark) NemoSurfaceContainerLowDark else NemoSurfaceBackground)
                            }
                        }
                    }
                }
            }

            // 菲涅尔边缘光渐变（用于降级分支）
            val borderBrush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.04f)
                    )
                }
            )

            val targetLeftDp = with(density) { motionState.targetLeftPx.toDp() }
            val targetTopDp = with(density) { motionState.targetTopPx.toDp() }
            val targetWidthDp = with(density) { motionState.targetWidthPx.toDp() }
            val targetHeightDp = with(density) { motionState.targetHeightPx.toDp() }
            val currentContentBlurDp = with(density) { motionState.contentBlurPx.toDp() }

            // 2. 菜单内容容器
            Box(
                modifier = Modifier
                    .width(targetWidthDp)
                    .height(targetHeightDp)
                    .onGloballyPositioned { coordinates ->
                        containerCoordsState.value = coordinates
                    }
                    .graphicsLayer {
                        motionState.renderTick
                        val tw = max(motionState.targetWidthPx, 1f)
                        val th = max(motionState.targetHeightPx, 1f)
                        val zoom = motionState.contentZoom
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = if (tw > 0f) (motionState.menuWidth / tw) * zoom else 1f
                        scaleY = if (th > 0f) (motionState.menuHeight / th) * zoom else 1f
                        translationX = motionState.menuLeft + motionState.menuWidth * 0.5f * (1f - zoom)
                        translationY = motionState.menuTop + motionState.menuHeight * 0.5f * (1f - zoom)
                        alpha = motionState.contentAlpha.coerceIn(0f, 1f)
                        clip = true
                        shape = menuShape
                    }
                    .then(
                        if (!isGlassActive) {
                            Modifier
                                .drawBackdrop(
                                    backdrop = effectiveBackdrop,
                                    shape = { menuShape },
                                    effects = {
                                        val minDimension = size.minDimension
                                        val dynamicLensHeight = refractionHeightRatio * minDimension + (motionState.lensHeightPx * 0.15f)
                                        colorControls(
                                            brightness = if (isDark) 0.05f else 0.10f,
                                            saturation = 1.35f
                                        )
                                        blur(blurRadius.toPx())
                                        lens(
                                            refractionHeight = dynamicLensHeight,
                                            refractionAmount = refractionAmountRatio * minDimension,
                                            depthEffect = true,
                                            chromaticAberration = false
                                        )
                                    },
                                    highlight = { Highlight.Plain },
                                    onDrawSurface = {}
                                )
                                .background(
                                    color = if (isDark) NemoSurfaceCardDark.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.72f),
                                    shape = menuShape
                                )
                                .border(0.6.dp, borderBrush, menuShape)
                                .clip(menuShape)
                        } else {
                            Modifier
                        }
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                motionState.onPointerDown(offset.x, offset.y)
                                val originX = motionState.menuLeft
                                val originY = motionState.menuTop
                                val winX = originX + offset.x
                                val winY = originY + offset.y
                                motionState.handleDragMove(winX, winY, with(density) { 24.dp.toPx() }) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                motionState.onPointerMove(change.position.x, change.position.y)
                                val originX = motionState.menuLeft
                                val originY = motionState.menuTop
                                val winX = originX + change.position.x
                                val winY = originY + change.position.y
                                motionState.handleDragMove(winX, winY, with(density) { 24.dp.toPx() }) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                motionState.onPointerUp()
                                motionState.handleDragRelease(onSelectDismiss = onDismiss)
                            },
                            onDragCancel = {
                                motionState.onPointerUp()
                                motionState.highlightItem(null, immediate = false)
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 阻断背景点击穿透 */ }
                    )
            ) {
                // 悬浮高亮药丸组件（底层渲染，支持微光菲涅尔描边与 PlateSlide 阻尼弹簧磁吸动画）
                GlassPlate(
                    motionState = motionState,
                    isDark = isDark,
                    modifier = Modifier.fillMaxSize()
                )

                // 3. 菜单条目列表（通过 GlassPortalShader 获得透镜放大与径向模糊动效）
                CompositionLocalProvider(
                    LocalLiquidMenuContainerCoordinates provides containerCoordsState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .verticalScroll(rememberScrollState())
                            .onSizeChanged { size ->
                                motionState.updateContentHeight(size.height.toFloat(), targetCornerRadiusPx, windowHeightPx)
                            }
                            .graphicsLayer {
                                alpha = motionState.contentAlpha
                                if (isGlassActive) {
                                    val warp = motionState.isAnimating &&
                                        (motionState.lensHeightPx > 0.001f || motionState.motion.frame.portalBlur > 0.001f)
                                    renderEffect = if (warp) {
                                        val currentOriginX = motionState.targetLeftPx + motionState.translationX
                                        val currentOriginY = motionState.targetTopPx + motionState.translationY + 10f * density.density
                                        glassRenderer.createPortalRenderEffect(
                                            state = surfaceState,
                                            originX = currentOriginX,
                                            originY = currentOriginY,
                                            portalBlur = motionState.motion.frame.portalBlur * density.density
                                        )?.asComposeRenderEffect()
                                    } else {
                                        null
                                    }
                                } else {
                                    val compScaleX = (motionState.contentZoom / motionState.scaleX.coerceAtLeast(0.01f)).coerceIn(0.1f, 10f)
                                    val compScaleY = (motionState.contentZoom / motionState.scaleY.coerceAtLeast(0.01f)).coerceIn(0.1f, 10f)
                                    scaleX = compScaleX
                                    scaleY = compScaleY
                                }
                                transformOrigin = TransformOrigin.Center
                            }
                            .then(
                                if (!isGlassActive && currentContentBlurDp > 0.5.dp) Modifier.blur(currentContentBlurDp) else Modifier
                            )
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * 液态悬浮菜单微光微透分割线
 */
@Composable
fun NemoLiquidMenuDivider(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    HorizontalDivider(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        color = dividerColor,
        thickness = 0.5.dp
    )
}

/**
 * 液态悬浮菜单分组段落小标题
 *
 * @param text 标题文本
 * @param modifier 修饰符
 */
@Composable
fun NemoLiquidMenuLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}

/**
 * 液态悬浮菜单标准条目
 *
 * @param text 标题文本
 * @param onClick 点击事件
 * @param modifier 修饰符
 * @param subtitle 可选副标题（非空时呈现双行 58dp 排版）
 * @param checked 可选勾选状态（非空时显示 Checkmark）
 * @param reservesCheck 是否预留左侧 Checkmark 对齐槽位（28dp）
 * @param leadingIcon 前置图标
 * @param iconColor 图标强调色
 * @param trailingText 尾部指示文本
 * @param enabled 是否可用
 * @param isDestructive 是否为破坏性操作（高亮切换为警示红）
 */
@Composable
fun NemoLiquidMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    checked: Boolean? = null,
    reservesCheck: Boolean = false,
    leadingIcon: ImageVector? = null,
    iconColor: Color? = null,
    trailingText: String? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val currentMotionState = LocalLiquidMenuMotionState.current ?: ActiveLiquidMenuMotionState.value
    val containerCoords = LocalLiquidMenuContainerCoordinates.current.value
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val destructiveColor = if (isDark) IosColors.RedDark else IosColors.Red

    val effectiveIconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> destructiveColor
        else -> iconColor ?: MaterialTheme.colorScheme.primary
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> destructiveColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemId = remember { Any() }
    val isItemActive = currentMotionState?.activeItemKey == itemId
    val bgAlpha = if ((isPressed || isItemActive) && enabled && currentMotionState?.isPlateVisible != true) 0.08f else 0f
    val itemShape = RoundedRectangle(22.dp)

    DisposableEffect(itemId) {
        onDispose {
            (currentMotionState ?: ActiveLiquidMenuMotionState.value)?.unregisterMenuItem(itemId)
        }
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha),
        shape = itemShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .onGloballyPositioned { coordinates ->
                val boundsInWindow = coordinates.boundsInWindow()
                val boundsInMenu = if (containerCoords != null && containerCoords.isAttached && coordinates.isAttached) {
                    containerCoords.localBoundingBoxOf(coordinates)
                } else {
                    coordinates.boundsInParent()
                }
                (currentMotionState ?: ActiveLiquidMenuMotionState.value)?.registerMenuItem(
                    LiquidMenuItemBounds(
                        id = itemId,
                        boundsInMenu = boundsInMenu,
                        boundsInWindow = boundsInWindow,
                        isEnabled = enabled,
                        onSelect = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    )
                )
            }
    ) {
        val rowVerticalPadding = if (subtitle != null) 8.dp else 10.dp
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = rowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val needsCheckSlot = checked != null || reservesCheck
            if (needsCheckSlot) {
                Box(
                    modifier = Modifier.width(28.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (checked == true) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = effectiveIconColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = effectiveIconColor
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            if (subtitle != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = contentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 液态悬浮菜单带开关条目
 *
 * @param text 标题文本
 * @param checked 开关状态
 * @param onCheckedChange 开关状态变更回调
 * @param modifier 修饰符
 * @param enabled 是否可用
 */
@Composable
fun NemoLiquidMenuToggleItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val currentMotionState = LocalLiquidMenuMotionState.current ?: ActiveLiquidMenuMotionState.value
    val containerCoords = LocalLiquidMenuContainerCoordinates.current.value
    val itemId = remember { Any() }
    val isItemActive = currentMotionState?.activeItemKey == itemId
    val itemShape = RoundedRectangle(22.dp)

    DisposableEffect(itemId) {
        onDispose {
            (currentMotionState ?: ActiveLiquidMenuMotionState.value)?.unregisterMenuItem(itemId)
        }
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
        },
        enabled = enabled,
        color = if (isItemActive && currentMotionState?.isPlateVisible != true) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        shape = itemShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .onGloballyPositioned { coordinates ->
                val boundsInWindow = coordinates.boundsInWindow()
                val boundsInMenu = if (containerCoords != null && containerCoords.isAttached && coordinates.isAttached) {
                    containerCoords.localBoundingBoxOf(coordinates)
                } else {
                    coordinates.boundsInParent()
                }
                (currentMotionState ?: ActiveLiquidMenuMotionState.value)?.registerMenuItem(
                    LiquidMenuItemBounds(
                        id = itemId,
                        boundsInMenu = boundsInMenu,
                        boundsInWindow = boundsInWindow,
                        isEnabled = enabled,
                        onSelect = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCheckedChange(!checked)
                        }
                    )
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            LiquidToggle(
                checked = checked,
                onCheckedChange = onCheckedChange,
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 液态悬浮菜单聚合组件（自动协同管理触发按钮与浮层形变）
 *
 * @param modifier 按钮修饰符
 * @param backdrop 背景采样层
 * @param buttonSize 触发按钮尺寸
 * @param isMenuExpanded 外部展开状态（为 null 时由组件内部管理）
 * @param onToggleMenu 展开状态变更回调
 * @param content 菜单面板内容
 */
@Composable
fun NemoLiquidMenu(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    buttonSize: Dp = 48.dp,
    isMenuExpanded: Boolean = false,
    onToggleMenu: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = if (onToggleMenu != null) isMenuExpanded else internalExpanded
    val setExpanded: (Boolean) -> Unit = { expandedValue ->
        internalExpanded = expandedValue
        onToggleMenu?.invoke(expandedValue)
    }

    val anchorBoundsState = remember { mutableStateOf<Rect?>(null) }
    val motionState = rememberLiquidMenuMotionState()

    LaunchedEffect(motionState) {
        ActiveLiquidMenuMotionState.value = motionState
    }

    CompositionLocalProvider(
        LocalLiquidMenuAnchorBounds provides anchorBoundsState,
        LocalLiquidMenuMotionState provides motionState
    ) {
        NemoLiquidMenuButton(
            isExpanded = expanded,
            onClick = { setExpanded(!expanded) },
            modifier = modifier,
            backdrop = backdrop,
            buttonSize = buttonSize
        )

        NemoLiquidMenuOverlay(
            isVisible = expanded,
            onDismiss = { setExpanded(false) },
            backdrop = backdrop,
            anchorBounds = anchorBoundsState.value,
            content = content
        )
    }
}
