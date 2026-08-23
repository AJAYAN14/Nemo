package com.jian.nemo.feature.learning.presentation.ai

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.NemoDialog
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
import androidx.compose.animation.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AIHistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyState.collectAsState()
    val playingAudioId by viewModel.playingAudioId.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<AIExerciseHistory?>(null) }


    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val textPrimary = colorScheme.onSurface

    Box(modifier = Modifier.fillMaxSize()) {
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
        }

        // 全景详情覆盖层 (Edge-to-Edge)
        // 使用 AnimatedContent 确保在 selectedItem 置空时退出动画能完整播放
        AnimatedContent(
            targetState = selectedItem,
            transitionSpec = {
                (fadeIn() + slideInVertically(initialOffsetY = { it / 2 }))
                    .togetherWith(fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }))
            },
            label = "DetailOverlay"
        ) { targetItem ->
            if (targetItem != null) {
                AIExerciseDetailDialog(
                    history = targetItem,
                    playingAudioId = playingAudioId,
                    onSpeak = { text, id -> viewModel.speakText(text, id) },
                    onDismiss = { selectedItem = null }
                )

            }
        }

        if (showClearConfirm) {
            NemoDialog(
                onDismissRequest = { showClearConfirm = false },
                title = "清空历史记录",
                text = "确定要清空所有的练习历史吗？此操作不可撤销。",
                confirmText = "确认清空",
                dismissText = "取消",
                isDangerous = true,
                onConfirm = {
                    viewModel.clearAllHistory()
                    showClearConfirm = false
                }
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
                val scoreColor = when {
                    history.score >= 80 -> NemoSecondary
                    history.score >= 60 -> NemoYellow
                    else -> NemoDanger
                }

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


