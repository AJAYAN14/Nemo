package com.jian.nemo.feature.learning.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.feature.learning.R

/**
 * 学习队列状态说明弹窗
 */
@Composable
fun QueueExplanationDialog(
    onDismissRequest: () -> Unit,
    queueNewCount: Int,
    queueLearningCount: Int,
    queueReviewCount: Int,
    queueRelearnCount: Int,
    isDarkTheme: Boolean,
    isPlumpDuo: Boolean
) {
    val newColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else if (isDarkTheme) LearningPalette.BadgeNewTextDark else LearningPalette.BadgeNewTextLight
    val learningColor = if (isPlumpDuo) IsoluminantPalette.LakesideCyan else if (isDarkTheme) LearningPalette.BadgeLearningTextDark else LearningPalette.BadgeLearningTextLight
    val reviewColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else if (isDarkTheme) LearningPalette.BadgeReviewTextDark else LearningPalette.BadgeReviewTextLight
    val relearnColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else if (isDarkTheme) LearningPalette.BadgeRelearnTextDark else LearningPalette.BadgeRelearnTextLight

    NemoDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.title_queue_explanation),
        confirmText = stringResource(R.string.btn_i_know),
        dismissText = null,
        onConfirm = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.desc_queue_explanation_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            QueueExplanationItem(
                title = stringResource(R.string.queue_stage_new_title),
                count = queueNewCount,
                color = newColor,
                description = stringResource(R.string.queue_stage_new_desc)
            )
            QueueExplanationItem(
                title = stringResource(R.string.queue_stage_learning_title),
                count = queueLearningCount,
                color = learningColor,
                description = stringResource(R.string.queue_stage_learning_desc)
            )
            QueueExplanationItem(
                title = stringResource(R.string.queue_stage_review_title),
                count = queueReviewCount,
                color = reviewColor,
                description = stringResource(R.string.queue_stage_review_desc)
            )
            QueueExplanationItem(
                title = stringResource(R.string.queue_stage_relearn_title),
                count = queueRelearnCount,
                color = relearnColor,
                description = stringResource(R.string.queue_stage_relearn_desc)
            )
        }
    }
}

@Composable
private fun QueueExplanationItem(
    title: String,
    count: Int,
    color: Color,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
