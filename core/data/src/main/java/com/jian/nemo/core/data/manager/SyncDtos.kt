package com.jian.nemo.core.data.manager

import com.jian.nemo.core.data.local.entity.*
import com.jian.nemo.core.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 对应 user_word_states 表的 DTO
 */
@Serializable
data class SyncWordStateDto(
    @SerialName("user_id") val userId: String,
    @SerialName("word_id") val wordId: Int,
    @SerialName("repetition_count") val repetitionCount: Int,
    @SerialName("stability") val stability: Float = 0f,
    @SerialName("difficulty") val difficulty: Float = 0f,
    @SerialName("interval") val interval: Int,
    @SerialName("next_review_date") val nextReviewDate: Long,
    @SerialName("is_favorite") val isFavorite: Boolean,
    @SerialName("is_skipped") val isSkipped: Boolean,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_time") val deletedTime: Long,
    @SerialName("last_modified_time") val lastModifiedTime: Long,
    @SerialName("first_learned_date") val firstLearnedDate: Long? = null,
    @SerialName("last_reviewed_date") val lastReviewedDate: Long? = null,
    @SerialName("buried_until_day") val buriedUntilDay: Long = 0,
    @SerialName("type") val type: Int = 0
)

/**
 * 对应 user_grammar_states 表的 DTO
 */
@Serializable
data class SyncGrammarStateDto(
    @SerialName("user_id") val userId: String,
    @SerialName("grammar_id") val grammarId: Int,
    @SerialName("repetition_count") val repetitionCount: Int,
    @SerialName("stability") val stability: Float = 0f,
    @SerialName("difficulty") val difficulty: Float = 0f,
    @SerialName("interval") val interval: Int,
    @SerialName("next_review_date") val nextReviewDate: Long,
    @SerialName("is_favorite") val isFavorite: Boolean,
    @SerialName("is_skipped") val isSkipped: Boolean,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_time") val deletedTime: Long,
    @SerialName("last_modified_time") val lastModifiedTime: Long,
    @SerialName("first_learned_date") val firstLearnedDate: Long? = null,
    @SerialName("last_reviewed_date") val lastReviewedDate: Long? = null,
    @SerialName("buried_until_day") val buriedUntilDay: Long = 0,
    @SerialName("type") val type: Int = 0
)

@Serializable
data class SyncStudyRecordDto(
    @SerialName("user_id") val userId: String,
    @SerialName("date") val date: Long,
    @SerialName("learned_words") val learnedWords: Int,
    @SerialName("learned_grammars") val learnedGrammars: Int,
    @SerialName("reviewed_words") val reviewedWords: Int,
    @SerialName("reviewed_grammars") val reviewedGrammars: Int,
    @SerialName("skipped_words") val skippedWords: Int,
    @SerialName("skipped_grammars") val skippedGrammars: Int,
    @SerialName("test_count") val testCount: Int,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_time") val deletedTime: Long,
    @SerialName("timestamp") val timestamp: Long
)

@Serializable
data class SyncTestRecordDto(
    @SerialName("user_id") val userId: String,
    @SerialName("uuid") val uuid: String,
    @SerialName("date") val date: Long,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("correct_answers") val correctAnswers: Int,
    @SerialName("test_mode") val testMode: String,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_time") val deletedTime: Long,
    @SerialName("timestamp") val timestamp: Long
)

@Serializable
data class SyncWrongAnswerDto(
    @SerialName("user_id") val userId: String,
    @SerialName("uuid") val uuid: String,
    @SerialName("word_id") val wordId: Int,
    @SerialName("test_mode") val testMode: String,
    @SerialName("user_answer") val userAnswer: String,
    @SerialName("correct_answer") val correctAnswer: String,
    @SerialName("consecutive_correct_count") val consecutiveCorrectCount: Int,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_time") val deletedTime: Long,
    @SerialName("timestamp") val timestamp: Long
)

