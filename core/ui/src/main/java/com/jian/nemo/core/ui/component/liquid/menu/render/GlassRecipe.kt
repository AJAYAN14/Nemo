package com.jian.nemo.core.ui.component.liquid.menu.render

/**
 * 玻璃材质深浅模式光学与色调配方
 *
 * 控制器与菜单面板核心参数规范：
 * 1. [ControlDark]: 黑电平归位 (black = 0.02f)，微光磨砂充填 (fillAlpha = 0.12f)，对齐深色 15% 微光；
 * 2. [ControlLight]: 真实暗部黑电平 (black = 0.05f)，轻磨砂充填 (fillAlpha = 0.24f)，消除高电平发灰泛白；
 * 3. 展开阶段通过 mix(control, menu) 线性平滑插值过渡至大面板配方，保持流体拉丝与几何连续性。
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
        val ControlDark = GlassRecipe(1.15f, 0.02f, 1.0f, 0.2f, 0.12f)
        val MenuDark = GlassRecipe(1.3f, 0.125f, 1.125f, 0.65f, 0f, tint = 0.042f, tintAlpha = 0.7f)
        val ControlLight = GlassRecipe(1.15f, 0.05f, 1.0f, 0f, 0.24f)
        val MenuLight = GlassRecipe(1.2f, 0.4f, 1.03f, 0f, 0.2f, tint = 0.975f, tintAlpha = 0.5f)
    }
}
