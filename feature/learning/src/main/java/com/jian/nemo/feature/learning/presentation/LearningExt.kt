package com.jian.nemo.feature.learning.presentation

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word

/**
 * 将领域模型列表转换为 LearningItem 列表
 */
@Suppress("UNCHECKED_CAST")
fun <T> List<T>.toLearningItems(): List<LearningItem> {
    return this.mapNotNull { item ->
        when (item) {
            is Word -> LearningItem.WordItem(item)
            is Grammar -> LearningItem.GrammarItem(item)
            else -> null
        }
    }
}

/**
 * 获取卡片状态标记扩展属性
 */
val LearningItem.cardBadge: CardBadge
    get() {
        // [容错处理] 如果复习次数大于0，但类型仍然标记为 NEW (0)，则修正为 REVIEW (2)
        // 这种情况通常发生于导入没有 type 字段的历史数据或同步字段缺失
        val effectiveType = if (this.repetitionCount > 0 && this.type == 0) 2 else this.type
        
        return when (effectiveType) {
            0 -> CardBadge.NEW
            1 -> CardBadge.LEARNING
            2 -> CardBadge.REVIEW
            3 -> CardBadge.RELEARN
            else -> CardBadge.REVIEW
        }
    }

/**
 * 解析学习步进配置字符串
 */
fun parseSteps(stepsStr: String): List<Int> {
    return try {
        stepsStr.trim().split(Regex("\\s+")).map { it.toInt() }
    } catch (e: Exception) {
        println("解析学习步进失败: $stepsStr, 使用默认值")
        listOf(1, 10)
    }
}
