package com.jian.nemo.core.ui.component.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 今日加餐居中配置对话框 (BonusStudyDialog)
 * 继承自统一美学的通用 CommonConfigDialog。
 */
@Composable
fun BonusStudyDialog(
    onDismissRequest: () -> Unit,
    onConfirmBonus: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialQuantity: Int = 10
) {
    CommonConfigDialog(
        headerIcon = Icons.Rounded.Bolt,
        headerIconTint = Color(0xFFFF5500),
        headerIconBgColor = Color(0xFFFFF1E6),
        headerIconBgColorDark = Color(0xFF332014),
        title = "今日加餐",
        subtitle = "突破常规，设定追加练习目标",
        initialValue = initialQuantity,
        min = 1,
        max = 200,
        presets = listOf(5, 10, 15, 20, 30, 50),
        presetLabelFormatter = { "${it}题" },
        confirmText = "开始挑战 →",
        confirmButtonColor = Color(0xFFFF5500),
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirmBonus,
        modifier = modifier
    )
}
