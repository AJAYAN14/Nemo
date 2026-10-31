package com.jian.nemo.core.data.mapper

import com.jian.nemo.core.data.local.entity.WrongAnswerEntity
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.model.WrongAnswer

/**
 * WrongAnswer Mapper
 *
 * Entity ↔ Domain Model 转换

 */
object WrongAnswerMapper {

    /**
     * Entity → Domain Model
     * @param word 关联的单词信息（需要外部提供）
     */
    fun WrongAnswerEntity.toDomainModel(word: Word? = null): WrongAnswer {
        return WrongAnswer(
            id = id,
            wordId = wordId,
            word = word,
            testMode = testMode,
            userAnswer = userAnswer,
            correctAnswer = correctAnswer,
            timestamp = timestamp,
            consecutiveCorrectCount = consecutiveCorrectCount
        )
    }

    /**
     * Domain Model → Entity
     */
    fun WrongAnswer.toEntity(): WrongAnswerEntity {
        return WrongAnswerEntity(
            id = id,
            wordId = wordId,
            testMode = testMode,
            userAnswer = userAnswer,
            correctAnswer = correctAnswer,
            timestamp = timestamp,
            consecutiveCorrectCount = consecutiveCorrectCount
        )
    }
}


