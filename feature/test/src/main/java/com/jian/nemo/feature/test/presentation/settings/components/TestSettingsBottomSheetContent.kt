package com.jian.nemo.feature.test.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.TestContentType
import com.jian.nemo.feature.test.presentation.settings.TestSettingsUiState

@Composable
fun TestSettingsBottomSheetContent(
    currentSetting: String,
    config: TestConfig,
    uiState: TestSettingsUiState,
    testModeId: String?,
    questionCountOptions: List<Int>,
    timeLimitOptions: List<Int>,
    questionSourceOptions: List<SingleSelectOptionItem<String>>,
    wrongAnswerRemovalOptions: List<SingleSelectOptionItem<Int>>,
    contentTypeOptions: List<SingleSelectOptionItem<String>>,
    allLevels: List<String>,
    onUpdateConfig: (TestConfig) -> Unit,
    isQuestionTypeSupported: (String) -> Boolean,
    onToggleLevel: (String, Boolean) -> Unit,
    onExclusiveSelectLevel: (String, Boolean) -> Unit,
    onToggleAllLevels: (Boolean) -> Unit,
    onShowCustomQuestionCount: () -> Unit,
    onShowCustomTimeLimit: () -> Unit,
    onDismiss: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        when (currentSetting) {
            "questionCount" -> QuestionCountSelector(
                options = questionCountOptions,
                currentValue = config.questionCount,
                onSelect = { count ->
                    onUpdateConfig(config.copy(questionCount = count))
                    onDismiss()
                },
                onCustom = { onDismiss(); onShowCustomQuestionCount() },
                onCancel = { onDismiss() }
            )

            "timeLimit" -> TimeLimitSelector(
                options = timeLimitOptions,
                currentValue = config.timeLimitMinutes,
                onSelect = {
                    onUpdateConfig(config.copy(timeLimitMinutes = it))
                    onDismiss()
                },
                onCustom = { onDismiss(); onShowCustomTimeLimit() },
                onCancel = { onDismiss() }
            )
            "questionSource" -> QuestionSourceSelector(
                options = questionSourceOptions,
                currentValue = config.questionSource.key,
                onSelect = {
                    onUpdateConfig(config.copy(questionSource = QuestionSource.fromKey(it)))
                    onDismiss()
                },
                onCancel = { onDismiss() }
            )
            "wrongAnswerRemoval" -> WrongAnswerRemovalSelector(
                options = wrongAnswerRemovalOptions,
                currentValue = config.wrongAnswerRemovalThreshold,
                onSelect = {
                    onUpdateConfig(config.copy(wrongAnswerRemovalThreshold = it))
                    onDismiss()
                },
                onCancel = { onDismiss() }
            )
            "contentType" -> ContentTypeSelector(
                options = contentTypeOptions,
                currentValue = config.testContentType.key,
                onSelect = {
                    onUpdateConfig(config.copy(testContentType = TestContentType.fromKey(it)))
                    onDismiss()
                },
                onCancel = { onDismiss() }
            )
            "wordLevel" -> {
                val currentLevels = config.selectedWordLevels
                val available = uiState.availableWordLevels
                
                LevelSelector(
                    title = "选择单词等级",
                    allLevels = allLevels,
                    selectedLevels = currentLevels,
                    availableLevels = available,
                    needsRestriction = true,
                    emptyMessage = "当前题源下没有可用的单词等级",
                    isAllSelected = uiState.isAllWordLevelsSelected,
                    onLevelToggle = { onToggleLevel(it, false) },
                    onLevelExclusive = { onExclusiveSelectLevel(it, false) },
                    onSelectAll = { onToggleAllLevels(false) },
                    onDismiss = { onDismiss() },
                    snackbarAction = { onSnackbar(it) }
                )
            }
            "grammarLevel" -> {
                val currentLevels = config.selectedGrammarLevels
                val available = uiState.availableGrammarLevels
                
                LevelSelector(
                    title = "选择语法等级",
                    allLevels = allLevels,
                    selectedLevels = currentLevels,
                    availableLevels = available,
                    needsRestriction = config.questionSource != QuestionSource.ALL,
                    emptyMessage = "当前题源下没有可用的语法等级",
                    isAllSelected = uiState.isAllGrammarLevelsSelected,
                    onLevelToggle = { onToggleLevel(it, true) },
                    onLevelExclusive = { onExclusiveSelectLevel(it, true) },
                    onSelectAll = { onToggleAllLevels(true) },
                    onDismiss = { onDismiss() },
                    snackbarAction = { onSnackbar(it) }
                )
            }
        }
    }
}
