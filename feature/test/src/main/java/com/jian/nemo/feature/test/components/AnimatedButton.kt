package com.jian.nemo.feature.test.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 自定义函数来收集按下状态
@Composable
fun MutableInteractionSource.collectIsPressedAsState(): State<Boolean> {
    val pressed = remember { mutableStateOf(false) }

    LaunchedEffect(this) {
        this@collectIsPressedAsState.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressed.value = true
                is PressInteraction.Release -> pressed.value = false
                is PressInteraction.Cancel -> pressed.value = false
            }
        }
    }

    return pressed
}

@Composable
fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOutlined: Boolean = false,
    containerColor: Color? = null, // Custom background
    contentColor: Color? = null    // Custom text color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // UI/UX PRO MAX: Physical Spring animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        label = "buttonScale",
        animationSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val buttonModifier = modifier
        .scale(scale)
        .height(56.dp) // Slightly taller for premium feel

    val colors = if (containerColor != null && contentColor != null) {
        ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
    } else if (isOutlined) {
        ButtonDefaults.outlinedButtonColors()
    } else {
        ButtonDefaults.buttonColors()
    }

    if (isOutlined && containerColor == null) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(24.dp), // Consistent with AnswerContainer
            enabled = enabled,
            interactionSource = interactionSource
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(24.dp), // Consistent with AnswerContainer
            enabled = enabled,
            colors = colors,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            interactionSource = interactionSource
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}
