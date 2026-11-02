package com.jian.nemo.feature.library.presentation.partofspeech

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import com.jian.nemo.feature.library.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.util.displayName


/**
 * 词性分类主界面
 *
 * 展示12种词性的网格卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartOfSpeechScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPosWords: (PartOfSpeech) -> Unit,
    viewModel: PartOfSpeechViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val containerColor = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        if (uiState.isLoading) {
            NemoChasingDotsLoader(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(PartOfSpeech.entries) { pos ->
                    PartOfSpeechCard(
                        partOfSpeech = pos,
                        count = uiState.partOfSpeechStats[pos] ?: 0,
                        onClick = { onNavigateToPosWords(pos) }
                    )
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarBottom + 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(error)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.95f),
                            containerColor.copy(alpha = 0.75f),
                            containerColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.lib_pos_title_screen),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }
    }
}

/**
 * 词性卡片
 */
@Composable
private fun PartOfSpeechCard(
    partOfSpeech: PartOfSpeech,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconForPartOfSpeech(partOfSpeech),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = partOfSpeech.displayName,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.lib_pos_count_format, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 根据词性获取对应图标
 */
private fun getIconForPartOfSpeech(pos: PartOfSpeech): ImageVector {
    return when (pos) {
        PartOfSpeech.VERB -> Icons.AutoMirrored.Filled.DirectionsRun
        PartOfSpeech.NOUN -> Icons.AutoMirrored.Filled.Article
        PartOfSpeech.ADJECTIVE -> Icons.Default.Palette
        PartOfSpeech.ADVERB -> Icons.Default.Speed
        PartOfSpeech.PARTICLE -> Icons.Default.Link
        PartOfSpeech.CONJUNCTION -> Icons.Default.SwapHoriz
        PartOfSpeech.RENTAI -> Icons.Default.Category
        PartOfSpeech.PREFIX -> Icons.Default.FormatQuote
        PartOfSpeech.SUFFIX -> Icons.Default.FormatQuote
        PartOfSpeech.INTERJECTION -> Icons.Default.Campaign
        PartOfSpeech.FIXED_EXPRESSION -> Icons.Default.Star
        PartOfSpeech.LOAN_WORD -> Icons.Default.Language
    }
}
