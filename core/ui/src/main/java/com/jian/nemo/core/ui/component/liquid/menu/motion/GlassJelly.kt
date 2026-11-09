package com.jian.nemo.core.ui.component.liquid.menu.motion

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 手势拖拽流体挤压（Squash & Stretch）与微晃回弹物理机
 */
internal class GlassJelly {
    private val press = GlassSpring(0.002f, 0.02f)
    private val glow = GlassSpring(0.002f, 0.02f)
    private val dragX = GlassSpring(0.3f, 6f)
    private val dragY = GlassSpring(0.3f, 6f)
    private val lightX = GlassSpring(0.3f, 6f)
    private val lightY = GlassSpring(0.3f, 6f)
    private var startX = 0f
    private var startY = 0f

    val glowValue: Float get() = glow.value.coerceIn(0f, 1f)
    val lightPositionX: Float get() = lightX.value
    val lightPositionY: Float get() = lightY.value
    val isSettled: Boolean
        get() = press.isSettled && glow.isSettled && dragX.isSettled && dragY.isSettled &&
            lightX.isSettled && lightY.isSettled

    fun down(x: Float, y: Float) {
        startX = x
        startY = y
        if (glow.value < 0.02f) {
            lightX.reset(x)
            lightY.reset(y)
        } else {
            lightX.target(x, LightFollow)
            lightY.target(y, LightFollow)
        }
        press.target(1f, JellySpring)
        glow.target(1f, GlowIn)
    }

    fun move(x: Float, y: Float) {
        dragX.target(x - startX, DragFollow)
        dragY.target(y - startY, DragFollow)
        lightX.target(x, LightFollow)
        lightY.target(y, LightFollow)
    }

    fun release() {
        press.target(0f, JellySpring)
        glow.target(0f, GlowOut)
        dragX.target(0f, JellySpring)
        dragY.target(0f, JellySpring)
    }

    fun advance(seconds: Float) {
        press.advance(seconds)
        glow.advance(seconds)
        dragX.advance(seconds)
        dragY.advance(seconds)
        lightX.advance(seconds)
        lightY.advance(seconds)
    }

    fun deformMenu(out: RectF, awayX: Float, awayY: Float, density: Float, amount: Float) {
        val reach = MenuReach * density
        val dx = dragX.value
        val dy = dragY.value
        val pullX = tanh(dx * awayX / reach)
        val pullY = tanh(dy * awayY / reach)
        val width = out.width() + (MenuStretch * pullX - MenuSquash * pullY) * density * amount
        val height = out.height() + (MenuStretch * pullY - MenuSquash * pullX) * density * amount
        val centerX = out.centerX() + MenuShift * tanh(dx / reach) * density * amount
        val centerY = out.centerY() + MenuShift * tanh(dy / reach) * density * amount
        out.set(centerX - width / 2f, centerY - height / 2f, centerX + width / 2f, centerY + height / 2f)
    }

    fun swell(height: Float, density: Float, amount: Float): Float =
        1f + press.value * ControlLift * density * amount / max(height, 1f)

    fun deformControl(out: RectF, density: Float, amount: Float) {
        val width = max(out.width(), 1f)
        val height = max(out.height(), 1f)
        val longest = max(width, height)
        val shortest = min(width, height)
        val dx = dragX.value
        val dy = dragY.value
        val scale = swell(height, density, amount)
        val angle = atan2(dy, dx)
        val stretch = ControlLift * density * amount / height
        val scaleX = scale + stretch * abs(cos(angle) * dx / longest) * min(width / height, 1f)
        val scaleY = scale + stretch * abs(sin(angle) * dy / longest) * min(height / width, 1f)
        val centerX = out.centerX() + shortest * tanh(DragLean * dx / shortest) * amount
        val centerY = out.centerY() + shortest * tanh(DragLean * dy / shortest) * amount
        val halfWidth = width * scaleX / 2f
        val halfHeight = height * scaleY / 2f
        out.set(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
    }

    fun releaseDrag() {
        dragX.target(0f, JellySpring)
        dragY.target(0f, JellySpring)
    }

    private companion object {
        const val MenuReach = 260f
        const val MenuStretch = 16f
        const val MenuSquash = 21f
        const val MenuShift = 21f
        const val ControlLift = 4f
        const val DragLean = 0.05f
        val JellySpring = GlassSpringSpec(0.5f, 0.45f)
        val DragFollow = GlassSpringSpec(0.65f, 0.18f)
        val LightFollow = GlassSpringSpec(0.8f, 0.16f)
        val GlowIn = GlassSpringSpec(1f, 0.1f)
        val GlowOut = GlassSpringSpec(1f, 0.5f)
    }
}
