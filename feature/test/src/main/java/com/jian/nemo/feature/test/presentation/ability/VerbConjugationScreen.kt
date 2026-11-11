package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.shape.UnevenRoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.NemoDangerDark
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoPrimaryDark
import com.jian.nemo.core.designsystem.theme.NemoSecondary
import com.jian.nemo.core.designsystem.theme.NemoSecondaryDark
import com.jian.nemo.core.designsystem.theme.NemoWarning
import com.jian.nemo.core.designsystem.theme.NemoWarningDark
import com.jian.nemo.core.designsystem.theme.TestResultPalette
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import androidx.compose.ui.graphics.Brush
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet
import com.jian.nemo.core.ui.component.text.FuriganaText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.airbnb.lottie.compose.*
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbConjugationScreen(
    onNavigateBack: () -> Unit,
    viewModel: VerbConjugationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    val containerColor = MaterialTheme.colorScheme.background
    val wordCardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val qCardBg = if (isDark) SystemColors.Blue.copy(alpha = 0.15f) else SystemColors.Blue.copy(alpha = 0.08f)
    val qCardBorderColor = if (isDark) SystemColors.Blue.copy(alpha = 0.2f) else SystemColors.Blue.copy(alpha = 0.1f)

    var showHistoryView by remember { mutableStateOf(false) }
    val historyRecords by viewModel.historyRecords.collectAsState(initial = emptyList())

    if (showHistoryView) {
        HistoryView(
            historyRecords = historyRecords,
            onBack = { showHistoryView = false },
            isDark = isDark
        )
        return
    }

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 16.dp
                )
        ) {
            Crossfade(targetState = uiState, label = "state_transition") { state ->
                when (state) {
                    is VerbUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SystemColors.Blue)
                        }
                    }
                    is VerbUiState.ApiNotConfigured -> {
                        ApiNotConfiguredView(onBack = onNavigateBack, isDark = isDark)
                    }
                    is VerbUiState.LevelSelecting -> {
                        LevelSelectionView(onSelectLevel = { viewModel.onLevelSelected(it) })
                    }
                    is VerbUiState.Generating -> {
                        GeneratingView(
                            isDark = isDark,
                            onCancel = { viewModel.cancelGeneration() },
                            onRetry = { viewModel.regenerateCurrentLevel() }
                        )
                    }
                    is VerbUiState.Ready -> {
                        ReadyView(
                            state = state,
                            viewModel = viewModel,
                            isDark = isDark,
                            textMain = textMain,
                            textSub = textSub,
                            containerColor = containerColor,
                            wordCardBg = wordCardBg,
                            qCardBg = qCardBg,
                            qCardBorderColor = qCardBorderColor,
                            onRegenerate = { viewModel.forceRegenerate() }
                        )
                    }
                    is VerbUiState.Finished -> {
                        ResultView(
                            correctCount = state.correctCount,
                            totalCount = state.totalCount,
                            onRestart = { viewModel.restart() },
                            onRegenerate = { viewModel.forceRegenerate() },
                            onBack = onNavigateBack
                        )
                    }
                    is VerbUiState.Error -> {
                        ErrorView(message = state.message, onRetry = { viewModel.restart() })
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.95f),
                            containerColor.copy(alpha = 0.75f),
                            containerColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.test_verb_header_title),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    FrostedLiquidGlassButton(
                        onClick = { showHistoryView = true },
                        backdrop = backdrop,
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        isInteractive = true,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.test_verb_cd_history),
                            tint = textMain,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryView(
    historyRecords: List<TestRecordEntity>,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val containerColor = MaterialTheme.colorScheme.background
    
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        if (historyRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop + 60.dp + 16.dp, bottom = navBarBottom + 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.test_verb_empty_history), color = NemoNeutrals.Gray500)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                )
            ) {
                items(historyRecords) { record ->
                    val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(record.date))
                    val acc = if (record.totalQuestions > 0) record.correctAnswers * 100 / record.totalQuestions else 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.test_verb_subtitle), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = textColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = NemoNeutrals.Gray500)
                        }
                        Text("$acc%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (acc >= 80) SystemColors.Green else if (acc >= 60) SystemColors.Yellow else SystemColors.Red)
                    }
                    HorizontalDivider(color = if(isDark) MaterialTheme.colorScheme.outlineVariant else NemoNeutrals.Gray200.copy(alpha = 0.5f))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.95f),
                            containerColor.copy(alpha = 0.75f),
                            containerColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.test_verb_title_history),
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }
    }
}

