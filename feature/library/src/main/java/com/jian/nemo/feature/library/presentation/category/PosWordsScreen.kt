package com.jian.nemo.feature.library.presentation.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.library.R
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.animation.animateListItem
import com.jian.nemo.core.ui.animation.animateListItem
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.util.displayName


/**
 * 词性单词列表界面
 *
 * 展示某个词性下的所有单词
 */
@Composable
fun PosWordsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PosWordsViewModel = hiltViewModel()
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
        } else if (uiState.words.isEmpty()) {
            Text(
                text = stringResource(R.string.lib_pos_words_empty_format, uiState.partOfSpeech.displayName),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.words) { word ->
                    Box(modifier = Modifier.animateListItem()) {
                        WordListItem(word = word)
                    }
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
                title = uiState.partOfSpeech.displayName,
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    Text(
                        text = stringResource(R.string.lib_pos_words_count_format, uiState.words.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

/**
 * 单词列表项
 */
@Composable
private fun WordListItem(
    word: Word
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = word.japanese,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = word.hiragana,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = word.chinese,
                style = MaterialTheme.typography.bodySmall
            )

            val examples = word.getAllExamples()
            if (examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                val firstExample = examples.first()
                Text(
                    text = firstExample.first,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = firstExample.second,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
