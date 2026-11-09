package com.jian.nemo.core.ui.component.liquid.menu.motion

import kotlin.math.max

/**
 * 触发按钮收缩配置
 */
internal data class GlassShrinkConfig(
    val duration: Float = 0.21f,
    val scale: Float = 0.25f,
    val damping: Float? = null
)

/**
 * 菜单回吸收回配置
 */
internal data class GlassAbsorbConfig(
    val duration: Float = 0.35f,
    val damping: Float = 0.8f
)

/**
 * 形变时序与弹簧规格调谐器
 */
internal class GlassMorphTuning(
    shrink: GlassShrinkConfig? = null,
    absorb: GlassAbsorbConfig? = null
) {
    private val shrinkDuration: Float = max(shrink?.duration ?: DefaultShrinkDuration, MinimumDuration)
    private val shrinkDamping: Float? = shrink?.damping?.coerceIn(MinimumDamping, 1f)

    val shrinkScale: Float = (shrink?.scale ?: DefaultShrinkScale).coerceIn(0.05f, 1f)
    val collapse: GlassSpringSpec = GlassSpringSpec(shrinkDamping ?: 1f, shrinkDuration)
    val eject: GlassSpringSpec = GlassSpringSpec(shrinkDamping ?: DefaultEjectDamping, shrinkDuration * EjectRatio)
    val absorb: GlassSpringSpec = GlassSpringSpec(
        absorb?.damping?.coerceIn(MinimumDamping, 1f) ?: DefaultAbsorbDamping,
        max(absorb?.duration ?: DefaultAbsorbDuration, MinimumDuration)
    )
    val neckHold: Float = max(shrinkDuration / DefaultShrinkDuration, 1f)

    companion object {
        private const val DefaultShrinkDuration = 0.21f
        private const val DefaultShrinkScale = 0.25f
        private const val DefaultEjectDamping = 0.75f
        private const val DefaultAbsorbDuration = 0.35f
        private const val DefaultAbsorbDamping = 0.8f
        private const val EjectRatio = 0.32f / 0.21f
        private const val MinimumDuration = 0.02f
        private const val MinimumDamping = 0.1f
    }
}
