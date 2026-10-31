package com.jian.nemo.feature.test.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import com.jian.nemo.feature.test.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.ExplanationPayload
import com.jian.nemo.core.domain.model.TestQuestion

/**
 * 手打题内容组件
 * Refactored to Flat UI
 */
@Composable
fun TypingTestContent(
    question: TestQuestion.Typing,
    userInput: String,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据 questionType 确定显示内容和提示
    val (displayText, hintText) = when (question.questionType) {
        1 -> question.word.chinese to "输入对应的日语假名"
        2 -> question.word.chinese to "输入对应的日语汉字"
        3 -> question.word.hiragana to "输入对应的日语汉字"
        4 -> question.word.japanese to "输入对应的日语假名"
        5 -> question.word.hiragana to "输入对应的中文释义"
        6 -> question.word.japanese to "输入对应的中文释义"
        else -> question.word.chinese to "输入对应的日语假名"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 题目文本 (Enhanced Typography)
        Text(
            text = displayText,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 32.sp, // Larger font size
                lineHeight = 40.sp
            ),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 提示文本
        Text(
            text = hintText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 48.dp) // More spacing
        )

        // 输入框
        TypingInput(
            question = question,
            userInput = userInput,
            onInputChange = onInputChange
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 答案反馈
        TypingFeedback(question = question)

        if (question.isAnswered) {
            Spacer(modifier = Modifier.height(24.dp))
            QuestionExplanationCard(
                payload = ExplanationPayload.WordSummary(
                    japanese = question.word.japanese,
                    hiragana = question.word.hiragana,
                    meaning = question.word.chinese
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TypingInput(
    question: TestQuestion.Typing,
    userInput: String,
    onInputChange: (String) -> Unit
) {
    val borderColor = when {
        !question.isAnswered -> MaterialTheme.colorScheme.outlineVariant
        question.isCorrect -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    // 自动聚焦和 haptic feedback
    val focusRequester = remember { FocusRequester() }
    val hapticFeedback = LocalHapticFeedback.current

    // 非回答状态时自动聚焦输入框
    LaunchedEffect(question.isAnswered) {
        if (!question.isAnswered) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
                // 忽略聚焦失败
            }
        }
    }

    OutlinedTextField(
        value = if (question.isAnswered) question.userAnswer else userInput,
        onValueChange = { newValue ->
            if (newValue != userInput) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onInputChange(newValue)
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        label = { Text(stringResource(R.string.test_common_label_your_answer)) },
        singleLine = true,
        enabled = !question.isAnswered,
        shape = RoundedCornerShape(16.dp), // 16dp Radius
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledBorderColor = borderColor,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = if (question.isAnswered && !question.isCorrect) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            disabledTextColor = if (!question.isCorrect) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            // Flat UI: Transparent or subtle background
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
}

@Composable
private fun TypingFeedback(question: TestQuestion.Typing) {
    AnimatedVisibility(
        visible = question.isAnswered,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
    ) {
        // Feedback Card (OutlinedCard style matches Choice Question explanation)
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (question.isCorrect)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            ),
            border = BorderStroke(
                1.dp,
                if (question.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp)
            ) {
                if (question.isCorrect) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp).padding(bottom = 8.dp)
                    )
                    Text(
                        text = "回答正确！",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp).padding(bottom = 8.dp)
                    )
                    Text(
                        text = "回答错误",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Show correct answer prominently
                    Text(
                        text = "正确答案",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = question.correctAnswer,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
