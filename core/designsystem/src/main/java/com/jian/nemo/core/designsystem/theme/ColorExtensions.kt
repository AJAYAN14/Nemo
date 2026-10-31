package com.jian.nemo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * ColorScheme扩展属性
 *
 * 为学习、复习、测试等页面提供与 Dynamic Color System 规范完全一致的动态语义色彩
 */

/**
 * 页面语义背景色 (Level 0: 浅色 #F2F2F7 / 深色 #000000)
 */
val ColorScheme.surfaceBackground: Color
    @Composable
    get() = if (background.luminance() < 0.5f) {
        NemoSurfaceBackgroundDark
    } else {
        NemoSurfaceBackground
    }

/**
 * 页面语义卡片容器色 (Level 1: 浅色 #FFFFFF / 深色 #1C1C1E)
 */
val ColorScheme.surfaceCard: Color
    @Composable
    get() = if (background.luminance() < 0.5f) {
        NemoSurfaceCardDark
    } else {
        NemoSurfaceCard
    }

/**
 * 页面语义二级嵌套表面 (Level 2: 浅色 #E5E5EA / 深色 #2C2C2E)
 */
val ColorScheme.surfaceSubcard: Color
    @Composable
    get() = if (background.luminance() < 0.5f) {
        NemoSurfaceSubcardDark
    } else {
        NemoSurfaceSubcard
    }

/**
 * 页面语义三级浮动/激活表面 (Level 3: 浅色 #D1D1D6 / 深色 #3A3A3C)
 */
val ColorScheme.surfaceElevated: Color
    @Composable
    get() = if (background.luminance() < 0.5f) {
        NemoSurfaceElevatedDark
    } else {
        NemoSurfaceElevated
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
