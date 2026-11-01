package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
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

@Composable
private fun getModeHint(mode: TestMode): String {
    return when (mode) {
        TestMode.JP_TO_CN -> stringResource(R.string.test_question_hint_jp_to_cn)
        TestMode.CN_TO_JP -> stringResource(R.string.test_question_hint_cn_to_jp)
        TestMode.KANA -> stringResource(R.string.test_question_hint_kana)
        TestMode.EXAMPLE -> stringResource(R.string.test_question_hint_example)
        TestMode.RANDOM -> stringResource(R.string.test_question_hint_random)
    }
}
