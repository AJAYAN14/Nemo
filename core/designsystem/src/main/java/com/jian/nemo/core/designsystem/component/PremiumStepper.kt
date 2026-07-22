package com.jian.nemo.core.designsystem.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 高级质感加减器 (PremiumStepper)
 * 依据 web-haptics / Apple HIG 设计规范注入原生 3 阶沉浸触觉震动：
 * 1. Tick 按键微震 (VibrationEffect.EFFECT_TICK / 轻短机械齿轮感)
 * 2. Heavy 边界止动重震 (VibrationEffect.EFFECT_HEAVY_CLICK / 到达边界物理碰撞感)
 * 3. 机械分位独立滚轮计数器 (Per-Digit Odometer Counter)
 * 4. 软灰色按键与点击数字键盘手写输入
 */
@Composable
fun PremiumStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 999,
    step: Int = 1,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = isSystemInDarkTheme()

    // 硬件 Vibrator 初始化
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // 1. 齿轮轻刻度震动 (Tick)
    fun performHapticTick() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 60))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // 2. 边界物理阻尼碰撞重震 (Heavy Click)
    fun performHapticHeavyBoundary() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, 255))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    var lastValue by remember { mutableIntStateOf(value) }
    val isIncreasing = value >= lastValue

    remember(value) {
        lastValue = value
        true
    }

    // 键盘直接输入状态控制
    var isEditing by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value.toString(), selection = TextRange(0, value.toString().length)))
    }
    val focusRequester = remember { FocusRequester() }

    fun submitInput() {
        val parsed = textFieldValue.text.toIntOrNull()
        if (parsed != null) {
            val clamped = parsed.coerceIn(min, max)
            onValueChange(clamped)
            performHapticTick()
        } else {
            textFieldValue = TextFieldValue(text = value.toString(), selection = TextRange(0, value.toString().length))
        }
        isEditing = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // 当处于编辑状态时，强制聚焦并唤起软键盘
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        modifier = modifier
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ),
                shape = CircleShape
            )
            .clip(CircleShape),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFF8FAFC),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 减号软灰按钮
            StepperIconButton(
                onClick = {
                    if (isEditing) {
                        submitInput()
                    }
                    if (value - step >= min) {
                        performHapticTick()
                        onValueChange(value - step)
                    } else {
                        performHapticHeavyBoundary()
                    }
                },
                enabled = enabled,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "减少",
                        tint = if (enabled && value - step >= min) {
                            if (isDark) Color.White else Color(0xFF334155)
                        } else {
                            (if (isDark) Color.White else Color(0xFF334155)).copy(alpha = 0.38f)
                        }
                    )
                }
            )

            // 中央数字区域：点击切换输入模式，平时呈现【分位独立机械滚轮】
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) {
                        performHapticTick()
                        textFieldValue = TextFieldValue(
                            text = value.toString(),
                            selection = TextRange(0, value.toString().length)
                        )
                        isEditing = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isEditing) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            if (newValue.text.length <= 4 && newValue.text.all { it.isDigit() }) {
                                textFieldValue = newValue
                            }
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitInput() }
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                    )
                } else {
                    // 分位独立机械滚轮计数器 (Per-Digit Odometer Counter)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val digitChars = value.toString().map { it }
                        digitChars.forEachIndexed { index, char ->
                            AnimatedContent(
                                targetState = char,
                                transitionSpec = {
                                    if (isIncreasing) {
                                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                                                (slideOutVertically { height -> -height } + fadeOut())
                                    } else {
                                        (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                                (slideOutVertically { height -> height } + fadeOut())
                                    }.using(SizeTransform(clip = false))
                                },
                                label = "PerDigitOdometerAnimation_$index"
                            ) { digitChar ->
                                Text(
                                    text = digitChar.toString(),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 加号软灰按钮
            StepperIconButton(
                onClick = {
                    if (isEditing) {
                        submitInput()
                    }
                    if (value + step <= max) {
                        performHapticTick()
                        onValueChange(value + step)
                    } else {
                        performHapticHeavyBoundary()
                    }
                },
                enabled = enabled,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "增加",
                        tint = if (enabled && value + step <= max) {
                            if (isDark) Color.White else Color(0xFF334155)
                        } else {
                            (if (isDark) Color.White else Color(0xFF334155)).copy(alpha = 0.38f)
                        }
                    )
                }
            )
        }
    }
}

/**
 * 软灰色按压缩放按钮
 */
@Composable
private fun StepperIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "StepperIconButtonScale"
    )

    // 软灰色调：Light 模式下采用柔软沉稳的 #E2E8F0 浅灰色，Dark 模式下采用 #262626
    val softButtonColor = if (isDark) Color(0xFF262626) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                color = if (enabled) {
                    softButtonColor
                } else {
                    softButtonColor.copy(alpha = 0.4f)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Preview(name = "Light Theme")
@Composable
private fun PremiumStepperPreviewLight() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            var count by remember { mutableIntStateOf(10) }
            PremiumStepper(
                value = count,
                onValueChange = { count = it }
            )
        }
    }
}
