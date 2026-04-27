package com.jian.nemo.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = NemoPrimary,                    // 主色：Nemo 品牌蓝 #0E68FF
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6F0FF),    // 主色容器：浅蓝色
    onPrimaryContainer = Color(0xFF003A8C),
    secondary = NemoSecondary,                // 次色：Nemo 辅助绿 #4CAF50
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8E6C9),   // 次色容器：浅绿色
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFF6B7280),             // 三级颜色：中性灰色
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5E7EB),    // 三级颜色容器：浅灰色
    onTertiaryContainer = Color(0xFF374151),
    error = NemoDanger,                       // 错误色：Nemo 红色 #E53935
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFEBEE),       // 错误色容器：浅红色
    onErrorContainer = Color(0xFFB71C1C),
    background = NemoSurfaceBackground,       // 背景色：语义化页面背景
    onBackground = NemoText,
    surface = NemoSurfaceCard,                // 容器面色：语义化卡片背景    onSurface = NemoText,
    surfaceVariant = Color(0xFFF3F4F6),       // 容器面色变体：浅灰色
    onSurfaceVariant = NemoTextLight,
    outline = NemoSurfaceBorder,              // 轮廓色：语义化边框颜色
    outlineVariant = Color(0xFFE5E7EB)
)

private val DarkColorScheme = darkColorScheme(
    primary = NemoPrimary,                // 强制使用 Nemo 品牌蓝 #0E68FF
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF003A8C),      // 主色容器：深蓝色
    onPrimaryContainer = Color(0xFFB3D9FF),
    secondary = Color(0xFF81C784),            // 次色：深色模式下的浅绿色
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF2E7D32),   // 次色容器：深绿色
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFF9CA3AF),              // 三级颜色：中性灰色
    onTertiary = Color(0xFF374151),
    tertiaryContainer = Color(0xFF4B5563),     // 三级颜色容器：深灰色
    onTertiaryContainer = Color(0xFFD1D5DB),
    error = Color(0xFFEF5350),                 // 错误色：深色模式下的浅红色
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFC62828),         // 错误色容器：深红色
    onErrorContainer = Color(0xFFFFCDD2),
    background = Color(0xFF1a1a1a),            // 背景色：Nemo 深色背景
    onBackground = Color(0xFFE0E0E0),          // 背景内容色：浅灰色文本
    surface = Color(0xFF2c2c2c),              // 容器面色：Nemo 深色卡片
    onSurface = Color(0xFFE0E0E0),             // 面内容色：浅灰色文本
    surfaceVariant = Color(0xFF2F2F2F),       // 容器面色变体：深灰色
    onSurfaceVariant = Color(0xFFB0B0B0),      // 面变体内容色：中灰色文本
    outline = Color(0xFF444444),               // 轮廓色：深色边框
    outlineVariant = Color(0xFF3A3A3A)          // 轮廓色变体：深色边框
)

/**
 * Nemo Material3 主题配置驱动
 *
 * 支持：
 * - 浅色与深色模式切换
 * - 动态色彩配置 (Android 12+)
 * - Material You 设计语言实现
 */
@Composable
fun NemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }.let { scheme ->
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
