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

    // 缓存几何矩形
    private val sourceRectF = RectF()
    private val throughRectF = RectF()
    private val targetRectF = RectF()
    private var contentHeightLocked = false

    fun updateAnchorBounds(rect: Rect) {
        sourceRectF.set(rect.left, rect.top, rect.right, rect.bottom)
        throughRectF.set(sourceRectF)
        motion.updateSource(sourceRectF, throughRectF)
    }

    internal fun syncToSurfaceState(state: GlassSurfaceState, isDark: Boolean, hasSource: Boolean = true) {
        val frame = motion.frame
        val liveBounds = RectF(frame.bounds)
        jelly.deformMenu(liveBounds, -1f, 1f, density, 1f)

        state.incoming.set(liveBounds)
        val hasValidSource = hasSource && sourceRectF.width() > 0f
        state.captureBounds.set(liveBounds)
        if (hasValidSource) {
            state.captureBounds.union(frame.sourceLobe)
            state.captureBounds.union(sourceRectF)
        }
        state.captureBounds.union(targetRectF)
        val captureMargin = 64f * density
        state.captureBounds.inset(-captureMargin, -captureMargin)

        state.radiusX = frame.radiusX
        state.radiusY = frame.radiusY
        state.hasSource = hasValidSource
        state.source.set(frame.sourceLobe)
        state.sourceRadius = frame.sourceRadius
        state.smoothing = frame.smoothing
        state.lensHeight = frame.lensHeight
        state.lensAmount = frame.lensAmount
        state.materialProgress = frame.contentProgress
        state.surfaceOpacity = if (state.hasSource) 1f else frame.contentProgress
        state.dark = isDark
        val t = frame.contentProgress.coerceIn(0f, 1f)
        state.rimLift = 0.5f * (1f - t)
    }

    fun updateContentHeight(heightPx: Float, targetCornerRadiusPx: Float, windowHeight: Float) {
        if (!motion.initialized || heightPx <= 50f || contentHeightLocked) return
        contentHeightLocked = true
        val currentH = targetRectF.height()
        if (kotlin.math.abs(currentH - heightPx) > 2f) {
            val margin = 16f * density
            val maxBottom = windowHeight - margin
            val naturalTop = if (sourceRectF.bottom > 0f) sourceRectF.bottom + 10f * density else targetRectF.top
            val newTop = if (naturalTop + heightPx > maxBottom) {
                (maxBottom - heightPx).coerceAtLeast(margin)
            } else {
                naturalTop
            }
            targetRectF.top = newTop
            targetRectF.bottom = newTop + heightPx
            targetTopPx = targetRectF.top
            targetHeightPx = targetRectF.height()
            motion.updateTarget(targetRectF, targetCornerRadiusPx)
            renderTick++
            startAnimationLoop()
        }
    }

    fun registerMenuItem(item: LiquidMenuItemBounds) {
        registeredItems[item.id] = item
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
            plateAlpha.target(0f, PlateFade)
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

    fun handleDragRelease(onSelectDismiss: () -> Unit): Boolean {
        val currentId = activeItemId
        val currentItem = if (currentId != null) registeredItems[currentId] else null
        highlightItem(null, immediate = false)
        close()
        return if (currentItem != null && currentItem.isEnabled) {
            currentItem.onSelect()
            onSelectDismiss()
            true
        } else {
            onSelectDismiss()
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
        contentHeightLocked = false
        activeItemId = null
        activeItemKey = null
        plateVisible = false
        isPlateVisible = false
        plateAlpha.reset(0f)
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
        highlightItem(null, immediate = false)
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
        val margin = 16f * density
        val width = max(menuSize.width.toFloat(), 200f * density)
        val height = max(menuSize.height.toFloat(), 48f * density)

        // 默认靠右对齐触发按钮
        val isRightAligned = anchorRect.center.x > windowWidth / 2f
        val left = if (isRightAligned) {
            (anchorRect.right - width).coerceIn(margin, windowWidth - width - margin)
        } else {
            anchorRect.left.coerceIn(margin, windowWidth - width - margin)
        }

        val top = (anchorRect.bottom + 10f * density).coerceIn(
            statusBarTopPx + 8f * density,
            (windowHeight - height - margin).coerceAtLeast(statusBarTopPx + 8f * density)
        )

        targetRectF.set(left, top, left + width, top + height)
        targetLeftPx = left
        targetTopPx = top
        targetWidthPx = width
        targetHeightPx = height
    }

    private fun startAnimationLoop() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        isAnimating = true
        lastFrameTimeNanos = 0L
        renderTick++
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }
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
    val onSelect: () -> Unit
)

