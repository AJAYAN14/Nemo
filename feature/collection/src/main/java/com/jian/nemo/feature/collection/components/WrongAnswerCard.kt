package com.jian.nemo.feature.collection.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.WrongAnswer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 错题卡片组件
 *
 * 用于错题本中显示错题信息
 *
 * @param wrongAnswer 错题数据
 * @param onDelete 删除回调
 */
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow

@Composable
fun WrongAnswerCard(
    wrongAnswer: WrongAnswer,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 12.dp, isDark = isDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 错题内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 单词信息（如果有）
                wrongAnswer.word?.let { word ->
                    Text(
                        text = "${word.japanese} (${word.hiragana})",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = word.chinese,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 测试模式
                Text(
                    text = "测试模式: ${getTestModeName(wrongAnswer.testMode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 错误答案
                Text(
                    text = "你的答案: ${wrongAnswer.userAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                // 正确答案
                Text(
                    text = "正确答案: ${wrongAnswer.correctAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // 时间戳
                Text(
                    text = formatTimestamp(wrongAnswer.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除错题",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 获取测试模式名称
 */
private fun getTestModeName(testMode: String): String {
    return when (testMode) {
        "multiple_choice" -> "选择题"
        "typing" -> "打字练习"
        "matching" -> "卡片匹配"
        "sorting" -> "排序题"
        else -> testMode
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
