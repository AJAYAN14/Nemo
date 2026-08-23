package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningComprehensionScreen(
    onNavigateBack: () -> Unit,
    viewModel: ListeningComprehensionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    val containerColor = if (isDark) MaterialTheme.colorScheme.background else BentoColors.BgBase
    val wordCardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val qCardBg = if (isDark) IosColors.Indigo.copy(alpha = 0.15f) else Color(0xFFF3F2FF)
    val qCardBorderColor = if (isDark) IosColors.Indigo.copy(alpha = 0.2f) else IosColors.Indigo.copy(alpha = 0.1f)

    var showHistoryView by remember { mutableStateOf(false) }
    val historyRecords by viewModel.historyRecords.collectAsState(initial = emptyList())

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState is ListeningUiState.Ready) {
        showExitDialog = true
    }

    if (showHistoryView) {
        ListeningHistoryView(
            historyRecords = historyRecords,
            onBack = { showHistoryView = false },
            isDark = isDark
        )
        return
    }

    com.jian.nemo.core.ui.component.common.NemoScaffold(
        title = "听力挑战",
        onBack = {
            if (uiState is ListeningUiState.Ready) {
                showExitDialog = true
            } else {
                onNavigateBack()
            }
        },
        actions = {
            IconButton(onClick = { showHistoryView = true }) {
                Icon(Icons.Rounded.History, contentDescription = "历史记录", tint = textMain)
            }
        },
        backgroundColor = containerColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Crossfade(targetState = uiState, label = "state_transition") { state ->
                when (state) {
                    is ListeningUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IosColors.Indigo)
                        }
                    }
                    is ListeningUiState.LevelSelecting -> {
                        LevelSelectionView(onSelectLevel = { viewModel.onLevelSelected(it) })
                    }
                    is ListeningUiState.Ready -> {
                        ReadyView(
                            state = state,
                            viewModel = viewModel,
                            isDark = isDark,
                            textMain = textMain,
                            textSub = textSub,
                            wordCardBg = wordCardBg,
                            qCardBg = qCardBg,
                            qCardBorderColor = qCardBorderColor,
                            onRegenerate = { viewModel.forceRegenerate() }
                        )
                    }
                    is ListeningUiState.Finished -> {
                        ResultView(
                            correctCount = state.correctCount,
                            totalCount = state.totalCount,
                            onRestart = { viewModel.restart() },
                            onRegenerate = { viewModel.forceRegenerate() },
                            onBack = onNavigateBack
                        )
                    }
                    is ListeningUiState.Error -> {
                        ErrorView(message = state.message, onRetry = { viewModel.restart() })
                    }
                }
            }
        }
    }

    AbilityExitDialog(
        show = showExitDialog,
        title = "退出听力挑战",
        message = "是否保留当前答题进度？保留后可下次继续作答，清空则删除当前进度。",
        themeColor = IosColors.Indigo,
        isDark = isDark,
        textMain = textMain,
        textSub = textSub,
        onDismiss = { showExitDialog = false },
        onKeepAndExit = {
            showExitDialog = false
            onNavigateBack()
        },
        onDestroyAndExit = {
            showExitDialog = false
            viewModel.clearSession()
            onNavigateBack()
        }
    )
}

