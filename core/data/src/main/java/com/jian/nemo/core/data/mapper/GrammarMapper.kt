package com.jian.nemo.core.data.mapper

import com.jian.nemo.core.data.local.entity.GrammarEntity
import com.jian.nemo.core.data.local.entity.GrammarExampleEntity
import com.jian.nemo.core.data.local.entity.GrammarStudyStateEntity
import com.jian.nemo.core.data.local.entity.relations.GrammarWithUsages
import com.jian.nemo.core.data.local.entity.relations.UsageWithExamples
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarExample
import com.jian.nemo.core.domain.model.GrammarUsage

/**
 * GrammarEntity ↔ Grammar 映射器（重构版）
 *
 * 职责：数据层关联实体和领域层Model之间的转换
 *
 * 新结构：
 * - GrammarWithUsages (Entity) ↔ Grammar (Domain)
 * - UsageWithExamples (Entity) ↔ GrammarUsage (Domain)
 * - GrammarExampleEntity (Entity) ↔ GrammarExample (Domain)
 */
object GrammarMapper {

    /**
     * 关联实体转领域模型
     * GrammarWithUsages -> Grammar
     */
    fun GrammarWithUsages.toDomainModel(): Grammar {
        return Grammar(
            id = grammar.id,
            rawId = grammar.rawId,
            grammar = grammar.grammar,
            grammarLevel = grammar.grammarLevel,
            isDelisted = grammar.isDelisted,
            usages = usages.sortedBy { it.usage.usageOrder }.map { it.toDomainModel() },
            // 进度信息从关联的 state 获取
            repetitionCount = state?.repetitionCount ?: 0,
            interval = state?.interval ?: 0,
            stability = state?.stability ?: 0f,
            difficulty = state?.difficulty ?: 0f,
            nextReviewDate = state?.nextReviewDate ?: 0,
            lastReviewedDate = state?.lastReviewedDate,
            firstLearnedDate = state?.firstLearnedDate,
            isFavorite = state?.isFavorite ?: false,
            isSkipped = state?.isSkipped ?: false,
            buriedUntilDay = state?.buriedUntilDay ?: 0,
            type = state?.type ?: 0, // 🎯 补全 type 映射
            lastModifiedTime = state?.lastModifiedTime ?: 0L
        )
    }

    /**
     * 用法关联实体转领域模型
     * UsageWithExamples -> GrammarUsage
     */
    fun UsageWithExamples.toDomainModel(): GrammarUsage {
        return GrammarUsage(
            subtype = usage.subtype,
            connection = usage.connection,
            explanation = usage.explanation,
            notes = usage.notes,
            examples = examples.sortedBy { it.exampleOrder }.map { it.toDomainModel() }
        )
    }

    /**
     * 例句实体转领域模型
     * GrammarExampleEntity -> GrammarExample
     */
    fun GrammarExampleEntity.toDomainModel(): GrammarExample {
        return GrammarExample(
            sentence = sentence,
            translation = translation,
            source = source,
            isDialog = isDialog
        )
    }

    /**
     * 领域模型转实体（仅主表）
     * Grammar -> GrammarEntity
     *
     * 注意：这个方法只转换主表数据（用于更新SRS状态等）
     * 不包含 usages 和 examples，因为这些需要单独插入
     */
    fun Grammar.toEntity(): GrammarEntity {
        return GrammarEntity(
            id = id,
            rawId = rawId,
            grammar = grammar,
            grammarLevel = grammarLevel,
            isDelisted = isDelisted
        )
    }

    /**
     * 转换为状态实体
     */
    fun Grammar.toStudyStateEntity(): GrammarStudyStateEntity {
        return GrammarStudyStateEntity(
            grammarId = id,
            repetitionCount = repetitionCount,
            interval = interval,
            stability = stability,
            difficulty = difficulty,
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
    fun List<GrammarWithUsages>.toDomainModels(): List<Grammar> {
        return map { it.toDomainModel() }
    }
}
