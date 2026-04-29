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

    Scaffold(
        topBar = {
            CommonHeader(
                title = "AI 例文工坊",
                onBack = onNavigateBack,
                backgroundColor = NemoNeutrals.Gray50,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Rounded.History, contentDescription = "历史记录", tint = NemoNeutrals.Gray900)
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Rounded.HelpOutline, contentDescription = "帮助", tint = NemoNeutrals.Gray900)
                    }
                }
            )
        },
        containerColor = NemoNeutrals.Gray50
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
                        .background(Color.White.copy(alpha = 0.7f))
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
        // 1. 难度切换面板
        DifficultySection(
            currentLevel = uiState.difficulty,
            onLevelChange = { onEvent(AIWorkshopEvent.UpdateDifficulty(it)) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 主视图切换
        AnimatedContent(
            targetState = uiState.currentExercise == null,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ViewSwitch"
        ) { isStartView ->
            if (isStartView) {
                HeroStartView(onStart = { onEvent(AIWorkshopEvent.GenerateNewExercise) })
            } else {
                ExerciseMainView(
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun DifficultySection(
    currentLevel: String,
    onLevelChange: (String) -> Unit,
    enabled: Boolean
) {
    val levels = listOf("N5", "N4", "N3", "N2", "N1")
    val selectedIndex = levels.indexOf(currentLevel)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NemoNeutrals.Gray100)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(NemoPrimary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.SignalCellularAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NemoPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "日语能力等级",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NemoNeutrals.Gray900
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Apple Style Segmented Control
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(NemoNeutrals.Gray50, RoundedCornerShape(14.dp))
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
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(0.5.dp, NemoNeutrals.Gray200.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                )
                
                // Interactive Labels
                Row(modifier = Modifier.fillMaxSize()) {
                    levels.forEach { level ->
                        val isSelected = level == currentLevel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
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
    }
}

@Composable
private fun HeroStartView(onStart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NemoNeutrals.Gray100)
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
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = NemoPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "开启智能练习",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = NemoNeutrals.Gray900
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
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

    Column(modifier = Modifier.fillMaxWidth()) {
        // 题目面板
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NemoNeutrals.Gray100)
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
                    color = NemoNeutrals.Gray900,
                    lineHeight = 34.sp
                )
                
                if (exercise.hints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = NemoNeutrals.Gray50,
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
                                color = NemoNeutrals.Gray600,
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
                    color = NemoNeutrals.Gray400,
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NemoPrimary,
                unfocusedBorderColor = NemoNeutrals.Gray200,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = NemoNeutrals.Gray50
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
    onNext: () -> Unit
) {
    val scoreColor = when {
        result.score >= 80 -> NemoSecondary
        result.score >= 60 -> Color(0xFFFBBF24)
        else -> NemoDanger
    }

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, scoreColor.copy(alpha = 0.3f))
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
                            color = NemoNeutrals.Gray900
                        )
                        Text(
                            text = "AI 综合评分结果",
                            style = MaterialTheme.typography.labelSmall,
                            color = NemoNeutrals.Gray400
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
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = NemoNeutrals.Gray100)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "AI 详细点评",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NemoPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NemoNeutrals.Gray800,
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
            colors = ButtonDefaults.buttonColors(containerColor = NemoPrimary),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            color = NemoNeutrals.Gray100,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.SettingsSuggest,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = NemoNeutrals.Gray400
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "未配置 AI 服务",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = NemoNeutrals.Gray900
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "请前往“设置 -> AI 工坊配置”\n填写 API Key 即可开启智能练习",
            style = MaterialTheme.typography.bodyMedium,
            color = NemoNeutrals.Gray500,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NemoPrimary),
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
                color = NemoNeutrals.Gray900
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
        containerColor = Color.White,
        tonalElevation = 0.dp
    )
}

@Composable
private fun HelpItem(index: String, title: String, content: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp)) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = NemoPrimary.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = index,
                    style = MaterialTheme.typography.labelSmall,
                    color = NemoPrimary,
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
                color = NemoNeutrals.Gray900
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = NemoNeutrals.Gray600,
                lineHeight = 18.sp
            )
        }
    }
}
