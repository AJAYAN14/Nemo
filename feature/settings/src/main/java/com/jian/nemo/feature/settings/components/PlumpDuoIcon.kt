package com.jian.nemo.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon as CorePlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon as CorePlumpIcon
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette as CoreIsoluminantPalette

/**
 * 兼容型别名导出：核心 Plump Duo 体系已下沉至 core:designsystem
 */
typealias PlumpIcon = CorePlumpIcon
typealias IsoluminantPalette = CoreIsoluminantPalette

@Composable
fun PlumpDuoIcon(
    icon: PlumpIcon,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    CorePlumpDuoIcon(
        icon = icon,
        color = color,
        modifier = modifier,
        size = size
    )
}
