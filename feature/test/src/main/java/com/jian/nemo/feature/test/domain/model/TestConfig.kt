package com.jian.nemo.feature.test.domain.model

/**
 * 测试配置
 * 定义了用户在测试设置界面可以调整的所有参数
 */
data class TestConfig(
    // 基础设置
    val questionCount: Int = 10,              // 题目数量
    val timeLimitMinutes: Int = 10,            // 时间限制(分钟)，0表示无限制

    // 范围设置
    val questionSource: QuestionSource = QuestionSource.TODAY,       // 题目来源
    val testContentType: TestContentType = TestContentType.MIXED,    // 测试内容
    val selectedWordLevels: List<String> = listOf("N5", "N4", "N3", "N2", "N1"),    // 选中的单词等级
    val selectedGrammarLevels: List<String> = listOf("N5", "N4", "N3", "N2", "N1"), // 选中的语法等级

    // 行为设置
    val shuffleQuestions: Boolean = true,     // 题目乱序
    val shuffleOptions: Boolean = true,       // 选项乱序
    val autoAdvance: Boolean = true,          // 答对自动跳转
    val prioritizeWrong: Boolean = false,     // 错题优先 (在选题时优先选择错误率高的)
    val prioritizeNew: Boolean = false,       // 未学优先 (在选题时优先选择从未测试过的)
    val wrongAnswerRemovalThreshold: Int = 0, // 错题移除阈值 (答对几次从错题集移除)
    val autoPlayAudio: Boolean = true,        // 答对/提交后自动朗读假名
)
