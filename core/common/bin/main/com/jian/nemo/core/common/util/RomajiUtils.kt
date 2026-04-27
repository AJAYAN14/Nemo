package com.jian.nemo.core.common.util

/**
 * Romaji 工具类
 * 用于验证用户输入的罗马音是否匹配假名答案
 */
object RomajiUtils {

    /**
     * 验证输入是否匹配答案（支持假名或罗马音）
     */
    fun isMatch(input: String, answer: String): Boolean {
        val trimmedInput = input.trim().lowercase()
        val trimmedAnswer = answer.trim()

        // 1. 直接匹配（假名匹配假名）
        if (trimmedInput == trimmedAnswer) return true

        // 2. 罗马音匹配
        // 生成该假名答案的所有有效罗马音组合
        val validRomajiList = toRomaji(trimmedAnswer)
        return validRomajiList.contains(trimmedInput)
    }

    /**
     * 将假名转换为所有可能的罗马音组合
     */
    fun toRomaji(kana: String): List<String> {
        if (kana.isEmpty()) return listOf("")

        val results = mutableListOf<String>()

        // 尝试匹配不同长度的前缀 (为了优先匹配拗音, e.g., "きゃ" 而不是 "き" + "ゃ")
        // 最长匹配 2 个字符 (通常拗音是2个字符, e.g. きゃ. 促音可能会影响, e.g. っきゃ)
        // 实际上表里面最长键就是2个字符 (拗音)

        // 1. 尝试匹配2个字符 (拗音)
        if (kana.length >= 2) {
            val twoChars = kana.substring(0, 2)
            if (kanaTable.containsKey(twoChars)) {
                val romajiPrefixes = kanaTable[twoChars]!!
                val suffixes = toRomaji(kana.substring(2))
                for (prefix in romajiPrefixes) {
                    for (suffix in suffixes) {
                        results.add(prefix + suffix)
                    }
                }
            }
        }

        // 2. 尝试匹配1个字符
        val oneChar = kana.substring(0, 1)

        // 特殊处理：促音 (っ/ッ)
        if ((oneChar == "っ" || oneChar == "ッ") && kana.length > 1) {
            // 获取下一个字符（或组合）的辅音
            // 递归获取剩余部分的罗马音，然后双写首字母
            val suffixes = toRomaji(kana.substring(1))
            for (suffix in suffixes) {
                if (suffix.isNotEmpty()) {
                    // 取第一个字母双写
                    // 注意：如果是 'ch' (chi, cha) -> 'tch'? 通常 'cch' 或 'tch'
                    // Hepburn: 'tchi' -> 'ttchi' (wrong), 'cchi' (wrong).
                    // stardard: sokuon usually doubles the first consonant.
                    // 'chi' -> 'tchi' or 'cchi' depending on system.
                    // 简单规则：直接重复首字母. e.g. "k" -> "kk"
                    val firstConsonant = suffix.first()
                    results.add(firstConsonant + suffix)
                }
            }
            // 也可以作为 'tsu' / 'tu' 独立输入 (如果不在促音位置)
            // 下面的单字符匹配会处理 'っ' -> 'tsu' / 'tu'
        }

        if (kanaTable.containsKey(oneChar)) {
            val romajiPrefixes = kanaTable[oneChar]!!
            val suffixes = toRomaji(kana.substring(1))
            for (prefix in romajiPrefixes) {
                for (suffix in suffixes) {
                    results.add(prefix + suffix)
                }
            }
        }

        // 如果都不匹配，保留原字符（比如汉字或标点），继续处理剩余部分
        if (results.isEmpty()) {
            val suffixes = toRomaji(kana.substring(1))
            for (suffix in suffixes) {
                results.add(oneChar + suffix)
            }
        }

        // 去重
        return results.distinct()
    }

