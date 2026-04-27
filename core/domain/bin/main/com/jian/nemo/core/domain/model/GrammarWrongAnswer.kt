package com.jian.nemo.core.domain.model

/**
 * 语法错题Domain Model
 *
 * 与Entity的区别：
 * - 包含关联的Grammar对象
 * - 业务逻辑使用的数据结构
 */
data class GrammarWrongAnswer(
    val id: Int,
    val grammarId: Int,
    val grammar: Grammar?,  // 关联的语法信息
    val testMode: String,  // 测试模式
    val userAnswer: String,  // 用户的错误答案
    val correctAnswer: String,  // 正确答案
    val questionType: String = "multiple_choice",
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val explanation: String? = null,
    val timestamp: Long,  // 错误时间戳
    val consecutiveCorrectCount: Int = 0 // 连续答对次数
)
