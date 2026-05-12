package com.jian.nemo.feature.collection.mistakes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
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
 * 错误单词列表界面
 *
 * UI/UX Pro Max V2: Custom Premium Colors, Tinted Squircle Tags, High-Quality Surfaces
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongWordsScreen(
    viewModel: WrongWordsViewModel = hiltViewModel(),
    onWordClick: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val useDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Premium Aesthetics
    val backgroundColor = MaterialTheme.colorScheme.background

    // Custom Premium Colors
    val premiumRed = Color(0xFFFF3B30) // Apple-style System Red
    val premiumBlue = Color(0xFF007AFF) // Apple-style System Blue
    val premiumOrange = Color(0xFFFF9500) // Apple-style System Orange
    val premiumGray = Color(0xFF8E8E93) // System Gray

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            com.jian.nemo.core.ui.component.common.CommonHeader(
                title = "错误的单词",
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
            uiState.words.isEmpty() -> {
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
                            color = premiumGray.copy(alpha = 0.1f),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = premiumGray.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "暂无错词记录",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "太棒了！继续保持全对的状态。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                        items = uiState.words,
                        key = { "wrong_word_${it.id}" }
                    ) { word ->
                        Box(modifier = Modifier.animateListItem()) {
                            WrongWordItem(
                                word = word,
                                onClick = { onWordClick(word.id) },
                                accentColor = premiumBlue,
                                tagColor = premiumBlue // Or determine based on level
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 错误单词列表项 - Premium Card Style V2
 */
@Composable
private fun WrongWordItem(
    word: Word,
    onClick: () -> Unit,
    accentColor: Color,
    tagColor: Color
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Clean surface
        ),
        // Subtle border for definition without heavy shadow
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp) // More padding for premium feel
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Japanese Word
                Text(
                    text = word.japanese,
                    style = MaterialTheme.typography.headlineSmall, // Larger
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

            // Premium Tag: Tinted Squircle
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tagColor.copy(alpha = 0.1f) // Tinted background
            ) {
                Text(
                    text = word.level,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tagColor // Solid text color
                )
            }
        }
    }
}
