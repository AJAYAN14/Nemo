package com.jian.nemo.feature.collection.mistakes

import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import com.jian.nemo.feature.collection.R
import androidx.compose.ui.Modifier
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.model.GrammarWrongAnswer
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.NemoDialog

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
    val iconStyle by viewModel.iconStyle.collectAsState()

    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val accentBlue = IosColors.Blue
    val deleteColor = IosColors.Red
    val emptyIconColor = IosColors.Green
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // 多选状态
    var selectedGrammarIds by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val isSelectionMode = selectedGrammarIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 拦截物理返回
    BackHandler(enabled = isSelectionMode) {
        selectedGrammarIds = emptySet()
    }

    if (showDeleteDialog) {
        NemoDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.collection_dialog_remove_mistake_title),
            text = stringResource(R.string.collection_dialog_remove_grammars_format, selectedGrammarIds.size),
            confirmText = stringResource(R.string.collection_btn_confirm_remove_mistake),
            dismissText = stringResource(R.string.collection_btn_cancel),
            isDangerous = true,
            onConfirm = {
                viewModel.deleteWrongGrammars(selectedGrammarIds)
                selectedGrammarIds = emptySet()
                showDeleteDialog = false
            }
        )
    }

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NemoChasingDotsLoader()
                    }
                }
                uiState.wrongAnswers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val emptyBgColor = if (isSoftTranslucent) emptyIconColor.copy(alpha = 0.15f) else emptyIconColor
                            val emptyTint = if (isSoftTranslucent) emptyIconColor else Color.White
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(emptyBgColor, RoundedRectangle(28.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = emptyTint,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(stringResource(R.string.collection_mistakes_empty_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.collection_mistakes_empty_desc), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
                else -> {
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items = uiState.wrongAnswers, key = { "mistake_${it.id}" }) { mistake ->
                            val isSelected = selectedGrammarIds.contains(mistake.grammarId)
                            Box(modifier = Modifier.animateListItem()) {
                                WrongGrammarCard(
                                    mistake = mistake,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onSelectedChange = { checked ->
                                        selectedGrammarIds = if (checked) {
                                            selectedGrammarIds + mistake.grammarId
                                        } else {
                                            selectedGrammarIds - mistake.grammarId
                                        }
                                    },
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedGrammarIds = if (isSelected) {
                                                selectedGrammarIds - mistake.grammarId
                                            } else {
                                                selectedGrammarIds + mistake.grammarId
                                            }
                                        } else {
                                            mistake.grammar?.id?.let { onGrammarClick(it) }
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedGrammarIds = selectedGrammarIds + mistake.grammarId
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
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
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FrostedLiquidGlassButton(
                            onClick = { selectedGrammarIds = emptySet() },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.collection_cd_cancel_selection),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.collection_selected_items_format, selectedGrammarIds.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        val allGrammarIds = uiState.wrongAnswers.map { it.grammarId }.toSet()
                        val isAllSelected = selectedGrammarIds.size == allGrammarIds.size
                        TextButton(
                            onClick = {
                                selectedGrammarIds = if (isAllSelected) emptySet() else allGrammarIds
                            }
                        ) {
                            Text(
                                text = if (isAllSelected) stringResource(R.string.collection_btn_deselect_all) else stringResource(R.string.collection_btn_select_all),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentBlue
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FrostedLiquidGlassButton(
                            onClick = { showDeleteDialog = true },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.collection_cd_batch_delete),
                                tint = deleteColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    com.jian.nemo.core.ui.component.common.CommonHeader(
                        title = stringResource(R.string.collection_mistakes_grammars_title),
                        onBack = onNavigateBack,
                        backgroundColor = Color.Transparent,
                        backdrop = backdrop,
                        backButtonSize = 48.dp
                    )
                }
            }
        }
    }
}

/**
 * 获取 JLPT 等级对应颜色（统一收拢至标准设计色彩手册 IosColors）
 */
private fun getLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "N5" -> IosColors.Green
        "N4" -> IosColors.Cyan
        "N3" -> IosColors.Blue
        "N2" -> IosColors.Orange
        "N1" -> IosColors.Pink
        else -> IosColors.Indigo
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WrongGrammarCard(
    mistake: GrammarWrongAnswer,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val accentBlue = IosColors.Blue
    val accentRed = IosColors.Red
    val accentGreen = IosColors.Green
    val tagColor = getLevelColor(mistake.grammar?.grammarLevel ?: "")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 20.dp, isDark = isDark)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedRectangle(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isSelected) BorderStroke(2.dp, accentBlue) else null,
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
                        checkedColor = accentBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // 主要内容 Column
            Column(modifier = Modifier.weight(1f)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedRectangle(8.dp),
                        color = tagColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = mistake.grammar?.grammarLevel ?: "N/A",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tagColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mistake.grammar?.grammar ?: stringResource(R.string.collection_unknown_grammar),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!isSelectionMode) {
                        Icon(Icons.Rounded.Cancel, null, tint = accentRed, modifier = Modifier.size(20.dp))
                    }
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
                                withStyle(style = SpanStyle(color = accentRed, fontWeight = FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
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
                    shape = RoundedRectangle(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Close, null, tint = accentRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.collection_your_answer_format_colon, mistake.userAnswer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = accentRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null, tint = accentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.collection_correct_answer_format_colon, mistake.correctAnswer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = accentGreen,
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
                            Text(
                                text = if (expanded) stringResource(R.string.collection_collapse_explanation) else stringResource(R.string.collection_expand_explanation),
                                style = MaterialTheme.typography.labelLarge,
                                color = accentBlue
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
