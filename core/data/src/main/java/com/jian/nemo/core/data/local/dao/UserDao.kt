package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jian.nemo.core.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUserSnapshot(): UserEntity?

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("UPDATE users SET avatar = :avatar WHERE id = :userId")
    suspend fun updateUserAvatar(userId: String, avatar: String?)

    @Query("UPDATE users SET username = :username WHERE id = :userId")
    suspend fun updateUsername(userId: String, username: String)

    @Query("UPDATE users SET email = :email WHERE id = :userId")
    suspend fun updateEmail(userId: String, email: String)
}
