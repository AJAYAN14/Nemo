package com.jian.nemo.core.data.mapper

import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import com.jian.nemo.core.domain.model.Word

/**
 * WordEntity ↔ Word 映射器
 *
 * 职责：数据层Entity和领域层Model之间的转换
 *
 * ⚠️ 注意字段名映射：
 * - Entity使用下划线: example_1
 * - Domain使用驼峰: example1
 */
object WordMapper {

    /**
     * 将 WordEntity 和 WordStudyStateEntity 组合转换为 Domain Model
     */
    fun toDomainModel(entity: WordEntity, state: WordStudyStateEntity?): Word {
        return Word(
            id = entity.id,
            japanese = entity.japanese,
            hiragana = entity.hiragana,
            chinese = entity.chinese,
            level = entity.level,
            pos = entity.pos,
            example1 = entity.example1,
            gloss1 = entity.gloss1,
            example2 = entity.example2,
            gloss2 = entity.gloss2,
            example3 = entity.example3,
            gloss3 = entity.gloss3,
            isDelisted = entity.isDelisted,
            // 进度信息从 StudyState 获取，若无则使用默认值
            repetitionCount = state?.repetitionCount ?: 0,
            stability = state?.stability ?: 0f,
            difficulty = state?.difficulty ?: 0f,
            interval = state?.interval ?: 0,
            nextReviewDate = state?.nextReviewDate ?: 0,
            lastReviewedDate = state?.lastReviewedDate,
            firstLearnedDate = state?.firstLearnedDate,
            isFavorite = state?.isFavorite ?: false,
            isSkipped = state?.isSkipped ?: false,
            buriedUntilDay = state?.buriedUntilDay ?: 0,
            type = state?.type ?: 0, // 🎯 补全 type 映射
            lastModifiedTime = state?.lastModifiedTime ?: 0L // 进度信息从 StudyState 获取，若无则使用默认值 0L
        )
    }

    /**
     * 兼容旧版调用 (仅从 Entity 转换，通常用于没有任何学习记录的生词)
     * ⚠️ 注意：此方法返回的 Word 进度字段均为默认值
     */
    fun WordEntity.toDomainModel(): Word = toDomainModel(this, null)


    /**
     * Domain Model转Entity
     */
    fun Word.toEntity(): WordEntity {
        return WordEntity(
            id = id,
            japanese = japanese,
            hiragana = hiragana,
            chinese = chinese,
            level = level,
            pos = pos,
            example1 = example1,
            gloss1 = gloss1,
            example2 = example2,
            gloss2 = gloss2,
            example3 = example3,
            gloss3 = gloss3,
            isDelisted = isDelisted
            // 进度字段已移除
        )
    }

    /**
     * 转换为状态实体
     */
    fun Word.toStudyStateEntity(): WordStudyStateEntity {
        return WordStudyStateEntity(
            wordId = id,
            repetitionCount = repetitionCount,
            stability = stability,
            difficulty = difficulty,
            interval = interval,
            nextReviewDate = nextReviewDate,
            lastReviewedDate = lastReviewedDate,
            firstLearnedDate = firstLearnedDate,
            isFavorite = isFavorite,
            isSkipped = isSkipped,
            buriedUntilDay = buriedUntilDay,
            type = type, // 🎯 补全 type 映射
            lastModifiedTime = lastModifiedTime
        )
    }

    /**
     * 批量转换扩展函数
     */
    fun List<WordEntity>.toDomainModels(): List<Word> {
        return map { it.toDomainModel() }
    }
}
