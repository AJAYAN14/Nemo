package com.jian.nemo.core.data.repository

import com.jian.nemo.core.data.local.dao.WordRelationDao
import com.jian.nemo.core.domain.model.WordRelation
import com.jian.nemo.core.domain.repository.WordRelationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRelationRepositoryImpl @Inject constructor(
    private val wordRelationDao: WordRelationDao
) : WordRelationRepository {

    override fun getRelationsByWordId(wordId: Int): Flow<List<WordRelation>> {
        return wordRelationDao.getByWordId(wordId).map { entities ->
            entities.map { entity ->
                WordRelation(
                    id = entity.id,
                    wordId = entity.wordId,
                    relatedWordId = entity.relatedWordId,
                    relationType = entity.relationType,
                    note = entity.note
                )
            }
        }
    }
}
