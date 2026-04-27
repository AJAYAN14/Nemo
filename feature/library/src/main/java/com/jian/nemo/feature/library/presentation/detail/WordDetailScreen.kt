package com.jian.nemo.feature.library.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.presentation.components.dialogs.ContentReportDialog
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Report

/**
 * 单词详情界面 (UI/UX Pro Max)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WordDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: WordDetailViewModel = hiltViewModel()
) {
    val contextIds by viewModel.contextIds.collectAsState()
    val initialWord by viewModel.currentWord.collectAsState()
    val playingAudioId by viewModel.playingAudioId.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Reporting states
    val showReportDialog by viewModel.showReportDialog.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Swipe Navigation State (Moved to outer scope for Reporting logic)
    val initialIndex = remember(contextIds, initialWord) {
        val index = contextIds.indexOf(initialWord?.id ?: -1)
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
            // Fallback: Show initial word or loading while fetching context
            if (initialWord != null) {
                WordDetailContent(
                    word = initialWord!!,
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
                    CircularProgressIndicator()
                }
            }
        } else {
            // Swipe Navigation Enabled
            // Calculate initial page based on the starting word ID
            // (Calculation and pagerState moved to outer scope)

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val wordId = contextIds[page]
                // Fetch word for this specific page
                val word by remember(wordId) { viewModel.getWordFlow(wordId) }.collectAsState(initial = null)

                if (word != null) {
                    WordDetailContent(
                        word = word!!,
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
                        CircularProgressIndicator()
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
            val currentWordId = if (contextIds.isEmpty()) initialWord?.id else contextIds.getOrNull(pagerState.currentPage)
            ContentReportDialog(
                learningMode = LearningMode.Word,
                onDismiss = { viewModel.cancelReportDialog() },
                onConfirm = { currentWordId?.let { viewModel.reportContentError(it) } }
            )
        }
    }
}

@Composable
private fun WordDetailContent(
    word: Word,
    isDark: Boolean,
    playingAudioId: String?,
    onPlayAudio: (String, String?) -> Unit,
    onBack: () -> Unit,
    onReportClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            // Hero Content (Centered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Add padding for content below status bar
                    .padding(top = 56.dp, bottom = 32.dp, start = 24.dp, end = 24.dp), // Add top padding to clear header
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Word (Kanji)
                Text(
                    text = word.japanese,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hiragana
                Text(
                    text = word.hiragana,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Play Audio Button (Hero Style)
                val wordAudioId = "word_${word.id}"
                SpeakerButton(
                    isPlaying = playingAudioId == wordAudioId,
                    onClick = { onPlayAudio(word.japanese, wordAudioId) },
                    size = 56.dp,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Common Header (Overlaid)
            com.jian.nemo.core.ui.component.common.CommonHeader(
                title = "", // Empty title for hero section look
                onBack = onBack,
                backgroundColor = Color.Transparent,
                actions = {
                    IconButton(onClick = onReportClick) {
                        Icon(
                            imageVector = Icons.Rounded.Report,
                            contentDescription = "举报内容",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }

        // === 2. Meaning & Tags ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            PremiumDetailCard(isDark = isDark) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Chinese Meaning
                    Text(
                        text = "中文释义",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = word.chinese,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tags Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Level Tag
                        DetailTag(
                            text = word.level,
                            containerColor = NemoPrimary,
                            contentColor = Color.White
                        )

                        // POS Tag
                        word.pos?.let { pos ->
                            DetailTag(
                                text = pos,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === 3. Example Sentences ===
            Text(
                text = "例句",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            val examples = listOfNotNull(
                if (!word.example1.isNullOrBlank() && !word.gloss1.isNullOrBlank()) Triple(word.example1!!, word.gloss1!!, 1) else null,
                if (!word.example2.isNullOrBlank() && !word.gloss2.isNullOrBlank()) Triple(word.example2!!, word.gloss2!!, 2) else null,
                if (!word.example3.isNullOrBlank() && !word.gloss3.isNullOrBlank()) Triple(word.example3!!, word.gloss3!!, 3) else null
            )

            if (examples.isNotEmpty()) {
                examples.forEach { (sentence, translation, index) ->
                    ExampleCard(
                        sentence = sentence,
                        translation = translation,
                        isDark = isDark,
                        index = index,
                        playingAudioId = playingAudioId,
                        onPlayAudio = onPlayAudio,
                        wordId = word.id
                    )
                    if (index < examples.size) {
                         Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                 Text(
                    text = "暂无例句",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Premium Card Container
 */
@Composable
private fun PremiumDetailCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val shadowElevation = if (isDark) 2.dp else 8.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(24.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor
            ),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        content = { Column(content = content) }
    )
}

/**
 * Detail Tag (Pill Shape)
 */
@Composable
private fun DetailTag(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = Modifier.height(28.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

/**
 * Example Sentence Card
 */
@Composable
private fun ExampleCard(
    sentence: String,
    translation: String,
    isDark: Boolean,
    index: Int,
    playingAudioId: String?,
    onPlayAudio: (String, String?) -> Unit,
    wordId: Int
) {
    val exampleId = "example_${wordId}_$index"

    PremiumDetailCard(isDark = isDark) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Index Number
            Text(
                text = "$index.",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                com.jian.nemo.core.ui.component.text.FuriganaText(
                    text = sentence,
                    baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal, // Changed from Medium to Normal
                        lineHeight = 24.sp
                    ),
                    baseTextColor = MaterialTheme.colorScheme.onSurface,
                    furiganaTextColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    furiganaTextSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Translation
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Speaker Button
            SpeakerButton(
                isPlaying = playingAudioId == exampleId,
                onClick = { onPlayAudio(sentence, exampleId) },
                size = 44.dp,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