@Serializable
data class SyncGrammarWrongAnswerDto(
    @SerialName("user_id") val userId: String,
    @SerialName("uuid") val uuid: String,
    @SerialName("grammar_id") val grammarId: Int,
    @SerialName("test_mode") val testMode: String,
    @SerialName("user_answer") val userAnswer: String,
    @SerialName("correct_answer") val correctAnswer: String,
    @SerialName("consecutive_correct_count") val consecutiveCorrectCount: Int,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_time") val deletedTime: Long,
    @SerialName("timestamp") val timestamp: Long
)

@Serializable
data class SyncFavoriteQuestionDto(
    @SerialName("user_id") val userId: String,
    @SerialName("grammar_id") val grammarId: Int?,
    @SerialName("json_id") val jsonId: String?,
    @SerialName("question_type") val questionType: String,
    @SerialName("question_text") val questionText: String,
    @SerialName("options_json") val optionsJson: String,
    @SerialName("correct_answer") val correctAnswer: String,
    @SerialName("explanation") val explanation: String?,
    @SerialName("timestamp") val timestamp: Long
)

@Serializable
data class SyncAppSettingsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("settings") val settings: AppSettings,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// ===================================
// Mappers (Cloud DTO <-> Local Entity)
// ===================================

fun SyncWordStateDto.toEntity() = WordStudyStateEntity(
    wordId = wordId,
    repetitionCount = repetitionCount,
    stability = stability,
    difficulty = difficulty,
    interval = interval,
    nextReviewDate = nextReviewDate,
    isFavorite = isFavorite,
    isSkipped = isSkipped,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    lastModifiedTime = lastModifiedTime,
    lastReviewedDate = lastReviewedDate,
    firstLearnedDate = firstLearnedDate,
    buriedUntilDay = buriedUntilDay,
    type = type
)

fun WordStudyStateEntity.toSyncDto(userId: String) = SyncWordStateDto(
    userId = userId,
    wordId = wordId,
    repetitionCount = repetitionCount,
    stability = stability,
    difficulty = difficulty,
    interval = interval,
    nextReviewDate = nextReviewDate,
    isFavorite = isFavorite,
    isSkipped = isSkipped,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    lastModifiedTime = lastModifiedTime,
    lastReviewedDate = lastReviewedDate,
    firstLearnedDate = firstLearnedDate,
    buriedUntilDay = buriedUntilDay,
    type = type
)

fun SyncGrammarStateDto.toEntity() = GrammarStudyStateEntity(
    grammarId = grammarId,
    repetitionCount = repetitionCount,
    stability = stability,
    difficulty = difficulty,
    interval = interval,
    nextReviewDate = nextReviewDate,
    isFavorite = isFavorite,
    isSkipped = isSkipped,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    lastModifiedTime = lastModifiedTime,
    lastReviewedDate = lastReviewedDate,
    firstLearnedDate = firstLearnedDate,
    buriedUntilDay = buriedUntilDay,
    type = type
)

fun GrammarStudyStateEntity.toSyncDto(userId: String) = SyncGrammarStateDto(
    userId = userId,
    grammarId = grammarId,
    repetitionCount = repetitionCount,
    stability = stability,
    difficulty = difficulty,
    interval = interval,
    nextReviewDate = nextReviewDate,
    isFavorite = isFavorite,
    isSkipped = isSkipped,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    lastModifiedTime = lastModifiedTime,
    lastReviewedDate = lastReviewedDate,
    firstLearnedDate = firstLearnedDate,
    buriedUntilDay = buriedUntilDay,
    type = type
)

