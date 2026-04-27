package com.jian.nemo.core.common.util

/**
 * 假名到罗马音转换工具
 *
 * 参考: 阶段12文档 - 12-测试系统-打字练习.md
 *
 * 简化实现，支持基本的平假名转罗马音
 */
object RomajiConverter {

    /**
     * 平假名到罗马音映射表
     */
    private val hiraganaToRomaji = mapOf(
        // 五十音图 - 清音
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
        "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
        "や" to "ya", "ゆ" to "yu", "よ" to "yo",
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
        "わ" to "wa", "を" to "wo", "ん" to "n",

        // 浊音
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
        "だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",

        // 半浊音
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",

        // 拗音
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",

        // 小字
        "ぁ" to "a", "ぃ" to "i", "ぅ" to "u", "ぇ" to "e", "ぉ" to "o",
        "ゃ" to "ya", "ゅ" to "yu", "ょ" to "yo", "ゎ" to "wa",
        "っ" to "" // 促音特殊处理
    )

    /**
     * 将平假名字符串转换为罗马音
     *
     * @param hiragana 平假名字符串
     * @return 罗马音字符串
     */
    fun toRomaji(hiragana: String): String {
        if (hiragana.isBlank()) return ""

        val result = StringBuilder()
        var i = 0

        while (i < hiragana.length) {
            // 尝试匹配两个字符的拗音
            if (i + 1 < hiragana.length) {
                val twoChar = hiragana.substring(i, i + 2)
                if (hiraganaToRomaji.containsKey(twoChar)) {
                    result.append(hiraganaToRomaji[twoChar])
                    i += 2
                } else {
                    // 单字符处理
                    val oneChar = hiragana[i].toString()
                    processChar(oneChar, hiragana, i, result)
                    i++
                }
            } else {
                // 单字符处理
                val oneChar = hiragana[i].toString()
                processChar(oneChar, hiragana, i, result)
                i++
            }
        }

        return result.toString()
    }

    private fun processChar(oneChar: String, hiragana: String, i: Int, result: StringBuilder) {
        val romaji = hiraganaToRomaji[oneChar]

        when {
            romaji != null -> {
                // 处理促音（っ）
                if (oneChar == "っ" && i + 1 < hiragana.length) {
                    // 促音：下一个假名的首字母重复
                    val nextChar = hiragana[i + 1].toString()
                    val nextRomaji = hiraganaToRomaji[nextChar]
                    if (nextRomaji != null && nextRomaji.isNotEmpty()) {
                        result.append(nextRomaji[0])
                    }
                } else {
                    result.append(romaji)
                }
            }
            oneChar == "ー" -> {
                // 长音符号：根据前一个音决定
                // 简化处理：忽略或用前一个元音
            }
            else -> {
                // 非假名字符（如汉字、片假名），保持原样
                result.append(oneChar)
            }
        }
    }
}
