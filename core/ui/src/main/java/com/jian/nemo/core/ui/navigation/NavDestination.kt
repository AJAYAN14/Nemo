package com.jian.nemo.core.ui.navigation

/**
 * 应用所有路由定义
 *
 * 使用字符串路由（Navigation Compose标准方式）
 * 移动自 :app 模块以支持跨模块共享
 */
object NavDestination {
    // 底部导航栏主要路由
    const val LEARNING = "learning"  // 学习页（替代原HOME）
    const val PROGRESS = "progress"  // 进度页
    const val TEST = "test"          // 测试页
    const val SETTINGS = "settings"  // 个人/设置页
    const val TTS_SETTINGS = "tts_settings" // TTS设置页
    const val THEME_SETTINGS = "theme_settings" // 主题设置页
    const val ADVANCED_LEARNING = "advanced_learning" // 记忆算法配置页


    // 保留HOME作为LEARNING的别名，确保兼容性
    const val HOME = LEARNING

    // 二级页面路由 (⚠️ 迁移中: 建议由各 feature 模块内部定义路由字符串)
    @Deprecated("Move to feature:learning module", ReplaceWith("LearningDestination.WORD_LEARNING"))
    const val WORD_LEARNING = "word_learning/{level}"
    @Deprecated("Move to feature:learning module", ReplaceWith("LearningDestination.GRAMMAR_LEARNING"))
    const val GRAMMAR_LEARNING = "grammar_learning/{level}"
    @Deprecated("Move to feature:learning module", ReplaceWith("LearningDestination.WORD_REVIEW"))
    const val WORD_REVIEW = "word_review"
    @Deprecated("Move to feature:learning module", ReplaceWith("LearningDestination.GRAMMAR_REVIEW"))
    const val GRAMMAR_REVIEW = "grammar_review"

    const val STATISTICS = "statistics"
    const val PROFILE = "profile"
    const val FAVORITES = "favorites"
    const val MISTAKES = "mistakes"
    const val PART_OF_SPEECH = "part_of_speech"
    const val POS_WORDS = "pos_words/{pos}"

    // 复习与训练路由
    @Deprecated("Move to feature:learning module")
    const val DUE_REVIEW = "due_review"
    const val CATEGORY_CLASSIFICATION = "category_classification/{source}"
    const val CATEGORY_CARD_LEARNING = "category_card_learning/{category}/{categoryTitle}"
    const val CATEGORY_WORDS = "category_words/{category}/{categoryTitle}"

    // 数据与资料路由
    const val LEARNING_CALENDAR = "learning_calendar"
    const val ACTIVITY_HEATMAP = "activity_heatmap" // Added
    const val KANA_CHART = "kana_chart"
    const val HISTORICAL_STATISTICS = "historical_statistics"
    const val WORD_LIST = "word_list"
    const val GRAMMAR_LIST = "grammar_list"
    const val LEECH_MANAGEMENT = "leech_management"

    // 详情页路由
    const val WORD_DETAIL = "wordDetail/{wordId}"
    const val GRAMMAR_DETAIL = "grammarDetail/{grammarId}"

    // Feature: Library
    @Deprecated("Move to feature:library module")
    const val LIBRARY = "library"

    // Test Feature Routes
    @Deprecated("Move to feature:test module")
    const val TEST_SETTINGS = "test/settings?testModeId={testModeId}"
    @Deprecated("Move to feature:test module")
    const val TEST_EXECUTION = "test/{level}/{mode}?questionType={questionType}&contentType={contentType}&source={source}"

    // 路由构建器
    fun wordLearning(level: String) = "word_learning/$level"
    fun grammarLearning(level: String) = "grammar_learning/$level"
    fun posWords(pos: String) = "pos_words/$pos"
    fun categoryClassification(source: String = "practice") = "category_classification/$source"
    fun categoryCardLearning(category: String, categoryTitle: String) = "category_card_learning/$category/$categoryTitle"
    fun categoryWords(category: String, categoryTitle: String) = "category_words/$category/$categoryTitle"
    fun wordDetail(wordId: Int) = "wordDetail/$wordId"
    fun grammarDetail(grammarId: Int) = "grammarDetail/$grammarId"

    // Test Route Builders
    fun testSettings(testModeId: String? = null) = if (testModeId != null) "test/settings?testModeId=$testModeId" else "test/settings"

    fun testExecution(
        level: String,
        mode: String,
        questionType: String,
        contentType: String = "words",
        source: String = "today"
    ) = "test/$level/$mode?questionType=$questionType&contentType=$contentType&source=$source"
}
