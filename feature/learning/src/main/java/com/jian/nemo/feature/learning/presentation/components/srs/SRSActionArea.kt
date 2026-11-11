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
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.LearningPalette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.feature.learning.R
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

private sealed interface ActionAreaState {
    data object ShowAnswer : ActionAreaState
    data class Rating(val intervals: Map<Int, String>) : ActionAreaState
}

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

    // 评分按钮纯实色定义 - 严格遵循项目调色盘手册（单一纯实色，零透明度，不分模式）
    val buttonContainerColor = MaterialTheme.colorScheme.surface
    val effectiveAgainColor = SystemColors.Red
    val effectiveHardColor = SystemColors.Orange
    val effectiveGoodColor = SystemColors.Blue
    val effectiveEasyColor = SystemColors.Green

    val effectiveStarColor = if (isDarkTheme) SystemColors.YellowDark else SystemColors.Yellow

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

    val contentState = if (isAnswerShown) {
        ActionAreaState.Rating(ratingIntervals)
    } else {
        ActionAreaState.ShowAnswer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp) // 仅保留基础内容间距，安全区域由父容器 Scaffold 统一处理
    ) {
        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "actionAreaTransition"
        ) { state ->
            when (state) {
                is ActionAreaState.ShowAnswer -> {
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
                            shape = RoundedRectangle(16.dp),
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
                                    imageVector = if (canShowAnswer) Icons.Rounded.AutoAwesome else Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = if (canShowAnswer) effectiveStarColor else Color.White.copy(alpha = 0.88f),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(18.dp)
                                )
                                Text(
                                    text = if (canShowAnswer) {
                                        stringResource(R.string.learning_srs_action_show_answer)
                                    } else {
                                        stringResource(R.string.learning_srs_action_show_answer_countdown, countdownText)
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = if (canShowAnswer) 1f else 0.9f)
                                )
                            }
                        }

                    }
                }
                is ActionAreaState.Rating -> {
                    // 评分按钮网格 (Anki Mode: 4个按钮)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. 重来 (Again) - Quality 1
                        SRSRatingButton(
                            label = stringResource(R.string.learn_srs_rating_again),
                            time = state.intervals[1] ?: "-",
                            color = effectiveAgainColor,
                            containerColor = buttonContainerColor,
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
                            label = stringResource(R.string.learn_srs_rating_hard),
                            time = state.intervals[3] ?: "-",
                            color = effectiveHardColor,
                            containerColor = buttonContainerColor,
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
                            label = stringResource(R.string.learn_srs_rating_good),
                            time = state.intervals[4] ?: "-",
                            color = effectiveGoodColor,
                            containerColor = buttonContainerColor,
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
                            label = stringResource(R.string.learn_srs_rating_easy),
                            time = state.intervals[5] ?: "-",
                            color = effectiveEasyColor,
                            containerColor = buttonContainerColor,
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
}
