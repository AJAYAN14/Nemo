package com.jian.nemo.feature.learning.presentation.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.softShadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.feature.learning.R
import com.jian.nemo.feature.learning.presentation.LearningMode

private const val ANIMATION_DURATION = 200
private const val PRESS_SCALE = 0.98f

/**
 * 沉浸式首页头部组件 (Solid Typography)
 */
@Composable
fun HomeUnifiedHeader(
    title: String,
    bgText: String = stringResource(R.string.learning_home_header_bg_text),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp, top = 8.dp)
    ) {
        // 背景大装饰字
        Text(
            text = bgText,
            fontSize = 90.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-15).dp, y = (-20).dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

/**
 * Premium Card (Solid Layered Surface)
 * 采用 G2 连续平滑曲率与柔和卡片阴影
 */
@Composable
fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by if (onClick != null) {
        val isPressed by interactionSource.collectIsPressedAsState()
        animateFloatAsState(
            targetValue = if (isPressed) PRESS_SCALE else 1f,
            label = "cardScale",
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedRectangle(26.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 26.dp, isDark = isDark)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        content = { Column(content = content) }
    )
}

/**
 * 首页双轨外观图标单容器组件
 *
 * 严格遵循《设置项与图标双轨系统设计规范》：
 * 1. 严格 2:3 黄金比例公理 (图标尺寸 = 容器尺寸 * 2/3)；
 * 2. G2 连续平滑曲率 (RoundedRectangle)；
 * 3. 单源同色单容器架构：
 *    - 柔和微透 (MD3): 15% 透明度底框 + 原色图标 (iconColor)；
 *    - 活力纯色 (PLUMP_DUO/SOLID): 100% 实心底框 + 纯白反白图标 (Color.White)；
 * 4. 两套风格切换时图标物理尺寸绝对恒定。
 */
@Composable
fun HomeSquircleIconBox(
    icon: ImageVector,
    iconColor: Color,
    containerSize: Dp = 36.dp,
    iconSize: Dp = (containerSize.value * 2 / 3).dp,
    cornerRadius: Dp = if (containerSize >= 42.dp) 12.dp else 10.dp,
    modifier: Modifier = Modifier
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val containerBgColor = if (isSoftTranslucent) iconColor.copy(alpha = 0.15f) else iconColor
    val iconTint = if (isSoftTranslucent) iconColor else Color.White

    Box(
        modifier = modifier
            .size(containerSize)
            .background(containerBgColor, RoundedRectangle(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * ListItem with "Squircle" Icon (Solid Premium)
 */
@Composable
fun HomeSquircleListItem(
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Icon (标准级 42dp 容器配 28dp 图标，严格遵守 2:3 黄金比例公理)
        HomeSquircleIconBox(
            icon = icon,
            iconColor = color,
            containerSize = 42.dp,
            iconSize = 28.dp,
            cornerRadius = 12.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(12.dp)
        )
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 76.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
            thickness = 0.5.dp
        )
    }
}

/**
 * 分组标题 (Solid Style)
 * 严格对齐《标题与卡片对齐设计规范》
 */
@Composable
fun HomeSectionTitle(
    text: String,
    modifier: Modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
