package com.jian.nemo.feature.learning.presentation.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.speaker.SoundWaveAnimation
import com.jian.nemo.core.designsystem.theme.NemoCategoryTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.learning.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoPrimaryDark
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import com.jian.nemo.core.ui.component.text.FuriganaText
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.speaker.SpeakerButton

/**
 * 单词卡片组件 (3D Flip)
 *
 * 特性：
 * 1. 3D 翻转动画 (Spring stiffness=200f)
 * 2. 增强透视效果 (CameraDistance 16f)
 * 3. 单 Surface + 条件渲染：任何时刻只有一面存在于组合树，彻底消除手势冲突
 */
@Composable
fun WordCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeak: () -> Unit,
    onSpeakText: (String) -> Unit = {},
    isPlaying: Boolean = false,
    playingText: String? = null,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    categoryId: String? = null, // 锁定的分类 ID
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "cardRotation"
    )
    val density = LocalDensity.current.density
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // UI/UX PRO: 优先基于锁定的分类色，其次基于单词词性
    val posColor = remember(word.pos, categoryId, isDarkTheme) {
        getThemeColorForCategory(categoryId, word.pos, isDarkTheme)
    }

    // 90° 临界点切换内容：卡片侧立时不可见，切换无感知
    val isFrontVisible = rotation < 90f

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxSize()
            .softCardShadow(borderRadius = 32.dp, isDark = isDark)
            .graphicsLayer {
                rotationY = if (isFrontVisible) rotation else rotation - 180f
                cameraDistance = 16f * density
            },
        shape = RoundedRectangle(32.dp),
        color = cardColor,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDarkTheme) 0.05f else 0.8f),
                            cardColor
                        )
                    ),
                    shape = RoundedRectangle(32.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            if (isFrontVisible) Color.White.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedRectangle(32.dp)
                )
        ) {
            if (isFrontVisible) {
                val isFrontWordPlaying = isPlaying || (playingText != null && playingText == word.japanese)
                WordCardFront(word = word, onFlip = onFlip, onSpeak = onSpeak, themeColor = posColor, isPlaying = isFrontWordPlaying)
            } else {
                WordCardBack(word = word, onFlip = onFlip, onSpeak = onSpeak, onSpeakText = onSpeakText, themeColor = posColor, playingText = playingText)
            }
        }
    }
}

@Composable
private fun WordCardFront(word: Word, onFlip: () -> Unit, onSpeak: () -> Unit, themeColor: Color, isPlaying: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = onFlip,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // UI/UX PRO: 增强排版感
            FuriganaText(
                text = word.japanese,
                baseTextStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 54.sp,
                    letterSpacing = 2.sp,
                    fontFamily = NotoSerifJP
                ),
                baseTextColor = themeColor,
                furiganaTextSize = 16.sp,
                furiganaTextColor = NemoTextLight.copy(alpha = 0.7f),
                furiganaFontFamily = NotoSerifJP
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = word.hiragana,
                style = MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = NotoSerifJP,
                    localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                ),
                color = NemoTextLight.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 48dp 规格液态玻璃磨砂朗读按钮（无背景折射，杜绝闪退）
            FrostedLiquidGlassButton(
                onClick = onSpeak,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                isInteractive = true,
                modifier = Modifier.size(48.dp)
            ) {
                if (isPlaying) {
                    SoundWaveAnimation(
                        tint = themeColor,
                        size = 32.dp
                    )
                } else {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = stringResource(R.string.learn_word_card_action_speak),
                        tint = themeColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        Text(
            text = stringResource(R.string.learn_word_card_hint_tap_to_flip),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = NemoTextLight.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun WordCardBack(
    word: Word,
    onFlip: () -> Unit,
    onSpeak: () -> Unit,
    onSpeakText: (String) -> Unit,
    themeColor: Color,
    playingText: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = onFlip,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // UI/UX PRO: 词性标签置于显著位置
        word.pos?.let {
            Box(
                modifier = Modifier
                    .background(themeColor.copy(alpha = 0.15f), RoundedRectangle(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = themeColor
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = word.japanese,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                fontFamily = NotoSerifJP,
                localeList = androidx.compose.ui.text.intl.LocaleList("ja")
            ),
            color = themeColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = word.hiragana,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = NotoSerifJP,
                localeList = androidx.compose.ui.text.intl.LocaleList("ja")
            ),
            color = NemoTextLight
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp),
            thickness = 0.5.dp,
            color = NemoTextLight.copy(alpha = 0.2f)
        )

        Text(
            text = stringResource(R.string.learn_word_card_label_meaning),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = themeColor.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = word.chinese,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            ),
            lineHeight = 28.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        if (!word.example1.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.learn_word_card_label_example),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = themeColor.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExampleSentence(sentence = word.example1!!, translation = word.gloss1, onSpeakText = onSpeakText, themeColor = themeColor, playingText = playingText)
            word.example2?.let { ExampleSentence(sentence = it, translation = word.gloss2, onSpeakText = onSpeakText, themeColor = themeColor, playingText = playingText) }
            word.example3?.let { ExampleSentence(sentence = it, translation = word.gloss3, onSpeakText = onSpeakText, themeColor = themeColor, playingText = playingText) }
        }
    }
}

@Composable
private fun ExampleSentence(
    sentence: String,
    translation: String?,
    onSpeakText: (String) -> Unit,
    themeColor: Color,
    playingText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .background(themeColor.copy(alpha = 0.03f), RoundedRectangle(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            FuriganaText(
                text = sentence,
                baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp,
                    fontFamily = NotoSerifJP
                ),
                furiganaTextSize = 10.sp,
                furiganaTextColor = NemoTextLight.copy(alpha = 0.6f),
                furiganaFontFamily = NotoSerifJP
            )
            if (!translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NemoTextLight.copy(alpha = 0.8f)
                )
            }
        }
        
        // UI/UX PRO: 带有声纹跳动与液态交互的例句播放按钮
        val isSentencePlaying = playingText != null && playingText == sentence
        SpeakerButton(
            isPlaying = isSentencePlaying,
            onClick = { onSpeakText(sentence) },
            tint = themeColor.copy(alpha = if (isSentencePlaying) 1.0f else 0.6f),
            size = 40.dp
        )
    }
}

/**
 * 获取基于分类 ID 或词性的调色盘单一标准主题色
 * 优先使用 categoryId，如果为空则回退到 pos
 */
fun getThemeColorForCategory(categoryId: String?, pos: String?, isDark: Boolean): Color {
    return if (!categoryId.isNullOrBlank()) {
        NemoCategoryTheme.getCategoryColor(categoryId, isDark)
    } else {
        NemoCategoryTheme.getPosColor(pos, isDark)
    }
}

@Deprecated("Use getThemeColorForCategory", ReplaceWith("getThemeColorForCategory(null, pos, isDark)"))
fun getThemeColorForPos(pos: String?, isDark: Boolean): Color = getThemeColorForCategory(null, pos, isDark)

@Preview
@Composable
private fun WordCardPreview() {
    MaterialTheme {
        val word = Word(
            id = 1,
            rawId = "1",
            japanese = stringResource(R.string.learning_card_preview_mock_japanese),
            hiragana = stringResource(R.string.learning_card_preview_mock_hiragana),
            chinese = stringResource(R.string.learning_card_preview_mock_chinese),
            level = "N5",
            pos = stringResource(R.string.learning_card_preview_mock_pos),
            example1 = stringResource(R.string.learning_card_preview_mock_example),
            gloss1 = "Eat rice"
        )
        WordCard(word = word, isFlipped = false, onFlip = {}, onSpeak = {})
    }
}
