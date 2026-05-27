package com.jian.nemo.feature.test.presentation.ability

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.roundToInt

@Serializable
data class ClozeQuestion(
    val wordId: Int,
    val japanese: String,       // 日语原文，如“漢字”
    val hiragana: String,       // 读音假名，如“かんじ”
    val chinese: String,        // 中文释义
    val maskIndices: List<Int>, // 被挖空的字符索引列表，例如对于“かんじ”如果挖空“ん”和“じ”，则为 [1, 2]
    val explanation: String     // 详细解析说明
)

@Serializable
data class ClozeSession(
    val level: String,
    val questions: List<ClozeQuestion>,
    val currentIndex: Int,
    val correctCount: Int,
    // 保存用户在每道题的各个输入框中所输入的内容。
    // 第一维为题目索引，第二维为每个题目挖空框中的字符
    val userInputs: List<List<String>>,
    val questionCorrectStates: List<Boolean> = List(questions.size) { false }
)

sealed interface ClozeUiState {
    data object Loading : ClozeUiState
    data object LevelSelecting : ClozeUiState
    data class Ready(
        val questions: List<ClozeQuestion>,
        val currentIndex: Int = 0,
        // userInputs 存放了每道题当前各挖空格的用户填入内容，与 Ready 状态下的题目及掩码严格对应
        // 长度为 maskIndices 的大小。如果还没输入，则是 ""
        val userInputs: List<List<String>>,
        // 记录每道题的错误次数，如果连续错3次将自动开启底部解析
        val errorCounts: List<Int> = List(questions.size) { 0 },
        val correctCount: Int = 0,
        // 是否显示底部解析 Sheet
        val showExplanation: Boolean = false,
        // 用于触发输入框抖动动画的标志。在检测到错误瞬间，该项的值置为 true
        // 答题完成后自动置为 false。
        val isShakeTriggered: Boolean = false,
        // 每道题是否已经拼写正确通关的布尔数组
        val questionCorrectStates: List<Boolean> = List(questions.size) { false }
    ) : ClozeUiState
    data class Finished(val correctCount: Int, val totalCount: Int) : ClozeUiState
    data class Error(val message: String) : ClozeUiState
}

