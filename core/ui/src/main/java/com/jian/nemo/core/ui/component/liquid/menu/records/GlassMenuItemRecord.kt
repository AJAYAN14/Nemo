package com.jian.nemo.core.ui.component.liquid.menu.records

data class GlassMenuItemRecord(
    val id: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val icon: String? = null,
    val iconPath: String? = null,
    val iconSlot: Int? = null,
    val checked: Boolean? = null,
    val destructive: Boolean = false,
    val disabled: Boolean = false,
    val keepOpen: Boolean = false,
    val separator: Boolean = false,
    val heading: Boolean = false,
    val iconColor: Int? = null
)
