package com.jian.nemo.core.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * DataStore 配置键定义
 *
 * 集中管理所有配置项，确保键名唯一性
 */
object PreferencesKeys {

    // ========== 数据同步 ==========
    val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
    val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    val LAST_SYNC_SUCCESS = booleanPreferencesKey("last_sync_success")
    val LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")
    val LAST_RESTORE_TIME = longPreferencesKey("last_restore_time")
    /** 上次同步冲突数量 */
    val LAST_SYNC_CONFLICT_COUNT = intPreferencesKey("last_sync_conflict_count")

    /** 是否正在恢复中（用于处理恢复中断） */
    val IS_RESTORING = booleanPreferencesKey("is_restoring")

    /** 学习完成后同步 默认: true */
    val SYNC_ON_LEARNING_COMPLETE = booleanPreferencesKey("sync_on_learning_complete")

    /** 测试完成后同步 默认: true */
    val SYNC_ON_TEST_COMPLETE = booleanPreferencesKey("sync_on_test_complete")

    // ========== 用户设置 ==========

    /** 用户头像路径 */
    val USER_AVATAR_PATH = stringPreferencesKey("user_avatar_path")

    /** 每日学习目标（单词数）默认: 20 */
    val DAILY_GOAL = intPreferencesKey("daily_goal")

    /** 每日学习目标（语法条数）默认: 10 */
    val GRAMMAR_DAILY_GOAL = intPreferencesKey("grammar_daily_goal")

    /** 待定每日学习目标（单词数）- 次日生效 */
    val PENDING_DAILY_GOAL = intPreferencesKey("pending_daily_goal")

    /** 待定每日学习目标（语法条数）- 次日生效 */
    val PENDING_GRAMMAR_DAILY_GOAL = intPreferencesKey("pending_grammar_daily_goal")

    /** 待定目标设置时的逻辑学习日 - 用于确保仅在“下一个”重置点后生效 */
    val PENDING_GOAL_SET_DATE = longPreferencesKey("pending_goal_set_date")

    /** 主题色 (存储为 ARGB Long 值) 默认: null (使用品牌蓝 0xFF0E68FF) */
    val THEME_COLOR = longPreferencesKey("theme_color")

    /** 深色模式（null=跟随系统） */
    val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")

    /** 深色模式策略 (system, scheduled) 默认: system */
    val DARK_MODE_STRATEGY = stringPreferencesKey("dark_mode_strategy")

    /** 浅色模式开始时间 (HH:mm) 默认: 07:00 */
    val DARK_MODE_START_TIME = stringPreferencesKey("dark_mode_start_time")

    /** 深色模式开始时间 (HH:mm) 默认: 19:00 */
    val DARK_MODE_END_TIME = stringPreferencesKey("dark_mode_end_time")

    /** 动态颜色（Android 12+）默认: true */
    val IS_DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("is_dynamic_color_enabled")

    /** 学习日重置时间（小时,0-23）默认: 4 (凌晨4:00) */
    val LEARNING_DAY_RESET_HOUR = intPreferencesKey("learning_day_reset_hour")

    /** 应用图标名称 默认: "Nemo" */
    val APP_ICON = stringPreferencesKey("app_icon")

    /** 通知开关 默认: true */
    val IS_NOTIFICATION_ENABLED = booleanPreferencesKey("is_notification_enabled")

    /** 复习提醒时间 默认: "20:00" */
    val REVIEW_REMINDER_TIME = stringPreferencesKey("review_reminder_time")

    // ========== 学习统计 ==========

    /** 连续学习天数 */
    val DAILY_STREAK = intPreferencesKey("daily_streak")

    /** 最后学习日期（Epoch Day） */
    val LAST_STUDY_DATE = longPreferencesKey("last_study_date")

    /** 累计学习天数 */
    val TOTAL_STUDY_DAYS = intPreferencesKey("total_study_days")

    /** 连续测试天数 */
    val TEST_STREAK = intPreferencesKey("test_streak")

