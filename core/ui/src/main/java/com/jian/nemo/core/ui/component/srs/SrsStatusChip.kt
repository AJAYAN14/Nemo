package com.jian.nemo.core.ui.component.srs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.util.SrsDateUtils
import com.kyant.backdrop.Backdrop

/**
 * SRS 记忆状态 Chip (磨砂液态玻璃紧凑型，适用于 AppBar)
 */
@Composable
fun SrsStatusChip(
    nextReviewDay: Long,
    repetitionCount: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    onClick: (() -> Unit)? = null
) {
    val isNew = repetitionCount == 0
    val isOverdue = !isNew && SrsDateUtils.isOverdue(nextReviewDay)
    
    val text = if (isNew) "未学习" else SrsDateUtils.formatNextReviewDate(nextReviewDay)

    val containerColor = when {
        isNew -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    }

    val contentColor = when {
        isNew -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val icon = if (isNew) Icons.Rounded.RadioButtonUnchecked else Icons.Rounded.Schedule

    FrostedLiquidGlassButton(
        onClick = { onClick?.invoke() },
        backdrop = backdrop,
        surfaceColor = containerColor,
        shape = RoundedCornerShape(50),
        isInteractive = true,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = modifier.height(30.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.sp
                ),
                color = contentColor
            )
        }
    }
}
