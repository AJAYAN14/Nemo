package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material3.*
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.component.NemoDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.AIReadingHistory
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.feature.learning.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AIReadingHistoryScreen(
    viewModel: AIReadingViewModel,
    onNavigateBack: () -> Unit,
    onHistorySelected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
        // 1. 底层滚动内容 / 空状态（被 backdrop 采样源录制）
        if (uiState.readingHistory.isEmpty()) {
            EmptyState(
                onBack = onNavigateBack,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .padding(top = statusBarTop + 60.dp, bottom = navBarBottom)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                items(uiState.readingHistory) { history ->
                    HistoryItemCard(
                        history = history,
                        isDark = isDark,
                        onClick = {
                            viewModel.onEvent(AIReadingEvent.LoadHistoryArticle(history))
                            onHistorySelected()
                        },
                        onDelete = {
                            viewModel.onEvent(AIReadingEvent.DeleteHistory(history.id))
                        }
                    )
                }
            }
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background.copy(alpha = 0.95f),
                            colorScheme.background.copy(alpha = 0.75f),
                            colorScheme.background.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.ai_reading_history_title),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    if (uiState.readingHistory.isNotEmpty()) {
                        FrostedLiquidGlassButton(
                            onClick = { showDeleteAllDialog = true },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.ai_reading_clear_all),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        }
    }

    if (showDeleteAllDialog) {
        NemoDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = stringResource(R.string.ai_reading_confirm_clear_title),
            text = stringResource(R.string.ai_reading_confirm_clear_desc),
            confirmText = stringResource(R.string.ai_reading_btn_clear),
            dismissText = stringResource(R.string.ai_reading_btn_cancel),
            isDangerous = true,
            onConfirm = {
                viewModel.onEvent(AIReadingEvent.ClearAllHistory)
                showDeleteAllDialog = false
            }
        )
    }
    }
}

@Composable
private fun HistoryItemCard(
    history: AIReadingHistory,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val colorScheme = MaterialTheme.colorScheme
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(history.createdAt))

    // 计算正确率
    val totalQuestions = history.questions.size
    val correctCount = history.questions.filterIndexed { index, question ->
        val userAns = history.selectedAnswers.getOrNull(index)
        userAns != null && userAns == question.answer
    }.size

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标装饰：在 Plump Duo 模式下移除纯色圆形底块，居中显示 28dp 双色图标
            if (isPlumpDuo) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlumpDuoIcon(
                        icon = PlumpIcon.BookOpen,
                        color = IsoluminantPalette.SkyIndigo,
                        size = 28.dp
                    )
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) colorScheme.primaryContainer.copy(alpha = 0.3f) else colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Book,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 主要文本内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) colorScheme.onSurface else BentoColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 级别标签
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) colorScheme.secondaryContainer.copy(alpha = 0.4f) else colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = history.level,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else BentoColors.TextSub
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 答题状态标签
            if (history.isSubmitted) {
                val isAllCorrect = correctCount == totalQuestions
                val badgeBg = when {
                    isPlumpDuo && isAllCorrect -> IsoluminantPalette.EmeraldGreen.copy(alpha = 0.08f)
                    isPlumpDuo -> IsoluminantPalette.CoralFlame.copy(alpha = 0.08f)
                    isAllCorrect -> LearningPalette.ReadingIconBgGreen
                    else -> LearningPalette.ReadingIconBgRed.copy(alpha = 0.6f)
                }
                val badgeTextColor = when {
                    isPlumpDuo && isAllCorrect -> IsoluminantPalette.EmeraldGreen
                    isPlumpDuo -> IsoluminantPalette.CoralFlame
                    isAllCorrect -> LearningPalette.ReadingAccentGreen
                    else -> LearningPalette.ReadingAccentRed
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_reading_score_format, correctCount, totalQuestions),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                val ongoingBg = if (isPlumpDuo) IsoluminantPalette.WarmAmber.copy(alpha = 0.08f) else LearningPalette.ReadingIconBgOrange
                val ongoingTextColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else LearningPalette.ReadingAccentOrange
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ongoingBg,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_reading_status_ongoing),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ongoingTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除图标
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.ai_reading_delete_item),
                    tint = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else BentoColors.TextSub.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPlumpDuo) {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlumpDuoIcon(
                        icon = PlumpIcon.History,
                        color = IsoluminantPalette.SkyIndigo,
                        size = 56.dp
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = if (isDark) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.ai_reading_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.ai_reading_empty_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(54.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else colorScheme.primary,
                contentColor = colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_reading_btn_start),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
}
