package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.feature.test.presentation.model.OptionStatus
import androidx.compose.foundation.BorderStroke

/**
 * 选择题测试内容组件
 * Safe version
 */
@Composable
fun MultipleChoiceTestContent(
    question: TestQuestion.MultipleChoice,
    selectedOptionIndex: Int,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Question Text
        com.jian.nemo.core.ui.component.text.FuriganaText(
            text = question.questionText,
            baseTextStyle = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                lineHeight = 32.sp
            ),
            baseTextColor = MaterialTheme.colorScheme.onBackground,
            furiganaTextSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        // Option List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Already Answered Warning
            if (question.isAnswered) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant, // Safe color
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = stringResource(R.string.test_choice_status_answered_locked),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Render Options
            question.options.forEachIndexed { index, option ->
                val status = calculateOptionStatus(
                    question = question,
                    option = option,
                    index = index,
                    selectedOptionIndex = selectedOptionIndex
                )

                TestOption(
                    index = index,
                    text = option,
                    status = status,
                    onClick = { onOptionSelect(index) }
                )
            }
        }

        // Explanation Card
        val explanationPayload = question.resolvedExplanationPayload
        if (question.isAnswered && explanationPayload != null) {
            Spacer(modifier = Modifier.height(32.dp))
            QuestionExplanationCard(payload = explanationPayload, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * 计算选项状态
 */
private fun calculateOptionStatus(
    question: TestQuestion.MultipleChoice,
    option: String,
    index: Int,
    selectedOptionIndex: Int
): OptionStatus {
    val isSelected = selectedOptionIndex == index

    return if (question.isAnswered) {
        // 已回答状态
        val isCorrectAnswer = (option == question.correctAnswer)
        val wasUserChoice = question.userAnswerIndex?.let { it == index }
            ?: (selectedOptionIndex == index && selectedOptionIndex >= 0)

        when {
            isCorrectAnswer -> OptionStatus.CORRECT           // 正确答案显示绿色
            wasUserChoice && !question.isCorrect -> OptionStatus.INCORRECT  // 用户选错显示红色
            else -> OptionStatus.DEFAULT                      // 其他选项默认样式
        }
    } else {
        // 未回答状态
        if (isSelected) {
            OptionStatus.SELECTED                             // 选中项显示蓝色
        } else {
            OptionStatus.DEFAULT                              // 未选中默认样式
        }
    }
}
