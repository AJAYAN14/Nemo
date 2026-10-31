package com.jian.nemo.feature.settings.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jian.nemo.feature.settings.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.data.manager.ImportPreview
import com.jian.nemo.core.data.manager.ImportStrategy
import com.jian.nemo.core.designsystem.theme.SettingsPalette

@Composable
fun RestorePreviewDialog(
    preview: ImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // 浅色纯白，深色纯净深灰，避免 MD3 默认自带的 primary tint
    val dialogBgColor = if (isDark) SettingsPalette.DialogBgDark else Color.White
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true
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
                    dimAmount = 0.20f
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
                .widthIn(min = 280.dp, max = 340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp), // 现代大圆角
            color = dialogBgColor,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题
                Text(
                    text = if (preview.strategy == ImportStrategy.MERGE) "合并恢复预览" else "覆盖恢复预览",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 正文内容区域
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (preview.validationSummary.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_restore_format_validation, preview.validationSummary),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (preview.strategy == ImportStrategy.MERGE) {
                        Text(stringResource(R.string.settings_restore_title_word_merge), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_restore_item_word_insert, preview.wordInsertCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.settings_restore_item_word_update, preview.wordUpdateCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.settings_restore_item_word_skip, preview.wordSkipCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.settings_restore_title_grammar_merge), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_restore_item_grammar_insert, preview.grammarInsertCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.settings_restore_item_grammar_update, preview.grammarUpdateCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.settings_restore_item_grammar_skip, preview.grammarSkipCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(R.string.settings_restore_title_data_impact), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.settings_restore_item_clear_progress, preview.localWordStateCount + preview.localGrammarStateCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.settings_restore_title_will_import), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_restore_item_import_words, preview.wordInsertCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.settings_restore_item_import_grammars, preview.grammarInsertCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_restore_title_other_data), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.settings_restore_item_new_mistakes, preview.wrongAnswerNewCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.settings_restore_item_new_tests, preview.testRecordNewCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.settings_restore_item_new_studies, preview.studyRecordNewCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.settings_restore_item_new_favorites, preview.favoriteNewCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (preview.settingsWillChange) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_restore_note_override_settings),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // 底部按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "取消",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = onConfirm,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "确认恢复",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
