package com.jian.nemo.core.ui.component.sheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import com.jian.nemo.core.designsystem.shape.UnevenRoundedRectangle
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
import com.jian.nemo.core.ui.component.dialog.NemoWindowBlurBehind

/**
 * Nemo 通用高质感毛玻璃抽屉组件 (NemoModalBottomSheet)
 *
 * 核心特性：
 * 1. 官方原生 Blur Behind 窗口级高斯毛玻璃：在 Android 12 (API 31+) 上自动开启 GPU 硬件级背景毛玻璃（blurBehindRadius = 48px, dimAmount = 0f，抽屉自带 scrim 遮罩故不叠加暗化）。
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
    shape: Shape = UnevenRoundedRectangle(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
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

        // 禁用底层 Window 系统退出动画，消除 300ms 销毁滞后
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setWindowAnimations(0)
            onDispose { }
        }

        // 抽屉自带 Scrim 遮罩，无需额外暗化
        NemoWindowBlurBehind(dimAmount = 0f)

        content()
    }
}
