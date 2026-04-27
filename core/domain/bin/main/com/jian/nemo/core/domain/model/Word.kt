package com.jian.nemo.core.domain.model

/**
 * 单词领域模型
 *
 * 特点:
 * 1. 纯Kotlin，不依赖Android框架
 * 2. 不包含数据库注解
 * 3. 所有字段不可变（val）
 * 4. 包含业务逻辑方法
 * 5. 实现SrsItem接口,用于SRS算法计算
 */
data class Word(
    val id: Int,

    // ========== 核心内容 ==========
    /**
     * 日语原文
     */
    val japanese: String,

    /**
     * 假名读音
     */
    val hiragana: String,

    /**
     * 中文意思
     */
    val chinese: String,

    /**
     * JLPT等级 (N1-N5)
     */
    val level: String,

    /**
     * 词性 (Part of Speech)
     */
    val pos: String? = null,

    // ========== 例句（驼峰命名）==========
    val example1: String? = null,
    val gloss1: String? = null,
    val example2: String? = null,
    val gloss2: String? = null,
    val example3: String? = null,
    val gloss3: String? = null,

    /**
     * 是否已下架（不参与新词/复习队列）
     */
    val isDelisted: Boolean = false,

    // ========== SRS 复习字段 (FSRS 6) ==========
    /**
     * 重复次数 (0表示未学习)
     */
    override val repetitionCount: Int = 0,

    /**
     * 记忆稳定性 (FSRS) — 多少天后回忆概率降至90%
     */
    override val stability: Float = 0f,

    /**
     * 难度 (FSRS, 1-10)
     */
    override val difficulty: Float = 0f,

    /**
     * 间隔天数
     */
    override val interval: Int = 0,

    /**
     * 下次复习日期 (Epoch Day)
     */
    override val nextReviewDate: Long = 0,

    /**
     * 最后复习日期 (Epoch Day)
     */
    override val lastReviewedDate: Long? = null,

    /**
     * 首次学习日期 (Epoch Day)
     */
    override val firstLearnedDate: Long? = null,

    // ========== 用户交互字段 ==========
    /**
     * 是否收藏
     */
    val isFavorite: Boolean = false,

    /**
     * 是否跳过
     */
    val isSkipped: Boolean = false,

    /**
     * 今日暂缓到期日 (Epoch Day)
     * 如果此字段 == today，则今天不出现
     */
    val buriedUntilDay: Long = 0,

    // ========== 元数据 ==========
    /**
     * 最后修改时间戳 (毫秒)
     */
    val lastModifiedTime: Long = System.currentTimeMillis()
) : SrsItem {

    /**
     * 是否已学习
     */
    val isLearned: Boolean
        get() = repetitionCount > 0

    /**
     * 是否到期复习
     * @param today 今天的Epoch Day
     */
    fun isDueForReview(today: Long): Boolean {
        return isLearned && nextReviewDate <= today && !isSkipped
    }

    /**
     * 获取所有例句
     * @return 例句和翻译的配对列表
     */
    fun getAllExamples(): List<Pair<String, String>> {
        return listOfNotNull(
            example1?.let { it to (gloss1 ?: "") },
            example2?.let { it to (gloss2 ?: "") },
            example3?.let { it to (gloss3 ?: "") }
        )
    }
}
