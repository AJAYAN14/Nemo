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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.ui.component.text.FuriganaText
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.presentation.CardBadge
import kotlin.math.abs

/**
 * HIG 风格配色列表 - 用于生成随机但稳定的颜色
 */
private val higColors = listOf(
    IosColors.Blue,
    IosColors.Green,
    IosColors.Orange,
    IosColors.Pink,
    IosColors.Indigo,
    IosColors.Purple,
    IosColors.Mint,
    IosColors.Red,
    IosColors.Teal,
    IosColors.Yellow
)



/**
 * 根据单词 ID 获取稳定的随机颜色
 */
private fun getColorForWord(wordId: Int): Color {
    val index = abs(wordId) % higColors.size
    return higColors[index]
}

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
    autoRevealProgress: Float? = null
) {
    // 检测图标风格
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    // 背面临时显示手写白板的状态
    var showBackWhiteboard by remember(word.id) { mutableStateOf(false) }

    // 检测深色模式
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // 根据主题选择颜色
    val cardBackground = if (isDarkTheme) NemoSurfaceCardDark else NemoSurfaceCard

    // Border/Divider
    // Border/Divider - Refined to 0.5dp for premium feel
    val borderColor = if (isDarkTheme) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else NemoNeutrals.Gray100

    // Shadow Parameters
    val shadowElevation = if (isDarkTheme) 2.dp else 10.dp
    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.03f)

    // Text: 使用 NemoNeutrals 定义的精准色值
    val primaryTextColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) NemoNeutrals.Gray400 else NemoNeutrals.Gray400

    // Labels
    val labelBgColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else NemoNeutrals.Gray100
    val labelTextColor = if (isDarkTheme) NemoNeutrals.DarkTextSecondary else NemoNeutrals.Gray500

    // Indigo 标签色 (用于等级徽章)
    val indigoBg = if (isDarkTheme) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
    val indigoBorder = if (isDarkTheme) Color(0xFF3730A3) else Color(0xFFE0E7FF)
    val indigoText = if (isDarkTheme) Color(0xFFA5B4FC) else Color(0xFF4F46E5)

    // 跟打按钮颜色 - 根据单词 ID 生成稳定的随机颜色
    val practiceButtonColor = remember(word.id) { getColorForWord(word.id) }
    val practiceButtonBgColor = if (isDarkTheme) {
        practiceButtonColor.copy(alpha = 0.2f)
    } else {
        practiceButtonColor.copy(alpha = 0.1f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // --- Question Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 26.dp, isDark = isDarkTheme)
                .clip(RoundedCornerShape(26.dp))
                .background(cardBackground, RoundedCornerShape(26.dp))
        ) {
            if (autoRevealProgress != null && autoRevealProgress > 0f) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { autoRevealProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .align(Alignment.TopCenter),
                    color = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else MaterialTheme.colorScheme.primary,
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
                    .padding(top = 20.dp), // 预留空间避免与顶部徽章重叠
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
                    val pronunciationColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else NemoNeutrals.Blue600
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
                        text = "思考这个单词的发音和含义...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 左上角等级徽章 (镜像对称)
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
                    .align(Alignment.TopStart)
                    .padding(top = 24.dp, start = 16.dp)
                    .background(effectiveLevelBg, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = word.level,
                    color = effectiveLevelText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }

            cardBadge?.let { badge ->
                val (text, bgColor, textColor) = when (badge) {
                    CardBadge.NEW -> if (isPlumpDuo) {
                        Triple("新学", IsoluminantPalette.SkyIndigo.copy(alpha = if (isDarkTheme) 0.12f else 0.08f), IsoluminantPalette.SkyIndigo)
                    } else {
                        Triple("新学", if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFE0EDFF), if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1D4ED8))
                    }
                    CardBadge.LEARNING -> if (isPlumpDuo) {
                        Triple("在学", IsoluminantPalette.LakesideCyan.copy(alpha = if (isDarkTheme) 0.12f else 0.08f), IsoluminantPalette.LakesideCyan)
                    } else {
                        Triple("在学", if (isDarkTheme) Color(0xFF083344) else Color(0xFFE0F2FE), if (isDarkTheme) Color(0xFF22D3EE) else Color(0xFF0891B2))
                    }
                    CardBadge.REVIEW -> if (isPlumpDuo) {
                        Triple("复习", IsoluminantPalette.EmeraldGreen.copy(alpha = if (isDarkTheme) 0.12f else 0.08f), IsoluminantPalette.EmeraldGreen)
                    } else {
                        Triple("复习", if (isDarkTheme) Color(0xFF14532D) else Color(0xFFDCFCE7), if (isDarkTheme) Color(0xFFBBF7D0) else Color(0xFF166534))
                    }
                    CardBadge.RELEARN -> if (isPlumpDuo) {
                        Triple("重学", IsoluminantPalette.WarmAmber.copy(alpha = if (isDarkTheme) 0.12f else 0.08f), IsoluminantPalette.WarmAmber)
                    } else {
                        Triple("重学", if (isDarkTheme) Color(0xFF7C2D12) else Color(0xFFFFEDD5), if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 16.dp)
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
                                .clip(RoundedCornerShape(26.dp))
                                .background(cardBackground.copy(alpha = if (isDarkTheme) 0.85f else 0.95f))
                                .border(
                                    width = 0.8.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(26.dp)
                                )
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
                        .background(cardBackground.copy(alpha = 0.95f), RoundedCornerShape(26.dp))
                        .padding(24.dp)
                ) {
                    // Meaning
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        // POS Tag
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .background(labelBgColor, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = word.pos ?: "未知",
                                color = labelTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "含义",
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
                            text = "例句",
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
                                text = "暂无例句",
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 按钮区域（右上角）：朗读 + 跟打练习 + 临时手写板
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPlumpDuo) {
                        // Plump Duo 方案：切换为 IsoluminantPalette 对应的等亮度颜色，图标换为 PlumpDuoIcon
                        val plumpAudioColor = IsoluminantPalette.SkyIndigo
                        val plumpPracticeColor = IsoluminantPalette.LakesideCyan
                        val plumpWhiteboardColor = IsoluminantPalette.WarmAmber

                        // 1. 朗读单词按钮
                        if (onSpeakWord != null) {
                            if (playingAudioId == "word") {
                                SpeakerButton(
                                    isPlaying = true,
                                    onClick = { onSpeakWord() },
                                    tint = plumpAudioColor,
                                    size = 44.dp,
                                    backgroundColor = plumpAudioColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f)
                                )
                            } else {
                                LiquidButton(
                                    onClick = { onSpeakWord() },
                                    backgroundColor = plumpAudioColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f),
                                    shape = CircleShape,
                                    elevation = 0.dp,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    PlumpDuoIcon(
                                        icon = PlumpIcon.AudioVolume,
                                        color = plumpAudioColor,
                                        size = 24.dp
                                    )
                                }
                            }
                        }

                        // 2. 跟打练习按钮
                        if (onPracticeClick != null) {
                            LiquidButton(
                                onClick = { onPracticeClick() },
                                backgroundColor = plumpPracticeColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f),
                                shape = CircleShape,
                                elevation = 0.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                PlumpDuoIcon(
                                    icon = PlumpIcon.Languages,
                                    color = plumpPracticeColor,
                                    size = 24.dp
                                )
                            }
                        }

                        // 3. 手写白板按钮
                        val whiteboardBg = if (showBackWhiteboard) {
                            plumpWhiteboardColor
                        } else {
                            plumpWhiteboardColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f)
                        }
                        val whiteboardIconColor = if (showBackWhiteboard) Color.White else plumpWhiteboardColor

                        LiquidButton(
                            onClick = { showBackWhiteboard = !showBackWhiteboard },
                            backgroundColor = whiteboardBg,
                            shape = CircleShape,
                            elevation = 0.dp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            PlumpDuoIcon(
                                icon = PlumpIcon.UserPen,
                                color = whiteboardIconColor,
                                size = 24.dp
                            )
                        }
                    } else {
                        // 默认方案：100% 保持原有配色和图标不变
                        // 朗读单词按钮
                        if (onSpeakWord != null) {
                            SpeakerButton(
                                isPlaying = playingAudioId == "word",
                                onClick = { onSpeakWord() },
                                tint = practiceButtonColor,
                                size = 44.dp,
                                backgroundColor = practiceButtonBgColor
                            )
                        }

                        // 跟打练习按钮
                        if (onPracticeClick != null) {
                            LiquidButton(
                                onClick = { onPracticeClick() },
                                backgroundColor = practiceButtonBgColor,
                                shape = CircleShape,
                                elevation = 0.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Translate,
                                    contentDescription = "跟打练习",
                                    tint = practiceButtonColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 手写白板按钮 (背面固定显示，不用去 menu 再次打开了)
                        LiquidButton(
                            onClick = { showBackWhiteboard = !showBackWhiteboard },
                            backgroundColor = if (showBackWhiteboard) practiceButtonColor else practiceButtonBgColor,
                            shape = CircleShape,
                            elevation = 0.dp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "临时手写板",
                                tint = if (showBackWhiteboard) Color.White else practiceButtonColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
