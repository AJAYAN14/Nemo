package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.text.FuriganaText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbConjugationScreen(
    onNavigateBack: () -> Unit,
    viewModel: VerbConjugationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    // 当回答状态改变时，控制 BottomSheet 显示
    LaunchedEffect(uiState.isAnswered) {
        if (uiState.isAnswered) {
            showSheet = true
        } else {
            showSheet = false
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    val containerColor = if (isDark) MaterialTheme.colorScheme.background else BentoColors.BgBase
    val wordCardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val qCardBg = if (isDark) IosColors.Blue.copy(alpha = 0.15f) else Color(0xFFF0F4FF)
    val qCardBorderColor = if (isDark) IosColors.Blue.copy(alpha = 0.2f) else IosColors.Blue.copy(alpha = 0.1f)

    Scaffold(
        topBar = {
            CommonHeader(
                title = "动词活用",
                onBack = onNavigateBack
            )
        },
        containerColor = containerColor
    ) { padding ->
        if (uiState.showResult) {
            ResultView(
                correctCount = uiState.correctCount,
                totalCount = uiState.questions.size,
                onRestart = { viewModel.restart() },
                onBack = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
        } else {
            val currentQuestion = uiState.questions[uiState.currentIndex]
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 1. 进度条与题号
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { (uiState.currentIndex + 1).toFloat() / uiState.questions.size },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = IosColors.Blue,
                        trackColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else NemoNeutrals.Gray200
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${uiState.currentIndex + 1}/${uiState.questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = textSub
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. 单词卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = wordCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentQuestion.furigana,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSub
                            )
                            Text(
                                text = currentQuestion.word,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = textMain
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentQuestion.meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSub
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 发音按钮
                        IconButton(
                            onClick = { viewModel.playWordTts() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(IosColors.Blue.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Play Audio",
                                tint = IosColors.Blue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 题目卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = qCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, qCardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FuriganaText(
                            text = currentQuestion.qText,
                            baseTextStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp,
                                lineHeight = 32.sp
                            ),
                            baseTextColor = textMain
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(IosColors.Blue.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "文",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IosColors.Blue
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentQuestion.translation,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSub
                            )
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
                                    isSelected = uiState.selectedOptionIndex == index,
                                    isCorrect = index == currentQuestion.correctIndex,
                                    isAnswered = uiState.isAnswered,
                                    onClick = { viewModel.selectOption(index) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // 反馈底栏
    if (showSheet) {
        val currentQuestion = uiState.questions[uiState.currentIndex]
        val isCorrect = uiState.selectedOptionIndex == currentQuestion.correctIndex
        
        ModalBottomSheet(
            onDismissRequest = { /* 锁定不可手动下滑取消，必须点击继续 */ },
            sheetState = sheetState,
            containerColor = if (isCorrect) {
                if (isDark) Color(0xFF14532D).copy(alpha = 0.9f) else Color(0xFFF0FDF4)
            } else {
                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.9f) else Color(0xFFFEF2F2)
            },
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = if (isCorrect) "✓ 正确答案：${currentQuestion.options[currentQuestion.correctIndex]}" 
                           else "✗ 正确答案：${currentQuestion.options[currentQuestion.correctIndex]}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) {
                        if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                    } else {
                        if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = currentQuestion.explanation,
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
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCorrect) IosColors.Green else IosColors.Red
                    )
                ) {
                    Text(text = "继续", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isAnswered) { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        color = bgColor,
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun ResultView(
    correctCount: Int,
    totalCount: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(IosColors.Blue.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = IosColors.Blue
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "练习完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "你答对了 $correctCount 道题，共 $totalCount 道。",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IosColors.Blue)
        ) {
            Text("重新开始", fontWeight = FontWeight.Bold)
        }
        
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("返回工坊", color = textColor)
        }
    }
}
