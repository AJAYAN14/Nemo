package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.AIExercise
import com.jian.nemo.core.domain.model.AIGradeResult
import com.jian.nemo.core.ui.component.common.CommonHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWorkshopScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: AIWorkshopViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showHelp by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    
    // 语义化颜色映射
    val backgroundColor = colorScheme.background
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val secondarySurface = if (isDark) colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50
    val textPrimary = colorScheme.onSurface
    val textSecondary = colorScheme.onSurfaceVariant
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100
    val dividerColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.2f) else NemoNeutrals.Gray100

    Scaffold(
        topBar = {
            CommonHeader(
                title = "AI 例文工坊",
                onBack = onNavigateBack,
                backgroundColor = backgroundColor,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Rounded.History, contentDescription = "历史记录", tint = textPrimary)
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Rounded.HelpOutline, contentDescription = "帮助", tint = textPrimary)
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
                    ConfigRequiredView()
                } else {
                    // 核心操作区
                    WorkshopContent(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 加载层 - 采用全屏磨砂质感（Flat）
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = NemoPrimary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "AI 正在思考中...",
                            style = MaterialTheme.typography.labelLarge,
                            color = NemoPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 错误提示
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = NemoDanger,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.onEvent(AIWorkshopEvent.ClearError) }) {
                            Text("确定", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Text(error)
                }
            }
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(secondarySurface, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / modes.size

        // Sliding Capsule
        val offset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "modeCapsuleOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(if (isDark) colorScheme.surfaceContainerHighest else Color.White, RoundedCornerShape(10.dp))
                .border(0.5.dp, if (isDark) colorScheme.outlineVariant.copy(0.2f) else NemoNeutrals.Gray200.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        )

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
                        color = if (isSelected) NemoPrimary else NemoNeutrals.Gray500
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(secondarySurface, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / levels.size
        
        // Sliding Capsule
        val offset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "capsuleOffset"
        )
        
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(if (isDark) colorScheme.surfaceContainerHighest else Color.White, RoundedCornerShape(10.dp))
                .border(0.5.dp, if (isDark) colorScheme.outlineVariant.copy(0.2f) else NemoNeutrals.Gray200.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        )
        
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
                        color = if (isSelected) NemoPrimary else NemoNeutrals.Gray500
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
                color = NemoPrimary.copy(alpha = 0.05f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isGrammarMode) Icons.Rounded.MenuBook else Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = NemoPrimary
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
                colors = ButtonDefaults.buttonColors(containerColor = NemoPrimary),
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
                            color = NemoPrimary.copy(alpha = 0.1f),
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
                                color = NemoPrimary,
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
                
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 34.sp
                )
                
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
                focusedBorderColor = NemoPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.4f),
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor,
                disabledContainerColor = surfaceColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = NemoPrimary
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
                containerColor = NemoPrimary,
                disabledContainerColor = NemoNeutrals.Gray300
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
    onNext: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    val scoreColor = when {
        result.score >= 80 -> NemoSecondary
        result.score >= 60 -> Color(0xFFFBBF24)
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
                        Text(
                            text = answer,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
            Icon(Icons.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ConfigRequiredView() {
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
