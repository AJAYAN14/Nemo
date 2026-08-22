package com.jian.nemo.feature.test.presentation.cardmatching

import androidx.compose.foundation.ExperimentalFoundationApi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.CardState
import com.jian.nemo.core.domain.model.FeedbackPanelState
import com.jian.nemo.core.domain.model.MatchableCard
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.util.SoundEffectPlayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.designsystem.theme.IosColors


private val DISTINCT_PAIR_PALETTE = listOf(
    Color(0xFFF97316), // 1. 珊瑚橙 (Orange)
    Color(0xFF10B981), // 2. 翡翠亮绿 (Green)
    Color(0xFF3B82F6), // 3. 湛蓝色 (Blue)
    Color(0xFFF59E0B), // 4. 琥珀金黄 (Yellow/Orange)
    Color(0xFF8B5CF6), // 5. 亮罗兰紫 (Purple)
    Color(0xFF06B6D4), // 6. 青空蓝 (Cyan)
    Color(0xFFEC4899), // 7. 玫瑰粉 (Pink)
    Color(0xFF14B8A6)  // 8. 绿松石 (Teal)
)

private fun getPairColor(pairIndex: Int): Color {
    return DISTINCT_PAIR_PALETTE[Math.abs(pairIndex) % DISTINCT_PAIR_PALETTE.size]
}

/**
 * 可翻转卡片组件
 * Refactored to Flat UI
 */
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("MissingPermission")
@Composable
fun FlippableCard(
    card: MatchableCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pairIndex: Int = 0
) {
    val cardState = card.state
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val pairColor = getPairColor(pairIndex)

    // Flat UI Colors - Using Material Theme Semantics
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    // 颜色动画
    val backgroundColor by animateColorAsState(
        targetValue = when (cardState) {
            CardState.SELECTED -> primaryColor.copy(alpha = 0.15f)
            CardState.CORRECT -> secondaryColor.copy(alpha = 0.15f)
            CardState.INCORRECT -> errorColor.copy(alpha = 0.15f)
            CardState.MATCHED -> pairColor.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor",
        animationSpec = tween(300)
    )

    val borderColor by animateColorAsState(
        targetValue = when (cardState) {
            CardState.SELECTED -> primaryColor
            CardState.CORRECT -> secondaryColor
            CardState.INCORRECT -> errorColor
            CardState.MATCHED -> pairColor.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.8f else 0.5f
            )
        },
        label = "borderColor",
        animationSpec = tween(300)
    )

    val textColor by animateColorAsState(
        targetValue = when (cardState) {
            CardState.SELECTED -> primaryColor
            CardState.CORRECT -> secondaryColor
            CardState.INCORRECT -> errorColor
            CardState.MATCHED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "textColor",
        animationSpec = tween(300)
    )

    // 缩放动画（选中时轻微放大）
    val scale by animateFloatAsState(
        targetValue = if (cardState == CardState.SELECTED) 1.02f else 1f,
        label = "scale",
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
    )

    // 错误时触发震动 + 播放提示音；正确时播放提示音
    LaunchedEffect(cardState) {
        when (cardState) {
            CardState.CORRECT -> {
                SoundEffectPlayer.playCorrect(context)
            }
            CardState.INCORRECT -> {
                SoundEffectPlayer.playError(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }
            else -> {}
        }
    }

    // Haptic feedback
    val hapticFeedback = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(22.dp)
    val borderWidth = if (cardState == CardState.DEFAULT) 1.dp else 2.dp

    androidx.compose.animation.AnimatedVisibility(
        modifier = modifier,
        visible = true,
        enter = fadeIn() + scaleIn(
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
            initialScale = 0.8f
        ),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth()
                .scale(scale)
        ) {
            LiquidButton(
                onClick = {
                    if (cardState == CardState.DEFAULT || cardState == CardState.SELECTED) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                backgroundColor = backgroundColor,
                shape = cardShape,
                border = BorderStroke(borderWidth, borderColor),
                useSoftShadow = true,
                isInteractive = true,
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (cardState in listOf(CardState.CORRECT, CardState.INCORRECT, CardState.SELECTED, CardState.MATCHED))
                                FontWeight.Bold
                            else
                                FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    if (cardState == CardState.MATCHED) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(22.dp)
                                .background(pairColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "已配对",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 卡片内容区域(左右分栏)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardMatchingContentArea(
    termCards: List<MatchableCard>,
    definitionCards: List<MatchableCard>,
    onCardClick: (MatchableCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val pairIndexMap = remember(termCards) {
        termCards.mapIndexed { index, card -> card.id to index }.toMap()
    }

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 左列 - 汉字和假名卡片
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = termCards,
                key = { card -> "${card.id}_${card.type.name}" },
                contentType = { "FlippableCard" }
            ) { card ->
                FlippableCard(
                    card = card,
                    pairIndex = pairIndexMap[card.id] ?: 0,
                    onClick = { onCardClick(card) },
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                )
            }
        }

        // 右列 - 释义卡片
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = definitionCards,
                key = { card -> "${card.id}_${card.type.name}" },
                contentType = { "FlippableCard" }
            ) { card ->
                FlippableCard(
                    card = card,
                    pairIndex = pairIndexMap[card.id] ?: 0,
                    onClick = { onCardClick(card) },
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                )
            }
        }
    }
}

/**
 * 配对反馈面板
 * Refactored to Flat UI (Bottom Panel style)
 */
@Composable
fun MatchingFeedbackPanel(
    feedbackState: FeedbackPanelState,
    onFinish: () -> Unit,
    onNextGroup: () -> Unit,
    isLastQuestion: Boolean,
    autoAdvance: Boolean,
    wrongCount: Int,
    wrongLimit: Int,
    isAutoAdvancing: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = feedbackState != FeedbackPanelState.HIDDEN,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        val backgroundColor = when (feedbackState) {
            FeedbackPanelState.COMPLETE -> IosColors.Green.copy(alpha = 0.95f)
            FeedbackPanelState.INCORRECT -> IosColors.Red.copy(alpha = 0.92f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        }

        val textColor = when (feedbackState) {
            FeedbackPanelState.COMPLETE -> Color.White
            FeedbackPanelState.INCORRECT -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = backgroundColor,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // COMPLETE：成功图标
                if (feedbackState == FeedbackPanelState.COMPLETE) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 提示文本
                Text(
                    text = when (feedbackState) {
                        FeedbackPanelState.COMPLETE -> "配对成功！"
                        FeedbackPanelState.INCORRECT -> {
                            if (wrongCount >= wrongLimit) {
                                "错误次数过多，已跳过此题"
                            } else {
                                "配对错误，请重试"
                            }
                        }
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )

                // INCORRECT：错误次数可视化（实心 = 已犯错，半透明 = 剩余机会）
                if (feedbackState == FeedbackPanelState.INCORRECT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(wrongLimit) { index ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index < wrongCount) Color.White
                                        else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }

                // 自动跳转
                if (feedbackState == FeedbackPanelState.COMPLETE) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1500)
                        onNextGroup()
                    }
                }
            }
        }
    }
}
