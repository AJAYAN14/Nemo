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
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.util.SrsDateUtils

/**
 * SRS 记忆状态 Chip (液态紧凑型，适用于 AppBar)
 */
@Composable
fun SrsStatusChip(
    nextReviewDay: Long,
    repetitionCount: Int,
    modifier: Modifier = Modifier,
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

    LiquidButton(
        onClick = { onClick?.invoke() },
        backgroundColor = containerColor,
        shape = RoundedCornerShape(50),
        elevation = 2.dp,
        isInteractive = true,
        modifier = modifier.height(30.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
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
