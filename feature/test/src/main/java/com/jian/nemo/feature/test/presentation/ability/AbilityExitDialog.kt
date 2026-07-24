package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 能力工坊通用退出确认弹窗 - UI/UX Pro Max 顶级拟物视觉与微动效重塑版
 */
@Composable
fun AbilityExitDialog(
    show: Boolean,
    title: String,
    message: String,
    themeColor: Color,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    onDismiss: () -> Unit,
    onKeepAndExit: () -> Unit,
    onDestroyAndExit: () -> Unit
) {
    if (!show) return

    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = "保留并退出挑战",
        dismissText = "继续挑战",
        confirmButtonColor = themeColor,
        onConfirm = onKeepAndExit,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = textSub,
                        lineHeight = 21.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDestroyAndExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30).copy(alpha = 0.12f),
                        contentColor = Color(0xFFFF3B30)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = "放弃本次进度并退出",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    )
}
