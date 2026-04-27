package com.jian.nemo.core.domain.repository

/**
 * 学习会话Repository接口
 *
 * 管理学习会话的持久化状态
 */
interface SessionRepository {

    /**
     * 保存学习会话
     *
     * @param wordIds 单词ID列表
     * @param currentIndex 当前学习位置
     * @param level 学习等级（如 "n5"）
     */
    suspend fun saveLearningSession(
        wordIds: List<Int>,
        currentIndex: Int,
        level: String
    )

    /**
     * 获取学习会话
     *
     * @return Pair(wordIds, currentIndex)
     */
    suspend fun getLearningSession(): Pair<List<Int>, Int>

    /**
     * 清空学习会话
     */
    suspend fun clearLearningSession()

    /**
     * 检查会话是否有效
     *
     * @return true表示会话是今天创建的，仍然有效
     */
    suspend fun isSessionValid(): Boolean
}
