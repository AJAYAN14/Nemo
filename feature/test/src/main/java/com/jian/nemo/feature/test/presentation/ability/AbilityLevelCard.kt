package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 能力工坊通用等级选择 Card 组件
 */
@Composable
fun AbilityLevelCard(
    level: String,
    description: String,
    color: Color,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = if (isDark) 0.12f else 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = if (isDark) 0.25f else 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧实心圆圈标签
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 中间粗体描述文本
            Text(
                text = description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textMain,
                modifier = Modifier.weight(1f)
            )
            
            // 右侧箭头指示器
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = textSub.copy(alpha = 0.5f)
            )
        }
    }
}
