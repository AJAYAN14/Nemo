package com.jian.nemo.feature.settings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 主题颜色定义
 */
private data class ThemeColor(
    val name: String,
    val color: Color
)

/**
 * 主题色选择弹窗 (高保真还原 + 带文字标签)
 */
@Composable
fun ThemeColorDialog(
    currentColor: Long? = null,
    onDismiss: () -> Unit,
    onColorSelect: (Color) -> Unit
) {
    val themeColors = listOf(
        ThemeColor("默认", Color(0xFF0E68FF)), // 品牌原生蓝置顶
        ThemeColor("蔷薇红", Color(0xFFFF2D55)),
        ThemeColor("活力橙", Color(0xFFFF9500)),
        ThemeColor("明快黄", Color(0xFFFFCC00)),
        ThemeColor("薄荷绿", Color(0xFF4CD964)),
        ThemeColor("天空蓝", Color(0xFF5AC8FA)),
        ThemeColor("经典蓝", Color(0xFF007AFF)),
        ThemeColor("薰衣草紫", Color(0xFF5856D6)),
        ThemeColor("罗兰紫", Color(0xFFAF52DE)),
        ThemeColor("荧光青", Color(0xFF00E5FF)),
        ThemeColor("亮青绿", Color(0xFF1DE9B6)),
        ThemeColor("炽热红", Color(0xFFFF3D00))
    )

    // 用于展示 UI 被选中效果的本地状态，初始化为当前已保存的颜色
    var currentlySelectedColor by remember {
        mutableStateOf(currentColor?.let { Color(it.toULong()) })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 全屏遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x9914191E)), // 深色质感遮罩 (60% alpha)
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .width(290.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题区
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "界面配色",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                fontSize = 22.sp
                            ),
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "定制您的专属应用风格",
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 1.4.sp,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF888888)
                        )
                    }

                    // 3列 x 4行 颜色网格 (带文字标签)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp) // 增加间距以容纳文字
                    ) {
                        themeColors.chunked(3).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowColors.forEach { themeColor ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        HifiColorCircle(
                                            color = themeColor.color,
                                            isSelected = currentlySelectedColor == themeColor.color,
                                            onClick = { 
                                                currentlySelectedColor = themeColor.color
                                                onColorSelect(themeColor.color)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = themeColor.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color(0xFF888888)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 底部操作按钮 (灰色胶囊样式)
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0F2F5),
                            contentColor = Color(0xFF555555)
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .width(112.dp),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(0.dp),
                        elevation = null
                    ) {
                        Text(
                            text = "关闭",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HifiColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Q弹动画配置
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessLow
        ),
        label = "ColorScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp, // 稍微减细一点边框，让整体更协调
                        color = Color.White,
                        shape = CircleShape
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // 选中态中心小白点
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
            )
        }
    }
}
