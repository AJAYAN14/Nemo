package com.jian.nemo.core.ui.component.sheet

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Nemo 通用高质感毛玻璃抽屉组件 (NemoModalBottomSheet)
 *
 * 核心特性：
 * 1. 官方原生 Blur Behind 窗口级高斯毛玻璃：在 Android 12 (API 31+) 上自动开启 GPU 硬件级背景毛玻璃（blurBehindRadius = 48px, dimAmount = 0.20f）。
 * 2. 现代视觉设计：28dp 顶部圆角、精致拖拽手柄、深浅自适应容器背景色。
 * 3. 展开触觉反馈：抽屉升起时伴随轻快触感。
 * 4. 低版本自动安全降级为标准半透明暗化遮罩。
 *
 * @param onDismissRequest 关闭抽屉请求回调
 * @param modifier 修饰符
 * @param sheetState 抽屉状态
 * @param shape 抽屉卡片圆角（默认 28dp 顶部平滑圆角）
 * @param containerColor 抽屉背景色（默认 surfaceContainerLow 自适应）
 * @param contentColor 抽屉内容色
 * @param tonalElevation 色调高度
 * @param scrimColor 遮罩颜色
 * @param dragHandle 拖拽手柄组件
 * @param contentWindowInsets 窗口边距
 * @param content 抽屉内部 Composable 布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NemoModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets
    ) {
        val view = LocalView.current
        val isHiding = sheetState.targetValue == SheetValue.Hidden

        // 绑定窗口级高斯毛玻璃与窗口动画优化
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                // 1. 禁用底层 Window 的系统级退出动画，消除多余的 300ms 窗口销毁滞后
                window.setWindowAnimations(0)

                // 2. 开启 Android 12+ 硬件级高斯毛玻璃
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 48
                        dimAmount = 0.20f
                    }
                }
            }

            onDispose {
                if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = 0
                            dimAmount = 0f
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 状态联动：关闭时第 0 毫秒先销毁高斯模糊，抽屉顺势滑下；重新打开时满血恢复
        LaunchedEffect(isHiding) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (isHiding) {
                        // 一触发关闭，立即清零高斯模糊，背景瞬间恢复清晰通透
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = 0
                            dimAmount = 0f
                        }
                    } else {
                        // 打开或展开时，确保满血挂载毛玻璃
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = 48
                            dimAmount = 0.20f
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        content()
    }
}
