package com.jian.nemo.core.domain.model

import kotlinx.serialization.Serializable

/**
 * 导出信息的元数据
 */
@Serializable
data class ExportInfo(
    val appName: String = "Nemo",
    val version: Int = 2,
    val exportDate: String = java.util.Date().toString()
)

/**
 * 用户资料信息
 */
@Serializable
data class UserProfile(
    val userId: String,
    val username: String,
    val email: String,
    val avatar: String? = null
)

/**
 * 应用设置 (可选，不再同步)
 */
@Serializable
data class AppSettings(
    val theme: String,
    val dailyGoal: Int,
    val grammarDailyGoal: Int,
    val isUnmasteredOnlyMode: Boolean,

    val isDynamicColorEnabled: Boolean = false,
    val learningDayResetHour: Int = 4,

    val testQuestionCount: Int = 10,
    val testTimeLimitMinutes: Int = 10,
    val testShuffleQuestions: Boolean = true,
    val testShuffleOptions: Boolean = true,
    val testAutoAdvance: Boolean = true,
    val testPrioritizeWrong: Boolean = false,
    val testPrioritizeNew: Boolean = false,
    val testQuestionSource: String = "today",
    val testWrongAnswerRemovalThreshold: Int = 0,
    val testContentType: String = "mixed",
    val testSelectedWordLevels: Set<String> = setOf("N5", "N4", "N3", "N2", "N1"),
    val testSelectedGrammarLevels: Set<String> = setOf("N5", "N4", "N3", "N2", "N1"),

    val comprehensiveTestMultipleChoiceCount: Int = 4,
    val comprehensiveTestTypingCount: Int = 3,
    val comprehensiveTestCardMatchingCount: Int = 2,
    val comprehensiveTestSortingCount: Int = 5,

    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVoiceName: String? = null,
    val isAutoPlayAudioEnabled: Boolean = false,

    // 高级学习策略
    val learningSteps: String = "1 10",
    val learnAheadLimit: Int = 20,
    val relearningSteps: String = "1 10",
    val isRandomNewContentEnabled: Boolean = true,

    // 同步触发策略
    val isSyncOnLearningComplete: Boolean = true,
    val isSyncOnTestComplete: Boolean = true
)

/**
 * 单词学习进度
 */
@Serializable
data class WordProgress(
    val wordId: Int,
    val srsLevel: Int,
    val stability: Float = 0f,
    val difficulty: Float = 0f,
    val interval: Int,
    val nextReviewDate: Long,
    val isFavorite: Boolean,
    val isSkipped: Boolean,
    val isDeleted: Boolean = false,
    val deletedTime: Long = 0,
    val lastModifiedTime: Long = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis(),
    val lastReviewedDate: Long? = null,
    val firstLearnedDate: Long? = null,
    // 兼容旧备份
    val japanese: String? = null,
    val hiragana: String? = null,
    val chinese: String? = null,
    val level: String? = null
)

/**
 * 语法学习进度
 */
@Serializable
data class GrammarProgress(
    val grammarId: Int,
    val srsLevel: Int,
    val stability: Float = 0f,
    val difficulty: Float = 0f,
    val interval: Int,
    val nextReviewDate: Long,
    val isFavorite: Boolean,
    val isDeleted: Boolean = false,
    val deletedTime: Long = 0,
    val lastModifiedTime: Long = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis(),
    val lastReviewedDate: Long? = null,
    val firstLearnedDate: Long? = null,
    val uuid: String? = null,
    // 兼容旧备份
    val grammar: String? = null,
    val grammarLevel: String? = null
)

/**
 * 错题本数据
 */
@Serializable
data class WrongAnswerItem(
    val wordId: Int,
    val timestamp: Long,
    val testMode: String? = null,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val uuid: String? = null,
    val isDeleted: Boolean = false,
    val deletedTime: Long = 0,
    val lastModifiedTime: Long = 0
)

@Serializable
data class GrammarWrongAnswerItem(
    val grammarId: Int,
    val timestamp: Long,
    val testMode: String? = null,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val uuid: String? = null,
    val isDeleted: Boolean = false,
    val deletedTime: Long = 0,
    val lastModifiedTime: Long = 0
)