@HiltViewModel
class WordClozeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordRepository: WordRepository,
    private val testRecordDao: TestRecordDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClozeUiState>(ClozeUiState.Loading)
    val uiState: StateFlow<ClozeUiState> = _uiState.asStateFlow()

    val historyRecords = testRecordDao.getRecordsByMode("word_cloze_local", 50)

    private val prefs = context.getSharedPreferences("word_cloze_session", Context.MODE_PRIVATE)

    var currentLevel: String = ""
        private set



    init {
        restoreOrStartSession()
    }

    /**
     * 初始化：尝试恢复本地保存的会话，如无则进入等级选择
     */
    private fun restoreOrStartSession() {
        val session = restoreSession()
        if (session != null) {
            currentLevel = session.level
            _uiState.value = ClozeUiState.Ready(
                questions = session.questions,
                currentIndex = session.currentIndex,
                userInputs = session.userInputs,
                correctCount = session.correctCount,
                questionCorrectStates = session.questionCorrectStates
            )
        } else {
            _uiState.value = ClozeUiState.LevelSelecting
        }
    }

    /**
     * 选择关卡等级并出题
     */
    fun onLevelSelected(level: String) {
        currentLevel = level
        _uiState.value = ClozeUiState.Loading

        viewModelScope.launch {
            try {
                // 1. 从本地获取指定等级的所有单词数据
                val allWords = wordRepository.getAllWordsByLevel(level).first()
                val targetWords = allWords.filter { !it.isDelisted && it.hiragana.isNotBlank() }

                // 2. 随机抽取 10 个单词，如果不够，则降级补齐
                val selectedWords = if (targetWords.size >= 10) {
                    targetWords.shuffled().take(10)
                } else {
                    val fallback = allWords.filter { !it.isDelisted && it.hiragana.isNotBlank() }
                    (targetWords + fallback).distinctBy { it.id }.shuffled().take(10)
                }

                if (selectedWords.isEmpty()) {
                    _uiState.value = ClozeUiState.Error("当前等级本地词库为空，请先在设置中导入或下载词库。")
                    return@launch
                }

                // 3. 对抽出的单词生成掩码挖空
                val questions = selectedWords.map { word ->
                    val maskIndices = generateMaskIndices(word.hiragana)
                    
                    ClozeQuestion(
                        wordId = word.id,
                        japanese = word.japanese,
                        hiragana = word.hiragana,
                        chinese = word.chinese,
                        maskIndices = maskIndices,
                        explanation = word.chinese
                    )
                }

                // 4. 创建初始化的输入空数组：每道题有其对应 maskIndices 长度的输入值
                val userInputs = questions.map { q ->
                    List(q.maskIndices.size) { "" }
                }

                val readyState = ClozeUiState.Ready(
                    questions = questions,
                    userInputs = userInputs,
                    correctCount = 0,
                    questionCorrectStates = List(questions.size) { false }
                )
                _uiState.value = readyState
                saveSession(readyState)
            } catch (e: Exception) {
                _uiState.value = ClozeUiState.Error("出题失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    /**
     * 智能默认挖空算法
     * 对平假名/片假名进行合理挖空：
     * - 长度为 1: 直接 100% 挖空
     * - 长度大于 1: 默认随机挖空其 50% 到 70% 的假名，且在算法上确保至少挖空 1 个假名，也至少保留 1 个已显示的假名
     */
    private fun generateMaskIndices(hiragana: String): List<Int> {
        // 1. 过滤提取出所有属于合法日文假名的字符索引
        val eligibleIndices = mutableListOf<Int>()
        hiragana.forEachIndexed { idx, char ->
            if (char in '\u3040'..'\u30ff') {
                eligibleIndices.add(idx)
            }
        }

        val eligibleLen = eligibleIndices.size
        if (eligibleLen == 0) return emptyList()
        if (eligibleLen == 1) return eligibleIndices

        // 2. 随机设定 50% - 70% 比例
        val minMask = (eligibleLen * 0.5).roundToInt().coerceAtLeast(1)
        val maxMask = (eligibleLen * 0.7).roundToInt().coerceAtMost(eligibleLen - 1)
        val maskCount = if (minMask <= maxMask) {
            (minMask..maxMask).random()
        } else {
            minMask.coerceAtMost(eligibleLen - 1).coerceAtLeast(1)
        }

        // 3. 从合法的假名索引中，随机挑选并返回排好序的挖空索引
        return eligibleIndices.shuffled().take(maskCount).sorted()
    }

    /**
     * 将字符统一转换为平假名进行对比（消除平假名与片假名的匹配隔阂）
     */
    private fun convertToHiragana(char: Char): Char {
        return if (char in '\u30a1'..'\u30f6') {
            (char.code - 0x60).toChar()
        } else {
            char
        }
    }

    /**
     * 核验用户输入的假名字符是否正确匹配目标假名。
     */
    fun checkInputIsCorrect(targetChar: Char, userInput: String): Boolean {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty()) return false

        // 直接将输入的日文字符与目标假名字符统一转换为平假名进行对比（消除平假名与片假名的匹配隔阂）
        val inputChar = trimmed[0]
        return convertToHiragana(inputChar) == convertToHiragana(targetChar)
    }

    /**
     * 实时同步用户的临时输入（英文罗马音）
     */
    fun updateUserInput(questionIndex: Int, maskIndexInQuestion: Int, value: String) {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return
        val updatedInputs = currentState.userInputs.mapIndexed { qIdx, inputs ->
            if (qIdx == questionIndex) {
                inputs.toMutableList().apply {
                    set(maskIndexInQuestion, value)
                }
            } else {
                inputs
            }
        }
        val newState = currentState.copy(userInputs = updatedInputs)
        _uiState.value = newState
        saveSession(newState)
    }

    /**
     * 提交且判定为全对
     */
    fun submitAnswerSuccess(questionIndex: Int) {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return
        val q = currentState.questions[questionIndex]

        // 增加正确计数（若错误次数少于3，算作答对）
        val wasAllCorrectWithoutAnalysis = (currentState.errorCounts[questionIndex] < 3)
        val updatedCorrectCount = if (wasAllCorrectWithoutAnalysis) {
            currentState.correctCount + 1
        } else {
            currentState.correctCount
        }

        // 全对时，将正确的平假名字符回填到输入中，以供 UI 只读渲染
        val updatedInputs = currentState.userInputs.mapIndexed { qIdx, inputs ->
            if (qIdx == questionIndex) {
                q.maskIndices.map { q.hiragana[it].toString() }
            } else {
                inputs
            }
        }

        val updatedCorrectStates = currentState.questionCorrectStates.toMutableList().apply {
            set(questionIndex, true)
        }

        val newState = currentState.copy(
            userInputs = updatedInputs,
            questionCorrectStates = updatedCorrectStates,
            correctCount = updatedCorrectCount
        )
        _uiState.value = newState
        saveSession(newState)
    }

    /**
     * 提交且判定为有错误
     */
    fun submitAnswerError(questionIndex: Int) {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return

        // 增加该题的错误计数
        val updatedErrors = currentState.errorCounts.toMutableList().apply {
            set(questionIndex, get(questionIndex) + 1)
        }

        // 连续错 3 次自动弹出悬浮毛玻璃解析卡片
        val triggerExplanation = updatedErrors[questionIndex] >= 3

        val newState = currentState.copy(
            errorCounts = updatedErrors,
            showExplanation = if (triggerExplanation) true else currentState.showExplanation
        )
        _uiState.value = newState
        saveSession(newState)

        // 触发左右弹性抖动动画
        triggerShake()
    }

    /**
     * 触发抖动动效
     */
    fun triggerShake() {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(isShakeTriggered = true)
            // 静默一会儿再重置触发器，保证下一次抖动可以响应
            kotlinx.coroutines.delay(100L)
            val nextState = _uiState.value as? ClozeUiState.Ready ?: return@launch
            _uiState.value = nextState.copy(isShakeTriggered = false)
        }
    }

    /**
     * 退格级联删除
     */
    fun clearInputSlot(questionIndex: Int, maskIndexInQuestion: Int) {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return
        val updatedInputs = currentState.userInputs.mapIndexed { qIdx, inputs ->
            if (qIdx == questionIndex) {
                inputs.toMutableList().apply {
                    set(maskIndexInQuestion, "")
                }
            } else {
                inputs
            }
        }
        val newState = currentState.copy(userInputs = updatedInputs)
        _uiState.value = newState
        saveSession(newState)
    }

    /**
     * 切题：上一题
     */
    fun previousQuestion() {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return
        if (currentState.currentIndex > 0) {
            val newState = currentState.copy(
                currentIndex = currentState.currentIndex - 1,
                showExplanation = false
            )
            _uiState.value = newState
            saveSession(newState)
        }
    }

    /**
     * 切题：下一题
     */
    fun nextQuestion() {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return
        if (currentState.currentIndex < currentState.questions.size - 1) {
            val newState = currentState.copy(
                currentIndex = currentState.currentIndex + 1,
                showExplanation = false
            )
            _uiState.value = newState
            saveSession(newState)
        } else {
            // 所有题目做完了！完成测试并存档
            clearSession()
            saveRecord(currentState.correctCount, currentState.questions.size)
            _uiState.value = ClozeUiState.Finished(
                correctCount = currentState.correctCount,
                totalCount = currentState.questions.size
            )
        }
    }

    /**
     * 解析 Sheet 开关显示控制
     */
    fun setExplanationVisible(visible: Boolean) {
        val currentState = _uiState.value as? ClozeUiState.Ready ?: return
        _uiState.value = currentState.copy(showExplanation = visible)
    }

    /**
     * 重新挑战本关卡
     */
    fun forceRegenerate() {
        clearSession()
        onLevelSelected(currentLevel)
    }

    /**
     * 返回等级选择页面
     */
    fun restart() {
        clearSession()
        _uiState.value = ClozeUiState.LevelSelecting
    }

    /**
     * 物理写入测试记录
     */
    private fun saveRecord(correct: Int, total: Int) {
        viewModelScope.launch {
            try {
                val record = TestRecordEntity(
                    testMode = "word_cloze_local",
                    totalQuestions = total,
                    correctAnswers = correct,
                    date = java.time.LocalDate.now().toEpochDay(),
                    timestamp = System.currentTimeMillis()
                )
                testRecordDao.insert(record)
            } catch (e: Exception) {
                android.util.Log.e("WordClozeViewModel", "保存测试历史记录失败", e)
            }
        }
    }

    // ========== 会话保存与恢复 ===========

    private fun saveSession(state: ClozeUiState.Ready) {
        try {
            val session = ClozeSession(
                level = currentLevel,
                questions = state.questions,
                currentIndex = state.currentIndex,
                correctCount = state.correctCount,
                userInputs = state.userInputs
            )
            prefs.edit().putString("current_cloze_session", Json.encodeToString(session)).apply()
        } catch (e: Exception) {
            // 忽略序列化异常
        }
    }

    fun clearSession() {
        prefs.edit().remove("current_cloze_session").apply()
    }

    private fun restoreSession(): ClozeSession? {
        val json = prefs.getString("current_cloze_session", null) ?: return null
        return try {
            Json.decodeFromString<ClozeSession>(json)
        } catch (e: Exception) {
            clearSession()
            null
        }
    }
}
