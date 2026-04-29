package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.AIExerciseHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AIHistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyState.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<AIExerciseHistory?>(null) }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val backgroundColor = colorScheme.background
    val textPrimary = colorScheme.onSurface

    Scaffold(
        topBar = {
            CommonHeader(
                title = "练习历史",
                onBack = onNavigateBack,
                backgroundColor = backgroundColor,
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "清空历史", tint = textPrimary)
                        }
                    }
                }
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "暂无练习历史",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList) { history ->
                    HistoryItemCard(history, onClick = { selectedItem = history })
                }
            }
        }

        if (selectedItem != null) {
            DetailDialog(
                history = selectedItem!!,
                onDismiss = { selectedItem = null }
            )
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("清空历史记录", fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
                text = { Text("确定要清空所有的练习历史吗？此操作不可撤销。", color = colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearConfirm = false
                        }
                    ) {
                        Text("确认清空", color = NemoDanger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("取消", color = colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = if (isDark) colorScheme.surfaceContainerHigh else Color.White,
                tonalElevation = 0.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryItemCard(
    history: AIExerciseHistory,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(history.createdAt))

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 类型标签
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (history.type == "CN_TO_JP") "中翻日" else "日翻中",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = NemoNeutrals.Gray500
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = history.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分数展示
                val scoreColor = if (history.score >= 60) colorScheme.secondary else NemoDanger
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(scoreColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${history.score}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "难度: ${history.difficulty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = NemoNeutrals.Gray400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailDialog(
    history: AIExerciseHistory,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Text("我知道了", fontWeight = FontWeight.Bold)
            }
        },
        title = { 
            Text(
                if (history.type == "CN_TO_JP") "中翻日练习" else "日翻中练习",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            ) 
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DetailSection("题目", history.question, colorScheme.primary)
                DetailSection("您的答案", history.userAnswer, colorScheme.onSurface)
                DetailSection("标准答案", history.standardAnswer, colorScheme.secondary)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colorScheme.outlineVariant.copy(0.2f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI 评分：", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
                    Text(
                        "${history.score} 分",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (history.score >= 60) colorScheme.secondary else NemoDanger
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = if (isDark) colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "AI 点评：",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant.copy(0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            history.feedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isDark) colorScheme.surfaceContainerHigh else Color.White,
        tonalElevation = 0.dp,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = true)
    )
}

@Composable
private fun DetailSection(label: String, content: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }
}
