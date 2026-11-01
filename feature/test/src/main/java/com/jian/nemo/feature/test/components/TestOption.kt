package com.jian.nemo.feature.test.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.feature.test.presentation.model.OptionStatus

/**
 * 测试选项组件
 * Safe version for compatibility
 */
@Composable
fun TestOption(
    index: Int,
    text: String,
    status: OptionStatus,
    onClick: () -> Unit
) {
    // Label Logic
    val optionLabel = if (index < 26) ('A' + index).toString() else (index + 1).toString()

    // Shape: Standard Rounded (Aligned with Pro Max style)
    val shape = RoundedRectangle(16.dp)

    // State Colors (Flat UI) - Using Safe Colors (avoiding *Container if missing)
    val containerColor = when (status) {
        OptionStatus.SELECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        OptionStatus.CORRECT -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) // Green-ish
        OptionStatus.INCORRECT -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when (status) {
        OptionStatus.SELECTED -> MaterialTheme.colorScheme.primary
        OptionStatus.CORRECT -> MaterialTheme.colorScheme.secondary // Green-ish
        OptionStatus.INCORRECT -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val contentColor = when (status) {
        OptionStatus.SELECTED -> MaterialTheme.colorScheme.primary
        OptionStatus.CORRECT -> MaterialTheme.colorScheme.secondary
        OptionStatus.INCORRECT -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Animation Scale
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && (status == OptionStatus.DEFAULT || status == OptionStatus.SELECTED)) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "optionScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(1.dp, borderColor, shape) // Flat Border
            .clip(shape)
            .clickable(
                enabled = status == OptionStatus.DEFAULT || status == OptionStatus.SELECTED,
                onClick = {
                    // Safe Haptic Feedback
                    try {
                         // LocalHapticFeedback might be available
                    } catch (e: Exception) {}
                    onClick()
                },
                interactionSource = interactionSource,
                indication = null // Disable ripple for now to avoid import issues
            ),
        color = containerColor,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label Circle (A/B/C)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        // Filled circle if active state, otherwise outlined or subtle
                        color = if (status == OptionStatus.DEFAULT) MaterialTheme.colorScheme.surfaceVariant else contentColor, // Use surfaceVariant safely
                        shape = CircleShape
                    )
                    .border(
                         // If default, add border. If filled, no border needed (or clean border).
                        width = if (status == OptionStatus.DEFAULT) 1.dp else 0.dp,
                        color = if (status == OptionStatus.DEFAULT) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (status == OptionStatus.DEFAULT) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.surface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            val hasFurigana = text.contains('[')
            val textStyle = MaterialTheme.typography.bodyLarge
            val fontWeight = if (status != OptionStatus.DEFAULT) FontWeight.SemiBold else FontWeight.Medium

            if (hasFurigana) {
                com.jian.nemo.core.ui.component.text.FuriganaText(
                    text = text,
                    modifier = Modifier.weight(1f),
                    baseTextStyle = textStyle.copy(fontWeight = fontWeight),
                    baseTextColor = contentColor,
                    furiganaTextSize = 10.sp
                )
            } else {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    style = textStyle,
                    fontWeight = fontWeight,
                    color = contentColor
                )
            }

            // Status Icon
            if (status == OptionStatus.CORRECT || status == OptionStatus.INCORRECT) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (status == OptionStatus.CORRECT) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
