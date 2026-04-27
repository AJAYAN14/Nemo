package com.jian.nemo.core.common.util

/**
 * 语法搜索工具类
 * 
 * 用于处理带注音的文本（格式：汉字[假名]），支持：
 * 1. 提取纯文本（去掉[]及其内容）
 * 2. 提取全假名文本（将汉字替换为[]中的假名）
 * 3. 增强的匹配逻辑
 */
object GrammarSearchUtils {

    /**
     * 匹配汉字的正则表达式（包含扩展区和々）
     */
    private val RUBI_PATTERN = Regex("""([\u4E00-\u9FFF\u3400-\u4DBF々]+)\[([^\]]+)]""")

    /**
     * 去除注音，保留原始汉字/文本
     * 例如："去[きょ]年[ねん]" -> "去年"
     */
    fun cleanRubi(text: String): String {
        return RUBI_PATTERN.replace(text) { it.groupValues[1] }
    }

    /**
     * 提取注音，将汉字替换为对应的假名
     * 例如："去[きょ]年[ねん]" -> "きょねん"
     */
    fun extractKana(text: String): String {
        return RUBI_PATTERN.replace(text) { it.groupValues[2] }
    }

    /**
     * 增强匹配逻辑
     * 只要查询词出现在以下任一形式中即算匹配：
     * 1. 原始文本（带[]）
     * 2. 纯化后的文本（仅汉字）
     * 3. 提取假名后的文本（全假名）
     */
    fun isMatch(text: String, query: String): Boolean {
        if (query.isBlank()) return true
        
        // 1. 检查纯化后的文本（最常用，如搜索 "去年"）
        val cleaned = cleanRubi(text)
        if (cleaned.contains(query, ignoreCase = true)) return true
        
        // 2. 检查提取假名后的文本（搜索 "きょねん"）
        val kana = extractKana(text)
        if (kana.contains(query, ignoreCase = true)) return true
        
        // 3. 检查原始文本（备选，万一用户输入了带括号的内容）
        if (text.contains(query, ignoreCase = true)) return true
        
        return false
    }
}
