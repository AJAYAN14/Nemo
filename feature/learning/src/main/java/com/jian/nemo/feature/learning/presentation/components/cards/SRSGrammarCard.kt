package com.jian.nemo.feature.learning.presentation.components.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.text.FuriganaText
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import kotlin.math.abs
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.presentation.CardBadge

/**
 * 获取随机贴纸文件名 (Grammar Version)
 */
private fun getStickerForGrammar(grammarId: Int): String {
    val stickers = listOf(
        "bad_taste", "birthday", "cleaning", "confused", "cooking",
        "cool", "eating_noodles", "headache", "listening_music", "love",
        "pretend_sleep", "really", "receiving_gift", "scared", "selfie",
        "shocked", "singing", "something", "studying", "superman",
        "sure", "taking_photo", "waving", "yoga", "zoning_out"
    )
    val index = abs(grammarId) % stickers.size
    return stickers[index]
}

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
@Composable
fun SRSGrammarCard(
    grammar: Grammar,
    isAnswerShown: Boolean,
    cardBadge: CardBadge? = null,
    modifier: Modifier = Modifier,
    onSpeakExample: ((String, String, String) -> Unit)? = null,
    playingAudioId: String? = null,
    isWhiteboardEnabled: Boolean = false
) {
    // 背面临时显示手写白板的状态
    var showBackWhiteboard by remember(grammar.id) { mutableStateOf(false) }

    // 获取触觉反馈控制器
    val haptic = LocalHapticFeedback.current

    // 检测深色模式
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // 颜色系统
    val cardBackground = if (isDarkTheme) NemoSurfaceCardDark else NemoSurfaceCard
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E7EB)
    val primaryTextColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    // Indigo 标签色
    val indigoBg = if (isDarkTheme) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
    val indigoBorder = if (isDarkTheme) Color(0xFF3730A3) else Color(0xFFE0E7FF)
    val indigoText = if (isDarkTheme) Color(0xFFA5B4FC) else Color(0xFF4F46E5)

    // Yellow 提示色
    val yellowBg = if (isDarkTheme) Color(0xFF713F12) else Color(0xFFFEFCE8)
    val yellowBorder = if (isDarkTheme) Color(0xFF92400E) else Color(0xFFFEF3C7)
    val yellowIcon = if (isDarkTheme) Color(0xFFFDE047) else Color(0xFFCA8A04)
    val yellowText = if (isDarkTheme) Color(0xFFFEF08A) else Color(0xFF92400E)

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
                .background(cardBackground, RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(indigoBg, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = grammar.grammarLevel,
                            color = indigoText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // 顶部手写白板按钮 (仅在背面且 isAnswerShown 为 true 时显示，避免错位和重叠)
                    if (isAnswerShown) {
                        Spacer(modifier = Modifier.width(8.dp))
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
                                contentDescription = "临时手写板",
                                tint = buttonTextColor,
                                modifier = Modifier.size(15.dp)
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
                        text = "思考这个语法的用法...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            cardBadge?.let { badge ->
                val (text, bgColor, textColor) = when (badge) {
                    CardBadge.NEW -> Triple("新学", if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFE0EDFF), if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1D4ED8))
                    CardBadge.LEARNING -> Triple("在学", if (isDarkTheme) Color(0xFF083344) else Color(0xFFE0F2FE), if (isDarkTheme) Color(0xFF22D3EE) else Color(0xFF0891B2)) // Cyan
                    CardBadge.REVIEW -> Triple("复习", if (isDarkTheme) Color(0xFF14532D) else Color(0xFFDCFCE7), if (isDarkTheme) Color(0xFFBBF7D0) else Color(0xFF166534))
                    CardBadge.RELEARN -> Triple("重学", if (isDarkTheme) Color(0xFF7C2D12) else Color(0xFFFFEDD5), if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
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
            val context = LocalContext.current
            val stickerName = remember(grammar.id) { getStickerForGrammar(grammar.id) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(300))
                    .padding(vertical = if (isWhiteboardEnabled) 8.dp else 24.dp),
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
                                .height(400.dp)
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("file:///android_asset/stickers/${stickerName}.svg")
                                .decoderFactory(SvgDecoder.Factory())
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(240.dp) // Slightly smaller than WordCard (320dp) as Grammar card is taller
                        )
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
                            .height(400.dp),
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 遍历每个用法，分别显示
                grammar.usages.forEachIndexed { usageIndex, usage ->
                    val isExpanded = expandedStates[usageIndex] ?: (usageIndex == 0)

                    // 用法标题（如果是多用法卡片，显示可点击的头部）
                    if (grammar.usages.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (usageIndex > 0) 8.dp else 0.dp)
                                .background(
                                    if (isExpanded) Color.Transparent else cardBackground,
                                    RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    expandedStates[usageIndex] = !isExpanded
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isExpanded) indigoBg else indigoBg.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                val chineseNumbers = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
                                val label = chineseNumbers.getOrNull(usageIndex) ?: (usageIndex + 1).toString()
                                Text(
                                    text = "用法$label",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isExpanded) indigoText else indigoText.copy(alpha = 0.7f),
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
                                imageVector = if (isExpanded)
                                    Icons.Rounded.KeyboardArrowUp
                                    else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "收起" else "展开",
                                tint = secondaryTextColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 用法详细内容（受折叠状态控制）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isExpanded || grammar.usages.size == 1,
                        enter = fadeIn() + slideInVertically { -10 },
                        exit = fadeOut()
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
                                            text = "接续",
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
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = indigoText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "说明",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = indigoText,
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
                                            text = "例句",
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
                                                if (isDarkTheme) Color(0xFF2D2D2D) else Color.White,
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(yellowBg, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = yellowIcon,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "TIPS: ${usage.notes}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = yellowText,
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
