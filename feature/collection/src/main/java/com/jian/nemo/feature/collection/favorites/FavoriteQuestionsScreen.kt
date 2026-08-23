package com.jian.nemo.feature.collection.favorites

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.FavoriteQuestion
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.NemoDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


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
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // 多选状态
    var selectedQuestionIds by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val isSelectionMode = selectedQuestionIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 拦截物理返回键
    BackHandler(enabled = isSelectionMode) {
        selectedQuestionIds = emptySet()
    }

    if (showDeleteDialog) {
        NemoDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "取消收藏",
            text = "确定要将选中的 ${selectedQuestionIds.size} 个题目取消收藏吗？",
            confirmText = "确认移出",
            dismissText = "取消",
            isDangerous = true,
            onConfirm = {
                viewModel.deleteQuestionFavorites(selectedQuestionIds)
                selectedQuestionIds = emptySet()
                showDeleteDialog = false
            }
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
                        IconButton(onClick = { selectedQuestionIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "取消选择",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "已选择 ${selectedQuestionIds.size} 项",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        
                        val allQuestionIds = uiState.favoriteQuestions.map { it.id }.toSet()
                        val isAllSelected = selectedQuestionIds.size == allQuestionIds.size
                        TextButton(
                            onClick = {
                                selectedQuestionIds = if (isAllSelected) emptySet() else allQuestionIds
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
                                contentDescription = "批量取消收藏",
                                tint = premiumRed
                            )
                        }
                    }
                }
            } else {
                com.jian.nemo.core.ui.component.common.CommonHeader(
                    title = "收藏题目",
                    onBack = onNavigateBack,
                    backgroundColor = backgroundColor
                )
            }
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
                        val isSelected = selectedQuestionIds.contains(question.id)
                        Box(modifier = Modifier.animateListItem()) {
                            FavoriteQuestionItem(
                                question = question,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedQuestionIds = if (isSelected) {
                                            selectedQuestionIds - question.id
                                        } else {
                                            selectedQuestionIds + question.id
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedQuestionIds = selectedQuestionIds + question.id
                                    }
                                },
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteQuestionItem(
    question: FavoriteQuestion,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    accentGreen: Color,
    tagColor: Color,
    heartColor: Color,
    onUnfavorite: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 20.dp, isDark = isDark)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
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
                        checkedColor = accentGreen
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
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

                    // ❤️ 收藏按钮 (多选模式下隐藏)
                    AnimatedVisibility(
                        visible = !isSelectionMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
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

