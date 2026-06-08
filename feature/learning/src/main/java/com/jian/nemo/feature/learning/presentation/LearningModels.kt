package com.jian.nemo.feature.learning.presentation

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word

/**
 * 学习项封装 (统一 Word 和 Grammar)
 */
sealed class LearningItem {
    abstract val id: Int
    abstract val isNew: Boolean
    abstract val displayName: String
    abstract val repetitionCount: Int

    // 调度系统必需字段
    abstract val step: Int
    abstract val dueTime: Long
    abstract val type: Int

    data class WordItem(
        val word: Word,
        override val step: Int = 0,
        override val dueTime: Long = 0,
        override val type: Int = word.type
    ) : LearningItem() {
        override val id: Int get() = word.id
        override val isNew: Boolean get() = word.repetitionCount == 0
        override val displayName: String get() = word.japanese
        override val repetitionCount: Int get() = word.repetitionCount
    }

    data class GrammarItem(
        val grammar: Grammar,
        override val step: Int = 0,
        override val dueTime: Long = 0,
        override val type: Int = grammar.type
    ) : LearningItem() {
        override val id: Int get() = grammar.id
        override val isNew: Boolean get() = grammar.repetitionCount == 0
        override val displayName: String get() = grammar.grammar
        override val repetitionCount: Int get() = grammar.repetitionCount
    }
}

/**
 * 卡片状态标记
 */
enum class CardBadge {
    NEW,      // 新词 (Type 0)
    LEARNING, // 学习中 (Type 1)
    REVIEW,   // 复习 (Type 2)
    RELEARN   // 重学 (Type 3)
}
