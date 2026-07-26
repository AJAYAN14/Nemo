package com.jian.nemo.feature.collection.mistakes

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.GrammarWrongAnswer
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader

/**
 * 错误语法列表界面 (题目快照版)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongGrammarsScreen(
    viewModel: WrongGrammarsViewModel = hiltViewModel(),
    onGrammarClick: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val premiumBlue = Color(0xFF007AFF)
    val premiumRed = Color(0xFFFF3B30)
    val premiumGray = Color(0xFF8E8E93)
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // 多选状态
    var selectedGrammarIds by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val isSelectionMode = selectedGrammarIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 拦截物理返回
    BackHandler(enabled = isSelectionMode) {
        selectedGrammarIds = emptySet()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "移出错题本",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要将选中的 ${selectedGrammarIds.size} 个语法从错题本中移除吗？此操作不会删除语法本身，仅清除错题记录。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWrongGrammars(selectedGrammarIds)
                        selectedGrammarIds = emptySet()
                        showDeleteDialog = false
                    }
                ) {
                    Text("确认移除", color = premiumRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp),
                    color = backgroundColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedGrammarIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "取消选择",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "已选择 ${selectedGrammarIds.size} 项",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        
                        val allGrammarIds = uiState.wrongAnswers.map { it.grammarId }.toSet()
                        val isAllSelected = selectedGrammarIds.size == allGrammarIds.size
                        TextButton(
                            onClick = {
                                selectedGrammarIds = if (isAllSelected) emptySet() else allGrammarIds
                            }
                        ) {
                            Text(
                                text = if (isAllSelected) "取消全选" else "全选",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = premiumBlue
                            )
                        }
                        
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "批量删除",
                                tint = premiumRed
                            )
                        }
                    }
                }
            } else {
                com.jian.nemo.core.ui.component.common.CommonHeader(
                    title = "错误的语法",
                    onBack = onNavigateBack,
                    backgroundColor = backgroundColor
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    NemoChasingDotsLoader()
                }
            }
            uiState.wrongAnswers.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(32.dp), color = premiumGray.copy(alpha = 0.1f), modifier = Modifier.size(100.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(48.dp), tint = Color(0xFF34C759).copy(alpha = 0.5f))
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("暂无错题记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("语法掌握得很好！继续保持。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    items(items = uiState.wrongAnswers, key = { "mistake_${it.id}" }) { mistake ->
                        val isSelected = selectedGrammarIds.contains(mistake.grammarId)
                        Box(modifier = Modifier.animateListItem()) {
                            WrongGrammarCard(
                                mistake = mistake,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onSelectedChange = { checked ->
                                    selectedGrammarIds = if (checked) {
                                        selectedGrammarIds + mistake.grammarId
                                    } else {
                                        selectedGrammarIds - mistake.grammarId
                                    }
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedGrammarIds = if (isSelected) {
                                            selectedGrammarIds - mistake.grammarId
                                        } else {
                                            selectedGrammarIds + mistake.grammarId
                                        }
                                    } else {
                                        mistake.grammar?.id?.let { onGrammarClick(it) }
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedGrammarIds = selectedGrammarIds + mistake.grammarId
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WrongGrammarCard(
    mistake: GrammarWrongAnswer,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val premiumRed = Color(0xFFFF3B30)
    val premiumGreen = Color(0xFF34C759)
    val premiumBlue = Color(0xFF007AFF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) premiumBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox 平滑滑出动效
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NemoRoundCheckbox(
                        checked = isSelected,
                        checkedColor = premiumBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // 主要内容 Column
            Column(modifier = Modifier.weight(1f)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = premiumBlue.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = mistake.grammar?.grammarLevel ?: "N/A",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = premiumBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mistake.grammar?.grammar ?: "未知语法",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!isSelectionMode) {
                        Icon(Icons.Rounded.Cancel, null, tint = premiumRed, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Question Text
                val annotatedText = buildAnnotatedString {
                    val text = mistake.questionText
                    if (text.contains("____")) {
                        val parts = text.split("____")
                        parts.forEachIndexed { index, part ->
                            append(part)
                            if (index < parts.size - 1) {
                                withStyle(style = SpanStyle(color = premiumRed, fontWeight = FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                                    append("____")
                                }
                            }
                        }
                    } else {
                        append(text)
                    }
                }
                Text(text = annotatedText, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // Answer Comparison
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Close, null, tint = premiumRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "你的答案：${mistake.userAnswer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = premiumRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null, tint = premiumGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "正确答案：${mistake.correctAnswer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = premiumGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Explanation Section
                if (!mistake.explanation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (expanded) "收起解析" else "查看解析", style = MaterialTheme.typography.labelLarge)
                            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, modifier = Modifier.size(18.dp))
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        val expl = mistake.explanation
                        if (expl != null) {
                            Text(
                                text = expl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 8.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NemoRoundCheckbox(
    checked: Boolean,
    checkedColor: Color,
    modifier: Modifier = Modifier
) {
    val uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .size(22.dp)
            .background(
                color = if (checked) checkedColor else Color.Transparent,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .border(
                width = if (checked) 0.dp else 2.dp,
                color = if (checked) Color.Transparent else uncheckedColor,
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
