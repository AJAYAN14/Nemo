package com.jian.nemo.feature.test.presentation.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.ui.component.dialog.CommonConfigDialog

/**
 * 测试设置对话框组件
 * 继承自统一美学的通用 CommonConfigDialog。
 */

/**
 * 自定义题目数量对话框
 */
@Composable
fun CustomQuestionCountDialog(
    show: Boolean,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (!show) return

    CommonConfigDialog(
        headerIcon = Icons.Rounded.FormatListNumbered,
        headerIconTint = Color(0xFF0088FF),
        headerIconBgColor = Color(0xFFE6F4FF),
        headerIconBgColorDark = Color(0xFF102A45),
        title = "自定义题数",
        subtitle = "设置本次测试的题目数量",
        initialValue = initialValue,
        min = 1,
        max = 200,
        presets = listOf(5, 10, 15, 20, 30, 50),
        presetLabelFormatter = { "${it}题" },
        confirmText = "保存配置",
        confirmButtonColor = Color(0xFF0088FF),
        onDismissRequest = onDismiss,
        onConfirm = onConfirm
    )
}

/**
 * 自定义时间限制对话框
 */
@Composable
fun CustomTimeLimitDialog(
    show: Boolean,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (!show) return

    CommonConfigDialog(
        headerIcon = Icons.Rounded.Timer,
        headerIconTint = Color(0xFF8B5CF6),
        headerIconBgColor = Color(0xFFF3E8FF),
        headerIconBgColorDark = Color(0xFF2E1065),
        title = "自定义时长",
        subtitle = "设置测试时间限制 (分钟)，0为不限时",
        initialValue = initialValue,
        min = 0,
        max = 180,
        presets = listOf(5, 10, 15, 20, 30, 60),
        presetLabelFormatter = { if (it == 0) "不限时" else "${it}分钟" },
        confirmText = "保存配置",
        confirmButtonColor = Color(0xFF8B5CF6),
        onDismissRequest = onDismiss,
        onConfirm = onConfirm
    )
}
