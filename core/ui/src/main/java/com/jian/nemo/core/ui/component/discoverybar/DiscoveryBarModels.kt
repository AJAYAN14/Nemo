package com.jian.nemo.core.ui.component.discoverybar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.UiPalette

import com.jian.nemo.core.designsystem.icon.PlumpIcon

/**
 * A model representing an option in the [DiscoveryBar].
 *
 * @param label The text label for this option.
 * @param icon The icon displayed next to the label. Set to null to show text only.
 * @param plumpIcon The Plump Duo icon displayed next to the label in Plump Duo mode.
 * @param activeColor The color used when this option is active.
 */
data class DiscoveryOption(
    val label: String,
    val icon: ImageVector? = null,
    val plumpIcon: PlumpIcon? = null,
    val activeColor: Color = NemoDanger,
)

/**
 * Style configuration for the [DiscoveryBar].
 *
 * Provides default premium values matching the Flutter DiscoveryBarStyle.
 */
data class DiscoveryBarStyle(
    /** The background color of the bar components. */
    val backgroundColor: Color = Color.Unspecified,
    /** The color of the items when they are not selected. */
    val inactiveColor: Color = UiPalette.DialogDarkBg,
    /** The color of the active indicator card. */
    val indicatorColor: Color = NemoDanger,
    /** The height of the discovery bar. */
    val height: Dp = 56.dp,
    /** The duration of the expansion/contraction animation in milliseconds. */
    val animationDurationMillis: Int = 800,
    /**
     * Spring stiffness for the main morphing animation.
     * Corresponds to Flutter PortalSpringCurve(stiffness=160, damping=17).
     */
    val mainSpringStiffness: Float = 160f,
    /**
     * Spring damping ratio for the main morphing animation.
     * Calculated from Flutter: dampingRatio = damping / (2 * sqrt(stiffness * mass))
     * = 17 / (2 * sqrt(160 * 1)) = 17 / 25.298 ≈ 0.672
     */
    val mainSpringDampingRatio: Float = 0.672f,
    /**
     * Spring stiffness for the option selector sliding indicator.
     * Corresponds to Flutter PortalSpringCurve(stiffness=200, damping=15).
     */
    val selectorSpringStiffness: Float = 200f,
    /**
     * Spring damping ratio for the option selector sliding indicator.
     * = 15 / (2 * sqrt(200 * 1)) = 15 / 28.284 ≈ 0.530
     */
    val selectorSpringDampingRatio: Float = 0.530f,
    /** The text style for inactive options. */
    val textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.W500,
        color = UiPalette.DialogDarkBg,
    ),
    /** The text style for the active option. */
    val activeTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.W700,
        color = Color.Black,
    ),
    /** The size of the icons in the discovery list. */
    val iconSize: Dp = 20.dp,
    /** The size of the search/close icon. */
    val searchIconSize: Dp = 24.dp,
    /** Whether haptic feedback is enabled. */
    val enableHaptics: Boolean = true,
)
