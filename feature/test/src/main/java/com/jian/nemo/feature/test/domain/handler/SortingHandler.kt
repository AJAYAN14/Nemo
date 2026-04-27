package com.jian.nemo.feature.test.domain.handler

import com.jian.nemo.core.domain.model.SortableChar
import com.jian.nemo.core.domain.model.TestQuestion
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 排序题答案状态
 */
data class SortingAnswerState(
    val selectedChars: List<SortableChar> = emptyList(),
    val updatedQuestion: TestQuestion.Sorting? = null,
    val isDebounced: Boolean = false
)

/**
 * 排序题处理器
 *
 * 职责：处理排序题的选中/取消选中逻辑
 */
@Singleton
class SortingHandler @Inject constructor() {

    // 防抖机制（复刻旧项目 QuestionLogic.kt 行36-37）
    private val lastClickTime = AtomicLong(0)
    private val clickDelay = 300L

    /**
     * 检查防抖
     * @return true 表示应该忽略此次点击
     */
    private fun shouldDebounce(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastClickTime.get()
        if (currentTime - lastTime < clickDelay) {
            return true
        }
        lastClickTime.set(currentTime)
        return false
    }

    /**
     * 选择可排序字符
     *
     * @param char 要选择的字符
     * @param question 当前排序题
     * @param currentSelectedChars 当前已选中的字符列表
     * @return 更新后的状态，如果防抖或已选中则返回 isDebounced=true
     */
    fun selectChar(
        char: SortableChar,
        question: TestQuestion.Sorting,
        currentSelectedChars: List<SortableChar>
    ): SortingAnswerState {
        // 题目已回答，不允许操作
        if (question.isAnswered) {
            return SortingAnswerState(isDebounced = true)
        }

        // 防重复点击
        if (shouldDebounce()) {
            return SortingAnswerState(isDebounced = true)
        }

        // 检查是否已选择
        if (currentSelectedChars.any { it.id == char.id }) {
            return SortingAnswerState(isDebounced = true)
        }

        // 更新选项状态
        val updatedOptions = question.options.map {
            if (it.id == char.id) it.copy(isSelected = true) else it
        }
        val updatedQuestion = question.copy(options = updatedOptions)

        return SortingAnswerState(
            selectedChars = currentSelectedChars + char,
            updatedQuestion = updatedQuestion,
            isDebounced = false
        )
    }

    /**
     * 取消选择可排序字符
     *
     * @param char 要取消选择的字符
     * @param question 当前排序题
     * @param currentSelectedChars 当前已选中的字符列表
     * @return 更新后的状态
     */
    fun deselectChar(
        char: SortableChar,
        question: TestQuestion.Sorting,
        currentSelectedChars: List<SortableChar>
    ): SortingAnswerState {
        // 题目已回答，不允许操作
        if (question.isAnswered) {
            return SortingAnswerState(isDebounced = true)
        }

        // 防重复点击
        if (shouldDebounce()) {
            return SortingAnswerState(isDebounced = true)
        }

        // 更新选项状态
        val updatedOptions = question.options.map {
            if (it.id == char.id) it.copy(isSelected = false) else it
        }
        val updatedQuestion = question.copy(options = updatedOptions)

        return SortingAnswerState(
            selectedChars = currentSelectedChars.filterNot { it.id == char.id },
            updatedQuestion = updatedQuestion,
            isDebounced = false
        )
    }

    /**
     * 验证排序答案是否正确
     *
     * @param userAnswerChars 用户排序的字符列表
     * @param correctAnswer 正确答案（如假名）
     * @return 是否正确
     */
    fun verifyAnswer(userAnswerChars: List<SortableChar>, correctAnswer: String): Boolean {
        val userAnswerText = userAnswerChars.joinToString("") { it.char.toString() }
        return userAnswerText == correctAnswer
    }
}
