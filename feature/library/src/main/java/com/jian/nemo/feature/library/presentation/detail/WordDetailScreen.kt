package com.jian.nemo.feature.library.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.jian.nemo.core.designsystem.shape.Capsule
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
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
import com.jian.nemo.core.ui.component.ContentReportDialog
import com.jian.nemo.core.domain.model.LearningMode
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Report
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.library.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.srs.SrsStatusChip

import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle

/**
 * 单词详情界面 (UI/UX Pro Max)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WordDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit = {},
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
                val relatedWords by remember(initialWord!!.id) { viewModel.getRelatedWordsFlow(initialWord!!.id) }.collectAsState(initial = emptyList())
                WordDetailContent(
                    word = initialWord!!,
                    isDark = isDark,
                    playingAudioId = playingAudioId,
                    onPlayAudio = viewModel::playAudio,
                    onBack = onNavigateBack,
                    onReportClick = viewModel::openReportDialog,
                    relatedWords = relatedWords,
                    onNavigateToWordDetail = onNavigateToWordDetail
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
                val relatedWords by remember(wordId) { viewModel.getRelatedWordsFlow(wordId) }.collectAsState(initial = emptyList())

                if (word != null) {
                    WordDetailContent(
                        word = word!!,
                        isDark = isDark,
                        playingAudioId = playingAudioId,
                        onPlayAudio = viewModel::playAudio,
                        onBack = onNavigateBack,
                        onReportClick = viewModel::openReportDialog,
                        relatedWords = relatedWords,
                        onNavigateToWordDetail = onNavigateToWordDetail
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
            val currentWordId = if (contextIds.isEmpty()) initialWord?.id else contextIds.getOrNull(pagerState.currentPage)
            ContentReportDialog(
                learningMode = LearningMode.Word,
                onDismiss = { viewModel.cancelReportDialog() },
                onConfirm = { errorType, desc -> currentWordId?.let { viewModel.reportContentError(it, errorType, desc) } }
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
    onReportClick: () -> Unit,
    relatedWords: List<com.jian.nemo.core.domain.model.RelatedWordItem> = emptyList(),
    onNavigateToWordDetail: (Int) -> Unit = {}
) {
    val backdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val isSoftTranslucent = LocalSettingIconStyle.current == SettingIconStyle.SOFT_TRANSLUCENT
    val accentColor = if (isDark) SystemColors.BlueDark else SystemColors.Blue

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
                                accentColor.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.background
                            ) else listOf(
                                accentColor.copy(alpha = 0.10f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                // Hero Content (Centered)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        // 预留顶部固定 AppBar 的高度与安全间距
                        .padding(top = 76.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Word (Kanji)
                    Text(
                        text = word.japanese,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = NotoSerifJP
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (word.hiragana != word.japanese) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Hiragana
                        Text(
                            text = word.hiragana,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = NotoSerifJP
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Play Audio Button (Hero Style)
                    val wordAudioId = "word_${word.id}"
                    val speakerBg = if (isSoftTranslucent) accentColor.copy(alpha = if (isDark) 0.18f else 0.12f) else accentColor
                    val speakerTint = if (isSoftTranslucent) accentColor else Color.White
                    SpeakerButton(
                        isPlaying = playingAudioId == wordAudioId,
                        onClick = { onPlayAudio(word.japanese, wordAudioId) },
                        size = 56.dp,
                        backgroundColor = speakerBg,
                        tint = speakerTint
                    )
                }
            }

            // === 1.2 Meaning & Tags ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                PremiumDetailCard(isDark = isDark) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Chinese Meaning
                        Text(
                            text = stringResource(R.string.lib_word_detail_section_meaning),
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor
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
                            val levelColor = when (word.level.uppercase()) {
                                "N5" -> if (isDark) SystemColors.GreenDark else SystemColors.Green
                                "N4" -> if (isDark) SystemColors.CyanDark else SystemColors.Cyan
                                "N3" -> if (isDark) SystemColors.IndigoDark else SystemColors.Indigo
                                "N2" -> if (isDark) SystemColors.OrangeDark else SystemColors.Orange
                                "N1" -> if (isDark) SystemColors.RedDark else SystemColors.Red
                                else -> if (isDark) SystemColors.BlueDark else SystemColors.Blue
                            }
                            DetailTag(
                                text = word.level,
                                tagColor = levelColor
                            )

                            // POS Tag
                            word.pos?.let { pos ->
                                val posColor = if (isDark) SystemColors.IndigoDark else SystemColors.Indigo
                                DetailTag(
                                    text = pos,
                                    tagColor = posColor
                                )
                            }
                        }
                    }
                }

                // === 1.3 Related Words ===
                if (relatedWords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DetailSectionTitle(stringResource(R.string.lib_word_detail_section_related_words))
                    RelatedWordsSection(
                        relatedWords = relatedWords,
                        isDark = isDark,
                        onWordClick = onNavigateToWordDetail
                    )
                }

                // === 1.4 Example Sentences ===
                Spacer(modifier = Modifier.height(24.dp))
                DetailSectionTitle(stringResource(R.string.lib_word_detail_section_examples))

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
                            wordId = word.id,
                            accentColor = accentColor
                        )
                        if (index < examples.size) {
                             Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                } else {
                     Text(
                        text = stringResource(R.string.lib_word_detail_examples_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 0.dp)
                    )
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
                        nextReviewDay = word.nextReviewDate,
                        repetitionCount = word.repetitionCount,
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
                            contentDescription = stringResource(R.string.lib_word_detail_report_content_description),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
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
    borderRadius: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = borderRadius, isDark = isDark),
        shape = RoundedRectangle(borderRadius),
        color = containerColor,
        content = { Column(content = content) }
    )
}

/**
 * Detail Section Title (遵循标题与卡片对齐规范)
 */
