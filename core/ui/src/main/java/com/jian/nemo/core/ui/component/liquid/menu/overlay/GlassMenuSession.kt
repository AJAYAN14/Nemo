package com.jian.nemo.core.ui.component.liquid.menu.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.view.MotionEvent
import com.jian.nemo.core.ui.component.liquid.menu.NemoGlassMenuView
import com.jian.nemo.core.ui.component.liquid.menu.content.GlassMenuContent
import com.jian.nemo.core.ui.component.liquid.menu.content.GlassRow
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassAbsorbConfig
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassJelly
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassMorphTuning
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassMotion
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassShrinkConfig
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpring
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpringSpec
import com.jian.nemo.core.ui.component.liquid.menu.render.GlassSurfaceState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class GlassMenuSession(
    private val overlay: GlassMenuOverlay,
    val anchor: NemoGlassMenuView,
    private val density: Float,
    private val supportsGlass: Boolean,
    through: RectF?,
    windowWidth: Int,
    windowHeight: Int,
    insets: Rect
) {
    private val motion = GlassMotion()
    private val content = GlassMenuContent(overlay.context, density) { slot -> anchor.iconView(slot) }
    private val target = RectF()
    private val source = RectF()
    private val throughRect = RectF()
    private val state = GlassSurfaceState()
    private val rowsNode: RenderNode? = if (supportsGlass) RenderNode("GlassMenu.rows") else null
    private var rowsDirty = true
    private val shown = RectF()
    private val captureBounds = RectF()
    private var lockedFieldStep = 0f

    private val plateTop = GlassSpring(0.25f, 8f)
    private val plateHeight = GlassSpring(0.25f, 8f)
    private val plateAlpha = GlassSpring(0.004f, 0.05f)
    private val jelly = GlassJelly()
    private val plateRect = RectF()
    private val plateNode: RenderNode? = if (supportsGlass) RenderNode("GlassMenu.plate") else null
    private var plateVisible = false
    private var activeRow: GlassRow? = null

    private var scroll = 0f
    private var maximumScroll = 0f
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartScroll = 0f
    private var scrolling = false
    private var touchInside = false
    private var animating = true
    private var dismissOnUp = false
    private val availableWidth: Float
    private val maximumHeight: Float
    private val opensAbove: Boolean
    private val opensFromRight: Boolean

    var expanded = true
        private set
    val needsFrame: Boolean
        get() = animating || !plateAlpha.isSettled || !plateTop.isSettled || !plateHeight.isSettled ||
            !jelly.isSettled

    init {
        val margin = 16f * density
        availableWidth = windowWidth - margin * 2f
        content.dark = anchor.isDark()
        content.setItems(anchor.items, anchor.menuWidth, availableWidth, anchor.fontFamily)
        overlay.anchorRect(anchor, source)
        throughRect.set(through ?: source)

        val top = insets.top.toFloat()
        val bottom = windowHeight - insets.bottom.toFloat()
        maximumHeight = bottom - top - margin
        val height = min(content.height, maximumHeight)
        maximumScroll = max(content.height - height, 0f)
        val width = content.width
        val alignment = anchor.alignment
        opensFromRight = alignment == "end" || alignment == "auto" && throughRect.centerX() > windowWidth / 2f
        val left = when {
            alignment == "center" -> throughRect.centerX() - width / 2f
            opensFromRight -> source.right - width
            else -> source.left
        }.coerceIn(margin, max(windowWidth - width - margin, margin))
        val roomBelow = bottom - source.top
        val roomAbove = source.bottom - top
        opensAbove = roomBelow < height && roomAbove > roomBelow
        val menuTop = (if (opensAbove) source.bottom - height else source.top)
            .coerceIn(top, max(bottom - height, top))
        target.set(left, menuTop, left + width, menuTop + height)
        updateCaptureBounds()

        motion.initialize(
            source, target, CornerRadius * density, density, throughRect, anchor.followsPress,
            GlassMorphTuning(
                shrink = GlassShrinkConfig(duration = 0.21f, scale = 0.25f),
                absorb = GlassAbsorbConfig(duration = 0.35f, damping = 0.8f)
            )
        )
        motion.retarget(true)
        plateAlpha.reset(0f)
        plateTop.reset(0f)
        plateHeight.reset(0f)
    }

    fun refreshItems() {
        if (!expanded) return
        content.setItems(anchor.items, anchor.menuWidth, availableWidth, anchor.fontFamily)
        val height = min(content.height, maximumHeight)
        maximumScroll = max(content.height - height, 0f)
        scroll = min(scroll, maximumScroll)
        activeRow = null
        plateVisible = false
        plateAlpha.reset(0f)
        rowsDirty = true
        if (height != target.height() || content.width != target.width()) {
            val left = if (opensFromRight) target.right - content.width else target.left
            val top = if (opensAbove) target.bottom - height else target.top
            target.set(left, top, left + content.width, top + height)
            updateCaptureBounds()
            motion.updateTarget(target, CornerRadius * density)
            animating = true
        }
        overlay.requestFrame()
        overlay.invalidate()
    }

    fun reopen() {
        expanded = true
        animating = true
        rowsDirty = true
        content.setItems(anchor.items, anchor.menuWidth, availableWidth, anchor.fontFamily)
        motion.retarget(true)
    }

    fun close() {
        expanded = false
        animating = true
        plateVisible = false
        plateAlpha.target(0f, PlateFade)
        jelly.release()
        overlay.anchorRect(anchor, source)
        motion.updateSource(source, if (throughRect.isEmpty) source else throughRect)
        motion.retarget(false)
    }

    fun tick(seconds: Float): Boolean {
        val settled = if (animating) motion.advanceBy(seconds) else true
        if (settled) animating = false
        plateTop.advance(seconds)
        plateHeight.advance(seconds)
        plateAlpha.advance(seconds)
        jelly.advance(seconds)
        return settled && !expanded
    }

    private fun updateCaptureBounds() {
        captureBounds.set(source)
        captureBounds.union(target)
        captureBounds.inset(-CaptureMargin * density, -CaptureMargin * density)
        lockedFieldStep = max(
            NeckSigma * density / FieldCellsPerSigma,
            max(captureBounds.width(), captureBounds.height()) / FieldCells
        )
    }

    private fun liveBounds(out: RectF) {
        out.set(motion.frame.bounds)
        jelly.deformMenu(out, if (opensFromRight) -1f else 1f, if (opensAbove) -1f else 1f, density, anchor.config.jelly)
    }

    fun draw(canvas: Canvas, backdrop: RenderNode, limits: Rect) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val frame = motion.frame
        liveBounds(shown)
        state.incoming.set(shown)
        state.captureBounds.set(captureBounds)
        state.lockedFieldStep = lockedFieldStep
        state.radiusX = frame.radiusX
        state.radiusY = frame.radiusY
        state.hasSource = anchor.glass
        state.source.set(frame.sourceLobe)
        val swell = anchor.jelly.swell(anchor.height.toFloat(), density, anchor.triggerConfig.jelly) - 1f
        state.source.inset(-state.source.width() * swell / 2f, -state.source.height() * swell / 2f)
        state.sourceRadius = min(state.source.width(), state.source.height()) / 2f
        state.smoothing = frame.smoothing
        state.lensHeight = frame.lensHeight
        state.lensAmount = frame.lensAmount
        state.materialProgress = frame.contentProgress
        state.surfaceOpacity = if (anchor.glass) 1f else frame.contentProgress
        state.dark = content.dark
        state.config = anchor.config
        state.trigger = anchor.triggerConfig
        applyLight(frame.contentProgress)
        val renderer = anchor.renderer(density)
        renderer.prepare(state)
        renderer.drawGlass(canvas, backdrop, state, limits)

        val rows = recordRows() ?: return
        val width = target.width()
        val height = target.height()
        val zoom = frame.contentZoom
        rows.setPivotX(0f)
        rows.setPivotY(0f)
        rows.setScaleX(shown.width() / width * zoom)
        rows.setScaleY(shown.height() / height * zoom)
        rows.setTranslationX(shown.left + shown.width() * 0.5f * (1f - zoom))
        rows.setTranslationY(shown.top + shown.height() * 0.5f * (1f - zoom))
        rows.setAlpha(frame.rowsOpacity)
        val morphBlur = anchor.config.morphBlur
        rows.setRenderEffect(blur(frame.contentBlur * morphBlur / max(min(rows.scaleX, rows.scaleY), MinimumRowsScale)))

        val trigger = anchor.recordContent()
        val lobe = state.source
        val sourceZoom = frame.sourceZoom
        val triggerWidth = max(anchor.width, 1)
        val triggerHeight = max(anchor.height, 1)
        trigger.setPivotX(triggerWidth / 2f)
        trigger.setPivotY(triggerHeight / 2f)
        trigger.setScaleX(lobe.width() / triggerWidth * sourceZoom)
        trigger.setScaleY(lobe.height() / triggerHeight * sourceZoom)
        trigger.setTranslationX(lobe.centerX() - triggerWidth / 2f)
        trigger.setTranslationY(lobe.centerY() - triggerHeight / 2f)
        trigger.setAlpha(frame.sourceOpacity)
        trigger.setRenderEffect(blur(frame.sourceBlur))

        val plate = recordPlate()
        if (plate != null) {
            plate.setPivotX(0f)
            plate.setPivotY(0f)
            plate.setScaleX(rows.scaleX)
            plate.setScaleY(rows.scaleY)
            plate.setTranslationX(rows.translationX)
            plate.setTranslationY(rows.translationY)
            plate.setAlpha(frame.rowsOpacity)
        }
        renderer.drawForeground(canvas, state, animating, frame.portalBlur * morphBlur) { recording ->
            if (anchor.glass && frame.sourceOpacity > 0.001f) recording.drawRenderNode(trigger)
            if (frame.rowsOpacity > 0.001f) {
                if (plate != null) recording.drawRenderNode(plate)
                recording.drawRenderNode(rows)
            }
        }
        if (!anchor.glass && frame.sourceOpacity > 0.001f) canvas.drawRenderNode(trigger)
    }

    private fun blur(radius: Float): RenderEffect? =
        if (radius > 0.01f) RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL) else null

    private fun applyLight(progress: Float) {
        val shine = anchor.config.shine
        val glow = jelly.glowValue * shine * progress
        if (glow > anchor.jelly.glowValue * (1f - progress)) {
            state.pressX = jelly.lightPositionX
            state.pressY = jelly.lightPositionY
            state.pressRadius = MenuLightRadius * density
            state.poolGain = MenuPool * glow
            state.pressLift = MenuLift * glow
            state.rimLift = MenuRimLift * glow
        } else {
            anchor.applyLight(state, source, 1f - progress)
        }
    }

    private fun recordRows(): RenderNode? {
        val node = rowsNode ?: return null
        val width = max(target.width().toInt(), 1)
        val height = max(target.height().toInt(), 1)
        if (rowsDirty || !node.hasDisplayList()) {
            rowsDirty = false
            node.setPosition(0, 0, width, height)
            content.draw(node.beginRecording(width, height), scroll)
            node.endRecording()
        }
        return node
    }

    private fun recordPlate(): RenderNode? {
        val node = plateNode ?: return null
        if (plateAlpha.value <= 0.001f) return null
        val width = max(target.width().toInt(), 1)
        val height = max(target.height().toInt(), 1)
        node.setPosition(0, 0, width, height)
        val recording = node.beginRecording(width, height)
        updatePlateRect()
        content.drawPlate(
            recording, plateRect, plateAlpha.value, jelly.lightPositionX - target.left,
            anchor.config.highlightOpacity, anchor.config.shine
        )
        node.endRecording()
        return node
    }

    private fun updatePlateRect() {
        val top = plateTop.value - scroll
        plateRect.set(PlateInset * density, top, target.width() - PlateInset * density, top + plateHeight.value)
    }

    fun drawFallback(canvas: Canvas) {
        val frame = motion.frame
        liveBounds(shown)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (content.dark) Color.rgb(30, 30, 32) else Color.rgb(245, 245, 247)
            alpha = (255 * frame.contentProgress).toInt()
        }
        val radius = min(frame.radiusX, frame.radiusY)
        canvas.drawRoundRect(shown, radius, radius, paint)
        val save = canvas.saveLayerAlpha(shown, (255 * frame.contentProgress).toInt())
        canvas.clipRect(shown)
        canvas.translate(shown.left, shown.top)
        canvas.scale(shown.width() / target.width(), shown.height() / target.height())
        updatePlateRect()
        content.drawPlate(canvas, plateRect, plateAlpha.value, plateRect.centerX(), anchor.config.highlightOpacity, 0f)
        content.draw(canvas, scroll)
        canvas.restoreToCount(save)
    }

    fun onTouch(event: MotionEvent) {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchInside = target.contains(x, y)
                dismissOnUp = !touchInside
                touchStartX = x
                touchStartY = y
                touchStartScroll = scroll
                scrolling = false
                if (touchInside) {
                    jelly.down(x, y)
                    highlight(rowFor(x, y), immediate = true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!touchInside) return
                val dy = y - touchStartY
                if (maximumScroll > 0f && (scrolling || abs(dy) > 10f * density)) {
                    if (!scrolling) {
                        highlight(null, immediate = false)
                        jelly.releaseDrag()
                    }
                    scrolling = true
                    scroll = (touchStartScroll - dy).coerceIn(0f, maximumScroll)
                    rowsDirty = true
                    overlay.invalidate()
                    return
                }
                jelly.move(x, y)
                highlight(rowFor(x, y), immediate = false)
            }
            MotionEvent.ACTION_UP -> {
                jelly.release()
                val row = content.rowAt(x - target.left, y - target.top + scroll)
                when {
                    dismissOnUp -> overlay.requestClose()
                    row != null && !scrolling -> select(row.index)
                    else -> highlight(null, immediate = false)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                jelly.release()
                highlight(null, immediate = false)
            }
        }
    }

    private fun rowFor(x: Float, y: Float): GlassRow? =
        content.nearestRow(x - target.left, y - target.top + scroll, ScrubSlack * density)

    private fun highlight(row: GlassRow?, immediate: Boolean) {
        if (row === activeRow) return
        val hadRow = activeRow != null
        activeRow = row
        if (row == null) {
            plateVisible = false
            plateAlpha.target(0f, PlateFade)
        } else {
            if (!plateVisible && !hadRow) {
                plateTop.reset(row.rect.top)
                plateHeight.reset(row.rect.height())
            } else {
                plateTop.target(row.rect.top, PlateSlide)
                plateHeight.target(row.rect.height(), PlateSlide)
                overlay.hapticTick()
            }
            plateVisible = true
            if (immediate) plateAlpha.reset(1f) else plateAlpha.target(1f, PlateFade)
        }
    }

    fun select(index: Int) {
        val row = content.rows.firstOrNull { it.index == index }?.takeIf { it.selectable } ?: return
        anchor.notifySelect(row.item, row.index)
        if (row.item.keepOpen) {
            highlight(null, immediate = false)
        } else {
            overlay.requestClose()
        }
    }

    fun rowIndexAt(x: Float, y: Float): Int = content.rowAt(x - target.left, y - target.top + scroll)?.index ?: -1

    fun accessibleRows(): List<GlassRow> = if (expanded) content.rows.filter { !it.isSeparator } else emptyList()

    fun screenRect(row: GlassRow?): Rect {
        val rect = row?.rect ?: return Rect(0, 0, 1, 1)
        return Rect(
            (target.left + rect.left).toInt(), (target.top + rect.top - scroll).toInt(),
            (target.left + rect.right).toInt(), (target.top + rect.bottom - scroll).toInt()
        )
    }

    fun release() {
        rowsNode?.discardDisplayList()
        plateNode?.discardDisplayList()
    }

    private companion object {
        const val CornerRadius = 34f
        const val PlateInset = 8f
        const val ScrubSlack = 24f
        const val CaptureMargin = 48f
        const val MinimumRowsScale = 0.25f
        const val NeckSigma = 20f
        const val FieldCellsPerSigma = 4f
        const val FieldCells = 64f
        const val MenuLightRadius = 80f
        const val MenuLift = 0.018f
        const val MenuPool = 0.03f
        const val MenuRimLift = 0.12f
        val PlateSlide = GlassSpringSpec(0.85f, 0.13f)
        val PlateFade = GlassSpringSpec(1f, 0.14f)
    }
}
