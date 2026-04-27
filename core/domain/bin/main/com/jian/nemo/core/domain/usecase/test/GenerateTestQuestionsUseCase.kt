package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.domain.model.QuestionType
import com.jian.nemo.core.domain.model.TestMode
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.common.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarTestQuestion
import com.jian.nemo.core.domain.repository.GrammarTestRepository
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingsRepository

/**
 * 生成测试题目 Use Case
 */
class GenerateTestQuestionsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: com.jian.nemo.core.domain.repository.GrammarRepository,
    private val grammarTestRepository: GrammarTestRepository,
    private val generateCardMatchingQuestionsUseCase: GenerateCardMatchingQuestionsUseCase,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val settingsRepository: SettingsRepository,
    private val testQuestionFactory: com.jian.nemo.core.domain.factory.TestQuestionFactory
) {
    /**
     * 生成测试题目
     *
     * @param level JLPT等级 (N1-N5)
     * @param mode 测试模式
     * @param count 题目数量
     * @param contentType 内容类型 ("words", "grammar", "mixed")
     * @param source 题目来源 ("today", "wrong", "favorite", etc.)
     * @return 题目列表
     */
    suspend operator fun invoke(
        level: String,
        mode: TestMode,
        count: Int = 20,
        questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
        contentType: String = "words",
        source: String = "today",
        typeCounts: Map<String, Int>? = null,
        // New params for settings
        shuffleQuestions: Boolean = true,
        shuffleOptions: Boolean = true,
        prioritizeWrong: Boolean = false,
        prioritizeNew: Boolean = false
    ): List<TestQuestion> {
        println("NemoTestDebug: Generating questions. Request: count=$count, source=$source, type=$contentType, levels=$level, questionType=$questionType")

        val questions = mutableListOf<TestQuestion>()
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = DateTimeUtils.getLearningDay(resetHour)

        // Helper to filter by levels
        fun filterWordsByLevels(list: List<Word>, levelsStr: String): List<Word> {
             val levels = levelsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
             if (levels.isEmpty()) return list
             return list.filter { it.level in levels }
        }

        fun filterGrammarsByLevels(list: List<Grammar>, levelsStr: String): List<Grammar> {
             val levels = levelsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
             if (levels.isEmpty()) return list
             return list.filter { it.grammarLevel in levels }
        }

        // 1. 获取目标单词 (Target Pool)
        val words = if (contentType != "grammar") {
            val rawWords = when(source) {
                "today" -> wordRepository.getTodayLearnedWords(today).first()
                "wrong" -> {
                     val ids = wrongAnswerRepository.getAllWrongWordIds()
                     if (ids.isNotEmpty()) wordRepository.getWordsByIds(ids) else emptyList()
                }
                "favorite" -> wordRepository.getFavoriteWords().first()
                "learned" -> wordRepository.getAllLearnedWords().first()
                "today_reviewed" -> {
                     val ids = settingsRepository.getTodayTestedWordIds()
                     if (ids.isNotEmpty()) wordRepository.getWordsByIds(ids.toList()) else emptyList()
                }
                else -> {
                    // "all" or explicit levels
                    // Same logic as before for "all"
                    val levels = level.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (levels.isEmpty()) {
                        emptyList()
                    } else {
                         val allWords = mutableListOf<Word>()
                        levels.forEach { lvl ->
                            allWords.addAll(wordRepository.getAllWordsByLevel(lvl).first())
                        }
                        allWords
                    }
                }
            }
            // For non-"all" sources, we still need to filter by level?
            if (source == "all" || source !in listOf("today", "wrong", "favorite", "learned", "today_reviewed")) rawWords
            else filterWordsByLevels(rawWords, level)
        } else emptyList()

        // 🎯 1.1 获取单词干扰项池 (Distractor Pool)
        // 无论 source 是什么，干扰项都应该来自同等级的所有单词
        val allWordsForDistractors = if (contentType != "grammar") {
            if (source == "all" || source !in listOf("today", "wrong", "favorite", "learned", "today_reviewed")) {
                // 如果 source 已经是 all，直接复用
                words
            } else {
                // 如果 source 是特定范围（如 today），我们需要加载该等级下的所有单词作为干扰项
                val levels = level.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (levels.isEmpty()) {
                    emptyList()
                } else {
                    val pool = mutableListOf<Word>()
                    levels.forEach { lvl ->
                        pool.addAll(wordRepository.getAllWordsByLevel(lvl).first())
                    }
                    pool
                }
            }
        } else emptyList()

        // 2. 获取语法 (Target Pool)
        val grammars = if (contentType != "words" && (questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.SORTING)) {
            val rawGrammars = when(source) {
                "today" -> grammarRepository.getTodayLearnedGrammars(today).first()
                "wrong" -> {
                     val ids = grammarWrongAnswerRepository.getAllWrongGrammarIds()
                     if (ids.isNotEmpty()) grammarRepository.getGrammarsByIds(ids) else emptyList()
                }
                "favorite" -> grammarRepository.getFavoriteGrammars().first()
                "learned" -> grammarRepository.getAllLearnedGrammars().first()
                "today_reviewed" -> {
                     val ids = settingsRepository.getTodayTestedGrammarIds()
                     if (ids.isNotEmpty()) grammarRepository.getGrammarsByIds(ids.toList()) else emptyList()
                }
                else -> {
                    val levels = level.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (levels.isEmpty()) {
                        emptyList()
                    } else {
                        grammarRepository.getGrammarsByLevels(levels).first()
                    }
                }
            }

            if (source == "all" || source !in listOf("today", "wrong", "favorite", "learned", "today_reviewed")) rawGrammars
            else filterGrammarsByLevels(rawGrammars, level)
        } else emptyList()

        // 🎯 2.1 获取语法干扰项池 (Distractor Pool)
        val allGrammarsForDistractors = if (contentType != "words" && (questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.SORTING)) {
             if (source == "all" || source !in listOf("today", "wrong", "favorite", "learned", "today_reviewed")) {
                grammars
            } else {
                val levels = level.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (levels.isEmpty()) {
                    emptyList()
                } else {
                    grammarRepository.getGrammarsByLevels(levels).first()
                }
            }
        } else emptyList()

        // 3. 始终获取 JSON 语法题目 (只要包含语法测试且题型支持)
        val isGrammarMc = contentType != "words" && questionType == QuestionType.MULTIPLE_CHOICE
        val jsonGrammarQuestions = if (isGrammarMc) {
            try {
                val levels = level.split(",")
                val allLoadedQuestions = mutableListOf<GrammarTestQuestion>()

                levels.forEach { lvl ->
                    val cleanLevel = lvl.trim()
                    if (cleanLevel.isNotEmpty()) {
                        val result = grammarTestRepository.loadQuestionsByLevel(cleanLevel)
                        if (result is Result.Success) {
                            allLoadedQuestions.addAll(result.data)
                        }
                    }
                }

                // 🎯 筛选：如果 source 不是 all，我们只保留匹配 target pool (grammars) 的题目
                val filteredQuestions = if (source != "all" && grammars.isNotEmpty()) {
                    val targetIds = grammars.map { it.id }.toSet()
                    allLoadedQuestions.filter { q ->
                        try {
                            val gid = extractNumericId(q.targetGrammarId)
                            targetIds.contains(gid)
                        } catch (_: Exception) {
                            false
                        }
                    }
                } else {
                    allLoadedQuestions
                }

                // 4. 实施方案 A：同考点随机抽取单一题目
                // 按 targetGrammarId 分组，每组只随机保留一个题目，确保考点覆盖面广而不重复
                filteredQuestions.groupBy { it.targetGrammarId }
                    .mapValues { (_, questions) -> questions.random() }
                    .values.toList()
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        // Create ID -> Grammar map for lookup
        val grammarEntityMap = if (isGrammarMc) {
             // 我们从 Distractor Pool 中构建 Map，以便所有题目都能找到对应的实体（即使题目不在 Target Pool 中）
             if (allGrammarsForDistractors.isNotEmpty()) {
                 allGrammarsForDistractors.associateBy { it.id }
             } else emptyMap()
        } else emptyMap()

        println("NemoTestDebug: Fetched data: TargetWords=${words.size}, DistractorWords=${allWordsForDistractors.size}, TargetGrammars=${grammars.size}, DistractorGrammars=${allGrammarsForDistractors.size}, JSON=${jsonGrammarQuestions.size}")



        // 2. 根据题型分布生成题目
        if (typeCounts != null) {
            println("NemoTestDebug: Generating with distribution: $typeCounts")
            val mcCount = typeCounts["multiple_choice"] ?: 0
            val typingCount = typeCounts["typing"] ?: 0

            // 2.1 生成打字题 (仅单词)
            if (typingCount > 0 && words.isNotEmpty()) {
                val typingWords = words.shuffled().take(typingCount)
                questions.addAll(typingWords.map { testQuestionFactory.createTyping(it) })
            }

            // 2.2 生成选择题 (单词 + 语法)
            if (mcCount > 0) {

                if (contentType == "mixed") {
                    // 混合模式
                    val wordMcCount = mcCount / 2
                    val grammarMcCount = mcCount - wordMcCount

                    if (words.isNotEmpty()) {
                        val sortedPool = when {
                            prioritizeNew -> words.sortedBy { it.repetitionCount }
                            prioritizeWrong -> words.sortedByDescending { it.difficulty }
                            else -> words.shuffled()
                        }
                        questions.addAll(sortedPool.take(wordMcCount).map {
                            testQuestionFactory.createMultipleChoice(it, mode, allWordsForDistractors, shuffleOptions)
                        })
                    }

                    if (jsonGrammarQuestions.isNotEmpty()) {
                         // ... JSON logic ...
                         val sortedJson = if (prioritizeNew || prioritizeWrong) {
                             jsonGrammarQuestions.sortedBy { q ->
                                 val id = try { extractNumericId(q.targetGrammarId) } catch(_:Exception){0}
                                 val g = grammarEntityMap[id]
                                 if (g != null) {
                                     if (prioritizeNew) g.repetitionCount.toDouble()
                                     else g.difficulty.toDouble()
                                 } else 0.0
                             }
                          } else jsonGrammarQuestions.shuffled()

                         questions.addAll(sortedJson.take(grammarMcCount).mapNotNull {
                             val q = testQuestionFactory.mapJsonToMultipleChoice(it, mode, grammarEntityMap, shuffleOptions)
                             if (q.grammar != null) q else null
                         })
                    } else if (grammars.isNotEmpty()) {
                        questions.addAll(grammars.shuffled().take(grammarMcCount).map {
                            testQuestionFactory.createGrammarMultipleChoice(it, mode, allGrammarsForDistractors, shuffleOptions)
                        })
                    }
                } else if (contentType == "words" && words.isNotEmpty()) {
                    val sortedPool = when {
                        prioritizeNew -> words.sortedBy { it.repetitionCount }
                        prioritizeWrong -> words.sortedByDescending { it.difficulty }
                        else -> words.shuffled()
                    }
                    questions.addAll(sortedPool.take(mcCount).map {
                        testQuestionFactory.createMultipleChoice(it, mode, allWordsForDistractors, shuffleOptions)
                    })
                } else if (contentType == "grammar") {
                    if (jsonGrammarQuestions.isNotEmpty()) {
                        questions.addAll(jsonGrammarQuestions.shuffled().take(mcCount).mapNotNull {
                             val q = testQuestionFactory.mapJsonToMultipleChoice(it, mode, grammarEntityMap, shuffleOptions)
                             if (q.grammar != null) q else null
                        })
                    } else if (grammars.isNotEmpty()) {
                        val sortedPool = when {
                            prioritizeNew -> grammars.sortedBy { it.repetitionCount }
                            prioritizeWrong -> grammars.sortedByDescending { it.difficulty }
                            else -> grammars.shuffled()
                        }
                        questions.addAll(sortedPool.take(mcCount).map {
                            testQuestionFactory.createGrammarMultipleChoice(it, mode, allGrammarsForDistractors, shuffleOptions)
                        })
                    }
                }
            }

            // 2.3 生成卡片题 (仅单词，每组5个单词)
            val cardMatchingCount = typeCounts["card_matching"] ?: 0
            if (cardMatchingCount > 0 && words.isNotEmpty()) {
                val cardMatchingWords = words.shuffled().take(cardMatchingCount * 5)
                val cardMatchingQuestions = generateCardMatchingQuestionsUseCase(
                    words = cardMatchingWords,
                    shuffle = false  // 已经shuffled过了
                )
                questions.addAll(cardMatchingQuestions)
            }

            // 2.4 生成排序题（复刻旧项目TestManager.kt行1089-1098）
            val sortingCount = typeCounts["sorting"] ?: 0
            if (sortingCount > 0) {
                if (contentType == "words" && words.isNotEmpty()) {
                    // 单词排序题
                    val sortingWords = words.shuffled().take(sortingCount)
                    questions.addAll(sortingWords.mapNotNull { word ->
                        if (word.hiragana.isBlank()) null
                        else testQuestionFactory.createSorting(word, shuffleOptions)
                    })
                } else if (contentType == "grammar" && grammars.isNotEmpty()) {
                    // 语法排序题（移除）
                } else if (contentType == "mixed") {
                    // 混合模式：只有单词有排序题
                    if (words.isNotEmpty()) {
                        val sortingWords = words.shuffled().take(sortingCount)
                        questions.addAll(sortingWords.mapNotNull { word ->
                            if (word.hiragana.isBlank()) null
                            else testQuestionFactory.createSorting(word, shuffleOptions)
                        })
                    }
                }
            }

            // Apply shuffleQuestions
            return if (shuffleQuestions) questions.shuffled() else questions
        }

        // 3. 传统逻辑 (无题型分布，使用单一 questionType)

        // 单词
        if (contentType != "grammar" && words.isNotEmpty()) {
            // 如果是混合模式，通常单词占一半。但如果题型不支持语法（如打字题），则全部分配给单词
            val isGrammarSupported = questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.SORTING
            val wordCount = if (contentType == "mixed" && isGrammarSupported) count / 2 else count

            // 🎯 Apply Prioritization Logic
            // If both true, verify precedence. Usually "Prioritize Wrong" might take precedence or mix?
            // Simple logic: sort based on flag.
            // Note: Data is already fetched.
            val sortedWords = when {
                prioritizeNew -> words.sortedBy { it.repetitionCount } // 0 is new
                prioritizeWrong -> words.sortedByDescending { it.difficulty } // High difficulty = hard/wrong
                else -> words.shuffled() // Default random if no priority (or if shuffle is on? Wait. If shuffleQuestions is off, we should keep order? But words came from Repo maybe ordered by ID).
                // Actually, if priority is OFF, we usually shuffle properties unless shuffleQuestions is off?
                // The words list from Repo might be ID ordered.
                // If prioritize is off, we select random words from the pool.
            }

            // If we are selecting a subset (take(wordCount)), we should shuffle the pool *before* taking if no priority is set,
            // otherwise we always take the first N words by ID.
            val selectedWords = if (prioritizeNew || prioritizeWrong) {
                sortedWords.take(wordCount)
            } else {
                words.shuffled().take(wordCount)
            }

            // 根据题型生成题目
            when (questionType) {
                QuestionType.CARD_MATCHING -> {
                    // 卡片题：需要count*5个单词，然后按5个一组生成题目
                    // If prioritizing, we take top N*5.
                    val pool = if (prioritizeNew || prioritizeWrong) sortedWords else words.shuffled()
                    val cardMatchingWords = pool.take(count * 5)
                    questions.addAll(generateCardMatchingQuestionsUseCase(
                        words = cardMatchingWords,
                        shuffle = false  // 已经shuffled过了 or sorted
                    ))
                }
                else -> {
                    questions.addAll(selectedWords.map { word ->
                        when (questionType) {
                            QuestionType.MULTIPLE_CHOICE -> testQuestionFactory.createMultipleChoice(word, mode, allWordsForDistractors, shuffleOptions)
                            QuestionType.TYPING -> testQuestionFactory.createTyping(word)
                            QuestionType.SORTING -> testQuestionFactory.createSorting(word, shuffleOptions)
                            else -> testQuestionFactory.createMultipleChoice(word, mode, allWordsForDistractors, shuffleOptions)
                        }
                    })
                }
            }
        }

        // 语法
        if (contentType != "words") {
            val grammarCount = if (contentType == "mixed") count - questions.size else count

            // Prioritize Grammars
            val sortedGrammars = when {
                prioritizeNew -> grammars.sortedBy { it.repetitionCount }
                prioritizeWrong -> grammars.sortedByDescending { it.difficulty }
                else -> grammars.shuffled()
            }
             val selectedGrammars = if (prioritizeNew || prioritizeWrong) {
                sortedGrammars.take(grammarCount)
            } else {
                grammars.shuffled().take(grammarCount)
            }

            if (questionType == QuestionType.SORTING && grammars.isNotEmpty()) {
                // 排序题 (单词保留，语法移除)
                 // Grammar Sorting removed.
            } else if (questionType == QuestionType.MULTIPLE_CHOICE) {
                // 选择题
                // 🎯 Use JSON questions if available
                if (jsonGrammarQuestions.isNotEmpty()) {
                     // For JSON questions, we don't have easy metrics for priority unless we link to entities.
                     // We did create grammarEntityMap.
                     val sortedJson = if (prioritizeNew || prioritizeWrong) {
                          jsonGrammarQuestions.sortedBy { q ->
                              val id = try { extractNumericId(q.targetGrammarId) } catch(_:Exception){0}
                              val g = grammarEntityMap[id]
                              if (g != null) {
                                  if (prioritizeNew) g.repetitionCount.toDouble()
                                  else g.difficulty.toDouble()
                              } else 0.0
                          }
                      } else jsonGrammarQuestions.shuffled()

                     questions.addAll(sortedJson.take(grammarCount).map {
                         testQuestionFactory.mapJsonToMultipleChoice(it, mode, grammarEntityMap, shuffleOptions)
                     })
                } else if (grammars.isNotEmpty()) {
                    questions.addAll(selectedGrammars.map { grammar ->
                        testQuestionFactory.createGrammarMultipleChoice(grammar, mode, allGrammarsForDistractors, shuffleOptions)
                    })
                }
            }
        }

        val result = if (shuffleQuestions) questions.shuffled() else questions

        println("NemoTestDebug: Generation complete. Total questions: ${result.size}")

        return result
    }

    private fun extractNumericId(id: String): Int {
        return try {
            val parts = id.split("_")
            val levelNum = parts[0].substring(1).toInt() // "N1" -> 1
            val num = parts[1].toInt()                // "001" -> 1
            levelNum * 1000 + num
        } catch (_: Exception) {
            0
        }
    }
}
