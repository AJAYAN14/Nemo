package com.jian.nemo.core.domain.factory

import com.jian.nemo.core.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 测试题目构建工厂
 *
 * 负责将 Word/Grammar 实体转换为具体题型 (MultipleChoice, Typing, Sorting 等)。
 * 剥离了策略逻辑，专注于"构建"。
 */
@Singleton
class TestQuestionFactory @Inject constructor() {

    // ========== Multiple Choice ==========

    fun createMultipleChoice(
        word: Word,
        mode: TestMode,
        distractors: List<Word>,
        shuffleOptions: Boolean = true
    ): TestQuestion.MultipleChoice {
        // 全随机模式逻辑：根据数据可用性抽取 1-8 种多维度模式之一
        val subMode = if (mode == TestMode.RANDOM) {
            val availableModes = mutableListOf(1, 2, 3, 4, 5, 6)
            if (!word.pos.isNullOrBlank()) {
                availableModes.add(7)
                availableModes.add(8)
            }
            availableModes.random()
        } else 0

        val correctAnswer = if (subMode > 0) getWordSubModeAnswer(word, subMode) else getCorrectAnswer(word, mode)
        val wrongOptions = generateWrongOptions(word, mode, distractors, 3, subMode)
        val options = (wrongOptions + correctAnswer).toList()

        return TestQuestion.MultipleChoice(
            id = word.id,
            word = word,
            mode = mode,
            questionText = if (subMode > 0) getWordSubModeQuestion(word, subMode) else getQuestionText(word, mode),
            correctAnswer = correctAnswer,
            options = if (shuffleOptions) options.shuffled() else options,
            explanationPayload = word.toWordExplanationPayload()
        )
    }

    fun createGrammarMultipleChoice(
        grammar: Grammar,
        mode: TestMode,
        distractors: List<Grammar>,
        shuffleOptions: Boolean = true
    ): TestQuestion.MultipleChoice {
        val correctAnswer = grammar.getFirstExplanation()
        val wrongOptions = generateGrammarWrongOptions(grammar, distractors, 3)
        val options = (wrongOptions + correctAnswer).toList()

        return TestQuestion.MultipleChoice(
            id = grammar.id,
            grammar = grammar,
            mode = mode,
            questionText = grammar.grammar,
            correctAnswer = correctAnswer,
            options = if (shuffleOptions) options.shuffled() else options,
            explanationPayload = ExplanationPayload.GrammarText(correctAnswer)
        )
    }

    fun mapJsonToMultipleChoice(
        jsonQ: GrammarTestQuestion,
        mode: TestMode,
        grammarMap: Map<Int, Grammar>,
        shuffleOptions: Boolean = true
    ): TestQuestion.MultipleChoice {
        // Link to real Grammar entity if possible
        val grammarId = try { extractNumericId(jsonQ.targetGrammarId) } catch (_: Exception) { 0 }
        val grammar = grammarMap[grammarId]

        return TestQuestion.MultipleChoice(
            id = jsonQ.id.hashCode(), // Use hashCode for Int ID risk collision but ok for MVP
            word = null,
            grammar = grammar,
            mode = mode,
            questionText = jsonQ.question,
            correctAnswer = jsonQ.options[jsonQ.correctIndex],
            options = jsonQ.options,
            explanation = jsonQ.explanation,
            explanationPayload = resolveJsonGrammarExplanationPayload(jsonQ.explanation, grammar)
        ).let { if (shuffleOptions) it.copy(options = it.options.shuffled()) else it }
    }

    // ========== Typing ==========

    fun createTyping(word: Word): TestQuestion.Typing {
        // 随机选择题型（1-6）
        val questionTypes = listOf(1, 2, 3, 4, 5, 6)
        val questionType = questionTypes.random()

        val (questionText, correctAnswer) = when (questionType) {
            1 -> word.chinese to word.hiragana
            2 -> word.chinese to word.japanese
            3 -> word.hiragana to word.japanese
            4 -> word.japanese to word.hiragana
            5 -> word.hiragana to word.chinese
            6 -> word.japanese to word.chinese
            else -> word.chinese to word.hiragana
        }

        return TestQuestion.Typing(
            id = word.id,
            word = word,
            questionText = questionText,
            correctAnswer = correctAnswer,
            questionType = questionType
        )
    }

    // ========== Sorting ==========

    fun createSorting(word: Word, shuffleOptions: Boolean = true): TestQuestion.Sorting {
        // 1. 获取正确答案的假名序列
        val correctChars = word.hiragana.map { SortableChar(it) }
        
        // 2. 随机生成 1-2 个干扰项 (Distractors)
        val distractorCount = (1..2).random()
        val distractors = HIRAGANA_POOL
            .filter { it !in word.hiragana } // 避开正确答案中的字符以防混淆
            .shuffled()
            .take(distractorCount)
            .map { SortableChar(it) }

        // 3. 混合并打乱 (根据 shuffleOptions 决定是否打乱)
        val combinedOptions = (correctChars + distractors)
        val options = if (shuffleOptions) combinedOptions.shuffled() else combinedOptions

        return TestQuestion.Sorting(
            id = word.id,
            word = word,
            options = options
        )
    }



