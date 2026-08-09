package com.jian.nemo.core.ui.component.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 单条通知数据模型
 */
data class NemoNotificationData(
    val id: String,
    val message: String,
    val type: NemoSnackbarType = NemoSnackbarType.INFO,
    val icon: ImageVector? = null,
    val actionText: String? = null,
    val autoDismissMs: Long? = 4000L,
    val onDismiss: (() -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * 带有离场动画缓冲状态的包装 Item
 */
private class AnimatedNotificationState(
    val data: NemoNotificationData,
    initialVisible: Boolean = true
) {
    var isVisible by mutableStateOf(initialVisible)
    var dismissed by mutableStateOf(false)
}

/**
 * Nemo 垂直堆叠通知容器 (NemoNotificationStackHost)
 *
 * 特点：
 * 1. 严格独立的 4 秒生命周期：通过 Compose key() 保证每条通知的 LaunchedEffect 定时器与 Recomposition 完全隔离。
 * 2. 完美双向动画：出现（slideIn + fadeIn + expand）与消失（slideOut + fadeOut + shrink）。
 * 3. 动态垂直堆叠：无硬编码 offset 错位，最新通知始终插入在最上方。
 * 4. 手势交互：支持左右滑动擦除（Swipe to dismiss）。
 */
@Composable
fun NemoNotificationStackHost(
    notifications: List<NemoNotificationData>,
    modifier: Modifier = Modifier
) {
    // 维护内部独立生命周期的栈状态
    val internalStack = remember { mutableStateListOf<AnimatedNotificationState>() }

    // 当外部传入新通知时，只负责新增缺失的项，绝不干预已有通知的独立 4 秒计时
    LaunchedEffect(notifications) {
        val currentIds = internalStack.map { it.data.id }.toSet()
        notifications.reversed().forEach { newItem ->
            if (newItem.id !in currentIds) {
                internalStack.add(0, AnimatedNotificationState(newItem, initialVisible = true))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300)),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 使用 Compose key() 确保每条通知的 Composable 实例与 ID 稳定绑定，
        // 避免列表变化时因位置索引偏移导致 LaunchedEffect 定时器被错误取消/重启。
        internalStack.toList().forEach { stateItem ->
            key(stateItem.data.id) {
                SwipeableNotificationItem(
                    stateItem = stateItem,
                    onRemoveFromTree = {
                        internalStack.remove(stateItem)
                    }
                )
            }
        }
    }
}

@Composable
private fun SwipeableNotificationItem(
    stateItem: AnimatedNotificationState,
    onRemoveFromTree: () -> Unit
) {
    val item = stateItem.data
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val gradientColors = getStackGradientColors(item.type, isDarkTheme)

    // 绝对独立的 4 秒定时器：只在首次组合时触发一次，Recomposition 不会取消/重启它
    LaunchedEffect(Unit) {
        if (item.autoDismissMs != null) {
            delay(item.autoDismissMs)
            if (!stateItem.dismissed) {
                stateItem.dismissed = true
                stateItem.isVisible = false
                delay(300)
                item.onDismiss?.invoke()
            }
        }
    }

    // 拖拽淡出 alpha 动画计算
    val swipeAlpha = (1f - (abs(offsetX.value) / 300f)).coerceIn(0f, 1f)

    AnimatedVisibility(
        visible = stateItem.isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(250)
        ) + fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .alpha(swipeAlpha)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(offsetX.value) > 160f) {
                                coroutineScope.launch {
                                    val target = if (offsetX.value > 0) 600f else -600f
                                    offsetX.animateTo(target, tween(150))
                                    if (!stateItem.dismissed) {
                                        stateItem.dismissed = true
                                        stateItem.isVisible = false
                                        delay(200)
                                        item.onDismiss?.invoke()
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, tween(150))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, tween(150))
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    )
                }
        ) {
            // 直接内联绘制通知内容，绕过 NemoSnackbar 的内嵌 AnimatedVisibility 导致的双重动画冲突
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(gradientColors))
                    .then(
                        if (item.onClick != null) {
                            Modifier.clickable(
                                onClick = item.onClick,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (item.actionText != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.actionText,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // 当完全不显示且已触发离场时，清理释放内存节点
    LaunchedEffect(stateItem.isVisible) {
        if (!stateItem.isVisible) {
            delay(350)
            onRemoveFromTree()
        }
    }
}

/**
 * 内联使用的渐变色（与 NemoSnackbar.getGradientColors 完全一致）
 */
private fun getStackGradientColors(type: NemoSnackbarType, isDarkTheme: Boolean): List<Color> {
    return when (type) {
        NemoSnackbarType.INFO -> {
            if (isDarkTheme) listOf(Color(0xFF3D3A50), Color(0xFF2B2930))
            else listOf(Color(0xFF0E68FF), Color(0xFF4A90D9))
        }
        NemoSnackbarType.SUCCESS -> {
            if (isDarkTheme) listOf(Color(0xFF2D4A3D), Color(0xFF1E3A2F))
            else listOf(Color(0xFF34C759), Color(0xFF28A745))
        }
        NemoSnackbarType.WARNING -> {
            if (isDarkTheme) listOf(Color(0xFF4A3D2D), Color(0xFF3A2F1E))
            else listOf(Color(0xFFFF9500), Color(0xFFE68A00))
        }
        NemoSnackbarType.ERROR -> {
            if (isDarkTheme) listOf(Color(0xFF4A2D2D), Color(0xFF3A1E1E))
            else listOf(Color(0xFFFF3B30), Color(0xFFE53935))
        }
    }
}
