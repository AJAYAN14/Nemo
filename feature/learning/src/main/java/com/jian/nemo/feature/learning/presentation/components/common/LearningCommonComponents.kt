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
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft

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
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.modifier.softCardShadow
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
    totalCount: Int,
    onClose: () -> Unit,
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
    queueRelearnCount: Int = 0,
    isMenuExpanded: Boolean = false,
    onToggleMenu: ((Boolean) -> Unit)? = null
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
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            // Top Row - 无剪裁弹性容器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Navigation Icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    LiquidButton(
                        onClick = onClose,
                        backgroundColor = navGroupBg,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        elevation = 0.dp,
                        isInteractive = true,
                        modifier = Modifier
                            .softCardShadow(borderRadius = 22.dp, isDark = isDarkTheme)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
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
                        LiquidButton(
                            onClick = {},
                            backgroundColor = navGroupBg,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            elevation = 0.dp,
                            isInteractive = true,
                            modifier = Modifier
                                .softCardShadow(borderRadius = 22.dp, isDark = isDarkTheme)
                                .height(44.dp)
                        ) {
                            val annotatedText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = contentColor.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append("剩余 ")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = contentColor.copy(alpha = 0.95f),
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("$remainingCount")
                                }
                            }
                            Text(
                                text = annotatedText,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // More Menu
                    val canShowMenu = remainingCount > 0 || onUndo != null
                    if (canShowMenu) {
                        if (menu != null) {
                            menu()
                        } else {
                            Box {
                                var internalExpanded by remember { mutableStateOf(false) }
                                val expanded = if (onToggleMenu != null) isMenuExpanded else internalExpanded
                                val setExpanded: (Boolean) -> Unit = { expandedValue ->
                                    internalExpanded = expandedValue
                                    onToggleMenu?.invoke(expandedValue)
                                }

                                LiquidButton(
                                    onClick = {
                                        performHapticFeedback()
                                        setExpanded(true)
                                    },
                                    backgroundColor = navGroupBg,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    elevation = 0.dp,
                                    modifier = Modifier
                                        .softCardShadow(borderRadius = 22.dp, isDark = isDarkTheme)
                                        .size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "更多选项",
                                        tint = contentColor
                                    )
                                }

                                NemoDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { setExpanded(false) }
                                ) {
                                    if (onUndo != null && canUndo) {
                                        NemoMenuItem(
                                            text = "撤销上一次评分",
                                            onClick = {
                                                performHapticFeedback()
                                                setExpanded(false)
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
                                                performHapticFeedback()
                                                setExpanded(false)
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
                                                performHapticFeedback()
                                                setExpanded(false)
                                                onSuspend()
                                            },
                                            leadingIcon = Icons.Rounded.Pause
                                        )
                                        NemoMenuItem(
                                            text = "今日暂缓此项 (Bury)",
                                            onClick = {
                                                performHapticFeedback()
                                                setExpanded(false)
                                                onBury()
                                            },
                                            leadingIcon = Icons.Rounded.AccessTime
                                        )

                                        NemoMenuItem(
                                            text = "报告条目错误",
                                            onClick = {
                                                performHapticFeedback()
                                                setExpanded(false)
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




// 内容不可用组件
@Composable
fun ContentUnavailable(text: String, cardColor: Color) {
    Box(
        modifier = Modifier.fillMaxWidth(), // Legacy fillMaxSize
        contentAlignment = Alignment.Center
    ) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .softCardShadow(borderRadius = 20.dp, isDark = isDark),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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


