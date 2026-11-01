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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import com.jian.nemo.feature.test.R
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
    SectionTitle(stringResource(R.string.test_settings_section_basic))
    PremiumGroupCard {
        // 1. 题目数量
        PremiumSettingRow(
            label = stringResource(R.string.test_settings_label_question_count),
            value = stringResource(R.string.test_settings_format_question_count, config.questionCount),
            onClick = { onSettingClick("questionCount") }
        )

        // 2. 可用数据量展示
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        val effectiveContentType = if (testModeId == "typing" || testModeId == "card_matching") TestContentType.WORDS else config.testContentType
        val (wordCount, grammarCount) = uiState.availableDataCount ?: (0 to 0)
        val dataCountText = when (effectiveContentType) {
            TestContentType.WORDS -> stringResource(R.string.test_settings_data_available_words_format, if (wordCount >= 1000) "1000+" else "$wordCount")
            TestContentType.GRAMMAR -> stringResource(R.string.test_settings_data_available_grammars_format, if (grammarCount >= 1000) "1000+" else "$grammarCount")
            else -> stringResource(R.string.test_settings_data_available_mixed_format, if (wordCount >= 1000) "1000+" else "$wordCount", if (grammarCount >= 1000) "1000+" else "$grammarCount")
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
            Text(stringResource(R.string.test_common_title_data_overview), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (uiState.isLoadingDataCount) stringResource(R.string.test_settings_data_querying) else if (isInsufficient) stringResource(R.string.test_settings_data_insufficient_format, dataCountText) else dataCountText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (!uiState.isLoadingDataCount && isInsufficient) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // 3. 时间限制
        PremiumSettingRow(
            label = stringResource(R.string.test_settings_label_time_limit),
            value = if (config.timeLimitMinutes == 0) stringResource(R.string.test_settings_time_limit_unlimited) else stringResource(R.string.test_settings_time_limit_minutes_format, config.timeLimitMinutes),
            onClick = { onSettingClick("timeLimit") }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // 4. 题目来源
        val currentSourceLabel = questionSourceOptions.find { it.key == config.questionSource.key }?.title ?: stringResource(R.string.test_settings_source_unknown)
        PremiumSettingRow(
            label = stringResource(R.string.test_settings_label_source),
            value = currentSourceLabel,
            onClick = { onSettingClick("questionSource") }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // 5. 错题移除
        val removalLabel = wrongAnswerRemovalLabels[config.wrongAnswerRemovalThreshold] ?: stringResource(R.string.test_removal_none_title)
        PremiumSettingRow(
            label = stringResource(R.string.test_settings_label_removal),
            value = removalLabel,
            onClick = { onSettingClick("wrongAnswerRemoval") }
        )

        // 6. 内容类型 (如果适用)
        if (testModeId == "multiple_choice" || testModeId == "comprehensive") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            PremiumSettingRow(
                label = stringResource(R.string.test_settings_label_content_type),
                value = currentContentTypeLabel,
                onClick = { onSettingClick("contentType") }
            )
        }


        // 8. 等级选择
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        
        val isRestrictedMode = testModeId in listOf("typing", "card_matching", "sorting")
        
        if (isRestrictedMode || config.testContentType != TestContentType.MIXED) {
            val label = if (isRestrictedMode) stringResource(R.string.test_settings_label_test_level) else if (config.testContentType == TestContentType.WORDS) stringResource(R.string.test_settings_label_word_level) else stringResource(R.string.test_settings_label_grammar_level)
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
                label = stringResource(R.string.test_settings_label_word_level),
                value = formatLevelsDisplay(config.selectedWordLevels, allLevels),
                onClick = { onSettingClick("wordLevel") }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            PremiumSettingRow(
                label = stringResource(R.string.test_settings_label_grammar_level),
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
    SectionTitle(stringResource(R.string.test_settings_section_quiz))
    PremiumGroupCard {
        PremiumSwitchRow(
            label = stringResource(R.string.test_settings_switch_shuffle_questions),
            checked = config.shuffleQuestions,
            onCheckedChange = { onUpdateConfig(config.copy(shuffleQuestions = it)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        PremiumSwitchRow(
            label = stringResource(R.string.test_settings_switch_shuffle_options),
            checked = config.shuffleOptions,
            onCheckedChange = { onUpdateConfig(config.copy(shuffleOptions = it)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        if (testModeId != "card_matching") {
            PremiumSwitchRow(
                label = stringResource(R.string.test_settings_switch_auto_advance),
                checked = config.autoAdvance,
                onCheckedChange = { onUpdateConfig(config.copy(autoAdvance = it)) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        }
        PremiumSwitchRow(
            label = stringResource(R.string.test_settings_switch_prioritize_wrong),
            checked = config.prioritizeWrong,
            onCheckedChange = { onUpdateConfig(config.copy(prioritizeWrong = it, prioritizeNew = if(it) false else config.prioritizeNew)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        PremiumSwitchRow(
            label = stringResource(R.string.test_settings_switch_prioritize_new),
            checked = config.prioritizeNew,
            onCheckedChange = { onUpdateConfig(config.copy(prioritizeNew = it, prioritizeWrong = if(it) false else config.prioritizeWrong)) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        PremiumSwitchRow(
            label = stringResource(R.string.test_settings_switch_auto_play_audio),
            checked = config.autoPlayAudio,
            onCheckedChange = { onUpdateConfig(config.copy(autoPlayAudio = it)) }
        )
    }
}

@Composable
private fun formatLevelsDisplay(levels: List<String>, allLevels: List<String>): String = when {
    levels.size == allLevels.size && levels.containsAll(allLevels) -> stringResource(R.string.test_settings_levels_all)
    levels.isEmpty() -> stringResource(R.string.test_settings_levels_none)
    else -> levels.sorted().joinToString(", ")
}