    // ========== Helpers ==========

    private fun getQuestionText(word: Word, mode: TestMode): String {
        return when (mode) {
            TestMode.JP_TO_CN -> word.japanese
            TestMode.CN_TO_JP -> word.chinese
            TestMode.KANA -> word.japanese
            TestMode.EXAMPLE -> word.chinese
            TestMode.RANDOM -> word.japanese
        }
    }

    private fun getCorrectAnswer(word: Word, mode: TestMode): String {
        return when (mode) {
            TestMode.JP_TO_CN -> word.chinese
            TestMode.CN_TO_JP -> word.japanese
            TestMode.KANA -> word.hiragana
            TestMode.EXAMPLE -> word.japanese
            TestMode.RANDOM -> word.chinese
        }
    }

    private fun generateWrongOptions(
        currentWord: Word,
        mode: TestMode,
        allWords: List<Word>,
        count: Int,
        subMode: Int = 0
    ): List<String> {
        val correctAnswer = if (subMode > 0) getWordSubModeAnswer(currentWord, subMode) else getCorrectAnswer(currentWord, mode)
        
        // 1. 从词池中尝试提取真实的干扰项
        val poolCandidates = allWords
            .asSequence()
            .filter { it.id != currentWord.id }
            .map { if (subMode > 0) getWordSubModeAnswer(it, subMode) else getCorrectAnswer(it, mode) }
            .filter { it.isNotBlank() && it != correctAnswer }
            .distinct()
            .toList()

        // 2. 针对词性模式的处理：如果词性种类不足，使用静态池补位
        val finalCandidates = if ((subMode == 7 || subMode == 8) && poolCandidates.size < count) {
            val fallbackPool = FALLBACK_POS_LIST.filter { it != correctAnswer }
            (poolCandidates + fallbackPool).distinct()
        } else {
            poolCandidates
        }

        return if (finalCandidates.size > count) {
            finalCandidates.shuffled().take(count)
        } else {
            finalCandidates.shuffled()
        }
    }

    companion object {
        /**
         * 词性模式干扰项生成的“后备池”
         * 当干扰词池中词性过于单一（例如全是名词）时，从该池中抽取不同词性补原。
         */
        private val FALLBACK_POS_LIST = listOf("名", "动", "形", "形动", "副", "代", "接", "感", "助", "连体", "接头", "接尾")

        /**
         * 常用平假名池，用于为排序题生成干扰项
         */
        private val HIRAGANA_POOL = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん".toList()
    }

    /**
     * 实现单词的 6 种维度映射逻辑
     * 1: 中文 -> 平假名
     * 2: 中文 -> 日语汉字 (CN_TO_JP)
     * 3: 平假名 -> 日语汉字
     * 4: 日语汉字 -> 平假名 (KANA)
     * 5: 平假名 -> 中文
     * 6: 日语汉字 -> 中文 (JP_TO_CN)
     */
    private fun getWordSubModeQuestion(word: Word, subMode: Int): String {
        return when (subMode) {
            1 -> word.chinese
            2 -> word.chinese
            3 -> word.hiragana
            4 -> word.japanese
            5 -> word.hiragana
            6 -> word.japanese
            7 -> word.hiragana
            8 -> word.japanese
            else -> word.japanese
        }.ifBlank { word.japanese } // Fallback to japanese if empty
    }

    private fun getWordSubModeAnswer(word: Word, subMode: Int): String {
        return when (subMode) {
            1 -> word.hiragana
            2 -> word.japanese
            3 -> word.japanese
            4 -> word.hiragana
            5 -> word.chinese
            6 -> word.chinese
            7 -> word.pos ?: ""
            8 -> word.pos ?: ""
            else -> ""
        }.ifBlank { 
            // 模式 7/8 属于特殊维度（词性），不应回退到普通中文意思，否则会导致选项维度不一致
            if (subMode == 7 || subMode == 8) "" else word.chinese 
        }
    }

    private fun generateGrammarWrongOptions(
        currentGrammar: Grammar,
        allGrammars: List<Grammar>,
        count: Int
    ): List<String> {
        val correctAnswer = currentGrammar.getFirstExplanation()
        val candidates = allGrammars
            .filter { it.id != currentGrammar.id }
            .map { it.getFirstExplanation() }
            .filter { it != correctAnswer }
            .distinct()

        return if (candidates.size <= count) candidates else candidates.shuffled().take(count)
    }

    private fun extractNumericId(id: String): Int {
        return try {
            val parts = id.split("_")
            val levelNum = parts[0].substring(1).toInt()
            val num = parts[1].toInt()
            levelNum * 1000 + num
        } catch (_: Exception) {
            0
        }
    }

    private fun resolveJsonGrammarExplanationPayload(
        jsonExplanation: String?,
        grammar: Grammar?
    ): ExplanationPayload? {
        val text = jsonExplanation?.takeIf { it.isNotBlank() }
            ?: grammar?.getFirstExplanation()?.takeIf { it.isNotBlank() }
        return text?.let { ExplanationPayload.GrammarText(it) }
    }
}