    /** 最高连续测试天数 */
    val MAX_TEST_STREAK = intPreferencesKey("max_test_streak")

    /** 上次测试日期（Epoch Day） */
    val LAST_TEST_DATE = longPreferencesKey("last_test_date")

    /** 今日复习过的单词IDs */
    val TODAY_TESTED_WORD_IDS = stringSetPreferencesKey("today_tested_word_ids")

    /** 今日复习错误的单词IDs */
    val TODAY_WRONG_WORD_IDS = stringSetPreferencesKey("today_wrong_word_ids")

    /** 今日复习过的语法IDs */
    val TODAY_TESTED_GRAMMAR_IDS = stringSetPreferencesKey("today_tested_grammar_ids")

    /** 今日复习错误的语法IDs */
    val TODAY_WRONG_GRAMMAR_IDS = stringSetPreferencesKey("today_wrong_grammar_ids")

    // ========== 学习会话 ==========

    /** 当前会话单词ID列表（JSON字符串） */
    val SESSION_WORD_IDS = stringPreferencesKey("session_word_ids")

    /** 当前学习位置 */
    val SESSION_CURRENT_INDEX = intPreferencesKey("session_current_index")

    /** 当前学习等级 */
    val SESSION_LEVEL = stringPreferencesKey("session_level")

    /** 会话开始日期（Epoch Day） */
    val SESSION_START_DATE = longPreferencesKey("session_start_date")

    /** 当前会话语法ID列表（JSON字符串） */
    val SESSION_GRAMMAR_IDS = stringPreferencesKey("session_grammar_ids")

    /** 当前语法学习位置 */
    val SESSION_GRAMMAR_CURRENT_INDEX = intPreferencesKey("session_grammar_current_index")

    /** 当前语法学习等级 */
    val SESSION_GRAMMAR_LEVEL = stringPreferencesKey("session_grammar_level")

    /** 语法会话开始日期（Epoch Day） */
    val SESSION_GRAMMAR_START_DATE = longPreferencesKey("session_grammar_start_date")

    /** 当前会话单词学习步长 (JSON Map: id -> step) */
    val SESSION_WORD_STEPS = stringPreferencesKey("session_word_steps")

    /** 当前会话语法学习步长 (JSON Map: id -> step) */
    val SESSION_GRAMMAR_STEPS = stringPreferencesKey("session_grammar_steps")

    /** 当前会话等待结束时间 (Epoch Millis) */
    val SESSION_WAITING_UNTIL = longPreferencesKey("session_waiting_until")



    // ========== 应用配置 ==========

    /** 是否首次启动 默认: true */
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")

    /** 上次启动的版本号 */
    val APP_VERSION_CODE = intPreferencesKey("app_version_code")

    /** 词库 JSON 云更新：上次已应用的内容版本号 */
    val LAST_CONTENT_VERSION = intPreferencesKey("last_content_version")

    /** 已关闭的通知 ID 集合 */
    val DISMISSED_NOTIFICATION_IDS = stringSetPreferencesKey("dismissed_notification_ids")

    /** 默认学习等级 默认: "n5" */
    val PREFERRED_WORD_LEVEL = stringPreferencesKey("preferred_word_level")

    /** 默认语法等级 默认: "n5" */
    val PREFERRED_GRAMMAR_LEVEL = stringPreferencesKey("preferred_grammar_level")

    /** 配置最后修改时间 (用于同步) */
    val LAST_SETTINGS_MODIFIED_TIME = longPreferencesKey("last_settings_modified_time")

    // ========== 测试配置 ==========

    /** 测试题目数量 */
    val TEST_QUESTION_COUNT = intPreferencesKey("test_question_count")

    /** 测试时间限制(分钟) 0=无限制 */
    val TEST_TIME_LIMIT_MINUTES = intPreferencesKey("test_time_limit_minutes")

    /** 题目乱序 */
    val TEST_SHUFFLE_QUESTIONS = booleanPreferencesKey("test_shuffle_questions")

