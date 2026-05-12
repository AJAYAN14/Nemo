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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.util.SrsDateUtils

/**
 * SRS 记忆状态 Chip (紧凑型，适用于 AppBar)
 */
@Composable
fun SrsStatusChip(
    nextReviewDay: Long,
    repetitionCount: Int,
    modifier: Modifier = Modifier
) {
    val isNew = repetitionCount == 0
    val isOverdue = !isNew && SrsDateUtils.isOverdue(nextReviewDay)
    
    val text = if (isNew) "未学习" else SrsDateUtils.formatNextReviewDate(nextReviewDay)

    val containerColor = when {
        isNew -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    }

    val contentColor = when {
        isNew -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val icon = if (isNew) Icons.Rounded.RadioButtonUnchecked else Icons.Rounded.Schedule

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.sp
                ),
                color = contentColor
            )
        }
    }
}
