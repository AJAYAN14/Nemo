package com.jian.nemo.core.data.validator

import android.util.Log
import com.jian.nemo.core.domain.model.*

/**
 * 备份数据校验器
 *
 * 在导入数据写入数据库之前，对反序列化后的 NemoExportData 进行全面校验和清洗。
 * - 设置类数据：越界值回退为安全默认值
 * - 学习记录数据：严重逻辑谬误的整条记录被剔除
 */
object BackupValidator {

    private const val TAG = "BackupValidator"

    // 已知的导出版本号
    private val SUPPORTED_VERSIONS = setOf(2)

    // 合理的时间范围：不能超过当前时间 + 365天
    private val MAX_FUTURE_MILLIS: Long
        get() = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000

    /**
     * 校验结果
     */
    data class ValidationResult(
        val data: NemoExportData,
        val skippedWordCount: Int = 0,
        val skippedGrammarCount: Int = 0,
        val settingsFixedCount: Int = 0,
        val warnings: List<String> = emptyList()
    ) {
        fun toSummary(): String {
            val parts = mutableListOf<String>()
            if (skippedWordCount > 0) parts.add("跳过 $skippedWordCount 条异常单词记录")
            if (skippedGrammarCount > 0) parts.add("跳过 $skippedGrammarCount 条异常语法记录")
            if (settingsFixedCount > 0) parts.add("修正 $settingsFixedCount 项异常设置")
            return if (parts.isEmpty()) "" else parts.joinToString("，")
        }
    }

    /**
     * 校验导出数据的版本号
     * @throws IllegalArgumentException 如果版本不支持
     */
    fun validateVersion(exportData: NemoExportData) {
        val version = exportData.exportInfo.version
        if (version !in SUPPORTED_VERSIONS) {
            throw IllegalArgumentException(
                "不支持的备份版本: $version（当前支持: ${SUPPORTED_VERSIONS.joinToString()}）"
            )
        }
    }

    /**
     * 全面校验并清洗数据
     */
    fun validate(exportData: NemoExportData): ValidationResult {
        validateVersion(exportData)

        var settingsFixedCount = 0
        val warnings = mutableListOf<String>()

        // 校验设置
        val cleanedSettings = exportData.userData.settings?.let { settings ->
            val result = validateSettings(settings)
            settingsFixedCount = result.second
            if (settingsFixedCount > 0) {
                warnings.add("$settingsFixedCount 项设置值超出合理范围，已回退为默认值")
            }
            result.first
        }

        // 校验单词进度
        val (cleanedWords, skippedWords) = validateWordProgress(exportData.userData.wordProgress)
        if (skippedWords > 0) {
            warnings.add("$skippedWords 条单词学习记录存在异常数据，已跳过")
            Log.w(TAG, "跳过 $skippedWords 条异常单词记录")
        }

        // 校验语法进度
        val (cleanedGrammars, skippedGrammars) = validateGrammarProgress(exportData.userData.grammarProgress)
        if (skippedGrammars > 0) {
            warnings.add("$skippedGrammars 条语法学习记录存在异常数据，已跳过")
            Log.w(TAG, "跳过 $skippedGrammars 条异常语法记录")
        }

        val cleanedUserData = exportData.userData.copy(
            settings = cleanedSettings,
            wordProgress = cleanedWords,
            grammarProgress = cleanedGrammars
        )

        return ValidationResult(
            data = exportData.copy(userData = cleanedUserData),
            skippedWordCount = skippedWords,
            skippedGrammarCount = skippedGrammars,
            settingsFixedCount = settingsFixedCount,
            warnings = warnings
        )
    }

    /**
     * 校验设置，越界值回退为默认值
     * @return Pair<清洗后的设置, 修正的字段数>
     */
    private fun validateSettings(settings: ExportAppSettings): Pair<ExportAppSettings, Int> {
        var fixedCount = 0

        fun <T : Comparable<T>> clamp(value: T, min: T, max: T, default: T, fieldName: String): T {
            return if (value in min..max) value
            else {
                fixedCount++
                Log.w(TAG, "设置字段 $fieldName 越界: $value，回退为默认值 $default")
                default
            }
        }

        val cleaned = settings.copy(
            dailyGoal = clamp(settings.dailyGoal, 1, 500, 50, "dailyGoal"),
            grammarDailyGoal = clamp(settings.grammarDailyGoal, 1, 500, 10, "grammarDailyGoal"),
            learningDayResetHour = clamp(settings.learningDayResetHour, 0, 23, 4, "learningDayResetHour"),
            testQuestionCount = clamp(settings.testQuestionCount, 1, 100, 10, "testQuestionCount"),
            testTimeLimitMinutes = clamp(settings.testTimeLimitMinutes, 1, 60, 10, "testTimeLimitMinutes"),
            learnAheadLimit = clamp(settings.learnAheadLimit, 0, 1440, 20, "learnAheadLimit"),
            targetRetention = clamp(settings.targetRetention, 0.5f, 1.0f, 0.9f, "targetRetention")
        )

        return Pair(cleaned, fixedCount)
    }

    /**
     * 校验单词学习进度列表
     * @return Pair<清洗后的列表, 被剔除的条数>
     */
    private fun validateWordProgress(words: List<WordProgress>): Pair<List<WordProgress>, Int> {
        val maxFuture = MAX_FUTURE_MILLIS
        val valid = mutableListOf<WordProgress>()
        var skipped = 0

        for (word in words) {
            if (!isProgressValid(word.stability, word.difficulty, word.interval,
                    word.nextReviewDate, word.lastModifiedTime, word.srsLevel, maxFuture)) {
                skipped++
                Log.w(TAG, "剔除异常单词记录: rawId=${word.rawId}, " +
                        "stability=${word.stability}, difficulty=${word.difficulty}, " +
                        "interval=${word.interval}, srsLevel=${word.srsLevel}")
                continue
            }
            valid.add(word)
        }

        return Pair(valid, skipped)
    }

    /**
     * 校验语法学习进度列表
     */
    private fun validateGrammarProgress(grammars: List<GrammarProgress>): Pair<List<GrammarProgress>, Int> {
        val maxFuture = MAX_FUTURE_MILLIS
        val valid = mutableListOf<GrammarProgress>()
        var skipped = 0

        for (grammar in grammars) {
            if (!isProgressValid(grammar.stability, grammar.difficulty, grammar.interval,
                    grammar.nextReviewDate, grammar.lastModifiedTime, grammar.srsLevel, maxFuture)) {
                skipped++
                Log.w(TAG, "剔除异常语法记录: rawId=${grammar.rawId}, " +
                        "stability=${grammar.stability}, difficulty=${grammar.difficulty}, " +
                        "interval=${grammar.interval}, srsLevel=${grammar.srsLevel}")
                continue
            }
            valid.add(grammar)
        }

        return Pair(valid, skipped)
    }

    /**
     * 通用的进度数据合理性检查
     */
    private fun isProgressValid(
        stability: Float,
        difficulty: Float,
        interval: Int,
        nextReviewDate: Long,
        lastModifiedTime: Long,
        srsLevel: Int,
        maxFuture: Long
    ): Boolean {
        if (stability < 0) return false
        if (difficulty < 0) return false
        if (interval < 0) return false
        if (srsLevel < 0) return false
        if (lastModifiedTime < 0) return false
        if (nextReviewDate < 0) return false
        if (nextReviewDate > maxFuture) return false
        return true
    }
}
