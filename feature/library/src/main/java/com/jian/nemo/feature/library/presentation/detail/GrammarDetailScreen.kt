package com.jian.nemo.feature.library.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
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
    val scrollState = rememberScrollState()
    val hazeState = remember { HazeState() }
    val navGroupBg = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
                .verticalScroll(scrollState)
        ) {
            // === 1. Immersive Hero Section ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background
                            ) else listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
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
                        .padding(top = 64.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
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
                        furiganaTextColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        furiganaTextSize = 12.sp,
                        furiganaFontFamily = NotoSerifJP
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Level Tag (Enhanced)
                    grammar.grammarLevel?.let { level ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.height(32.dp)
                        ) {
                             Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
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
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp, top = if (index > 0) 24.dp else 0.dp)
                    )
                }

                PremiumDetailCard(isDark = isDark) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Explanation
                        if (usage.explanation.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "解释",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
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
                                furiganaTextColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                furiganaTextSize = 10.sp
                            )
                        }

                        // Connection Rules (Visualized)
                        if (usage.connection.isNotBlank()) {
                             Spacer(modifier = Modifier.height(24.dp))
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = NemoOrange
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "接续",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NemoOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            ConnectionPill(text = usage.connection.trim(), isDark = isDark)
                        }

                        // Notes
                        if (!usage.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "注意",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            com.jian.nemo.core.ui.component.text.FuriganaText(
                                text = usage.notes?.trim() ?: "",
                                baseTextStyle = MaterialTheme.typography.bodyMedium,
                                baseTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                furiganaTextColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
                                            exampleIndex = exIndex
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

        // Common Header (Overlaid on top with haze)
        com.jian.nemo.core.ui.component.common.CommonHeader(
            title = "",
            onBack = onBack,
            hazeState = hazeState,
            backgroundColor = Color.Transparent,
            centerContent = {
                com.jian.nemo.core.ui.component.srs.SrsStatusChip(
                    nextReviewDay = grammar.nextReviewDate,
                    repetitionCount = grammar.repetitionCount
                )
            },
            actions = {
                com.jian.nemo.core.ui.component.liquid.LiquidButton(
                    onClick = onReportClick,
                    backgroundColor = navGroupBg,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    isInteractive = true,
                    modifier = Modifier.size(44.dp)
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

/**
 * Visual Pill for Connection Rules
 */
@Composable
private fun ConnectionPill(text: String, isDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        com.jian.nemo.core.ui.component.text.FuriganaText(
            text = text,
            baseTextStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            baseTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            furiganaTextColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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
    exampleIndex: Int
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
            furiganaTextColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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
            tint = MaterialTheme.colorScheme.primary
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
