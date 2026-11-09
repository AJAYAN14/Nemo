package com.jian.nemo.core.ui.component.liquid.menu

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.RenderNode
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassJelly
import com.jian.nemo.core.ui.component.liquid.menu.overlay.GlassMenuOverlay
import com.jian.nemo.core.ui.component.liquid.menu.records.GlassMenuItemRecord
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassMaterialConfig
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassRenderer
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassSurfaceState
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassTriggerConfig
import kotlin.math.min

class NemoGlassMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onItemSelect: ((item: GlassMenuItemRecord, index: Int) -> Unit)? = null
    var onMenuOpenChange: ((open: Boolean) -> Unit)? = null

    private var overlay: GlassMenuOverlay? = null
    private var glassRenderer: Any? = null
    private var contentNode: Any? = null
    private var menuActive = false
    val isMenuActive get() = menuActive
    private val density = resources.displayMetrics.density
    internal val jelly = GlassJelly()
    private var pressX = 0f
    private var pressY = 0f
    private var pressTime = 0L
    internal val placement = RectF()
    internal var placementAlpha = -1f

    internal var triggerConfig = GlassTriggerConfig()
        set(value) {
            field = value
            overlay?.invalidate()
        }
    internal var config = GlassMaterialConfig()
        set(value) {
            field = value
            overlay?.invalidate()
        }

    internal val surfaceState = GlassSurfaceState()
    var items: List<GlassMenuItemRecord> = emptyList()
        set(value) {
            field = value
            overlay?.menuSession?.takeIf { it.anchor === this }?.refreshItems()
        }
    var glass = true
        set(value) {
            field = value
            refreshDrawing()
        }
    var cornerRadius: Float? = null
        set(value) {
            field = value
            overlay?.invalidate()
        }
    var colorScheme = "auto"
        set(value) {
            field = value
            overlay?.invalidate()
        }
    var menuWidth: Float? = null
    var fontFamily: String? = null
    var alignment = "auto"
    var openOnPress = true

    private val supportsGlass get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    internal val drawsGlass get() = glass && supportsGlass
    private val overlayDrawsChildren get() = supportsGlass && overlay != null && (glass || menuActive)

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val root = rootView as? ViewGroup ?: return
        overlay = GlassMenuOverlay.obtain(root).also { it.register(this) }
        refreshDrawing()
    }

    override fun onDetachedFromWindow() {
        overlay?.unregister(this)
        overlay = null
        menuActive = false
        if (supportsGlass) releaseGraphics()
        super.onDetachedFromWindow()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun releaseGraphics() {
        (glassRenderer as? GlassRenderer)?.release()
        glassRenderer = null
        (contentNode as? RenderNode)?.discardDisplayList()
        contentNode = null
    }

    private fun refreshDrawing() {
        invalidate()
        overlay?.invalidate()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            pressX = event.x
            pressY = event.y
            pressTime = SystemClock.uptimeMillis()
        }
        if (drawsGlass) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    jelly.down(event.x, event.y)
                    overlay?.requestFrame()
                    if (openOnPress && !menuActive) {
                        open()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    jelly.move(event.x, event.y)
                    overlay?.requestFrame()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    jelly.release()
                    overlay?.requestFrame()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    internal fun applyLight(state: GlassSurfaceState, origin: RectF, opacity: Float) {
        val glow = jelly.glowValue * opacity * triggerConfig.shine
        state.pressX = origin.left + jelly.lightPositionX
        state.pressY = origin.top + jelly.lightPositionY
        state.pressRadius = LightRadius * density
        state.poolGain = LightPool * glow
        state.pressLift = LightLift * glow
        state.rimLift = LightRimLift * glow
    }

    internal fun iconView(slot: Int): View? = null

    private val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }

    private fun drawDefaultEllipsis(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val dotRadius = 2.4f * density
        val spacing = 7.5f * density
        dotPaint.color = if (isDark()) android.graphics.Color.WHITE else android.graphics.Color.rgb(28, 28, 30)
        canvas.drawCircle(cx - spacing, cy, dotRadius, dotPaint)
        canvas.drawCircle(cx, cy, dotRadius, dotPaint)
        canvas.drawCircle(cx + spacing, cy, dotRadius, dotPaint)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (overlayDrawsChildren) return
        if (childCount == 0) {
            drawDefaultEllipsis(canvas)
        }
        super.dispatchDraw(canvas)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal fun recordContent(): RenderNode {
        val node = contentNode as? RenderNode ?: RenderNode("GlassMenu.trigger").also { contentNode = it }
        val w = maxOf(width, 1)
        val h = maxOf(height, 1)
        node.setPosition(0, 0, w, h)
        node.setPivotX(0f)
        node.setPivotY(0f)
        node.setScaleX(1f)
        node.setScaleY(1f)
        node.setTranslationX(0f)
        node.setTranslationY(0f)
        node.setRenderEffect(null)
        val recording = node.beginRecording(w, h)
        if (childCount == 0) {
            drawDefaultEllipsis(recording)
        }
        super.dispatchDraw(recording)
        node.endRecording()
        return node
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal fun renderer(pixelDensity: Float): GlassRenderer =
        glassRenderer as? GlassRenderer ?: GlassRenderer(pixelDensity, pixelDensity * FieldStep).also { glassRenderer = it }

    internal fun isDark(): Boolean = when (colorScheme) {
        "dark" -> true
        "light" -> false
        else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    internal fun cornerRadiusPx(bounds: RectF): Float {
        val capsule = min(bounds.width(), bounds.height()) / 2f
        return min(cornerRadius?.times(density) ?: capsule, capsule)
    }

    internal fun effectiveAlpha(): Float {
        var result = 1f
        var current: View? = this
        while (current != null) {
            result *= current.alpha
            current = current.parent as? View
        }
        return result
    }

    fun open(x: Double? = null, y: Double? = null, width: Double? = null, height: Double? = null) {
        val target = overlay ?: return
        if (items.isEmpty() || this.width == 0 || this.height == 0) return
        if (menuActive && target.menuSession?.expanded == true) return
        val pressed = if (followsPress) pressedRect(target) else null
        val through = pressed ?: if (x != null && y != null && width != null && height != null) {
            RectF(
                (x * density).toFloat(), (y * density).toFloat(),
                ((x + width) * density).toFloat(), ((y + height) * density).toFloat()
            )
        } else null
        menuActive = true
        target.open(this, through)
        invalidate()
    }

    internal val followsPress get() = false

    private fun pressedRect(target: GlassMenuOverlay): RectF? {
        if (SystemClock.uptimeMillis() - pressTime > PressMemory) return null
        val bounds = RectF()
        target.anchorRect(this, bounds)
        val half = min(bounds.height() / 2f, PressRadius * density)
        val centerX = (bounds.left + pressX).coerceIn(bounds.left + half, bounds.right - half)
        val centerY = (bounds.top + pressY).coerceIn(bounds.top + half, bounds.bottom - half)
        return RectF(centerX - half, centerY - half, centerX + half, centerY + half)
    }

    fun close() {
        overlay?.takeIf { it.menuSession?.anchor === this }?.requestClose()
    }

    internal fun notifySelect(item: GlassMenuItemRecord, index: Int) {
        onItemSelect?.invoke(item, index)
    }

    internal fun notifyOpenChange(open: Boolean) {
        onMenuOpenChange?.invoke(open)
    }

    internal fun onSessionFinished() {
        menuActive = false
        invalidate()
    }

    private companion object {
        const val FieldStep = 4f
        const val PressMemory = 1000L
        const val PressRadius = 22f
        const val LightRadius = 38f
        const val LightLift = 0.055f
        const val LightPool = 0.07f
        const val LightRimLift = 0.3f
    }
}
