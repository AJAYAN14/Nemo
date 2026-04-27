package com.jian.nemo.core.domain.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsFuzzTest {

    private val fsrs = FsrsAlgorithm()

    @Test
    fun `fuzz should be deterministic for same seed`() {
        val base = 30
        val seed = 123456L

        val a = fsrs.fuzzIntervalDays(base, seed)
        val b = fsrs.fuzzIntervalDays(base, seed)

        assertEquals(a, b)
    }

    @Test
    fun `fuzz should keep interval in reasonable range`() {
        val base = 60
        val seed = 42L

        val fuzzed = fsrs.fuzzIntervalDays(base, seed)

        assertTrue(fuzzed in 1..FsrsAlgorithm.MAX_INTERVAL)
        assertTrue(fuzzed in 51..69) // span ~= 60 * 0.12 = 7
    }

    @Test
    fun `small intervals should not be fuzzed`() {
        assertEquals(1, fsrs.fuzzIntervalDays(1, 1L))
        assertEquals(2, fsrs.fuzzIntervalDays(2, 2L))
    }
}
