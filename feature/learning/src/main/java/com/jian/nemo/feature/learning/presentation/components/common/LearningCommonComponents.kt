package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.jian.nemo.core.ui.component.common.NemoDropdownMenu
import com.jian.nemo.core.ui.component.common.NemoMenuItem
import androidx.compose.material3.MaterialTheme
import com.jian.nemo.core.ui.component.common.NemoGooeyToggle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.feature.learning.presentation.LearningMode
import androidx.compose.ui.graphics.toArgb
import com.jian.nemo.core.designsystem.theme.NemoText
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.List
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity

/**
 * 按压缩放效果 (Scale on Press)
 * 按下时缩小到 targetScale，松开时回弹
 */
@Composable
fun Modifier.scaleOnPress(
    targetScale: Float = 0.95f,
    onTap: (() -> Unit)? = null
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnTap by rememberUpdatedState(onTap)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        label = "scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    currentOnTap?.invoke()
                }
            )
        }
}

// 学习界面头部组件 (SRS 样式)
// 学习界面头部组件 - 遵循 Material Design 3 TopAppBar 规范
@Composable
fun LearnHeader(
    learningMode: LearningMode,
    completedCount: Int,
    dailyGoal: Int,
    currentIndex: Int,
    totalCount: Int,
    isNavigating: Boolean = false,
    isAnswerShown: Boolean = false,
    onClose: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSuspend: () -> Unit,
    onBury: () -> Unit,
    onReportError: () -> Unit,
    onShowRatingGuide: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isAutoAudioEnabled: Boolean = false,
    onToggleAutoAudio: ((Boolean) -> Unit)? = null,
    isShowAnswerDelayEnabled: Boolean = false,
    onToggleShowAnswerDelay: ((Boolean) -> Unit)? = null,
    showAnswerDelayDurationLabel: String = "1.0s",
    onCycleShowAnswerDelayDuration: (() -> Unit)? = null,
    isAutoRevealAnswerEnabled: Boolean = false,
    onToggleAutoRevealAnswer: ((Boolean) -> Unit)? = null,
    autoRevealAnswerDurationLabel: String = "5.0s",
    onCycleAutoRevealAnswerDuration: (() -> Unit)? = null,
    isWhiteboardEnabled: Boolean = false,
    onToggleWhiteboard: ((Boolean) -> Unit)? = null,
    canUndo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    menu: @Composable (() -> Unit)? = null,
    isDarkMode: Boolean? = null,
    onCycleDarkMode: () -> Unit = {},
    queueNewCount: Int = 0,
    queueLearningCount: Int = 0,
    queueReviewCount: Int = 0,
    queueRelearnCount: Int = 0
) {
    val progress = if (dailyGoal > 0) completedCount.toFloat() / dailyGoal else 0f



    // MD3: 使用 MaterialTheme 的颜色系统
    val contentColor = MaterialTheme.colorScheme.onSurface

    // 导航按钮组背景：深色模式用半透明白色，浅色模式用纯白色
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val navGroupBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White

    val progressBackground = MaterialTheme.colorScheme.surfaceVariant
    val context = LocalContext.current

    // 震动辅助函数
    @android.annotation.SuppressLint("MissingPermission")
    fun performHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(30)
                }
            }
        } catch (e: Exception) {
            // 忽略震动失败
        }
    }

    // MD3: 使用 Surface 提供容器结构，但背景透明以融入界面
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent, // 透明背景，与整个界面背景色一致
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp) // MD3: leading 4dp, trailing 24dp (内部调整)
        ) {
            // Top Row - MD3: 标准 64dp 高度的主要内容行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // MD3: TopAppBar 内容高度 56dp
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Navigation Icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // MD3: 使用 IconButton，标准触摸目标 48dp × 48dp
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = contentColor
                        )
                    }

                    // MD3: 标题使用 titleLarge (22sp)
                    Text(
                        text = if(learningMode == LearningMode.Word) "单词学习" else "语法学习",
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor,
                        modifier = Modifier.padding(start = 8.dp) // MD3: navigation 和 title 之间间距
                    )
                }

                // Right: Navigation Group & Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 【硬约束】剩余数量 (Remaining Count) 定义：
                    // 这里显示的是当前学习队列（wordList/grammarList）中的总数。
                    // 它是“待处理任务量”，而不是“页面浏览进度”。
                    // 只要卡片没有被评分（Rate）并移出队列，该数字在滑动浏览时不应减小。
                    val remainingCount = totalCount

                    if (remainingCount > 0) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier, // Removed padding(end) to accommodate menu
                            color = navGroupBg,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Prev Button
                                val canGoPrev = currentIndex > 0 && !isNavigating && !isAnswerShown
                                IconButton(
                                    onClick = {
                                        performHapticFeedback()
                                        onPrev()
                                    },
                                    enabled = canGoPrev,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "上一个",
                                        tint = contentColor.copy(alpha = if (canGoPrev) 1f else 0.38f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Count Text
                                Text(
                                    text = "剩余 $remainingCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = contentColor.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )

                                // Next Button
                                val canGoNext = currentIndex < totalCount - 1 && !isNavigating && !isAnswerShown
                                IconButton(
                                    onClick = {
                                        performHapticFeedback()
                                        onNext()
                                    },
                                    enabled = canGoNext,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "下一个",
                                        tint = contentColor.copy(alpha = if (canGoNext) 1f else 0.38f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // More Menu
                    val canShowMenu = remainingCount > 0 || onUndo != null
                    if (canShowMenu) {
                        if (menu != null) {
                            menu()
                        } else {
                            Box {
                                var expanded by remember { mutableStateOf(false) }

                                IconButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            navGroupBg,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "更多选项",
                                        tint = contentColor
                                    )
                                }

                                NemoDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    if (onUndo != null && canUndo) {
                                        NemoMenuItem(
                                            text = "撤销上一次评分",
                                            onClick = {
                                                expanded = false
                                                onUndo()
                                            },
                                            leadingIcon = Icons.AutoMirrored.Rounded.Undo
                                        )

                                        androidx.compose.material3.HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    if (onShowRatingGuide != null) {

                                        NemoMenuItem(
                                            text = "评分说明（新学/复习）",
                                            onClick = {
                                                expanded = false
                                                onShowRatingGuide()
                                            },
                                            leadingIcon = Icons.Rounded.CheckCircle
                                        )

                                        androidx.compose.material3.HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    if (remainingCount > 0) {
                                        NemoMenuItem(
                                            text = "暂停此卡片 (Suspend)",
                                            onClick = {
                                                expanded = false
                                                onSuspend()
                                            },
                                            leadingIcon = Icons.Rounded.Pause
                                        )
                                        NemoMenuItem(
                                            text = "今日暂缓此项 (Bury)",
                                            onClick = {
                                                expanded = false
                                                onBury()
                                            },
                                            leadingIcon = Icons.Rounded.AccessTime
                                        )

                                        NemoMenuItem(
                                            text = "报告条目错误",
                                            onClick = {
                                                expanded = false
                                                onReportError()
                                            },
                                            leadingIcon = Icons.Rounded.Report
                                        )
                                    }


                                    // 分隔线
                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(
                                            vertical = 4.dp
                                        )
                                    )

                                    // 自动朗读开关
                                    if (onToggleAutoAudio != null) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "翻面自动朗读",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    NemoGooeyToggle(
                                                        checked = isAutoAudioEnabled,
                                                        onCheckedChange = {
                                                            onToggleAutoAudio(it)
                                                        },
                                                        activeColor = MaterialTheme.colorScheme.primary,
                                                        inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                // 点击整个条目也切换
                                                onToggleAutoAudio(!isAutoAudioEnabled)
                                            }
                                        )
                                    }

                                    if (onToggleShowAnswerDelay != null) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "显示答案等待",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    NemoGooeyToggle(
                                                        checked = isShowAnswerDelayEnabled,
                                                        onCheckedChange = {
                                                            onToggleShowAnswerDelay(it)
                                                        },
                                                        activeColor = MaterialTheme.colorScheme.primary,
                                                        inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onToggleShowAnswerDelay(!isShowAnswerDelayEnabled)
                                            }
                                        )

                                        if (onCycleShowAnswerDelayDuration != null) {
                                            NemoMenuItem(
                                                text = "等待时长: $showAnswerDelayDurationLabel",
                                                onClick = {
                                                    onCycleShowAnswerDelayDuration()
                                                },
                                                leadingIcon = Icons.Rounded.Timer
                                            )
                                        }
                                    }

                                    if (onToggleAutoRevealAnswer != null) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "自动翻面",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    NemoGooeyToggle(
                                                        checked = isAutoRevealAnswerEnabled,
                                                        onCheckedChange = {
                                                            onToggleAutoRevealAnswer(it)
                                                        },
                                                        activeColor = MaterialTheme.colorScheme.primary,
                                                        inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onToggleAutoRevealAnswer(!isAutoRevealAnswerEnabled)
                                            }
                                        )

                                        if (onCycleAutoRevealAnswerDuration != null) {
                                            NemoMenuItem(
                                                text = "翻面时长: $autoRevealAnswerDurationLabel",
                                                onClick = {
                                                    onCycleAutoRevealAnswerDuration()
                                                },
                                                leadingIcon = Icons.Rounded.Timer
                                            )
                                        }
                                    }

                                    if (onToggleWhiteboard != null) {
                                        val whiteboardLabel = if (isWhiteboardEnabled) "已开启" else "已关闭"
                                        NemoMenuItem(
                                            text = "手写白板: $whiteboardLabel",
                                            onClick = {
                                                onToggleWhiteboard(!isWhiteboardEnabled)
                                            },
                                            leadingIcon = Icons.Rounded.Edit
                                        )
                                    }

                                    // 主题切换项 (循环模式)
                                    val themeLabel = when (isDarkMode) {
                                        null -> "跟随系统"
                                        true -> "深色模式"
                                        false -> "浅色模式"
                                    }
                                    val themeIcon = when (isDarkMode) {
                                        null -> Icons.Rounded.SettingsBrightness
                                        true -> Icons.Rounded.DarkMode
                                        false -> Icons.Rounded.LightMode
                                    }

                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    NemoMenuItem(
                                        text = "显示模式: $themeLabel",
                                        onClick = {
                                            onCycleDarkMode()
                                        },
                                        leadingIcon = themeIcon
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Progress Bar + Queue Badge Counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress Bar - MD3: 使用 LinearProgressIndicator 风格
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp) // MD3: 推荐的进度条高度 4dp
                        .background(progressBackground, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.primary, // MD3: 使用 primary 色
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }

                // 四色状态计数
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 新学 (NEW) - 蓝色
                    val newColor = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)
                    Text(
                        text = "$queueNewCount",
                        color = newColor.copy(alpha = if (queueNewCount > 0) 1f else 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 在学 (LEARNING) - 青色
                    val learningColor = if (isDarkTheme) Color(0xFF22D3EE) else Color(0xFF0891B2)
                    Text(
                        text = "$queueLearningCount",
                        color = learningColor.copy(alpha = if (queueLearningCount > 0) 1f else 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 复习 (REVIEW) - 绿色
                    val reviewColor = if (isDarkTheme) Color(0xFFBBF7D0) else Color(0xFF166534)
                    Text(
                        text = "$queueReviewCount",
                        color = reviewColor.copy(alpha = if (queueReviewCount > 0) 1f else 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 重学 (RELEARN) - 橙色
                    val relearnColor = if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412)
                    Text(
                        text = "$queueRelearnCount",
                        color = relearnColor.copy(alpha = if (queueRelearnCount > 0) 1f else 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 等级指示器组件
@Composable
fun LevelIndicator(level: String, onClick: () -> Unit) {
    Text(
        text = "JLPT $level",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// 语法子头部组件
@Composable
fun GrammarSubHeader(
    isGrammarDailyGoalMet: Boolean,
    todayLearnedGrammarCount: Int,
    grammarDailyGoal: Int,
    selectedGrammarLevel: String,
    onLevelClick: () -> Unit
) {
    // [Requirement Fix] 采用“剩余”逻辑
    val remaining = (grammarDailyGoal - todayLearnedGrammarCount).coerceAtLeast(0)
    val grammarProgressText = if (isGrammarDailyGoalMet) "今日已完成" else "剩余 $remaining / $grammarDailyGoal"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = grammarProgressText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        LevelIndicator(
            level = selectedGrammarLevel,
            onClick = onLevelClick
        )
    }
}


// 今日学习任务完成内容组件 (Premium Design)
@Composable
fun LearningFinishedContent(
    title: String = "今日任务达成！",
    subtitle: String = "坚持就是胜利，明天继续加油",
    completedToday: Int = 0,
    dailyGoal: Int = 20,
    sessionDurationSeconds: Long = 0L,
    sessionMaxCombo: Int = 0,
    sessionNewCount: Int = 0,
    sessionReviewCount: Int = 0,
    sessionRelearnCount: Int = 0,
    tomorrowReviewForecastCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // 🆕 结算页专属多色循环渐变过渡动画（薄荷绿 -> 珊瑚橙 -> 海蓝 -> 紫罗兰）
    val infiniteTransition = rememberInfiniteTransition(label = "morphColorTransition")
    val morphColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF10B981),
        targetValue = Color(0xFF10B981),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                Color(0xFF10B981) at 0 with LinearEasing
                Color(0xFFF97316) at 1500 with LinearEasing
                Color(0xFF0EA5E9) at 3000 with LinearEasing
                Color(0xFFA855F7) at 4500 with LinearEasing
                Color(0xFF10B981) at 6000 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "morphColor"
    )

    // 动画状态定义
    val outerScale = remember { Animatable(0f) }
    val innerScale = remember { Animatable(0f) }
    val centerScale = remember { Animatable(0.5f) }
    val centerAlpha = remember { Animatable(0f) }
    
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(30f) }
    
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(30f) }

    // 🆕 4组扁平卡片各自独立的动效状态
    val card1Alpha = remember { Animatable(0f) }
    val card1OffsetY = remember { Animatable(30f) }

    val card2Alpha = remember { Animatable(0f) }
    val card2OffsetY = remember { Animatable(30f) }

    val card3Alpha = remember { Animatable(0f) }
    val card3OffsetY = remember { Animatable(30f) }

    val card4Alpha = remember { Animatable(0f) }
    val card4OffsetY = remember { Animatable(30f) }
    
    val quoteAlpha = remember { Animatable(0f) }
    val quoteOffsetY = remember { Animatable(30f) }
    
    var showConfetti by remember { mutableStateOf(false) }

    // 震动辅助函数 (分级强度)
    @android.annotation.SuppressLint("MissingPermission")
    fun triggerVibrate(duration: Long, amplitude: Int) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255))
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(duration)
                }
            }
        } catch (e: Exception) {}
    }

    LaunchedEffect(Unit) {
        // T+100ms: 外环弹出 (Level 1)
        launch {
            delay(100)
            triggerVibrate(20, 50)
            outerScale.animateTo(1f, tween(400))
        }

        // T+200ms: 内环弹出 (Level 2)
        launch {
            delay(200)
            triggerVibrate(20, 100)
            innerScale.animateTo(1f, tween(400))
        }

        // T+400ms: 中心图标回弹入场 (Level 3)
        launch {
            delay(400)
            triggerVibrate(40, 180)
            launch { centerAlpha.animateTo(1f, tween(200)) }
            centerScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }

        // T+800ms: 文字滑入 (Level 4)
        launch {
            delay(800)
            triggerVibrate(15, 255)
            launch { titleAlpha.animateTo(1f, tween(600)) }
            titleOffsetY.animateTo(0f, tween(600))
        }

        launch {
            delay(900)
            launch { subtitleAlpha.animateTo(1f, tween(600)) }
            subtitleOffsetY.animateTo(0f, tween(600))
        }

        // 🆕 T+950ms: 卡片1滑入 (薄荷绿)
        launch {
            delay(950)
            launch { card1Alpha.animateTo(1f, tween(500)) }
            card1OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // 🆕 T+1050ms: 卡片2滑入 (珊瑚橙)
        launch {
            delay(1050)
            launch { card2Alpha.animateTo(1f, tween(500)) }
            card2OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // 🆕 T+1150ms: 卡片3滑入 (海蓝色)
        launch {
            delay(1150)
            launch { card3Alpha.animateTo(1f, tween(500)) }
            card3OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // 🆕 T+1250ms: 卡片4滑入 (紫罗兰)
        launch {
            delay(1250)
            launch { card4Alpha.animateTo(1f, tween(500)) }
            card4OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // T+1400ms: 极简名言卡片滑入
        launch {
            delay(1400)
            launch { quoteAlpha.animateTo(1f, tween(600)) }
            quoteOffsetY.animateTo(0f, tween(600))
        }

        // T+1500ms: 启动彩花与终极震动
        launch {
            delay(1500)
            triggerVibrate(50, 200)
            showConfetti = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Hero Icon with Animation (Morph Loading with Check)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = outerScale.value
                        scaleY = outerScale.value
                        alpha = outerScale.value.coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                // 播放平滑变色（多彩渐变）形变动画的加载器作为底座
                NemoChasingDotsLoader(
                    size = 120.dp,
                    color = morphColor,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 中间的打勾✓图标
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "完成",
                    tint = Color.White,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            scaleX = centerScale.value
                            scaleY = centerScale.value
                            alpha = centerAlpha.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Title & Subtitle with Animation
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = with(density) { titleOffsetY.value.dp.toPx() }
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NemoTextLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = subtitleAlpha.value
                        translationY = with(density) { subtitleOffsetY.value.dp.toPx() }
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🆕 3. 2x2 多彩色块扁平网格布局 (Flat Grid Card)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 第一行卡片 (时长 & 连击)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 卡片 1：学习用时 (清新薄荷绿)
                    FlatCard(
                        backgroundColor = Color(0xFF10B981),
                        alpha = card1Alpha.value,
                        offsetY = with(density) { card1OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.AccessTime)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "学习用时", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            val minutes = (sessionDurationSeconds / 60).coerceAtLeast(1)
                            Text(
                                text = "$minutes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "分钟",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 卡片 2：最高连击 (活力珊瑚橙)
                    FlatCard(
                        backgroundColor = Color(0xFFF97316),
                        alpha = card2Alpha.value,
                        offsetY = with(density) { card2OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.Star)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "最高连击", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$sessionMaxCombo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "次",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 第二行卡片 (预测 & 详情)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 卡片 3：明日复习 (明快海蓝色)
                    FlatCard(
                        backgroundColor = Color(0xFF0EA5E9),
                        alpha = card3Alpha.value,
                        offsetY = with(density) { card3OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.CalendarToday)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "明日复习", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$tomorrowReviewForecastCount",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "个",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 卡片 4：今日学习详情 (柔和紫罗兰)
                    FlatCard(
                        backgroundColor = Color(0xFFA855F7),
                        alpha = card4Alpha.value,
                        offsetY = with(density) { card4OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.List)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "今日学习详情", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DetailItem("新学", sessionNewCount)
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.3f)))
                            DetailItem("复习", sessionReviewCount)
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.3f)))
                            DetailItem("重学", sessionRelearnCount)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 极简名言卡片
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5
            val quoteBgColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                Color.White
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = quoteBgColor
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .graphicsLayer {
                        alpha = quoteAlpha.value
                        translationY = with(density) { quoteOffsetY.value.dp.toPx() }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "“温故而知新，可以为师矣。”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = if (isDark) NemoText else Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // 彩花特效层
        AnimatedVisibility(
            visible = showConfetti,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(1500)),
            modifier = Modifier.fillMaxSize()
        ) {
            val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
            val party = Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x10B981, primaryColorArgb),
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                position = Position.Relative(0.5, 0.3)
            )

            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(party)
            )
        }
    }
}

// 🆕 扁平色块战报网格辅助组件
@Composable
private fun FlatCard(
    backgroundColor: Color,
    alpha: Float,
    offsetY: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .height(152.dp)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            content()
        }
    }
}

