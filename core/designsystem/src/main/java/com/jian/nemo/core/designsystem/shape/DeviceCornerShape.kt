package com.jian.nemo.core.designsystem.shape

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

/**
 * 探测设备物理屏幕四角圆角曲率的仿生形状
 *
 * 在 Android 12 (API 31+) 上读取系统的 `WindowInsets.getRoundedCorner` 硬件圆角半径，
 * 结合 [UnevenRoundedRectangle] 生成具有 G2 连续平滑曲率的界面外形剪裁。
 * 低版本系统或无硬件圆角时，自动采用 [defaultRadius] 进行优雅兜底。
 *
 * @param padding 针对物理圆角的收缩内边距
 * @param topLeft 是否开启左上角圆角
 * @param topRight 是否开启右上角圆角
 * @param bottomRight 是否开启右下角圆角
 * @param bottomLeft 是否开启左下角圆角
 * @param maxRadius 最大允许的圆角半径上限
 * @param defaultRadius 硬件圆角不可用时的兜底圆角半径
 */
@Composable
fun deviceCornerShape(
    padding: Dp = 0.dp,
    topLeft: Boolean = true,
    topRight: Boolean = true,
    bottomRight: Boolean = true,
    bottomLeft: Boolean = true,
    maxRadius: Dp = Dp.Infinity,
    defaultRadius: Dp = 36.dp
): UnevenRoundedRectangle {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    return remember(view, configuration, padding, topLeft, topRight, bottomRight, bottomLeft, maxRadius, defaultRadius) {
        fun getCornerRadius(position: Int, enabled: Boolean): Dp {
            if (!enabled) return 0.dp

            val radius: Dp? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val insets = view.rootWindowInsets
                val roundedCorner = insets?.getRoundedCorner(position)
                with(density) { roundedCorner?.radius?.toFloat()?.toDp() }
            } else {
                null
            }

            return if (radius == null || radius <= 16.dp) {
                (defaultRadius - padding).coerceAtLeast(0.dp)
            } else {
                (radius - padding).coerceAtLeast(0.dp)
            }
        }

        val topLeftRadius = getCornerRadius(RoundedCorner.POSITION_TOP_LEFT, topLeft)
        val topRightRadius = getCornerRadius(RoundedCorner.POSITION_TOP_RIGHT, topRight)
        val bottomRightRadius = getCornerRadius(RoundedCorner.POSITION_BOTTOM_RIGHT, bottomRight)
        val bottomLeftRadius = getCornerRadius(RoundedCorner.POSITION_BOTTOM_LEFT, bottomLeft)

        UnevenRoundedRectangle(
            topStart = minOf(topLeftRadius, maxRadius),
            topEnd = minOf(topRightRadius, maxRadius),
            bottomEnd = minOf(bottomRightRadius, maxRadius),
            bottomStart = minOf(bottomLeftRadius, maxRadius)
        )
    }
}
