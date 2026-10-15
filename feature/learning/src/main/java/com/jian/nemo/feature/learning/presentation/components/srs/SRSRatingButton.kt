package com.jian.nemo.feature.learning.presentation.components.srs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.Backdrop

/**
 * SRS 评分按钮组件 (无描边磨砂液态玻璃规范)
 *
 * @param label 评分标签 (如 "重来", "良好")
 * @param time 间隔时间 (如 "< 1m", "4d")
 * @param color 按钮文字主题色
 * @param containerColor 按钮容器底色
 * @param onClick 点击回调
 * @param backdrop 背景采样源 (读取底层卡片像素以产生真实透镜折射)
 */
@Composable
fun SRSRatingButton(
    label: String,
    time: String,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    FrostedLiquidGlassButton(
        onClick = onClick,
        backdrop = backdrop,
        surfaceColor = containerColor.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        border = null,
        contentPadding = PaddingValues(0.dp),
        isInteractive = true,
        modifier = modifier.height(56.dp)
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
            color = Color(0xFFBE123C),
            containerColor = Color(0xFFFFE4E6),
            onClick = {}
        )
    }
}