@Composable
private fun DetailSectionTitle(
    text: String,
    modifier: Modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Detail Tag (双轨单源同色、单容器架构)
 */
@Composable
private fun DetailTag(
    text: String,
    tagColor: Color,
    height: androidx.compose.ui.unit.Dp = 28.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    textStyle: androidx.compose.ui.text.TextStyle? = null
) {
    val isSoftTranslucent = LocalSettingIconStyle.current == SettingIconStyle.SOFT_TRANSLUCENT
    val containerBg = if (isSoftTranslucent) tagColor.copy(alpha = 0.15f) else tagColor
    val contentColor = if (isSoftTranslucent) tagColor else Color.White

    Surface(
        shape = Capsule(),
        color = containerBg,
        modifier = Modifier.height(height)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        ) {
            Text(
                text = text,
                style = (textStyle ?: MaterialTheme.typography.labelMedium).copy(fontWeight = FontWeight.Bold),
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
    wordId: Int,
    accentColor: Color = MaterialTheme.colorScheme.primary
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
                color = accentColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                com.jian.nemo.core.ui.component.text.FuriganaText(
                    text = sentence,
                    baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp,
                        fontFamily = NotoSerifJP
                    ),
                    baseTextColor = MaterialTheme.colorScheme.onSurface,
                    furiganaTextColor = if (isDark) accentColor.copy(alpha = 0.8f) else accentColor.copy(alpha = 0.7f),
                    furiganaTextSize = 10.sp,
                    furiganaFontFamily = NotoSerifJP
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
                tint = accentColor
            )
        }
    }
}

@Composable
private fun RelatedWordsSection(
    relatedWords: List<com.jian.nemo.core.domain.model.RelatedWordItem>,
    isDark: Boolean,
    onWordClick: (Int) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(relatedWords.size) { index ->
            val item = relatedWords[index]
            RelatedWordCard(
                item = item,
                isDark = isDark,
                onClick = { onWordClick(item.relatedWord.id) }
            )
        }
    }
}

@Composable
private fun RelatedWordCard(
    item: com.jian.nemo.core.domain.model.RelatedWordItem,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val tagColor = if (isDark) SystemColors.TealDark else SystemColors.Teal

    PremiumDetailCard(
        isDark = isDark,
        borderRadius = 16.dp,
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            DetailTag(
                text = item.relation.relationType,
                tagColor = tagColor,
                height = 20.dp,
                horizontalPadding = 8.dp,
                textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Japanese
            Text(
                text = item.relatedWord.japanese,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = NotoSerifJP
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))
            
            // Hiragana (or placeholder to maintain height)
            if (item.relatedWord.hiragana.isNotEmpty() && item.relatedWord.hiragana != item.relatedWord.japanese) {
                Text(
                    text = item.relatedWord.hiragana,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = " ",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meaning
            Text(
                text = item.relatedWord.chinese,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
