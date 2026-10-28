package com.jian.nemo.feature.learning.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.jian.nemo.feature.learning.presentation.LearningMode

/**
 * 今日学习概览面板弹窗（点击顶部剩余胶囊触发）
 */
@Composable
fun LearningOverviewDialog(
    onDismissRequest: () -> Unit,
    learningMode: LearningMode,
    completedCount: Int,
    dailyGoal: Int,
    remainingCount: Int,
    queueNewCount: Int,
    queueLearningCount: Int,
    queueReviewCount: Int,
    queueRelearnCount: Int,
    isDarkTheme: Boolean,
    isPlumpDuo: Boolean
) {
    val modeText = if (learningMode == LearningMode.Word) "单词" else "语法"
    val percent = if (dailyGoal > 0) ((completedCount.toFloat() / dailyGoal) * 100).toInt() else 0
    val estMinutes = ((remainingCount * 15) / 60).coerceAtLeast(1)

    val newColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)
    val learningColor = if (isPlumpDuo) IsoluminantPalette.LakesideCyan else if (isDarkTheme) Color(0xFF22D3EE) else Color(0xFF0891B2)
    val reviewColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else if (isDarkTheme) Color(0xFFBBF7D0) else Color(0xFF166534)
    val relearnColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412)

    NemoDialog(
        onDismissRequest = onDismissRequest,
        title = "今日学习概览",
        confirmText = "继续学习",
        dismissText = null,
        onConfirm = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 今日进度卡片
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "今日目标完成度",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedCount / $dailyGoal $modeText",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 待学任务与预估耗时卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "待处理剩余",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$remainingCount $modeText",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "预估剩余耗时",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (remainingCount > 0) "约 $estMinutes 分钟" else "已全部完成",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 队列卡片分布
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "队列卡片分布",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "新学: $queueNewCount", fontSize = 12.sp, color = newColor, fontWeight = FontWeight.SemiBold)
                    Text(text = "在学: $queueLearningCount", fontSize = 12.sp, color = learningColor, fontWeight = FontWeight.SemiBold)
                    Text(text = "复习: $queueReviewCount", fontSize = 12.sp, color = reviewColor, fontWeight = FontWeight.SemiBold)
                    Text(text = "重学: $queueRelearnCount", fontSize = 12.sp, color = relearnColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
