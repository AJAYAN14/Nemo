package com.jian.nemo.feature.collection.mistakes

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
    val premiumGray = Color(0xFF8E8E93)
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            com.jian.nemo.core.ui.component.common.CommonHeader(
                title = "错误的语法",
                onBack = onNavigateBack,
                backgroundColor = backgroundColor
            )
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
                        Box(modifier = Modifier.animateListItem()) {
                            WrongGrammarCard(
                                mistake = mistake,
                                onClick = { mistake.grammar?.id?.let { onGrammarClick(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WrongGrammarCard(
    mistake: GrammarWrongAnswer,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val premiumRed = Color(0xFFFF3B30)
    val premiumGreen = Color(0xFF34C759)
    val premiumBlue = Color(0xFF007AFF)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Level + Title + Status
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Icon(Icons.Rounded.Cancel, null, tint = premiumRed, modifier = Modifier.size(20.dp))
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
