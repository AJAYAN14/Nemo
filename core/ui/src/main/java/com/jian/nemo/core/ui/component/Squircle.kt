package com.jian.nemo.core.ui.component

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.jian.nemo.core.designsystem.shape.Capsule as DesignCapsule
import com.jian.nemo.core.designsystem.shape.RoundedRectangle as DesignRoundedRectangle

/**
 * G2 连续曲率平滑圆角矩形重定向，确保与 :core:designsystem 规范同源
 */
fun RoundedRectangle(radius: Dp): Shape = DesignRoundedRectangle(radius)

/**
 * G2 连续曲率胶囊形状重定向，确保与 :core:designsystem 规范同源
 */
fun Capsule(): Shape = DesignCapsule()
