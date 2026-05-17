package com.jian.nemo.feature.test.presentation.ability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.data.audio.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerbConjugationQuestion(
    val word: String,
    val furigana: String,
    val meaning: String,
    val qText: String, // 带有 FuriganaText 格式 [kana] 的文本
    val translation: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class VerbConjugationUiState(
    val questions: List<VerbConjugationQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswered: Boolean = false,
    val showResult: Boolean = false,
    val correctCount: Int = 0
)

@HiltViewModel
class VerbConjugationViewModel @Inject constructor(
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerbConjugationUiState(questions = hardcodedQuestions))
    val uiState: StateFlow<VerbConjugationUiState> = _uiState.asStateFlow()

    fun selectOption(index: Int) {
        if (_uiState.value.isAnswered) return
        
        val isCorrect = index == _uiState.value.questions[_uiState.value.currentIndex].correctIndex
        _uiState.update { 
            it.copy(
                selectedOptionIndex = index,
                isAnswered = true,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount
            )
        }
    }

    fun nextQuestion() {
        _uiState.update { 
            if (it.currentIndex < it.questions.size - 1) {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedOptionIndex = null,
                    isAnswered = false
                )
            } else {
                it.copy(showResult = true)
            }
        }
    }

    fun playWordTts() {
        val currentQuestion = _uiState.value.questions[_uiState.value.currentIndex]
        viewModelScope.launch {
            ttsManager.speak(currentQuestion.word)
        }
    }

    fun restart() {
        _uiState.update { 
            VerbConjugationUiState(questions = hardcodedQuestions)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    companion object {
        private val hardcodedQuestions = listOf(
            VerbConjugationQuestion(
                word = "覚える",
                furigana = "おぼえる",
                meaning = "记住，学会",
                qText = "電話[でんわ]番号[ばんごう]を____ください。",
                translation = "请记住电话号码。",
                options = listOf("覚えた", "覚えない", "覚える", "覚えて"),
                correctIndex = 3,
                explanation = "「ください」前面通常接动词て形，表示请求对方“请做某事”。「覚える」是二类动词（一段动词），去「る」加「て」形成「覚えて」。"
            ),
            VerbConjugationQuestion(
                word = "借りる",
                furigana = "かりる",
                meaning = "借入，借用",
                qText = "お金[かね]が足[た]りなければ友達[ともだち]に____いい。",
                translation = "钱不够的话，向朋友借就好。",
                options = listOf("借りろ", "借りる", "借りれば", "借りた"),
                correctIndex = 2,
                explanation = "这里需要用条件形. 线索是后面的「いい」，构成「～ばいい」，表示“……就好”。「借りる」是一段动词，去「る」加「れば」，变成「借りれば」。"
            ),
            VerbConjugationQuestion(
                word = "押す",
                furigana = "おす",
                meaning = "推，按，压",
                qText = "ボタンを____、ドアを開[あ]けて。",
                translation = "按下按钮，然后打开门。",
                options = listOf("押して", "押せば", "押した", "押します"),
                correctIndex = 0,
                explanation = "这里要表示动作的先后顺序，“按下按钮”和“开门”两个动作连接起来，所以用て形。「押す」变て形是「押して」。"
            ),
            VerbConjugationQuestion(
                word = "使う",
                furigana = "つかう",
                meaning = "使用",
                qText = "ペンを____、名前[なまえ]を書[か]いてください。",
                translation = "请用笔写名字。",
                options = listOf("使わなくて", "使った", "使います", "使って"),
                correctIndex = 3,
                explanation = "这里表示使用工具或手段，后面接着另一个动作「書いてください」，所以要用て形连接。「使う」的て形是发生促音变的「使って」。"
            ),
            VerbConjugationQuestion(
                word = "消す",
                furigana = "けす",
                meaning = "关掉，熄灭",
                qText = "テレビを____ください。",
                translation = "请关掉电视。",
                options = listOf("消して", "消せ", "消した", "消します"),
                correctIndex = 0,
                explanation = "表示请求的「ください」前接て形。「消す」以「す」结尾，变て形为「消して」。"
            )
        )

    }
}
