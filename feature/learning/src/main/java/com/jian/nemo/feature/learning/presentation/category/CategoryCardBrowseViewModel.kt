package com.jian.nemo.feature.learning.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.jian.nemo.feature.learning.data.preferences.CategoryLearningPreferences
import com.jian.nemo.core.data.audio.TtsManager

enum class SlideDirection {
    FORWARD,  // 向前（下一个）
    BACKWARD  // 向后（上一个）
}

data class CategoryCardLearningUiState(
    val isLoading: Boolean = true,
    val words: List<Word> = emptyList(),
    val currentWordIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isProcessingClick: Boolean = false,
    val slideDirection: SlideDirection = SlideDirection.FORWARD,
    val error: String? = null,
    val navigationHistory: List<Int> = emptyList() // 访问历史栈，用于返回上一个位置
) {
    val currentWord: Word?
        get() = words.getOrNull(currentWordIndex)

    val hasNext: Boolean
        get() = currentWordIndex < words.size - 1

    val hasPrevious: Boolean
        get() = currentWordIndex > 0

    val canGoBack: Boolean
        get() = navigationHistory.isNotEmpty()
}



@HiltViewModel(assistedFactory = CategoryCardLearningViewModel.Factory::class)
class CategoryCardLearningViewModel @AssistedInject constructor(
    private val repository: WordRepository,
    private val preferences: CategoryLearningPreferences,
    private val ttsManager: TtsManager,
    @Assisted private val categoryId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryCardLearningUiState())
    val uiState: StateFlow<CategoryCardLearningUiState> = _uiState.asStateFlow()

    @AssistedFactory
    interface Factory {
        fun create(categoryId: String): CategoryCardLearningViewModel
    }

    init {
        loadCategoryWords()
        initializeTts()
    }

    private fun initializeTts() {
        viewModelScope.launch {
            ttsManager.initialize()
        }
    }

    private fun loadCategoryWords() {
        viewModelScope.launch {
            try {
                _uiState.value = CategoryCardLearningUiState(isLoading = true)
                val words = when (categoryId) {
                    // 1. 名词类（noun）：名、代、連語（名词型）
                    "noun" -> repository.getWordsByPartOfSpeech(PartOfSpeech.NOUN)
                    // 2. 形容词类（adj）：包括一类形容词（イ形）和二类形容词（ナ形）
                    "adj" -> repository.getWordsByPartOfSpeech(PartOfSpeech.ADJECTIVE)
                    // 3. 副词（adv）：副
                    "adv" -> repository.getWordsByPartOfSpeech(PartOfSpeech.ADVERB)
                    // 4. 连体词（rentai）：連体
                    "rentai" -> repository.getWordsByPartOfSpeech(PartOfSpeech.RENTAI)
                    // 5. 接续词（conj）：接（排除接頭/接尾）
                    "conj" -> repository.getWordsByPartOfSpeech(PartOfSpeech.CONJUNCTION)
                    // 6. 感叹词（exclam）：嘆、喫
                    "exclam" -> repository.getWordsByPartOfSpeech(PartOfSpeech.INTERJECTION)
                    // 7. 助词（particle）：助
                    "particle" -> repository.getWordsByPartOfSpeech(PartOfSpeech.PARTICLE)
                    // 8. 接头词（prefix）：接頭、御〜
                    "prefix" -> repository.getWordsByPartOfSpeech(PartOfSpeech.PREFIX)
                    // 9. 接尾词（suffix）：接尾
                    "suffix" -> repository.getWordsByPartOfSpeech(PartOfSpeech.SUFFIX)
                    // 10. 动词类（verb）：自動1/2/3、他動1/2/3、自他動1/2/3
                    "verb" -> repository.getWordsByPartOfSpeech(PartOfSpeech.VERB)
                    // 11. 表达・固定句型（expression）：連語（固定表达）
                    "expression" -> repository.getWordsByPartOfSpeech(PartOfSpeech.FIXED_EXPRESSION)
                    // 12. 外来语（kata）：片假名拼写判断（非POS）
                    "kata" -> repository.getLoanWords()
                    else -> emptyList()
                }


                val sortedWords = words.sortedBy { it.id } // 按ID从小到大排序

                // Restore last index
                val lastIndex = preferences.getLastIndex(categoryId)
                val safeIndex = if (lastIndex in sortedWords.indices) lastIndex else 0

                _uiState.value = CategoryCardLearningUiState(
                    isLoading = false,
                    words = sortedWords,
                    currentWordIndex = safeIndex,
                    navigationHistory = listOf(safeIndex) // 初始化时，当前位置加入历史
                )
            } catch (e: Exception) {
                _uiState.value = CategoryCardLearningUiState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * 翻转卡片
     */
    fun flipCard() {
        val currentState = _uiState.value
        if (!currentState.isProcessingClick) {
            val newFlippedState = !currentState.isFlipped
            _uiState.value = currentState.copy(isFlipped = newFlippedState)
            
            // 翻转到背面时自动朗读（如果需要显示背面信息）
            // 或者翻转动作本身触发朗读
            speakCurrentWord()
        }
    }

    /**
     * 朗读当前单词
     */
    fun speakCurrentWord() {
        uiState.value.currentWord?.let { word ->
            ttsManager.speak(word.japanese)
        }
    }

    /**
     * 朗读指定文本
     */
    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    /**
     * 下一个单词
     */
    fun nextWord() {
        val currentState = _uiState.value
        if (currentState.hasNext && !currentState.isProcessingClick) {
            val newIndex = currentState.currentWordIndex + 1
            // 将当前位置加入历史栈（如果当前位置不在栈顶）
            val newHistory = if (currentState.navigationHistory.lastOrNull() != currentState.currentWordIndex) {
                currentState.navigationHistory + currentState.currentWordIndex
            } else {
                currentState.navigationHistory
            }
            _uiState.value = currentState.copy(
                currentWordIndex = newIndex,
                isFlipped = false,
                isProcessingClick = true,
                slideDirection = SlideDirection.FORWARD,
                navigationHistory = newHistory
            )
            // Save progress
            preferences.saveLastIndex(categoryId, newIndex)

            // 重置处理状态
            viewModelScope.launch {
                delay(400) // 等待动画完成
                _uiState.value = _uiState.value.copy(isProcessingClick = false)
            }
        }
    }

    /**
     * 上一个单词（顺序上一个）
     */
    fun previousWord() {
        val currentState = _uiState.value
        if (currentState.hasPrevious && !currentState.isProcessingClick) {
            val newIndex = currentState.currentWordIndex - 1
            // 将当前位置加入历史栈（如果当前位置不在栈顶）
            val newHistory = if (currentState.navigationHistory.lastOrNull() != currentState.currentWordIndex) {
                currentState.navigationHistory + currentState.currentWordIndex
            } else {
                currentState.navigationHistory
            }
            _uiState.value = currentState.copy(
                currentWordIndex = newIndex,
                isFlipped = false,
                isProcessingClick = true,
                slideDirection = SlideDirection.BACKWARD,
                navigationHistory = newHistory
            )
            // Save progress
            preferences.saveLastIndex(categoryId, newIndex)

            // 重置处理状态
            viewModelScope.launch {
                delay(400) // 等待动画完成
                _uiState.value = _uiState.value.copy(isProcessingClick = false)
            }
        }
    }

    /**
     * 返回上一个访问的位置（用于抽屉中的左箭头按钮）
     */
    fun goBack() {
        val currentState = _uiState.value
        if (currentState.canGoBack && !currentState.isProcessingClick) {
            val previousHistory = currentState.navigationHistory.dropLast(1)
            val previousIndex = currentState.navigationHistory.lastOrNull() ?: currentState.currentWordIndex

            _uiState.value = currentState.copy(
                currentWordIndex = previousIndex,
                isFlipped = false,
                isProcessingClick = true,
                slideDirection = SlideDirection.BACKWARD,
                navigationHistory = previousHistory
            )
            // Save progress
            preferences.saveLastIndex(categoryId, previousIndex)

            // 重置处理状态
            viewModelScope.launch {
                delay(400) // 等待动画完成
                _uiState.value = _uiState.value.copy(isProcessingClick = false)
            }
        }
    }

    /**
     * 跳过当前单词
     */
    fun skipWord() {
        val currentState = _uiState.value
        if (!currentState.isProcessingClick) {
            nextWord()
        }
    }

    /**
     * 跳转到指定序号的单词（序号从1开始）
     */
    fun jumpToWord(sequenceNumber: Int) {
        val currentState = _uiState.value
        val targetIndex = sequenceNumber - 1 // 转换为0-based索引

        if (targetIndex >= 0 && targetIndex < currentState.words.size && !currentState.isProcessingClick) {
            val direction = if (targetIndex > currentState.currentWordIndex) {
                SlideDirection.FORWARD
            } else {
                SlideDirection.BACKWARD
            }

            // 将当前位置加入历史栈（如果当前位置不在栈顶，且不是连续跳转）
            val newHistory = if (currentState.navigationHistory.lastOrNull() != currentState.currentWordIndex) {
                currentState.navigationHistory + currentState.currentWordIndex
            } else {
                currentState.navigationHistory
            }

            _uiState.value = currentState.copy(
                currentWordIndex = targetIndex,
                isFlipped = false,
                isProcessingClick = true,
                slideDirection = direction,
                navigationHistory = newHistory
            )
            // Save progress
            preferences.saveLastIndex(categoryId, targetIndex)

            // 重置处理状态
            viewModelScope.launch {
                delay(400) // 等待动画完成
                _uiState.value = _uiState.value.copy(isProcessingClick = false)
            }
        }
    }

    fun refresh() {
        loadCategoryWords()
    }
}
