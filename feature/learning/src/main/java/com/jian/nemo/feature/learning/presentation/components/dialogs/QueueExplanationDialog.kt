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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.ui.component.NemoDialog

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
    val newColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)
    val learningColor = if (isPlumpDuo) IsoluminantPalette.LakesideCyan else if (isDarkTheme) Color(0xFF22D3EE) else Color(0xFF0891B2)
    val reviewColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else if (isDarkTheme) Color(0xFFBBF7D0) else Color(0xFF166534)
    val relearnColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412)

    NemoDialog(
        onDismissRequest = onDismissRequest,
        title = "学习队列说明",
        confirmText = "我知道了",
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
                text = "系统依据记忆算法将卡片分为四个学习阶段：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            QueueExplanationItem(
                title = "新学 (NEW)",
                count = queueNewCount,
                color = newColor,
                description = "今日新增待学卡片，首次接触学习"
            )
            QueueExplanationItem(
                title = "在学 (LEARNING)",
                count = queueLearningCount,
                color = learningColor,
                description = "新词初记阶段，处于短时间步长强化中"
            )
            QueueExplanationItem(
                title = "复习 (REVIEW)",
                count = queueReviewCount,
                color = reviewColor,
                description = "已掌握卡片，依据记忆算法在今日到期巩固"
            )
            QueueExplanationItem(
                title = "重学 (RELEARN)",
                count = queueRelearnCount,
                color = relearnColor,
                description = "复习时遗忘的卡片，重新进入短时间强化"
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
