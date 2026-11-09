package com.jian.nemo.core.ui.component.liquid.menu.render

/**
 * 玻璃材质深浅模式光学与色调配方
 */
internal class GlassRecipe(
    val saturation: Float,
    val black: Float,
    val white: Float,
    val rollOff: Float,
    val fillAlpha: Float,
    val tint: Float = 0f,
    val tintAlpha: Float = 0f
) {
    companion object {
        val ControlDark = GlassRecipe(1.3f, 0.125f, 1.125f, 0.4f, 0f)
        val MenuDark = GlassRecipe(1.3f, 0.125f, 1.125f, 0.65f, 0f, tint = 0.042f, tintAlpha = 0.7f)
        val ControlLight = GlassRecipe(1.2f, 0.4f, 1.03f, 0f, 0.2f)
        val MenuLight = GlassRecipe(1.2f, 0.4f, 1.03f, 0f, 0.2f, tint = 0.975f, tintAlpha = 0.5f)
    }
}
