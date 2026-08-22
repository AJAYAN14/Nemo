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
                        TranslationLoadingContent(translatingText)
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
 * 柔和色彩流体渐变 Canvas 组件
 * 还原 6 色模糊交融的质感流光背景，并提速一倍以上
 */
@Composable
private fun FluidGradientCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_gradient")

    // 周期缩短至 2.5s~4.2s，大幅度提升流动感和动效速度
    val p1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p1"
    )
    val p2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p2"
    )
    val p3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p3"
    )
    val p4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p4"
    )
    val p5 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p5"
    )
    val p6 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p6"
    )

    // 对应 CSS 中的明亮鲜艳色值系
    val c1 = Color(0xFFFF66FF) // 明亮洋红
    val c2 = Color(0xFF66FFFF) // 明亮湖蓝青
    val c3 = Color(0xFFFFE666) // 明亮暖阳黄
    val c4 = Color(0xFFFF8566) // 明亮珊瑚橙红
    val c5 = Color(0xFF66FF85) // 明亮治愈绿
    val c6 = Color(0xFFB385FF) // 明亮优雅蓝紫

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(60.dp) // 系统高斯模糊
    ) {
        val w = size.width
        val h = size.height

        // 1. blob1
        val cx1 = -0.3f * w + 0.7f * w + (0.2f * w * p1)
        val cy1 = -0.3f * h + 0.7f * h + (0.15f * h * p1)
        val r1 = 0.7f * w * (1f + 0.1f * p1)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1, Color.Transparent),
                center = Offset(cx1, cy1),
                radius = r1
            ),
            center = Offset(cx1, cy1),
            radius = r1
        )

        // 2. blob2
        val dx2 = -0.3f * w * p2 + 0.15f * w * (1f - p2)
        val dy2 = -0.1f * h * p2 + 0.25f * h * (1f - p2)
        val cx2 = -0.2f * w + 0.75f * w + dx2
        val cy2 = 0.2f * h + 0.75f * h + dy2
        val r2 = 0.75f * w * (1f + 0.2f * p2)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c2, Color.Transparent),
                center = Offset(cx2, cy2),
                radius = r2
            ),
            center = Offset(cx2, cy2),
            radius = r2
        )

        // 3. blob3
        val cx3 = 0.1f * w + 0.6f * w - (0.2f * w * p3)
        val cy3 = 0.2f * h + 0.6f * h - (0.3f * h * p3)
        val r3 = 0.6f * w * (1f + 0.1f * p3)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c3, Color.Transparent),
                center = Offset(cx3, cy3),
                radius = r3
            ),
            center = Offset(cx3, cy3),
            radius = r3
        )

        // 4. blob4
        val dx4 = -0.25f * w * p4 + 0.2f * w * (1f - p4)
        val dy4 = 0.25f * h * p4 - 0.15f * h * (1f - p4)
        val cx4 = 0.3f * w + 0.65f * w + dx4
        val cy4 = -0.1f * h + 0.65f * h + dy4
        val r4 = 0.65f * w * (1f + 0.2f * p4)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c4, Color.Transparent),
                center = Offset(cx4, cy4),
                radius = r4
            ),
            center = Offset(cx4, cy4),
            radius = r4
        )

        // 5. blob5
        val dx5 = -0.4f * w * p5 + 0.1f * w * (1f - p5)
        val dy5 = -0.3f * h * p5 + 0.1f * h * (1f - p5)
        val cx5 = 0.2f * w + 0.55f * w + dx5
        val cy5 = 0.1f * h + 0.55f * h + dy5
        val r5 = 0.55f * w * (1f + 0.3f * p5)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c5, Color.Transparent),
                center = Offset(cx5, cy5),
                radius = r5
            ),
            center = Offset(cx5, cy5),
            radius = r5
        )

        // 6. blob6
        val cx6 = -0.2f * w + 0.8f * w + (0.25f * w * p6)
        val cy6 = 0.4f * h + 0.8f * h - (0.3f * h * p6)
        val r6 = 0.8f * w * (1f + 0.15f * p6)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c6, Color.Transparent),
                center = Offset(cx6, cy6),
                radius = r6
            ),
            center = Offset(cx6, cy6),
            radius = r6
        )
    }
}

/**
 * 加载态：在自制深色圆角背板中平铺流体渐变，解决顶部白边割裂感；高饱和色彩呈现
 */
@Composable
private fun TranslationLoadingContent(translatingText: String) {
    // 文字高对比度呼吸灯动效
    val infiniteTransition = rememberInfiniteTransition(label = "loading_text_pulse")
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xFF050505)) // 强制使用黑色底，释放鲜艳色彩
    ) {
        // 1. 铺底流体渐变
        FluidGradientCanvas(modifier = Modifier.fillMaxSize())

        // 2. 超轻量暗色半透明保护层，既还原高亮鲜艳颜色，又能保障字体清晰
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
        )

        // 3. 顶部自制 Drag Handle 指示器，维持加载弹窗的高级外观一致性
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        )

        // 4. 文字内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = translatingText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "AI 正在分析语义...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White.copy(alpha = textAlpha),
                textAlign = TextAlign.Center
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
