package com.jian.nemo.feature.test.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.TestContentType

@Composable
fun BasicSettingsSection(
    config: TestConfig,
    testModeId: String?,
    uiState: com.jian.nemo.feature.test.presentation.settings.TestSettingsUiState,
    questionSourceOptions: List<SingleSelectOptionItem<String>>,
    wrongAnswerRemovalLabels: Map<Int, String>,
    currentContentTypeLabel: String,
    onSettingClick: (String) -> Unit
) {
    SectionTitle("基础设置")
    PremiumGroupCard {
        // 1. 题目数量
        PremiumSettingRow(
            label = "题目数量",
            value = "${config.questionCount} 题",
            onClick = { onSettingClick("questionCount") }
        )

        // 2. 可用数据量展示
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        val effectiveContentType = if (testModeId == "typing" || testModeId == "card_matching") TestContentType.WORDS else config.testContentType
        val (wordCount, grammarCount) = uiState.availableDataCount ?: (0 to 0)
        val dataCountText = when (effectiveContentType) {
            TestContentType.WORDS -> "可用: ${if (wordCount >= 1000) "1000+" else "$wordCount"} 词"
            TestContentType.GRAMMAR -> "可用: ${if (grammarCount >= 1000) "1000+" else "$grammarCount"} 语法"
            else -> "可用: ${if (wordCount >= 1000) "1000+" else "$wordCount"} 词 / ${if (grammarCount >= 1000) "1000+" else "$grammarCount"} 语法"
        }
        val isInsufficient = when (effectiveContentType) {
            TestContentType.WORDS -> wordCount < config.questionCount
            TestContentType.GRAMMAR -> grammarCount < config.questionCount
            else -> (wordCount + grammarCount) < config.questionCount
        }

        // 特殊的信息行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text("数据概况", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (uiState.isLoadingDataCount) "查询中..." else if (isInsufficient) "$dataCountText (不足)" else dataCountText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (!uiState.isLoadingDataCount && isInsufficient) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // 3. 时间限制
        PremiumSettingRow(
            label = "时间限制",
            value = if (config.timeLimitMinutes == 0) "无限制" else "${config.timeLimitMinutes} 分钟",
            onClick = { onSettingClick("timeLimit") }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // 4. 题目来源
        val currentSourceLabel = questionSourceOptions.find { it.key == config.questionSource.key }?.title ?: "未知"
        PremiumSettingRow(
            label = "题目来源",
            value = currentSourceLabel,
            onClick = { onSettingClick("questionSource") }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // 5. 错题移除
        val removalLabel = wrongAnswerRemovalLabels[config.wrongAnswerRemovalThreshold] ?: "不移除"
        PremiumSettingRow(
            label = "错题移除",
            value = removalLabel,
            onClick = { onSettingClick("wrongAnswerRemoval") }
        )

        // 6. 内容类型 (如果适用)
        if (testModeId == "multiple_choice" || testModeId == "comprehensive") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            PremiumSettingRow(
                label = "测试内容",
                value = currentContentTypeLabel,
                onClick = { onSettingClick("contentType") }
            )
        }


        // 8. 等级选择
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        
        val isRestrictedMode = testModeId in listOf("typing", "card_matching", "sorting")
        
        if (isRestrictedMode || config.testContentType != TestContentType.MIXED) {
            val label = if (isRestrictedMode) "测试等级" else if (config.testContentType == TestContentType.WORDS) "单词等级" else "语法等级"
            val levels = if (config.testContentType == TestContentType.GRAMMAR && !isRestrictedMode) config.selectedGrammarLevels else config.selectedWordLevels
            val settingType = if (config.testContentType == TestContentType.GRAMMAR && !isRestrictedMode) "grammarLevel" else "wordLevel"
            
            val allLevels = listOf("N5", "N4", "N3", "N2", "N1")
            
            PremiumSettingRow(
                label = label,
                value = formatLevelsDisplay(levels, allLevels),
                onClick = { onSettingClick(settingType) }
            )
        } else {
            // Mixed: Show both
            val allLevels = listOf("N5", "N4", "N3", "N2", "N1")
            
            PremiumSettingRow(
                label = "单词等级",
                value = formatLevelsDisplay(config.selectedWordLevels, allLevels),
                onClick = { onSettingClick("wordLevel") }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            PremiumSettingRow(
                label = "语法等级",
                value = formatLevelsDisplay(config.selectedGrammarLevels, allLevels),
                onClick = { onSettingClick("grammarLevel") }
            )
        }
    }
}

@Composable
fun QuizSettingsSection(
    config: TestConfig,
    testModeId: String?,
    onUpdateConfig: (TestConfig) -> Unit
) {
    SectionTitle("答题设置")
    PremiumGroupCard {
        PremiumSwitchRow(
            label = "题目乱序",
            checked = config.shuffleQuestions,
            onCheckedChange = { onUpdateConfig(config.copy(shuffleQuestions = it)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        PremiumSwitchRow(
            label = "选项乱序",
            checked = config.shuffleOptions,
            onCheckedChange = { onUpdateConfig(config.copy(shuffleOptions = it)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        if (testModeId != "card_matching") {
            PremiumSwitchRow(
                label = "自动跳转",
                checked = config.autoAdvance,
                onCheckedChange = { onUpdateConfig(config.copy(autoAdvance = it)) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        }
        PremiumSwitchRow(
            label = "错题优先",
            checked = config.prioritizeWrong,
            onCheckedChange = { onUpdateConfig(config.copy(prioritizeWrong = it, prioritizeNew = if(it) false else config.prioritizeNew)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        PremiumSwitchRow(
            label = "未做题优先",
            checked = config.prioritizeNew,
            onCheckedChange = { onUpdateConfig(config.copy(prioritizeNew = it, prioritizeWrong = if(it) false else config.prioritizeWrong)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        PremiumSwitchRow(
            label = "答题自动朗读假名",
            checked = config.autoPlayAudio,
            onCheckedChange = { onUpdateConfig(config.copy(autoPlayAudio = it)) }
        )
    }
}

private fun formatLevelsDisplay(levels: List<String>, allLevels: List<String>): String = when {
    levels.size == allLevels.size && levels.containsAll(allLevels) -> "全部等级"
    levels.isEmpty() -> "未选择"
    else -> levels.sorted().joinToString(", ")
}
