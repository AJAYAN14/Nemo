package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Book
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
import com.jian.nemo.core.domain.model.AIReadingHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIReadingHistoryScreen(
    viewModel: AIReadingViewModel,
    onNavigateBack: () -> Unit,
    onHistorySelected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "阅读历史", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.readingHistory.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete, 
                                contentDescription = "清空所有"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.readingHistory.isEmpty()) {
                EmptyState(onNavigateBack)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.readingHistory) { history ->
                        HistoryItemCard(
                            history = history,
                            isDark = isDark,
                            onClick = {
                                viewModel.onEvent(AIReadingEvent.LoadHistoryArticle(history))
                                onHistorySelected()
                            },
                            onDelete = {
                                viewModel.onEvent(AIReadingEvent.DeleteHistory(history.id))
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("确认清空", fontWeight = FontWeight.Bold) },
            text = { Text("您确定要清除所有的阅读和答题历史记录吗？此操作无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(AIReadingEvent.ClearAllHistory)
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(
    history: AIReadingHistory,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(history.createdAt))

    // 计算正确率
    val totalQuestions = history.questions.size
    val correctCount = history.questions.filterIndexed { index, question ->
        val userAns = history.selectedAnswers.getOrNull(index)
        userAns != null && userAns == question.answer
    }.size

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标装饰
            Surface(
                shape = CircleShape,
                color = if (isDark) colorScheme.primaryContainer.copy(alpha = 0.3f) else colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Book,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 主要文本内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) colorScheme.onSurface else BentoColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 级别标签
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) colorScheme.secondaryContainer.copy(alpha = 0.4f) else colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = history.level,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else BentoColors.TextSub
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 答题状态标签
            if (history.isSubmitted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (correctCount == totalQuestions) ReadingIconBgGreen else ReadingIconBgRed.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "得分 $correctCount/$totalQuestions",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (correctCount == totalQuestions) ReadingAccentGreen else ReadingAccentRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ReadingIconBgOrange,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "进行中",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ReadingAccentOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除图标
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "删除该历史",
                    tint = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else BentoColors.TextSub.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDark) colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else NemoNeutrals.Gray400,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "暂无阅读历史",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "在这里记录并恢复您的 AI 日语阅读及答题表现",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else BentoColors.TextSub,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("开始阅读", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 补充的局部设计颜色和样式常量
private object NemoNeutrals {
    val Gray100 = Color(0xFFF3F4F6)
    val Gray400 = Color(0xFF9CA3AF)
}

private object BentoColors {
    val BgBase = Color(0xFFF9FAFB)
    val TextMain = Color(0xFF1F2937)
    val TextSub = Color(0xFF6B7280)
}

// 级别对应的特殊卡片颜色（在 AIReadingScreen 中定义）
private val ReadingIconBgRed = Color(0xFFFEE2E2)
private val ReadingAccentRed = Color(0xFFEF4444)
private val ReadingIconBgGreen = Color(0xFFD1FAE5)
private val ReadingAccentGreen = Color(0xFF10B981)
private val ReadingIconBgOrange = Color(0xFFFEF3C7)
private val ReadingAccentOrange = Color(0xFFD97706)
