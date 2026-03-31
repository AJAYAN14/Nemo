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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.jian.nemo.feature.settings.ConflictResolutionOption

/**
 * 冲突解决对话框 (V2: Pro Max Style)
 */
@Composable
fun ConflictResolutionDialog(
    conflictCount: Int,
    onDismiss: () -> Unit,
    onResolve: (ConflictResolutionOption) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // UI/UX Pro Max Colors & Styles (Blue Theme for Process/Sync)
    val primaryColor = if (useDarkTheme) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val errorActionColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                        imageVector = Icons.Rounded.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Title
                Text(
                    text = "解决同步冲突",
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
                    text = "检测到 $conflictCount 个数据冲突。请选择您的解决方案：",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = bodyColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Options explanation Card
                Surface(
                    color = if (useDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "• 保留云端：使用云端数据覆盖本地。",
                            style = MaterialTheme.typography.bodySmall,
                            color = bodyColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 保留本地：强制上传本地数据覆盖云端。",
                            style = MaterialTheme.typography.bodySmall,
                            color = bodyColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Actions
                Button(
                    onClick = { onResolve(ConflictResolutionOption.FORCE_CLOUD) },
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
                    Text("保留云端 (推荐)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { onResolve(ConflictResolutionOption.FORCE_LOCAL) },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = errorActionColor)
                ) {
                    Text("保留本地并覆盖云端", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("取消", color = bodyColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * 重置确认对话框 (V2: Pro Max Style)
 */
@Composable
fun ConfirmResetDialog(
    isResetting: Boolean = false,
    errorMessage: String? = null,
    isLoggedIn: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // UI/UX Pro Max Colors & Styles (Red Theme for Danger)
    val primaryColor = if (useDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val containerColor = if (useDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val titleColor = if (useDarkTheme) Color.White else Color.Black
    val bodyColor = if (useDarkTheme) Color(0xFF8E8E93) else Color(0xFF6E6E73)

    var includeCloud by remember { mutableStateOf(false) }

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

                if (isLoggedIn) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                         color = if (useDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                         shape = RoundedCornerShape(12.dp),
                         modifier = Modifier.fillMaxWidth().clickable { if (!isResetting) includeCloud = !includeCloud }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeCloud,
                                onCheckedChange = { if (!isResetting) includeCloud = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    uncheckedColor = bodyColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                             Text(
                                text = "同时删除云端同步数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = titleColor
                            )
                        }
                    }
                }

                if (isResetting) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = primaryColor,
                        strokeWidth = 3.dp
                    )
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
                    onClick = { onConfirm(includeCloud) },
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

