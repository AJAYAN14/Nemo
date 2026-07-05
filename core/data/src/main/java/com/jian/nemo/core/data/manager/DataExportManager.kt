package com.jian.nemo.core.data.manager

import android.content.Context
import android.util.Log

import com.jian.nemo.core.data.local.NemoDatabase
import com.jian.nemo.core.data.local.entity.GrammarWrongAnswerEntity
import com.jian.nemo.core.data.local.entity.WrongAnswerEntity
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import com.jian.nemo.core.data.local.entity.GrammarStudyStateEntity
import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.data.local.entity.GrammarEntity
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.jian.nemo.core.data.local.entity.StudyRecordEntity
import com.jian.nemo.core.data.local.entity.FavoriteQuestionEntity
import com.jian.nemo.core.domain.model.*
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.DataExportRepository
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import android.util.Base64OutputStream
import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.jian.nemo.core.data.validator.BackupValidator


/**
 * 导入结果
 */
data class ImportResult(
    val success: Boolean,
    val message: String
)

/**
 * 导入策略
 */
enum class ImportStrategy {
    /** 智能合并：只更新较新的记录 */
    MERGE,
    /** 完全覆盖：清空本地进度后全量写入 */
    REPLACE
}

/**
 * 导入统计（精确计数）
 */
data class ImportStats(
    val wordUpdateCount: Int = 0,
    val wordInsertCount: Int = 0,
    val wordSkipCount: Int = 0,
    val grammarUpdateCount: Int = 0,
    val grammarInsertCount: Int = 0,
    val grammarSkipCount: Int = 0
)

/**
 * 导入预览结果（dry-run 模式）
 */
data class ImportPreview(
    val strategy: ImportStrategy,
    val wordUpdateCount: Int = 0,
    val wordInsertCount: Int = 0,
    val wordSkipCount: Int = 0,
    val grammarUpdateCount: Int = 0,
    val grammarInsertCount: Int = 0,
    val grammarSkipCount: Int = 0,
    val wrongAnswerNewCount: Int = 0,
    val testRecordNewCount: Int = 0,
    val studyRecordNewCount: Int = 0,
    val favoriteNewCount: Int = 0,
    val localWordStateCount: Int = 0,
    val localGrammarStateCount: Int = 0,
    val settingsWillChange: Boolean = false,
    val validationSummary: String = ""
)


/**
 * 数据导出/导入管理器
 *
 * 负责将本地数据库数据导出为 JSON 格式（兼容旧版 Nemo），
 * 以及从 JSON 文件导入数据并恢复到本地数据库。
 */
