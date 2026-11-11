package com.jian.nemo.feature.learning.presentation.components.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.jian.nemo.feature.learning.presentation.components.dialogs.QueueExplanationDialog
import com.jian.nemo.feature.learning.presentation.components.common.TrackProgressIndicator
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.shape.UnevenRoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.learning.R
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoText
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import com.jian.nemo.core.designsystem.theme.NemoNeutralGray
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBorder
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBorderDark
import com.jian.nemo.core.designsystem.theme.NemoSurfaceSubcard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceSubcardDark
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.ui.component.text.FuriganaText
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.presentation.CardBadge
/**
 * @param word 单词数据
 * @param isAnswerShown 是否显示答案
 * @param onPracticeClick 点击跟打练习按钮的回调
 * @param onSpeakWord 朗读单词回调
 * @param onSpeakExample 朗读例句回调 (日文, 中文, ID)
 * @param playingAudioId 当前正在朗读的 ID
 */
@Composable
fun SRSLearningCard(
    word: Word,
    isAnswerShown: Boolean,
    cardBadge: CardBadge? = null,
    modifier: Modifier = Modifier,
    onPracticeClick: (() -> Unit)? = null,
    onSpeakWord: (() -> Unit)? = null,
    onSpeakExample: ((String, String, String) -> Unit)? = null,
    playingAudioId: String? = null,
    isWhiteboardEnabled: Boolean = false,
    autoRevealProgress: Float? = null,
    progress: Float = 0f,
    queueNewCount: Int = 0,
    queueLearningCount: Int = 0,
    queueReviewCount: Int = 0,
    queueRelearnCount: Int = 0,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val haptic = LocalHapticFeedback.current
    var showQueueExplanationDialog by remember { mutableStateOf(false) }
    // 背面临时显示手写白板的状态
    var showBackWhiteboard by remember(word.id) { mutableStateOf(false) }

    // 检测深色模式
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // 根据主题选择颜色
    val cardBackground = if (isDarkTheme) NemoSurfaceCardDark else NemoSurfaceCard

    // Divider
    val dividerColor = if (isDarkTheme) NemoSurfaceBorderDark else NemoSurfaceBorder

    // Text: 严格对齐 Dynamic_Color_System_Spec 四级文本明度阶梯
    val primaryTextColor = if (isDarkTheme) Color.White else NemoText
    val secondaryTextColor = if (isDarkTheme) NemoNeutralGray else NemoTextLight

    // Labels: 二级表面与二级文本
    val labelBgColor = if (isDarkTheme) NemoSurfaceSubcardDark else NemoSurfaceSubcard
    val labelTextColor = if (isDarkTheme) NemoNeutralGray else NemoTextLight

    // 标准调色盘统一色彩系统
    val indigoColor = if (isDarkTheme) SystemColors.IndigoDark else SystemColors.Indigo
    val levelBg = indigoColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
    val levelText = indigoColor

    val audioColor = if (isDarkTheme) SystemColors.BlueDark else SystemColors.Blue
    val audioBg = audioColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)

    val practiceColor = if (isDarkTheme) SystemColors.CyanDark else SystemColors.Cyan
    val practiceBg = practiceColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)

    val whiteboardColor = if (isDarkTheme) SystemColors.OrangeDark else SystemColors.Orange
    val whiteboardBg = if (showBackWhiteboard) whiteboardColor else whiteboardColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
    val whiteboardTint = if (showBackWhiteboard) Color.White else whiteboardColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        if (topPadding > 0.dp) {
            Spacer(modifier = Modifier.height(topPadding))
        }

        // --- Question Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 26.dp, isDark = isDarkTheme)
                .clip(RoundedRectangle(26.dp))
                .background(cardBackground, RoundedRectangle(26.dp))
        ) {
            if (autoRevealProgress != null && autoRevealProgress > 0f) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { autoRevealProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(UnevenRoundedRectangle(topStart = 26.dp, topEnd = 26.dp))
                        .align(Alignment.TopCenter),
                    color = if (isDarkTheme) SystemColors.BlueDark else SystemColors.Blue,
                    trackColor = Color.Transparent,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp)
                    .padding(top = 26.dp), // 预留空间避免与顶部徽章重叠
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Japanese
                Text(
                    text = word.japanese,
                    color = primaryTextColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold, // Refined from Black to ExtraBold
                    fontFamily = NotoSerifJP,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    lineHeight = 56.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    )
                )

                // 假名读音 (答案显示时) / 思考提示 (答案隐藏时)
                if (isAnswerShown) {
                    val pronunciationColor = if (isDarkTheme) SystemColors.BlueDark else SystemColors.Blue
                    Text(
                        text = word.hiragana,
                        color = pronunciationColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = NotoSerifJP,
                        style = TextStyle(
                            localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                        )
                    )
                } else {
                    Text(
                        text = stringResource(R.string.hint_think_word_pronunciation),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 顶部栏：等级徽章 (左) + 跑道进度条 (中) + 状态徽章 (右)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 等级徽章 (高度 34dp)
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .background(levelBg, CircleShape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word.level,
                        color = levelText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }

                // 400米跑道进度条 (高度 34dp，宽度 130dp，沿赛道转圈推进)
                TrackProgressIndicator(
                    progress = progress,
                    queueNewCount = queueNewCount,
                    queueLearningCount = queueLearningCount,
                    queueReviewCount = queueReviewCount,
                    queueRelearnCount = queueRelearnCount,
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showQueueExplanationDialog = true
                    }
                )

                // 状态徽章 (高度 34dp)
                if (cardBadge != null) {
                    val badgeColor = when (cardBadge) {
                        CardBadge.NEW -> if (isDarkTheme) SystemColors.BlueDark else SystemColors.Blue
                        CardBadge.LEARNING -> if (isDarkTheme) SystemColors.CyanDark else SystemColors.Cyan
                        CardBadge.REVIEW -> if (isDarkTheme) SystemColors.GreenDark else SystemColors.Green
                        CardBadge.RELEARN -> if (isDarkTheme) SystemColors.OrangeDark else SystemColors.Orange
                    }
                    val badgeText = when (cardBadge) {
                        CardBadge.NEW -> stringResource(R.string.card_badge_new)
                        CardBadge.LEARNING -> stringResource(R.string.card_badge_learning)
                        CardBadge.REVIEW -> stringResource(R.string.card_badge_review)
                        CardBadge.RELEARN -> stringResource(R.string.card_badge_relearn)
                    }
                    val badgeBg = badgeColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)

                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .background(badgeBg, CircleShape)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(56.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                            wordId = word.id,
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
                                .softCardShadow(borderRadius = 26.dp, isDark = isDarkTheme)
                                .clip(RoundedRectangle(26.dp))
                                .background(cardBackground)
                                .drawBehind {
                                    val dotSpacing = 18.dp.toPx()
                                    val dotRadius = 1.25.dp.toPx()

                                    val cols = (size.width / dotSpacing).toInt()
                                    val rows = (size.height / dotSpacing).toInt()
                                    val startX = (size.width - (cols - 1) * dotSpacing) / 2f
                                    val startY = (size.height - (rows - 1) * dotSpacing) / 2f

                                    // 图标在垂直偏上位置，避让中心设为略偏上的位置
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

                                            // 方案 A：中心正圆避让（呼吸圈）加柔和渐变过渡
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
                        wordId = word.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp),
                        onClose = { showBackWhiteboard = false }
                    )
                }
            }
        }

        // --- Answer Area ---
        AnimatedVisibility(
            visible = isAnswerShown,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .softCardShadow(borderRadius = 26.dp, isDark = isDarkTheme)
                        .background(cardBackground, RoundedRectangle(26.dp))
                        .padding(24.dp)
                ) {
                    // Meaning
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        // POS Tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(y = (-10).dp)
                                    .height(34.dp)
                                    .background(levelBg, CircleShape)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = word.pos ?: stringResource(R.string.pos_unknown),
                                    color = levelText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    )
                                )
                            }

                            // 按钮区域：朗读 + 跟打练习 + 临时手写板
                            Row(
                                modifier = Modifier.offset(y = (-10).dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. 朗读单词按钮
                                if (onSpeakWord != null) {
                                    SpeakerButton(
                                        isPlaying = playingAudioId == "word",
                                        onClick = { onSpeakWord() },
                                        tint = audioColor,
                                        size = 48.dp,
                                        iconSize = 30.dp,
                                        backgroundColor = audioBg
                                    )
                                }

                                // 2. 跟打练习按钮
                                if (onPracticeClick != null) {
                                    LiquidButton(
                                        onClick = { onPracticeClick() },
                                        backgroundColor = practiceBg,
                                        shape = CircleShape,
                                        elevation = 0.dp,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Translate,
                                            contentDescription = stringResource(R.string.btn_practice_typing),
                                            tint = practiceColor,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                // 3. 手写白板按钮
                                LiquidButton(
                                    onClick = { showBackWhiteboard = !showBackWhiteboard },
                                    backgroundColor = whiteboardBg,
                                    shape = CircleShape,
                                    elevation = 0.dp,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.btn_temp_whiteboard),
                                        tint = whiteboardTint,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.label_word_meaning),
                            color = secondaryTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium, // Refined from Bold to Medium
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = word.chinese,
                            color = primaryTextColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 34.sp
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(dividerColor)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Example
                    Column {
                        Text(
                            text = stringResource(R.string.label_word_example),
                            color = secondaryTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium, // Refined from Bold to Medium
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val examples = listOf(
                            Triple(word.example1, word.gloss1, "example_${word.id}_1"),
                            Triple(word.example2, word.gloss2, "example_${word.id}_2"),
                            Triple(word.example3, word.gloss3, "example_${word.id}_3")
                        ).filter { !it.first.isNullOrBlank() }

                        if (examples.isNotEmpty()) {
                            examples.forEachIndexed { index, (example, gloss, id) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        FuriganaText(
                                            text = example!!,
                                            baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 24.sp,
                                                fontFamily = NotoSerifJP
                                            ),
                                            baseTextColor = primaryTextColor,
                                            furiganaTextSize = 9.sp,
                                            furiganaTextColor = secondaryTextColor,
                                            furiganaFontFamily = NotoSerifJP,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        if (!gloss.isNullOrBlank()) {
                                            Text(
                                                text = gloss,
                                                color = secondaryTextColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = if (index < examples.lastIndex) 12.dp else 0.dp)
                                            )
                                        }
                                    }

                                    // 例句朗读按钮
                                    if (onSpeakExample != null && !example.isNullOrBlank()) {
                                        SpeakerButton(
                                            isPlaying = playingAudioId == id,
                                            onClick = { onSpeakExample(example, gloss ?: "", id) },
                                            tint = secondaryTextColor,
                                            size = 36.dp
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.label_no_example),
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

            }
        }
    }

    if (showQueueExplanationDialog) {
        QueueExplanationDialog(
            onDismissRequest = { showQueueExplanationDialog = false },
            queueNewCount = queueNewCount,
            queueLearningCount = queueLearningCount,
            queueReviewCount = queueReviewCount,
            queueRelearnCount = queueRelearnCount,
            isDarkTheme = isDarkTheme
        )
    }
}
