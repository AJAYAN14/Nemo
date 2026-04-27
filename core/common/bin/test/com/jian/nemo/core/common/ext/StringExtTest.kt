package com.jian.nemo.core.common.ext

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for String extension functions
 */
class StringExtTest {

    // Hiragana tests
    @Test
    fun `isHiragana should return true for pure hiragana string`() {
        assertTrue("あいうえお".isHiragana())
        assertTrue("ひらがな".isHiragana())
        assertTrue("こんにちは".isHiragana())
    }

    @Test
    fun `isHiragana should return false for katakana`() {
        assertFalse("カタカナ".isHiragana())
    }

    @Test
    fun `isHiragana should return false for mixed characters`() {
        assertFalse("ひらがなカタカナ".isHiragana())
        assertFalse("あ漢字".isHiragana())
    }

    @Test
    fun `isHiragana should return false for romaji`() {
        assertFalse("hiragana".isHiragana())
    }

    @Test
    fun `isHiragana should return false for empty string`() {
        assertFalse("".isHiragana())
    }

    // Katakana tests
    @Test
    fun `isKatakana should return true for pure katakana string`() {
        assertTrue("アイウエオ".isKatakana())
        assertTrue("カタカナ".isKatakana())
        assertTrue("コンニチハ".isKatakana())
    }

    @Test
    fun `isKatakana should return false for hiragana`() {
        assertFalse("ひらがな".isKatakana())
    }

    @Test
    fun `isKatakana should return false for mixed characters`() {
        assertFalse("カタカナひらがな".isKatakana())
        assertFalse("カ漢字".isKatakana())
    }

    @Test
    fun `isKatakana should return false for romaji`() {
        assertFalse("katakana".isKatakana())
    }

    @Test
    fun `isKatakana should return false for empty string`() {
        assertFalse("".isKatakana())
    }

    // Kana tests
    @Test
    fun `isKana should return true for hiragana`() {
        assertTrue("ひらがな".isKana())
    }

    @Test
    fun `isKana should return true for katakana`() {
        assertTrue("カタカナ".isKana())
    }

    @Test
    fun `isKana should return false for mixed kana and kanji`() {
        assertFalse("ひらがな漢字".isKana())
    }

    @Test
    fun `isKana should return false for romaji`() {
        assertFalse("kana".isKana())
    }

    @Test
    fun `isKana should return false for empty string`() {
        assertFalse("".isKana())
    }

    // Kanji tests
    @Test
    fun `containsKanji should return true for strings with kanji`() {
        assertTrue("漢字".containsKanji())
        assertTrue("日本語".containsKanji())
        assertTrue("東京".containsKanji())
    }

    @Test
    fun `containsKanji should return true for mixed kana and kanji`() {
        assertTrue("ひらがな漢字".containsKanji())
        assertTrue("カタカナ漢字".containsKanji())
        assertTrue("こんにちは世界".containsKanji())
    }

    @Test
    fun `containsKanji should return false for pure hiragana`() {
        assertFalse("ひらがな".containsKanji())
    }

    @Test
    fun `containsKanji should return false for pure katakana`() {
        assertFalse("カタカナ".containsKanji())
    }

    @Test
    fun `containsKanji should return false for romaji`() {
        assertFalse("kanji".containsKanji())
    }

    @Test
    fun `containsKanji should return false for empty string`() {
        assertFalse("".containsKanji())
    }

    // Edge cases
    @Test
    fun `should handle single character correctly`() {
        assertTrue("あ".isHiragana())
        assertTrue("ア".isKatakana())
        assertTrue("漢".containsKanji())
    }

    @Test
    fun `should handle numbers and special characters`() {
        assertFalse("123".isHiragana())
        assertFalse("123".isKatakana())
        assertFalse("123".containsKanji())
        assertFalse("!@#".isKana())
    }
}
