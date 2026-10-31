package com.jian.nemo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NemoPrimary,                    // 主色：规范交互蓝 #007AFF
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5F1FF),    // 主色容器：柔浅蓝
    onPrimaryContainer = Color(0xFF004CB3),
    secondary = NemoSecondary,                // 次色：规范绿 #34C759
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDF5E3),   // 次色容器：浅绿色
    onSecondaryContainer = Color(0xFF0F5A24),
    tertiary = Color(0xFF8E8E93),             // 三级中性灰 #8E8E93
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = NemoSurfaceSubcard,   // 三级容器：#E5E5EA
    onTertiaryContainer = Color(0xFF3C3C43),
    error = NemoDanger,                       // 错误色：规范红 #FF3B30
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE5E5),       // 错误色容器：浅红
    onErrorContainer = Color(0xFF991B1B),
    background = NemoSurfaceBackground,       // Level 0 画布基底：#F2F2F7 (微暖浅灰沉底)
    onBackground = NemoText,                  // 一级文字：#000000 (100% 对比度)
    surface = NemoSurfaceCard,                // Level 1 一级卡片：#FFFFFF (绝对纯白)
    onSurface = NemoText,                     // 面内容文字：#000000
    surfaceContainer = NemoSurfaceCard,       // 卡片容器：#FFFFFF
    surfaceContainerLow = Color(0xFFF7F7FA),  // 微低卡片表面
    surfaceContainerHigh = NemoSurfaceSubcard, // Level 2 二级表面：#E5E5EA
    surfaceContainerHighest = NemoSurfaceElevated, // Level 3 三级部件/激活轨道：#D1D1D6
    surfaceVariant = NemoSurfaceSubcard,      // 变体面色：#E5E5EA
    onSurfaceVariant = NemoTextLight,         // 二级副文字：#8A8A8E (60% 对比度)
    outline = NemoSurfaceBorder,              // 轮廓线/分割线：#C6C6C8 (发丝线)
    outlineVariant = Color(0xFFE5E5EA)        // 轮廓变体
)

private val DarkColorScheme = darkColorScheme(
    primary = NemoPrimaryDark,                // 主色：深色自发光高亮蓝 #0A84FF
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF00408A),     // 主色容器：深自发光蓝容器
    onPrimaryContainer = Color(0xFFB3D9FF),
    secondary = NemoSecondaryDark,            // 次色：深色高荧光绿 #30D158
    onSecondary = Color(0xFF003912),
    secondaryContainer = Color(0xFF135324),   // 次色容器：深暗绿
    onSecondaryContainer = Color(0xFF82F79E),
    tertiary = Color(0xFF8E8E93),             // 三级中性灰 #8E8E93
    onTertiary = Color(0xFF000000),
    tertiaryContainer = NemoSurfaceSubcardDark, // 三级容器：#2C2C2E
    onTertiaryContainer = Color(0xFFEBEBF5),
    error = NemoDangerDark,                   // 错误色：深色防刺眼自发光红 #FF453A
    onError = Color(0xFF4A0002),
    errorContainer = Color(0xFF801015),       // 错误色容器：暗深红
    onErrorContainer = Color(0xFFFFD4D4),
    background = NemoSurfaceBackgroundDark,   // Level 0 画布基底：#000000 (OLED 纯黑像素完全熄灭)
    onBackground = Color(0xFFFFFFFF),         // 一级文字：#FFFFFF (100% 对比度纯白)
    surface = NemoSurfaceCardDark,             // Level 1 一级卡片：#1C1C1E (~11% 亮度深石板灰)
    onSurface = Color(0xFFFFFFFF),            // 面内容文字：#FFFFFF
    surfaceContainer = NemoSurfaceCardDark,   // 卡片容器：#1C1C1E
    surfaceContainerLow = Color(0xFF141416),  // 微低卡片表面
    surfaceContainerHigh = NemoSurfaceSubcardDark, // Level 2 二级表面：#2C2C2E (~17% 亮度浮动石板灰)
    surfaceContainerHighest = NemoSurfaceElevatedDark, // Level 3 三级部件/激活轨道：#3A3A3C (~23% 亮度)
    surfaceVariant = NemoSurfaceSubcardDark,  // 变体面色：#2C2C2E
    onSurfaceVariant = Color(0xFF8E8E93),     // 二级副文字：#8E8E93 (60% 对比度深色副文本)
    outline = NemoSurfaceBorderDark,          // 轮廓线/分割线：#38383A (微光灰)
    outlineVariant = Color(0xFF2C2C2E)        // 轮廓变体
)

/**
 * Nemo Material3 主题配置驱动
 *
 * 严格支持：
 * - 纯正深浅色双模式与明度梯阶映射
 * - 主题色覆盖支持
 * - 杜绝系统壁纸动态取色，全机型展现纯正 Nemo 视觉体系
 */
@Composable
fun NemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = (if (darkTheme) DarkColorScheme else LightColorScheme).let { scheme ->
        if (themeColor != null) {
            scheme.copy(primary = themeColor)
        } else scheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NemoTypography,
        shapes = NemoShapes,
        content = content
    )
}

/**
 * 统一的页面背景色访问扩展
 * 所有主界面的 Scaffold 统一使用此颜色，以消除各界面间的硬编码导致的色差。
 * 浅色模式下为 #F2F2F7 (NemoSurfaceBackground)，深色模式下自动适配深色 #000000。
 */
val androidx.compose.material3.ColorScheme.screenBackground: Color
    @Composable
    get() = background
