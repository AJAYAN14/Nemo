package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
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
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
    onOpenShowAnswerDelayDialog: (() -> Unit)? = null,
    isAutoRevealAnswerEnabled: Boolean = false,
    onToggleAutoRevealAnswer: ((Boolean) -> Unit)? = null,
    autoRevealAnswerDurationLabel: String = "5.0s",
    onCycleAutoRevealAnswerDuration: (() -> Unit)? = null,
    onOpenAutoRevealAnswerDialog: (() -> Unit)? = null,
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
    onToggleMenu: ((Boolean) -> Unit)? = null,
    backdrop: Backdrop? = null
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
                    FrostedLiquidGlassButton(
                        onClick = onClose,
                        shape = CircleShape,
                        backdrop = backdrop,
                        hasLens = true,
                        contentPadding = PaddingValues(0.dp),
                        isInteractive = true,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "返回",
                            tint = contentColor,
                            modifier = Modifier.size(30.dp)
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
                    // 撤销上一次评分按钮 (动态平滑展开/收起)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = onUndo != null && canUndo,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                    ) {
                        FrostedLiquidGlassButton(
                            onClick = { onUndo?.invoke() },
                            shape = CircleShape,
                            backdrop = backdrop,
                            hasLens = true,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = "撤销上一次评分",
                                tint = contentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // 【硬约束】剩余数量 (Remaining Count) 定义：
                    // 这里显示的是当前学习队列（wordList/grammarList）中的总数。
                    // 它是“待处理任务量”，而不是“页面浏览进度”。
                    // 只要卡片没有被评分（Rate）并移出队列，该数字在滑动浏览时不应减小。
                    val remainingCount = totalCount

                    if (remainingCount > 0) {
                        FrostedLiquidGlassButton(
                            onClick = {},
                            shape = CircleShape,
                            backdrop = backdrop,
                            hasLens = true,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            isInteractive = true,
                            modifier = Modifier.height(48.dp)
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
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // More Menu
                    val canShowMenu = true
                    if (canShowMenu) {
                        if (menu != null) {
                            menu()
                        } else {
                            var internalExpanded by remember { mutableStateOf(false) }
                            val expanded = if (onToggleMenu != null) isMenuExpanded else internalExpanded
                            val setExpanded: (Boolean) -> Unit = { expandedValue ->
                                internalExpanded = expandedValue
                                onToggleMenu?.invoke(expandedValue)
                            }

                            NemoMorphMenu(
                                expanded = expanded,
                                onExpandedChange = setExpanded,
                                backdrop = backdrop,
                                buttonSize = 48.dp,
                                menuWidth = 240.dp,
                                icon = Icons.Rounded.MoreVert,
                                contentDescription = "更多选项"
                            ) {
                                if (onShowRatingGuide != null) {
                                    NemoMorphMenuItem(
                                        text = "评分说明（新学/复习）",
                                        leadingIcon = Icons.Rounded.CheckCircle,
                                        onClick = {
                                            close()
                                            onShowRatingGuide()
                                        }
                                    )

                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    )
                                }

                                if (remainingCount > 0) {
                                    NemoMorphMenuItem(
                                        text = "暂停此卡片 (Suspend)",
                                        leadingIcon = Icons.Rounded.Pause,
                                        onClick = {
                                            close()
                                            onSuspend()
                                        }
                                    )
                                    NemoMorphMenuItem(
                                        text = "今日暂缓此项 (Bury)",
                                        leadingIcon = Icons.Rounded.AccessTime,
                                        onClick = {
                                            close()
                                            onBury()
                                        }
                                    )

                                    NemoMorphMenuItem(
                                        text = "报告条目错误",
                                        leadingIcon = Icons.Rounded.Report,
                                        onClick = {
                                            close()
                                            onReportError()
                                        }
                                    )

                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    )
                                }

                                // 自动朗读开关
                                if (onToggleAutoAudio != null) {
                                    MorphMenuToggleItem(
                                        text = "翻面自动朗读",
                                        checked = isAutoAudioEnabled,
                                        onCheckedChange = { onToggleAutoAudio(it) }
                                    )
                                }

                                if (onToggleShowAnswerDelay != null) {
                                    MorphMenuToggleItem(
                                        text = "显示答案等待",
                                        checked = isShowAnswerDelayEnabled,
                                        onCheckedChange = { onToggleShowAnswerDelay(it) }
                                    )

                                    if (onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
                                        NemoMorphMenuItem(
                                            text = "等待时长: $showAnswerDelayDurationLabel",
                                            leadingIcon = Icons.Rounded.Timer,
                                            onClick = {
                                                close()
                                                if (onOpenShowAnswerDelayDialog != null) {
                                                    onOpenShowAnswerDelayDialog()
                                                } else {
                                                    onCycleShowAnswerDelayDuration?.invoke()
                                                }
                                            }
                                        )
                                    }
                                }

                                if (onToggleAutoRevealAnswer != null) {
                                    MorphMenuToggleItem(
                                        text = "自动翻面",
                                        checked = isAutoRevealAnswerEnabled,
                                        onCheckedChange = { onToggleAutoRevealAnswer(it) }
                                    )

                                    if (onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null) {
                                        NemoMorphMenuItem(
                                            text = "翻面时长: $autoRevealAnswerDurationLabel",
                                            leadingIcon = Icons.Rounded.Timer,
                                            onClick = {
                                                close()
                                                if (onOpenAutoRevealAnswerDialog != null) {
                                                    onOpenAutoRevealAnswerDialog()
                                                } else {
                                                    onCycleAutoRevealAnswerDuration?.invoke()
                                                }
                                            }
                                        )
                                    }
                                }

                                if (onToggleWhiteboard != null) {
                                    val whiteboardLabel = if (isWhiteboardEnabled) "已开启" else "已关闭"
                                    NemoMorphMenuItem(
                                        text = "手写白板: $whiteboardLabel",
                                        leadingIcon = Icons.Rounded.Edit,
                                        onClick = {
                                            onToggleWhiteboard(!isWhiteboardEnabled)
                                        }
                                    )
                                }

                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )

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

                                NemoMorphMenuItem(
                                    text = "显示模式: $themeLabel",
                                    leadingIcon = themeIcon,
                                    onClick = {
                                        onCycleDarkMode()
                                    }
                                )
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
                val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
                val effectiveProgressColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else MaterialTheme.colorScheme.primary

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
                                color = effectiveProgressColor,
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
                    val defaultNewColor = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)
                    val newColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else defaultNewColor
                    Text(
                        text = "$queueNewCount",
                        color = newColor.copy(alpha = if (queueNewCount > 0) 1f else 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 在学 (LEARNING) - 青色
                    val defaultLearningColor = if (isDarkTheme) Color(0xFF22D3EE) else Color(0xFF0891B2)
                    val learningColor = if (isPlumpDuo) IsoluminantPalette.LakesideCyan else defaultLearningColor
                    Text(
                        text = "$queueLearningCount",
                        color = learningColor.copy(alpha = if (queueLearningCount > 0) 1f else 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 复习 (REVIEW) - 绿色
                    val defaultReviewColor = if (isDarkTheme) Color(0xFFBBF7D0) else Color(0xFF166534)
                    val reviewColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else defaultReviewColor
                    Text(
                        text = "$queueReviewCount",
                        color = reviewColor.copy(alpha = if (queueReviewCount > 0) 1f else 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 重学 (RELEARN) - 橙色
                    val defaultRelearnColor = if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412)
                    val relearnColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else defaultRelearnColor
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
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
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

// ==========================================
// 现代化短时记忆冷却仓组件 (Learn Ahead Limit Waiting Chamber)
// 100% 对齐 gemini-code-1792823982566.html 设计规范
// 采用与登录界面完全一致的 WebGL 2 原生粒子流体背景 + 精美 Compose 交互层
// ==========================================

/**
 * 等待界面的全屏流光粒子特效背景层
 * 可挂载在不受安全内边距限制的最外层容器，铺满至手机物理边缘和状态栏
 */
@Composable
fun WaitingBackground(
    backdrop: LayerBackdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onControllerReady: ((triggerCenterBlast: () -> Unit) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
    ) {
        PixelBlastBackground(
            isDark = isDark,
            primaryColor = primaryColor,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize(),
            onControllerReady = onControllerReady
        )

        // 纵深光影渐变遮罩层（增强文字与圆环在 WebGL 粒子之上的清晰度与呼吸层次）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color(0x33090714),
                            Color(0x05090714),
                            Color(0x33090714),
                            Color(0x66090714)
                        ) else listOf(
                            Color(0x66FFFFFF),
                            Color(0x10FFFFFF),
                            Color(0x55F8FAFC),
                            Color(0x88F1F5F9)
                        )
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaitingContent(
    until: Long,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onRest: (() -> Unit)? = null,
    completedCount: Int = 0,
    queueCount: Int = 0,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    backdrop: LayerBackdrop? = null,
    onTriggerCenterBlast: (() -> Unit)? = null,
    showBackground: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val localBackdrop = rememberLayerBackdrop()
    val chamberBackdrop = backdrop ?: localBackdrop
    val interactiveHighlight = remember(coroutineScope) {
        InteractiveHighlight(animationScope = coroutineScope)
    }

    var localTriggerCenterBlast by remember { mutableStateOf<(() -> Unit)?>(null) }
    val triggerCenterBlast = onTriggerCenterBlast ?: localTriggerCenterBlast
    var remainingSeconds by remember { mutableStateOf(0L) }

    // 初始等待周期（秒），用于计算圆环进度流逝
    val initialTotalSeconds = remember(until) {
        val diff = (until - System.currentTimeMillis()) / 1000L
        if (diff > 0) diff.coerceAtLeast(60L).toFloat() else 600f
    }

    // 倒计时循环
    androidx.compose.runtime.LaunchedEffect(until) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = (until - now) / 1000
            if (diff <= 0) {
                onContinue() // 时间到，自动恢复并继续学习
                break
            }
            remainingSeconds = diff
            kotlinx.coroutines.delay(1000L)
        }
    }

    val minutes = (remainingSeconds / 60).coerceAtLeast(0L)
    val seconds = (remainingSeconds % 60).coerceAtLeast(0L)
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    // 快进矢量图标 (Learn Ahead)
    val learnAheadIcon = remember {
        ImageVector.Builder(
            name = "LearnAheadIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = androidx.compose.ui.graphics.SolidColor(Color.White),
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 4f)
                lineTo(15f, 12f)
                lineTo(5f, 20f)
                close()
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(19f, 5f)
                lineTo(19f, 19f)
            }
        }.build()
    }

    // 咖啡休息矢量图标 (Take Rest)
    val coffeeRestIcon = remember(isDark) {
        val strokeColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
        ImageVector.Builder(
            name = "CoffeeRestIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(strokeColor),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 8f)
                horizontalLineTo(19f)
                arcTo(4f, 4f, 0f, false, true, 19f, 16f)
                horizontalLineTo(18f)
                moveTo(2f, 8f)
                horizontalLineTo(18f)
                verticalLineTo(17f)
                arcTo(4f, 4f, 0f, false, true, 14f, 21f)
                horizontalLineTo(6f)
                arcTo(4f, 4f, 0f, false, true, 2f, 17f)
                verticalLineTo(8f)
                close()
                moveTo(6f, 2f)
                lineTo(6f, 5f)
                moveTo(10f, 2f)
                lineTo(10f, 5f)
                moveTo(14f, 2f)
                lineTo(14f, 5f)
            }
        }.build()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. 底层：原生 WebGL 2 PixelBlast 粒子流光波纹背景引擎 + 光影渐变遮罩层（挂载为物理透镜采样源）
        if (showBackground) {
            WaitingBackground(
                backdrop = chamberBackdrop,
                isDark = isDark,
                onControllerReady = { trigger ->
                    localTriggerCenterBlast = trigger
                }
            )
        }

        // 2. 业务前景交互层
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (showBackground) 24.dp else 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 2. 中央核心冷却仓视觉（纯圆形 + 真实物理凸透镜折射 + 原位弹性拉扯形变高光）
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .drawBackdrop(
                            backdrop = chamberBackdrop,
                            shape = { CircleShape },
                            effects = {
                                val minDimension = size.minDimension
                                vibrancy()
                                blur(2.5f.dp.toPx())
                                lens(
                                    refractionHeight = 0.1f * minDimension,
                                    refractionAmount = 0.2f * minDimension,
                                    depthEffect = true,
                                    chromaticAberration = false
                                )
                            },
                            highlight = { Highlight.Plain },
                            layerBlock = {
                                val width = size.width
                                val height = size.height

                                val progress = interactiveHighlight.pressProgress
                                val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                                val maxOffset = size.minDimension
                                val initialDerivative = 0.05f
                                val offset = interactiveHighlight.offset
                                // 限制在原位不被移走
                                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                                val maxDragScale = 4f.dp.toPx() / size.height
                                val offsetAngle = atan2(offset.y, offset.x)
                                scaleX =
                                    scale +
                                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                            (width / height).fastCoerceAtMost(1f)
                                scaleY =
                                    scale +
                                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                            (height / width).fastCoerceAtMost(1f)
                            }
                        )
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                triggerCenterBlast?.invoke()
                            }
                        )
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier),
                    contentAlignment = Alignment.Center
                ) {
                    // 圆环内部文案（已取消外围进度弧线条）
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "复习冷却中",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeFormatted,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1E1B4B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "暂无已到期的卡片",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "当前词已进入短时记忆巩固阶梯。依据记忆曲线规律，稍候再次复习可达到最佳记忆留存效果。",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 19.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 3.3 底部卡片与决策按钮区
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 本轮学习战报卡片 (Audit Card)
                val displayCompleted = if (completedCount > 0) completedCount else 15
                val displayQueue = if (queueCount > 0) queueCount else 10

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xB2120F24) else Color(0xEBFFFFFF)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDark) Color(0x38A78BFA) else Color(0x297C3AED)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // 头部：时钟图标 + 战报标题 + 冷却胶囊
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF7C3AED),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "本轮学习战报",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF0F172A)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x26F59E0B))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "冷却中 $displayQueue 词",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }

                        // 数据网格
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 左项：本轮已学
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x0DFFFFFF) else Color(0xF2F1F5F9))
                                    .padding(horizontal = 11.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "本轮已学",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$displayCompleted 词",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "已推进",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            // 右项：待巩固卡片
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x0DFFFFFF) else Color(0xF2F1F5F9))
                                    .padding(horizontal = 11.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "待巩固卡片",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$displayQueue 词",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "正在冷却",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    }
                }

                // 决策按钮 1：提前复习 - 实验室同款彩色液态玻璃按钮（无描边，高度48dp）
                ColoredLiquidGlassButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onContinue()
                    },
                    backdrop = chamberBackdrop,
                    shape = RoundedCornerShape(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    border = null,
                    isInteractive = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = learnAheadIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "提前复习",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 决策按钮 2：稍后复习，去休息片刻 - 实验室同款带底色磨砂液态玻璃按钮（无描边，高度48dp）
                if (onRest != null) {
                    FrostedLiquidGlassButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRest()
                        },
                        backdrop = chamberBackdrop,
                        shape = RoundedCornerShape(14.dp),
                        surfaceColor = if (isDark) Color(0xFF262038).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.55f),
                        border = null,
                        isInteractive = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = coffeeRestIcon,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "稍后复习，去休息片刻",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MorphMenuToggleItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    androidx.compose.material3.Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
        },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            com.jian.nemo.core.ui.component.liquid.LiquidToggle(
                checked = checked,
                onCheckedChange = onCheckedChange,
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}


