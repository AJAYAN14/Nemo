package com.jian.nemo.feature.library.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryListComponentsLogicTest {

    @Test
    fun `computeAllLevelsCount sums level counts without double counting ALL key`() {
        val countsWithAll = mapOf(
            "ALL" to 15,
            "N5" to 5,
            "N4" to 4,
            "N3" to 3,
            "N2" to 2,
            "N1" to 1
        )
        assertEquals(15, computeAllLevelsCount(countsWithAll))
        assertEquals(15, computeAllLevelsCount(mapOf("N5" to 5, "N4" to 4, "N3" to 3, "N2" to 2, "N1" to 1)))
        assertEquals(0, computeAllLevelsCount(emptyMap()))
    }

    @Test
    fun `formatMixedCnHnString merges adjacent same-script runs into minimal spans`() {
        val mixed = formatMixedCnHnString("〜に違いない・中文説明")
        assertEquals("〜に違いない・中文説明", mixed.text)
        assertTrue(
            "Expected merged spans instead of per-character spans, got ${mixed.spanStyles.size}",
            mixed.spanStyles.size <= 5
        )

        val empty = formatMixedCnHnString("")
        assertEquals("", empty.text)
        assertTrue(empty.spanStyles.isEmpty())
    }
}
