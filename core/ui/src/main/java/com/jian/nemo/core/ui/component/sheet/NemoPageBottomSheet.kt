package com.jian.nemo.core.ui.component.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.shape.deviceCornerShape
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.Backdrop
import com.jian.nemo.core.designsystem.R as DesignR
import kotlinx.coroutines.launch

/**
 * 页面级通用下沉抽屉弹窗 (NemoPageBottomSheet)
 *
 * 核心特性：
 * 1. 窗口高度自适应：基于 LocalConfiguration 动态避让状态栏，展开停靠在状态栏下方形成类似 PageSheet 的沉浸式视界。
 * 2. 硬件连续平滑曲率：结合 [deviceCornerShape] 采样真机四角硬件圆角，仅裁剪顶部圆角。
 * 3. 物理阻尼弹簧动效：进场使用物理弹簧，退场使用平滑贝塞尔滑出。
 * 4. 闭包退场驱动：向子组件暴露 `slideOut` 协程函数，支持内部交互完成后先滑出再销毁。
 * 5. 遮罩与手势闭环：自带淡入淡出半透明黑色遮罩，拦截系统返回手势（BackHandler）。
 *
 * @param onDismissed 弹窗完全退出并销毁后的回调
 * @param modifier 外部修饰符
 * @param shape 弹窗外轮廓裁剪形状，默认采用真机硬件平滑圆角（上限 36dp）
 * @param containerColor 弹窗主背景色，默认采用 MaterialTheme.colorScheme.background
 * @param scrimColor 外部遮罩底色，默认采用 45% 不透明黑色
 * @param onDismissRequest 点击遮罩或触发返回时的拦截回调，默认直接调用 slideOut
 * @param content 弹窗内容作用域，接收 slideOut 回调
 */
@Composable
fun NemoPageBottomSheet(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = deviceCornerShape(
        bottomLeft = false,
        bottomRight = false,
        maxRadius = 36.dp
    ),
    containerColor: Color = MaterialTheme.colorScheme.background,
    scrimColor: Color = Color.Black.copy(alpha = 0.45f),
    onDismissRequest: (slideOut: () -> Unit) -> Unit = { slideOut -> slideOut() },
    content: @Composable BoxScope.(slideOut: () -> Unit) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    var isVisible by remember { mutableStateOf(false) }
    var hasSlidIn by remember { mutableStateOf(false) }

    // 避让状态栏高度，保证下沉沉浸间隙
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomSheetHeight = configuration.screenHeightDp.dp - statusBarTop
    val bottomSheetHeightPx = with(density) { bottomSheetHeight.toPx() }

    val offset = remember { Animatable(bottomSheetHeightPx) }
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(isReady, bottomSheetHeightPx) {
        if (isReady) {
            isVisible = true
            if (!hasSlidIn) {
                offset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = 380f
                    )
                )
                hasSlidIn = true
            } else {
                offset.snapTo(0f)
            }
        }
    }

    var isDismissing by remember { mutableStateOf(false) }
    fun slideOut() {
        if (isDismissing) return
        scope.launch {
            isDismissing = true
            isVisible = false
            offset.animateTo(
                targetValue = bottomSheetHeightPx,
                animationSpec = tween(
                    durationMillis = 150,
                    easing = CubicBezierEasing(0.4f, 0.0f, 1f, 1f)
                )
            )
            onDismissed()
        }
    }

    BackHandler {
        onDismissRequest(::slideOut)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 外部渐隐遮罩
        PageSheetScrim(
            visible = isVisible,
            scrimColor = scrimColor,
            onDismiss = { onDismissRequest(::slideOut) }
        )

        // 抽屉弹窗主体卡片
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = offset.value
                }
                .layout { measurable, constraints ->
                    val unboundedConstraints = constraints.copy(maxHeight = Constraints.Infinity)
                    val placeable = measurable.measure(unboundedConstraints)
                    layout(placeable.width, constraints.maxHeight) {
                        // 底部延伸 32.dp 消除回弹间隙
                        placeable.place(
                            0,
                            constraints.maxHeight - placeable.height + 32.dp.roundToPx()
                        )
                        if (!isReady) isReady = true
                    }
                }
                .height(bottomSheetHeight + 32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .clip(shape)
                .background(color = containerColor)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            content(::slideOut)
        }
    }
}

/**
 * 通用抽屉配套顶栏 (NemoSheetTopBar)
 *
 * 核心特性：
 * 1. 采用自定义 Layout 动态计算两翼占位，保证标题在视口中绝对水平居中。
 * 2. 左侧自带实验室同款磨砂液态玻璃关闭按钮 (48dp 正圆底座 + 34dp 谷歌 Weight 700 圆润叉)，支持平滑退出。
 * 3. 右侧预留扩展操作槽位。
 *
 * @param title 标题文字
 * @param onBack 返回按钮点击回调
 * @param modifier 修饰符
 * @param backdrop 背景采样源 (用于物理透镜折射与高斯模糊)
 * @param backContentDescription 返回按钮无障碍说明
 * @param trailing 右侧操作插槽
 */
@Composable
fun NemoSheetTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    backContentDescription: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        content = {
            Box(
                modifier = Modifier.layoutId("leading"),
                contentAlignment = Alignment.Center
            ) {
                FrostedLiquidGlassButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    },
                    backdrop = backdrop,
                    shape = CircleShape,
                    hasLens = true,
                    contentPadding = PaddingValues(0.dp),
                    isInteractive = true,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = DesignR.drawable.ic_google_close_rounded_bold),
                        contentDescription = backContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId("trailing"),
                    contentAlignment = Alignment.Center
                ) {
                    trailing()
                }
            }

            Text(
                text = title,
                modifier = Modifier.layoutId("title"),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    ) { measurables, constraints ->
        val sideConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxWidth = constraints.maxWidth / 2
        )

        val leadingPlaceable = measurables.find { it.layoutId == "leading" }?.measure(sideConstraints)
        val trailingPlaceable = measurables.find { it.layoutId == "trailing" }?.measure(sideConstraints)

        val leadingWidth = leadingPlaceable?.width ?: 0
        val trailingWidth = trailingPlaceable?.width ?: 0

        val safePaddingPx = 12.dp.roundToPx()
        val maxSideWidth = maxOf(leadingWidth, trailingWidth)
        val sidePadding = if (maxSideWidth > 0) maxSideWidth + safePaddingPx else 24.dp.roundToPx()

        val titleMaxWidth = (constraints.maxWidth - sidePadding * 2).coerceAtLeast(0)
        val titlePlaceable = measurables.find { it.layoutId == "title" }?.measure(
            constraints.copy(
                minWidth = 0,
                maxWidth = titleMaxWidth,
                minHeight = 0
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            fun centerVertically(childHeight: Int) = (constraints.maxHeight - childHeight) / 2

            leadingPlaceable?.placeRelative(
                x = 0,
                y = centerVertically(leadingPlaceable.height)
            )

            trailingPlaceable?.placeRelative(
                x = constraints.maxWidth - trailingWidth,
                y = centerVertically(trailingPlaceable.height)
            )

            titlePlaceable?.let {
                it.placeRelative(
                    x = (constraints.maxWidth - it.width) / 2,
                    y = centerVertically(it.height)
                )
            }
        }
    }
}

@Composable
private fun PageSheetScrim(
    visible: Boolean,
    scrimColor: Color,
    onDismiss: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "PageSheetScrimAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = visible,
                onClick = onDismiss
            )
            .background(scrimColor)
    )
}
