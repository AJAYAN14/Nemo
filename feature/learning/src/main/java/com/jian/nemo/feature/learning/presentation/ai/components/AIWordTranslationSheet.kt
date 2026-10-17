package com.jian.nemo.feature.learning.presentation.ai.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.domain.model.AIWordTranslation
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateContentSize

private enum class TranslationState {
    NONE, LOADING, SUCCESS, ERROR
}

/**
 * AI 划词/点词翻译底部面板
 *
 * 展示 AI 返回的单词/短语翻译结果，包括：
 * - 加载状态（质感柔和流体渐变背景）
 * - 成功状态（假名、词性、释义、语法备注）
 * - 错误状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWordTranslationSheet(
    showSheet: Boolean,
    translatingText: String,
    isTranslating: Boolean,
    translationResult: AIWordTranslation?,
    translationError: String?,
    onDismiss: () -> Unit,
    onSpeakWord: ((String) -> Unit)? = null
) {
    if (!showSheet) return

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val sheetBg = if (isDark) colorScheme.surfaceContainer else BentoColors.Surface
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    NemoModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        dragHandle = null,
        shape = RoundedCornerShape(0.dp),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Crossfade(
                targetState = when {
                    isTranslating -> TranslationState.LOADING
                    translationError != null -> TranslationState.ERROR
                    translationResult != null -> TranslationState.SUCCESS
                    else -> TranslationState.NONE
                },
                animationSpec = tween(durationMillis = 350),
                label = "translation_state_crossfade"
            ) { state ->
                when (state) {
                    TranslationState.LOADING -> {
                        TranslationLoadingContent(translatingText = translatingText, isDark = isDark)
                    }
                    TranslationState.ERROR -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .background(sheetBg)
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 12.dp, bottom = 12.dp)
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f))
                            )
                            TranslationErrorContent(translationError ?: "", isDark)
                        }
                    }
                    TranslationState.SUCCESS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .background(sheetBg)
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 12.dp, bottom = 12.dp)
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f))
                            )
                            TranslationSuccessContent(
                                result = translationResult!!,
                                isDark = isDark,
                                borderColor = borderColor,
                                onSpeakWord = onSpeakWord
                            )
                        }
                    }
                    else -> {
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}

/**
 * 加载态：100% 对齐 抽屉弹窗.html 原型规范
 * 采用全屏升腾的 WebGL2 极光引擎（高度 380dp，圆角 32dp），待翻译单词置于左上角，深浅色自适应
 */
@Composable
private fun TranslationLoadingContent(
    translatingText: String,
    isDark: Boolean
) {
    // 文字呼吸灯轻微呼吸动效
    val infiniteTransition = rememberInfiniteTransition(label = "loading_text_pulse")
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    val drawerBg = if (isDark) Color(0xFF06060E) else Color(0xFFFFFFFF)
    val handleBg = if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(drawerBg)
    ) {
        // 1. 扎根底部的原生 WebGL2 极光引擎
        AIAuroraBackground(
            isDark = isDark,
            modifier = Modifier.fillMaxSize()
        )

        // 2. 抽屉顶部标准拖拽指示条 (对齐 抽屉弹窗.html：宽 40px，高 5px)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .width(40.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(handleBg)
        )

        // 3. 待翻译单词放置在左上角（半透明磨砂胶囊卡片包裹，突出显示且不遮挡极光升腾）
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.85f),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.18f) else Color(0x18000000)
                ),
                shadowElevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF7DD3FC) else BentoColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = translatingText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isDark) Color.White else BentoColors.TextMain,
                        maxLines = 1
                    )
                }
            }
        }

        // 4. 左下角优雅的 AI 语义分析呼吸提示
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isDark) Color(0xFF7DD3FC) else BentoColors.Primary).copy(alpha = textAlpha)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "AI 正在分析语义...",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = if (isDark) Color.White.copy(alpha = textAlpha * 0.9f) else Color.Black.copy(alpha = textAlpha * 0.8f)
            )
        }
    }
}


/**
 * 错误态
 */
@Composable
private fun TranslationErrorContent(errorMsg: String, isDark: Boolean) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(40.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = errorMsg,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 成功态：精美展示翻译结果
 */
@Composable
private fun TranslationSuccessContent(
    result: AIWordTranslation,
    isDark: Boolean,
    borderColor: Color,
    onSpeakWord: ((String) -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 头部：假名 + 发音按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 假名标注
                if (result.kana.isNotBlank() && result.kana != result.word) {
                    Text(
                        text = result.kana,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = 2.sp
                        ),
                        color = BentoColors.Primary
                    )
                }

                // 大字标题（单词的标准书写形式）
                Text(
                    text = result.word,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
                )
            }

            // 发音按钮
            if (onSpeakWord != null) {
                IconButton(
                    onClick = { onSpeakWord(result.word) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "朗读",
                        tint = BentoColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 词性胶囊标签
        if (result.pos.isNotBlank()) {
            Surface(
                shape = CircleShape,
                color = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.PrimaryLight.copy(alpha = 0.7f)
            ) {
                Text(
                    text = result.pos,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = BentoColors.Primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider(color = borderColor)

        Spacer(Modifier.height(16.dp))

        // 中文释义区域
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                tint = BentoColors.AccentOrange,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "释义",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = result.meaning,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 26.sp
                    ),
                    color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
                )
            }
        }

        // 语法备注区域（如有）
        if (result.note.isNotBlank()) {
            Spacer(Modifier.height(16.dp))

            Surface(
                color = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = BentoColors.AccentOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = result.note,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
                    )
                }
            }
        }
    }
}
