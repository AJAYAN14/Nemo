package com.jian.nemo.core.ui.component.liquid.menu

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    @Test
    fun testLiquidMenuItemBoundsKeepOpen() {
        var clicked = false
        val itemDefault = LiquidMenuItemBounds(
            id = "item_default",
            boundsInMenu = Rect(0f, 0f, 100f, 40f),
            onSelect = { clicked = true }
        )
        assertFalse(itemDefault.keepOpen)

        val itemKeepOpen = LiquidMenuItemBounds(
            id = "item_keep_open",
            boundsInMenu = Rect(0f, 40f, 100f, 80f),
            keepOpen = true,
            onSelect = { clicked = true }
        )
        assertTrue(itemKeepOpen.keepOpen)
    }

    @Test
    fun testSurfaceStateLightUniformMapping() {
        val state = com.jian.nemo.core.ui.component.liquid.menu.render.GlassSurfaceState()
        assertEquals(0f, state.pressX, 0.001f)
        assertEquals(0f, state.pressY, 0.001f)
        assertEquals(0f, state.poolGain, 0.001f)
        assertEquals(0f, state.pressLift, 0.001f)

        state.pressX = 120f
        state.pressY = 340f
        state.pressRadius = 80f * 2.5f
        state.poolGain = 0.03f
        state.pressLift = 0.018f
        state.rimLift = 0.12f

        assertEquals(120f, state.pressX, 0.001f)
        assertEquals(340f, state.pressY, 0.001f)
        assertEquals(200f, state.pressRadius, 0.001f)
        assertEquals(0.03f, state.poolGain, 0.0001f)
        assertEquals(0.018f, state.pressLift, 0.0001f)
        assertEquals(0.12f, state.rimLift, 0.0001f)
    }

    @Test
    fun testGlassJellyAdvanceAndSettle() {
        val jelly = com.jian.nemo.core.ui.component.liquid.menu.motion.GlassJelly()
        jelly.down(100f, 200f)
        assertFalse(jelly.isSettled)
        jelly.move(120f, 220f)
        assertFalse(jelly.isSettled)

        jelly.release()
        for (i in 0 until 80) {
            jelly.advance(0.016f)
        }
        assertTrue(jelly.isSettled)
        assertEquals(0f, jelly.glowValue, 0.01f)
    }

    @Test
    fun testLiquidMenuItemDynamicClosureUpdate() {
        var counter = 0
        var currentKeepOpen = false
        var currentEnabled = true

        val registered = mutableMapOf<Any, LiquidMenuItemBounds>()
        val id = "dynamic_item"

        registered[id] = LiquidMenuItemBounds(
            id = id,
            boundsInMenu = Rect(0f, 0f, 200f, 48f),
            isEnabled = currentEnabled,
            keepOpen = currentKeepOpen,
            onSelect = { counter += 1 }
        )

        registered[id]?.onSelect?.invoke()
        assertEquals(1, counter)

        currentKeepOpen = true
        currentEnabled = false
        val existing = registered[id]!!
        registered[id] = existing.copy(
            isEnabled = currentEnabled,
            keepOpen = currentKeepOpen,
            onSelect = { counter += 10 }
        )

        val updated = registered[id]!!
        assertFalse(updated.isEnabled)
        assertTrue(updated.keepOpen)
        updated.onSelect()
        assertEquals(11, counter)
    }

    @Test
    fun testDragReleaseSelectionContract() {
        var isDismissed = false
        var isMenuClosed = false
        val onSelectDismiss = {
            isDismissed = true
            isMenuClosed = true
        }

        var itemClicked = false
        val itemNormal = LiquidMenuItemBounds(
            id = "normal",
            boundsInMenu = Rect.Zero,
            keepOpen = false,
            onSelect = { itemClicked = true }
        )

        itemNormal.onSelect()
        if (!itemNormal.keepOpen) {
            onSelectDismiss()
        }
        assertTrue(itemClicked)
        assertTrue(isDismissed)
        assertTrue(isMenuClosed)

        itemClicked = false
        isDismissed = false
        isMenuClosed = false
        val itemKeepOpen = LiquidMenuItemBounds(
            id = "keep_open",
            boundsInMenu = Rect.Zero,
            keepOpen = true,
            onSelect = { itemClicked = true }
        )

        itemKeepOpen.onSelect()
        if (!itemKeepOpen.keepOpen) {
            onSelectDismiss()
        }
        assertTrue(itemClicked)
        assertFalse(isDismissed)
        assertFalse(isMenuClosed)

        isDismissed = false
        val dismissIfNoSelection = false
        if (dismissIfNoSelection) {
            onSelectDismiss()
        }
        assertFalse(isDismissed)
    }

    @Test
    fun testSessionGeometryDownwardOpenAlignsWithSourceTop() {
        val density = 3f
        val windowWidth = 1080f
        val windowHeight = 2400f
        val statusBarTop = 100f

        val btnSize = 48f * density
        val btnRight = windowWidth - 16f * density
        val btnLeft = btnRight - btnSize
        val btnTop = statusBarTop + 10f * density
        val btnBottom = btnTop + btnSize

        val geometry = calculateLiquidMenuGeometry(
            anchorLeft = btnLeft,
            anchorTop = btnTop,
            anchorRight = btnRight,
            anchorBottom = btnBottom,
            menuWidth = 260f * density,
            menuHeight = 320f * density,
            windowWidth = windowWidth,
            windowHeight = windowHeight,
            statusBarTopPx = statusBarTop,
            density = density
        )

        // 验证关键几何对齐：向下展开时，菜单顶部必须严格与按钮顶部重合，垂直偏移为 0
        assertEquals(btnTop, geometry.top, 0.001f)

        // 验证靠右对齐：菜单右边缘必须严格与按钮右边缘重合，水平偏移为 0
        assertEquals(btnRight, geometry.left + geometry.width, 0.001f)

        // 验证展开极性
        assertFalse(geometry.opensAbove)
        assertTrue(geometry.opensFromRight)
    }

    @Test
    fun testSessionGeometryUpwardOpenWhenRoomBelowInsufficient() {
        val density = 3f
        val windowWidth = 1080f
        val windowHeight = 2400f
        val statusBarTop = 100f

        val btnSize = 48f * density
        val btnRight = windowWidth - 20f * density
        val btnLeft = btnRight - btnSize
        val btnTop = windowHeight - 120f * density
        val btnBottom = btnTop + btnSize

        val geometry = calculateLiquidMenuGeometry(
            anchorLeft = btnLeft,
            anchorTop = btnTop,
            anchorRight = btnRight,
            anchorBottom = btnBottom,
            menuWidth = 260f * density,
            menuHeight = 350f * density,
            windowWidth = windowWidth,
            windowHeight = windowHeight,
            statusBarTopPx = statusBarTop,
            density = density
        )

        // 验证智能向上翻转被触发
        assertTrue(geometry.opensAbove)
        // 向上展开时，菜单底边缘必须锁定对齐按钮底边缘
        assertEquals(btnBottom, geometry.top + geometry.height, 0.001f)
    }

    @Test
    fun testMaximumHeightConstraint() {
        val density = 3f
        val windowWidth = 1080f
        val windowHeight = 1200f // 矮屏幕/横屏环境
        val statusBarTop = 80f

        val geometry = calculateLiquidMenuGeometry(
            anchorLeft = 800f,
            anchorTop = 100f,
            anchorRight = 950f,
            anchorBottom = 250f,
            menuWidth = 260f * density,
            menuHeight = 2000f, // 尝试打开超高菜单（2000px，远超屏幕高度）
            windowWidth = windowWidth,
            windowHeight = windowHeight,
            statusBarTopPx = statusBarTop,
            density = density
        )

        assertTrue(geometry.maximumHeightPx > 0f)
        // 验证目标高度受 maximumHeight 约束压制，绝不穿透屏幕
        assertTrue(geometry.height <= geometry.maximumHeightPx)
        assertTrue(geometry.top + geometry.height <= windowHeight)
    }

    @Test
    fun testSingleEdgeLockingOnDynamicHeightUpdate() {
        val density = 3f
        val windowHeight = 2400f
        val statusBarTop = 100f
        val anchorTop = 200f
        val anchorBottom = 350f

        val initialGeometry = calculateLiquidMenuGeometry(
            anchorLeft = 800f,
            anchorTop = anchorTop,
            anchorRight = 950f,
            anchorBottom = anchorBottom,
            menuWidth = 260f * density,
            menuHeight = 300f * density,
            windowWidth = 1080f,
            windowHeight = windowHeight,
            statusBarTopPx = statusBarTop,
            density = density
        )

        assertEquals(anchorTop, initialGeometry.top, 0.001f)
        assertFalse(initialGeometry.opensAbove)

        // 模拟内容二阶段测量高度增加（例如从 300dp 变到 420dp）
        val newHeightPx = 420f * density
        val (newTop, constrainedHeight) = calculateUpdatedContentHeight(
            newHeightPx = newHeightPx,
            maximumHeightPx = initialGeometry.maximumHeightPx,
            anchorTop = anchorTop,
            anchorBottom = anchorBottom,
            opensAbove = initialGeometry.opensAbove,
            statusBarTopPx = statusBarTop,
            windowHeight = windowHeight,
            density = density
        )

        // 向下展开模式下：必须严格锁定顶边缘不变，高度只向下延伸
        assertEquals(anchorTop, newTop, 0.001f)
        assertEquals(newHeightPx, constrainedHeight, 0.001f)
    }
}
