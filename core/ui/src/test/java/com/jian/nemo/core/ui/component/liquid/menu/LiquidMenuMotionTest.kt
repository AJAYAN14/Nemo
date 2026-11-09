package com.jian.nemo.core.ui.component.liquid.menu

import androidx.compose.ui.geometry.Rect
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidMenuMotionTest {

    @Test
    fun testPlateSlideSpringSpecParameters() {
        assertEquals(0.85f, PlateSlide.dampingRatio, 0.001f)
        assertEquals(0.13f, PlateSlide.response, 0.001f)
        assertTrue(PlateSlide.omega > 0f)
        assertTrue(PlateSlide.alpha > 0f)
    }

    @Test
    fun testPlateFadeSpringSpecParameters() {
        assertEquals(1.0f, PlateFade.dampingRatio, 0.001f)
        assertEquals(0.14f, PlateFade.response, 0.001f)
    }

    @Test
    fun testGlassPlateConstants() {
        assertEquals(8f, PlateInsetDpValue, 0.001f)
        assertEquals(24f, ScrubSlackDpValue, 0.001f)
    }

    @Test
    fun testGlassSpringAdvanceAndSettle() {
        val spring = GlassSpring()
        spring.reset(0f)
        spring.target(100f, PlateSlide)

        assertFalse(spring.isSettled)
        assertEquals(0f, spring.value, 0.001f)

        // Advance 0.5s in 16ms steps
        for (i in 0 until 35) {
            spring.advance(0.016f)
        }

        assertTrue(spring.isSettled)
        assertEquals(100f, spring.value, 0.01f)
    }

    @Test
    fun testLiquidMenuItemBoundsCallback() {
        var clicked = false
        val item = LiquidMenuItemBounds(
            id = "test_item_1",
            boundsInMenu = Rect(0f, 10f, 260f, 52f),
            isEnabled = true,
            onSelect = { clicked = true }
        )

        assertEquals("test_item_1", item.id)
        assertTrue(item.isEnabled)
        assertEquals(42f, item.boundsInMenu.height, 0.001f)

        item.onSelect()
        assertTrue(clicked)
    }
}
