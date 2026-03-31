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
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoText
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

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
    onShowRatingGuide: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isAutoAudioEnabled: Boolean = false,
    onToggleAutoAudio: ((Boolean) -> Unit)? = null,
    isShowAnswerDelayEnabled: Boolean = false,
    onToggleShowAnswerDelay: ((Boolean) -> Unit)? = null,
    showAnswerDelayDurationLabel: String = "1.0s",
    onCycleShowAnswerDelayDuration: (() -> Unit)? = null,
    canUndo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    menu: @Composable (() -> Unit)? = null
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
                                }
                            }
                        }
                    }
                }
            }

            // Progress Bar - MD3: 使用 LinearProgressIndicator 风格
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
        color = com.jian.nemo.core.designsystem.theme.NemoPrimary,
        modifier = Modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(Color(0xFFE6F0FF), RoundedCornerShape(20.dp))
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
    title: String = "今日目标达成！",
    subtitle: String = "坚持就是胜利，明天继续加油",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Hero Icon with Glow Effect
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                color = NemoPrimary.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = NemoPrimary.copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "完成",
                    tint = NemoPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Title & Subtitle
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NemoTextLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 3. Quote Card (Atmospheric)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
             Column(
                 modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                 Text(
                    text = "“温故而知新，可以为师矣。”",
                    style = MaterialTheme.typography.titleMedium,
                    color = NemoText,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
             }
        }
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
                    color = NemoPrimary.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccessTime, // 需要 import Icons.Rounded.AccessTime
                contentDescription = "Waiting",
                tint = NemoPrimary,
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
                color = NemoPrimary
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
            colors = ButtonDefaults.buttonColors(containerColor = NemoPrimary),
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


