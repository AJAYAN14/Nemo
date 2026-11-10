package com.jian.nemo.core.ui.compositionlocal

import androidx.compose.runtime.compositionLocalOf
import com.jian.nemo.core.domain.repository.SettingIconStyle

/**
 * 全局图标视觉风格组合局部变量
 * 提供柔和微透 (SOFT_TRANSLUCENT) 与活力纯色 (VIBRANT_SOLID) 两种视觉表现形态的层级下发
 */
val LocalSettingIconStyle = compositionLocalOf { SettingIconStyle.SOFT_TRANSLUCENT }
