package com.jian.nemo.feature.test.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 定义常量
private const val ANIMATION_DURATION = 200
private const val PRESS_SCALE = 0.97f

/**
 * 沉浸式头部 (V2: Clean & Minimal)
 * 去除背景干扰，强调信息本身
 */
@Composable
fun ImmersiveTestHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp, top = 8.dp)
    ) {
        // 主标题 (Bold & Clean)
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Premium Card (V2: Clean White/Dark + Soft Shadow)
 */
@Composable
fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Scale Animation if clickable
    val scale by if(onClick != null) {
        val isPressed by interactionSource.collectIsPressedAsState()
         animateFloatAsState(
            targetValue = if (isPressed) PRESS_SCALE else 1f,
            label = "cardScale",
            animationSpec = tween(ANIMATION_DURATION)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Light mode: Pure white + strong soft shadow. Dark mode: Surface Container + light shadow.
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val shadowElevation = if (isDark) 4.dp else 12.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f) // Very soft ambient shadow

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        shadowElevation = if (isDark) 2.dp else 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        content = { Column(content = content) }
    )
}

/**
 * 视觉化统计项 (Bold Numeric)
 */
@Composable
fun VisualStatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    // 简单的数字/单位分离逻辑
    val numberPart = value.filter { it.isDigit() || it == '.' }
    val unitPart = value.filterNot { it.isDigit() || it == '.' }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 使用 AnnotatedString 分离样式
        val text = androidx.compose.ui.text.buildAnnotatedString {
            withStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    fontSize = 32.sp, // 大字号
                    fontWeight = FontWeight.Black, // 最粗体
                    color = color
                )
            ) {
                append(numberPart)
            }
            if (unitPart.isNotEmpty()) {
                withStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        fontSize = 14.sp, // 小字号单位
                        fontWeight = FontWeight.Bold,
                        color = color.copy(alpha = 0.8f) // 稍微浅一点
                    )
                ) {
                    append(" $unitPart".trim()) // 稍微加个空格如果需要
                }
            }
        }

        Text(
            text = text,
            lineHeight = 32.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy( // 调大回 labelMedium
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp // 稍微减小一点字间距以平衡
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * ListItem with "Squircle" Icon (Apple Settings Style)
 */
@Composable
fun SquircleListItem(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), // Squircle approx
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 74.dp), // Align with text start
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
    )
}
