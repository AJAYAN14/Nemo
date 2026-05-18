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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import com.jian.nemo.core.designsystem.theme.NemoCategoryColors
import com.jian.nemo.core.ui.component.text.FuriganaText

import com.jian.nemo.core.designsystem.theme.NotoSerifJP

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

    Surface(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = if (isFrontVisible) rotation else rotation - 180f
                cameraDistance = 16f * density
            }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = posColor.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(32.dp),
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
                    shape = RoundedCornerShape(32.dp)
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
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            if (isFrontVisible) {
                WordCardFront(word = word, onFlip = onFlip, onSpeak = onSpeak, themeColor = posColor)
            } else {
                WordCardBack(word = word, onFlip = onFlip, onSpeak = onSpeak, onSpeakText = onSpeakText, themeColor = posColor)
            }
        }
    }
}

@Composable
private fun WordCardFront(word: Word, onFlip: () -> Unit, onSpeak: () -> Unit, themeColor: Color) {
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
                    fontFamily = NotoSerifJP
                ),
                color = NemoTextLight.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // UI/UX PRO: 带有交互动效的大朗读按钮
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1.0f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                label = "speakButtonScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .size(84.dp)
                    .background(themeColor.copy(alpha = 0.1f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSpeak() }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onSpeak() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "朗读",
                    tint = themeColor,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        
        Text(
            text = "点 击 翻 转",
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
private fun WordCardBack(word: Word, onFlip: () -> Unit, onSpeak: () -> Unit, onSpeakText: (String) -> Unit, themeColor: Color) {
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
                    .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
                fontFamily = NotoSerifJP
            ),
            color = themeColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = word.hiragana,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = NotoSerifJP
            ),
            color = NemoTextLight
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp),
            thickness = 0.5.dp,
            color = NemoTextLight.copy(alpha = 0.2f)
        )

        Text(
            text = "词 义",
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
                text = "例 句",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = themeColor.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ExampleSentence(sentence = word.example1!!, translation = word.gloss1, onSpeakText = onSpeakText, themeColor = themeColor)
            word.example2?.let { ExampleSentence(sentence = it, translation = word.gloss2, onSpeakText = onSpeakText, themeColor = themeColor) }
            word.example3?.let { ExampleSentence(sentence = it, translation = word.gloss3, onSpeakText = onSpeakText, themeColor = themeColor) }
        }
    }
}

@Composable
private fun ExampleSentence(sentence: String, translation: String?, onSpeakText: (String) -> Unit, themeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .background(themeColor.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
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
        
        // UI/UX PRO: 带有交互动效的播放按钮
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.8f else 1.0f,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
            label = "playButtonScale"
        )

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .background(Color.Transparent, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onSpeakText(sentence) }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "朗读例句",
                tint = themeColor.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * UI/UX PRO: 获取基于分类 ID 或词性的主题色
 * 优先使用 categoryId，如果为空则回退到 pos
 */
fun getThemeColorForCategory(categoryId: String?, pos: String?, isDark: Boolean): Color {
    // 1. 优先从分类 ID 映射 (对应 CategoryClassificationScreen 中的 id)
    if (!categoryId.isNullOrBlank()) {
        val colors = NemoCategoryColors
        return when (categoryId) {
            "verb" -> if (isDark) colors.CardVerbTextDark else colors.CardVerbTextLight
            "noun" -> if (isDark) colors.CardNounTextDark else colors.CardNounTextLight
            "adj" -> if (isDark) colors.CardAdjITextDark else colors.CardAdjITextLight
            "adv" -> if (isDark) colors.CardAdvTextDark else colors.CardAdvTextLight
            "rentai" -> if (isDark) colors.CardRentaiTextDark else colors.CardRentaiTextLight
            "conj" -> if (isDark) colors.CardConjTextDark else colors.CardConjTextLight
            "particle" -> if (isDark) colors.CardFixTextDark else colors.CardFixTextLight
            "kata" -> if (isDark) colors.CardAdjNaTextDark else colors.CardAdjNaTextLight
            "prefix" -> if (isDark) colors.CardKataTextDark else colors.CardKataTextLight
            "suffix" -> if (isDark) colors.CardSoundTextDark else colors.CardSoundTextLight
            "expression" -> if (isDark) colors.CardKeigoTextDark else colors.CardKeigoTextLight
            "exclam" -> if (isDark) colors.CardIdiomTextDark else colors.CardIdiomTextLight
            else -> NemoPrimary
        }
    }

    // 2. 回退到基于词性字符串的模糊匹配
    if (pos == null) return NemoPrimary
    
    val p = pos.trim()
    val colors = NemoCategoryColors
    return when {
        // 动词
        p.contains("动词") || p.endsWith("动") -> 
            if (isDark) colors.CardVerbTextDark else colors.CardVerbTextLight
        
        // 名词
        p.contains("名词") || p == "名" -> 
            if (isDark) colors.CardNounTextDark else colors.CardNounTextLight
            
        // 形容词 (い)
        p.contains("形容词") && (p.contains("い") || p.contains("1")) -> 
            if (isDark) colors.CardAdjITextDark else colors.CardAdjITextLight
            
        // 形容动词 (な)
        p.contains("形容词") || p.contains("形动") || p.contains("な") -> 
            if (isDark) colors.CardAdjNaTextDark else colors.CardAdjNaTextLight
            
        // 副词
        p.contains("副词") || p == "副" -> 
            if (isDark) colors.CardAdvTextDark else colors.CardAdvTextLight
            
        // 接续词
        p.contains("接续") -> 
            if (isDark) colors.CardConjTextDark else colors.CardConjTextLight
            
        // 连体词
        p.contains("连体") -> 
            if (isDark) colors.CardRentaiTextDark else colors.CardRentaiTextLight
            
        // 外来语 / 片假名
        p.contains("外来") || p.contains("片假名") -> 
            if (isDark) colors.CardKataTextDark else colors.CardKataTextLight
            
        // 熟语 / 惯用
        p.contains("熟语") || p.contains("惯用") -> 
            if (isDark) colors.CardIdiomTextDark else colors.CardIdiomTextLight
            
        // 敬语
        p.contains("敬语") -> 
            if (isDark) colors.CardKeigoTextDark else colors.CardKeigoTextLight
            
        // 拟声 / 拟态
        p.contains("声") || p.contains("态") -> 
            if (isDark) colors.CardSoundTextDark else colors.CardSoundTextLight
            
        // 固定句 / 其他
        p.contains("固定") || p.contains("句") -> 
            if (isDark) colors.CardFixTextDark else colors.CardFixTextLight
            
        else -> NemoPrimary
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
            japanese = "食べる",
            hiragana = "たべる",
            chinese = "吃",
            level = "N5",
            pos = "动词",
            example1 = "ご飯を食べる",
            gloss1 = "Eat rice"
        )
        WordCard(word = word, isFlipped = false, onFlip = {}, onSpeak = {})
    }
}
