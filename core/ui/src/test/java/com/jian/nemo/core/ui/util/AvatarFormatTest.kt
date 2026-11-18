package com.jian.nemo.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarFormatTest {

    @Test
    fun formatMegabytesCeil_handlesExactAndFractionalOverflow() {
        val mb = 1024L * 1024L
        assertEquals("0", formatMegabytesCeil(0L))
        assertEquals("5", formatMegabytesCeil(5L * mb))
        assertEquals("20", formatMegabytesCeil(20L * mb))
        assertEquals("20.1", formatMegabytesCeil(20L * mb + 8 * 1024L))
        assertEquals("20.5", formatMegabytesCeil(20L * mb + 512L * 1024L))
    }
}