@Singleton
class DataExportManager @Inject constructor(
    private val database: NemoDatabase,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : DataExportRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }



    /**
     * 流式导出数据到文件
     *
     * 采用流式处理管道：
     * Database Cursor -> JsonWriter -> OutputStreamWriter -> GZIPOutputStream -> Base64OutputStream -> FileOutputStream
     *
     * @param userId 用户ID
     * @param outputFile 输出文件
     * @return 输出文件
     */
    suspend fun exportDataToFile(userId: String = "default_user", outputFile: java.io.File, isCompressed: Boolean = true): java.io.File = withContext(Dispatchers.IO) {
        Log.d(TAG, "开始流式导出数据到文件: ${outputFile.absolutePath}")

        val totalStudyDays = settingsRepository.totalStudyDaysFlow.first()
        val dailyStreak = settingsRepository.dailyStreakFlow.first()
        val maxTestStreak = settingsRepository.maxTestStreakFlow.first()
        val testStreak = settingsRepository.testStreakFlow.first()

        var wordCount = 0
        var grammarCount = 0

        try {
            database.withTransaction {
                val wordIdToRawIdMap = database.wordDao().getAllWordsSync().associate { it.id to it.rawId }
                val grammarIdToRawIdMap = database.grammarDao().getAllGrammarsSync().associate { it.id to it.rawId }

                FileOutputStream(outputFile).use { fileOs ->
                    val base64Os = if (isCompressed) Base64OutputStream(fileOs, Base64.NO_WRAP) else null
                    try {
                        val gzipOs = if (isCompressed) GZIPOutputStream(base64Os) else null
                        try {
                            val targetOs = if (isCompressed) gzipOs!! else fileOs
                            java.io.BufferedWriter(OutputStreamWriter(targetOs, Charsets.UTF_8)).use { writer ->

                                writer.write("{")

                                writer.write("\"exportInfo\":")
                                writer.write(json.encodeToString(ExportInfo()))
                                writer.write(",")

                                writer.write("\"userData\":{")

                                writer.write("\"profile\":")
                                writer.write(json.encodeToString(UserProfile(userId, "User", "")))
                                writer.write(",")

                                val settingsSnapshot = settingsRepository.getAppSettingsSnapshot()
                                val exportSettings = ExportAppSettings(
                                    dailyGoal = settingsSnapshot.dailyGoal,
                                    grammarDailyGoal = settingsSnapshot.grammarDailyGoal,
                                    learningDayResetHour = settingsSnapshot.learningDayResetHour,
                                    testQuestionCount = settingsSnapshot.testQuestionCount,
                                    testTimeLimitMinutes = settingsSnapshot.testTimeLimitMinutes,
                                    testShuffleQuestions = settingsSnapshot.testShuffleQuestions,
                                    testShuffleOptions = settingsSnapshot.testShuffleOptions,
                                    testAutoAdvance = settingsSnapshot.testAutoAdvance,
                                    testPrioritizeWrong = settingsSnapshot.testPrioritizeWrong,
                                    testPrioritizeNew = settingsSnapshot.testPrioritizeNew,
                                    testQuestionSource = settingsSnapshot.testQuestionSource,
                                    testWrongAnswerRemovalThreshold = settingsSnapshot.testWrongAnswerRemovalThreshold,
                                    testContentType = settingsSnapshot.testContentType,
                                    testSelectedWordLevels = settingsSnapshot.testSelectedWordLevels,
                                    testSelectedGrammarLevels = settingsSnapshot.testSelectedGrammarLevels,
                                    learningSteps = settingsSnapshot.learningSteps,
                                    learnAheadLimit = settingsSnapshot.learnAheadLimit,
                                    relearningSteps = settingsSnapshot.relearningSteps,
                                    isRandomNewContentEnabled = settingsSnapshot.isRandomNewContentEnabled,
                                    targetRetention = settingsSnapshot.targetRetention,
                                    aiWorkshopDifficulty = settingsSnapshot.aiWorkshopDifficulty,
                                    aiReadingTheme = settingsSnapshot.aiReadingTheme,
                                    aiReadingDifficulty = settingsSnapshot.aiReadingDifficulty
                                )
                                writer.write("\"settings\":${json.encodeToString(exportSettings)},")

                                writer.write("\"wordProgress\":[")
                                var isFirstWord = true
                                database.wordDao().getExportWordsCursor().use { cursor ->
                                    val idIdx = cursor.getColumnIndexOrThrow("id")
                                    val repIdx = cursor.getColumnIndexOrThrow("repetitionCount")
                                    val stabIdx = cursor.getColumnIndexOrThrow("stability")
                                    val diffIdx = cursor.getColumnIndexOrThrow("difficulty")
                                    val intIdx = cursor.getColumnIndexOrThrow("interval")
                                    val nextReviewIdx = cursor.getColumnIndexOrThrow("nextReviewDate")
                                    val favIdx = cursor.getColumnIndexOrThrow("isFavorite")
                                    val skipIdx = cursor.getColumnIndexOrThrow("isSkipped")
                                    val lastModIdx = cursor.getColumnIndexOrThrow("lastModifiedTime")
                                    val lastRevIdx = cursor.getColumnIndexOrThrow("lastReviewedDate")
                                    val firstLearnIdx = cursor.getColumnIndexOrThrow("firstLearnedDate")
                                    val rawIdIdx = cursor.getColumnIndexOrThrow("raw_id")
                                    val delIdx = cursor.getColumnIndexOrThrow("isDeleted")

                                    while (cursor.moveToNext()) {
                                        if (!isFirstWord) writer.write(",")
                                        isFirstWord = false

                                        val word = WordProgress(
                                            wordId = cursor.getInt(idIdx),
                                            srsLevel = cursor.getInt(repIdx),
                                            stability = cursor.getFloat(stabIdx),
                                            difficulty = cursor.getFloat(diffIdx),
                                            interval = cursor.getInt(intIdx),
                                            nextReviewDate = cursor.getLong(nextReviewIdx),
                                            isFavorite = cursor.getInt(favIdx) == 1,
                                            isSkipped = cursor.getInt(skipIdx) == 1,
                                            lastModifiedTime = cursor.getLong(lastModIdx),
                                            lastReviewedDate = if (cursor.isNull(lastRevIdx)) null else cursor.getLong(lastRevIdx),
                                            firstLearnedDate = if (cursor.isNull(firstLearnIdx)) null else cursor.getLong(firstLearnIdx),
                                            isDeleted = cursor.getInt(delIdx) == 1,
                                            deletedTime = cursor.getLong(cursor.getColumnIndexOrThrow("deletedTime")),
                                            rawId = cursor.getString(rawIdIdx)
                                        )
                                        writer.write(json.encodeToString(word))
                                        wordCount++
                                    }
                                }
                                writer.write("],")

                                writer.write("\"grammarProgress\":[")
                                var isFirstGrammar = true
                                database.grammarDao().getExportGrammarsCursor().use { cursor ->
                                    val idIdx = cursor.getColumnIndexOrThrow("id")
                                    val repIdx = cursor.getColumnIndexOrThrow("repetitionCount")
                                    val stabIdx = cursor.getColumnIndexOrThrow("stability")
                                    val diffIdx = cursor.getColumnIndexOrThrow("difficulty")
                                    val intIdx = cursor.getColumnIndexOrThrow("interval")
                                    val nextReviewIdx = cursor.getColumnIndexOrThrow("nextReviewDate")
                                    val favIdx = cursor.getColumnIndexOrThrow("isFavorite")
                                    val lastModIdx = cursor.getColumnIndexOrThrow("lastModifiedTime")
                                    val lastRevIdx = cursor.getColumnIndexOrThrow("lastReviewedDate")
                                    val firstLearnIdx = cursor.getColumnIndexOrThrow("firstLearnedDate")
                                    val rawIdIdx = cursor.getColumnIndexOrThrow("raw_id")
                                    val delIdx = cursor.getColumnIndexOrThrow("isDeleted")

                                    while (cursor.moveToNext()) {
                                        if (!isFirstGrammar) writer.write(",")
                                        isFirstGrammar = false

                                        val grammar = GrammarProgress(
                                            grammarId = cursor.getInt(idIdx),
                                            srsLevel = cursor.getInt(repIdx),
                                            stability = cursor.getFloat(stabIdx),
                                            difficulty = cursor.getFloat(diffIdx),
                                            interval = cursor.getInt(intIdx),
                                            nextReviewDate = cursor.getLong(nextReviewIdx),
                                            isFavorite = cursor.getInt(favIdx) == 1,
                                            lastModifiedTime = cursor.getLong(lastModIdx),
                                            lastReviewedDate = if (cursor.isNull(lastRevIdx)) null else cursor.getLong(lastRevIdx),
                                            firstLearnedDate = if (cursor.isNull(firstLearnIdx)) null else cursor.getLong(firstLearnIdx),
                                            isDeleted = cursor.getInt(delIdx) == 1,
                                            deletedTime = cursor.getLong(cursor.getColumnIndexOrThrow("deletedTime")),
                                            rawId = cursor.getString(rawIdIdx)
                                        )
                                        writer.write(json.encodeToString(grammar))
                                        grammarCount++
                                    }
                                }
                                writer.write("],")

                                writer.write("\"wrongAnswers\":{")
                                writer.write("\"words\":[")
                                var isFirstWaWord = true
                                database.wrongAnswerDao().getExportWrongAnswersCursor().use { cursor ->
                                    val wIdIdx = cursor.getColumnIndexOrThrow("word_id")
                                    val tsIdx = cursor.getColumnIndexOrThrow("timestamp")
                                    val modeIdx = cursor.getColumnIndexOrThrow("test_mode")
                                    val userAnsIdx = cursor.getColumnIndexOrThrow("user_answer")
                                    val corrAnsIdx = cursor.getColumnIndexOrThrow("correct_answer")
                                    val uuidIdx = cursor.getColumnIndexOrThrow("uuid")

                                    while (cursor.moveToNext()) {
                                        val wordId = cursor.getInt(wIdIdx)
                                        val rawId = wordIdToRawIdMap[wordId]
                                        if (rawId == null) continue

                                        if (!isFirstWaWord) writer.write(",")
                                        isFirstWaWord = false

                                        val item = WrongAnswerItem(
                                            wordId = wordId,
                                            timestamp = cursor.getLong(tsIdx),
                                            testMode = cursor.getString(modeIdx),
                                            userAnswer = cursor.getString(userAnsIdx),
                                            correctAnswer = cursor.getString(corrAnsIdx),
                                            uuid = cursor.getString(uuidIdx),
                                            rawId = rawId
                                        )
                                        writer.write(json.encodeToString(item))
                                    }
                                }
                                writer.write("],")

                                writer.write("\"grammars\":[")
                                var isFirstWaGrammar = true
                                database.grammarWrongAnswerDao().getExportWrongAnswersCursor().use { cursor ->
                                    val gIdIdx = cursor.getColumnIndexOrThrow("grammar_id")
                                    val tsIdx = cursor.getColumnIndexOrThrow("timestamp")
                                    val modeIdx = cursor.getColumnIndexOrThrow("test_mode")
                                    val userAnsIdx = cursor.getColumnIndexOrThrow("user_answer")
                                    val corrAnsIdx = cursor.getColumnIndexOrThrow("correct_answer")
                                    val uuidIdx = cursor.getColumnIndexOrThrow("uuid")

                                    while (cursor.moveToNext()) {
                                        val grammarId = cursor.getInt(gIdIdx)
                                        val rawId = grammarIdToRawIdMap[grammarId]
                                        if (rawId == null) continue

                                        if (!isFirstWaGrammar) writer.write(",")
                                        isFirstWaGrammar = false

                                        val item = GrammarWrongAnswerItem(
                                            grammarId = grammarId,
                                            timestamp = cursor.getLong(tsIdx),
                                            testMode = cursor.getString(modeIdx),
                                            userAnswer = cursor.getString(userAnsIdx),
                                            correctAnswer = cursor.getString(corrAnsIdx),
                                            uuid = cursor.getString(uuidIdx),
                                            rawId = rawId
                                        )
                                        writer.write(json.encodeToString(item))
                                    }
                                }
                                writer.write("]")
                                writer.write("},")

                                writer.write("\"testRecords\":[")
                                var isFirstTestRecord = true
                                database.testRecordDao().getExportTestRecordsCursor().use { cursor ->
                                    val dateIdx = cursor.getColumnIndexOrThrow("date")
                                    val totIdx = cursor.getColumnIndexOrThrow("total_questions")
                                    val corrIdx = cursor.getColumnIndexOrThrow("correct_answers")
                                    val modeIdx = cursor.getColumnIndexOrThrow("test_mode")
                                    val tsIdx = cursor.getColumnIndexOrThrow("timestamp")
                                    val uuidIdx = cursor.getColumnIndexOrThrow("uuid")

                                    while (cursor.moveToNext()) {
                                        if (!isFirstTestRecord) writer.write(",")
                                        isFirstTestRecord = false

                                        val item = TestRecordItem(
                                            date = cursor.getLong(dateIdx),
                                            totalQuestions = cursor.getInt(totIdx),
                                            correctAnswers = cursor.getInt(corrIdx),
                                            testMode = cursor.getString(modeIdx),
                                            timestamp = cursor.getLong(tsIdx),
                                            uuid = cursor.getString(uuidIdx)
                                        )
                                        writer.write(json.encodeToString(item))
                                    }
                                }
                                writer.write("],")

                                writer.write("\"studyRecords\":[")
                                var isFirstStudyRecord = true
                                database.studyRecordDao().getExportStudyRecordsCursor().use { cursor ->
                                    val dateIdx = cursor.getColumnIndexOrThrow("date")
                                    val lwIdx = cursor.getColumnIndexOrThrow("learned_words")
                                    val lgIdx = cursor.getColumnIndexOrThrow("learned_grammars")
                                    val rwIdx = cursor.getColumnIndexOrThrow("reviewed_words")
                                    val rgIdx = cursor.getColumnIndexOrThrow("reviewed_grammars")
                                    val swIdx = cursor.getColumnIndexOrThrow("skipped_words")
                                    val sgIdx = cursor.getColumnIndexOrThrow("skipped_grammars")
                                    val tcIdx = cursor.getColumnIndexOrThrow("test_count")
                                    val tsIdx = cursor.getColumnIndexOrThrow("timestamp")

                                    while (cursor.moveToNext()) {
                                        if (!isFirstStudyRecord) writer.write(",")
                                        isFirstStudyRecord = false

                                        val item = StudyRecordItem(
                                            date = cursor.getLong(dateIdx),
                                            learnedWords = cursor.getInt(lwIdx),
                                            learnedGrammars = cursor.getInt(lgIdx),
                                            reviewedWords = cursor.getInt(rwIdx),
                                            reviewedGrammars = cursor.getInt(rgIdx),
                                            skippedWords = cursor.getInt(swIdx),
                                            skippedGrammars = cursor.getInt(sgIdx),
                                            testCount = cursor.getInt(tcIdx),
                                            timestamp = cursor.getLong(tsIdx)
                                        )
                                        writer.write(json.encodeToString(item))
                                    }
                                }
                                writer.write("],")

                                writer.write("\"favoriteQuestions\":[")
                                var isFirstFavorite = true
                                database.favoriteQuestionDao().getExportFavoritesCursor().use { cursor ->
                                    val idIdx = cursor.getColumnIndexOrThrow("id")
                                    val grammarIdIdx = cursor.getColumnIndexOrThrow("grammar_id")
                                    val jsonIdIdx = cursor.getColumnIndexOrThrow("json_id")
                                    val questionTypeIdx = cursor.getColumnIndexOrThrow("question_type")
                                    val questionTextIdx = cursor.getColumnIndexOrThrow("question_text")
                                    val optionsJsonIdx = cursor.getColumnIndexOrThrow("options_json")
                                    val correctAnswerIdx = cursor.getColumnIndexOrThrow("correct_answer")
                                    val explanationIdx = cursor.getColumnIndexOrThrow("explanation")
                                    val tsIdx = cursor.getColumnIndexOrThrow("timestamp")

                                    while (cursor.moveToNext()) {
                                        if (!isFirstFavorite) writer.write(",")
                                        isFirstFavorite = false

                                        val item = FavoriteQuestionItem(
                                            id = cursor.getInt(idIdx),
                                            grammarId = if (cursor.isNull(grammarIdIdx)) null else cursor.getInt(grammarIdIdx),
                                            jsonId = if (cursor.isNull(jsonIdIdx)) null else cursor.getString(jsonIdIdx),
                                            questionType = cursor.getString(questionTypeIdx),
                                            questionText = cursor.getString(questionTextIdx),
                                            optionsJson = cursor.getString(optionsJsonIdx),
                                            correctAnswer = cursor.getString(correctAnswerIdx),
                                            explanation = if (cursor.isNull(explanationIdx)) null else cursor.getString(explanationIdx),
                                            timestamp = cursor.getLong(tsIdx)
                                        )
                                        writer.write(json.encodeToString(item))
                                    }
                                }
                                writer.write("],")

                                writer.write("\"studyStreak\":$dailyStreak,")
                                writer.write("\"testStreak\":$testStreak,")
                                writer.write("\"maxTestStreak\":$maxTestStreak,")
                                writer.write("\"totalStudyDays\":$totalStudyDays")

                                writer.write("}")
                                writer.write("}")
                            }
                        } finally {
                            gzipOs?.close()
                        }
                    } finally {
                        base64Os?.close()
                    }
                }
            }
            Log.d(TAG, "流式导出完成: 单词$wordCount, 语法$grammarCount")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "流式导出失败", e)
            throw e
        }
    }


    /**
     * 导入数据
     * @param dataString 可能是压缩的 Base64 字符串，也可能是原始 JSON
     */
    suspend fun importData(dataString: String, strategy: ImportStrategy = ImportStrategy.MERGE): ImportResult = withContext(Dispatchers.IO) {
        // 设置回滚用：缓存旧值（完整快照，确保所有字段可回滚）
        var oldSettingsSnapshot: ExportAppSettings? = null
        try {
            Log.d(TAG, "开始导入数据 (策略: $strategy)...")

            val jsonString = if (BackupCompression.isCompressed(dataString)) {
                BackupCompression.decompress(dataString)
            } else {
                dataString
            }

            val exportData = json.decodeFromString<NemoExportData>(jsonString)

            // 第一道防线：版本校验 + 数据清洗
            val validationResult = BackupValidator.validate(exportData)
            val cleanedData = validationResult.data
            val userData = cleanedData.userData

            // 先执行数据库事务（不改动设置）
            val stats = when (strategy) {
                ImportStrategy.MERGE -> importMerge(userData)
                ImportStrategy.REPLACE -> importReplace(userData)
            }

            // 导入成功后执行数据去重修复
            repairDataDuplicates()

            // 数据库事务成功后才应用设置，避免事务失败导致设置不一致
            userData.settings?.let { settings ->
                oldSettingsSnapshot = captureSettingsSnapshot()
                applySettings(settings)
            }

            val validationSummary = validationResult.toSummary()
            val suffix = if (validationSummary.isNotEmpty()) "\n校验提示: $validationSummary" else ""
            Log.d(TAG, "导入完成: 单词更新${stats.wordUpdateCount}/新增${stats.wordInsertCount}/跳过${stats.wordSkipCount}, 语法更新${stats.grammarUpdateCount}/新增${stats.grammarInsertCount}/跳过${stats.grammarSkipCount}")
            ImportResult(true, "导入成功！\n单词: 更新${stats.wordUpdateCount} / 新增${stats.wordInsertCount} / 跳过${stats.wordSkipCount}\n语法: 更新${stats.grammarUpdateCount} / 新增${stats.grammarInsertCount} / 跳过${stats.grammarSkipCount}$suffix")

        } catch (e: Exception) {
            Log.e(TAG, "导入失败", e)
            // 回滚设置（仅在设置已被修改的情况下）
            oldSettingsSnapshot?.let {
                try {
                    restoreSettingsSnapshot(it)
                    Log.d(TAG, "设置已回滚")
                } catch (rollbackEx: Exception) {
                    Log.e(TAG, "设置回滚失败", rollbackEx)
                }
            }
            val errorMsg = when (e) {
                is kotlinx.serialization.SerializationException -> "文件格式异常，无法解析"
                is IllegalArgumentException -> e.message ?: "数据校验失败"
                else -> "导入失败: ${e.message}"
            }
            ImportResult(false, errorMsg)
        }
    }

    /**
     * 将设置应用到 SettingsRepository
     */
    private suspend fun applySettings(settings: ExportAppSettings) {
        settings.dailyGoal.let { settingsRepository.setDailyGoal(it) }
        settings.grammarDailyGoal.let { settingsRepository.setGrammarDailyGoal(it) }
        settings.learningDayResetHour.let { settingsRepository.setLearningDayResetHour(it) }
        settings.testQuestionCount.let { settingsRepository.setTestQuestionCount(it) }
        settings.testTimeLimitMinutes.let { settingsRepository.setTestTimeLimitMinutes(it) }
        settings.testShuffleQuestions.let { settingsRepository.setTestShuffleQuestions(it) }
        settings.testShuffleOptions.let { settingsRepository.setTestShuffleOptions(it) }
        settings.testAutoAdvance.let { settingsRepository.setTestAutoAdvance(it) }
        settings.testPrioritizeWrong.let { settingsRepository.setTestPrioritizeWrong(it) }
        settings.testPrioritizeNew.let { settingsRepository.setTestPrioritizeNew(it) }
        settings.testQuestionSource.let { settingsRepository.setTestQuestionSource(it) }
        settings.testWrongAnswerRemovalThreshold.let { settingsRepository.setTestWrongAnswerRemovalThreshold(it) }
        settings.testContentType.let { settingsRepository.setTestContentType(it) }
        settings.testSelectedWordLevels.let { settingsRepository.setTestSelectedWordLevels(it) }
        settings.testSelectedGrammarLevels.let { settingsRepository.setTestSelectedGrammarLevels(it) }
        settings.aiWorkshopDifficulty.let { settingsRepository.setAiWorkshopDifficulty(it) }
        settings.aiReadingTheme.let { settingsRepository.setAiReadingTheme(it) }
        settings.aiReadingDifficulty.let { settingsRepository.setAiReadingDifficulty(it) }
        settings.learningSteps.let { settingsRepository.setLearningSteps(it) }
        settings.learnAheadLimit.let { settingsRepository.setLearnAheadLimit(it) }
        settings.relearningSteps.let { settingsRepository.setRelearningSteps(it) }
        settings.isRandomNewContentEnabled.let { settingsRepository.setRandomNewContentEnabled(it) }
        settings.targetRetention.let { settingsRepository.setTargetRetention(it) }
    }

    /**
     * 捕获当前设置快照（用于回滚）
     * 使用 ExportAppSettings 覆盖全部可导入字段，确保导入失败时完整回滚
     */
    private suspend fun captureSettingsSnapshot(): ExportAppSettings {
        val snapshot = settingsRepository.getAppSettingsSnapshot()
        return ExportAppSettings(
            dailyGoal = snapshot.dailyGoal,
            grammarDailyGoal = snapshot.grammarDailyGoal,
            learningDayResetHour = snapshot.learningDayResetHour,
            testQuestionCount = snapshot.testQuestionCount,
            testTimeLimitMinutes = snapshot.testTimeLimitMinutes,
            testShuffleQuestions = snapshot.testShuffleQuestions,
            testShuffleOptions = snapshot.testShuffleOptions,
            testAutoAdvance = snapshot.testAutoAdvance,
            testPrioritizeWrong = snapshot.testPrioritizeWrong,
            testPrioritizeNew = snapshot.testPrioritizeNew,
            testQuestionSource = snapshot.testQuestionSource,
            testWrongAnswerRemovalThreshold = snapshot.testWrongAnswerRemovalThreshold,
            testContentType = snapshot.testContentType,
            testSelectedWordLevels = snapshot.testSelectedWordLevels,
            testSelectedGrammarLevels = snapshot.testSelectedGrammarLevels,
            learningSteps = snapshot.learningSteps,
            learnAheadLimit = snapshot.learnAheadLimit,
            relearningSteps = snapshot.relearningSteps,
            isRandomNewContentEnabled = snapshot.isRandomNewContentEnabled,
            targetRetention = snapshot.targetRetention,
            aiWorkshopDifficulty = snapshot.aiWorkshopDifficulty,
            aiReadingTheme = snapshot.aiReadingTheme,
            aiReadingDifficulty = snapshot.aiReadingDifficulty
        )
    }

    /**
     * 恢复设置快照
     * 复用 applySettings 确保回滚路径与写入路径一致
     */
    private suspend fun restoreSettingsSnapshot(snapshot: ExportAppSettings) {
        applySettings(snapshot)
    }

    /**
     * 预览导入（dry-run 模式）
     * 不写入数据库，只做解析、校验和统计对比
     */
    suspend fun previewImport(dataString: String, strategy: ImportStrategy): ImportPreview = withContext(Dispatchers.IO) {
        val jsonString = if (BackupCompression.isCompressed(dataString)) {
            BackupCompression.decompress(dataString)
        } else {
            dataString
        }

        val exportData = json.decodeFromString<NemoExportData>(jsonString)
        val validationResult = BackupValidator.validate(exportData)
        val userData = validationResult.data.userData
        val validationSummary = validationResult.toSummary()

        when (strategy) {
            ImportStrategy.MERGE -> previewMerge(userData, validationSummary)
            ImportStrategy.REPLACE -> previewReplace(userData, validationSummary)
        }
    }

    /**
     * MERGE 模式预览：与本地数据对比统计
     */
    private suspend fun previewMerge(userData: UserData, validationSummary: String): ImportPreview {
        val wordDao = database.wordDao()
        val wordStudyStateDao = database.wordStudyStateDao()
        val localWordStates = wordStudyStateDao.getAllSync().associateBy { it.wordId }
        val allLocalWords = wordDao.getAllWordsSync()
        val localWordRawIdMap = allLocalWords.associateBy { it.rawId }

        var wordUpdate = 0; var wordInsert = 0; var wordSkip = 0
        userData.wordProgress.forEach { remote ->
            val targetLocalId = localWordRawIdMap[remote.rawId]?.id
            if (targetLocalId == null) { wordSkip++; return@forEach }
            val local = localWordStates[targetLocalId]
            if (local == null) { wordInsert++ }
            else if (remote.lastModifiedTime > local.lastModifiedTime) { wordUpdate++ }
            else { wordSkip++ }
        }

        val grammarDao = database.grammarDao()
        val grammarStudyStateDao = database.grammarStudyStateDao()
        val localGrammarStates = grammarStudyStateDao.getAllSync().associateBy { it.grammarId }
        val allLocalGrammars = grammarDao.getAllGrammarsSync()
        val localGrammarRawIdMap = allLocalGrammars.associateBy { it.rawId }

        var grammarUpdate = 0; var grammarInsert = 0; var grammarSkip = 0
        userData.grammarProgress.forEach { remote ->
            val targetLocalId = localGrammarRawIdMap[remote.rawId]?.id
            if (targetLocalId == null) { grammarSkip++; return@forEach }
            val local = localGrammarStates[targetLocalId]
            if (local == null) { grammarInsert++ }
            else if (remote.lastModifiedTime > local.lastModifiedTime) { grammarUpdate++ }
            else { grammarSkip++ }
        }

        val wrongAnswerDao = database.wrongAnswerDao()
        val localWrongAnswers = wrongAnswerDao.getAllWrongAnswersSync().associateBy { it.uuid }
        var wrongAnswerNew = 0
        userData.wrongAnswers.words.forEach { remote ->
            if (localWordRawIdMap[remote.rawId]?.id != null && localWrongAnswers[remote.uuid] == null) wrongAnswerNew++
        }
        val grammarWrongAnswerDao = database.grammarWrongAnswerDao()
        val localGrammarWrongAnswers = grammarWrongAnswerDao.getAllWrongAnswersSync().associateBy { it.uuid }
        userData.wrongAnswers.grammars.forEach { remote ->
            if (localGrammarRawIdMap[remote.rawId]?.id != null && localGrammarWrongAnswers[remote.uuid] == null) wrongAnswerNew++
        }

        val testRecordDao = database.testRecordDao()
        val localTestRecords = testRecordDao.getAllTestRecordsSync().associateBy { it.uuid }
        val testRecordNew = userData.testRecords.count { localTestRecords[it.uuid] == null }

        val studyRecordDao = database.studyRecordDao()
        val localStudyRecords = studyRecordDao.getAllStudyRecordsSync().associateBy { it.date }
        val studyRecordNew = userData.studyRecords.count { localStudyRecords[it.date] == null }

        val favoriteQuestionDao = database.favoriteQuestionDao()
        val localFavorites = favoriteQuestionDao.getAllFavoriteQuestionsSync().associateBy { it.jsonId ?: it.id.toString() }
        val favoriteNew = userData.favoriteQuestions.count { localFavorites[it.jsonId ?: it.id.toString()] == null }

        return ImportPreview(
            strategy = ImportStrategy.MERGE,
            wordUpdateCount = wordUpdate,
            wordInsertCount = wordInsert,
            wordSkipCount = wordSkip,
            grammarUpdateCount = grammarUpdate,
            grammarInsertCount = grammarInsert,
            grammarSkipCount = grammarSkip,
            wrongAnswerNewCount = wrongAnswerNew,
            testRecordNewCount = testRecordNew,
            studyRecordNewCount = studyRecordNew,
            favoriteNewCount = favoriteNew,
            settingsWillChange = userData.settings != null,
            validationSummary = validationSummary
        )
    }

    /**
     * REPLACE 模式预览：统计本地将被清空的数据量和远端将写入的数据量
     */
    private suspend fun previewReplace(userData: UserData, validationSummary: String): ImportPreview {
        val localWordStateCount = database.wordStudyStateDao().getAllSync().size
        val localGrammarStateCount = database.grammarStudyStateDao().getAllSync().size

        return ImportPreview(
            strategy = ImportStrategy.REPLACE,
            wordInsertCount = userData.wordProgress.size,
            grammarInsertCount = userData.grammarProgress.size,
            wrongAnswerNewCount = userData.wrongAnswers.words.size + userData.wrongAnswers.grammars.size,
            testRecordNewCount = userData.testRecords.size,
            studyRecordNewCount = userData.studyRecords.size,
            favoriteNewCount = userData.favoriteQuestions.size,
            localWordStateCount = localWordStateCount,
            localGrammarStateCount = localGrammarStateCount,
            settingsWillChange = userData.settings != null,
            validationSummary = validationSummary
        )
    }

    /**
     * MERGE 模式：智能合并
     */
    private suspend fun importMerge(userData: UserData): ImportStats {
        var wordUpdateCount = 0
        var wordInsertCount = 0
        var wordSkipCount = 0
        var grammarUpdateCount = 0
        var grammarInsertCount = 0
        var grammarSkipCount = 0

        database.withTransaction {
                val wordDao = database.wordDao()
                val wordStudyStateDao = database.wordStudyStateDao()
                
                val localWordStates = wordStudyStateDao.getAllSync().associateBy { it.wordId }
                val allLocalWords = wordDao.getAllWordsSync()
                val localWordRawIdMap = allLocalWords.associateBy { it.rawId }

                val wordIdRedirectMap = mutableMapOf<Int, Int>()
                userData.wordProgress.forEach { remoteWord ->
                    val targetLocalId = localWordRawIdMap[remoteWord.rawId]?.id
                    if (targetLocalId == null) {
                        wordSkipCount++
                        Log.w(TAG, "跳过未找到对应 rawId 的词条: ${remoteWord.rawId}")
                        return@forEach
                    }
                    wordIdRedirectMap[remoteWord.wordId] = targetLocalId

                    val localState = localWordStates[targetLocalId]
                    if (localState != null) {
                        if (remoteWord.lastModifiedTime > localState.lastModifiedTime) {
                            wordUpdateCount++
                            wordStudyStateDao.insert(WordStudyStateEntity(
                                wordId = targetLocalId,
                                repetitionCount = remoteWord.srsLevel,
                                stability = remoteWord.stability,
                                difficulty = remoteWord.difficulty,
                                interval = remoteWord.interval,
                                nextReviewDate = remoteWord.nextReviewDate,
                                isFavorite = remoteWord.isFavorite,
                                isSkipped = remoteWord.isSkipped,
                                lastModifiedTime = remoteWord.lastModifiedTime,
                                lastReviewedDate = remoteWord.lastReviewedDate,
                                firstLearnedDate = remoteWord.firstLearnedDate,
                                type = remoteWord.type,
                                isDeleted = remoteWord.isDeleted,
                                deletedTime = remoteWord.deletedTime
                            ))
                        } else {
                            wordSkipCount++
                        }
                    } else {
                        wordInsertCount++
                        wordStudyStateDao.insert(WordStudyStateEntity(
                            wordId = targetLocalId,
                            repetitionCount = remoteWord.srsLevel,
                            stability = remoteWord.stability,
                            difficulty = remoteWord.difficulty,
                            interval = remoteWord.interval,
                            nextReviewDate = remoteWord.nextReviewDate,
                            isFavorite = remoteWord.isFavorite,
                            isSkipped = remoteWord.isSkipped,
                            lastModifiedTime = remoteWord.lastModifiedTime,
                            lastReviewedDate = remoteWord.lastReviewedDate,
                            firstLearnedDate = remoteWord.firstLearnedDate,
                            type = remoteWord.type,
                            isDeleted = remoteWord.isDeleted,
                            deletedTime = remoteWord.deletedTime
                        ))
                    }
                }

                val grammarDao = database.grammarDao()
                val grammarStudyStateDao = database.grammarStudyStateDao()
                
                val localGrammarStates = grammarStudyStateDao.getAllSync().associateBy { it.grammarId }
                val allLocalGrammars = grammarDao.getAllGrammarsSync()
                val localGrammarRawIdMap = allLocalGrammars.associateBy { it.rawId }

                val grammarIdRedirectMap = mutableMapOf<Int, Int>()
                userData.grammarProgress.forEach { remoteGrammar ->
                    val targetLocalId = localGrammarRawIdMap[remoteGrammar.rawId]?.id
                    if (targetLocalId == null) {
                        grammarSkipCount++
                        Log.w(TAG, "跳过未找到对应 rawId 的语法: ${remoteGrammar.rawId}")
                        return@forEach
                    }
                    grammarIdRedirectMap[remoteGrammar.grammarId] = targetLocalId

                    val localState = localGrammarStates[targetLocalId]
                    if (localState != null) {
                         if (remoteGrammar.lastModifiedTime > localState.lastModifiedTime) {
                            grammarUpdateCount++
                            grammarStudyStateDao.insert(GrammarStudyStateEntity(
                                grammarId = targetLocalId,
                                repetitionCount = remoteGrammar.srsLevel,
                                stability = remoteGrammar.stability,
                                difficulty = remoteGrammar.difficulty,
                                interval = remoteGrammar.interval,
                                nextReviewDate = remoteGrammar.nextReviewDate,
                                isFavorite = remoteGrammar.isFavorite,
                                isSkipped = remoteGrammar.isSkipped,
                                lastModifiedTime = remoteGrammar.lastModifiedTime,
                                lastReviewedDate = remoteGrammar.lastReviewedDate,
                                firstLearnedDate = remoteGrammar.firstLearnedDate,
                                type = remoteGrammar.type,
                                isDeleted = remoteGrammar.isDeleted,
                                deletedTime = remoteGrammar.deletedTime
                            ))
                         } else {
                            grammarSkipCount++
                         }
                    } else {
                         grammarInsertCount++
                         grammarStudyStateDao.insert(GrammarStudyStateEntity(
                             grammarId = targetLocalId,
                             repetitionCount = remoteGrammar.srsLevel,
                             stability = remoteGrammar.stability,
                             difficulty = remoteGrammar.difficulty,
                             interval = remoteGrammar.interval,
                             nextReviewDate = remoteGrammar.nextReviewDate,
                             isFavorite = remoteGrammar.isFavorite,
                             lastModifiedTime = remoteGrammar.lastModifiedTime,
                             lastReviewedDate = remoteGrammar.lastReviewedDate,
                             firstLearnedDate = remoteGrammar.firstLearnedDate,
                             type = remoteGrammar.type,
                             isSkipped = remoteGrammar.isSkipped,
                             isDeleted = remoteGrammar.isDeleted,
                             deletedTime = remoteGrammar.deletedTime
                         ))
                    }
                }

                val wrongAnswerDao = database.wrongAnswerDao()
                val localWrongAnswers = wrongAnswerDao.getAllWrongAnswersSync().associateBy { it.uuid }
                userData.wrongAnswers.words.forEach { remote ->
                    val targetLocalId = localWordRawIdMap[remote.rawId]?.id
                    if (targetLocalId == null) {
                        Log.w(TAG, "跳过未找到对应 rawId 的单词错题: ${remote.rawId}")
                        return@forEach
                    }

                    val local = localWrongAnswers[remote.uuid]
                    if (local == null) {
                        wrongAnswerDao.insert(WrongAnswerEntity(
                            id = 0,
                            wordId = targetLocalId,
                            testMode = remote.testMode ?: "",
                            userAnswer = remote.userAnswer ?: "",
                            correctAnswer = remote.correctAnswer ?: "",
                            uuid = remote.uuid ?: java.util.UUID.randomUUID().toString(),
                            timestamp = remote.timestamp
                        ))
                    } else if (remote.timestamp > local.timestamp) {
                        wrongAnswerDao.insert(local.copy(
                            testMode = remote.testMode ?: local.testMode,
                            userAnswer = remote.userAnswer ?: local.userAnswer,
                            correctAnswer = remote.correctAnswer ?: local.correctAnswer,
                            timestamp = remote.timestamp
                        ))
                    }
                }

                val grammarWrongAnswerDao = database.grammarWrongAnswerDao()
                val localGrammarWrongAnswers = grammarWrongAnswerDao.getAllWrongAnswersSync().associateBy { it.uuid }
                userData.wrongAnswers.grammars.forEach { remote ->
                    val targetLocalId = localGrammarRawIdMap[remote.rawId]?.id
                    if (targetLocalId == null) {
                        Log.w(TAG, "跳过未找到对应 rawId 的语法错题: ${remote.rawId}")
                        return@forEach
                    }

                    val local = localGrammarWrongAnswers[remote.uuid]
                    if (local == null) {
                        grammarWrongAnswerDao.insert(GrammarWrongAnswerEntity(
                            id = 0,
                            grammarId = targetLocalId,
                            testMode = remote.testMode ?: "",
                            userAnswer = remote.userAnswer ?: "",
                            correctAnswer = remote.correctAnswer ?: "",
                            uuid = remote.uuid ?: java.util.UUID.randomUUID().toString(),
                            timestamp = remote.timestamp
                        ))
                    } else if (remote.timestamp > local.timestamp) {
                        grammarWrongAnswerDao.insert(local.copy(
                            testMode = remote.testMode ?: local.testMode,
                            userAnswer = remote.userAnswer ?: local.userAnswer,
                            correctAnswer = remote.correctAnswer ?: local.correctAnswer,
                            timestamp = remote.timestamp
                        ))
                    }
                }

                val favoriteQuestionDao = database.favoriteQuestionDao()
                val localFavorites = favoriteQuestionDao.getAllFavoriteQuestionsSync().associateBy { it.jsonId ?: it.id.toString() }
                userData.favoriteQuestions.forEach { remote ->
                    val local = localFavorites[remote.jsonId ?: remote.id.toString()]
                    if (local == null) {
                        favoriteQuestionDao.insert(FavoriteQuestionEntity(
                            id = 0,
                            grammarId = remote.grammarId?.let { grammarIdRedirectMap[it] ?: it },
                            jsonId = remote.jsonId,
                            questionType = remote.questionType,
                            questionText = remote.questionText,
                            optionsJson = remote.optionsJson,
                            correctAnswer = remote.correctAnswer,
                            explanation = remote.explanation,
                            timestamp = remote.timestamp
                        ))
                    }
                }

                val testRecordDao = database.testRecordDao()
                val localTestRecords = testRecordDao.getAllTestRecordsSync().associateBy { it.uuid }
                userData.testRecords.forEach { remote ->
                    val local = localTestRecords[remote.uuid]
                    if (local == null) {
                        testRecordDao.insert(TestRecordEntity(
                            id = 0,
                            date = remote.date,
                            totalQuestions = remote.totalQuestions,
                            correctAnswers = remote.correctAnswers,
                            testMode = remote.testMode,
                            uuid = remote.uuid ?: java.util.UUID.randomUUID().toString(),
                            timestamp = remote.timestamp
                        ))
                    } else if (remote.timestamp > local.timestamp) {
                        testRecordDao.insert(local.copy(
                            date = remote.date,
                            totalQuestions = remote.totalQuestions,
                            correctAnswers = remote.correctAnswers,
                            testMode = remote.testMode,
                            timestamp = remote.timestamp
                        ))
                    }
                }

                val studyRecordDao = database.studyRecordDao()
                val localStudyRecords = studyRecordDao.getAllStudyRecordsSync().associateBy { it.date }
                userData.studyRecords.forEach { remote ->
                    val local = localStudyRecords[remote.date]
                    if (local == null) {
                        studyRecordDao.insert(StudyRecordEntity(
                            date = remote.date,
                            learnedWords = remote.learnedWords,
                            learnedGrammars = remote.learnedGrammars,
                            reviewedWords = remote.reviewedWords,
                            reviewedGrammars = remote.reviewedGrammars,
                            skippedWords = remote.skippedWords,
                            skippedGrammars = remote.skippedGrammars,
                            testCount = remote.testCount,
                            timestamp = remote.timestamp
                        ))
                    } else if (remote.timestamp > local.timestamp) {
                        studyRecordDao.insert(local.copy(
                            learnedWords = maxOf(local.learnedWords, remote.learnedWords), // 倾向于保留更大数据或 LWW
                            learnedGrammars = maxOf(local.learnedGrammars, remote.learnedGrammars),
                            reviewedWords = maxOf(local.reviewedWords, remote.reviewedWords),
                            reviewedGrammars = maxOf(local.reviewedGrammars, remote.reviewedGrammars),
                            skippedWords = maxOf(local.skippedWords, remote.skippedWords),
                            skippedGrammars = maxOf(local.skippedGrammars, remote.skippedGrammars),
                            testCount = maxOf(local.testCount, remote.testCount),
                            timestamp = remote.timestamp
                        ))
                    }
                }

                userData.totalStudyDays?.let { totalStudyDays ->
                    settingsRepository.restoreStudyStats(
                        totalStudyDays = totalStudyDays,
                        dailyStreak = userData.studyStreak ?: 0,
                        lastStudyDate = userData.studyRecords.maxOfOrNull { it.date } ?: 0L,
                        maxTestStreak = userData.maxTestStreak ?: 0,
                        testStreak = userData.testStreak ?: 0
                    )
            }
        }

        return ImportStats(wordUpdateCount, wordInsertCount, wordSkipCount, grammarUpdateCount, grammarInsertCount, grammarSkipCount)
    }

    /**
     * REPLACE 模式：清空进度表后全量写入
     * 注意：只清空进度相关表，不清空词库基础表（words/grammars）
     */
    private suspend fun importReplace(userData: UserData): ImportStats {
        var wordInsertCount = 0
        var wordSkipCount = 0
        var grammarInsertCount = 0
        var grammarSkipCount = 0

        database.withTransaction {
            // 清空进度相关表
            database.wordStudyStateDao().deleteAll()
            database.grammarStudyStateDao().deleteAll()
            database.wrongAnswerDao().deleteAll()
            database.grammarWrongAnswerDao().deleteAll()
            database.testRecordDao().deleteAll()
            database.studyRecordDao().deleteAll()
            database.favoriteQuestionDao().deleteAll()

            // 重建 rawId 映射
            val wordDao = database.wordDao()
            val wordStudyStateDao = database.wordStudyStateDao()
            val allLocalWords = wordDao.getAllWordsSync()
            val localWordRawIdMap = allLocalWords.associateBy { it.rawId }

            // 全量写入单词进度（不做时间戳对比）
            userData.wordProgress.forEach { remoteWord ->
                val targetLocalId = localWordRawIdMap[remoteWord.rawId]?.id
                if (targetLocalId == null) {
                    wordSkipCount++
                    Log.w(TAG, "[REPLACE] 跳过未找到对应 rawId 的词条: ${remoteWord.rawId}")
                    return@forEach
                }
                wordInsertCount++
                wordStudyStateDao.insert(WordStudyStateEntity(
                    wordId = targetLocalId,
                    repetitionCount = remoteWord.srsLevel,
                    stability = remoteWord.stability,
                    difficulty = remoteWord.difficulty,
                    interval = remoteWord.interval,
                    nextReviewDate = remoteWord.nextReviewDate,
                    isFavorite = remoteWord.isFavorite,
                    isSkipped = remoteWord.isSkipped,
                    lastModifiedTime = remoteWord.lastModifiedTime,
                    lastReviewedDate = remoteWord.lastReviewedDate,
                    firstLearnedDate = remoteWord.firstLearnedDate,
                    type = remoteWord.type,
                    isDeleted = remoteWord.isDeleted,
                    deletedTime = remoteWord.deletedTime
                ))
            }

            // 全量写入语法进度
            val grammarDao = database.grammarDao()
            val grammarStudyStateDao = database.grammarStudyStateDao()
            val allLocalGrammars = grammarDao.getAllGrammarsSync()
            val localGrammarRawIdMap = allLocalGrammars.associateBy { it.rawId }

            userData.grammarProgress.forEach { remoteGrammar ->
                val targetLocalId = localGrammarRawIdMap[remoteGrammar.rawId]?.id
                if (targetLocalId == null) {
                    grammarSkipCount++
                    Log.w(TAG, "[REPLACE] 跳过未找到对应 rawId 的语法: ${remoteGrammar.rawId}")
                    return@forEach
                }
                grammarInsertCount++
                grammarStudyStateDao.insert(GrammarStudyStateEntity(
                    grammarId = targetLocalId,
                    repetitionCount = remoteGrammar.srsLevel,
                    stability = remoteGrammar.stability,
                    difficulty = remoteGrammar.difficulty,
                    interval = remoteGrammar.interval,
                    nextReviewDate = remoteGrammar.nextReviewDate,
                    isFavorite = remoteGrammar.isFavorite,
                    isSkipped = remoteGrammar.isSkipped,
                    lastModifiedTime = remoteGrammar.lastModifiedTime,
                    lastReviewedDate = remoteGrammar.lastReviewedDate,
                    firstLearnedDate = remoteGrammar.firstLearnedDate,
                    type = remoteGrammar.type,
                    isDeleted = remoteGrammar.isDeleted,
                    deletedTime = remoteGrammar.deletedTime
                ))
            }

            // 全量写入错题、测试记录、学习记录等
            val wrongAnswerDao = database.wrongAnswerDao()
            val wordIdRedirectMap = mutableMapOf<Int, Int>()
            userData.wordProgress.forEach { w -> localWordRawIdMap[w.rawId]?.id?.let { wordIdRedirectMap[w.wordId] = it } }

            userData.wrongAnswers.words.forEach { remote ->
                val targetLocalId = localWordRawIdMap[remote.rawId]?.id ?: return@forEach
                wrongAnswerDao.insert(WrongAnswerEntity(
                    id = 0,
                    wordId = targetLocalId,
                    testMode = remote.testMode ?: "",
                    userAnswer = remote.userAnswer ?: "",
                    correctAnswer = remote.correctAnswer ?: "",
                    uuid = remote.uuid ?: java.util.UUID.randomUUID().toString(),
                    timestamp = remote.timestamp
                ))
            }

            val grammarWrongAnswerDao = database.grammarWrongAnswerDao()
            val grammarIdRedirectMap = mutableMapOf<Int, Int>()
            userData.grammarProgress.forEach { g -> localGrammarRawIdMap[g.rawId]?.id?.let { grammarIdRedirectMap[g.grammarId] = it } }

            userData.wrongAnswers.grammars.forEach { remote ->
                val targetLocalId = localGrammarRawIdMap[remote.rawId]?.id ?: return@forEach
                grammarWrongAnswerDao.insert(GrammarWrongAnswerEntity(
                    id = 0,
                    grammarId = targetLocalId,
                    testMode = remote.testMode ?: "",
                    userAnswer = remote.userAnswer ?: "",
                    correctAnswer = remote.correctAnswer ?: "",
                    uuid = remote.uuid ?: java.util.UUID.randomUUID().toString(),
                    timestamp = remote.timestamp
                ))
            }

            val testRecordDao = database.testRecordDao()
            userData.testRecords.forEach { remote ->
                testRecordDao.insert(TestRecordEntity(
                    id = 0,
                    date = remote.date,
                    totalQuestions = remote.totalQuestions,
                    correctAnswers = remote.correctAnswers,
                    testMode = remote.testMode,
                    uuid = remote.uuid ?: java.util.UUID.randomUUID().toString(),
                    timestamp = remote.timestamp
                ))
            }

            val studyRecordDao = database.studyRecordDao()
            userData.studyRecords.forEach { remote ->
                studyRecordDao.insert(StudyRecordEntity(
                    date = remote.date,
                    learnedWords = remote.learnedWords,
                    learnedGrammars = remote.learnedGrammars,
                    reviewedWords = remote.reviewedWords,
                    reviewedGrammars = remote.reviewedGrammars,
                    skippedWords = remote.skippedWords,
                    skippedGrammars = remote.skippedGrammars,
                    testCount = remote.testCount,
                    timestamp = remote.timestamp
                ))
            }

            val favoriteQuestionDao = database.favoriteQuestionDao()
            userData.favoriteQuestions.forEach { remote ->
                favoriteQuestionDao.insert(FavoriteQuestionEntity(
                    id = 0,
                    grammarId = remote.grammarId?.let { grammarIdRedirectMap[it] ?: it },
                    jsonId = remote.jsonId,
                    questionType = remote.questionType,
                    questionText = remote.questionText,
                    optionsJson = remote.optionsJson,
                    correctAnswer = remote.correctAnswer,
                    explanation = remote.explanation,
                    timestamp = remote.timestamp
                ))
            }

            userData.totalStudyDays?.let { totalStudyDays ->
                settingsRepository.restoreStudyStats(
                    totalStudyDays = totalStudyDays,
                    dailyStreak = userData.studyStreak ?: 0,
                    lastStudyDate = userData.studyRecords.maxOfOrNull { it.date } ?: 0L,
                    maxTestStreak = userData.maxTestStreak ?: 0,
                    testStreak = userData.testStreak ?: 0
                )
            }
        }

        return ImportStats(0, wordInsertCount, wordSkipCount, 0, grammarInsertCount, grammarSkipCount)
    }

    override suspend fun exportDataToUri(uriString: String, isCompressed: Boolean): Boolean = withContext(Dispatchers.IO) {
        val tempFile = java.io.File(context.cacheDir, "temp_export_uri_${System.currentTimeMillis()}.json.gz")
        try {
            val uri = Uri.parse(uriString)
            exportDataToFile("default_user", tempFile, isCompressed)

            val outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                Log.e(TAG, "导出失败: 无法打开输出流 uri=$uri")
                return@withContext false
            }

            outputStream.use { os ->
                java.io.FileInputStream(tempFile).use { inputStream ->
                    inputStream.copyTo(os)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "导出到文件失败", e)
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    override suspend fun importDataFromUri(uriString: String): String = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        content.append(line)
                        line = reader.readLine()
                    }
                }
            }
            importData(content.toString()).message
        } catch (e: Exception) {
            Log.e(TAG, "从文件读取失败", e)
            "读取文件失败: ${e.message}"
        }
    }

    /**
     * 数据自动去重修复工具
     * 解决语义重复但 ID 不一致的问题
     */
    suspend fun repairDataDuplicates() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始执行数据去重修复...")
            database.withTransaction {
                val grammarDao = database.grammarDao()
                val grammarStudyStateDao = database.grammarStudyStateDao()
                
                // 1. 处理语法重复
                val allGrammars = grammarDao.getAllGrammarsSync()
                val groups = allGrammars.groupBy { "${it.grammarLevel.uppercase()}_${it.grammar}" }
                
                groups.forEach { (key, entities) ->
                    if (entities.size > 1) {
                        Log.d(TAG, "发现重复语法标题: $key, 数量: ${entities.size}")
                        
                        // 1. 将实体分为“标准类”（种子生成的 ID >= 10000）和“非标类”
                        val standardEntities = entities.filter { it.id >= 10000 }.sortedBy { it.id }
                        val redundantEntities = entities.filter { it.id < 10000 }.sortedBy { it.id }
                        
                        // 2. 如果标准实体有多个，它们可能是合法的同名不同义项（如 N5 的两个“が”），必须全部保留
                        // 我们只处理真正的冗余项（非标 ID）
                        
                        if (redundantEntities.isNotEmpty()) {
                            // 确定一个主目标：如果有标准实体，选第一个标准实体；否则选非标实体中 ID 最小的
                            val standardTarget = standardEntities.firstOrNull() ?: redundantEntities.first()
                            val duplicateIds = redundantEntities.map { it.id }.filter { it != standardTarget.id }
                            
                            if (duplicateIds.isNotEmpty()) {
                                // 迁移关联数据 (错题记录、收藏题目)
                                val grammarWrongAnswerDao = database.grammarWrongAnswerDao()
                                val favoriteQuestionDao = database.favoriteQuestionDao()
                                
                                // 初始化合并后的状态
                                val standardState = grammarStudyStateDao.getByIdSync(standardTarget.id)
                                var mergedState = standardState
                                
                                duplicateIds.forEach { dupId ->
                                    // 迁移错题
                                    grammarWrongAnswerDao.migrateGrammarId(dupId, standardTarget.id)
                                    // 迁移收藏
                                    favoriteQuestionDao.migrateGrammarId(dupId, standardTarget.id)
                                    
                                    // 合并进度
                                    val dupState = grammarStudyStateDao.getByIdSync(dupId)
                                    if (dupState != null) {
                                        mergedState = if (mergedState == null) {
                                            dupState.copy(grammarId = standardTarget.id)
                                        } else {
                                            val localTime = mergedState!!.lastModifiedTime
                                            val remoteTime = dupState.lastModifiedTime
                                            if (remoteTime > localTime) {
                                                dupState.copy(grammarId = standardTarget.id, isFavorite = mergedState!!.isFavorite || dupState.isFavorite)
                                            } else {
                                                mergedState!!.copy(isFavorite = mergedState!!.isFavorite || dupState.isFavorite)
                                            }
                                        }
                                    }
                                }
                                
                                // 更新标准状态
                                mergedState?.let { grammarStudyStateDao.insert(it) }
                                
                                // 删除冗余
                                grammarDao.deleteByIds(duplicateIds)
                                grammarStudyStateDao.deleteByIds(duplicateIds)
                                Log.d(TAG, "已清理语法冗余项: $key, 保留标准 ID: ${standardTarget.id}, 删除重复 ID: $duplicateIds")
                            }
                        } else if (standardEntities.size > 1) {
                            Log.d(TAG, "检测到多个同名标准项 ($key)，已确认为合法项目，不做去重处理。")
                        }
                    }
                }

                // 2. 对单词执行相同逻辑 (略，如果需要可以增加)
            }
            Log.d(TAG, "数据去重修复完成")
        } catch (e: Exception) {
            Log.e(TAG, "数据修复失败", e)
        }
    }

    companion object {
        private const val TAG = "DataExportManager"
    }
}
