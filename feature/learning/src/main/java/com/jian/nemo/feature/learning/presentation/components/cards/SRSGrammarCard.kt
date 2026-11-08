package com.jian.nemo.feature.learning.presentation.components.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.shape.UnevenRoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import com.jian.nemo.feature.learning.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoText
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import com.jian.nemo.core.designsystem.theme.NemoNeutralGray
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBorder
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBorderDark
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.text.FuriganaText
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.presentation.CardBadge

/**
 * 全新设计的语法学习卡片
 *
 * 设计理念：
 * - 极简问题卡片：只显示语法条目和标签
 * - 分层答案区域：接续→说明→例句→TIPS
 * - 清晰的视觉层级和舒适的阅读体验
 *
 * @param grammar 语法数据
 * @param isAnswerShown 是否显示答案
 * @param onSpeakExample 朗读例句回调 (日文, 中文, ID)
 * @param playingAudioId 当前正在朗读的 ID
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SRSGrammarCard(
    grammar: Grammar,
    isAnswerShown: Boolean,
    cardBadge: CardBadge? = null,
    modifier: Modifier = Modifier,
    onSpeakExample: ((String, String, String) -> Unit)? = null,
    playingAudioId: String? = null,
    isWhiteboardEnabled: Boolean = false,
    autoRevealProgress: Float? = null,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    // 背面临时显示手写白板的状态
    var showBackWhiteboard by remember(grammar.id) { mutableStateOf(false) }

    // 获取触觉反馈控制器
    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()

    // 检测深色模式
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // 颜色系统
    val cardBackground = if (isDarkTheme) NemoSurfaceCardDark else NemoSurfaceCard
    val borderColor = if (isDarkTheme) NemoSurfaceBorderDark else NemoSurfaceBorder
    val primaryTextColor = if (isDarkTheme) Color.White else NemoText
    val secondaryTextColor = if (isDarkTheme) NemoNeutralGray else NemoTextLight

    // 标准调色盘统一色彩系统
    val indigoColor = if (isDarkTheme) IosColors.IndigoDark else IosColors.Indigo
    val levelBg = indigoColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
    val levelText = indigoColor

    val whiteboardColor = if (isDarkTheme) IosColors.OrangeDark else IosColors.Orange
    val whiteboardBg = if (showBackWhiteboard) whiteboardColor else whiteboardColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
    val whiteboardTint = if (showBackWhiteboard) Color.White else whiteboardColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
            .padding(bottom = 100.dp), // 为底部评分按钮区域预留空间
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (topPadding > 0.dp) {
            Spacer(modifier = Modifier.height(topPadding))
        }

        // ========== 问题卡片 ==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedRectangle(24.dp))
                .background(cardBackground, RoundedRectangle(24.dp))
        ) {
            // 顶部自动翻面进度条
            if (autoRevealProgress != null) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { autoRevealProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(UnevenRoundedRectangle(topStart = 24.dp, topEnd = 24.dp))
                        .align(Alignment.TopCenter),
                    color = (if (isDarkTheme) IosColors.BlueDark else IosColors.Blue).copy(alpha = 0.6f),
                    trackColor = Color.Transparent,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    gapSize = 0.dp,
                    drawStopIndicator = {} // 去掉 Material 3 的默认停止指示点
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标签行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 等级标签
                    Box(
                        modifier = Modifier
                            .background(levelBg, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = grammar.grammarLevel,
                            color = levelText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // 顶部手写白板按钮 (仅在背面且 isAnswerShown 为 true 时显示，避免错位和重叠)
                    if (isAnswerShown) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(whiteboardBg)
                                .clickable { showBackWhiteboard = !showBackWhiteboard },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.learn_grammar_card_btn_whiteboard),
                                tint = whiteboardTint,
                                modifier = Modifier.size(20.dp) // 30 * 2/3 = 20dp 严格黄金比例
                            )
                        }
                    }
                }

                // 语法条目（大标题）
                Text(
                    text = grammar.grammar,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryTextColor,
                    fontFamily = NotoSerifJP,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    ),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // 思考提示（答案隐藏时）
                if (!isAnswerShown) {
                    Text(
                        text = stringResource(R.string.learn_grammar_card_hint_think),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            cardBadge?.let { badge ->
                val badgeColor = when (badge) {
                    CardBadge.NEW -> if (isDarkTheme) IosColors.BlueDark else IosColors.Blue
                    CardBadge.LEARNING -> if (isDarkTheme) IosColors.CyanDark else IosColors.Cyan
                    CardBadge.REVIEW -> if (isDarkTheme) IosColors.GreenDark else IosColors.Green
                    CardBadge.RELEARN -> if (isDarkTheme) IosColors.OrangeDark else IosColors.Orange
                }
                val badgeText = when (badge) {
                    CardBadge.NEW -> stringResource(R.string.learn_grammar_card_badge_new)
                    CardBadge.LEARNING -> stringResource(R.string.learn_grammar_card_badge_learning)
                    CardBadge.REVIEW -> stringResource(R.string.learn_grammar_card_badge_review)
                    CardBadge.RELEARN -> stringResource(R.string.learn_grammar_card_badge_relearn)
                }
                val badgeBg = badgeColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 32.dp, end = 32.dp)
                        .background(badgeBg, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }


        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Sticker Area or Whiteboard (答案显示前) ---
        if (!isAnswerShown) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(300)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isWhiteboardEnabled,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)))
                            .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)))
                    },
                    label = "WhiteboardToggle"
                ) { enabled ->
                    if (enabled) {
                        com.jian.nemo.feature.learning.presentation.components.common.NemoWhiteboard(
                            wordId = grammar.id,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(440.dp)
                        )
                    } else {
                        val dotColor = if (isDarkTheme) Color.White else Color.Black
                        val baseDotAlpha = if (isDarkTheme) 0.12f else 0.08f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(440.dp)
                                .softCardShadow(borderRadius = 24.dp, isDark = isDarkTheme)
                                .clip(RoundedRectangle(24.dp))
                                .background(cardBackground)
                                .drawBehind {
                                    val dotSpacing = 18.dp.toPx()
                                    val dotRadius = 1.25.dp.toPx()

                                    val cols = (size.width / dotSpacing).toInt()
                                    val rows = (size.height / dotSpacing).toInt()
                                    val startX = (size.width - (cols - 1) * dotSpacing) / 2f
                                    val startY = (size.height - (rows - 1) * dotSpacing) / 2f

                                    // 图标位于几何正中心，避让中心正对称
                                    val clearCenterX = size.width / 2f
                                    val clearCenterY = size.height / 2f
                                    val clearRadius = 80.dp.toPx()

                                    for (i in 0 until cols) {
                                        for (j in 0 until rows) {
                                            val x = startX + i * dotSpacing
                                            val y = startY + j * dotSpacing

                                            val dx = x - clearCenterX
                                            val dy = y - clearCenterY
                                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                            val distRatio = dist / clearRadius

                                            // 中心正圆避让（呼吸圈）加柔和渐变过渡
                                            if (distRatio >= 0.85f) {
                                                val pointAlpha = if (distRatio < 1.35f) {
                                                    ((distRatio - 0.85f) / 0.5f).coerceIn(0f, 1f) * baseDotAlpha
                                                } else {
                                                    baseDotAlpha
                                                }
                                                drawCircle(
                                                    color = dotColor.copy(alpha = pointAlpha),
                                                    radius = dotRadius,
                                                    center = Offset(x, y)
                                                )
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 42.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else {
            // 当显示答案时，若用户临时调出了白板，在这里渲染它
            AnimatedVisibility(
                visible = showBackWhiteboard
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.jian.nemo.feature.learning.presentation.components.common.NemoWhiteboard(
                        wordId = grammar.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp),
                        onClose = { showBackWhiteboard = false }
                    )
                }
            }
        }

        // ========== 答案区域 ==========
        AnimatedVisibility(
            visible = isAnswerShown,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 }
        ) {
            // 状态管理：记录每个用法的展开状态
            // 默认第一个用法 (index 0) 展开，其余折叠
            // 注意：此处使用 remember 而非 rememberSaveable，因为 SnapshotStateMap 默认不支持序列化，
            // 且对于折叠状态，在内存中保持即可，无需处理进程死亡后的恢复。
            val expandedStates = remember(grammar.id) {
                mutableStateMapOf<Int, Boolean>().apply {
                    grammar.usages.indices.forEach { index ->
                        this[index] = index == 0
                    }
                }
            }

            val bringIntoViewRequesters = remember(grammar.id) {
                List(grammar.usages.size) { BringIntoViewRequester() }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 遍历每个用法，分别显示
                grammar.usages.forEachIndexed { usageIndex, usage ->
                    val isExpanded = expandedStates[usageIndex] ?: (usageIndex == 0)

                    val headerBgColor by animateColorAsState(
                        targetValue = if (isExpanded) Color.Transparent else cardBackground,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "headerBgColor"
                    )

                    val rotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "arrowRotation"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequesters[usageIndex]),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 用法标题（如果是多用法卡片，显示可点击的头部）
                        if (grammar.usages.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = headerBgColor,
                                        shape = RoundedRectangle(12.dp)
                                    )
                                    .clip(RoundedRectangle(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val nextState = !isExpanded
                                        expandedStates[usageIndex] = nextState
                                        if (nextState) {
                                            scope.launch {
                                                kotlinx.coroutines.delay(200)
                                                bringIntoViewRequesters[usageIndex].bringIntoView()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            val effectiveUsageColor = if (isDarkTheme) IosColors.IndigoDark else IosColors.Indigo
                            val effectiveUsageBg = if (isExpanded) {
                                effectiveUsageColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
                            } else {
                                effectiveUsageColor.copy(alpha = if (isDarkTheme) 0.08f else 0.05f)
                            }
                            val effectiveUsageText = if (isExpanded) effectiveUsageColor else effectiveUsageColor.copy(alpha = 0.7f)

                            Box(
                                modifier = Modifier
                                    .background(
                                        effectiveUsageBg,
                                        RoundedRectangle(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                val chineseNumbers = stringArrayResource(R.array.learning_grammar_card_chinese_numbers)
                                val label = chineseNumbers.getOrNull(usageIndex) ?: (usageIndex + 1).toString()
                                Text(
                                    text = stringResource(R.string.learn_grammar_card_label_usage_prefix, label),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = effectiveUsageText,
                                    letterSpacing = 1.sp
                                )
                            }

                            usage.subtype?.let { subtype ->
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = subtype,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpanded) primaryTextColor else secondaryTextColor
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // 展开状态图标
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown, // 始终使用向下箭头
                                contentDescription = if (isExpanded) stringResource(R.string.learn_grammar_card_action_collapse) else stringResource(R.string.learn_grammar_card_action_expand),
                                tint = secondaryTextColor.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotation) // 绑定旋转角度
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 用法详细内容（受折叠状态控制）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isExpanded || grammar.usages.size == 1,
                        enter = fadeIn(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = 0.9f, 
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Top
                        ),
                        exit = fadeOut(
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy, 
                                stiffness = Spring.StiffnessMedium
                            ),
                            shrinkTowards = Alignment.Top
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 接续卡片
                            if (usage.connection.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBackground, RoundedRectangle(24.dp))
                                        .padding(24.dp)
                                ) {
                                    // 标题
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Link,
                                            contentDescription = null,
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.learn_grammar_card_label_connection),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryTextColor,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    // 接续内容
                                    Text(
                                        text = usage.connection,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryTextColor,
                                        lineHeight = 28.sp
                                    )
                                }
                            }

                            // 说明卡片
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardBackground, RoundedRectangle(24.dp))
                                    .padding(24.dp)
                            ) {
                                // 标题
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    val explanationColor = if (isDarkTheme) IosColors.IndigoDark else IosColors.Indigo
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = null,
                                        tint = explanationColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.learn_grammar_card_label_explanation),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = explanationColor,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // 说明内容
                                Text(
                                    text = usage.explanation,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = primaryTextColor,
                                    lineHeight = 28.sp
                                )
                            }

                            // 例句卡片（紧凑设计）
                            if (usage.examples.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBackground, RoundedRectangle(24.dp))
                                        .padding(24.dp)
                                ) {
                                    // 标题
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MenuBook,
                                            contentDescription = null,
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.learn_grammar_card_label_example),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryTextColor,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    // 例句列表（统一背景，用分隔线区分）
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isDarkTheme) LearningPalette.ExampleBoxDark else Color.White,
                                                RoundedRectangle(16.dp)
                                            )
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        usage.examples.forEachIndexed { index, example ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        // 日文例句（带注音）
                                                        FuriganaText(
                                                            text = example.sentence,
                                                            baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                                                fontSize = 16.sp,
                                                                lineHeight = 26.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                fontFamily = NotoSerifJP
                                                            ),
                                                            baseTextColor = primaryTextColor,
                                                            furiganaTextSize = 10.sp,
                                                            furiganaTextColor = secondaryTextColor,
                                                            furiganaFontFamily = NotoSerifJP
                                                        )

                                                        // 中文翻译
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = example.translation,
                                                            fontSize = 14.sp,
                                                            color = secondaryTextColor.copy(alpha = 0.8f),
                                                            lineHeight = 22.sp
                                                        )
                                                    }

                                                    // 例句朗读按钮
                                                    if (onSpeakExample != null && example.sentence.isNotBlank()) {
                                                        val id = "grammar_${grammar.id}_u${usageIndex}_e${index}"
                                                        SpeakerButton(
                                                            isPlaying = playingAudioId == id,
                                                            onClick = { onSpeakExample(example.sentence, example.translation, id) },
                                                            tint = secondaryTextColor,
                                                            size = 36.dp
                                                        )
                                                    }
                                                }
                                            }

                                            // 分隔线（最后一个例句不显示）
                                            if (index < usage.examples.size - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(borderColor.copy(alpha = 0.5f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // TIPS卡片（如果存在）
                            if (!usage.notes.isNullOrBlank()) {
                                val tipsColor = if (isDarkTheme) IosColors.OrangeDark else IosColors.Orange
                                val tipsBg = tipsColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(tipsBg, RoundedRectangle(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = tipsColor,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.learn_grammar_card_tips_prefix, usage.notes ?: ""),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tipsColor,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
