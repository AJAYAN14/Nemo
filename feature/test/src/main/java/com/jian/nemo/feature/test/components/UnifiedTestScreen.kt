package com.jian.nemo.feature.test.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun UnifiedTestScreen(
    headerContent: @Composable () -> Unit,
    progressContent: @Composable () -> Unit,
    testContent: @Composable () -> Unit,
    footerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // 动态背景色：适配深色模式
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.background
    val isDark = backgroundColor.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor) // 极致纯白背景
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 可滚动内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            headerContent()
            Spacer(modifier = Modifier.height(16.dp))
            progressContent()
            Spacer(modifier = Modifier.height(16.dp))
            testContent()

            // 动态底部留白，确保解析卡片全展现，不被悬浮按钮及渐变蒙层遮挡
            Spacer(modifier = Modifier.height(160.dp))
        }

        // 底部渐变蒙层：从透明到纯白的平滑过渡
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp) // 使用 180.dp 渐变，确保内容滚动到底部时有自然消失的感官
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = if (isDark) 0.6f else 0.9f),
                            backgroundColor
                        )
                    )
                )
        )

        // 固定底部按钮区域
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp) // 保持底部间距，增加通透感
        ) {
            footerContent()
        }
    }
}
