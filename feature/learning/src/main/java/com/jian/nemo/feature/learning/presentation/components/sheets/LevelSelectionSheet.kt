package com.jian.nemo.feature.learning.presentation.components.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.BentoColors
import androidx.compose.foundation.clickable

/**
 * 等级选择底部Sheet (UI/UX Pro Max Optimized)
 *
 * 使用卡片式布局替代传统 RadioButton 列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectionBottomSheet(
    show: Boolean,
    title: String = "选择学习等级",
    levels: List<String> = listOf("N5", "N4", "N3", "N2", "N1"),
    selectedLevel: String,
    primaryColor: androidx.compose.ui.graphics.Color = BentoColors.Primary,
    onDismiss: () -> Unit,
    onLevelSelected: (String) -> Unit
) {
    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = BentoColors.Surface,
            contentColor = BentoColors.TextMain,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = BentoColors.TextMain,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val levelDescriptions = mapOf(
                    "N5" to "初级 - 基础词汇",
                    "N4" to "初级 - 进阶词汇",
                    "N3" to "中级 - 常用词汇",
                    "N2" to "中高级 - 商务词汇",
                    "N1" to "高级 - 学术词汇"
                )

                // Use LazyColumn for potential long lists, though N1-N5 fits easily.
                // Using Column is fine for 5 items, but let's stick to Column for simplicity within Sheet.
                levels.forEach { level ->
                    LevelSelectionCard(
                        level = "JLPT $level",
                        description = levelDescriptions[level] ?: "",
                        isSelected = level == selectedLevel,
                        primaryColor = primaryColor,
                        onClick = {
                            onLevelSelected(level)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LevelSelectionCard(
    level: String,
    description: String,
    isSelected: Boolean,
    primaryColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val themePrimary = primaryColor
    val surfaceColor = BentoColors.Surface
    val onSurfaceColor = BentoColors.TextMain

    // Premium Gray for Unselected
    val premiumGrayLow = androidx.compose.ui.graphics.Color(0xFFF2F2F7)

    // Animations
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) themePrimary.copy(alpha = 0.12f) else premiumGrayLow,
        label = "cardBg",
        animationSpec = tween(300)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "cardScale",
        animationSpec = tween(100)
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) themePrimary else onSurfaceColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) themePrimary.copy(alpha = 0.8f) else BentoColors.TextSub,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) themePrimary else BentoColors.TextSub.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


