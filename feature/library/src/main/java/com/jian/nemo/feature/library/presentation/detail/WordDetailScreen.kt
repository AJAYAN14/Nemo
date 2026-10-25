package com.jian.nemo.feature.library.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Report
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.srs.SrsStatusChip

import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
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

    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val accentColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else MaterialTheme.colorScheme.primary

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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Play Audio Button (Hero Style)
                    val wordAudioId = "word_${word.id}"
                    SpeakerButton(
                        isPlaying = playingAudioId == wordAudioId,
                        onClick = { onPlayAudio(word.japanese, wordAudioId) },
                        size = 56.dp,
                        backgroundColor = if (isPlumpDuo) accentColor.copy(alpha = if (isDark) 0.18f else 0.12f) else MaterialTheme.colorScheme.primaryContainer,
                        tint = if (isPlumpDuo) accentColor else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // === 1.2 Meaning & Tags ===
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
                            val levelColor = if (isPlumpDuo) {
                                when (word.level.uppercase()) {
                                    "N5" -> IsoluminantPalette.EmeraldGreen
                                    "N4" -> IsoluminantPalette.LakesideCyan
                                    "N3" -> IsoluminantPalette.SkyIndigo
                                    "N2" -> IsoluminantPalette.WarmAmber
                                    "N1" -> IsoluminantPalette.RoseRuby
                                    else -> IsoluminantPalette.WarmAmber
                                }
                            } else {
                                NemoPrimary
                            }
                            DetailTag(
                                text = word.level,
                                containerColor = if (isPlumpDuo) levelColor.copy(alpha = if (isDark) 0.18f else 0.12f) else NemoPrimary,
                                contentColor = if (isPlumpDuo) levelColor else Color.White
                            )

                            // POS Tag
                            word.pos?.let { pos ->
                                DetailTag(
                                    text = pos,
                                    containerColor = if (isPlumpDuo) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isPlumpDuo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === 1.3 Related Words ===
                if (relatedWords.isNotEmpty()) {
                    RelatedWordsSection(
                        relatedWords = relatedWords,
                        isDark = isDark,
                        isPlumpDuo = isPlumpDuo,
                        onWordClick = onNavigateToWordDetail
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // === 1.4 Example Sentences ===
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
                            wordId = word.id,
                            accentColor = accentColor
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
        shape = RoundedCornerShape(borderRadius),
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
    contentColor: Color,
    height: androidx.compose.ui.unit.Dp = 28.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    textStyle: androidx.compose.ui.text.TextStyle? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
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
                        fontWeight = FontWeight.Normal, // Changed from Medium to Normal
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
    isPlumpDuo: Boolean,
    onWordClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Title
        Text(
            text = "相关词",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 28.dp, bottom = 16.dp)
        )

        // Horizontal Carousel of Related Words
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(relatedWords.size) { index ->
                val item = relatedWords[index]
                RelatedWordCard(
                    item = item,
                    isDark = isDark,
                    isPlumpDuo = isPlumpDuo,
                    onClick = { onWordClick(item.relatedWord.id) }
                )
            }
        }
    }
}

@Composable
private fun RelatedWordCard(
    item: com.jian.nemo.core.domain.model.RelatedWordItem,
    isDark: Boolean,
    isPlumpDuo: Boolean,
    onClick: () -> Unit
) {
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
            // Relation Type Tag
            val tagContainerColor = if (isPlumpDuo) IsoluminantPalette.LakesideCyan.copy(alpha = if (isDark) 0.18f else 0.12f) else MaterialTheme.colorScheme.tertiaryContainer
            val tagContentColor = if (isPlumpDuo) IsoluminantPalette.LakesideCyan else MaterialTheme.colorScheme.onTertiaryContainer
            DetailTag(
                text = item.relation.relationType,
                containerColor = tagContainerColor,
                contentColor = tagContentColor,
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
