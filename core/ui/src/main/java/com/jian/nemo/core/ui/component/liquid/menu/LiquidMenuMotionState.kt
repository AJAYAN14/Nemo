package com.jian.nemo.core.ui.component.liquid.menu

import android.graphics.RectF
import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassJelly
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassMorphTuning
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassMotion
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassSurfaceState
import kotlinx.coroutines.CoroutineScope
import kotlin.math.max
import kotlin.math.min

/**
 * 驱动液态菜单流体形变与动力学时钟的控制器状态
 */
@Stable
class LiquidMenuMotionState internal constructor(
    private val scope: CoroutineScope,
    private val density: Float
) {
    internal val motion: GlassMotion = GlassMotion()
    internal val jelly: GlassJelly = GlassJelly()

    var isExpanded by mutableStateOf(false)
        private set

    var isShowing by mutableStateOf(false)
        private set

    var isAnimating by mutableStateOf(false)
        private set

    var renderTick by mutableLongStateOf(0L)
        private set

    // 目标静态边界（像素），用于 Draw/GraphicsLayer 阶段零重排基准
    var targetLeftPx by mutableFloatStateOf(0f)
        private set
    var targetTopPx by mutableFloatStateOf(0f)
        private set
    var targetWidthPx by mutableFloatStateOf(0f)
        private set
    var targetHeightPx by mutableFloatStateOf(0f)
        private set

    // GPU 图层变换属性（零 Measure 零 Layout 开销）
    var scaleX by mutableFloatStateOf(1f)
        private set
    var scaleY by mutableFloatStateOf(1f)
        private set
    var translationX by mutableFloatStateOf(0f)
        private set
    var translationY by mutableFloatStateOf(0f)
        private set

    // 当前菜单面板的实时几何位置与大小（像素）
    var menuLeft by mutableFloatStateOf(0f)
        private set
    var menuTop by mutableFloatStateOf(0f)
        private set
    var menuWidth by mutableFloatStateOf(0f)
        private set
    var menuHeight by mutableFloatStateOf(0f)
        private set

    // 实时曲率半径（像素）
    var radiusX by mutableFloatStateOf(0f)
        private set
    var radiusY by mutableFloatStateOf(0f)
        private set

    // 触发按钮实时的形变缩放、透明度与后坐力位移
    var sourceScale by mutableFloatStateOf(1f)
        private set
    var sourceAlpha by mutableFloatStateOf(1f)
        private set
    var sourceOffsetY by mutableFloatStateOf(0f)
        private set

    // 内容透明度、模糊度与等比视口缩放
    var contentAlpha by mutableFloatStateOf(0f)
        private set
    var contentBlurPx by mutableFloatStateOf(0f)
        private set
    var contentZoom by mutableFloatStateOf(1f)
        private set

    // 动态透镜隆起高度（像素）
    var lensHeightPx by mutableFloatStateOf(0f)
        private set

    // 菜单展开极性与视口最大高度限制
    var opensAbove by mutableStateOf(false)
        private set
    var opensFromRight by mutableStateOf(true)
        private set
    var maximumHeightPx by mutableFloatStateOf(0f)
        private set

    // 液体粘连融合系数（像素）
    var smoothing by mutableFloatStateOf(0f)
        private set

    // 果冻手势交互偏移与缩放量
    var jellyOffsetX by mutableFloatStateOf(0f)
        private set
    var jellyOffsetY by mutableFloatStateOf(0f)
        private set

    internal val plateTop = com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpring()
    internal val plateHeight = com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpring()
    internal val plateAlpha = com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpring(0.001f, 0.01f)

    private var plateVisible = false
    private var activeItemId: Any? = null
    private val registeredItems = mutableMapOf<Any, LiquidMenuItemBounds>()

    var plateTopPx by mutableFloatStateOf(0f)
        private set
    var plateHeightPx by mutableFloatStateOf(0f)
        private set
    var plateAlphaVal by mutableFloatStateOf(0f)
        private set
    var plateLightXPx by mutableFloatStateOf(0f)
        private set
    var isPlateVisible by mutableStateOf(false)
        private set
    var activeItemKey by mutableStateOf<Any?>(null)
        private set

    val sourceCenterX: Float
        get() = if (motion.initialized) motion.frame.sourceLobe.centerX() else sourceRectF.centerX()
    val sourceCenterY: Float
        get() = if (motion.initialized) motion.frame.sourceLobe.centerY() else sourceRectF.centerY()
    val sourceRadius: Float
        get() = if (motion.initialized) motion.frame.sourceRadius else sourceRectF.width() / 2f

    private var lastFrameTimeNanos: Long = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isAnimating) return
            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = frameTimeNanos
            }
            val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastFrameTimeNanos = frameTimeNanos

            val settled = motion.advanceBy(dt)
            jelly.advance(dt)
            plateTop.advance(dt)
            plateHeight.advance(dt)
            plateAlpha.advance(dt)

            plateTopPx = plateTop.value
            plateHeightPx = plateHeight.value
            plateAlphaVal = plateAlpha.value

            // 同步状态到 Compose Snapshot
            val frame = motion.frame
            val liveBounds = RectF(frame.bounds)
            jelly.deformMenu(liveBounds, -1f, 1f, density, 1f)

            jellyOffsetX = liveBounds.centerX() - frame.bounds.centerX()
            jellyOffsetY = liveBounds.centerY() - frame.bounds.centerY()

            menuLeft = liveBounds.left
            menuTop = liveBounds.top
            menuWidth = liveBounds.width()
            menuHeight = liveBounds.height()
            radiusX = frame.radiusX
            radiusY = frame.radiusY
            smoothing = frame.smoothing

            val tw = max(targetRectF.width(), 1f)
            val th = max(targetRectF.height(), 1f)
            scaleX = (liveBounds.width() / tw).coerceIn(0.01f, 1.5f)
            scaleY = (liveBounds.height() / th).coerceIn(0.01f, 1.5f)
            translationX = liveBounds.centerX() - targetRectF.centerX()
            translationY = liveBounds.centerY() - targetRectF.centerY()

            val sourceW = frame.sourceLobe.width()
            val originalW = max(sourceRectF.width(), 1f)
            sourceScale = sourceW / originalW
            sourceAlpha = frame.sourceOpacity
            sourceOffsetY = frame.sourceLobe.centerY() - sourceRectF.centerY()

            contentAlpha = frame.rowsOpacity
            contentBlurPx = frame.contentBlur
            contentZoom = frame.contentZoom
            lensHeightPx = frame.lensHeight

            renderTick++

            val plateSettled = plateTop.isSettled && plateHeight.isSettled && plateAlpha.isSettled
            val allSettled = settled && jelly.isSettled && plateSettled
            if (allSettled) {
                isAnimating = false
                if (!isExpanded) {
                    isShowing = false
                }
            } else {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    // 缓存几何矩形与视口边界
    internal val sourceRectF = RectF()
    internal val throughRectF = RectF()
    internal val targetRectF = RectF()
    private var cachedStatusBarTop = 0f

    fun updateAnchorBounds(
        rect: Rect,
        menuSize: IntSize? = null,
        windowWidth: Float = 0f,
        windowHeight: Float = 0f,
        statusBarTopPx: Float = 0f,
        targetCornerRadiusPx: Float = 0f
    ) {
        sourceRectF.set(rect.left, rect.top, rect.right, rect.bottom)
        throughRectF.set(sourceRectF)
        motion.updateSource(sourceRectF, throughRectF)
        if (menuSize != null && windowWidth > 0f) {
            calculateTargetRect(rect, menuSize, windowWidth, windowHeight, statusBarTopPx)
            motion.updateTarget(targetRectF, targetCornerRadiusPx)
        }
    }

    fun updateTargetSize(
        menuSize: IntSize,
        windowWidth: Float,
        windowHeight: Float,
        statusBarTopPx: Float,
        targetCornerRadiusPx: Float
    ) {
        if (!motion.initialized || sourceRectF.isEmpty) return
        calculateTargetRect(
            anchorRect = Rect(sourceRectF.left, sourceRectF.top, sourceRectF.right, sourceRectF.bottom),
            menuSize = menuSize,
            windowWidth = windowWidth,
            windowHeight = windowHeight,
            statusBarTopPx = statusBarTopPx
        )
        motion.updateTarget(targetRectF, targetCornerRadiusPx)
        renderTick++
        startAnimationLoop()
    }

    internal fun syncToSurfaceState(state: GlassSurfaceState, isDark: Boolean, hasSource: Boolean = true) {
        val frame = motion.frame
        val liveBounds = RectF(frame.bounds)
        jelly.deformMenu(
            liveBounds,
            if (opensFromRight) -1f else 1f,
            if (opensAbove) -1f else 1f,
            density,
            1f
        )

        state.incoming.set(liveBounds)
        val hasValidSource = hasSource && sourceRectF.width() > 0f
        // 关键修复：当菜单展开且动画完成、次级实体已经完全隐去且拉丝断开时，关闭次级实体 SDF 并集计算，消除右上角小圆凸起
        val isSecondaryActive = hasValidSource && (!isExpanded || frame.sourceOpacity > 0.005f || frame.smoothing > 0.5f)
        state.captureBounds.set(liveBounds)
        if (isSecondaryActive) {
            state.captureBounds.union(frame.sourceLobe)
            state.captureBounds.union(sourceRectF)
        }
        state.captureBounds.union(targetRectF)
        val captureMargin = 48f * density
        state.captureBounds.inset(-captureMargin, -captureMargin)
        val neckSigma = 20f * density
        state.lockedFieldStep = max(
            neckSigma / 4f,
            max(state.captureBounds.width(), state.captureBounds.height()) / 64f
        )

        state.radiusX = frame.radiusX
        state.radiusY = frame.radiusY
        state.hasSource = isSecondaryActive
        state.source.set(frame.sourceLobe)
        state.sourceRadius = frame.sourceRadius
        state.smoothing = frame.smoothing
        state.lensHeight = frame.lensHeight
        state.lensAmount = frame.lensAmount
        state.materialProgress = frame.contentProgress
        state.surfaceOpacity = if (isSecondaryActive) 1f else frame.contentProgress
        state.dark = isDark
        val t = frame.contentProgress.coerceIn(0f, 1f)
        val glow = jelly.glowValue * t
        state.pressX = jelly.lightPositionX
        state.pressY = jelly.lightPositionY
        state.pressRadius = 80f * density
        state.poolGain = 0.03f * glow
        state.pressLift = 0.018f * glow
        state.rimLift = 0.12f * glow + 0.5f * (1f - t)
    }

    fun updateContentHeight(heightPx: Float, targetCornerRadiusPx: Float, windowHeight: Float) {
        if (!motion.initialized || heightPx <= 50f) return
        val currentH = targetRectF.height()
        if (heightPx <= currentH) return
        val (newTop, constrainedHeight) = calculateUpdatedContentHeight(
            newHeightPx = heightPx,
            maximumHeightPx = maximumHeightPx,
            anchorTop = sourceRectF.top,
            anchorBottom = sourceRectF.bottom,
            opensAbove = opensAbove,
            statusBarTopPx = cachedStatusBarTop,
            windowHeight = windowHeight,
            density = density
        )
        if (kotlin.math.abs(currentH - constrainedHeight) > 1f) {
            targetRectF.top = newTop
            targetRectF.bottom = newTop + constrainedHeight
            targetTopPx = targetRectF.top
            targetHeightPx = targetRectF.height()
            motion.updateTarget(targetRectF, targetCornerRadiusPx)
            renderTick++
            startAnimationLoop()
        }
    }

    fun registerMenuItem(item: LiquidMenuItemBounds) {
        val existing = registeredItems[item.id]
        if (existing != null) {
            registeredItems[item.id] = existing.copy(
                boundsInMenu = item.boundsInMenu,
                boundsInWindow = item.boundsInWindow,
                isEnabled = item.isEnabled,
                keepOpen = item.keepOpen,
                onSelect = item.onSelect
            )
        } else {
            registeredItems[item.id] = item
        }
    }

    fun updateMenuItem(
        id: Any,
        isEnabled: Boolean,
        keepOpen: Boolean,
        onSelect: () -> Unit
    ) {
        val existing = registeredItems[id]
        if (existing != null) {
            registeredItems[id] = existing.copy(
                isEnabled = isEnabled,
                keepOpen = keepOpen,
                onSelect = onSelect
            )
        }
    }

    fun unregisterMenuItem(id: Any) {
        registeredItems.remove(id)
        if (activeItemId == id) {
            highlightItem(null, immediate = false)
        }
    }

    fun highlightItem(
        item: LiquidMenuItemBounds?,
        immediate: Boolean = false,
        onHapticTick: (() -> Unit)? = null
    ) {
        if (item?.id == activeItemId) return
        val hadItem = activeItemId != null
        activeItemId = item?.id
        activeItemKey = item?.id

        if (item == null) {
            plateVisible = false
            isPlateVisible = false
            if (immediate) {
                plateAlpha.reset(0f)
                plateAlphaVal = 0f
            } else {
                plateAlpha.target(0f, PlateFade)
            }
        } else {
            if (!plateVisible && !hadItem) {
                plateTop.reset(item.boundsInMenu.top)
                plateHeight.reset(item.boundsInMenu.height)
            } else {
                plateTop.target(item.boundsInMenu.top, PlateSlide)
                plateHeight.target(item.boundsInMenu.height, PlateSlide)
            }
            onHapticTick?.invoke()
            plateVisible = true
            isPlateVisible = true
            if (immediate) plateAlpha.reset(1f) else plateAlpha.target(1f, PlateFade)
        }
        startAnimationLoop()
    }

    fun highlightItemById(id: Any?, immediate: Boolean = false) {
        val item = id?.let { registeredItems[it] }
        highlightItem(item, immediate)
    }

    fun findNearestItem(windowX: Float, windowY: Float, slackPx: Float): LiquidMenuItemBounds? {
        if (registeredItems.isEmpty()) return null
        val menuL = targetLeftPx + translationX
        val menuR = menuL + targetWidthPx
        if (windowX < menuL - slackPx || windowX > menuR + slackPx) {
            return null
        }
        var best: LiquidMenuItemBounds? = null
        var minDistance = Float.MAX_VALUE
        val liveMenuTop = targetTopPx + translationY
        for (item in registeredItems.values) {
            if (!item.isEnabled) continue
            val itemLiveTop = liveMenuTop + item.boundsInMenu.top
            val itemLiveBottom = liveMenuTop + item.boundsInMenu.bottom
            val gap = when {
                windowY < itemLiveTop -> itemLiveTop - windowY
                windowY > itemLiveBottom -> windowY - itemLiveBottom
                else -> 0f
            }
            if (gap < minDistance) {
                minDistance = gap
                best = item
            }
        }
        return if (minDistance <= slackPx) best else null
    }

    fun handleDragMove(windowX: Float, windowY: Float, slackPx: Float, onHapticTick: (() -> Unit)? = null) {
        val nearest = findNearestItem(windowX, windowY, slackPx)
        val menuL = targetLeftPx + translationX
        plateLightXPx = windowX - menuL
        highlightItem(nearest, immediate = false, onHapticTick = onHapticTick)
    }

    fun handleDragRelease(
        dismissIfNoSelection: Boolean = true,
        onSelectDismiss: () -> Unit
    ): Boolean {
        val currentId = activeItemId
        val currentItem = if (currentId != null) registeredItems[currentId] else null
        highlightItem(null, immediate = false)
        return if (currentItem != null && currentItem.isEnabled) {
            currentItem.onSelect()
            if (!currentItem.keepOpen) {
                close()
                onSelectDismiss()
            }
            true
        } else {
            if (dismissIfNoSelection) {
                close()
                onSelectDismiss()
            }
            false
        }
    }

    fun open(
        anchorRect: Rect,
        menuSize: IntSize,
        windowWidth: Float,
        windowHeight: Float,
        statusBarTopPx: Float,
        targetCornerRadiusPx: Float
    ) {
        activeItemId = null
        activeItemKey = null
        plateVisible = false
        isPlateVisible = false
        plateAlpha.reset(0f)
        plateTop.reset(0f)
        plateHeight.reset(0f)
        plateTopPx = 0f
        plateHeightPx = 0f
        plateAlphaVal = 0f
        updateAnchorBounds(anchorRect)
        calculateTargetRect(anchorRect, menuSize, windowWidth, windowHeight, statusBarTopPx)

        motion.initialize(
            sourceRect = sourceRectF,
            target = targetRectF,
            targetRadius = targetCornerRadiusPx,
            pixelDensity = density,
            throughRect = throughRectF,
            fromPress = false,
            morphTuning = GlassMorphTuning()
        )

        isExpanded = true
        isShowing = true
        motion.retarget(true)
        startAnimationLoop()
    }

    fun close() {
        if (!isExpanded && !isShowing) return
        isExpanded = false
        activeItemId = null
        activeItemKey = null
        plateVisible = false
        isPlateVisible = false
        plateAlpha.reset(0f)
        plateTopPx = 0f
        plateHeightPx = 0f
        plateAlphaVal = 0f
        motion.retarget(false)
        jelly.release()
        startAnimationLoop()
    }

    fun onPointerDown(x: Float, y: Float) {
        jelly.down(x, y)
        startAnimationLoop()
    }

    fun onPointerMove(x: Float, y: Float) {
        jelly.move(x, y)
        startAnimationLoop()
    }

    fun onPointerUp() {
        jelly.release()
        startAnimationLoop()
    }

    private fun calculateTargetRect(
        anchorRect: Rect,
        menuSize: IntSize,
        windowWidth: Float,
        windowHeight: Float,
        statusBarTopPx: Float
    ) {
        cachedStatusBarTop = statusBarTopPx
        val geometry = calculateLiquidMenuGeometry(
            anchorLeft = anchorRect.left,
            anchorTop = anchorRect.top,
            anchorRight = anchorRect.right,
            anchorBottom = anchorRect.bottom,
            menuWidth = menuSize.width.toFloat(),
            menuHeight = menuSize.height.toFloat(),
            windowWidth = windowWidth,
            windowHeight = windowHeight,
            statusBarTopPx = statusBarTopPx,
            density = density
        )
        opensAbove = geometry.opensAbove
        opensFromRight = geometry.opensFromRight
        maximumHeightPx = geometry.maximumHeightPx
        targetLeftPx = geometry.left
        targetTopPx = geometry.top
        targetWidthPx = geometry.width
        targetHeightPx = geometry.height
        targetRectF.set(geometry.left, geometry.top, geometry.left + geometry.width, geometry.top + geometry.height)
    }

    private fun startAnimationLoop() {
        try {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            isAnimating = true
            lastFrameTimeNanos = 0L
            renderTick++
            Choreographer.getInstance().postFrameCallback(frameCallback)
        } catch (_: Throwable) {
            isAnimating = true
            renderTick++
        }
    }
}

