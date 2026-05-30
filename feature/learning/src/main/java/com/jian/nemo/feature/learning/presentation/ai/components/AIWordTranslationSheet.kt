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

/**
 * AI 划词/点词翻译底部面板
 *
 * 展示 AI 返回的单词/短语翻译结果，包括：
 * - 加载状态（骨架屏动画）
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            when {
                isTranslating -> TranslationLoadingContent(translatingText, isDark)
                translationError != null -> TranslationErrorContent(translationError, isDark)
                translationResult != null -> TranslationSuccessContent(
                    result = translationResult,
                    isDark = isDark,
                    borderColor = borderColor,
                    onSpeakWord = onSpeakWord
                )
            }
        }
    }
}

/**
 * 加载态：显示用户选中的文字和精致的加载动画
 */
@Composable
private fun TranslationLoadingContent(translatingText: String, isDark: Boolean) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 选中文本展示
        Text(
            text = translatingText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = if (isDark) colorScheme.onSurface else BentoColors.TextMain,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        NemoChasingDotsLoader(size = 40.dp)

        Spacer(Modifier.height(12.dp))

        Text(
            text = "AI 正在分析语义...",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
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