/**
 * 测试记录
 */
@Serializable
data class TestRecordItem(
    val date: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val testMode: String,
    val timestamp: Long,
    val uuid: String? = null,
    val isDeleted: Boolean = false,
    val deletedTime: Long = 0,
    val lastModifiedTime: Long = 0
)

/**
 * 学习记录
 */
@Serializable
data class StudyRecordItem(
    val date: Long,
    val learnedWords: Int = 0,
    val learnedGrammars: Int = 0,
    val reviewedWords: Int = 0,
    val reviewedGrammars: Int = 0,
    val skippedWords: Int = 0,
    val skippedGrammars: Int = 0,
    val testCount: Int = 0,
    val timestamp: Long,
    val isDeleted: Boolean = false,
    val deletedTime: Long = 0,
    val lastModifiedTime: Long = 0
)

/**
 * 错题本集合
 */
@Serializable
data class WrongAnswersData(
    val words: List<WrongAnswerItem>,
    val grammars: List<GrammarWrongAnswerItem>
)

@Serializable
data class FavoriteQuestionItem(
    val id: Int,
    val grammarId: Int?,
    val jsonId: String?,
    val questionType: String,
    val questionText: String,
    val optionsJson: String,
    val correctAnswer: String,
    val explanation: String?,
    val timestamp: Long
)

/**
 * 用户数据主体
 */
@Serializable
data class UserData(
    val profile: UserProfile,
    val settings: AppSettings?,
    val wordProgress: List<WordProgress>,
    val grammarProgress: List<GrammarProgress>,
    val favoriteQuestions: List<FavoriteQuestionItem> = emptyList(), // [NEW] Added for sync
    val wrongAnswers: WrongAnswersData,
    val testRecords: List<TestRecordItem>,
    val studyRecords: List<StudyRecordItem> = emptyList(),
    val todaySkippedWordIds: String? = null,
    val studyStreak: Int? = null,
    val testStreak: Int? = null,
    val lastTestDate: String? = null,
    val maxTestStreak: Int? = null,
    val totalStudyDays: Int? = null
)

/**
 * 完整的导出数据结构
 */
@Serializable
data class NemoExportData(
    val exportInfo: ExportInfo,
    val userData: UserData
)

@Serializable
data class SyncReport(
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val syncVersion: Int = 0,
    val stats: SyncStats = SyncStats(),
    val conflicts: List<ConflictInfo> = emptyList(),
    val operationType: SyncOperation = SyncOperation.PUSH
)

/**
 * 同步统计信息
 */
@Serializable
data class SyncStats(
    val totalItems: Int = 0,
    val changedItems: Int = 0,
    val mergedItems: Int = 0,
    val wordCount: Int = 0,
    val grammarCount: Int = 0,
    val wrongAnswerCount: Int = 0,
    val testRecordCount: Int = 0,
    val favoriteQuestionCount: Int = 0, // [NEW] Added for sync
    val addedItems: Int = 0,
    val updatedItems: Int = 0,
    val deletedItems: Int = 0
)

/**
 * 冲突信息
 */
@Serializable
data class ConflictInfo(
    val itemType: String,
    val itemId: Int,
    val fieldName: String,
    val localValue: String,
    val remoteValue: String,
    val resolvedValue: String,
    val resolutionStrategy: String
)

/**
 * 同步操作类型
 */
@Serializable
enum class SyncOperation {
    PUSH,
    RESTORE
}

/**
 * 同步进度状态
 */
sealed class SyncProgress {
    data object Idle : SyncProgress()

    data class Running(
        val section: String,
        val current: Int = 0,
        val total: Int = 0
    ) : SyncProgress()

    data class Completed(val report: SyncReport) : SyncProgress()

    data class Failed(val error: String) : SyncProgress()

    /** 已经有同步任务正在运行 */
    data object AlreadyRunning : SyncProgress()

    /** 恢复前需要用户确认（发现本地有未同步内容） */
    data object RequireConfirmation : SyncProgress()
}
