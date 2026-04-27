package com.jian.nemo.feature.learning.presentation.components.srs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.feature.learning.presentation.components.common.scaleOnPress

/**
 * SRS 评分按钮组件 (100% 还原 HTML SRSButton)
 *
 * @param label 评分标签 (如 "完全忘记")
 * @param time 间隔时间 (如 "< 1m", "3d")
 * @param color 按钮主题色
 * @param onClick 点击回调
 */
@Composable
fun SRSRatingButton(
    label: String,
    time: String,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp) // h-14 = 56dp
            .scaleOnPress(onTap = onClick)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(containerColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = time,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview
@Composable
private fun SRSRatingButtonPreview() {
    MaterialTheme {
        SRSRatingButton(
            label = "完全忘记",
            time = "< 1m",
            color = Color(0xFFBE123C), // rose-700
            containerColor = Color(0xFFFFE4E6), // rose-100
            onClick = {}
        )
    }
}
