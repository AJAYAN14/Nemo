package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag


/**
 * 重置确认对话框 (V2: Pro Max Style)
 */
@Composable
fun ConfirmResetDialog(
    isResetting: Boolean = false,
    errorMessage: String? = null,
    isLoggedIn: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // UI/UX Pro Max Colors & Styles (Red Theme for Danger)
    val primaryColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!isResetting) onDismiss() }
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = containerColor,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = primaryColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Title
                Text(
                    text = "确认重置",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Content
                Text(
                    text = "您确定要重置所有学习进度吗？此操作将永久删除本地所有进度数据，且无法撤销。",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = bodyColor
                )

                // Cloud option removed

                if (isResetting) {
                    Spacer(modifier = Modifier.height(24.dp))
                    NemoChasingDotsLoader(size = 24.dp)
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = primaryColor,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Actions
                Button(
                    onClick = onConfirm,
                    enabled = !isResetting,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(if (isResetting) "正在重置..." else "确认重置", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !isResetting,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("取消", color = bodyColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

