package com.jian.nemo.core.domain.model

/**
 * 学习模式
 * 用于区分单词学习和语法学习两种模式
 */
sealed class LearningMode {
    /**
     * 单词学习模式
     */
    data object Word : LearningMode()

    /**
     * 语法学习模式
     */
    data object Grammar : LearningMode()
}
