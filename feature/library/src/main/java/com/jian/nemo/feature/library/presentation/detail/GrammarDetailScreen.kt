package com.jian.nemo.feature.library.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.presentation.components.dialogs.ContentReportDialog
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Report
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.srs.SrsStatusChip


/**
 * 语法详情界面 (UI/UX Pro Max)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GrammarDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: GrammarDetailViewModel = hiltViewModel()
) {
    val contextIds by viewModel.contextIds.collectAsState()
    val initialGrammar by viewModel.currentGrammar.collectAsState()
    val playingAudioId by viewModel.playingAudioId.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Reporting states
    val showReportDialog by viewModel.showReportDialog.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Swipe Navigation State (Moved to outer scope for Reporting logic)
    val initialIndex = remember(contextIds, initialGrammar) {
        val index = contextIds.indexOf(initialGrammar?.id ?: -1)
        if (index >= 0) index else 0
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialIndex,
        pageCount = { if (contextIds.isEmpty()) 1 else contextIds.size }
    )

    // [FIX] 解决异步加载导致 initialPage 失效的问题
    // 当 initialIndex 从 0 变为有效值时，强制 Pager 跳转到正确位置
    androidx.compose.runtime.LaunchedEffect(initialIndex) {
        if (initialIndex > 0 && pagerState.currentPage == 0) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (contextIds.isEmpty()) {
             // Fallback
             if (initialGrammar != null) {
                GrammarDetailContent(
                    grammar = initialGrammar!!,
                    isDark = isDark,
                    playingAudioId = playingAudioId,
                    onPlayAudio = viewModel::playAudio,
                    onBack = onNavigateBack,
                    onReportClick = viewModel::openReportDialog
                )
             } else {
                 Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NemoChasingDotsLoader()
                }
             }
        } else {
            // Swipe Navigation
            // (Calculation and pagerState moved to outer scope)

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val grammarId = contextIds[page]
                val grammar by remember(grammarId) { viewModel.getGrammarFlow(grammarId) }.collectAsState(initial = null)

                if (grammar != null) {
                    GrammarDetailContent(
                        grammar = grammar!!,
                        isDark = isDark,
                        playingAudioId = playingAudioId,
                        onPlayAudio = viewModel::playAudio,
                        onBack = onNavigateBack,
                        onReportClick = viewModel::openReportDialog
                    )
                } else {
                     Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NemoChasingDotsLoader()
                    }
                }
            }
        }

        // Result Snackbars
        NemoSnackbar(
            visible = successMessage != null,
            message = successMessage ?: "",
            type = NemoSnackbarType.SUCCESS,
            icon = Icons.Rounded.CheckCircle,
            onDismiss = { viewModel.clearSuccessMessage() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        )

        NemoSnackbar(
            visible = errorMessage != null,
            message = errorMessage ?: "",
            type = NemoSnackbarType.ERROR,
            icon = Icons.Rounded.Report,
            onDismiss = { viewModel.clearErrorMessage() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        )

        // Report Dialog
        if (showReportDialog) {
            val currentGrammarId = if (contextIds.isEmpty()) initialGrammar?.id else contextIds.getOrNull(pagerState.currentPage)
            ContentReportDialog(
                learningMode = LearningMode.Grammar,
                onDismiss = { viewModel.cancelReportDialog() },
                onConfirm = { errorType, desc -> currentGrammarId?.let { viewModel.reportContentError(it, errorType, desc) } }
            )
        }
    }
}

@Composable
private fun GrammarDetailContent(
    grammar: Grammar,
    isDark: Boolean,
    playingAudioId: String?,
    onPlayAudio: (String, String?) -> Unit,
    onBack: () -> Unit,
    onReportClick: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val accentColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else MaterialTheme.colorScheme.primary

    // 计算滚动过渡进度：前 90dp 范围内平滑淡入渐变遮罩并过渡
    val transitionDistancePx = with(density) { 90.dp.toPx() }
    val scrollProgress = (scrollState.value / transitionDistancePx).coerceIn(0f, 1f)
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // === 1. 可滚动主体内容层 (挂载 layerBackdrop 用于背景折射采样) ===
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .verticalScroll(scrollState)
        ) {
            // === 1.1 Immersive Hero Section ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) listOf(
                                accentColor.copy(alpha = if (isPlumpDuo) 0.20f else 0.2f),
                                MaterialTheme.colorScheme.background
                            ) else listOf(
                                accentColor.copy(alpha = if (isPlumpDuo) 0.10f else 0.1f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                // Hero Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        // 预留顶部固定 AppBar 的高度与安全间距
                        .padding(top = 76.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grammar Title
                    com.jian.nemo.core.ui.component.text.FuriganaText(
                        text = grammar.grammar ?: "",
                        baseTextStyle = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = NotoSerifJP
                        ),
                        baseTextColor = MaterialTheme.colorScheme.onBackground,
                        furiganaTextColor = if (isDark) accentColor.copy(alpha = if (isPlumpDuo) 0.85f else 0.8f) else accentColor.copy(alpha = if (isPlumpDuo) 0.75f else 0.7f),
                        furiganaTextSize = 12.sp,
                        furiganaFontFamily = NotoSerifJP
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Level Tag (Enhanced)
                    grammar.grammarLevel?.let { level ->
                        val levelColor = if (isPlumpDuo) {
                            when (level.uppercase()) {
                                "N5" -> IsoluminantPalette.EmeraldGreen
                                "N4" -> IsoluminantPalette.LakesideCyan
                                "N3" -> IsoluminantPalette.SkyIndigo
                                "N2" -> IsoluminantPalette.WarmAmber
                                "N1" -> IsoluminantPalette.RoseRuby
                                else -> IsoluminantPalette.EmeraldGreen
                            }
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isPlumpDuo) levelColor.copy(alpha = if (isDark) 0.18f else 0.12f) else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.height(32.dp)
                        ) {
                             Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isPlumpDuo) levelColor else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Play Audio Button
                    val grammarAudioId = "grammar_${grammar.id}"
                    SpeakerButton(
                        isPlaying = playingAudioId == grammarAudioId,
                        onClick = { onPlayAudio(grammar.grammar ?: "", grammarAudioId) },
                        size = 56.dp,
                        backgroundColor = if (isPlumpDuo) accentColor.copy(alpha = if (isDark) 0.18f else 0.12f) else MaterialTheme.colorScheme.primaryContainer,
                        tint = if (isPlumpDuo) accentColor else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

        // === 2. Usages ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            grammar.usages.forEachIndexed { index, usage ->
                // Usage Header (if multiple)
                if (grammar.usages.size > 1) {
                    Text(
                        text = "用法 ${index + 1}${if (usage.subtype != null) " · ${usage.subtype}" else ""}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 12.dp, top = if (index > 0) 24.dp else 0.dp)
                    )
                }

                PremiumDetailCard(isDark = isDark) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Explanation
                        if (usage.explanation.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPlumpDuo) {
                                    PlumpDuoIcon(
                                        icon = PlumpIcon.BookOpen,
                                        color = accentColor,
                                        size = 18.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "解释",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            com.jian.nemo.core.ui.component.text.FuriganaText(
                                text = usage.explanation,
                                baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp,
                                    lineHeight = 26.sp
                                ),
                                baseTextColor = MaterialTheme.colorScheme.onSurface,
                                furiganaTextColor = if (isDark) accentColor.copy(alpha = if (isPlumpDuo) 0.75f else 0.7f) else accentColor.copy(alpha = if (isPlumpDuo) 0.65f else 0.6f),
                                furiganaTextSize = 10.sp
                            )
                        }

                        // Connection Rules (Visualized)
                        if (usage.connection.isNotBlank()) {
                            val connectionColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else NemoOrange
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPlumpDuo) {
                                    PlumpDuoIcon(
                                        icon = PlumpIcon.Link2,
                                        color = connectionColor,
                                        size = 18.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = NemoOrange
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "接续",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = connectionColor
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            ConnectionPill(
                                text = usage.connection.trim(),
                                isDark = isDark,
                                isPlumpDuo = isPlumpDuo,
                                accentColor = connectionColor
                            )
                        }

                        // Notes
                        if (!usage.notes.isNullOrBlank()) {
                            val notesColor = if (isPlumpDuo) IsoluminantPalette.CoralFlame else MaterialTheme.colorScheme.error
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "注意",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = notesColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            com.jian.nemo.core.ui.component.text.FuriganaText(
                                text = usage.notes?.trim() ?: "",
                                baseTextStyle = MaterialTheme.typography.bodyMedium,
                                baseTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                furiganaTextColor = notesColor.copy(alpha = 0.7f),
                                furiganaTextSize = 9.sp,
                                furiganaFontFamily = NotoSerifJP
                            )
                        }

                        // Examples (Boxed inside the Card)
                        if (usage.examples.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Example Container
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.softCardShadow(borderRadius = 16.dp, isDark = isDark)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    usage.examples.forEachIndexed { exIndex, example ->
                                        GrammarExampleItem(
                                            sentence = example.sentence,
                                            translation = example.translation,
                                            isDark = isDark,
                                            playingAudioId = playingAudioId,
                                            onPlayAudio = onPlayAudio,
                                            grammarId = grammar.id,
                                            usageIndex = index,
                                            exampleIndex = exIndex,
                                            accentColor = accentColor
                                        )
                                        if (exIndex < usage.examples.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } // End of Column (padding 20.dp)
                } // End of PremiumDetailCard
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

        // === 2. 悬浮固定顶栏 (带柔和垂直渐变保护遮罩，读取 backdrop) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f * scrollProgress),
                            backgroundColor.copy(alpha = 0.75f * scrollProgress),
                            backgroundColor.copy(alpha = 0.35f * scrollProgress),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = "",
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                centerContent = {
                    SrsStatusChip(
                        nextReviewDay = grammar.nextReviewDate,
                        repetitionCount = grammar.repetitionCount,
                        backdrop = backdrop
                    )
                },
                actions = {
                    FrostedLiquidGlassButton(
                        onClick = onReportClick,
                        backdrop = backdrop,
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        isInteractive = true,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Report,
                            contentDescription = "举报内容",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    }
}

/**
 * Visual Pill for Connection Rules
 */
