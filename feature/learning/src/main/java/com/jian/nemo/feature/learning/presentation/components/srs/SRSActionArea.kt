package com.jian.nemo.feature.learning.presentation.components.srs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.jian.nemo.core.ui.util.SoundEffectPlayer
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import kotlinx.coroutines.delay

/**
 * SRS 操作区域组件 (100% 还原 HTML Bottom Action Bar)
 *
 * @param isAnswerShown 是否已显示答案
 * @param ratingIntervals 评分对应的预估间隔文本 (Map<Int, String>)
 * @param onShowAnswer 点击显示答案
 * @param onRate 点击评分
 */
@Composable
fun SRSActionArea(
    isAnswerShown: Boolean,
    isShowAnswerDelayEnabled: Boolean = false,
    showAnswerAvailableAt: Long = 0L,
    ratingIntervals: Map<Int, String>,
    onShowAnswer: () -> Unit,
    onShowAnswerBlocked: ((Int) -> Unit)? = null,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val graceMs = 150L
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    // 检测深色模式
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // 评分按钮颜色定义 - 根据 MD3 规范区分浅色/深色模式
    // 浅色模式：600-700 文字色 + 50-100 容器色（高饱和度，高对比）
    // 深色模式：200-300 文字色 + 800-900 容器色（降低饱和度，避免视觉振动）

    val colorRose700 = if (isDarkTheme) LearningPalette.SrsRoseTextDark else LearningPalette.SrsRoseTextLight
    val colorRose100 = if (isDarkTheme) LearningPalette.SrsRoseBgDark else LearningPalette.SrsRoseBgLight

    val colorOrange600 = if (isDarkTheme) LearningPalette.SrsOrangeTextDark else LearningPalette.SrsOrangeTextLight
    val colorOrange50 = if (isDarkTheme) LearningPalette.SrsOrangeBgDark else LearningPalette.SrsOrangeBgLight

    val colorBlue600 = if (isDarkTheme) LearningPalette.SrsBlueTextDark else LearningPalette.SrsBlueTextLight
    val colorBlue50 = if (isDarkTheme) LearningPalette.SrsBlueBgDark else LearningPalette.SrsBlueBgLight

    val colorEmerald600 = if (isDarkTheme) LearningPalette.SrsEmeraldTextDark else LearningPalette.SrsEmeraldTextLight
    val colorEmerald50 = if (isDarkTheme) LearningPalette.SrsEmeraldBgDark else LearningPalette.SrsEmeraldBgLight

    val isDelayActive = isShowAnswerDelayEnabled || showAnswerAvailableAt > 0L

    val now by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = isAnswerShown,
        key2 = isDelayActive,
        key3 = showAnswerAvailableAt
    ) {
        while (!isAnswerShown && isDelayActive && showAnswerAvailableAt > System.currentTimeMillis()) {
            value = System.currentTimeMillis()
            delay(100L)
        }
        value = System.currentTimeMillis()
    }

    val remainingMs = if (!isAnswerShown && isDelayActive) {
        (showAnswerAvailableAt - now).coerceAtLeast(0L)
    } else {
        0L
    }
    val canShowAnswer = remainingMs <= graceMs
    val remainingSec = ((remainingMs + 999L) / 1000L).toInt()
    val countdownText = if (remainingSec >= 60) "${remainingSec / 60}m ${remainingSec % 60}s" else "${remainingSec}s"


    val lastRateClickTime = androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val safeRate: (Int, () -> Unit) -> Unit = { quality, feedback ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRateClickTime.longValue >= 200L) {
            lastRateClickTime.longValue = currentTime
            feedback()
            onRate(quality)
        }
    }

    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    // 4个评分按钮在双模式下的自适应配色
    val effectiveAgainColor = if (isPlumpDuo) IsoluminantPalette.CoralFlame else colorRose700
    val effectiveAgainContainer = if (isPlumpDuo) IsoluminantPalette.CoralFlame.copy(alpha = if (isDarkTheme) 0.12f else 0.08f) else colorRose100

    val effectiveHardColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else colorOrange600
    val effectiveHardContainer = if (isPlumpDuo) IsoluminantPalette.WarmAmber.copy(alpha = if (isDarkTheme) 0.12f else 0.08f) else colorOrange50

    val effectiveGoodColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else colorBlue600
    val effectiveGoodContainer = if (isPlumpDuo) IsoluminantPalette.SkyIndigo.copy(alpha = if (isDarkTheme) 0.12f else 0.08f) else colorBlue50

    val effectiveEasyColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else colorEmerald600
    val effectiveEasyContainer = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen.copy(alpha = if (isDarkTheme) 0.12f else 0.08f) else colorEmerald50

    val effectiveStarColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else LearningPalette.StarAmber

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp) // 仅保留基础内容间距，安全区域由父容器 Scaffold 统一处理
    ) {
        AnimatedContent(
            targetState = isAnswerShown,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "actionAreaTransition"
        ) { showAnswer ->
            if (!showAnswer) {
                // 显示答案按钮
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidButton(
                        onClick = {
                            if (canShowAnswer) {
                                onShowAnswer()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShowAnswerBlocked?.invoke(remainingSec)
                            }
                        },
                        backgroundColor = if (canShowAnswer) LearningPalette.ActionDarkBg else LearningPalette.ActionMutedText,
                        shape = RoundedCornerShape(16.dp),
                        elevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (canShowAnswer) Icons.Default.AutoAwesome else Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (canShowAnswer) effectiveStarColor else Color.White.copy(alpha = 0.88f),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp)
                            )
                            Text(
                                text = if (canShowAnswer) "显示答案" else "显示答案 ($countdownText)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = if (canShowAnswer) 1f else 0.9f)
                            )
                        }
                    }

                }
            } else {
                // 评分按钮网格 (Anki Mode: 4个按钮)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 重来 (Again) - Quality 1
                    SRSRatingButton(
                        label = "重来",
                        time = ratingIntervals[1] ?: "-",
                        color = effectiveAgainColor,
                        containerColor = effectiveAgainContainer,
                        onClick = { 
                            safeRate(1) {
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                SoundEffectPlayer.playOtherSound(context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 2. 困难 (Hard) - Quality 3
                    SRSRatingButton(
                        label = "困难",
                        time = ratingIntervals[3] ?: "-",
                        color = effectiveHardColor,
                        containerColor = effectiveHardContainer,
                        onClick = { 
                            safeRate(3) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                SoundEffectPlayer.playHardSound(context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 3. 良好 (Good) - Quality 4
                    SRSRatingButton(
                        label = "良好",
                        time = ratingIntervals[4] ?: "-",
                        color = effectiveGoodColor,
                        containerColor = effectiveGoodContainer,
                        onClick = { 
                            safeRate(4) {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                SoundEffectPlayer.playGoodSound(context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 4. 简单 (Easy) - Quality 5
                    SRSRatingButton(
                        label = "简单",
                        time = ratingIntervals[5] ?: "-",
                        color = effectiveEasyColor,
                        containerColor = effectiveEasyContainer,
                        onClick = { 
                            safeRate(5) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                SoundEffectPlayer.playEasySound(context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
