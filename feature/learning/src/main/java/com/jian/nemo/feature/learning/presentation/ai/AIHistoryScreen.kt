package com.jian.nemo.feature.learning.presentation.ai

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.NemoDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jian.nemo.feature.learning.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.animation.*
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AIHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AIHistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyState.collectAsState()
    val playingAudioId by viewModel.playingAudioId.collectAsState()
    val settingIconStyle by viewModel.settingIconStyle.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<AIExerciseHistory?>(null) }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 1. 底层滚动内容 / 空状态（被 backdrop 采样源录制）
            if (historyList.isEmpty()) {
                EmptyHistoryView(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .padding(top = statusBarTop + 60.dp, bottom = navBarBottom)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList) { history ->
                        HistoryItemCard(history, onClick = { selectedItem = history })
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
                                backgroundColor.copy(alpha = 0.95f),
                                backgroundColor.copy(alpha = 0.75f),
                                backgroundColor.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(bottom = 8.dp)
            ) {
                CommonHeader(
                    title = stringResource(R.string.learn_ai_history_title),
                    onBack = onNavigateBack,
                    backgroundColor = Color.Transparent,
                    backdrop = backdrop,
                    backButtonSize = 48.dp,
                    actions = {
                        if (historyList.isNotEmpty()) {
                            FrostedLiquidGlassButton(
                                onClick = { showClearConfirm = true },
                                backdrop = backdrop,
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                isInteractive = true,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.learn_ai_history_cd_clear),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                )
            }

            // 全景详情覆盖层 (Edge-to-Edge)
            // 使用 AnimatedContent 确保在 selectedItem 置空时退出动画能完整播放
            AnimatedContent(
                targetState = selectedItem,
                transitionSpec = {
                    (fadeIn() + slideInVertically(initialOffsetY = { it / 2 }))
                        .togetherWith(fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }))
                },
                label = "DetailOverlay"
            ) { targetItem ->
                if (targetItem != null) {
                    AIExerciseDetailDialog(
                        history = targetItem,
                        playingAudioId = playingAudioId,
                        onSpeak = { text, id -> viewModel.speakText(text, id) },
                        onDismiss = { selectedItem = null }
                    )
                }
            }

            if (showClearConfirm) {
                NemoDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = stringResource(R.string.learn_ai_history_dialog_clear_title),
                    text = stringResource(R.string.learn_ai_history_dialog_clear_msg),
                    confirmText = stringResource(R.string.learn_ai_history_confirm_clear),
                    dismissText = stringResource(R.string.ai_reading_btn_cancel),
                    isDangerous = true,
                    onConfirm = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(
    modifier: Modifier = Modifier
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val historyIconColor = if (isDark) IosColors.IndigoDark else IosColors.Indigo
    val historyContainerBg = if (isSoftTranslucent) historyIconColor.copy(alpha = 0.15f) else historyIconColor
    val historyIconTint = if (isSoftTranslucent) historyIconColor else Color.White

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(historyContainerBg, RoundedRectangle(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = historyIconTint
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.learn_ai_history_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.learn_ai_history_empty_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryItemCard(
    history: AIExerciseHistory,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(history.createdAt))

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedRectangle(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 类型标签
                val typeTagColor = if (history.type == "CN_TO_JP") {
                    if (isDark) IosColors.BlueDark else IosColors.Blue
                } else {
                    if (isDark) IosColors.PurpleDark else IosColors.Purple
                }
                val typeTagBg = typeTagColor.copy(alpha = if (isDark) 0.16f else 0.1f)
                Surface(
                    color = typeTagBg,
                    shape = RoundedRectangle(6.dp)
                ) {
                    Text(
                        text = if (history.type == "CN_TO_JP") stringResource(R.string.learn_ai_history_cn_to_jp) else stringResource(R.string.learn_ai_history_jp_to_cn),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = typeTagColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = NemoNeutrals.Gray500
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = history.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分数展示
                val scoreColor = when {
                    history.score >= 80 -> if (isDark) IosColors.GreenDark else IosColors.Green
                    history.score >= 60 -> if (isDark) IosColors.OrangeDark else IosColors.Orange
                    else -> if (isDark) IosColors.RedDark else IosColors.Red
                }
                val scoreBgAlpha = if (isDark) 0.16f else 0.1f

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(scoreColor.copy(alpha = scoreBgAlpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${history.score}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = stringResource(R.string.learn_ai_history_difficulty_format, history.difficulty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = NemoNeutrals.Gray400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


