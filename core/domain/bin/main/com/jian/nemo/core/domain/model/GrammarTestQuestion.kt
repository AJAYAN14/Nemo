package com.jian.nemo.core.domain.model

import kotlinx.serialization.Serializable

data class GrammarTestQuestion(
    val id: String,
    val targetGrammarId: String,
    val targetUsageIndex: Int,
    val type: GrammarQuestionType,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
) {
    fun extractJlptLevel(): String {
        val parts = id.split("_")
        return if (parts.size >= 2) parts[1] else "N3"
    }

    fun getCorrectAnswerText(): String {
        return options.getOrNull(correctIndex) ?: options.firstOrNull() ?: ""
    }

    fun getCorrectAnswerLetter(): String {
        return ('A' + correctIndex).toString()
    }
}

@Serializable
enum class GrammarQuestionType {
    CHOICE,
    SORT,
    FILL
}
