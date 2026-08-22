package com.jian.nemo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * ColorScheme扩展属性
 *
 * 为学习、复习、测试等页面提供与旧项目一致的专用色彩
 */

/**
 * 页面语义背景色
 */
val ColorScheme.surfaceBackground: Color
    @Composable
    get() = if (background.luminance() < 0.5f) {
        NemoSurfaceBackgroundDark
    } else {
        NemoSurfaceBackground
    }

/**
 * 页面语义卡片容器色
 */
val ColorScheme.surfaceCard: Color
    @Composable
    get() = if (background.luminance() < 0.5f) {
        NemoSurfaceCardDark
    } else {
        NemoSurfaceCard
    }

// 废弃带有特定页面名称的扩展，建议使用通用的 surfaceBackground / surfaceCard
@Deprecated("Use surfaceBackground", ReplaceWith("surfaceBackground"))
val ColorScheme.learningBackground: Color @Composable get() = surfaceBackground

@Deprecated("Use surfaceCard", ReplaceWith("surfaceCard"))
val ColorScheme.learningCardBackground: Color @Composable get() = surfaceCard

@Deprecated("Use surfaceBackground", ReplaceWith("surfaceBackground"))
val ColorScheme.testBackground: Color @Composable get() = surfaceBackground

@Deprecated("Use surfaceCard", ReplaceWith("surfaceCard"))
val ColorScheme.testCardBackground: Color @Composable get() = surfaceCard
