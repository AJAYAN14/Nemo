package com.jian.nemo.core.common.ext

/**
 * Checks if the string contains only Hiragana characters
 *
 * Hiragana Unicode range: U+3040 to U+309F
 *
 * @return true if all characters are Hiragana, false otherwise
 */
fun String.isHiragana(): Boolean {
    if (isEmpty()) return false
    return all { it in '\u3040'..'\u309F' }
}

/**
 * Checks if the string contains only Katakana characters
 *
 * Katakana Unicode range: U+30A0 to U+30FF
 *
 * @return true if all characters are Katakana, false otherwise
 */
fun String.isKatakana(): Boolean {
    if (isEmpty()) return false
    return all { it in '\u30A0'..'\u30FF' }
}

/**
 * Checks if the string contains only Kana characters (Hiragana or Katakana)
 *
 * @return true if all characters are Hiragana or Katakana, false otherwise
 */
fun String.isKana(): Boolean {
    return isHiragana() || isKatakana()
}

/**
 * Checks if the string contains any Kanji (Chinese characters)
 *
 * Kanji Unicode range: U+4E00 to U+9FFF (CJK Unified Ideographs)
 *
 * @return true if the string contains at least one Kanji character
 */
fun String.containsKanji(): Boolean {
    return any { it in '\u4E00'..'\u9FFF' }
}
