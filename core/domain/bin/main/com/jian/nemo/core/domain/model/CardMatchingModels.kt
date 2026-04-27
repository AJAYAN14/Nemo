package com.jian.nemo.core.domain.model

/**
 * 卡片类型
 *
 * 参考: 旧项目 TestModels.kt 行152
 */
enum class CardType {
    /** 日文汉字 */
    TERM,
    /** 假名 */
    HIRAGANA,
    /** 中文释义 */
    DEFINITION
}

/**
 * 卡片状态
 *
 * 参考: 旧项目 TestModels.kt 行157
 */
enum class CardState {
    /** 未选中 */
    DEFAULT,
    /** 已选中 */
    SELECTED,
    /** 配对正确 */
    CORRECT,
    /** 配对错误 */
    INCORRECT,
    /** 已匹配完成(隐藏) */
    MATCHED
}

/**
 * 可匹配卡片
 *
 * 参考: 旧项目 TestModels.kt 行142-147
 *
 * @param id 单词ID，用于配对判断（相同ID表示可以配对）
 * @param text 卡片显示文本
 * @param type 卡片类型
 * @param state 卡片状态
 */
data class MatchableCard(
    val id: Int,
    val text: String,
    val type: CardType,
    val state: CardState = CardState.DEFAULT
)

/**
 * 反馈面板状态
 *
 * 参考: 旧项目 TestModels.kt 行162
 */
enum class FeedbackPanelState {
    /** 隐藏 */
    HIDDEN,
    /** 错误提示 */
    INCORRECT,
    /** 完成提示 */
    COMPLETE
}
