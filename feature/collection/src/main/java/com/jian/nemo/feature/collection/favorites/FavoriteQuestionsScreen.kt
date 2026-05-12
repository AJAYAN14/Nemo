package com.jian.nemo.feature.collection.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.FavoriteQuestion
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader


/**
 * 收藏题目列表界面
 *
 * Flat Design: 无阴影、粗体色彩、简洁线条、排版为核心
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteQuestionsScreen(
    viewModel: FavoriteQuestionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Premium Flat Colors
    val premiumRed = Color(0xFFFF3B30)
    val premiumBlue = Color(0xFF007AFF)
    val premiumGreen = Color(0xFF34C759)
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            com.jian.nemo.core.ui.component.common.CommonHeader(
                title = "收藏题目",
                onBack = onNavigateBack,
                backgroundColor = backgroundColor
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    NemoChasingDotsLoader()
                }
            }
            uiState.favoriteQuestions.isEmpty() -> {
                // Flat Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = premiumRed.copy(alpha = 0.1f),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = premiumRed.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "暂无收藏题目",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "做题时点击 ❤️ 收藏重点题目",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    items(
                        items = uiState.favoriteQuestions,
                        key = { "fav_q_${it.id}" }
                    ) { question ->
                        Box(modifier = Modifier.animateListItem()) {
                            FavoriteQuestionItem(
                                question = question,
                                accentGreen = premiumGreen,
                                tagColor = premiumBlue,
                                heartColor = premiumRed,
                                onUnfavorite = { viewModel.unfavorite(question.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 收藏题目列表项 — Flat Card
 *
 * elevation = 0, 细线边框, 无渐变
 */
@Composable
private fun FavoriteQuestionItem(
    question: FavoriteQuestion,
    accentGreen: Color,
    tagColor: Color,
    heartColor: Color,
    onUnfavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // 顶部行: 等级标签 + ❤️ 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 语法点名称 (如果有 grammarId)
                Text(
                    text = question.questionType.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                // ❤️ 收藏按钮
                IconButton(
                    onClick = onUnfavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = heartColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "取消收藏",
                                tint = heartColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 题干文本
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 答案行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = accentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = question.correctAnswer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 解析 (如果有)
            val explanationText = question.explanation
            if (!explanationText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = explanationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
