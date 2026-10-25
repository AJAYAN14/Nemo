package com.jian.nemo.feature.collection.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.GrammarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 错题本ViewModel
 * 管理单词和语法两种错题
 */
@HiltViewModel
class MistakesViewModel @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MistakesUiState())
    val uiState: StateFlow<MistakesUiState> = _uiState.asStateFlow()

    /**
     * 图标风格（MD3 / Plump Duo），供界面双风格自适应
     */
    val iconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingIconStyle.MD3
        )

    init {
        loadWrongAnswers()
    }

    /**
     * 加载所有错题
     */
    private fun loadWrongAnswers() {
        // 加载知识盲点率所需数据：已学单词数 + 已学语法数
        viewModelScope.launch {
            combine(
                wordRepository.getLearnedWordCount(),
                grammarRepository.getLearnedGrammarCount()
            ) { wordCount, grammarCount ->
                wordCount + grammarCount
            }.collect { totalLearned ->
                _uiState.update {
                    it.copy(totalLearnedCount = totalLearned)
                }
            }
        }

        // 加载单词错题
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            wrongAnswerRepository.getAllWrongAnswers()
                .catch { e ->
                    println("❌ 加载单词错题失败: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "加载单词错题失败: ${e.message}"
                        )
                    }
                }
                .collect { wrongAnswers ->
                    _uiState.update {
                        it.copy(
                            wordWrongAnswers = wrongAnswers,
                            wrongWordsCount = wrongAnswers.size,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }

        // 加载语法错题
        viewModelScope.launch {
            grammarWrongAnswerRepository.getAllWrongAnswers()
                .catch { e ->
                    println("❌ 加载语法错题失败: ${e.message}")
                }
                .collect { grammarWrongAnswers ->
                    _uiState.update {
                        it.copy(
                            grammarWrongAnswers = grammarWrongAnswers,
                            wrongGrammarsCount = grammarWrongAnswers.size
                        )
                    }
                }
        }
    }

    /**
     * 切换Tab
     */
    fun selectTab(tab: MistakeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * 删除单词错题
     */
    fun deleteWordMistake(wordId: Int) {
        viewModelScope.launch {
            when (val result = wrongAnswerRepository.deleteByWordId(wordId)) {
                is Result.Success -> {
                    println("✅ 删除单词错题成功: wordId=$wordId")
                    // Flow会自动更新列表
                }
                is Result.Error -> {
                    println("❌ 删除单词错题失败: ${result.exception.message}")
                    _uiState.update {
                        it.copy(error = "删除失败: ${result.exception.message}")
                    }
                }
                is Result.Loading -> { /* Do nothing */ }
            }
        }
    }

    /**
     * 删除语法错题
     */
    fun deleteGrammarMistake(grammarId: Int) {
        viewModelScope.launch {
            when (val result = grammarWrongAnswerRepository.deleteByGrammarId(grammarId)) {
                is Result.Success -> {
                    println("✅ 删除语法错题成功: grammarId=$grammarId")
                }
                is Result.Error -> {
                    println("❌ 删除语法错题失败: ${result.exception.message}")
                    _uiState.update {
                        it.copy(error = "删除失败: ${result.exception.message}")
                    }
                }
                is Result.Loading -> { /* Do nothing */ }
            }
        }
    }

    /**
     * 清空所有单词错题
     */
    fun clearAllWordMistakes() {
        viewModelScope.launch {
            when (val result = wrongAnswerRepository.clearAll()) {
                is Result.Success -> {
                    println("✅ 清空单词错题成功")
                }
                is Result.Error -> {
                    println("❌ 清空单词错题失败: ${result.exception.message}")
                    _uiState.update {
                        it.copy(error = "清空失败: ${result.exception.message}")
                    }
                }
                is Result.Loading -> { /* Do nothing */ }
            }
        }
    }

    /**
     * 清空所有语法错题
     */
    fun clearAllGrammarMistakes() {
        viewModelScope.launch {
            when (val result = grammarWrongAnswerRepository.clearAll()) {
                is Result.Success -> {
                    println("✅ 清空语法错题成功")
                }
                is Result.Error -> {
                    println("❌ 清空语法错题失败: ${result.exception.message}")
                    _uiState.update {
                        it.copy(error = "清空失败: ${result.exception.message}")
                    }
                }
                is Result.Loading -> { /* Do nothing */ }
            }
        }
    }

    /**
     * 清空所有错题（单词+语法）
     */
    fun clearAllWrongAnswers() {
        clearAllWordMistakes()
        clearAllGrammarMistakes()
    }


    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
