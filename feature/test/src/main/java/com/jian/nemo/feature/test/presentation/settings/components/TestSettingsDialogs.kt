package com.jian.nemo.feature.test.presentation.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.designsystem.theme.NemoCategoryColors
import com.jian.nemo.core.designsystem.theme.NemoPrimary
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

    val countPresetFormat = stringResource(R.string.test_dialog_format_preset_count)
    CommonConfigDialog(
        headerIcon = Icons.Rounded.FormatListNumbered,
        headerIconTint = NemoPrimary,
        headerIconBgColor = NemoCategoryColors.CardVerbBgLight,
        headerIconBgColorDark = NemoCategoryColors.CardVerbBgDark,
        title = stringResource(R.string.test_dialog_custom_count_title),
        subtitle = stringResource(R.string.test_dialog_custom_count_subtitle),
        initialValue = initialValue,
        min = 1,
        max = 200,
        presets = listOf(5, 10, 15, 20, 30, 50),
        presetLabelFormatter = { String.format(countPresetFormat, it) },
        confirmText = stringResource(R.string.test_dialog_btn_save_config),
        confirmButtonColor = NemoPrimary,
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

    val unlimitedPreset = stringResource(R.string.test_dialog_time_unlimited)
    val timePresetFormat = stringResource(R.string.test_dialog_format_preset_time)
    CommonConfigDialog(
        headerIcon = Icons.Rounded.Timer,
        headerIconTint = NemoCategoryColors.CardNounTextLight,
        headerIconBgColor = NemoCategoryColors.CardNounBgLight,
        headerIconBgColorDark = NemoCategoryColors.CardNounBgDark,
        title = stringResource(R.string.test_dialog_custom_time_title),
        subtitle = stringResource(R.string.test_dialog_custom_time_subtitle),
        initialValue = initialValue,
        min = 0,
        max = 180,
        presets = listOf(5, 10, 15, 20, 30, 60),
        presetLabelFormatter = { if (it == 0) unlimitedPreset else String.format(timePresetFormat, it) },
        confirmText = stringResource(R.string.test_dialog_btn_save_config),
        confirmButtonColor = NemoCategoryColors.CardNounTextLight,
        onDismissRequest = onDismiss,
        onConfirm = onConfirm
    )
}
