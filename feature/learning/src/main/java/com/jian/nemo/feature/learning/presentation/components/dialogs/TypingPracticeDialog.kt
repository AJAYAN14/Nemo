package com.jian.nemo.feature.learning.presentation.components.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.NemoDialog

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

    NemoDialog(
        onDismissRequest = onDismiss,
        title = "跟打练习",
        confirmText = "确定",
        dismissText = "清空",
        confirmButtonColor = themeColor,
        onConfirm = onConfirm,
        onDismiss = {
            focusManager.clearFocus()
            state.clear()
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 单词显示
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = word.japanese,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = NotoSerifJP,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
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
                        .height(36.dp)
                        .padding(bottom = 8.dp),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.85f, animationSpec = tween(400)))
                            .togetherWith(fadeOut(animationSpec = tween(300)))
                    },
                    label = "FeedbackAnimation"
                ) { feedback ->
                    when (feedback) {
                        FeedbackState.CORRECT -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
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
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                        FeedbackState.INCORRECT -> {
                            Text(
                                text = "回答错误，请检查您的输入",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 15.sp,
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
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
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
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
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
            }
        }
    )
}
