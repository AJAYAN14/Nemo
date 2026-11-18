package com.jian.nemo.feature.learning.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jian.nemo.core.domain.model.LearningMode
import com.jian.nemo.feature.learning.di.LearningStatsDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class RawStatsData(
    val lastResetDay: Long = 0L,
    val wordElapsedTime: Long = 0L,
    val grammarElapsedTime: Long = 0L,
    val wordCurrentCombo: Int = 0,
    val wordMaxCombo: Int = 0,
    val wordSessionNewCount: Int = 0,
    val wordSessionReviewCount: Int = 0,
    val wordSessionRelearnCount: Int = 0,
    val wordRelearnIds: String = "",
    val grammarCurrentCombo: Int = 0,
    val grammarMaxCombo: Int = 0,
    val grammarSessionNewCount: Int = 0,
    val grammarSessionReviewCount: Int = 0,
    val grammarSessionRelearnCount: Int = 0,
    val grammarRelearnIds: String = ""
)

interface LearningSessionStatsStorage {
    suspend fun loadStats(): RawStatsData
    suspend fun resetAllStats(today: Long)
    suspend fun saveElapsedTime(mode: LearningMode, today: Long, elapsedTime: Long)
    suspend fun saveModeStats(
        mode: LearningMode,
        today: Long,
        currentCombo: Int,
        maxCombo: Int,
        newCount: Int,
        reviewCount: Int,
        relearnCount: Int,
        relearnIds: String
    )
    suspend fun saveAllStats(
        mode: LearningMode,
        today: Long,
        elapsedTime: Long,
        currentCombo: Int,
        maxCombo: Int,
        newCount: Int,
        reviewCount: Int,
        relearnCount: Int,
        relearnIds: String
    )
}

