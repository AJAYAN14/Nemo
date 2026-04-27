package com.jian.nemo.core.domain.model

/**
 * 词性枚举
 *
 * 对应数据库中的12种词性分类
 */
enum class PartOfSpeech {
    /** 动词 */
    VERB,
    /** 名词 */
    NOUN,
    /** 形容词 */
    ADJECTIVE,
    /** 副词 */
    ADVERB,
    /** 助词 */
    PARTICLE,
    /** 接续词 */
    CONJUNCTION,
    /** 连体词 */
    RENTAI,
    /** 接头词 */
    PREFIX,
    /** 接尾词 */
    SUFFIX,
    /** 感叹词 */
    INTERJECTION,
    /** 固定表达 */
    FIXED_EXPRESSION,
    /** 外来语 */
    LOAN_WORD
}