    /** 选项乱序 */
    val TEST_SHUFFLE_OPTIONS = booleanPreferencesKey("test_shuffle_options")

    /** 答對自动跳转 */
    val TEST_AUTO_ADVANCE = booleanPreferencesKey("test_auto_advance")

    /** 错题优先 */
    val TEST_PRIORITIZE_WRONG = booleanPreferencesKey("test_prioritize_wrong")

    /** 未学优先 */
    val TEST_PRIORITIZE_NEW = booleanPreferencesKey("test_prioritize_new")

    /** 题目来源 (all, favorites, wrong, today) */
    val TEST_QUESTION_SOURCE = stringPreferencesKey("test_question_source")

    /** 错题移除阈值 */
    val TEST_WRONG_ANSWER_REMOVAL_THRESHOLD = intPreferencesKey("test_wrong_answer_removal_threshold")

    /** 测试内容类型 (words, grammar, mixed) */
    val TEST_CONTENT_TYPE = stringPreferencesKey("test_content_type")

    /** 选中的单词等级列表 (JSON or comma-separated) - 这里使用 StringSet存储 */
    val TEST_SELECTED_WORD_LEVELS = stringSetPreferencesKey("test_selected_word_levels")

    /** 选中的语法等级列表 (JSON or comma-separated) - 这里使用 StringSet存储 */
    val TEST_SELECTED_GRAMMAR_LEVELS = stringSetPreferencesKey("test_selected_grammar_levels")

    // ========== 综合测试配置 (各个题型的数量) ==========

    /** 综合测试：选择题数量 */
    val COMPREHENSIVE_TEST_MC_COUNT = intPreferencesKey("comprehensive_test_mc_count")
    
    /** 综合测试：拼写题数量 */
    val COMPREHENSIVE_TEST_TYPING_COUNT = intPreferencesKey("comprehensive_test_typing_count")
    
    /** 综合测试：配对题数量 */
    val COMPREHENSIVE_TEST_MATCHING_COUNT = intPreferencesKey("comprehensive_test_matching_count")
    
    /** 综合测试：排序题数量 */
    val COMPREHENSIVE_TEST_SORTING_COUNT = intPreferencesKey("comprehensive_test_sorting_count")

    // ========== 学习状态持久化 ==========

    /** 上次选择的学习模式 (word, grammar) */
    val LAST_LEARNING_MODE = stringPreferencesKey("last_learning_mode")
    // Leech/Lapse Counts (Store as JSON String: Map<Int, Int>)
    val KEY_WORD_LAPSES = stringPreferencesKey("word_lapses")
    val KEY_GRAMMAR_LAPSES = stringPreferencesKey("grammar_lapses")

    /** 新内容随机抽取开关 默认: true */
    val IS_RANDOM_NEW_CONTENT_ENABLED = booleanPreferencesKey("is_random_new_content_enabled")

    // ========== 学习高级设置 ==========

    /** 学习步进 (例如 "1 10") 默认: "1 10" */
    val LEARNING_STEPS = stringPreferencesKey("learning_steps")
    val RELEARNING_STEPS = stringPreferencesKey("relearning_steps")

    /** 提前学习限制 (分钟) 默认: 20 */
    val LEARN_AHEAD_LIMIT = intPreferencesKey("learn_ahead_limit")

    /** Leech 阈值 (累计 lapse) 默认: 5 */
    val LEECH_THRESHOLD = intPreferencesKey("leech_threshold")

    /** Leech 行为 默认: skip (skip | bury_today) */
    val LEECH_ACTION = stringPreferencesKey("leech_action")

    // ========== TTS 设置 ==========

    /** 朗读语速 默认: 1.0f */
    val TTS_SPEECH_RATE = androidx.datastore.preferences.core.floatPreferencesKey("tts_speech_rate")

    /** 朗读音调 默认: 1.0f */
    val TTS_PITCH = androidx.datastore.preferences.core.floatPreferencesKey("tts_pitch")

