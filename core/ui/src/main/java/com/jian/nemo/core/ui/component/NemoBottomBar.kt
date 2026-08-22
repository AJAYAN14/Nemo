package com.jian.nemo.core.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import com.jian.nemo.core.domain.model.User
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 定义颜色常量
private val LearningCardBackgroundDark = Color(0xFF2c2c2c)

/**
 * Nemo应用底部导航栏 (悬浮纯色胶囊版)
 *
 * 包含4个主要Tab：学习、进度、测试、个人
 * 采用悬浮胶囊布局设计，背景纯色，深浅自适应。
 * 选中项的背景改为弹簧横向滑动的胶囊指示器，体验极其高级。
 */
@Composable
fun NemoBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    hazeState: HazeState? = null,
    user: User? = null
) {
    // 根据主题判断深色/浅色模式
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5
    
    // 原版 LiquidGlass accentColor
    val accentColor = if (isDarkTheme) Color(0xFF0091FF) else Color(0xFF0088FF)
    
    // 胶囊容器背景色设置 (对齐原版 LiquidGlass)
    val containerColor = if (isDarkTheme) 
        Color(0xFF121212).copy(alpha = 0.4f)
    else 
        Color(0xFFFAFAFA).copy(alpha = 0.4f)
        
    // 边框描边颜色
    val borderColor = if (isDarkTheme)
        Color.White.copy(alpha = 0.12f)
    else
        Color.White.copy(alpha = 0.45f)

    // 避免 pointerInput 闭包中捕获到旧的值
    val currentRouteState by androidx.compose.runtime.rememberUpdatedState(currentRoute)
    val currentOnNavigateState by androidx.compose.runtime.rememberUpdatedState(onNavigate)

    val tabsCount = BottomNavItem.entries.size
    var lastValidIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedIndex = remember(currentRoute) {
        val matchedIndex = BottomNavItem.entries.indexOfFirst { it.route == currentRoute }
        if (matchedIndex >= 0) {
            lastValidIndex = matchedIndex
            matchedIndex
        } else {
            lastValidIndex
        }
    }

    // 核心动画状态
    val offsetIndex = remember { Animatable(selectedIndex.toFloat()) }
    val pressProgress = remember { Animatable(0f) }
    val smoothedVelocity = remember { Animatable(0f) }
    // 对齐原版：scaleX/scaleY 独立 Animatable，使用不同弹簧产生不对称回弹
    val scaleXAnim = remember { Animatable(1f) }
    val scaleYAnim = remember { Animatable(1f) }
    
    // 主题色
    val themePrimary = MaterialTheme.colorScheme.primary

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    val velocityTracker = remember { VelocityTracker() }

    // 1. 整体容器的阻尼拉伸平移动画 (X轴偏置动画)
    val offsetAnimation = remember { Animatable(0f) }
    
    // 2. 交互式流光触碰点 X 坐标动画
    val touchX = remember { Animatable(0f) }

    // 保存内部 Row 的实际可用宽度
    var innerWidth by remember { mutableFloatStateOf(0f) }
    val paddingOffsetPx = with(density) { 4.dp.toPx() }
    val strokeWidthPx = with(density) { 1.dp.toPx() }

    // vibrancy 效果：缓存 RenderEffect（对齐原版 saturation = 1.5f）
    @Suppress("NewApi")
    val vibrancyEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cm = android.graphics.ColorMatrix()
            cm.setSaturation(1.5f)
            android.graphics.RenderEffect.createColorFilterEffect(
                android.graphics.ColorMatrixColorFilter(cm)
            ).asComposeRenderEffect()
        } else null
    }

    // 用来识别当前是否处于拖动状态
    var isDragging by remember { mutableStateOf(false) }

    // 计算整个胶囊体的阻尼弹性偏置拉扯位移
    val panelOffset = remember(density, innerWidth) {
        derivedStateOf<Float> {
            if (innerWidth <= 0f) 0f else {
                val fraction = (offsetAnimation.value / innerWidth).coerceIn(-1f, 1f)
                // 物理公式：最高拉扯 4.dp 的弹性位移量（对齐原版）
                with(density) {
                    val directionSign = if (fraction > 0f) 1f else if (fraction < 0f) -1f else 0f
                    4.dp.toPx() * directionSign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }

    // 当页面由外部（如刚进入应用或系统导航）改变时，让滑块动画移动 to 对应 Tab
    LaunchedEffect(selectedIndex) {
        if (!offsetIndex.isRunning || offsetIndex.targetValue != selectedIndex.toFloat()) {
            offsetIndex.animateTo(
                targetValue = selectedIndex.toFloat(),
                // 对齐原版 valueAnimationSpec: spring(1f, 300f)
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = 300f
                )
            )
            // 外部对齐时，高光也流淌到对应的 Tab 中心
            if (innerWidth > 0f) {
                val tabWidth = innerWidth / tabsCount
                val targetCenter = (selectedIndex + 0.5f) * tabWidth
                coroutineScope.launch {
                    touchX.animateTo(
                        targetCenter,
                        spring(dampingRatio = 0.5f, stiffness = 300f)
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically { it } + fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 外壳层：提供阴影和整体变换，不裁剪内容
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        clip = false,
                        spotColor = Color.Black.copy(alpha = 0.12f),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .graphicsLayer {
                        // 胶囊外壳阻尼弹性位移 + 按压微缩放
                        translationX = panelOffset.value
                        val containerScale = lerp(1f, 1f + 16.dp.toPx() / size.width, pressProgress.value)
                        scaleX = containerScale
                        scaleY = containerScale
                    }
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // 背景层：haze + vibrancy + containerColor + border（裁剪到胶囊形状）
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            // vibrancy 效果：对齐原版 saturation = 1.5f (API 31+)
                            renderEffect = vibrancyEffect
                        }
                        .then(
                            if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = CircleShape) else Modifier
                        )
                        .background(containerColor, CircleShape)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = CircleShape
                        )
                )

                // 内容层：指示器 + Tab项（不受裁剪，指示器可超出容器）
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth()
                        .padding(4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // 计算滑块尺寸与位移
                    if (innerWidth > 0f) {
                        val tabWidth = innerWidth / tabsCount
                        val tabWidthDp = with(density) { tabWidth.toDp() }
                        val indicatorWidth = tabWidthDp

                        Box(
                            modifier = Modifier
                                .width(indicatorWidth)
                                .height(56.dp)
                                .graphicsLayer {
                                    // 核心性能优化：位移完全移入 graphicsLayer
                                    translationX = (offsetIndex.value * tabWidth)
                                    
                                    // 对齐原版：scaleX/scaleY 独立动画值
                                    scaleX = scaleXAnim.value
                                    scaleY = scaleYAnim.value
                                    
                                    // 对齐原版速度形变：dampedDragAnimation.velocity / 10f
                                    // smoothedVelocity 已是归一化值(index空间/range)，直接 /10f
                                    // 非拖动时 offsetIndex.velocity 是 index/s，除以 range 再 /10f
                                    val velocity = if (isDragging) {
                                        smoothedVelocity.value / 10f
                                    } else {
                                        offsetIndex.velocity / (tabsCount - 1).toFloat() / 10f
                                    }
                                    scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                }
                                .clip(CircleShape)
                                .drawBehind {
                                    // 纯主题色背景
                                    drawRect(themePrimary)
                                }
                        )
                    }

                    // 2. 选项列表 Row，统一在该 Row 上拦截所有触摸手势，避免子项 clickable 冲突
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .onGloballyPositioned { coordinates ->
                                innerWidth = coordinates.size.width.toFloat()
                            }
                            .pointerInput(Unit) {
                                coroutineScope {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val startX = down.position.x
                                            isDragging = false
                                            velocityTracker.resetTracking()

                                            // 按下时立刻膨胀（对齐原版 press()）
                                            launch {
                                                pressProgress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 1000f))
                                            }
                                            launch {
                                                scaleXAnim.animateTo(78f / 56f, spring(dampingRatio = 0.6f, stiffness = 250f))
                                            }
                                            launch {
                                                scaleYAnim.animateTo(78f / 56f, spring(dampingRatio = 0.7f, stiffness = 250f))
                                            }
                                            launch {
                                                touchX.snapTo(startX)
                                            }

                                            var pointerId = down.id
                                            var lastX = down.position.x

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.find { it.id == pointerId }
                                                if (change == null || change.pressed == false) {
                                                    // 手指抬起（释放手势）
                                                    isDragging = false
                                                    launch {
                                                        // 手势平滑速度阻尼归 0（对齐原版 velocityAnimationSpec）
                                                        smoothedVelocity.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 300f))
                                                    }
                                                    // 整体容器阻尼复位（对齐原版 spring(1f, 300f, 0.5f)）
                                                    launch {
                                                        offsetAnimation.animateTo(0f, spring(dampingRatio = 1f, stiffness = 300f, visibilityThreshold = 0.5f))
                                                    }

                                                    val dragVelocity = velocityTracker.calculateVelocity().x
                                                    val totalDragX = lastX - startX

                                                    val targetIndex: Int
                                                    if (abs(totalDragX) > 15f) {
                                                        // 滑动对齐
                                                        targetIndex = snapToNearestTab(offsetIndex.value, dragVelocity, tabsCount)
                                                        if (BottomNavItem.entries[targetIndex].route != currentRouteState) {
                                                            currentOnNavigateState(BottomNavItem.entries[targetIndex].route)
                                                        }
                                                        launch {
                                                            offsetIndex.animateTo(
                                                                targetIndex.toFloat(),
                                                                spring(dampingRatio = 1f, stiffness = 300f)
                                                            )
                                                        }
                                                        // 高光流光向选中的 Tab 中心平滑流淌
                                                        if (innerWidth > 0f) {
                                                            val tabWidth = innerWidth / tabsCount
                                                            val targetCenter = (targetIndex + 0.5f) * tabWidth
                                                            launch {
                                                                touchX.animateTo(
                                                                    targetCenter,
                                                                    spring(dampingRatio = 0.5f, stiffness = 300f)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        // 点击逻辑
                                                        if (innerWidth > 0f) {
                                                            val tabWidth = innerWidth / tabsCount
                                                            val clickedIndex = (startX / tabWidth).toInt().coerceIn(0, tabsCount - 1)
                                                            targetIndex = clickedIndex
                                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                            if (BottomNavItem.entries[clickedIndex].route != currentRouteState) {
                                                                currentOnNavigateState(BottomNavItem.entries[clickedIndex].route)
                                                            }
                                                            launch {
                                                                offsetIndex.animateTo(
                                                                    clickedIndex.toFloat(),
                                                                    spring(dampingRatio = 1f, stiffness = 300f)
                                                                )
                                                            }
                                                            // 点击处高光流淌
                                                            val targetCenter = (clickedIndex + 0.5f) * tabWidth
                                                            launch {
                                                                touchX.animateTo(
                                                                    targetCenter,
                                                                    spring(dampingRatio = 0.5f, stiffness = 300f)
                                                                )
                                                            }
                                                        } else {
                                                            targetIndex = selectedIndex
                                                        }
                                                    }

                                                    // 对齐原版 release()：延迟释放 press/scale
                                                    // 等到滑块接近目标位置后才开始消退 press 效果
                                                    launch {
                                                        val threshold = (tabsCount - 1).toFloat() * 0.025f
                                                        if (offsetIndex.value != offsetIndex.targetValue) {
                                                            snapshotFlow { offsetIndex.value }
                                                                .filter { abs(it - offsetIndex.targetValue) < threshold }
                                                                .first()
                                                        }
                                                        launch { pressProgress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 1000f)) }
                                                        launch { scaleXAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 250f)) }
                                                        launch { scaleYAnim.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 250f)) }
                                                    }
                                                    break
                                                }

                                                // 移动中
                                                val currentX = change.position.x
                                                val dragAmountX = currentX - lastX
                                                val totalDragX = currentX - startX

                                                if (!isDragging && abs(totalDragX) > 15f) {
                                                    isDragging = true
                                                }

                                                // 实时更新流光位置
                                                launch {
                                                    touchX.snapTo(currentX)
                                                }

                                                // 实时更新容器弹性位移
                                                launch {
                                                    offsetAnimation.snapTo(offsetAnimation.value + dragAmountX)
                                                }

                                                if (isDragging && innerWidth > 0f) {
                                                    change.consume()
                                                    val tabWidth = innerWidth / tabsCount
                                                    val deltaIndex = dragAmountX / tabWidth
                                                    val newTarget = (offsetIndex.value + deltaIndex).coerceIn(0f, (tabsCount - 1).toFloat())
                                                    launch {
                                                        offsetIndex.snapTo(newTarget)
                                                    }

                                                    // 对齐原版 updateVelocity()：在 index 空间追踪速度并归一化
                                                    velocityTracker.addPosition(change.uptimeMillis, Offset(offsetIndex.value, 0f))
                                                    val rawVelocity = velocityTracker.calculateVelocity().x / (tabsCount - 1).toFloat()
                                                    launch {
                                                        smoothedVelocity.animateTo(rawVelocity, spring(dampingRatio = 0.5f, stiffness = 300f))
                                                    }
                                                }
                                                lastX = currentX
                                            }
                                        }
                                    }
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem.entries.forEachIndexed { index, item ->
                            val distance = abs(offsetIndex.value - index)
                            val activeFraction = (1f - distance).coerceIn(0f, 1f)

                            CapsuleNavItem(
                                item = item,
                                activeFraction = activeFraction,
                                isDarkTheme = isDarkTheme,
                                user = user,
                                modifier = Modifier.weight(1f) // 平分 Row 宽度
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 根据滑动速度和当前位置，对齐至最近的 Tab
 */
private fun snapToNearestTab(currentValue: Float, dragVelocity: Float, tabsCount: Int): Int {
    val target = if (abs(dragVelocity) > 400f) {
        val direction = if (dragVelocity > 0) 1 else -1
        (currentValue + direction * 0.4f).roundToInt()
    } else {
        currentValue.roundToInt()
    }
    return target.coerceIn(0, tabsCount - 1)
}

/**
 * 单个导航项，常驻文本，支持与滑块接近程度的颜色平滑过渡
 */
@Composable
private fun CapsuleNavItem(
    item: BottomNavItem,
    activeFraction: Float,
    isDarkTheme: Boolean,
    user: User? = null,
    modifier: Modifier = Modifier
) {
    val activeContentColor = MaterialTheme.colorScheme.onPrimary
    val inactiveContentColor = if (isDarkTheme) Color.White else Color.Black

    val contentColor = remember(activeFraction, isDarkTheme) {
        androidx.compose.ui.graphics.lerp(inactiveContentColor, activeContentColor, activeFraction)
    }

    Column(
        modifier = modifier
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        if (item == BottomNavItem.SETTINGS && user != null) {
            // 已登录且是“个人”Tab，显示头像
            AvatarImage(
                username = user.username,
                avatarPath = user.avatarUrl,
                size = 24.dp,
                borderWidth = 1.5.dp * activeFraction,
                borderColor = Color.White.copy(alpha = 0.8f * activeFraction)
            )
        } else {
            // 默认显示 Icon
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        

        Text(
            text = item.title,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (activeFraction > 0.5f) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

/**
 * 底部导航栏 Tab 项
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    LEARNING(
        route = "learning",
        title = "学习",
        icon = Icons.AutoMirrored.Rounded.MenuBook
    ),
    PROGRESS(
        route = "progress",
        title = "进度",
        icon = Icons.Rounded.BarChart
    ),
    TEST(
        route = "test",
        title = "测试",
        icon = Icons.Rounded.Interests
    ),
    SETTINGS(
        route = "settings",
        title = "个人",
        icon = Icons.Rounded.AccountCircle
    )
}
