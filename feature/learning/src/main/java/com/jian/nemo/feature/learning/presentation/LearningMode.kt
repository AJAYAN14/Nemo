package com.jian.nemo.feature.learning.presentation

import android.content.Context
import androidx.annotation.StringRes
import com.jian.nemo.feature.learning.R

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

    /**
     * 模式显示名称资源 ID
     */
    val displayNameResId: Int
        @StringRes
        get() = when (this) {
            is Word -> R.string.learning_mode_word
            is Grammar -> R.string.learning_mode_grammar
        }

    /**
     * 获取模式显示名称
     */
    fun getDisplayName(context: Context? = null): String {
        return context?.getString(displayNameResId) ?: when (this) {
            is Word -> "\u5355\u8bcd"
            is Grammar -> "\u8bed\u6cd5"
        }
    }
}