    /** 朗读语音名称 (Locale + Name) 默认: null (system default) */
    val TTS_VOICE_NAME = stringPreferencesKey("tts_voice_name")

    /** 翻面自动朗读开关 默认: true */
    val IS_AUTO_PLAY_AUDIO_ENABLED = booleanPreferencesKey("is_auto_play_audio_enabled")

    /** 显示答案等待开关 默认: false */
    val IS_SHOW_ANSWER_DELAY_ENABLED = booleanPreferencesKey("is_show_answer_delay_enabled")

    /** 显示答案等待时长 (毫秒) 默认: 5000 */
    val SHOW_ANSWER_DELAY_MS = longPreferencesKey("show_answer_delay_ms")

    // ========== 恢复断点续传 ==========

    /** 恢复断点表名 默认: "" */
    val RESTORE_CHECKPOINT_TABLE = stringPreferencesKey("restore_checkpoint_table")

    /** 恢复断点偏移量 默认: 0 */
    val RESTORE_CHECKPOINT_OFFSET = intPreferencesKey("restore_checkpoint_offset")

    // ========== 动态 Key 生成辅助方法 ==========

    fun getTestQuestionCountKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Int> {
        return if (mode.isNullOrEmpty()) TEST_QUESTION_COUNT else intPreferencesKey("${mode}_test_question_count")
    }

    fun getTestTimeLimitKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Int> {
        return if (mode.isNullOrEmpty()) TEST_TIME_LIMIT_MINUTES else intPreferencesKey("${mode}_test_time_limit_minutes")
    }

    fun getTestShuffleQuestionsKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Boolean> {
        return if (mode.isNullOrEmpty()) TEST_SHUFFLE_QUESTIONS else booleanPreferencesKey("${mode}_test_shuffle_questions")
    }

    fun getTestShuffleOptionsKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Boolean> {
        return if (mode.isNullOrEmpty()) TEST_SHUFFLE_OPTIONS else booleanPreferencesKey("${mode}_test_shuffle_options")
    }

    fun getTestAutoAdvanceKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Boolean> {
        return if (mode.isNullOrEmpty()) TEST_AUTO_ADVANCE else booleanPreferencesKey("${mode}_test_auto_advance")
    }

    fun getTestPrioritizeWrongKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Boolean> {
        return if (mode.isNullOrEmpty()) TEST_PRIORITIZE_WRONG else booleanPreferencesKey("${mode}_test_prioritize_wrong")
    }

    fun getTestPrioritizeNewKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Boolean> {
        return if (mode.isNullOrEmpty()) TEST_PRIORITIZE_NEW else booleanPreferencesKey("${mode}_test_prioritize_new")
    }

    fun getTestQuestionSourceKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<String> {
        return if (mode.isNullOrEmpty()) TEST_QUESTION_SOURCE else stringPreferencesKey("${mode}_test_question_source")
    }

    fun getTestWrongAnswerRemovalThresholdKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Int> {
        return if (mode.isNullOrEmpty()) TEST_WRONG_ANSWER_REMOVAL_THRESHOLD else intPreferencesKey("${mode}_test_wrong_answer_removal_threshold")
    }

    fun getTestContentTypeKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<String> {
        return if (mode.isNullOrEmpty()) TEST_CONTENT_TYPE else stringPreferencesKey("${mode}_test_content_type")
    }

    fun getTestSelectedWordLevelsKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Set<String>> {
        return if (mode.isNullOrEmpty()) TEST_SELECTED_WORD_LEVELS else stringSetPreferencesKey("${mode}_test_selected_word_levels")
    }

    fun getTestSelectedGrammarLevelsKey(mode: String?): androidx.datastore.preferences.core.Preferences.Key<Set<String>> {
        return if (mode.isNullOrEmpty()) TEST_SELECTED_GRAMMAR_LEVELS else stringSetPreferencesKey("${mode}_test_selected_grammar_levels")
    }
}
