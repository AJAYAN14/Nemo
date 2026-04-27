package com.jian.nemo.feature.test.presentation.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * BottomSheet 选择器组件 - UI/UX Pro Max 风格
 *
 * 统一采用 Grid (Column + Row) + Squircle Chips 设计
 */

/**
 * 题目数量选择器
 */
@Composable
fun QuestionCountSelector(
    options: List<Int>,
    currentValue: Int,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
    onCancel: () -> Unit
) {
    SelectorHeader("选择题目数量")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val allOptionsWithCustom = options.map { it to "$it 题" }

        // Split into chunks of 3 for grid layout
        allOptionsWithCustom.chunked(3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { (count, label) ->
                    PremiumSelectorChip(
                        text = label,
                        selected = count == currentValue,
                        onClick = { onSelect(count) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if last row is incomplete
                if (rowOptions.size < 3) {
                    repeat(3 - rowOptions.size) {
                       Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Custom button as a separate full-width row
        PremiumCustomChip(onClick = onCustom)
    }

    Spacer(modifier = Modifier.height(24.dp))
    CancelButton(onCancel)
}

/**
 * 时间限制选择器
 */
@Composable
fun TimeLimitSelector(
    options: List<Int>,
    currentValue: Int,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
    onCancel: () -> Unit
) {
    SelectorHeader("选择时间限制")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val displayOptions = options.map {
            it to if (it == 0) "无限制" else "$it 分钟"
        }

        displayOptions.chunked(3).forEach { rowOptions ->
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { (minutes, label) ->
                    PremiumSelectorChip(
                        text = label,
                        selected = minutes == currentValue,
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.weight(1f)
                    )
                }
                 if (rowOptions.size < 3) {
                    repeat(3 - rowOptions.size) {
                       Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        PremiumCustomChip(onClick = onCustom)
    }
    Spacer(modifier = Modifier.height(24.dp))
    CancelButton(onCancel)
}

/**
 * 题目来源选择器
 */
@Composable
fun QuestionSourceSelector(
    options: List<Pair<String, String>>,
    currentValue: String,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit
) {
    SelectorHeader("选择题目来源")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            PremiumSelectorChip(
                text = option.first,
                selected = option.second == currentValue,
                onClick = { onSelect(option.second) },
                modifier = Modifier.fillMaxWidth() // List style for longer text
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    CancelButton(onCancel)
}

/**
 * 测试内容类型选择器
 */
@Composable
fun ContentTypeSelector(
    options: List<Pair<String, String>>,
    currentValue: String,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit
) {
    SelectorHeader("选择测试内容类型")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            PremiumSelectorChip(
                text = option.first,
                selected = option.second == currentValue,
                onClick = { onSelect(option.second) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    CancelButton(onCancel)
}

/**
 * 错题移除阈值选择器
 */
@Composable
fun WrongAnswerRemovalSelector(
    options: List<Int>,
    labels: Map<Int, String>,
    currentValue: Int,
    onSelect: (Int) -> Unit,
    onCancel: () -> Unit
) {
    SelectorHeader("答对几次后从错题中移除")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val displayOptions = options.map { it to (labels[it] ?: "") }

        displayOptions.chunked(3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { (threshold, label) ->
                    PremiumSelectorChip(
                        text = label,
                        selected = threshold == currentValue,
                        onClick = { onSelect(threshold) },
                        modifier = Modifier.weight(1f)
                    )
                }
                 if (rowOptions.size < 3) {
                    repeat(3 - rowOptions.size) {
                       Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    CancelButton(onCancel)
}

/**
 * 综合测试题型分布编辑器 - UI/UX Pro Max 升级版
 */
@Composable
fun QuestionTypeCountEditor(
    comprehensiveCounts: Map<String, Int>,
    questionCountLimit: Int,
    isQuestionTypeSupported: (String) -> Boolean,
    onCountChange: (String, Int) -> Unit,
    onCancel: () -> Unit
) {
    val totalCount = comprehensiveCounts.values.sum()
    val isOverLimit = totalCount > questionCountLimit

    SelectorHeader("设置题型题数")

    // 🎭 顶部统计区 - Flat UI 风格
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(26.dp), // 对齐 PremiumCard 圆角
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "当前总题数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "$totalCount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 分割线
            Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "限制",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "$questionCountLimit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    if (isOverLimit) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text(
                text = "⚠️ 总题数已超过限制",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    // 📋 题型卡片列表
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            "选择题" to "multiple_choice",
            "手打题" to "typing",
            "卡片题" to "card_matching",
            "排序题" to "sorting"
        ).forEach { (typeName, typeKey) ->
            val isSupported = isQuestionTypeSupported(typeKey)
            val currentCount = comprehensiveCounts[typeKey] ?: 0

            QuestionTypeCountRow(
                typeName = typeName,
                typeKey = typeKey,
                count = currentCount,
                isSupported = isSupported,
                onDecrease = {
                    if (currentCount > 0) onCountChange(typeKey, currentCount - 1)
                },
                onIncrease = {
                    onCountChange(typeKey, currentCount + 1)
                }
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    CancelButton(onCancel)
}

// ===== Common Components =====

@Composable
private fun SelectorHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PremiumSelectorChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp), // Squircle-ish
        color = containerColor,
        modifier = modifier.height(48.dp) // Fixed height for consistency
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium, // Larger font
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun PremiumCustomChip(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(48.dp).fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text("自定义...", fontWeight = FontWeight.SemiBold)
    }
}

/**
 * 题型数量行组件 - 卡片式设计
 */
@Composable
private fun QuestionTypeCountRow(
    typeName: String,
    typeKey: String,
    count: Int,
    isSupported: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        color = if (isSupported) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(26.dp), // 对齐 PremiumCard 圆角
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isSupported && count > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        shadowElevation = if (isSupported && count > 0) 4.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 🏷️ 左侧描述区
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold, // 强化标题感
                    color = if (isSupported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (typeKey == "card_matching" && isSupported) {
                    Text(
                        text = "每组5个单词",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                if (!isSupported) {
                    val reasonText = when (typeKey) {
                        "typing" -> "语法/无假名拼写场景暂不支持"
                        "card_matching" -> "语法场景暂不支持"
                        else -> "暂不支持此题型"
                    }
                    Text(
                        text = reasonText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }

            // 🔢 右侧步进器 - Flat Capsule 风格
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrease,
                        enabled = isSupported && count > 0,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "减少",
                            tint = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$count",
                        modifier = Modifier.widthIn(min = 34.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Black,
                        color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = onIncrease,
                        enabled = isSupported,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "增加",
                            tint = if (isSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 等级选择器内容
 */
@Composable
fun LevelSelector(
    title: String,
    allLevels: List<String>,
    selectedLevels: List<String>,
    availableLevels: List<Pair<String, Int>>,
    needsRestriction: Boolean,
    emptyMessage: String,
    isAllSelected: Boolean, // 由外部 ViewModel 控制逻辑
    onLevelToggle: (String) -> Unit,
    onLevelExclusive: (String) -> Unit, // 新增：仅选此项
    onSelectAll: () -> Unit,
    onDismiss: () -> Unit,
    snackbarAction: (String) -> Unit
) {
    SelectorHeader(title)

    // 数据量统计
    val totalCount = remember(availableLevels) { availableLevels.sumOf { it.second } }

    // 合并“全部”选项到网格中，总共 6 个项目 (全部, N5, N4, N3, N2, N1)
    val displayLevels = remember(allLevels) { listOf("全部") + allLevels }
    val rows = remember(displayLevels) { displayLevels.chunked(2) }

    // 等级网格 (双列布局，刚好 3 行)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEachIndexed { rowIndex, rowLevels ->
            androidx.compose.runtime.key(rowIndex) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowLevels.forEach { levelKey ->
                        androidx.compose.runtime.key(levelKey) {
                            if (levelKey == "全部") {
                                PremiumLevelChip(
                                    level = "全部等级",
                                    count = totalCount,
                                    isSelected = isAllSelected,
                                    isAvailable = true,
                                    onClick = onSelectAll,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                val isSelected = selectedLevels.contains(levelKey)
                                val isAvailable = remember(needsRestriction, availableLevels, levelKey) {
                                    if (needsRestriction) {
                                        availableLevels.any { it.first == levelKey }
                                    } else {
                                        true
                                    }
                                }
                                val count = remember(availableLevels, levelKey) {
                                    availableLevels.find { it.first == levelKey }?.second ?: 0
                                }

                                PremiumLevelChip(
                                    level = levelKey,
                                    count = count,
                                    isSelected = isSelected,
                                    isAvailable = isAvailable,
                                    onClick = {
                                        if (!isAvailable) {
                                            snackbarAction("该等级在当前题源下没有可用内容")
                                        } else {
                                            onLevelToggle(levelKey)
                                        }
                                    },
                                    onLongClick = {
                                        if (isAvailable) {
                                            onLevelExclusive(levelKey)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (rowLevels.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    // 完成按钮使用主色强调
    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text("确定", fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/**
 * 现代等级选择卡片 (多选风格)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PremiumLevelChip(
    level: String,
    count: Int,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val targetContainerColor = when {
        !isAvailable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }

    val targetContentColor = when {
        !isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetContainerColor,
        label = "chip_container_color"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetContentColor,
        label = "chip_content_color"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = modifier
            .height(64.dp)
            .alpha(if (isAvailable) 1f else 0.6f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = level,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
                Text(
                    text = if (isAvailable) "$count 词" else "不可用",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

/**
 * 取消按钮（通用）
 */
@Composable
fun CancelButton(onClick: () -> Unit) {
    Text(
        text = "取消",
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center
    )
}
