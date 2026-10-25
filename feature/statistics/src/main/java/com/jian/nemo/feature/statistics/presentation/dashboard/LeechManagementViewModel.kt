package com.jian.nemo.feature.statistics.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.usecase.grammar.GetSkippedGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.RecoverLeechGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.GetSkippedWordsUseCase
import com.jian.nemo.core.domain.usecase.word.RecoverLeechWordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import javax.inject.Inject

data class LeechManagementUiState(
    val skippedWords: List<Word> = emptyList(),
    val skippedGrammars: List<Grammar> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTab: LeechTab = LeechTab.Word,
    val error: String? = null,
    val successMessage: String? = null,
    val settingIconStyle: SettingIconStyle = SettingIconStyle.MD3
)

enum class LeechTab { Word, Grammar }

sealed class LeechEvent {
    data class TabChanged(val tab: LeechTab) : LeechEvent()
    data class RecoverWord(val id: Int) : LeechEvent()
    data class RecoverGrammar(val id: Int) : LeechEvent()
    object ClearMessages : LeechEvent()
}

@HiltViewModel
class LeechManagementViewModel @Inject constructor(
    private val getSkippedWordsUseCase: GetSkippedWordsUseCase,
    private val getSkippedGrammarsUseCase: GetSkippedGrammarsUseCase,
    private val recoverLeechWordUseCase: RecoverLeechWordUseCase,
    private val recoverLeechGrammarUseCase: RecoverLeechGrammarUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeechManagementUiState())
    val uiState: StateFlow<LeechManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingIconStyleFlow.collect { style ->
                _uiState.update { it.copy(settingIconStyle = style) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 同时监听单词和语法的变动
            combine(
                getSkippedWordsUseCase(),
                getSkippedGrammarsUseCase()
            ) { words, grammars ->
                _uiState.update {
                    it.copy(
                        skippedWords = words,
                        skippedGrammars = grammars,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onEvent(event: LeechEvent) {
        when (event) {
            is LeechEvent.TabChanged -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
            }
            is LeechEvent.RecoverWord -> recoverWord(event.id)
            is LeechEvent.RecoverGrammar -> recoverGrammar(event.id)
            LeechEvent.ClearMessages -> {
                _uiState.update { it.copy(error = null, successMessage = null) }
            }
        }
    }

    private fun recoverWord(id: Int) {
        viewModelScope.launch {
            val result = recoverLeechWordUseCase(id)
            if (result is Result.Success) {
                _uiState.update { it.copy(successMessage = "单词已恢复至学习队列") }
            } else {
                _uiState.update { it.copy(error = "恢复失败") }
            }
        }
    }

    private fun recoverGrammar(id: Int) {
        viewModelScope.launch {
            val result = recoverLeechGrammarUseCase(id)
            if (result is Result.Success) {
                _uiState.update { it.copy(successMessage = "语法已恢复至学习队列") }
            } else {
                _uiState.update { it.copy(error = "恢复失败") }
            }
        }
    }
}
