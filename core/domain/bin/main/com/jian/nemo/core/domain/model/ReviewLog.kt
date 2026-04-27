package com.jian.nemo.core.domain.model

/**
 * 复习日志模型
 * 用于记录用户的每一次复习行为，支持未来生成遗忘曲线
 */
data class ReviewLog(
    val id: Long = 0,
    val itemId: Int,           // 单词/语法ID
    val itemType: String,      // "word" 或 "grammar"
    val reviewDate: Long,      // 复习时间戳
    val intervalDays: Int,     // 本次复习时的间隔 (天)
    val rating: Int            // 评分 0-5
)
