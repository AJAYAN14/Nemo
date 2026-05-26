package com.jian.nemo.feature.test.presentation.ability

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.data.audio.TtsManager
import com.jian.nemo.core.data.local.dao.TestRecordDao
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@Serializable
data class ListeningQuestion(
    val word: String,
    val hiragana: String,
    val meaning: String,
    val questionType: String, // "IDENTIFY_WORD" 或 "IDENTIFY_MEANING"
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Serializable
data class ListeningSession(
    val level: String,
    val questions: List<ListeningQuestion>,
    val currentIndex: Int,
    val correctCount: Int,
    val userAnswers: List<Int?> = emptyList()
)

sealed interface ListeningUiState {
    data object Loading : ListeningUiState
    data object LevelSelecting : ListeningUiState
    data class Ready(
        val questions: List<ListeningQuestion>,
        val currentIndex: Int = 0,
        val userAnswers: List<Int?> = List(questions.size) { null },
        val correctCount: Int = 0
    ) : ListeningUiState {
        val selectedOptionIndex: Int? get() = userAnswers.getOrNull(currentIndex)
        val isAnswered: Boolean get() = selectedOptionIndex != null
    }
    data class Finished(val correctCount: Int, val totalCount: Int) : ListeningUiState
    data class Error(val message: String) : ListeningUiState
}

@HiltViewModel
class ListeningComprehensionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsManager: TtsManager,
    private val wordRepository: WordRepository,
    private val testRecordDao: TestRecordDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListeningUiState>(ListeningUiState.Loading)
    val uiState: StateFlow<ListeningUiState> = _uiState.asStateFlow()

    val historyRecords: Flow<List<TestRecordEntity>> = testRecordDao.getRecordsByMode("listening_comprehension_local", 50)

    private val prefs = context.getSharedPreferences("listening_local_session", Context.MODE_PRIVATE)

    var currentLevel: String = ""
        private set

    init {
        restoreOrStart()
    }

    private fun restoreOrStart() {
        val session = restoreSession()
        if (session != null) {
            currentLevel = session.level
            _uiState.value = ListeningUiState.Ready(
                questions = session.questions,
                currentIndex = session.currentIndex,
                correctCount = session.correctCount,
                userAnswers = session.userAnswers.ifEmpty { List(session.questions.size) { null } }
            )
        } else {
            _uiState.value = ListeningUiState.LevelSelecting
        }
    }

    fun onLevelSelected(level: String) {
        currentLevel = level
        _uiState.value = ListeningUiState.Loading
        
        viewModelScope.launch {
            try {
                // 1. 瞬间从本地数据库拉取指定等级的全部单词
                val allLevelWords = wordRepository.getAllWordsByLevel(level).first().filter { !it.isDelisted }
                
                if (allLevelWords.size < 5) {
                    _uiState.value = ListeningUiState.Error("该JLPT级别的本地词库单词数不足以生成测试题，请补充词库。")
                    return@launch
                }

                // 2. 随机混洗，选取 10 个作为题源单词（不足10个则全部选取）
                val targetCount = minOf(10, allLevelWords.size)
                val testWords = allLevelWords.shuffled().take(targetCount)
                
                val questions = testWords.mapIndexed { index, targetWord ->
                    // 随机确定本题考察：“听音辨词” 或是 “听音辨义”
                    val qType = if (index % 2 == 0) "IDENTIFY_WORD" else "IDENTIFY_MEANING"
                    
                    // 找出同等级的其他单词做干扰项
                    val distractors = allLevelWords.filter { it.id != targetWord.id }
                        .shuffled()
                        .take(3)
                    
                    val options = mutableListOf<String>()
                    val correctIndex: Int
                    
                    if (qType == "IDENTIFY_WORD") {
                        // 选项为日语拼写原文（若显示了汉字就不要在后面用括号来显示假名了）
                        val correctOption = targetWord.japanese
                        val wrongOptions = distractors.map { it.japanese }
                        
                        options.addAll(wrongOptions)
                        correctIndex = (0..3).random()
                        options.add(correctIndex, correctOption)
                    } else {
                        // 选项为中文释义
                        val correctOption = targetWord.chinese
                        val wrongOptions = distractors.map { it.chinese }
                        
                        options.addAll(wrongOptions)
                        correctIndex = (0..3).random()
                        options.add(correctIndex, correctOption)
                    }
                    
                    val qTypeDesc = if (qType == "IDENTIFY_WORD") "日语单词" else "中文释义"
                    
                    val explanation = buildString {
                        append("【发音单词】「${targetWord.japanese}」\n")
                        append("【假名读音】「${targetWord.hiragana}」\n")
                        append("【中文解释】「${targetWord.chinese}」\n\n")
                        if (targetWord.pos != null) {
                            append("【词性分类】${targetWord.pos}\n")
                        }
                        append("【正确答案】${options[correctIndex]}。本题主要考察根据语音识别出正确的${qTypeDesc}。")
                    }

                    ListeningQuestion(
                        word = targetWord.japanese,
                        hiragana = targetWord.hiragana,
                        meaning = targetWord.chinese,
                        questionType = qType,
                        options = options,
                        correctIndex = correctIndex,
                        explanation = explanation
                    )
                }

                val readyState = ListeningUiState.Ready(questions = questions)
                _uiState.value = readyState
                saveSession(readyState)
            } catch (e: Exception) {
                _uiState.value = ListeningUiState.Error("出题失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun selectOption(index: Int) {
        val currentState = _uiState.value as? ListeningUiState.Ready ?: return
        if (currentState.isAnswered) return
        
        val currentQ = currentState.questions[currentState.currentIndex]
        val isCorrect = index == currentQ.correctIndex
        
        val newUserAnswers = currentState.userAnswers.toMutableList().apply {
            set(currentState.currentIndex, index)
        }
        
        val newState = currentState.copy(
            userAnswers = newUserAnswers,
            correctCount = if (isCorrect) currentState.correctCount + 1 else currentState.correctCount
        )
        _uiState.value = newState
        saveSession(newState)
    }

    fun nextQuestion() {
        val currentState = _uiState.value as? ListeningUiState.Ready ?: return
        if (currentState.currentIndex < currentState.questions.size - 1) {
            val newState = currentState.copy(
                currentIndex = currentState.currentIndex + 1
            )
            _uiState.value = newState
            saveSession(newState)
        } else {
            // 答题结束
            clearSession()
            saveRecord(currentState.correctCount, currentState.questions.size)
            _uiState.value = ListeningUiState.Finished(
                correctCount = currentState.correctCount,
                totalCount = currentState.questions.size
            )
        }
    }

    fun previousQuestion() {
        val currentState = _uiState.value as? ListeningUiState.Ready ?: return
        if (currentState.currentIndex > 0) {
            val newState = currentState.copy(
                currentIndex = currentState.currentIndex - 1
            )
            _uiState.value = newState
            saveSession(newState)
        }
    }

    fun playTts(speed: Float = 1.0f) {
        val currentState = _uiState.value as? ListeningUiState.Ready ?: return
        val currentQuestion = currentState.questions[currentState.currentIndex]
        viewModelScope.launch {
            try {
                // 初始化 TTS 发音引擎
                ttsManager.initialize()
                // 设置语速
                ttsManager.setSpeechRate(speed)
                // 朗读单词
                ttsManager.speak(currentQuestion.word)
            } catch (e: Exception) {
                android.util.Log.e("ListeningComprehensionVM", "TTS 播放失败", e)
            }
        }
    }

    private fun saveRecord(correct: Int, total: Int) {
        viewModelScope.launch {
            try {
                val entity = TestRecordEntity(
                    testMode = "listening_comprehension_local",
                    totalQuestions = total,
                    correctAnswers = correct,
                    date = System.currentTimeMillis()
                )
                testRecordDao.insert(entity)
            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    private fun saveSession(state: ListeningUiState.Ready) {
        val session = ListeningSession(
            level = currentLevel,
            questions = state.questions,
            currentIndex = state.currentIndex,
            correctCount = state.correctCount,
            userAnswers = state.userAnswers
        )
        prefs.edit().putString("current_session", Json.encodeToString(session)).apply()
    }

    fun clearSession() {
        prefs.edit().remove("current_session").apply()
    }

    private fun restoreSession(): ListeningSession? {
        val jsonStr = prefs.getString("current_session", null) ?: return null
        return try {
            Json.decodeFromString<ListeningSession>(jsonStr)
        } catch (e: Exception) {
            clearSession()
            null
        }
    }

    fun forceRegenerate() {
        clearSession()
        onLevelSelected(currentLevel)
    }

    fun restart() {
        clearSession()
        _uiState.value = ListeningUiState.LevelSelecting
    }

    override fun onCleared() {
        super.onCleared()
        // 恢复语速并停止
        try {
            ttsManager.setSpeechRate(1.0f)
            ttsManager.stop()
        } catch (e: Exception) {
            // 忽略
        }
    }
}
