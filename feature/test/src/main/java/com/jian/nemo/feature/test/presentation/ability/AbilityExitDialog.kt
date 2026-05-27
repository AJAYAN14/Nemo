package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import kotlinx.coroutines.launch

/**
 * 能力工坊通用退出确认弹窗 - UI/UX Pro Max 顶级拟物视觉与微动效重塑版
 */
@Composable
fun AbilityExitDialog(
    show: Boolean,
    title: String,
    message: String,
    themeColor: Color,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    onDismiss: () -> Unit,
    onKeepAndExit: () -> Unit,
    onDestroyAndExit: () -> Unit
) {
    if (!show) return

    // 物理弹性飞入与渐显微动效控制
    val scale = remember { Animatable(0.88f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = EaseOut)
            )
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    alpha = alpha.value
                )
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.15f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.15f)
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            )
        ) {
            Column(
                modifier = Modifier.padding(top = 32.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 双重渐变放射发光立体图标圈 (Depth & Glow Bubble)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.22f),
                                    themeColor.copy(alpha = 0.02f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            BorderStroke(1.5.dp, themeColor.copy(alpha = 0.15f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = themeColor.copy(alpha = 0.4f),
                                spotColor = themeColor.copy(alpha = 0.4f)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        themeColor,
                                        themeColor.copy(alpha = 0.82f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ExitToApp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. 标题排版微调 (ExtraBold Heading)
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textMain,
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. 段落文案精细排版
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textSub,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4. 视觉主路径按钮：大圆角线性双色渐变胶囊主按钮 (保留退出)
                Button(
                    onClick = onKeepAndExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = themeColor.copy(alpha = 0.35f),
                            spotColor = themeColor.copy(alpha = 0.35f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        themeColor,
                                        themeColor.copy(alpha = 0.85f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "保留并退出挑战",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. 并排次路径按钮：左高危销毁 vs 右温和继续 (安全人机工学)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 销毁退出 (高危警告动作，放左侧)
                    Button(
                        onClick = onDestroyAndExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IosColors.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "销毁进度",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    // 继续挑战 (取消/返回答题，突出主操作)
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) NemoNeutrals.Gray800 else NemoNeutrals.Gray100
                        )
                    ) {
                        Text(
                            text = "继续挑战",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else NemoNeutrals.Gray700
                            )
                        )
                    }
                }
            }
        }
    }
}
