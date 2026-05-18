package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.AIGradeResult
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.core.designsystem.R as DesignR
import com.airbnb.lottie.compose.*
import com.jian.nemo.feature.learning.R



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWorkshopScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AIWorkshopViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showHelp by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_ai_thinking))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    val loadingTexts = remember {
        listOf(
            "AI 正在呼唤日语音符，为您纺织专属的例文故事...",
            "AI 正在细心琢磨，想为您呈上一句最动心的日语翻译...",
            "字里行间皆是心意，例文正在精心雕琢中...",
            "正在为您检索最地道的表达，让日语练习更有温度...",
            "思维的火花正在闪烁，只为与您的下一次日语邂逅作准备...",
            "请稍候哦，AI 正在为您手写带有樱花香气的练习题...",
            "正在铺展纸笔，为您绘制一行闪闪发光的日语例文..."
        )
    }
    var loadingTextIndex by remember { mutableIntStateOf((0..loadingTexts.lastIndex).random()) }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            while (true) {
                kotlinx.coroutines.delay(2500)
                loadingTextIndex = (loadingTextIndex + 1) % loadingTexts.size
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    
    // 语义化颜色映射
    val backgroundColor = colorScheme.background
    val textPrimary = colorScheme.onSurface
    val haptic = LocalHapticFeedback.current

    // 监听错误，触发震动
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CommonHeader(
                    title = "AI 例文工坊",
                    onBack = onNavigateBack,
                    backgroundColor = backgroundColor,
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(AIWorkshopEvent.QuickSwitchPlatform) }) {
                            val platform = uiState.aiPlatform
                            when (platform) {
                                "gemini" -> Icon(painterResource(DesignR.drawable.ic_gemini), contentDescription = "Gemini", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                                "deepseek" -> Icon(painterResource(DesignR.drawable.ic_deepseek), contentDescription = "DeepSeek", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                                "openai" -> Icon(painterResource(DesignR.drawable.ic_openai), contentDescription = "OpenAI", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                                else -> Icon(Icons.Rounded.Memory, contentDescription = "Custom", tint = textPrimary)
                            }
                        }
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Rounded.History, contentDescription = "历史记录", tint = textPrimary)
                        }
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "帮助", tint = textPrimary)
                        }
                    }
                )
            },
            containerColor = backgroundColor
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!uiState.isConfigured) {
                    ConfigRequiredView(onNavigateToSettings = onNavigateToSettings)
                } else {
                    // 核心操作区
                    WorkshopContent(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 加载层 - 采用全屏磨砂质感（Flat）并使用唯美的 Lottie 加载动效
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        if (composition != null) {
                            LottieAnimation(
                                composition = composition,
                                progress = { lottieProgress },
                                modifier = Modifier.size(240.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NemoChasingDotsLoader(size = 48.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = loadingTexts[loadingTextIndex],
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    uiState.error?.let {
            NemoSnackbar(
                visible = true,
                message = "操作失败，请重试",
                type = NemoSnackbarType.ERROR,
                icon = Icons.Rounded.ErrorOutline,
                onDismiss = { viewModel.onEvent(AIWorkshopEvent.ClearError) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarHeight + 8.dp)
            )
        }

        // 帮助弹窗
        if (showHelp) {
            HelpDialog(onDismiss = { showHelp = false })
        }
    }
}

@Composable
private fun WorkshopContent(
    uiState: AIWorkshopUiState,
    onEvent: (AIWorkshopEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. 模式切换面板（Apple Style 胶囊）
        ModeSection(
            currentMode = uiState.workshopMode,
            onModeChange = { onEvent(AIWorkshopEvent.UpdateWorkshopMode(it)) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 难度切换
        DifficultySection(
            currentLevel = uiState.difficulty,
            onLevelChange = { onEvent(AIWorkshopEvent.UpdateDifficulty(it)) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 主视图切换
        AnimatedContent(
            targetState = uiState.currentExercise == null,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ViewSwitch"
        ) { isStartView ->
            if (isStartView) {
                // 语法专项模式下，如果无数据则显示兜底视图
                if (uiState.workshopMode == WorkshopMode.GRAMMAR && !uiState.hasGrammarData) {
                    GrammarDataEmptyView()
                } else {
                    HeroStartView(
                        onStart = { onEvent(AIWorkshopEvent.GenerateNewExercise) },
                        isGrammarMode = uiState.workshopMode == WorkshopMode.GRAMMAR
                    )
                }
            } else {
                ExerciseMainView(
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}

/**
 * 模式切换：紧凑型 Apple Style 胶囊控件（无卡片外壳）
 */
@Composable
private fun ModeSection(
    currentMode: WorkshopMode,
    onModeChange: (WorkshopMode) -> Unit,
    enabled: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val secondarySurface = if (isDark) colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50
    val modes = listOf(WorkshopMode.FREE, WorkshopMode.GRAMMAR)
    val modeLabels = listOf("自由模式", "语法专项")
    val selectedIndex = modes.indexOf(currentMode)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(secondarySurface, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        // Sliding Capsule - 使用 weight 布局实现，无需 BoxWithConstraints
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "modeCapsuleIndex"
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(animatedIndex.coerceAtLeast(0.01f)))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isDark) colorScheme.surfaceContainerHighest else Color.White, RoundedCornerShape(10.dp))
                    .border(0.5.dp, if (isDark) colorScheme.outlineVariant.copy(0.2f) else NemoNeutrals.Gray200.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.weight((modes.size - 1 - animatedIndex).coerceAtLeast(0.01f)))
        }

        // Interactive Labels

        // Interactive Labels
        Row(modifier = Modifier.fillMaxSize()) {
            modes.forEachIndexed { index, mode ->
                val isSelected = mode == currentMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onModeChange(mode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeLabels[index],
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) colorScheme.primary else NemoNeutrals.Gray500
                    )
                }
            }
        }
    }
}

/**
 * 语法数据为空时的兜底视图
 */
@Composable
private fun GrammarDataEmptyView() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f) else NemoNeutrals.Gray400
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "本地暂无该等级语法数据",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "请先返回主页同步数据\n或切换到「自由模式」进行练习",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun DifficultySection(
    currentLevel: String,
    onLevelChange: (String) -> Unit,
    enabled: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val secondarySurface = if (isDark) colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50
    val levels = listOf("N5", "N4", "N3", "N2", "N1")
    val selectedIndex = levels.indexOf(currentLevel)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(secondarySurface, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        // Sliding Capsule - 使用 weight 布局实现
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "difficultyCapsuleIndex"
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(animatedIndex.coerceAtLeast(0.01f)))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isDark) colorScheme.surfaceContainerHighest else Color.White, RoundedCornerShape(10.dp))
                    .border(0.5.dp, if (isDark) colorScheme.outlineVariant.copy(0.2f) else NemoNeutrals.Gray200.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.weight((levels.size - 1 - animatedIndex).coerceAtLeast(0.01f)))
        }
        
        // Interactive Labels
        Row(modifier = Modifier.fillMaxSize()) {
            levels.forEach { level ->
                val isSelected = level == currentLevel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onLevelChange(level)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) colorScheme.primary else NemoNeutrals.Gray500
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStartView(onStart: () -> Unit, isGrammarMode: Boolean = false) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val textPrimary = colorScheme.onSurface
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                color = colorScheme.primary.copy(alpha = 0.05f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isGrammarMode) Icons.AutoMirrored.Rounded.MenuBook else Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                if (isGrammarMode) "语法专项训练" else "开启智能练习",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                if (isGrammarMode)
                    "AI 将从本地语法库中随机抽取\n一个语法点为您生成针对性练习"
                else
                    "AI 将根据您的日语等级\n为您实时生成专属的翻译例文",
                style = MaterialTheme.typography.bodyMedium,
                color = NemoNeutrals.Gray500,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                elevation = null
            ) {
                Text("开始练习", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ExerciseMainView(
    uiState: AIWorkshopUiState,
    onEvent: (AIWorkshopEvent) -> Unit
) {
    ExerciseContent(uiState, onEvent)
}

@Composable
private fun ExerciseContent(
    uiState: AIWorkshopUiState,
    onEvent: (AIWorkshopEvent) -> Unit
) {
    val exercise = uiState.currentExercise ?: return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    Column(modifier = Modifier.fillMaxWidth()) {
        // 题目面板
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NemoSecondary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (exercise.type == "CN_TO_JP") "中翻日" else "日翻中",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NemoSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // 语法专项模式标签
                    uiState.currentGrammarPoint?.let { grammarPoint ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            val label = buildString {
                                append(grammarPoint)
                                uiState.currentGrammarSubtype?.let { subtype ->
                                    if (subtype.isNotBlank()) append(" · $subtype")
                                }
                            }
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onEvent(AIWorkshopEvent.GenerateNewExercise) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "换一题", tint = NemoNeutrals.Gray400)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.question,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 34.sp
                    )

                    if (exercise.type == "JP_TO_CN") {
                        Spacer(modifier = Modifier.width(8.dp))
                        SpeakerButton(
                            isPlaying = uiState.playingAudioId == "question",
                            onClick = { onEvent(AIWorkshopEvent.SpeakText(exercise.question, "question")) },
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            size = 40.dp
                        )
                    }
                }

                
                if (exercise.hints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFBBF24)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "提示: ${exercise.hints.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 输入与评分区
        if (uiState.gradeResult == null) {
            InputSection(
                answer = uiState.userAnswer,
                onAnswerChange = { onEvent(AIWorkshopEvent.UpdateUserAnswer(it)) },
                onSubmit = { onEvent(AIWorkshopEvent.SubmitAnswer) },
                enabled = !uiState.isLoading
            )
        } else {
            FeedbackSection(
                result = uiState.gradeResult,
                userAnswer = uiState.userAnswer,
                exerciseType = exercise.type,
                playingAudioId = uiState.playingAudioId,
                onSpeak = { text, id -> onEvent(AIWorkshopEvent.SpeakText(text, id)) },
                onNext = { onEvent(AIWorkshopEvent.GenerateNewExercise) }
            )

        }
    }
}

@Composable
private fun InputSection(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Column {
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            placeholder = { 
                Text(
                    "在这里输入您的翻译答案...",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.4f),
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor,
                disabledContainerColor = surfaceColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            enabled = enabled
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray200,
                disabledContentColor = if (isDark) Color.White.copy(alpha = 0.3f) else NemoNeutrals.Gray400
            ),
            enabled = answer.isNotBlank() && enabled,
            elevation = null
        ) {
            Text("提交 AI 评分", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedbackSection(
    result: AIGradeResult,
    userAnswer: String,
    exerciseType: String,
    playingAudioId: String?,
    onSpeak: (String, String) -> Unit,
    onNext: () -> Unit
) {

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    val scoreColor = when {
        result.score >= 80 -> NemoSecondary
        result.score >= 60 -> NemoYellow
        else -> NemoDanger
    }


    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, scoreColor.copy(alpha = 0.3f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = if (result.is_correct) "完成得很棒！" else "继续加油！",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AI 综合评分结果",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Surface(
                        color = scoreColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${result.score} 分",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                // 我的答案
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "我的答案",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = userAnswer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 参考答案展示
                result.standard_answer?.let { answer ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "标准参考答案",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NemoSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = NemoSecondary.copy(alpha = if (isDark) 0.15f else 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NemoSecondary.copy(alpha = if (isDark) 0.3f else 0.1f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = answer,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (exerciseType == "CN_TO_JP") {
                                Spacer(modifier = Modifier.width(8.dp))
                                SpeakerButton(
                                    isPlaying = playingAudioId == "standard",
                                    onClick = { onSpeak(answer, "standard") },
                                    tint = NemoSecondary,
                                    backgroundColor = NemoSecondary.copy(alpha = 0.05f),
                                    size = 36.dp
                                )
                            }
                        }
                    }

                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(0.2f) else NemoNeutrals.Gray100)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "AI 详细点评",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = null
        ) {
            Text("下一题", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ConfigRequiredView(onNavigateToSettings: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.SettingsSuggest,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f) else NemoNeutrals.Gray400
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "未配置 AI 服务",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "请前往“设置 -> AI 工坊配置”\n填写 API Key 即可开启智能练习",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNavigateToSettings,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("立即去配置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = null
            ) {
                Text("我知道了", fontWeight = FontWeight.Bold)
            }
        },
        title = { 
            Text(
                "工坊使用指南", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column {
                HelpItem("1", "定制化练习", "AI 会根据您设置的日语等级（N1-N5）生成针对性的练习题。")
                HelpItem("2", "多样化题型", "包含中译日和日译中两种类型，全面提升翻译和表达能力。")
                HelpItem("3", "专业反馈", "提交后 AI 会即时评分并提供详细的语法及用词点评建议。")
                HelpItem("4", "记录回顾", "所有练习均保存 30 天，可随时通过顶部历史图标回顾。")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
        tonalElevation = 0.dp
    )
}

@Composable
private fun HelpItem(index: String, title: String, content: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp)) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = index,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
