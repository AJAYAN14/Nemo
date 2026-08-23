package com.jian.nemo.core.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.os.Build
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.modifier.softCardShadow

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
 * @param hazeState 可选的 HazeState，若传入则启用与底部导航栏一致的高斯毛玻璃效果
 * @param actions 可选的右侧操作按钮
 */
@Composable
fun CommonHeader(
    title: String,
    onBack: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    hazeState: HazeState? = null,
    avatarUrl: String? = null,
    username: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    centerContent: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val navGroupBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White

    // 纯色实体顶栏背景（恢复经典纯色风格，平滑遮挡滚动内容）
    val actualBackgroundColor = if (backgroundColor != Color.Transparent) {
        backgroundColor
    } else {
        MaterialTheme.colorScheme.background
    }

    val headerModifier = Modifier
        .fillMaxWidth()
        .background(actualBackgroundColor)

    Box(
        modifier = headerModifier
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：返回按钮 + 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    LiquidButton(
                        onClick = onBack,
                        backgroundColor = navGroupBg,
                        shape = CircleShape,
                        elevation = 0.dp,
                        isInteractive = true,
                        modifier = Modifier
                            .softCardShadow(borderRadius = 22.dp, isDark = isDarkTheme)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (centerContent == null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // 中间自定义内容（若指定）
                if (centerContent != null) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        centerContent()
                    }
                }

                // 右侧：Actions 与头像
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    actions?.invoke(this)

                    if (username != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(enabled = onAvatarClick != null, onClick = { onAvatarClick?.invoke() }),
                            contentAlignment = Alignment.Center
                        ) {
                            com.jian.nemo.core.ui.component.AvatarImage(
                                username = username,
                                avatarPath = avatarUrl,
                                size = 36.dp,
                                borderWidth = 1.dp,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
