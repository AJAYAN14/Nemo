package com.jian.nemo.core.domain.model

/**
 * 测试题目
 *
 * 参考: 旧项目 TestQuestion (sealed class)
 */
sealed class TestQuestion {
    abstract val id: Int
    abstract val word: Word?
    open val grammar: Grammar? = null
    abstract val isAnswered: Boolean
    abstract val isCorrect: Boolean
    abstract val correctAnswer: String

    /**
     * 选择题
     */
    data class MultipleChoice(
        override val id: Int,
        override val word: Word? = null,
        override val grammar: Grammar? = null,
        val mode: TestMode,
        val questionText: String,
        override val correctAnswer: String,
        val options: List<String>,
        override val isAnswered: Boolean = false,
        override val isCorrect: Boolean = false,
        val userAnswerIndex: Int? = null,
        val explanation: String? = null,
        val explanationPayload: ExplanationPayload? = null
    ) : TestQuestion() {
        /** 用户的答案文本 */
        val userAnswer: String?
            get() = userAnswerIndex?.let { options.getOrNull(it) }

        /**
         * 解析统一入口。
         *
         * 优先使用显式 payload，保留对旧字段的兼容回退。
         */
        val resolvedExplanationPayload: ExplanationPayload?
            get() = explanationPayload
                ?: word?.toWordExplanationPayload()
                ?: explanation?.takeIf { it.isNotBlank() }?.let { ExplanationPayload.GrammarText(it) }
                ?: grammar?.getFirstExplanation()?.takeIf { it.isNotBlank() }?.let { ExplanationPayload.GrammarText(it) }
    }

    /**
     * 打字题
     * @param questionType 题型标识（1-6）：
     *   1: 题目=释义, 答案=假名
     *   2: 题目=释义, 答案=汉字
     *   3: 题目=假名, 答案=汉字
     *   4: 题目=汉字, 答案=假名
     *   5: 题目=假名, 答案=释义
     *   6: 题目=汉字, 答案=释义
     */
    data class Typing(
        override val id: Int,
        override val word: Word,
        val questionText: String,
        override val correctAnswer: String,
        override val isAnswered: Boolean = false,
        override val isCorrect: Boolean = false,
        val userAnswer: String = "",
        val questionType: Int = 1  // 题型标识：1-6
    ) : TestQuestion()

    /**
     * 卡片匹配题
     *
     * 参考: 旧项目 TestModels.kt 行100-104
     *
     * @param pairs 5个单词对象的列表
     */
    data class CardMatching(
        override val id: Int,
        val pairs: List<Word>,
        override val isAnswered: Boolean = false,
        override val isCorrect: Boolean = false
    ) : TestQuestion() {
        override val word: Word? = null
        override val correctAnswer: String = ""  // 卡片题无单一正确答案
    }

    /**
     * 排序题
     *
     * 参考: 旧项目 TestModels.kt 行114-120
     *
     * @param word 单词对象
     * @param options 可排序字符列表
     * @param isAnswered 是否已回答
     * @param isCorrect 是否正确
     * @param userAnswer 用户选择的答案（字符列表）
     */
    data class Sorting(
        override val id: Int,
        override val word: Word,
        override val grammar: Grammar? = null,
        val options: List<SortableChar>,
        override val isAnswered: Boolean = false,
        override val isCorrect: Boolean = false,
        val userAnswer: List<SortableChar> = emptyList()
    ) : TestQuestion() {
        override val correctAnswer: String
            get() = word.hiragana
    }
}

/**
 * 选择题解析的统一载体。
 */
sealed interface ExplanationPayload {
    data class WordSummary(
        val japanese: String,
        val hiragana: String,
        val meaning: String
    ) : ExplanationPayload

    data class GrammarText(
        val text: String
    ) : ExplanationPayload
}

fun Word.toWordExplanationPayload(): ExplanationPayload.WordSummary {
    return ExplanationPayload.WordSummary(
        japanese = japanese,
        hiragana = hiragana,
        meaning = chinese
    )
}

/**
 * 可排序字符数据类
 *
 * 参考: 旧项目 TestModels.kt 行129-133
 *
 * @param char 字符
 * @param id 唯一ID（用于处理重复字符）
 * @param isSelected 是否已选择
 */
data class SortableChar(
    val char: Char,
    val id: String = java.util.UUID.randomUUID().toString(),
    var isSelected: Boolean = false
)
