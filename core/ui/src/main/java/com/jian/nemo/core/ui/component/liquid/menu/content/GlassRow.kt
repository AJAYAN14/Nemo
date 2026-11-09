package com.jian.nemo.core.ui.component.liquid.menu.content

import android.graphics.RectF
import com.jian.nemo.core.ui.component.liquid.menu.records.GlassMenuItemRecord

internal class GlassRow(val item: GlassMenuItemRecord, val index: Int) {
    val rect = RectF()
    var title: CharSequence = ""
    var subtitle: CharSequence? = null
    val isSeparator get() = item.separator
    val isHeading get() = item.heading
    val selectable get() = !item.separator && !item.heading && !item.disabled
}
