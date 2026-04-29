package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.AIExercise
import com.jian.nemo.core.domain.model.AIGradeResult
import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.ui.component.common.CommonHeader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWorkshopScreen(
    onNavigateBack: () -> Unit,
    viewModel: AIWorkshopViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showHistory by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var selectedHistoryItem by remember { mutableStateOf<AIExerciseHistory?>(null) }

    Scaffold(
        topBar = {
            CommonHeader(
                title = "AI 例文工坊",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Rounded.History, contentDescription = "历史记录")
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Rounded.HelpOutline, contentDescription = "帮助")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!uiState.isConfigured) {
                    ConfigRequiredView()
                } else if (uiState.currentExercise == null && !uiState.isLoading) {
                    StartView(onStart = { viewModel.onEvent(AIWorkshopEvent.GenerateNewExercise) })
                } else {
                    ExerciseView(
                        uiState = uiState,
                        onAnswerChange = { viewModel.onEvent(AIWorkshopEvent.UpdateUserAnswer(it)) },
                        onSubmit = { viewModel.onEvent(AIWorkshopEvent.SubmitAnswer) },
                        onNext = { viewModel.onEvent(AIWorkshopEvent.GenerateNewExercise) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NemoPrimary)
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onEvent(AIWorkshopEvent.ClearError) }) {
                            Text("确定", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }

        if (showHistory) {
            ModalBottomSheet(
                onDismissRequest = { showHistory = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                HistoryListView(
                    history = uiState.history,
                    onItemClick = { 
                        selectedHistoryItem = it
                    }
                )
            }
        }

        if (selectedHistoryItem != null) {
            AlertDialog(
                onDismissRequest = { selectedHistoryItem = null },
                confirmButton = {
                    TextButton(onClick = { selectedHistoryItem = null }) {
                        Text("关闭")
                    }
                },
                title = { Text("练习详情", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("题目：", style = MaterialTheme.typography.labelLarge, color = NemoPrimary)
                        Text(selectedHistoryItem!!.question, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("您的回答：", style = MaterialTheme.typography.labelLarge, color = NemoPrimary)
                        Text(selectedHistoryItem!!.userAnswer, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("标准答案：", style = MaterialTheme.typography.labelLarge, color = NemoSecondary)
                        Text(selectedHistoryItem!!.standardAnswer, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("评分：", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${selectedHistoryItem!!.score} 分",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedHistoryItem!!.score >= 60) NemoSecondary else NemoDanger
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI 点评：", style = MaterialTheme.typography.labelLarge)
                        Text(selectedHistoryItem!!.feedback, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            )
        }

        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) {
                        Text("知道了")
                    }
                },
                title = { Text("AI 例文工坊说明", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("1. AI 会根据您在设置中选择的日语等级（N1-N5）生成适合的练习题。")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("2. 题目包含中译日和日译中两种类型，旨在提升您的翻译和表达能力。")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("3. 提交答案后，AI 会给出评分及详细的点评建议。")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("4. 所有的练习记录都会保存 30 天，您可以随时通过顶部的历史记录按钮回顾。")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryListView(
    history: List<AIExerciseHistory>,
    onItemClick: (AIExerciseHistory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "过去 30 天的练习记录",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                history.forEach { item ->
                    HistoryItem(item, onClick = { onItemClick(item) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItem(item: AIExerciseHistory, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = remember(item.createdAt) { sdf.format(Date(item.createdAt)) }
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NemoPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.difficulty,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NemoPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = "${item.score}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (item.score >= 60) NemoSecondary else NemoDanger
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ConfigRequiredView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.SettingsSuggest,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = NemoPrimary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "需要配置 AI 服务",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请先前往“设置 -> AI 工坊配置”\n填写您的 API Key 以启用此功能",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StartView(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Image/Icon
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(NemoPrimary.copy(alpha = 0.1f), NemoCyan.copy(alpha = 0.1f))
                    ),
                    shape = RoundedCornerShape(40.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = NemoPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "准备好开始练习了吗？",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "AI 将根据您的等级为您生成专属的翻译练习",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NemoPrimary)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始生成题目", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExerciseView(
    uiState: AIWorkshopUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit
) {
    val exercise = uiState.currentExercise ?: return
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Level Badge
        Surface(
            color = NemoPrimary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(
                text = "难度: ${exercise.difficulty}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = NemoPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Question Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (exercise.type == "CN_TO_JP") "请翻译成日语：" else "请翻译成中文：",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
                
                if (exercise.hints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = NemoGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "提示: ${exercise.hints.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Input Area
        OutlinedTextField(
            value = uiState.userAnswer,
            onValueChange = onAnswerChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text("在此输入您的答案...") },
            shape = RoundedCornerShape(16.dp),
            enabled = uiState.gradeResult == null && !uiState.isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Button
        if (uiState.gradeResult == null) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.userAnswer.isNotBlank() && !uiState.isLoading
            ) {
                Text("提交答案并评分", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            GradeResultView(uiState.gradeResult, onNext)
        }
    }
}

@Composable
fun GradeResultView(result: AIGradeResult, onNext: () -> Unit) {
    val scoreColor = when {
        result.score >= 90 -> NemoSecondary
        result.score >= 60 -> NemoGold
        else -> NemoDanger
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = scoreColor.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(2.dp, scoreColor.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (result.is_correct) "非常棒！" else "继续加油！",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    
                    Surface(
                        color = scoreColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${result.score} 分",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = scoreColor.copy(alpha = 0.1f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "AI 点评：",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = result.feedback,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NemoPrimary)
        ) {
            Text("下一题", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowForward, contentDescription = null)
        }
    }
}
