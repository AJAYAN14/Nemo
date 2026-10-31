package com.jian.nemo.feature.library.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分类单词列表 ViewModel
 *
 * 管理特定分类下的单词数据

 */
@HiltViewModel
class CategoryWordsViewModel @Inject constructor(
    private val getWordsByPartOfSpeechUseCase: com.jian.nemo.core.domain.usecase.word.GetWordsByPartOfSpeechUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val iconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingIconStyle.MD3
        )

    private val _uiState = MutableStateFlow(CategoryWordsUiState())
    val uiState: StateFlow<CategoryWordsUiState> = _uiState.asStateFlow()

    private var currentCategory: String = ""

    /**
     * 加载指定分类的单词
     */
    fun loadWords(category: String) {
        if (currentCategory == category && _uiState.value.words.isNotEmpty()) {
            return
        }

        currentCategory = category
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 1. 将字符串分类映射为 PartOfSpeech
                val pos = mapCategoryToPos(category)

                // 2. 获取单词列表
                val words = if (pos != null) {
                    getWordsByPartOfSpeechUseCase(pos)
                } else {
                    emptyList()
                }

                // 3. 按等级分组并排序
                // N1 -> N2 -> N3 -> N4 -> N5 -> 其他
                val wordsByLevel = words.groupBy { it.level.uppercase() }
                    .toSortedMap(compareBy { level ->
                        when (level.uppercase()) {
                            "N1" -> 1
                            "N2" -> 2
                            "N3" -> 3
                            "N4" -> 4
                            "N5" -> 5
                            else -> 99
                        }
                    })

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        words = words,
                        wordsByLevel = wordsByLevel,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 刷新单词列表
     */
    fun refresh() {
        if (currentCategory.isNotEmpty()) {
            val category = currentCategory
            currentCategory = "" // 重置以强制重新加载
            loadWords(category)
        }
    }

    /**
     * 映射分类字符串到 PartOfSpeech 枚举
     */
    private fun mapCategoryToPos(category: String): com.jian.nemo.core.domain.model.PartOfSpeech? {
        return when (category) {
            "noun" -> com.jian.nemo.core.domain.model.PartOfSpeech.NOUN
            "adj" -> com.jian.nemo.core.domain.model.PartOfSpeech.ADJECTIVE
            "verb" -> com.jian.nemo.core.domain.model.PartOfSpeech.VERB
            "adv" -> com.jian.nemo.core.domain.model.PartOfSpeech.ADVERB
            "rentai" -> com.jian.nemo.core.domain.model.PartOfSpeech.RENTAI
            "conj" -> com.jian.nemo.core.domain.model.PartOfSpeech.CONJUNCTION
            "exclam" -> com.jian.nemo.core.domain.model.PartOfSpeech.INTERJECTION
            "particle" -> com.jian.nemo.core.domain.model.PartOfSpeech.PARTICLE
            "prefix" -> com.jian.nemo.core.domain.model.PartOfSpeech.PREFIX
            "suffix" -> com.jian.nemo.core.domain.model.PartOfSpeech.SUFFIX
            "expression" -> com.jian.nemo.core.domain.model.PartOfSpeech.FIXED_EXPRESSION
            "kata" -> com.jian.nemo.core.domain.model.PartOfSpeech.LOAN_WORD
            else -> null
        }
    }
}

/**
 * 分类单词列表 UI 状态
 */
data class CategoryWordsUiState(
    /**
     * 是否正在加载
     */
    val isLoading: Boolean = false,

    /**
     * 单词列表
     */
    val words: List<Word> = emptyList(),

    /**
     * 按等级分组的单词列表
     */
    val wordsByLevel: Map<String, List<Word>> = emptyMap(),

    /**
     * 错误消息
     */
    val error: String? = null
)
