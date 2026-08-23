package com.jian.nemo.feature.learning.presentation.components.dialogs

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.designsystem.theme.NotoSerifJP

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TypingPracticeDialog(
    word: Word,
    onDismiss: () -> Unit,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    val state = rememberTypingPracticeDialogState(onDismiss = onDismiss)
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    val onConfirm = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        focusManager.clearFocus()
        state.validate(word)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false // Disable dismiss on click outside
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
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Box {
                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(top = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题
                    Text(
                        text = "跟打练习",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                    )

                    // 单词显示
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth() // Ensure full width for centering
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = word.japanese,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = NotoSerifJP,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center // Explicitly center text
                            )
                            Text(
                                text = word.hiragana,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = NotoSerifJP,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // 反馈消息
                    AnimatedContent(
                        targetState = state.feedbackState,
                        modifier = Modifier
                            .height(50.dp)
                            .padding(bottom = 16.dp),
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)))
                                .togetherWith(fadeOut(animationSpec = tween(500)))
                        },
                        label = "FeedbackAnimation"
                    ) { feedback ->
                        when (feedback) {
                            FeedbackState.CORRECT -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "回答正确！",
                                        color = themeColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            FeedbackState.INCORRECT -> {
                                Text(
                                    text = "回答错误，请检查您的输入",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            FeedbackState.HIDDEN -> { /* 空占位 */ }
                        }
                    }

                    // 输入区域
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 假名输入
                        OutlinedTextField(
                            value = state.kanaInput,
                            onValueChange = state::updateKanaInput,
                            label = { Text("假名", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(20.dp), // Optimized to Squashy Rect
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = themeColor,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // 汉字输入
                        OutlinedTextField(
                            value = state.kanjiInput,
                            onValueChange = state::updateKanjiInput,
                            label = { Text("汉字", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            shape = RoundedCornerShape(20.dp), // Optimized to Squashy Rect
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = themeColor,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = state::clear,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("清空", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f).height(50.dp), // Minimal height increase
                            shape = RoundedCornerShape(25.dp), // Full Pill Shape
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text("确定", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
