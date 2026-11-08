package com.jian.nemo.core.data.mapper

import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import com.jian.nemo.core.data.mapper.WordMapper.toDomainModel
import com.jian.nemo.core.data.mapper.WordMapper.toDomainModels
import com.jian.nemo.core.data.mapper.WordMapper.toEntity
import com.jian.nemo.core.data.mapper.WordMapper.toStudyStateEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * WordMapper单元测试
 */
class WordMapperTest {

    @Test
    fun `entity to domain model conversion is correct`() {
        val entity = WordEntity(
            id = 1,
            rawId = "N5_001",
            japanese = "単語",
            hiragana = "たんご",
            chinese = "单词",
            level = "n5",
            pos = "名",
            example1 = "例文1",
            gloss1 = "释义1",
            example2 = "例文2",
            gloss2 = "释义2",
            example3 = "例文3",
            gloss3 = "释义3",
            isDelisted = false
        )
        
        val stateEntity = WordStudyStateEntity(
            wordId = 1,
            repetitionCount = 5,
            stability = 2.6f,
            difficulty = 5.0f,
            interval = 10,
            nextReviewDate = 12345L,
            lastReviewedDate = 12300L,
            firstLearnedDate = 12000L,
            isFavorite = true,
            isSkipped = false,
            buriedUntilDay = 0,
            lastModifiedTime = 99999L
        )

        val domain = WordMapper.toDomainModel(entity, stateEntity)

        assertEquals(entity.id, domain.id)
        assertEquals(entity.japanese, domain.japanese)
        assertEquals(entity.level, domain.level)
        assertEquals(stateEntity.repetitionCount, domain.repetitionCount)
        assertEquals(stateEntity.stability, domain.stability, 0.001f)
        assertEquals(stateEntity.difficulty, domain.difficulty, 0.001f)
        assertEquals(stateEntity.interval, domain.interval)
        assertEquals(stateEntity.isFavorite, domain.isFavorite)
    }

    @Test
    fun `loanword with english letters replaces japanese with hiragana`() {
        val entity = WordEntity(
            id = 10,
            rawId = "N5_010",
            japanese = "tennis",
            hiragana = "テニス",
            chinese = "网球",
            level = "n5",
            isDelisted = false
        )

        val domain = entity.toDomainModel()

        assertEquals("テニス", domain.japanese)
        assertEquals("テニス", domain.hiragana)
    }

    @Test
    fun `loanword with symbols and numbers replaces japanese with hiragana`() {
        val entity = WordEntity(
            id = 11,
            rawId = "N5_011",
            japanese = "Wi-Fi",
            hiragana = "ワイファイ",
            chinese = "无线网络",
            level = "n5",
            isDelisted = false
        )

        val domain = entity.toDomainModel()

        assertEquals("ワイファイ", domain.japanese)
        assertEquals("ワイファイ", domain.hiragana)
    }

    @Test
    fun `regular kanji and kana words keep original japanese`() {
        val kanjiEntity = WordEntity(
            id = 12,
            rawId = "N5_012",
            japanese = "学生",
            hiragana = "がくせい",
            chinese = "学生",
            level = "n5",
            isDelisted = false
        )
        val kanaEntity = WordEntity(
            id = 13,
            rawId = "N5_013",
            japanese = "かわいい",
            hiragana = "かわいい",
            chinese = "可爱",
            level = "n5",
            isDelisted = false
        )

        assertEquals("学生", kanjiEntity.toDomainModel().japanese)
        assertEquals("かわいい", kanaEntity.toDomainModel().japanese)
    }

    @Test
    fun `domain model to entity conversion is correct`() {
        val domain = com.jian.nemo.core.domain.model.Word(
            id = 2,
            rawId = "N4_002",
            japanese = "文法",
            hiragana = "ぶんぽう",
            chinese = "语法",
            level = "n4",
            pos = "名",
            example1 = "例句1",
            gloss1 = "解释1",
            example2 = "例句2",
            gloss2 = "解释2",
            example3 = "例句3",
            gloss3 = "解释3",
            repetitionCount = 3,
            stability = 2.5f,
            difficulty = 5.0f,
            interval = 5,
            nextReviewDate = 54321L,
            lastReviewedDate = 54000L,
            firstLearnedDate = 53000L,
            isFavorite = false,
            isSkipped = true,
            buriedUntilDay = 0,
            lastModifiedTime = 88888L,
            isDelisted = false
        )

        val entity = domain.toEntity()
        val stateEntity = domain.toStudyStateEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.japanese, entity.japanese)
        
        assertEquals(domain.id, stateEntity.wordId)
        assertEquals(domain.stability, stateEntity.stability, 0.001f)
        assertEquals(domain.difficulty, stateEntity.difficulty, 0.001f)
        assertEquals(domain.interval, stateEntity.interval)
        assertEquals(domain.isSkipped, stateEntity.isSkipped)
    }

    @Test
    fun `batch conversion works correctly`() {
        val entities = listOf(
            createTestEntity(id = 1),
            createTestEntity(id = 2),
            createTestEntity(id = 3)
        )

        val domainModels = entities.toDomainModels()

        assertEquals(3, domainModels.size)
        assertEquals(1, domainModels[0].id)
        assertEquals(2, domainModels[1].id)
        assertEquals(3, domainModels[2].id)
    }

    private fun createTestEntity(id: Int) = WordEntity(
        id = id,
        rawId = "N5_$id",
        japanese = "テスト$id",
        hiragana = "てすと$id",
        chinese = "测试$id",
        level = "n5",
        isDelisted = false
    )
}