/**
 * 液态菜单目标几何计算结果
 */
data class LiquidMenuTargetGeometry(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val opensAbove: Boolean,
    val opensFromRight: Boolean,
    val maximumHeightPx: Float
)

/**
 * 按照 Demo Session 几何模型计算菜单展开的目标区域
 */
internal fun calculateLiquidMenuGeometry(
    anchorLeft: Float,
    anchorTop: Float,
    anchorRight: Float,
    anchorBottom: Float,
    menuWidth: Float,
    menuHeight: Float,
    windowWidth: Float,
    windowHeight: Float,
    statusBarTopPx: Float,
    density: Float
): LiquidMenuTargetGeometry {
    val margin = 16f * density
    val topBound = statusBarTopPx
    val bottomBound = windowHeight - margin
    val maximumHeightPx = maxOf(bottomBound - topBound - margin, 100f * density)

    val height = minOf(maxOf(menuHeight, 48f * density), maximumHeightPx)
    val width = minOf(maxOf(menuWidth, 200f * density), windowWidth - margin * 2f)

    val fromRight = (anchorLeft + anchorRight) / 2f > windowWidth / 2f
    val left = (if (fromRight) anchorRight - width else anchorLeft)
        .coerceIn(margin, maxOf(windowWidth - width - margin, margin))

    val roomBelow = bottomBound - anchorTop
    val roomAbove = anchorBottom - topBound
    val isAnchorInTopHalf = anchorTop < windowHeight * 0.5f
    val above = !isAnchorInTopHalf && roomBelow < height && roomAbove > roomBelow

    val menuTop = (if (above) anchorBottom - height else anchorTop)
        .coerceIn(topBound, maxOf(bottomBound - height, topBound))

    return LiquidMenuTargetGeometry(
        left = left,
        top = menuTop,
        width = width,
        height = height,
        opensAbove = above,
        opensFromRight = fromRight,
        maximumHeightPx = maximumHeightPx
    )
}

