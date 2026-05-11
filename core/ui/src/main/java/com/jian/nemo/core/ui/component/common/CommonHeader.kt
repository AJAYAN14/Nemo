package com.jian.nemo.core.ui.component.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 通用带返回按钮的顶部栏组件
 *
 * 符合 Material Design 3 TopAppBar 规范
 * 适用于需要返回功能的界面，已自动处理状态栏padding
 *
 * [Important Note / 注意事项]:
 * 本组件内部使用了 `Modifier.statusBarsPadding()`。
 * 如果父容器（如 Scaffold）已经处理了 windowInsets 或 paddingValues，
 * 请务必移除父容器传递给本组件的 top padding，否则会导致双重 padding，
 * 使 Title 看起来位置偏下。
 *
 * @param title 标题文本
 * @param onBack 返回按钮回调
 * @param backgroundColor 背景颜色，默认为透明
 * @param actions 可选的右侧操作按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonHeader(
    title: String,
    onBack: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    avatarUrl: String? = null,
    username: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    // MD3 TopAppBar 使用 Surface 提供容器
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            // MD3: TopAppBar 标准内容高度 64dp (包含 padding)
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge, // MD3: 标准 titleLarge (22sp)
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    // MD3: 标准 48dp × 48dp 触摸目标
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface // MD3: 使用主题颜色
                        )
                    }
                },
                actions = {
                    // 如果提供了 actions，则显示
                    actions?.invoke(this)

                    // Avatar Area
                    if (username != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp) // Optimized size
                                .clickable(enabled = onAvatarClick != null, onClick = { onAvatarClick?.invoke() }),
                            contentAlignment = Alignment.Center
                        ) {
                            com.jian.nemo.core.ui.component.AvatarImage(
                                username = username,
                                avatarPath = avatarUrl,
                                size = 36.dp,
                                borderWidth = 1.dp, // Subtle border
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
