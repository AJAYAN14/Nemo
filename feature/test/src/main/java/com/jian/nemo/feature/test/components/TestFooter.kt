package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.liquid.LiquidButton

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

    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Previous" Liquid Button - Solid Slate Style
        LiquidButton(
            onClick = { if (canGoPrev) onPrev() },
            modifier = Modifier
                .weight(0.4f)
                .height(56.dp),
            backgroundColor = if (canGoPrev) slate200 else slate100,
            shape = shape,
            isInteractive = true,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "上一题",
                color = if (canGoPrev) slate700 else slate700.copy(alpha = 0.4f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }

        val mainButtonText = when {
            !isAnswered -> submitText
            isLastQuestion -> "完成测试"
            else -> "下一题"
        }
        
        val isMainEnabled = (canSubmit || isAnswered) && !isAutoAdvancing

        // "Main" Liquid Button - Solid Indigo Style
        LiquidButton(
            onClick = {
                if (isMainEnabled) {
                    when {
                        !isAnswered -> onSubmit()
                        isLastQuestion -> onFinish()
                        else -> onNext()
                    }
                }
            },
            modifier = Modifier
                .weight(0.6f)
                .height(56.dp),
            backgroundColor = if (isMainEnabled) indigo600 else indigo100,
            shape = shape,
            isInteractive = true,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mainButtonText,
                color = if (isMainEnabled) Color.White else indigo600.copy(alpha = 0.5f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
    }
}