@Composable
private fun CardHeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RowScope.DetailItem(label: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label, 
            fontSize = 10.sp, 
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$count", 
            fontSize = 18.sp, 
            fontWeight = FontWeight.Bold, 
            color = Color.White
        )
    }
}

// 兼容旧调用
@Composable
fun DailyGoalMetContent() {
    LearningFinishedContent()
}

// 内容不可用组件
@Composable
fun ContentUnavailable(text: String, cardColor: Color) {
    Box(
        modifier = Modifier.fillMaxWidth(), // Legacy fillMaxSize
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NemoTextLight,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// 等待界面组件 (Learn Ahead Limit)
@Composable
fun WaitingContent(
    until: Long,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember { mutableStateOf(0L) }

    // 倒计时
    androidx.compose.runtime.LaunchedEffect(until) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = (until - now) / 1000
            if (diff <= 0) {
                onContinue() // 时间到，自动继续
                break
            }
            remainingSeconds = diff
            kotlinx.coroutines.delay(1000L)
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = if (minutes > 0) "${minutes}分${seconds}秒" else "${seconds}秒"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccessTime, // 需要 import Icons.Rounded.AccessTime
                contentDescription = "Waiting",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "请稍候...",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "下一个学习内容将在",
            style = MaterialTheme.typography.bodyLarge,
            color = NemoTextLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = timeText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "后准备好",
            style = MaterialTheme.typography.bodyLarge,
            color = NemoTextLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = "立即学习 (Learn Ahead)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


