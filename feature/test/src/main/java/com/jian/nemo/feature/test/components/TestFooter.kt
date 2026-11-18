package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.*
import com.jian.nemo.core.designsystem.shape.Capsule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    submitText: String = stringResource(R.string.test_footer_btn_submit),
    isAutoAdvancing: Boolean = false,
    backdrop: Backdrop? = null
) {
    val shape = Capsule()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "上一题" 液态玻璃按钮 - 带底色磨砂轻透质感 (Frosted Liquid Glass)
        FrostedLiquidGlassButton(
            onClick = { if (canGoPrev) onPrev() },
            modifier = Modifier
                .weight(0.38f)
                .height(52.dp),
            backdrop = backdrop,
            surfaceColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.35f),
            shape = shape,
            isInteractive = canGoPrev,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(
                text = stringResource(R.string.test_footer_btn_prev),
                color = if (canGoPrev) onSurfaceColor else onSurfaceColor.copy(alpha = 0.38f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        val mainButtonText = when {
            !isAnswered -> submitText
            isLastQuestion -> stringResource(R.string.test_footer_btn_finish)
            else -> stringResource(R.string.test_footer_btn_next)
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
                .weight(0.62f)
                .height(52.dp),
            backdrop = backdrop,
            tint = if (isMainEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tintAlpha = if (isMainEnabled) 0.85f else 0.4f,
            shape = shape,
            isInteractive = isMainEnabled,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
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