@Singleton
class DataStoreLearningSessionStatsStorage @Inject constructor(
    @LearningStatsDataStore private val dataStore: DataStore<Preferences>
) : LearningSessionStatsStorage {

    companion object {
        val KEY_LAST_RESET_DAY = longPreferencesKey("last_reset_day")
        val KEY_WORD_ELAPSED_TIME = longPreferencesKey("word_elapsed_time")
        val KEY_GRAMMAR_ELAPSED_TIME = longPreferencesKey("grammar_elapsed_time")

        val KEY_WORD_CURRENT_COMBO = intPreferencesKey("word_current_combo")
        val KEY_WORD_MAX_COMBO = intPreferencesKey("word_max_combo")
        val KEY_WORD_SESSION_NEW_COUNT = intPreferencesKey("word_session_new_count")
        val KEY_WORD_SESSION_REVIEW_COUNT = intPreferencesKey("word_session_review_count")
        val KEY_WORD_SESSION_RELEARN_COUNT = intPreferencesKey("word_session_relearn_count")
        val KEY_WORD_RELEARN_IDS = stringPreferencesKey("word_relearn_ids")

        val KEY_GRAMMAR_CURRENT_COMBO = intPreferencesKey("grammar_current_combo")
        val KEY_GRAMMAR_MAX_COMBO = intPreferencesKey("grammar_max_combo")
        val KEY_GRAMMAR_SESSION_NEW_COUNT = intPreferencesKey("grammar_session_new_count")
        val KEY_GRAMMAR_SESSION_REVIEW_COUNT = intPreferencesKey("grammar_session_review_count")
        val KEY_GRAMMAR_SESSION_RELEARN_COUNT = intPreferencesKey("grammar_session_relearn_count")
        val KEY_GRAMMAR_RELEARN_IDS = stringPreferencesKey("grammar_relearn_ids")
    }

    override suspend fun loadStats(): RawStatsData {
        val prefs = dataStore.data.first()
        return RawStatsData(
            lastResetDay = prefs[KEY_LAST_RESET_DAY] ?: 0L,
            wordElapsedTime = prefs[KEY_WORD_ELAPSED_TIME] ?: 0L,
            grammarElapsedTime = prefs[KEY_GRAMMAR_ELAPSED_TIME] ?: 0L,
            wordCurrentCombo = prefs[KEY_WORD_CURRENT_COMBO] ?: 0,
            wordMaxCombo = prefs[KEY_WORD_MAX_COMBO] ?: 0,
            wordSessionNewCount = prefs[KEY_WORD_SESSION_NEW_COUNT] ?: 0,
            wordSessionReviewCount = prefs[KEY_WORD_SESSION_REVIEW_COUNT] ?: 0,
            wordSessionRelearnCount = prefs[KEY_WORD_SESSION_RELEARN_COUNT] ?: 0,
            wordRelearnIds = prefs[KEY_WORD_RELEARN_IDS] ?: "",
            grammarCurrentCombo = prefs[KEY_GRAMMAR_CURRENT_COMBO] ?: 0,
            grammarMaxCombo = prefs[KEY_GRAMMAR_MAX_COMBO] ?: 0,
            grammarSessionNewCount = prefs[KEY_GRAMMAR_SESSION_NEW_COUNT] ?: 0,
            grammarSessionReviewCount = prefs[KEY_GRAMMAR_SESSION_REVIEW_COUNT] ?: 0,
            grammarSessionRelearnCount = prefs[KEY_GRAMMAR_SESSION_RELEARN_COUNT] ?: 0,
            grammarRelearnIds = prefs[KEY_GRAMMAR_RELEARN_IDS] ?: ""
        )
    }

    override suspend fun resetAllStats(today: Long) {
        dataStore.edit { p ->
            p[KEY_LAST_RESET_DAY] = today
            p[KEY_WORD_ELAPSED_TIME] = 0L
            p[KEY_GRAMMAR_ELAPSED_TIME] = 0L
            p[KEY_WORD_CURRENT_COMBO] = 0
            p[KEY_WORD_MAX_COMBO] = 0
            p[KEY_WORD_SESSION_NEW_COUNT] = 0
            p[KEY_WORD_SESSION_REVIEW_COUNT] = 0
            p[KEY_WORD_SESSION_RELEARN_COUNT] = 0
            p[KEY_WORD_RELEARN_IDS] = ""
            p[KEY_GRAMMAR_CURRENT_COMBO] = 0
            p[KEY_GRAMMAR_MAX_COMBO] = 0
            p[KEY_GRAMMAR_SESSION_NEW_COUNT] = 0
            p[KEY_GRAMMAR_SESSION_REVIEW_COUNT] = 0
            p[KEY_GRAMMAR_SESSION_RELEARN_COUNT] = 0
            p[KEY_GRAMMAR_RELEARN_IDS] = ""
        }
    }

    override suspend fun saveElapsedTime(mode: LearningMode, today: Long, elapsedTime: Long) {
        dataStore.edit { p ->
            p[KEY_LAST_RESET_DAY] = today
            if (mode == LearningMode.Word) {
                p[KEY_WORD_ELAPSED_TIME] = elapsedTime
            } else {
                p[KEY_GRAMMAR_ELAPSED_TIME] = elapsedTime
            }
        }
    }

    override suspend fun saveModeStats(
        mode: LearningMode,
        today: Long,
        currentCombo: Int,
        maxCombo: Int,
        newCount: Int,
        reviewCount: Int,
        relearnCount: Int,
        relearnIds: String
    ) {
        dataStore.edit { p ->
            p[KEY_LAST_RESET_DAY] = today
            if (mode == LearningMode.Word) {
                p[KEY_WORD_CURRENT_COMBO] = currentCombo
                p[KEY_WORD_MAX_COMBO] = maxCombo
                p[KEY_WORD_SESSION_NEW_COUNT] = newCount
                p[KEY_WORD_SESSION_REVIEW_COUNT] = reviewCount
                p[KEY_WORD_SESSION_RELEARN_COUNT] = relearnCount
                p[KEY_WORD_RELEARN_IDS] = relearnIds
            } else {
                p[KEY_GRAMMAR_CURRENT_COMBO] = currentCombo
                p[KEY_GRAMMAR_MAX_COMBO] = maxCombo
                p[KEY_GRAMMAR_SESSION_NEW_COUNT] = newCount
                p[KEY_GRAMMAR_SESSION_REVIEW_COUNT] = reviewCount
                p[KEY_GRAMMAR_SESSION_RELEARN_COUNT] = relearnCount
                p[KEY_GRAMMAR_RELEARN_IDS] = relearnIds
            }
        }
    }

    override suspend fun saveAllStats(
        mode: LearningMode,
        today: Long,
        elapsedTime: Long,
        currentCombo: Int,
        maxCombo: Int,
        newCount: Int,
        reviewCount: Int,
        relearnCount: Int,
        relearnIds: String
    ) {
        dataStore.edit { p ->
            p[KEY_LAST_RESET_DAY] = today
            if (mode == LearningMode.Word) {
                p[KEY_WORD_ELAPSED_TIME] = elapsedTime
                p[KEY_WORD_CURRENT_COMBO] = currentCombo
                p[KEY_WORD_MAX_COMBO] = maxCombo
                p[KEY_WORD_SESSION_NEW_COUNT] = newCount
                p[KEY_WORD_SESSION_REVIEW_COUNT] = reviewCount
                p[KEY_WORD_SESSION_RELEARN_COUNT] = relearnCount
                p[KEY_WORD_RELEARN_IDS] = relearnIds
            } else {
                p[KEY_GRAMMAR_ELAPSED_TIME] = elapsedTime
                p[KEY_GRAMMAR_CURRENT_COMBO] = currentCombo
                p[KEY_GRAMMAR_MAX_COMBO] = maxCombo
                p[KEY_GRAMMAR_SESSION_NEW_COUNT] = newCount
                p[KEY_GRAMMAR_SESSION_REVIEW_COUNT] = reviewCount
                p[KEY_GRAMMAR_SESSION_RELEARN_COUNT] = relearnCount
                p[KEY_GRAMMAR_RELEARN_IDS] = relearnIds
            }
        }
    }
}

