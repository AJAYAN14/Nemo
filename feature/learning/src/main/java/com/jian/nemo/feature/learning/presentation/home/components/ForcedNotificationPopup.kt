package com.jian.nemo.feature.learning.presentation.home.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.domain.model.AppNotification

/**
 * 强制推送通知弹窗
 * @param notification 通知对象，null 时不显示
 * @param onDismiss 点击关闭时的回调，返回通知 ID
 * @param canDismissByBackdrop 是否可以通过点击背景阴影关闭
 */
@Composable
fun ForcedNotificationPopup(
    notification: AppNotification?,
    onDismiss: (String) -> Unit,
    canDismissByBackdrop: Boolean = false
) {
    if (notification != null) {
        Dialog(
            onDismissRequest = {
                if (canDismissByBackdrop) {
                    onDismiss(notification.id)
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val view = LocalView.current

            // 开启 Android 12+ 官方窗口级高斯毛玻璃 (Blur Behind)
            DisposableEffect(view) {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 48
                        dimAmount = 0.20f
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
                    .clickable(enabled = canDismissByBackdrop) {
                        onDismiss(notification.id)
                    },
                contentAlignment = Alignment.Center
            ) {
                ForcedNotificationCard(
                    notification = notification,
                    onDismiss = { onDismiss(notification.id) }
                )
            }
        }
    }
}

@Composable
private fun ForcedNotificationCard(
    notification: AppNotification,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ping")
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingScale"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingAlpha"
    )

    Surface(
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 400.dp)
            .clickable(enabled = false) { /* 防止点击卡片内部触发背景点击 */ },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A. 顶部图标 (Top Icon)
            Box(
                modifier = Modifier
                    .padding(top = 32.dp, bottom = 16.dp)
                    .size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                // 装饰光环 (Ping 效果)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = pingScale, scaleY = pingScale)
                        .background(Color(0xFFFFEBEE), CircleShape) // Red 50
                        .graphicsLayer(alpha = pingAlpha)
                )
                
                // 图标背景
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 标题
            Text(
                text = notification.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // B. 中间可滚动内容 (Middle Scrollable Content)
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 100.dp, max = 300.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = notification.body,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
                
                // 底部渐变掩罩
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                            )
                        )
                )
            }

            // C. 底部按钮 (Bottom Button)
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "我知道了",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
