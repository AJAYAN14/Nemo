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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import com.jian.nemo.core.ui.component.common.NemoDropdownMenu
import com.jian.nemo.core.ui.component.common.NemoMenuItem
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
import com.jian.nemo.core.ui.util.SoundEffectPlayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.util.Locale

/**
 * 可翻转卡片组件
 *
 * 参考: 旧项目 TestComponents.kt 行284-404
 */
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
    modifier: Modifier = Modifier
) {
    val cardState = card.state
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    // Flat UI Colors - Using Material Theme Semantics
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    // 颜色动画
    val backgroundColor by animateColorAsState(
        targetValue = when (cardState) {
            CardState.SELECTED -> primaryColor.copy(alpha = 0.1f)
            CardState.CORRECT -> secondaryColor.copy(alpha = 0.1f)
            CardState.INCORRECT -> errorColor.copy(alpha = 0.1f)
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
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        label = "borderColor",
        animationSpec = tween(300)
    )

    val textColor by animateColorAsState(
        targetValue = when (cardState) {
            CardState.SELECTED -> primaryColor
            CardState.CORRECT -> secondaryColor
            CardState.INCORRECT -> errorColor
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "textColor",
        animationSpec = tween(300)
    )

    // 缩放动画（选中时轻微放大）
    val scale by animateFloatAsState(
        targetValue = if (cardState == CardState.SELECTED) 1.02f else 1f,
        label = "scale",
        animationSpec = tween(300)
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

    androidx.compose.animation.AnimatedVisibility(
        modifier = modifier,
        visible = cardState != CardState.MATCHED,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        exit = shrinkVertically(
            animationSpec = tween(300),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
                .height(100.dp)
                .scale(scale)
                .clip(RoundedCornerShape(16.dp)) // 16dp Radius
                .border(
                    width = if (cardState == CardState.DEFAULT) 1.dp else 2.dp, // Thicker border for active states
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    enabled = cardState == CardState.DEFAULT || cardState == CardState.SELECTED,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (cardState in listOf(CardState.CORRECT, CardState.INCORRECT, CardState.SELECTED))
                        FontWeight.Bold
                    else
                        FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * 卡片内容区域(左右分栏)
 *
 * 参考: 旧项目 CardMatchingScreen.kt 行111-178
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardMatchingContentArea(
    termCards: List<MatchableCard>,
    definitionCards: List<MatchableCard>,
    onCardClick: (MatchableCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            FeedbackPanelState.COMPLETE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f) // Success -> Primary Container
            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
        }

        val textColor = when (feedbackState) {
            FeedbackPanelState.COMPLETE -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onErrorContainer
        }

        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 0.dp, // No shadow
            color = backgroundColor,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)), // Subtle border
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // Large top radius
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                // 按钮区域（仅COMPLETE状态且未自动跳转时显示）
                if (feedbackState == FeedbackPanelState.COMPLETE && !autoAdvance) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isLastQuestion) {
                            Button(
                                onClick = onNextGroup,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = textColor,
                                    contentColor = backgroundColor
                                )
                            ) {
                                Text("下一组")
                            }
                        }

                        OutlinedButton(
                            onClick = onFinish,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, textColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Text("完成测试")
                        }
                    }
                }

                // 自动跳转提示
                if (autoAdvance && isAutoAdvancing) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1500)
                        onNextGroup()
                    }
                }
            }
        }
    }
}

/**
 * 卡片题测试头部
 *
 * 参考: 旧项目 TestComponents.kt 行753-800
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardMatchingTestHeader(
    onBack: () -> Unit,
    timeLimitSeconds: Int,
    timeRemainingSeconds: Int,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }

        // 倒计时显示
        if (timeLimitSeconds > 0) {
            val minutes = timeRemainingSeconds / 60
            val seconds = timeRemainingSeconds % 60
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (timeRemainingSeconds < 60) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // 菜单入口按钮
        Box {
            var expanded by remember { mutableStateOf(false) }

            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多选项",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            NemoDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // 暂停测试选项
                NemoMenuItem(
                    text = "暂停测试",
                    onClick = {
                        expanded = false
                        onPause()
                    },
                    leadingIcon = Icons.Rounded.Pause
                )
            }
        }
    }
}
