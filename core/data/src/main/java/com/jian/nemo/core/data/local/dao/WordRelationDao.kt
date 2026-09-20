package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.WordRelationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordRelationDao {
    @Query("SELECT * FROM word_relations WHERE word_id = :wordId OR related_word_id = :wordId")
    fun getByWordId(wordId: Int): Flow<List<WordRelationEntity>>

    @Query("SELECT COUNT(*) FROM word_relations WHERE word_id = :wordId OR related_word_id = :wordId")
    suspend fun getCountByWordId(wordId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<WordRelationEntity>)

    @Query("DELETE FROM word_relations")
    suspend fun deleteAll()
}
