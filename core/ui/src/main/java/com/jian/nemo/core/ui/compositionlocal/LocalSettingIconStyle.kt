package com.jian.nemo.core.ui.compositionlocal

import androidx.compose.runtime.compositionLocalOf
import com.jian.nemo.core.domain.repository.SettingIconStyle

/**
 * 全局图标视觉风格组合局部变量
 * 提供 MD3 与 Plump Duo 两种视觉表现形态的层级下发
 */
val LocalSettingIconStyle = compositionLocalOf { SettingIconStyle.MD3 }
