package com.jian.nemo.core.domain.model

/**
 * 测试结果
 * 包含测试的统计信息
 *
 * 参考: 旧项目 TestUiState
 * 参考: 阶段11文档
 */
data class TestResult(
    /** 总题目数 */
    val totalQuestions: Int,

    /** 正确题目数 */
    val correctCount: Int,

    /** 错误题目数 */
    val wrongCount: Int,

    /** 分数 (0-100) */
    val score: Int,

    /** 正确率 (0.0 - 1.0) */
    val accuracy: Float,

    /** 测试耗时(毫秒) */
    val timeSpentMs: Long,

    /** 测试开始时间戳（复刻旧项目） */
    val startTimeMs: Long = 0L,

    /** 测试结束时间戳（复刻旧项目） */
    val endTimeMs: Long = 0L,

    /** 完整的题目列表 (包含用户答案) */
    val questions: List<TestQuestion> = emptyList(),

    /** 错误的题目列表 */
    val wrongQuestions: List<TestQuestion> = emptyList(),

    /** 打字速度 (WPM)，仅打字测试有效 */
    val wpm: Int = 0
) {
    companion object {
        /** 创建测试结果 */
        fun create(
            questions: List<TestQuestion>,
            startTimeMs: Long,
            endTimeMs: Long
        ): TestResult {
            val totalQuestions = questions.size
            val correctCount = questions.count { it.isCorrect }
            val wrongCount = totalQuestions - correctCount
            val accuracy = if (totalQuestions > 0) {
                correctCount.toFloat() / totalQuestions
            } else 0f
            val score = (accuracy * 100).toInt()
            val timeSpentMs = endTimeMs - startTimeMs
            val wrongQuestions = questions.filter { !it.isCorrect }

            // 计算WPM
            var wpm = 0
            val typingQuestions = questions.filterIsInstance<TestQuestion.Typing>()
            if (typingQuestions.isNotEmpty() && timeSpentMs > 0) {
                val totalChars = typingQuestions.filter { it.isCorrect }.sumOf { it.correctAnswer.length }
                val minutes = timeSpentMs / 60000.0
                if (minutes > 0.01) {
                    wpm = ((totalChars / 5.0) / minutes).toInt()
                }
            }

            return TestResult(
                totalQuestions = totalQuestions,
                correctCount = correctCount,
                wrongCount = wrongCount,
                score = score,
                accuracy = accuracy,
                timeSpentMs = timeSpentMs,
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
                questions = questions,
                wrongQuestions = wrongQuestions,
                wpm = wpm
            )
        }
    }
}
