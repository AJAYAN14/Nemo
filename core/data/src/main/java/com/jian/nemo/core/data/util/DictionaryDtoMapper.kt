package com.jian.nemo.core.data.util

import com.jian.nemo.core.data.local.entity.GrammarEntity
import com.jian.nemo.core.data.local.entity.GrammarExampleEntity
import com.jian.nemo.core.data.local.entity.GrammarUsageEntity
import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.domain.model.GrammarDto
import com.jian.nemo.core.domain.model.WordDto

/**
 * 将网络 WordDto 转换为 WordEntity
 */
fun WordDto.toEntity(): WordEntity {
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
    )
}

/**
 * 将网络 GrammarDto 转换为 GrammarEntity
 */
fun GrammarDto.toGrammarEntity(): GrammarEntity {
    return GrammarEntity(
        id = id,
        grammar = title,
        grammarLevel = level.uppercase(),
        isDelisted = isDelisted
    )
}

/**
 * 将网络 GrammarDto 的内容转换为 GrammarUsageEntity 列表
 */
fun GrammarDto.toUsageEntities(): List<GrammarUsageEntity> {
    val grammarId = id
    return content.mapIndexed { index, usage ->
        GrammarUsageEntity(
            grammarId = grammarId,
            subtype = usage.subtype,
            connection = usage.connection,
            explanation = usage.explanation,
            notes = usage.notes,
            usageOrder = index
        )
    }
}

/**
 * 将网络 GrammarDto 的内容转换为 GrammarExampleEntity 列表
 * @param usageIds 插入 usage 后返回的本地 ID 列表
 */
fun GrammarDto.toExampleEntities(usageIds: List<Long>): List<GrammarExampleEntity> {
    val result = mutableListOf<GrammarExampleEntity>()
    content.forEachIndexed { usageIndex, usage ->
        val usageId = usageIds[usageIndex].toInt()
        usage.examples.forEachIndexed { exampleIndex, example ->
            result.add(
                GrammarExampleEntity(
                    usageId = usageId,
                    sentence = example.sentence,
                    translation = example.translation,
                    source = example.source,
                    isDialog = example.isDialog,
                    exampleOrder = exampleIndex
                )
            )
        }
    }
    return result
}
