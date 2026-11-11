package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import kotlinx.coroutines.flow.StateFlow

/**
 * 单词列表预加载快照
 */
data class WordListSnapshot(
    val wordsByLevel: Map<String, List<Word>> = emptyMap(),
    val countsByLevel: Map<String, Int> = emptyMap(),
    val allWords: List<Word> = emptyList(),
    val isReady: Boolean = false
)

/**
 * 语法列表预处理条目（已清洗注音文本）
 */
data class GrammarPreloadItem(
    val grammar: Grammar,
    val cleanedTitle: String,
    val cleanedFirstExplanation: String
)

/**
 * 语法列表预加载快照
 */
data class GrammarListSnapshot(
    val grammarsByLevel: Map<String, List<GrammarPreloadItem>> = emptyMap(),
    val countsByLevel: Map<String, Int> = emptyMap(),
    val allGrammars: List<GrammarPreloadItem> = emptyList(),
    val isReady: Boolean = false
)

/**
 * 词库列表应用级单例预加载缓存
 */
interface LibraryPreloadCache {
    val wordListSnapshot: StateFlow<WordListSnapshot>
    val grammarListSnapshot: StateFlow<GrammarListSnapshot>
    val settingIconStyleState: StateFlow<SettingIconStyle>

    /**
     * 启动后台异步预热；若词库尚未就绪，则等待就绪后自动开始订阅与防抖保鲜
     */
    fun warmUp()

    /**
     * 当页面已进入词库列表时立即激活后台订阅（若尚未启动）
     */
    fun ensureStartedNow()
}
