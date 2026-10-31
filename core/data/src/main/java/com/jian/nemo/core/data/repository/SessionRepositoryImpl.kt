package com.jian.nemo.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.data.datastore.PreferencesKeys
import com.jian.nemo.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习会话 Repository
 *
 * 管理学习会话的持久化状态，支持会话恢复
 *

 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : SessionRepository {
    /**
     * 保存学习会话
     *
     * @param wordIds 当前会话的单词ID列表
     * @param currentIndex 当前学习位置
     * @param level 学习等级 (n1-n5)
     */
    override suspend fun saveLearningSession(
        wordIds: List<Int>,
        currentIndex: Int,
        level: String
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SESSION_WORD_IDS] = json.encodeToString(wordIds)
            preferences[PreferencesKeys.SESSION_CURRENT_INDEX] = currentIndex
            preferences[PreferencesKeys.SESSION_LEVEL] = level
            val resetHour = preferences[com.jian.nemo.core.data.datastore.PreferencesKeys.LEARNING_DAY_RESET_HOUR] ?: 4
            preferences[PreferencesKeys.SESSION_START_DATE] = DateTimeUtils.getLearningDay(resetHour)
        }
        Log.d(TAG, "学习会话已保存: ${wordIds.size} 个单词，位置 $currentIndex")
    }

    /**
     * 获取学习会话
     *
     * @return Pair(wordIds, currentIndex)
     */
    override suspend fun getLearningSession(): Pair<List<Int>, Int> {
        val preferences = dataStore.data.first()
        val wordIdsJson = preferences[PreferencesKeys.SESSION_WORD_IDS] ?: ""
        val currentIndex = preferences[PreferencesKeys.SESSION_CURRENT_INDEX] ?: 0

        val wordIds = if (wordIdsJson.isNotEmpty()) {
            try {
                json.decodeFromString<List<Int>>(wordIdsJson)
            } catch (e: Exception) {
                Log.e(TAG, "解析会话数据失败", e)
                emptyList()
            }
        } else {
            emptyList()
        }

        return Pair(wordIds, currentIndex)
    }

    /**
     * 清空学习会话
     */
    override suspend fun clearLearningSession() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SESSION_WORD_IDS)
            preferences.remove(PreferencesKeys.SESSION_CURRENT_INDEX)
            preferences.remove(PreferencesKeys.SESSION_LEVEL)
            preferences.remove(PreferencesKeys.SESSION_START_DATE)
        }
        Log.d(TAG, "学习会话已清空")
    }

    /**
     * 检查会话是否有效
     *
     * 规则: 如果会话是逻辑上的今天创建的，则有效
     * @return true 如果会话有效
     */
    override suspend fun isSessionValid(): Boolean {
        // 由于 SessionRepositoryImpl 在 Data 层，无法直接循环注入 SettingsRepository
        // 这里采用 DateTimeUtils 提供的补偿时间加上系统默认重置逻辑，
        // 或者从 DataStore 直接读取重置时间（如果存在）。
        // 鉴于目前架构，学习会话本身就是短期的，使用物理日期 vs 逻辑日期影响较小
        // 但为了极致统一，我们尝试从 DataStore 读取 PreferencesKeys.LEARNING_DAY_RESET_HOUR
        val preferences = dataStore.data.first()
        val sessionDate = preferences[PreferencesKeys.SESSION_START_DATE] ?: 0L
        val resetHour = preferences[com.jian.nemo.core.data.datastore.PreferencesKeys.LEARNING_DAY_RESET_HOUR] ?: 4

        val today = DateTimeUtils.getLearningDay(resetHour)
        return sessionDate == today
    }

    companion object {
        private const val TAG = "SessionRepository"
    }
}
