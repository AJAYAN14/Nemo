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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
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
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.designsystem.theme.LearningPalette
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
    autoRevealProgress: Float? = null
) {
    // 背面临时显示手写白板的状态
    var showBackWhiteboard by remember(grammar.id) { mutableStateOf(false) }

    // 获取触觉反馈控制器
    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()

    // 检测图标风格
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    // 检测深色模式
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // 颜色系统
    val cardBackground = if (isDarkTheme) NemoSurfaceCardDark else NemoSurfaceCard
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else NemoNeutrals.Gray200
    val primaryTextColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) NemoNeutrals.Gray400 else NemoNeutrals.Gray500

    // Indigo 标签色
    val indigoBg = if (isDarkTheme) LearningPalette.IndigoBgDark else LearningPalette.IndigoBgLight
    val indigoBorder = if (isDarkTheme) LearningPalette.IndigoBorderDark else LearningPalette.IndigoBorderLight
    val indigoText = if (isDarkTheme) LearningPalette.IndigoTextDark else LearningPalette.IndigoTextLight

    // Yellow 提示色
    val yellowBg = if (isDarkTheme) LearningPalette.YellowTipsBgDark else LearningPalette.YellowTipsBgLight
    val yellowBorder = if (isDarkTheme) LearningPalette.YellowTipsBorderDark else LearningPalette.YellowTipsBorderLight
    val yellowIcon = if (isDarkTheme) LearningPalette.YellowTipsIconDark else LearningPalette.YellowTipsIconLight
    val yellowText = if (isDarkTheme) LearningPalette.YellowTipsTextDark else LearningPalette.YellowTipsTextLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
            .padding(bottom = 100.dp), // 为底部评分按钮区域预留空间
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== 问题卡片 ==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardBackground, RoundedCornerShape(24.dp))
        ) {
            // 顶部自动翻面进度条
            if (autoRevealProgress != null) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { autoRevealProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .align(Alignment.TopCenter),
                    color = if (isPlumpDuo) IsoluminantPalette.SkyIndigo.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
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
                    val effectiveLevelBg = if (isPlumpDuo) {
                        IsoluminantPalette.LilacViolet.copy(alpha = if (isDarkTheme) 0.12f else 0.08f)
                    } else {
                        indigoBg
                    }
                    val effectiveLevelText = if (isPlumpDuo) {
                        IsoluminantPalette.LilacViolet
                    } else {
                        indigoText
                    }

                    Box(
                        modifier = Modifier
                            .background(effectiveLevelBg, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = grammar.grammarLevel,
                            color = effectiveLevelText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // 顶部手写白板按钮 (仅在背面且 isAnswerShown 为 true 时显示，避免错位和重叠)
                    if (isAnswerShown) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isPlumpDuo) {
                            val whiteboardColor = IsoluminantPalette.WarmAmber
                            val buttonBgColor = if (showBackWhiteboard) whiteboardColor else whiteboardColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f)
                            val buttonTextColor = if (showBackWhiteboard) Color.White else whiteboardColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(buttonBgColor)
                                    .clickable { showBackWhiteboard = !showBackWhiteboard },
                                contentAlignment = Alignment.Center
                            ) {
                                PlumpDuoIcon(
                                    icon = PlumpIcon.UserPen,
                                    color = buttonTextColor,
                                    size = 18.dp
                                )
                            }
                        } else {
                            val buttonBgColor = if (showBackWhiteboard) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                            val buttonTextColor = if (showBackWhiteboard) Color.White else (if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(buttonBgColor)
                                    .clickable { showBackWhiteboard = !showBackWhiteboard },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.btn_temp_whiteboard),
                                    tint = buttonTextColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
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
                        text = stringResource(R.string.hint_think_grammar_usage),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            cardBadge?.let { badge ->
                val (text, bgColor, textColor) = when (badge) {
                    CardBadge.NEW -> Triple(stringResource(R.string.card_badge_new), if (isDarkTheme) LearningPalette.BadgeNewBgDark else LearningPalette.BadgeNewBgLight, if (isDarkTheme) LearningPalette.BadgeNewTextDark else LearningPalette.BadgeNewTextLight)
                    CardBadge.LEARNING -> Triple(stringResource(R.string.card_badge_learning), if (isDarkTheme) LearningPalette.BadgeLearningBgDark else LearningPalette.BadgeLearningBgLight, if (isDarkTheme) LearningPalette.BadgeLearningTextDark else LearningPalette.BadgeLearningTextLight)
                    CardBadge.REVIEW -> Triple(stringResource(R.string.card_badge_review), if (isDarkTheme) LearningPalette.BadgeReviewBgDark else LearningPalette.BadgeReviewBgLight, if (isDarkTheme) LearningPalette.BadgeReviewTextDark else LearningPalette.BadgeReviewTextLight)
                    CardBadge.RELEARN -> Triple(stringResource(R.string.card_badge_relearn), if (isDarkTheme) LearningPalette.BadgeRelearnBgDark else LearningPalette.BadgeRelearnBgLight, if (isDarkTheme) LearningPalette.BadgeRelearnTextDark else LearningPalette.BadgeRelearnTextLight)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 32.dp, end = 32.dp)
                        .background(bgColor, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = text,
                        color = textColor,
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
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBackground.copy(alpha = if (isDarkTheme) 0.85f else 0.95f))
                                .border(
                                    width = 0.8.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(24.dp)
                                )
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
                            if (isPlumpDuo) {
                                PlumpDuoIcon(
                                    icon = PlumpIcon.Lightbulb,
                                    color = MaterialTheme.colorScheme.primary,
                                    size = 96.dp
                                )
                            } else {
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
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
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
                            val effectiveUsageBg = if (isPlumpDuo) {
                                IsoluminantPalette.LilacViolet.copy(alpha = if (isExpanded) (if (isDarkTheme) 0.16f else 0.10f) else (if (isDarkTheme) 0.08f else 0.05f))
                            } else {
                                if (isExpanded) indigoBg else indigoBg.copy(alpha = 0.5f)
                            }
                            val effectiveUsageText = if (isPlumpDuo) {
                                if (isExpanded) IsoluminantPalette.LilacViolet else IsoluminantPalette.LilacViolet.copy(alpha = 0.7f)
                            } else {
                                if (isExpanded) indigoText else indigoText.copy(alpha = 0.7f)
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        effectiveUsageBg,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                val chineseNumbers = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
                                val label = chineseNumbers.getOrNull(usageIndex) ?: (usageIndex + 1).toString()
                                Text(
                                    text = stringResource(R.string.grammar_label_usage_prefix, label),
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
                                contentDescription = if (isExpanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
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
                                        .background(cardBackground, RoundedCornerShape(24.dp))
                                        .padding(24.dp)
                                ) {
                                    // 标题
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.grammar_label_connection),
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
                                    .background(cardBackground, RoundedCornerShape(24.dp))
                                    .padding(24.dp)
                            ) {
                                // 标题
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    val effectiveExplanationColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else indigoText
                                    if (isPlumpDuo) {
                                        PlumpDuoIcon(
                                            icon = PlumpIcon.Lightbulb,
                                            color = effectiveExplanationColor,
                                            size = 20.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = effectiveExplanationColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.grammar_label_explanation),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = effectiveExplanationColor,
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
                                        .background(cardBackground, RoundedCornerShape(24.dp))
                                        .padding(24.dp)
                                ) {
                                    // 标题
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.label_word_example),
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
                                                RoundedCornerShape(16.dp)
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
                                val effectiveTipsBg = if (isPlumpDuo) {
                                    IsoluminantPalette.WarmAmber.copy(alpha = if (isDarkTheme) 0.12f else 0.08f)
                                } else {
                                    yellowBg
                                }
                                val effectiveTipsIcon = if (isPlumpDuo) IsoluminantPalette.WarmAmber else yellowIcon
                                val effectiveTipsText = if (isPlumpDuo) IsoluminantPalette.WarmAmber else yellowText

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(effectiveTipsBg, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (isPlumpDuo) {
                                        PlumpDuoIcon(
                                            icon = PlumpIcon.Sparkles,
                                            color = effectiveTipsIcon,
                                            size = 18.dp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = effectiveTipsIcon,
                                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.grammar_tips_prefix, usage.notes ?: ""),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = effectiveTipsText,
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
