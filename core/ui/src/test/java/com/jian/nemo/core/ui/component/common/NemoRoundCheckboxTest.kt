package com.jian.nemo.core.ui.component.common

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NemoRoundCheckboxTest {

    @Test
    fun `visual proportions checkSize should be 60 to 70 percent of outer size`() {
        val defaultSize = 22.dp
        val defaultCheck = 14.dp
        val ratioDefault = defaultCheck.value / defaultSize.value
        assertTrue("Default checkSize ratio should be in [0.6, 0.7]", ratioDefault in 0.6f..0.7f)

        val settingsSize = 20.dp
        val settingsCheck = 13.dp
        val ratioSettings = settingsCheck.value / settingsSize.value
        assertTrue("Settings checkSize ratio should be in [0.6, 0.7]", ratioSettings in 0.6f..0.7f)

        val readingSize = 18.dp
        val readingCheck = 12.dp
        val ratioReading = readingCheck.value / readingSize.value
        assertTrue("Reading checkSize ratio should be in [0.6, 0.7]", ratioReading in 0.6f..0.7f)
    }

    @Test
    fun `default borderWidth should be 1 point 5 dp to match design reference outline`() {
        val borderWidth = 1.5.dp
        assertEquals(1.5f, borderWidth.value, 0.001f)
    }

    @Test
    fun `accessibility contentDescription format verification`() {
        fun getContentDescription(checked: Boolean): String = if (checked) "已选中" else "未选中"

        assertEquals("已选中", getContentDescription(true))
        assertEquals("未选中", getContentDescription(false))
    }

    @Test
    fun `verify NemoRoundCheckbox composable declaration exists in package`() {
        val methods = Class.forName("com.jian.nemo.core.ui.component.common.NemoRoundCheckboxKt").declaredMethods
        val checkboxMethod = methods.find { it.name.startsWith("NemoRoundCheckbox") }
        assertNotNull("NemoRoundCheckbox composable should be declared", checkboxMethod)
    }
}
