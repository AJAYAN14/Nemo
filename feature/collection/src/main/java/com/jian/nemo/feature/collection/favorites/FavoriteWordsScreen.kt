package com.jian.nemo.feature.collection.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val backgroundColor = MaterialTheme.colorScheme.background

    // Custom Premium Colors (Shared Palette)
    val premiumRed = Color(0xFFFF3B30)
    val premiumBlue = Color(0xFF007AFF)
    val premiumOrange = Color(0xFFFF9500)
    val premiumGray = Color(0xFF8E8E93)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            com.jian.nemo.core.ui.component.common.CommonHeader(
                title = "收藏单词",
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
                        Box(modifier = Modifier.animateListItem()) {
                            FavoriteWordItem(
                                word = word,
                                onClick = { onWordClick(word.id) },
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
@Composable
private fun FavoriteWordItem(
    word: Word,
    onClick: () -> Unit,
    accentColor: Color,
    tagColor: Color
) {
    val premiumRed = Color(0xFFFF3B30)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
