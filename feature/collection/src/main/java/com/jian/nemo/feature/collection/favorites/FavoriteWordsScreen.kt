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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


/**
 * 收藏单词列表界面
 *
 * UI/UX Pro Max V2: Custom Premium Colors, Tinted Squircle Tags, High-Quality Surfaces
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteWordsScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onWordClick: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val useDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Premium Aesthetics
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // Custom Premium Colors (Shared Palette)
    val premiumRed = Color(0xFFFF3B30)
    val premiumBlue = Color(0xFF007AFF)
    val premiumOrange = Color(0xFFFF9500)
    val premiumGray = Color(0xFF8E8E93)

    // 多选状态
    var selectedWordIds by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val isSelectionMode = selectedWordIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 拦截物理返回键
    BackHandler(enabled = isSelectionMode) {
        selectedWordIds = emptySet()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "取消收藏",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要将选中的 ${selectedWordIds.size} 个单词取消收藏吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWordFavorites(selectedWordIds)
                        selectedWordIds = emptySet()
                        showDeleteDialog = false
                    }
                ) {
                    Text("确认移出", color = premiumRed, fontWeight = FontWeight.Bold)
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
                        IconButton(onClick = { selectedWordIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "取消选择",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "已选择 ${selectedWordIds.size} 项",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        
                        val allWordIds = uiState.favoriteWords.map { it.id }.toSet()
                        val isAllSelected = selectedWordIds.size == allWordIds.size
                        TextButton(
                            onClick = {
                                selectedWordIds = if (isAllSelected) emptySet() else allWordIds
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
                    title = "收藏单词",
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
            uiState.favoriteWords.isEmpty() -> {
                // Premium Empty State
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
                            color = premiumRed.copy(alpha = 0.1f), // Red tint for Favorites
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
                            text = "暂无收藏单词",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "在学习过程中遇到喜欢的单词可以收藏哦",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        items = uiState.favoriteWords,
                        key = { "favorite_${it.id}" }
                    ) { word ->
                        val isSelected = selectedWordIds.contains(word.id)
                        Box(modifier = Modifier.animateListItem()) {
                            FavoriteWordItem(
                                word = word,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedWordIds = if (isSelected) {
                                            selectedWordIds - word.id
                                        } else {
                                            selectedWordIds + word.id
                                        }
                                    } else {
                                        onWordClick(word.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedWordIds = selectedWordIds + word.id
                                    }
                                },
                                accentColor = premiumBlue,
                                tagColor = premiumBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 收藏单词列表项 - Premium Card Style V2
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteWordItem(
    word: Word,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    accentColor: Color,
    tagColor: Color
) {
    val premiumRed = Color(0xFFFF3B30)

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
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
                        checkedColor = accentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Japanese Word
                Text(
                    text = word.japanese,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Meaning and Kana
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Kana (Subtle)
                    Text(
                        text = word.hiragana,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Meaning (Primary Content)
                    Text(
                        text = word.chinese,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Stacked Icons/Tags
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                 // Heart Icon (Tinted Squircle for consistency)
                Surface(
                     shape = RoundedCornerShape(8.dp),
                     color = premiumRed.copy(alpha = 0.1f),
                     modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorited",
                            tint = premiumRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // JLPT Squircle Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tagColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = word.level,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tagColor
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

