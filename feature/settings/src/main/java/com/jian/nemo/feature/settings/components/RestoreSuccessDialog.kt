package com.jian.nemo.feature.settings.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.designsystem.theme.SettingsPalette
import com.jian.nemo.core.designsystem.theme.TestResultPalette

/**
 * 恢复成功数据明细
 */
data class RestoreDetailStats(
    val wordUpdate: String = "0",
    val wordInsert: String = "0",
    val wordSkip: String = "0",
    val grammarUpdate: String = "0",
    val grammarInsert: String = "0",
    val grammarSkip: String = "0",
    val validationNote: String? = null
)

/**
 * 解析恢复结果文本
 */
private fun parseRestoreStats(message: String): RestoreDetailStats {
    var wordUpdate = "0"
    var wordInsert = "0"
    var wordSkip = "0"
    var grammarUpdate = "0"
    var grammarInsert = "0"
    var grammarSkip = "0"
    var validationNote: String? = null

    // 匹配: 单词: 更新X / 新增Y / 跳过Z
    val wordRegex = Regex("""单词:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)""")
    wordRegex.find(message)?.let { match ->
        wordUpdate = match.groupValues[1]
        wordInsert = match.groupValues[2]
        wordSkip = match.groupValues[3]
    }

    // 匹配: 语法: 更新A / 新增B / 跳过C
    val grammarRegex = Regex("""语法:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)""")
    grammarRegex.find(message)?.let { match ->
        grammarUpdate = match.groupValues[1]
        grammarInsert = match.groupValues[2]
        grammarSkip = match.groupValues[3]
    }

    // 匹配校验提示
    if (message.contains("校验提示:")) {
        validationNote = message.substringAfter("校验提示:").trim()
    }

    return RestoreDetailStats(
        wordUpdate = wordUpdate,
        wordInsert = wordInsert,
        wordSkip = wordSkip,
        grammarUpdate = grammarUpdate,
        grammarInsert = grammarInsert,
        grammarSkip = grammarSkip,
        validationNote = validationNote
    )
}

@Composable
fun RestoreSuccessDialog(
    message: String,
    isFromLogin: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dialogBgColor = if (isDark) SettingsPalette.DialogBgDark else Color.White
    val cardBgColor = if (isDark) SettingsPalette.DialogCardBgDark else SettingsPalette.DialogCardBgLight
    val successGreen = if (isDark) TestResultPalette.CorrectCardContentDark else TestResultPalette.CorrectCardContentLight
    val stats = remember(message) { parseRestoreStats(message) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val view = LocalView.current

        // 开启 Android 12+ 官方窗口级高斯毛玻璃 (Blur Behind)
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 48
                    dimAmount = 0.25f
                }
            }
            onDispose {
                if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = 0
                            dimAmount = 0f
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        Surface(
            modifier = Modifier
                .widthIn(min = 290.dp, max = 340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = dialogBgColor,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 成功图标
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = successGreen.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = successGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                    text = "档案恢复成功",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 副标题
                Text(
                    text = "云端学习档案已安全同步至本机",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 统计数据卡片
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = cardBgColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 词库部分
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "词库档案",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "更新: ${stats.wordUpdate}  /  新增: ${stats.wordInsert}  /  跳过: ${stats.wordSkip}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.8.dp
                        )

                        // 语法部分
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "语法档案",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "更新: ${stats.grammarUpdate}  /  新增: ${stats.grammarInsert}  /  跳过: ${stats.grammarSkip}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 校验提示（若有）
                        stats.validationNote?.let { note ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = 0.8.dp
                            )
                            Text(
                                text = "校验说明: $note",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 操作主按钮
                Button(
                    onClick = onConfirm,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (isFromLogin) "开启学习之旅" else "完成",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