fun SyncStudyRecordDto.toEntity() = StudyRecordEntity(
    date = date,
    learnedWords = learnedWords,
    learnedGrammars = learnedGrammars,
    reviewedWords = reviewedWords,
    reviewedGrammars = reviewedGrammars,
    skippedWords = skippedWords,
    skippedGrammars = skippedGrammars,
    testCount = testCount,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun StudyRecordEntity.toSyncDto(userId: String) = SyncStudyRecordDto(
    userId = userId,
    date = date,
    learnedWords = learnedWords,
    learnedGrammars = learnedGrammars,
    reviewedWords = reviewedWords,
    reviewedGrammars = reviewedGrammars,
    skippedWords = skippedWords,
    skippedGrammars = skippedGrammars,
    testCount = testCount,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun SyncTestRecordDto.toEntity() = TestRecordEntity(
    uuid = uuid,
    date = date,
    totalQuestions = totalQuestions,
    correctAnswers = correctAnswers,
    testMode = testMode,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun TestRecordEntity.toSyncDto(userId: String) = SyncTestRecordDto(
    userId = userId,
    uuid = uuid,
    date = date,
    totalQuestions = totalQuestions,
    correctAnswers = correctAnswers,
    testMode = testMode,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun SyncWrongAnswerDto.toEntity() = WrongAnswerEntity(
    uuid = uuid,
    wordId = wordId,
    testMode = testMode,
    userAnswer = userAnswer,
    correctAnswer = correctAnswer,
    consecutiveCorrectCount = consecutiveCorrectCount,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun WrongAnswerEntity.toSyncDto(userId: String) = SyncWrongAnswerDto(
    userId = userId,
    uuid = uuid,
    wordId = wordId,
    testMode = testMode,
    userAnswer = userAnswer,
    correctAnswer = correctAnswer,
    consecutiveCorrectCount = consecutiveCorrectCount,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun SyncGrammarWrongAnswerDto.toEntity() = GrammarWrongAnswerEntity(
    uuid = uuid,
    grammarId = grammarId,
    testMode = testMode,
    userAnswer = userAnswer,
    correctAnswer = correctAnswer,
    consecutiveCorrectCount = consecutiveCorrectCount,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun GrammarWrongAnswerEntity.toSyncDto(userId: String) = SyncGrammarWrongAnswerDto(
    userId = userId,
    uuid = uuid,
    grammarId = grammarId,
    testMode = testMode,
    userAnswer = userAnswer,
    correctAnswer = correctAnswer,
    consecutiveCorrectCount = consecutiveCorrectCount,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    timestamp = timestamp
)

fun SyncFavoriteQuestionDto.toEntity() = FavoriteQuestionEntity(
    grammarId = grammarId,
    jsonId = jsonId,
    questionType = questionType,
    questionText = questionText,
    optionsJson = optionsJson,
    correctAnswer = correctAnswer,
    explanation = explanation,
    timestamp = timestamp
)

fun FavoriteQuestionEntity.toSyncDto(userId: String) = SyncFavoriteQuestionDto(
    userId = userId,
    grammarId = grammarId,
    jsonId = jsonId,
    questionType = questionType,
    questionText = questionText,
    optionsJson = optionsJson,
    correctAnswer = correctAnswer,
    explanation = explanation,
    timestamp = timestamp
)

// ===================================
// Domain Bridge (Optional, but useful)
// ===================================

fun SyncWordStateDto.toWordProgress() = WordProgress(
    wordId = wordId,
    srsLevel = repetitionCount,
    stability = stability,
    difficulty = difficulty,
    interval = interval,
    nextReviewDate = nextReviewDate,
    isFavorite = isFavorite,
    isSkipped = isSkipped,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    lastModifiedTime = lastModifiedTime,
    lastReviewedDate = lastReviewedDate,
    firstLearnedDate = firstLearnedDate,
    type = type
)

fun SyncGrammarStateDto.toGrammarProgress() = GrammarProgress(
    grammarId = grammarId,
    srsLevel = repetitionCount,
    stability = stability,
    difficulty = difficulty,
    interval = interval,
    nextReviewDate = nextReviewDate,
    isFavorite = isFavorite,
    isSkipped = isSkipped,
    isDeleted = isDeleted,
    deletedTime = deletedTime,
    lastModifiedTime = lastModifiedTime,
    lastReviewedDate = lastReviewedDate,
    firstLearnedDate = firstLearnedDate,
    type = type
)
