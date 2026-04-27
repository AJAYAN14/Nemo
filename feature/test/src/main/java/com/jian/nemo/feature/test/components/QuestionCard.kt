package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.TestMode

/**
 * 题目卡片组件
 * 显示测试题目
 */
@Composable
fun QuestionCard(
    questionText: String,
    mode: TestMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 题目类型提示
                Text(
                    text = getModeHint(mode),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 题目文本
                Text(
                    text = questionText,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getModeHint(mode: TestMode): String {
    return when (mode) {
        TestMode.JP_TO_CN -> "请选择对应的中文释义"
        TestMode.CN_TO_JP -> "请选择对应的日语单词"
        TestMode.KANA -> "请选择正确的假名"
        TestMode.EXAMPLE -> "请选择例句的意思"
        TestMode.RANDOM -> "请选择正确答案"
    }
}