class SharedPreferencesLearningSessionStatsStorage(
    context: Context
) : LearningSessionStatsStorage {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("nemo_learning_time_prefs", Context.MODE_PRIVATE)

    override suspend fun loadStats(): RawStatsData {
        return RawStatsData(
            lastResetDay = sharedPrefs.getLong("last_reset_day", 0L),
            wordElapsedTime = sharedPrefs.getLong("word_elapsed_time", 0L),
            grammarElapsedTime = sharedPrefs.getLong("grammar_elapsed_time", 0L),
            wordCurrentCombo = sharedPrefs.getInt("word_current_combo", 0),
            wordMaxCombo = sharedPrefs.getInt("word_max_combo", 0),
            wordSessionNewCount = sharedPrefs.getInt("word_session_new_count", 0),
            wordSessionReviewCount = sharedPrefs.getInt("word_session_review_count", 0),
            wordSessionRelearnCount = sharedPrefs.getInt("word_session_relearn_count", 0),
            wordRelearnIds = sharedPrefs.getString("word_relearn_ids", "") ?: "",
            grammarCurrentCombo = sharedPrefs.getInt("grammar_current_combo", 0),
            grammarMaxCombo = sharedPrefs.getInt("grammar_max_combo", 0),
            grammarSessionNewCount = sharedPrefs.getInt("grammar_session_new_count", 0),
            grammarSessionReviewCount = sharedPrefs.getInt("grammar_session_review_count", 0),
            grammarSessionRelearnCount = sharedPrefs.getInt("grammar_session_relearn_count", 0),
            grammarRelearnIds = sharedPrefs.getString("grammar_relearn_ids", "") ?: ""
        )
    }

    override suspend fun resetAllStats(today: Long) {
        sharedPrefs.edit()
            .putLong("last_reset_day", today)
            .putLong("word_elapsed_time", 0L)
            .putLong("grammar_elapsed_time", 0L)
            .putInt("word_current_combo", 0)
            .putInt("word_max_combo", 0)
            .putInt("word_session_new_count", 0)
            .putInt("word_session_review_count", 0)
            .putInt("word_session_relearn_count", 0)
            .putString("word_relearn_ids", "")
            .putInt("grammar_current_combo", 0)
            .putInt("grammar_max_combo", 0)
            .putInt("grammar_session_new_count", 0)
            .putInt("grammar_session_review_count", 0)
            .putInt("grammar_session_relearn_count", 0)
            .putString("grammar_relearn_ids", "")
            .apply()
    }

    override suspend fun saveElapsedTime(mode: LearningMode, today: Long, elapsedTime: Long) {
        val key = if (mode == LearningMode.Word) "word_elapsed_time" else "grammar_elapsed_time"
        sharedPrefs.edit()
            .putLong("last_reset_day", today)
            .putLong(key, elapsedTime)
            .apply()
    }

    override suspend fun saveModeStats(
        mode: LearningMode,
        today: Long,
        currentCombo: Int,
        maxCombo: Int,
        newCount: Int,
        reviewCount: Int,
        relearnCount: Int,
        relearnIds: String
    ) {
        val prefix = if (mode == LearningMode.Word) "word_" else "grammar_"
        sharedPrefs.edit()
            .putLong("last_reset_day", today)
            .putInt("${prefix}current_combo", currentCombo)
            .putInt("${prefix}max_combo", maxCombo)
            .putInt("${prefix}session_new_count", sessionNewCountCompat(newCount))
            .putInt("${prefix}session_review_count", reviewCount)
            .putInt("${prefix}session_relearn_count", relearnCount)
            .putString("${prefix}relearn_ids", relearnIds)
            .apply()
    }

    override suspend fun saveAllStats(
        mode: LearningMode,
        today: Long,
        elapsedTime: Long,
        currentCombo: Int,
        maxCombo: Int,
        newCount: Int,
        reviewCount: Int,
        relearnCount: Int,
        relearnIds: String
    ) {
        val prefix = if (mode == LearningMode.Word) "word_" else "grammar_"
        val timeKey = if (mode == LearningMode.Word) "word_elapsed_time" else "grammar_elapsed_time"
        sharedPrefs.edit()
            .putLong("last_reset_day", today)
            .putLong(timeKey, elapsedTime)
            .putInt("${prefix}current_combo", currentCombo)
            .putInt("${prefix}max_combo", maxCombo)
            .putInt("${prefix}session_new_count", sessionNewCountCompat(newCount))
            .putInt("${prefix}session_review_count", reviewCount)
            .putInt("${prefix}session_relearn_count", relearnCount)
            .putString("${prefix}relearn_ids", relearnIds)
            .apply()
    }

    private fun sessionNewCountCompat(count: Int): Int = count
}
