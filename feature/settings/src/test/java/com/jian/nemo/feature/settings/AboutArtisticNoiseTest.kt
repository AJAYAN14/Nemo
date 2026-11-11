package com.jian.nemo.feature.settings

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutArtisticNoiseTest {

    @Test
    fun `非正数或非有限尺寸参数返回空点集`() {
        val invalidCases = listOf(
            buildArtisticNoisePoints(0f, 400f),
            buildArtisticNoisePoints(400f, 0f),
            buildArtisticNoisePoints(-100f, 400f),
            buildArtisticNoisePoints(400f, 400f, step = 0f),
            buildArtisticNoisePoints(400f, 400f, step = -4f),
            buildArtisticNoisePoints(Float.NaN, 400f),
            buildArtisticNoisePoints(400f, Float.POSITIVE_INFINITY),
            buildArtisticNoisePoints(400f, 400f, step = Float.NaN)
        )

        invalidCases.forEach { points ->
            assertEquals(0, points.smallPoints.size)
            assertEquals(0, points.largePoints.size)
        }
    }

    @Test
    fun `有效尺寸生成成对合法坐标点且小点与大点均有分布`() {
        val width = 816f
        val height = 432f
        val points = buildArtisticNoisePoints(width = width, height = height, step = 12f)

        assertTrue(points.smallPoints.isNotEmpty())
        assertTrue(points.largePoints.isNotEmpty())
        assertEquals(0, points.smallPoints.size % 2)
        assertEquals(0, points.largePoints.size % 2)

        fun assertValidPointPairs(coords: FloatArray) {
            for (i in coords.indices step 2) {
                val x = coords[i]
                val y = coords[i + 1]
                assertTrue(x.isFinite() && x in -3f..(width + 3f))
                assertTrue(y.isFinite() && y in -3f..(height + 3f))
            }
        }

        assertValidPointPairs(points.smallPoints)
        assertValidPointPairs(points.largePoints)
    }

    @Test
    fun `同参数调用生成完全确定的点阵`() {
        val first = buildArtisticNoisePoints(width = 640f, height = 360f, step = 12f)
        val second = buildArtisticNoisePoints(width = 640f, height = 360f, step = 12f)

        assertArrayEquals(first.smallPoints, second.smallPoints, 0f)
        assertArrayEquals(first.largePoints, second.largePoints, 0f)
    }

    @Test
    fun `自定义步长按比例缩放偏移且极端微步长防护溢出`() {
        val scaled = buildArtisticNoisePoints(width = 480f, height = 360f, step = 6f)
        assertTrue(scaled.smallPoints.isNotEmpty())
        assertTrue(scaled.largePoints.isNotEmpty())
        for (i in scaled.smallPoints.indices step 2) {
            val x = scaled.smallPoints[i]
            val y = scaled.smallPoints[i + 1]
            assertTrue(x in -1.5f..(480f + 1.5f))
            assertTrue(y in -1.5f..(360f + 1.5f))
        }

        val overflowGuarded = buildArtisticNoisePoints(width = 2048f, height = 2048f, step = 0.001f)
        assertEquals(0, overflowGuarded.smallPoints.size)
        assertEquals(0, overflowGuarded.largePoints.size)
    }
}
