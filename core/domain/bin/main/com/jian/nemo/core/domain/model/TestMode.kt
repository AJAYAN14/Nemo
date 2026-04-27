package com.jian.nemo.core.domain.model

/**
 * 测试模式
 * 定义4种选择题测试模式
 */
enum class TestMode {
    /** 日译中：显示日语，选择中文释义 */
    JP_TO_CN,

    /** 中译日：显示中文，选择日语单词 */
    CN_TO_JP,

    /** 假名：显示日语单词，选择假名 */
    KANA,

    /** 例句：显示例句，选择意思 */
    EXAMPLE,

    /** 全随机：随机选择以上所有模式中的对应关系（单词特有的 8 种模式，含词性） */
    RANDOM
}