/**
 * 按照单边锚定规则计算动态内容高度变化后的新几何位置
 */
internal fun calculateUpdatedContentHeight(
    newHeightPx: Float,
    maximumHeightPx: Float,
    anchorTop: Float,
    anchorBottom: Float,
    opensAbove: Boolean,
    statusBarTopPx: Float,
    windowHeight: Float,
    density: Float
): Pair<Float, Float> {
    val constrainedHeight = minOf(
        newHeightPx,
        if (maximumHeightPx > 0f) maximumHeightPx else windowHeight - statusBarTopPx - 16f * density
    )
    val margin = 16f * density
    val topBound = statusBarTopPx
    val bottomBound = windowHeight - margin

    val newTop = if (opensAbove) {
        (anchorBottom - constrainedHeight).coerceIn(topBound, maxOf(bottomBound - constrainedHeight, topBound))
    } else {
        anchorTop.coerceIn(topBound, maxOf(bottomBound - constrainedHeight, topBound))
    }
    return Pair(newTop, constrainedHeight)
}

/**
 * 创建并记忆一个 LiquidMenuMotionState
 */
@Composable
fun rememberLiquidMenuMotionState(): LiquidMenuMotionState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    return remember(scope, density) {
        LiquidMenuMotionState(scope, density)
    }
}

/**
 * 液态菜单条目位置与点击交互模型
 */
data class LiquidMenuItemBounds(
    val id: Any,
    val boundsInMenu: Rect,
    val boundsInWindow: Rect = Rect.Zero,
    val isEnabled: Boolean = true,
    val keepOpen: Boolean = false,
    val onSelect: () -> Unit
)

