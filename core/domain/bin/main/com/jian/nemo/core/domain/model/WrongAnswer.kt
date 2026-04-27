package com.jian.nemo.core.domain.model

/**
 * 单词错题Domain Model
 *
 * 与Entity的区别：
 * - 包含关联的Word对象
 * - 业务逻辑使用的数据结构
 */
data class WrongAnswer(
    val id: Int,
    val wordId: Int,
    val word: Word?,  // 关联的单词信息
    val testMode: String,  // 测试模式：multiple_choice, typing, matching, sorting
    val userAnswer: String,  // 用户的错误答案
    val correctAnswer: String,  // 正确答案
    val timestamp: Long,  // 错误时间戳
    val consecutiveCorrectCount: Int = 0 // 连续答对次数
)