    // 完整的假名映射表
    private val kanaTable = mapOf(
        // 清音
        "あ" to listOf("a"), "い" to listOf("i"), "う" to listOf("u"), "え" to listOf("e"), "お" to listOf("o"),
        "か" to listOf("ka"), "き" to listOf("ki"), "く" to listOf("ku"), "け" to listOf("ke"), "こ" to listOf("ko"),
        "さ" to listOf("sa"), "し" to listOf("shi", "si"), "す" to listOf("su"), "せ" to listOf("se"), "そ" to listOf("so"),
        "た" to listOf("ta"), "ち" to listOf("chi", "ti"), "つ" to listOf("tsu", "tu"), "て" to listOf("te"), "と" to listOf("to"),
        "な" to listOf("na"), "に" to listOf("ni"), "ぬ" to listOf("nu"), "ね" to listOf("ne"), "の" to listOf("no"),
        "は" to listOf("ha"), "ひ" to listOf("hi"), "ふ" to listOf("fu", "hu"), "へ" to listOf("he"), "ほ" to listOf("ho"),
        "ま" to listOf("ma"), "み" to listOf("mi"), "む" to listOf("mu"), "め" to listOf("me"), "も" to listOf("mo"),
        "や" to listOf("ya"), "ゆ" to listOf("yu"), "よ" to listOf("yo"),
        "ら" to listOf("ra"), "り" to listOf("ri"), "る" to listOf("ru"), "れ" to listOf("re"), "ろ" to listOf("ro"),
        "わ" to listOf("wa"), "を" to listOf("wo", "o"), "ん" to listOf("n", "nn"),

        // 浊音
        "が" to listOf("ga"), "ぎ" to listOf("gi"), "ぐ" to listOf("gu"), "げ" to listOf("ge"), "ご" to listOf("go"),
        "ざ" to listOf("za"), "じ" to listOf("ji", "zi"), "ず" to listOf("zu"), "ぜ" to listOf("ze"), "ぞ" to listOf("zo"),
        "だ" to listOf("da"), "ぢ" to listOf("ji", "di"), "づ" to listOf("zu", "du"), "で" to listOf("de"), "ど" to listOf("do"),
        "ば" to listOf("ba"), "び" to listOf("bi"), "ぶ" to listOf("bu"), "べ" to listOf("be"), "ぼ" to listOf("bo"),

        // 半浊音
        "ぱ" to listOf("pa"), "ぴ" to listOf("pi"), "ぷ" to listOf("pu"), "ぺ" to listOf("pe"), "ぽ" to listOf("po"),

        // 小假名 (普通)
        "ぁ" to listOf("xa", "la"), "ぃ" to listOf("xi", "li"), "ぅ" to listOf("xu", "lu"), "ぇ" to listOf("xe", "le"), "ぉ" to listOf("xo", "lo"),
        "ゃ" to listOf("ya", "xya", "lya"), "ゅ" to listOf("yu", "xyu", "lyu"), "ょ" to listOf("yo", "xyo", "lyo"),
        "っ" to listOf("tsu", "tu", "xtu", "ltsu"),

        // 拗音 (Contracted sounds)
        "きゃ" to listOf("kya"), "きゅ" to listOf("kyu"), "きョ" to listOf("kyo"), "きょ" to listOf("kyo"),
        "しゃ" to listOf("sha", "sya"), "しゅ" to listOf("shu", "syu"), "しょ" to listOf("sho", "syo"),
        "ちゃ" to listOf("cha", "tya"), "ちゅ" to listOf("chu", "tyu"), "ちょ" to listOf("cho", "tyo"),
        "にゃ" to listOf("nya"), "にゅ" to listOf("nyu"), "にョ" to listOf("nyo"), "にょ" to listOf("nyo"),
        "ひゃ" to listOf("hya"), "ひゅ" to listOf("hyu"), "ひョ" to listOf("hyo"), "ひょ" to listOf("hyo"),
        "みゃ" to listOf("mya"), "みゅ" to listOf("myu"), "みョ" to listOf("myo"), "みょ" to listOf("myo"),
        "りゃ" to listOf("rya"), "りゅ" to listOf("ryu"), "りョ" to listOf("ryo"), "りょ" to listOf("ryo"),
        "ぎゃ" to listOf("gya"), "ぎゅ" to listOf("gyu"), "ぎョ" to listOf("gyo"), "ぎょ" to listOf("gyo"),
        "じゃ" to listOf("ja", "zya", "jya"), "じゅ" to listOf("ju", "zyu", "jyu"), "じョ" to listOf("jo", "zyo", "jyo"), "じょ" to listOf("jo", "zyo", "jyo"),
        "びゃ" to listOf("bya"), "びゅ" to listOf("byu"), "びョ" to listOf("byo"), "びょ" to listOf("byo"),
        "ぴゃ" to listOf("pya"), "ぴゅ" to listOf("pyu"), "ぴョ" to listOf("pyo"), "ぴょ" to listOf("pyo"),

        // 片假名 (对应平假名)
        "ア" to listOf("a"), "イ" to listOf("i"), "I" to listOf("i"), "ウ" to listOf("u"), "エ" to listOf("e"), "オ" to listOf("o"),
        "カ" to listOf("ka"), "キ" to listOf("ki"), "ク" to listOf("ku"), "ケ" to listOf("ke"), "コ" to listOf("ko"),
        "サ" to listOf("sa"), "シ" to listOf("shi", "si"), "ス" to listOf("su"), "セ" to listOf("se"), "ソ" to listOf("so"),
        "タ" to listOf("ta"), "チ" to listOf("chi", "ti"), "ツ" to listOf("tsu", "tu"), "テ" to listOf("te"), "ト" to listOf("to"),
        "ナ" to listOf("na"), "ニ" to listOf("ni"), "ヌ" to listOf("nu"), "ネ" to listOf("ne"), "ノ" to listOf("no"),
        "ハ" to listOf("ha"), "ヒ" to listOf("hi"), "フ" to listOf("fu", "hu"), "ヘ" to listOf("he"), "ホ" to listOf("ho"),
        "マ" to listOf("ma"), "ミ" to listOf("mi"), "ム" to listOf("mu"), "メ" to listOf("me"), "モ" to listOf("mo"),
        "ヤ" to listOf("ya"), "ユ" to listOf("yu"), "ヨ" to listOf("yo"),
        "ラ" to listOf("ra"), "リ" to listOf("ri"), "ル" to listOf("ru"), "レ" to listOf("re"), "ロ" to listOf("ro"),
        "ワ" to listOf("wa"), "ヲ" to listOf("wo", "o"), "ン" to listOf("n", "nn"),

        "ガ" to listOf("ga"), "ギ" to listOf("gi"), "グ" to listOf("gu"), "ゲ" to listOf("ge"), "ゴ" to listOf("go"),
        "ザ" to listOf("za"), "ジ" to listOf("ji", "zi"), "ズ" to listOf("zu"), "ゼ" to listOf("ze"), "ゾ" to listOf("zo"),
        "ダ" to listOf("da"), "ヂ" to listOf("ji", "di"), "ヅ" to listOf("zu", "du"), "デ" to listOf("de"), "ド" to listOf("do"),
        "バ" to listOf("ba"), "ビ" to listOf("bi"), "ブ" to listOf("bu"), "ベ" to listOf("be"), "ボ" to listOf("bo"),
        "パ" to listOf("pa"), "ピ" to listOf("pi"), "プ" to listOf("pu"), "ペ" to listOf("pe"), "ポ" to listOf("po"),

        "ャ" to listOf("ya"), "ュ" to listOf("yu"), "ョ" to listOf("yo"), "ッ" to listOf("tsu", "tu"),

        // 片假名拗音
        "キャ" to listOf("kya"), "キュ" to listOf("kyu"), "キョ" to listOf("kyo"),
        "シャ" to listOf("sha", "sya"), "シュ" to listOf("shu", "syu"), "ショ" to listOf("sho", "syo"),
        "チャ" to listOf("cha", "tya"), "チュ" to listOf("chu", "tyu"), "チョ" to listOf("cho", "tyo"),
        "ニャ" to listOf("nya"), "ニュ" to listOf("nyu"), "ニョ" to listOf("nyo"),
        "ヒャ" to listOf("hya"), "ヒュ" to listOf("hyu"), "ヒョ" to listOf("hyo"),
        "ミャ" to listOf("mya"), "ミュ" to listOf("myu"), "ミョ" to listOf("myo"),
        "リャ" to listOf("rya"), "リュ" to listOf("ryu"), "リョ" to listOf("ryo"),
        "ギャ" to listOf("gya"), "ギュ" to listOf("gyu"), "ギョ" to listOf("gyo"),
        "ジャ" to listOf("ja", "zya", "jya"), "ジュ" to listOf("ju", "zyu", "jyu"), "ジョ" to listOf("jo", "zyo", "jyo"),
        "ビャ" to listOf("bya"), "ビュ" to listOf("byu"), "ビョ" to listOf("byo"),
        "ピャ" to listOf("pya"), "ピュ" to listOf("pyu"), "ピョ" to listOf("pyo"),

        // 长音
        "ー" to listOf("-")
    )
}
