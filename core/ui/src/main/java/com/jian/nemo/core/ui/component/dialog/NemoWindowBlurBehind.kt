package com.jian.nemo.core.ui.component.dialog

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Nemo 统一的窗口级毛玻璃效果 (Android 12+ 官方 Blur Behind)
 *
 * 在 `Dialog {}` / `ModalBottomSheet {}` 等 Compose 窗口内容作用域内调用一次，
 * 即可为该窗口开启系统级背景高斯毛玻璃（FLAG_BLUR_BEHIND + blurBehindRadius）
 * 与暗化遮罩；窗口销毁时自动清理标记并归零参数，保证硬件资源即用即销的对称闭环，
 * 防止 Window 泄漏或背景残影。
 *
 * - Android 12 (API 31) 以下自动安全降级为无效果
 * - 窗口获取失败（不在窗口作用域内）时静默跳过
 *
 * @param blurBehindRadiusPx 模糊半径（px），默认 48
 * @param dimAmount 遮罩暗化程度，默认 0.20f；无需暗化遮罩时传 0f
 */
@Composable
fun NemoWindowBlurBehind(
    blurBehindRadiusPx: Int = 48,
    dimAmount: Float = 0.20f
) {
    val view = LocalView.current
    DisposableEffect(view, blurBehindRadiusPx, dimAmount) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val attrs = window.attributes
            attrs.blurBehindRadius = blurBehindRadiusPx
            attrs.dimAmount = dimAmount
            window.attributes = attrs
        }
        onDispose {
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val attrs = window.attributes
                    attrs.blurBehindRadius = 0
                    attrs.dimAmount = 0f
                    window.attributes = attrs
                } catch (_: Exception) {
                }
            }
        }
    }
}