@Composable
private fun ApiNotConfiguredView(onBack: () -> Unit, isDark: Boolean) {
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(84.dp).background(SystemColors.Yellow.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(56.dp), tint = SystemColors.Yellow)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.test_verb_title_no_ai), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.test_verb_desc_no_ai),
            style = MaterialTheme.typography.bodyLarge, color = textColor, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            shape = RoundedRectangle(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.Blue)
        ) {
            Text(stringResource(R.string.test_verb_btn_back_to_config), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LevelSelectionView(onSelectLevel: (String) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val levels = listOf(
        Triple("N5", "初级基础", if (isDark) NemoSecondaryDark else NemoSecondary),
        Triple("N4", "初级进阶", if (isDark) NemoPrimaryDark else NemoPrimary),
        Triple("N3", "中级应用", if (isDark) NemoWarningDark else NemoWarning),
        Triple("N2", "中高级进阶", if (isDark) NemoWarningDark else NemoWarning),
        Triple("N1", "高级熟练", if (isDark) NemoDangerDark else NemoDanger)
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.test_verb_title_select_level), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.test_verb_desc_select_level), style = MaterialTheme.typography.bodyMedium, color = NemoNeutrals.Gray500)
        Spacer(modifier = Modifier.height(32.dp))
        levels.forEach { (level, desc, color) ->
            Button(
                onClick = { onSelectLevel(level) },
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 6.dp),
                shape = RoundedRectangle(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.1f), contentColor = color)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(level, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(desc, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun GeneratingView(isDark: Boolean, onCancel: () -> Unit, onRetry: () -> Unit) {
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    
    val loadingTexts = remember {
        listOf(
            "AI 正在脑洞大开地为您现编题目，请稍候...",
            "正在翻找日语语法秘笈... 马上出炉！",
            "大语言模型正在抓耳挠腮中，不要走开哦...",
            "正在为您量身定制动词活用挑战...",
            "正在抓取词库中的最强动词... 马上就好！"
        )
    }
    var textIndex by remember { mutableIntStateOf((0..loadingTexts.lastIndex).random()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2500)
            textIndex = (textIndex + 1) % loadingTexts.size
        }
    }

    val loadingText = loadingTexts[textIndex]

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_ai_thinking))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(240.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = loadingText,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 放弃生成：辅助操作，使用 OutlinedButton 退回等级选择
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedRectangle(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) NemoNeutrals.Gray400 else NemoNeutrals.Gray600
                ),
                border = BorderStroke(1.dp, if (isDark) NemoNeutrals.Gray700 else NemoNeutrals.Gray300),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.test_verb_btn_abort_generate), maxLines = 1)
            }

            // 重新生成：核心操作，使用主色调 Button 终止并重试当前等级
            Button(
                onClick = onRetry,
                shape = RoundedRectangle(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.Blue,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.test_verb_btn_regenerate), maxLines = 1)
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = SystemColors.Red)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.test_verb_title_generate_failed), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = NemoNeutrals.Gray500)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SystemColors.Blue)) {
            Text(stringResource(R.string.test_common_btn_retry))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyView(
    state: VerbUiState.Ready,
    viewModel: VerbConjugationViewModel,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    containerColor: Color,
    wordCardBg: Color,
    qCardBg: Color,
    qCardBorderColor: Color,
    onRegenerate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.isAnswered) {
        showSheet = state.isAnswered
    }

    val currentQuestion = state.questions[state.currentIndex]

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        // 1. 进度条与题号
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { (state.currentIndex + 1).toFloat() / state.questions.size },
                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                color = SystemColors.Blue,
                trackColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else NemoNeutrals.Gray200
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${state.currentIndex + 1}/${state.questions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = textSub
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.test_verb_cd_refresh_batch), tint = textSub)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 单词卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 24.dp, isDark = isDark),
            shape = RoundedRectangle(24.dp),
            colors = CardDefaults.cardColors(containerColor = wordCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val wordAnnotatedString = remember(currentQuestion.word, currentQuestion.furigana) {
                        androidx.compose.ui.text.buildAnnotatedString {
                            append(currentQuestion.word)
                            withStyle(style = androidx.compose.ui.text.SpanStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = textSub
                            )) {
                                append("（${currentQuestion.furigana}）")
                            }
                        }
                    }
                    Text(text = wordAnnotatedString, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = textMain)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = currentQuestion.meaning, style = MaterialTheme.typography.bodyMedium, color = textSub)
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { viewModel.playWordTts() },
                    modifier = Modifier.size(36.dp).background(SystemColors.Blue.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(R.string.test_verb_cd_play_audio), tint = SystemColors.Blue, modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 题目卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedRectangle(24.dp),
            colors = CardDefaults.cardColors(containerColor = qCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, qCardBorderColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                FuriganaText(
                    text = currentQuestion.qText,
                    baseTextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.sp, lineHeight = 32.sp),
                    baseTextColor = textMain
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).background(SystemColors.Blue.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.test_verb_badge_text), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SystemColors.Blue)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = currentQuestion.translation, style = MaterialTheme.typography.bodySmall, color = textSub)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. 选项区域 (2x2 网格)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val rows = currentQuestion.options.chunked(2)
            rows.forEachIndexed { rowIndex, optionsRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    optionsRow.forEachIndexed { colIndex, option ->
                        val index = rowIndex * 2 + colIndex
                        OptionButton(
                            text = option,
                            isSelected = state.selectedOptionIndex == index,
                            isCorrect = index == currentQuestion.correctIndex,
                            isAnswered = state.isAnswered,
                            onClick = { viewModel.selectOption(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (optionsRow.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isFirst = state.currentIndex == 0
            OutlinedButton(
                onClick = { viewModel.previousQuestion() },
                enabled = !isFirst,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedRectangle(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFirst) {
                        Color.Transparent
                    } else if (isDark) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    } else {
                        NemoNeutrals.Gray300
                    }
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = textMain,
                    disabledContentColor = NemoNeutrals.Gray400
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.test_common_btn_prev_question), fontWeight = FontWeight.Bold)
            }

            val isLast = state.currentIndex == state.questions.size - 1
            val isAnswered = state.isAnswered
            
            Button(
                onClick = { viewModel.nextQuestion() },
                enabled = isAnswered,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedRectangle(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLast) SystemColors.Green else SystemColors.Blue,
                    disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else NemoNeutrals.Gray200
                )
            ) {
                Text(
                    text = if (isLast) stringResource(R.string.test_verb_btn_finish) else stringResource(R.string.test_verb_btn_next),
                    fontWeight = FontWeight.Bold,
                    color = if (isAnswered) Color.White else NemoNeutrals.Gray500
                )
                if (!isLast) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = if (isAnswered) Color.White else NemoNeutrals.Gray500
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    // 反馈底栏
    if (showSheet) {
        val isCorrect = state.selectedOptionIndex == currentQuestion.correctIndex
        NemoModalBottomSheet(
            onDismissRequest = { /* 必须点击继续 */ },
            sheetState = sheetState,
            containerColor = if (isCorrect) {
                if (isDark) TestResultPalette.CorrectCardBgDark else TestResultPalette.CorrectCardBgLight
            } else {
                if (isDark) TestResultPalette.WrongCardBgDark else TestResultPalette.WrongCardBgLight
            },
            dragHandle = null,
            shape = UnevenRoundedRectangle(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                Text(
                    text = if (isCorrect) "✓ 正确答案：${currentQuestion.options[currentQuestion.correctIndex]}" 
                           else "✗ 正确答案：${currentQuestion.options[currentQuestion.correctIndex]}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) {
                        if (isDark) NemoSecondaryDark else NemoSecondary
                    } else {
                        if (isDark) NemoDangerDark else NemoDanger
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentQuestion.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCorrect) {
                        if (isDark) TestResultPalette.CorrectCardContentDark else TestResultPalette.CorrectCardContentLight
                    } else {
                        if (isDark) TestResultPalette.WrongCardContentDark else TestResultPalette.WrongCardContentLight
                    },
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedRectangle(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCorrect) {
                            if (isDark) SystemColors.GreenDark else SystemColors.Green
                        } else {
                            if (isDark) SystemColors.RedDark else SystemColors.Red
                        }
                    )
                ) {
                    Text(text = stringResource(R.string.test_verb_btn_continue), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = when {
        isAnswered && isCorrect -> SystemColors.Green
        isSelected && !isCorrect -> SystemColors.Red
        isDark -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    val bgColor = when {
        isAnswered && isCorrect -> SystemColors.Green.copy(alpha = 0.05f)
        isSelected && !isCorrect -> SystemColors.Red.copy(alpha = 0.05f)
        isDark -> MaterialTheme.colorScheme.surface
        else -> Color.White
    }
    val textColor = when {
        isAnswered && !isSelected && !isCorrect -> if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else NemoNeutrals.Gray400
        else -> if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    }

    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedRectangle(16.dp))
            .clickable(enabled = !isAnswered) { onClick() }
            .border(2.dp, borderColor, RoundedRectangle(16.dp)),
        color = bgColor,
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun ResultView(
    correctCount: Int,
    totalCount: Int,
    onRestart: () -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    val accuracy = if (totalCount > 0) (correctCount.toFloat() / totalCount * 100).toInt() else 0
    val ringColor = when {
        accuracy >= 80 -> SystemColors.Green
        accuracy >= 60 -> SystemColors.Yellow
        else -> SystemColors.Red
    }
    
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { accuracy / 100f },
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                trackColor = ringColor.copy(alpha = 0.1f),
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$accuracy%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ringColor)
                Text(stringResource(R.string.test_common_label_accuracy), style = MaterialTheme.typography.bodySmall, color = textColor)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(stringResource(R.string.test_verb_title_completed), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.test_verb_format_result, correctCount, totalCount), style = MaterialTheme.typography.bodyLarge, color = textColor)
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onRegenerate,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = RoundedRectangle(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.Blue)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.test_verb_btn_new_batch), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = RoundedRectangle(28.dp)
        ) {
            Text(stringResource(R.string.test_verb_btn_back_to_levels), fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.test_common_btn_exit_workshop), color = textColor)
        }
    }
}
