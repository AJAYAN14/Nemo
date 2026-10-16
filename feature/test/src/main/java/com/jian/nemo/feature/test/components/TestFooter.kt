package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.Backdrop

/**
 * 通用测试底部按钮组件 (液态玻璃质感版)
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
    isAutoAdvancing: Boolean = false,
    backdrop: Backdrop? = null
) {
    val shape = RoundedCornerShape(24.dp)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "上一题" 液态玻璃按钮 - 磨砂轻透质感 (Frosted Liquid Glass)
        FrostedLiquidGlassButton(
            onClick = { if (canGoPrev) onPrev() },
            modifier = Modifier
                .weight(0.4f)
                .height(56.dp),
            backdrop = backdrop,
            shape = shape,
            isInteractive = canGoPrev,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(
                text = "上一题",
                color = if (canGoPrev) onSurfaceColor else onSurfaceColor.copy(alpha = 0.38f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        val mainButtonText = when {
            !isAnswered -> submitText
            isLastQuestion -> "完成测试"
            else -> "下一题"
        }
        
        val isMainEnabled = (canSubmit || isAnswered) && !isAutoAdvancing

        // "检查 / 下一题 / 完成测试" 液态玻璃主按钮 - 彩色通透宝石质感 (Colored Liquid Glass)
        ColoredLiquidGlassButton(
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
            backdrop = backdrop,
            tint = if (isMainEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = shape,
            isInteractive = isMainEnabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(
                text = mainButtonText,
                color = if (isMainEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

