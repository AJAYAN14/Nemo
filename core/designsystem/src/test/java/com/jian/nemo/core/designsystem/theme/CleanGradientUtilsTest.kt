package com.jian.nemo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CleanGradientUtilsTest {

    @Test
    fun `complementary blue to gold inserts smooth hue bridges without gray zone`() {
        val stops = CleanGradientUtils.buildCleanSegmentedGradientStops(
            segments = listOf(
                10f to BentoColors.AccentGreen,
                10f to BentoColors.AccentBlue,
                10f to BentoColors.AccentOrange
            )
        )
        assertTrue("Expected bridge stops for blue-to-orange transition", stops.size >= 8)
        assertStopsClean(stops)
    }

    @Test
    fun `four stability buckets transition cleanly without crossing gray zone`() {
        val stops = CleanGradientUtils.buildCleanSegmentedGradientStops(
            segments = listOf(
                25f to StabilityEarly,
                25f to StabilityDeveloping,
                25f to StabilityMature,
                25f to StabilityExpert
            )
        )
        assertTrue("Expected bridge stops across 4 buckets", stops.size >= 10)
        assertStopsClean(stops)
    }

    private fun assertStopsClean(stops: Array<Pair<Float, Color>>) {
        for (i in 0 until stops.size - 1) {
            val (p1, c1) = stops[i]
            val (p2, c2) = stops[i + 1]
            assertTrue("Stop positions must be strictly increasing: $p1 -> $p2", p2 > p1)

            val hsv1 = CleanGradientUtils.rgbToHsv(c1)
            val hsv2 = CleanGradientUtils.rgbToHsv(c2)
            val deltaH = abs(CleanGradientUtils.shortestSignedHueDelta(hsv1[0], hsv2[0]))
            assertTrue(
                "Adjacent stops should not jump across color wheel (deltaH=$deltaH at index $i)",
                deltaH <= 46f
            )
        }
    }
}
