package com.jian.nemo.core.ui.navigation

/**
 * 单词与语法详情界面的滑动上下文持有者
 *
 * 用于在从特定列表（如“今日统计”、“历史统计”等）跳转到详情界面时，
 * 传递当前列表的 ID 序列，以便详情界面的 Pager 能够按照进入时的列表顺序进行左右滑动切换。
 */
object DetailContextHolder {
    @Volatile
    private var wordContextIds: List<Int>? = null

    @Volatile
    private var grammarContextIds: List<Int>? = null

    /**
     * 设置单词滑动上下文
     * @param ids 当前列表的单词 ID 序列
     */
    fun setWordContext(ids: List<Int>) {
        wordContextIds = if (ids.isEmpty()) null else ids.toList()
    }

    /**
     * 消费单词滑动上下文
     * @param targetId 当前进入详情页的目标单词 ID
     * @return 如果暂存的上下文包含目标 ID，则返回该上下文并清空暂存；否则返回 null
     */
    fun consumeWordContext(targetId: Int): List<Int>? {
        val list = wordContextIds
        return if (list != null && list.contains(targetId)) {
            wordContextIds = null
            list
        } else {
            null
        }
    }

    /**
     * 设置语法滑动上下文
     * @param ids 当前列表的语法 ID 序列
     */
    fun setGrammarContext(ids: List<Int>) {
        grammarContextIds = if (ids.isEmpty()) null else ids.toList()
    }

    /**
     * 消费语法滑动上下文
     * @param targetId 当前进入详情页的目标语法 ID
     * @return 如果暂存的上下文包含目标 ID，则返回该上下文并清空暂存；否则返回 null
     */
    fun consumeGrammarContext(targetId: Int): List<Int>? {
        val list = grammarContextIds
        return if (list != null && list.contains(targetId)) {
            grammarContextIds = null
            list
        } else {
            null
        }
    }

    /**
     * 清理所有上下文缓存
     */
    fun clear() {
        wordContextIds = null
        grammarContextIds = null
    }
}