@Composable
private fun ListeningHistoryView(
    historyRecords: List<TestRecordEntity>,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val containerColor = if (isDark) MaterialTheme.colorScheme.background else BentoColors.BgBase

    Scaffold(
        topBar = {
            CommonHeader(
                title = "听力挑战记录",
                onBack = onBack
            )
        },
        containerColor = containerColor
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            if (historyRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无挑战记录", color = NemoNeutrals.Gray500)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(historyRecords) { record ->
                        val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(record.date))
                        val acc = if (record.totalQuestions > 0) record.correctAnswers * 100 / record.totalQuestions else 0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("本地听辨强化训练", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = NemoNeutrals.Gray500)
                            }
                            Text("$acc%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (acc >= 80) IosColors.Green else if (acc >= 60) IosColors.Yellow else IosColors.Red)
                        }
                        HorizontalDivider(color = if(isDark) MaterialTheme.colorScheme.outlineVariant else NemoNeutrals.Gray200.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelSelectionView(onSelectLevel: (String) -> Unit) {
    val levels = listOf(
        Triple("N5", "初级基础词汇听辨", Color(0xFF10B981)),
        Triple("N4", "常用社交词汇听辨", Color(0xFF3B82F6)),
        Triple("N3", "中级日常词汇听辨", Color(0xFFF59E0B)),
        Triple("N2", "中高级书面词汇听辨", Color(0xFFF97316)),
        Triple("N1", "高级专业词汇听辨", Color(0xFFEF4444))
    )
    
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("选择听力挑战等级", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("声波频段已锁定，系统将基于本地词库出题", style = MaterialTheme.typography.bodyMedium, color = NemoNeutrals.Gray500)
        Spacer(modifier = Modifier.height(32.dp))
        levels.forEach { (level, desc, color) ->
            AbilityLevelCard(
                level = level,
                description = desc,
                color = color,
                isDark = isDark,
                textMain = textMain,
                textSub = textSub,
                onClick = { onSelectLevel(level) }
            )
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
        Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = IosColors.Red)
        Spacer(modifier = Modifier.height(16.dp))
        Text("出题失败", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = NemoNeutrals.Gray500)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = IosColors.Indigo)) {
            Text("返回")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyView(
    state: ListeningUiState.Ready,
    viewModel: ListeningComprehensionViewModel,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    wordCardBg: Color,
    qCardBg: Color,
    qCardBorderColor: Color,
    onRegenerate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.isAnswered) {
        if (state.isAnswered) {
            isSheetOpen = true
        }
    }

    val currentQuestion = state.questions[state.currentIndex]

    // TTS 播放动画状态 (利用模拟声波)
    var isPlaying by remember { mutableStateOf(false) }
    var playTrigger by remember { mutableStateOf(0) }

    // 当切换题目时，自动朗读一次 (良好体验)，且重置播放动画
    LaunchedEffect(state.currentIndex) {
        isPlaying = true
        viewModel.playTts(1.0f)
        kotlinx.coroutines.delay(1200)
        isPlaying = false
    }

    // 点击播放逻辑
    fun triggerPlay(speed: Float) {
        isPlaying = true
        playTrigger++
        viewModel.playTts(speed)
    }

    // 监听手动播放完毕动画重置
    LaunchedEffect(playTrigger) {
        if (playTrigger > 0) {
            kotlinx.coroutines.delay(1200)
            isPlaying = false
        }
    }

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
                color = IosColors.Indigo,
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
                Icon(Icons.Rounded.Refresh, contentDescription = "换一批", tint = textSub)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 声波拟真控制台 (核心听音盘)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 24.dp, isDark = isDark),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = wordCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (currentQuestion.questionType == "IDENTIFY_WORD") "听录音辨析日语单词" else "听录音辨析中文释义",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = IosColors.Indigo
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                // 纯 Compose 绘制的 5 频段跳动声波线
                Row(
                    modifier = Modifier.height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "wave")
                    
                    // 定义 5 个不同步的随机高度系数
                    val heights = listOf(0.4f, 0.9f, 0.6f, 0.8f, 0.5f)
                    
                    heights.forEachIndexed { idx, factor ->
                        val duration = remember { (600..900).random() }
                        val scaleY by infiniteTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = factor,
                            animationSpec = infiniteRepeatable(
                                animation = tween(duration, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar_$idx"
                        )
                        
                        // 只有在isPlaying状态下，声波才激烈跳动，否则呈现平静涟漪
                        val activeScale = if (isPlaying) scaleY else 0.15f
                        
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(activeScale)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(IosColors.Indigo, IosColors.Teal)
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 控制按钮区（圆形播放按钮与文字标签双层绝对轴对齐布局）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 圆形播放按钮层 (只含圆形按钮，绝对物理中轴对齐)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 慢速 0.75x
                        IconButton(
                            onClick = { triggerPlay(0.75f) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(IosColors.Teal.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.SlowMotionVideo, contentDescription = "慢速播放", tint = IosColors.Teal)
                        }

                        // 主播放按钮 (中间大的是标准速 1.0x)
                        val scaleBtn by animateFloatAsState(targetValue = if (isPlaying) 0.9f else 1.0f, label = "btn_scale")
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .border(4.dp, IosColors.Indigo.copy(alpha = 0.15f), CircleShape)
                                .clip(CircleShape)
                                .clickable { triggerPlay(1.0f) }
                                .background(IosColors.Indigo)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // 快速 1.25x
                        IconButton(
                            onClick = { triggerPlay(1.25f) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(IosColors.Indigo.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.FastForward, contentDescription = "快速播放", tint = IosColors.Indigo)
                        }
                    }

                    // 2. 文字标签层 (通过匹配宽度实现横向完美挂接)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                            Text("0.75x 慢速", fontSize = 10.sp, color = textSub, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Box(modifier = Modifier.width(76.dp), contentAlignment = Alignment.Center) {
                            Text("1.0x 标准", fontSize = 10.sp, color = IosColors.Indigo, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                            Text("1.25x 快速", fontSize = 10.sp, color = textSub, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = state.currentIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                    )
                }.using(SizeTransform(clip = false))
            },
            label = "question_transition",
            modifier = Modifier.fillMaxWidth()
        ) { targetIndex ->
            val question = state.questions[targetIndex]
            Column {
                // 3. 题目专注防作弊卡片 (作弊锁)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = qCardBg),
                    border = BorderStroke(1.dp, qCardBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = IosColors.Indigo.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "听力内容已锁定",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textMain
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "请仔细聆听上方的纯正录音，并在下方选出最匹配的选项。答题后将完整解密词汇拼写与释义。",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSub,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. 2x2 极速选项卡片
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val rows = question.options.chunked(2)
                    rows.forEachIndexed { rowIndex, optionsRow ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            optionsRow.forEachIndexed { colIndex, option ->
                                val index = rowIndex * 2 + colIndex
                                OptionButton(
                                    text = option,
                                    isSelected = state.selectedOptionIndex == index,
                                    isCorrect = index == question.correctIndex,
                                    isAnswered = state.isAnswered,
                                    onClick = { viewModel.selectOption(index) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (optionsRow.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 5. 底部控制栏
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
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
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
                Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("上一题", fontWeight = FontWeight.Bold)
            }

            val isLast = state.currentIndex == state.questions.size - 1
            val isAnswered = state.isAnswered

            Button(
                onClick = { viewModel.nextQuestion() },
                enabled = isAnswered,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLast) IosColors.Green else IosColors.Indigo,
                    disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else NemoNeutrals.Gray200
                )
            ) {
                Text(
                    text = if (isLast) "完成练习" else "下一题",
                    fontWeight = FontWeight.Bold,
                    color = if (isAnswered) Color.White else NemoNeutrals.Gray500
                )
                if (!isLast) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = if (isAnswered) Color.White else NemoNeutrals.Gray500
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    // 6. 底栏 ModalSheet 滑动解析 (解锁模式)
    if (isSheetOpen) {
        val isCorrect = state.selectedOptionIndex == currentQuestion.correctIndex
        NemoModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    isSheetOpen = false
                    viewModel.nextQuestion()
                }
            },
            sheetState = sheetState,
            containerColor = if (isCorrect) {
                if (isDark) Color(0xFF14532D).copy(alpha = 0.9f) else Color(0xFFF0FDF4)
            } else {
                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.9f) else Color(0xFFFEF2F2)
            },
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        contentDescription = null,
                        tint = if (isCorrect) {
                            if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                        } else {
                            if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCorrect) "回答正确！解锁发音卡" else "回答错误，解锁发音卡",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) {
                            if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                        } else {
                            if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 实时清洗过滤掉任何可能残留的 emoji 字符，提供绝对安全的视觉呈现！
                val cleanExplanation = remember(currentQuestion.explanation) {
                    currentQuestion.explanation.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2700-\\u27BF\\u2600-\\u26FF]+"), "")
                        .replace("🔊 ", "")
                        .replace("📝 ", "")
                        .replace("📖 ", "")
                        .replace("🏷️ ", "")
                        .replace("💡 ", "")
                        .trim()
                }
                
                // 解析文字排版
                Text(
                    text = cleanExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCorrect) {
                        if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    } else {
                        if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                    },
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isSheetOpen = false
                            viewModel.nextQuestion()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCorrect) IosColors.Green else IosColors.Red
                    )
                ) {
                    Text(text = "继续", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        isAnswered && isCorrect -> IosColors.Green
        isSelected && !isCorrect -> IosColors.Red
        isDark -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    val bgColor = when {
        isAnswered && isCorrect -> IosColors.Green.copy(alpha = 0.05f)
        isSelected && !isCorrect -> IosColors.Red.copy(alpha = 0.05f)
        isDark -> MaterialTheme.colorScheme.surface
        else -> Color.White
    }
    val textColor = when {
        isAnswered && !isSelected && !isCorrect -> if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else NemoNeutrals.Gray400
        else -> if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    }

    Surface(
        modifier = modifier
            .height(72.dp)
            .softCardShadow(borderRadius = 18.dp, isDark = isDark)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !isAnswered) { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(18.dp)),
        color = bgColor,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
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
        accuracy >= 80 -> IosColors.Green
        accuracy >= 60 -> IosColors.Yellow
        else -> IosColors.Red
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(130.dp),
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
                Text("正确率", style = MaterialTheme.typography.bodySmall, color = textColor)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("听力练习完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("本次通关：答对 $correctCount 题 / 共 $totalCount 题", style = MaterialTheme.typography.bodyLarge, color = textColor)
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onRegenerate,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IosColors.Indigo)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("换一批单词重新听", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("返回等级选择", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) {
            Text("离开工坊", color = textColor)
        }
    }
}
