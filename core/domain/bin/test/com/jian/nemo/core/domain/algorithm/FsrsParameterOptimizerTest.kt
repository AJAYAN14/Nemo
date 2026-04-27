package com.jian.nemo.core.domain.algorithm

import com.jian.nemo.core.domain.model.ReviewLog
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsParameterOptimizerTest {

    @Test
    fun `should return null when logs are insufficient`() {
        val smallLogs = List(100) {
            ReviewLog(
                id = it.toLong(),
                itemId = it,
                itemType = "word",
                reviewDate = 1_000_000L + it,
                intervalDays = 3,
                rating = 4
            )
        }

        val tuned = FsrsParameterOptimizer.optimize(smallLogs)
        assertNull(tuned)
    }

    @Test
    fun `should return tuned parameters when logs are enough`() {
        val logs = List(500) { index ->
            val rating = when {
                index % 5 == 0 -> 2 // Again-heavy
                index % 3 == 0 -> 3 // Hard
                else -> 4
            }
            ReviewLog(
                id = index.toLong(),
                itemId = index,
                itemType = "word",
                reviewDate = 2_000_000L + index,
                intervalDays = 5,
                rating = rating
            )
        }

        val tuned = FsrsParameterOptimizer.optimize(logs)

        assertNotNull(tuned)
        assertTrue(tuned!!.parameters.size == FsrsAlgorithm.DEFAULT_PARAMETERS.size)
        assertTrue(tuned.parameters[11] > 0f)
        assertTrue(tuned.parameters[16] > 1f)
        assertTrue(tuned.sampleSize == 500)
    }
}
