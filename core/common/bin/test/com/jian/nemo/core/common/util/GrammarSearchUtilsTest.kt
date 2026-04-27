package com.jian.nemo.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarSearchUtilsTest {

    @Test
    fun `cleanRubi should remove brackets and keep kanji`() {
        assertEquals("去年", GrammarSearchUtils.cleanRubi("去[きょ]年[ねん]"))
        assertEquals("食べる", GrammarSearchUtils.cleanRubi("食[た]べる"))
        assertEquals("日本語", GrammarSearchUtils.cleanRubi("日[に]本[ほん]語[ご]"))
        assertEquals("々测试", GrammarSearchUtils.cleanRubi("々[のま]测试"))
    }

    @Test
    fun `extractKana should replace kanji with kana from brackets`() {
        assertEquals("きょねん", GrammarSearchUtils.extractKana("去[きょ]年[ねん]"))
        assertEquals("たべる", GrammarSearchUtils.extractKana("食[た]べる"))
        assertEquals("にほんご", GrammarSearchUtils.extractKana("日[に]本[ほん]語[ご]"))
    }

    @Test
    fun `isMatch should match cleaned text`() {
        val text = "去[きょ]年[ねん]"
        assertTrue(GrammarSearchUtils.isMatch(text, "去年"))
        assertTrue(GrammarSearchUtils.isMatch(text, "去"))
        assertTrue(GrammarSearchUtils.isMatch(text, "年"))
    }

    @Test
    fun `isMatch should match extracted kana`() {
        val text = "去[きょ]年[ねん]"
        assertTrue(GrammarSearchUtils.isMatch(text, "きょねん"))
        assertTrue(GrammarSearchUtils.isMatch(text, "きょ"))
        assertTrue(GrammarSearchUtils.isMatch(text, "ねん"))
    }

    @Test
    fun `isMatch should match original text with brackets`() {
        val text = "去[きょ]年[ねん]"
        assertTrue(GrammarSearchUtils.isMatch(text, "去[きょ]"))
        assertTrue(GrammarSearchUtils.isMatch(text, "[ねん]"))
    }

    @Test
    fun `isMatch should return false for non-matching queries`() {
        val text = "去[きょ]年[ねん]"
        assertFalse(GrammarSearchUtils.isMatch(text, "明年"))
        assertFalse(GrammarSearchUtils.isMatch(text, "あした"))
    }

    @Test
    fun `isMatch should be case insensitive`() {
        val text = "Apple[アップル]"
        assertTrue(GrammarSearchUtils.isMatch(text, "apple"))
        assertTrue(GrammarSearchUtils.isMatch(text, "アップル"))
    }
}
