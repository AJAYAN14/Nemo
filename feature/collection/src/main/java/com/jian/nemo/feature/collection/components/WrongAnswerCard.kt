package com.jian.nemo.feature.collection.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.collection.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.WrongAnswer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.shape.RoundedRectangle

@Composable
fun WrongAnswerCard(
    wrongAnswer: WrongAnswer,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val wrongColor = SystemColors.Red
    val correctColor = SystemColors.Green

    Card(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 16.dp, isDark = isDark),
        shape = RoundedRectangle(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = stringResource(R.string.collection_test_mode_format, getTestModeName(wrongAnswer.testMode)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 错误答案
                Text(
                    text = stringResource(R.string.collection_your_answer_format, wrongAnswer.userAnswer),
                    style = MaterialTheme.typography.bodySmall,
                    color = wrongColor
                )

                // 正确答案
                Text(
                    text = stringResource(R.string.collection_correct_answer_format, wrongAnswer.correctAnswer),
                    style = MaterialTheme.typography.bodySmall,
                    color = correctColor
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
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.collection_cd_delete_mistake),
                    tint = wrongColor
                )
            }
        }
    }
}

/**
 * 获取测试模式名称
 */
@Composable
private fun getTestModeName(testMode: String): String {
    return when (testMode) {
        "multiple_choice" -> stringResource(R.string.collection_mode_multiple_choice)
        "typing" -> stringResource(R.string.collection_mode_typing)
        "matching" -> stringResource(R.string.collection_mode_matching)
        "sorting" -> stringResource(R.string.collection_mode_sorting)
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
