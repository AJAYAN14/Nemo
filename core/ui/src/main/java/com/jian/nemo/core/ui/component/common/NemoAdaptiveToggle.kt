package com.jian.nemo.core.ui.component.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.domain.repository.ToggleStyle
import com.jian.nemo.core.ui.component.liquid.LiquidToggle

/**
 * 自适应开关控件 (Adaptive Toggle)
 *
 * 根据传入的 [ToggleStyle] 或当前全局设置，自适应渲染为：
 * - [ToggleStyle.LIQUID_GLASS]: 液态玻璃质感开关
 * - [ToggleStyle.GOOEY]: 液态拉伸果冻物理开关
 *
 * @param checked 当前是否开启
 * @param onCheckedChange 开关改变回调
 * @param modifier 修饰符
 * @param style 开关视觉风格，默认为 LIQUID_GLASS
 * @param enabled 是否可用
 * @param activeColor 开启状态的激活高亮颜色
 */
@Composable
fun NemoAdaptiveToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: ToggleStyle = ToggleStyle.LIQUID_GLASS,
    enabled: Boolean = true,
    activeColor: Color = Color(0xFF0E68FF)
) {
    when (style) {
        ToggleStyle.LIQUID_GLASS -> {
            LiquidToggle(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                modifier = modifier,
                accentColor = activeColor
            )
        }
        ToggleStyle.GOOEY -> {
            NemoGooeyToggle(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
                activeColor = activeColor
            )
        }
    }
}
