package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 通用测试底部按钮组件
 */
@Composable
fun TestFooter(
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onFinish: () -> Unit,
    canGoPrev: Boolean = true,
    canSubmit: Boolean = true,
    isAnswered: Boolean = false,
    isLastQuestion: Boolean = false,
    submitText: String = "提交",
    isAutoAdvancing: Boolean = false
) {
    // UI/UX PRO MAX: Pure Solid Tonal Palette (No Alpha)
    val indigo600 = Color(0xFF4F46E5)
    val indigo100 = Color(0xFFE0E7FF) 
    val slate700 = Color(0xFF334155)
    val slate200 = Color(0xFFE2E8F0)
    val slate100 = Color(0xFFF1F5F9)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp), // Preserving user's adjustment
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Previous" Button - Solid Slate Style
        AnimatedButton(
            text = "上一题",
            onClick = onPrev,
            modifier = Modifier.weight(0.4f), 
            enabled = canGoPrev,
            containerColor = if (canGoPrev) slate200 else slate100,
            contentColor = if (canGoPrev) slate700 else slate700.copy(alpha = 0.4f)
        )

        val mainButtonText = when {
            !isAnswered -> submitText
            isLastQuestion -> "完成测试"
            else -> "下一题"
        }
        
        // "Main" Button - Solid Indigo Style
        AnimatedButton(
            text = mainButtonText,
            onClick = {
                when {
                    !isAnswered -> onSubmit()
                    isLastQuestion -> onFinish()
                    else -> onNext()
                }
            },
            modifier = Modifier.weight(0.6f),
            enabled = (canSubmit || isAnswered) && !isAutoAdvancing,
            containerColor = if (canSubmit || isAnswered) indigo600 else indigo100,
            contentColor = if (canSubmit || isAnswered) Color.White else indigo600.copy(alpha = 0.5f)
        )
    }
}
