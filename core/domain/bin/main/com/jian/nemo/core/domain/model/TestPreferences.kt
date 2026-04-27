package com.jian.nemo.core.domain.model

/**
 * 测试偏好设置聚合类
 * 用于原子化地从 Repository 获取所有测试相关的配置，避免因为多个 Flow 组合导致的中间状态闪烁问题。
 */
data class TestPreferences(
    val questionCount: Int,
    val timeLimitMinutes: Int,
    val shuffleQuestions: Boolean,
    val shuffleOptions: Boolean,
    val autoAdvance: Boolean,
    val prioritizeWrong: Boolean,
    val prioritizeNew: Boolean,
    val questionSource: String,
    val wrongAnswerRemovalThreshold: Int,
    val testContentType: String,
    val selectedWordLevels: List<String>,
    val selectedGrammarLevels: List<String>,
    
    // 综合测试各个题型数量
    val comprehensiveMultipleChoiceCount: Int,
    val comprehensiveTypingCount: Int,
    val comprehensiveCardMatchingCount: Int,
    val comprehensiveSortingCount: Int
)