@Composable
private fun ConnectionPill(
    text: String,
    isDark: Boolean,
    isPlumpDuo: Boolean = false,
    accentColor: Color = NemoOrange
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isPlumpDuo) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        com.jian.nemo.core.ui.component.text.FuriganaText(
            text = text,
            baseTextStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            baseTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            furiganaTextColor = if (isDark) accentColor.copy(alpha = 0.8f) else accentColor.copy(alpha = 0.7f),
            furiganaTextSize = 9.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Grammar Example Item (Simpler than Card)
 */
@Composable
private fun GrammarExampleItem(
    sentence: String,
    translation: String,
    isDark: Boolean,
    playingAudioId: String?,
    onPlayAudio: (String, String?) -> Unit,
    grammarId: Int,
    usageIndex: Int,
    exampleIndex: Int,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val exampleId = "grammar_${grammarId}_u${usageIndex}_e${exampleIndex}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
        com.jian.nemo.core.ui.component.text.FuriganaText(
            text = sentence,
            baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal, // Changed from Medium to Normal
                fontFamily = NotoSerifJP
            ),
            baseTextColor = MaterialTheme.colorScheme.onBackground,
            furiganaTextColor = if (isDark) accentColor.copy(alpha = 0.8f) else accentColor.copy(alpha = 0.7f),
            furiganaTextSize = 9.sp,
            furiganaFontFamily = NotoSerifJP
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = translation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        }

        Spacer(modifier = Modifier.width(8.dp))

        SpeakerButton(
            isPlaying = playingAudioId == exampleId,
            onClick = { onPlayAudio(sentence, exampleId) },
            size = 44.dp,
            tint = accentColor
        )
    }
}

/**
 * Reusable Premium Card
 */
@Composable
private fun PremiumDetailCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 20.dp, isDark = isDark),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        content = { Column(content = content) }
    )
}
