package com.example.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 生产级 Android Jetpack Compose 规范设计色彩系统
 * 严格保留官方原生色值标准，区分浅色模式 (Light Mode) 与深色模式 (Dark Mode)。
 */
object AppTokens {
    // ==========================================
    // 1. 系统动态主色调 - 浅色模式 (Light Mode)
    // ==========================================
    val SystemRedLight = Color(0xFFFF3B30)
    val SystemOrangeLight = Color(0xFFFF9500)
    val SystemYellowLight = Color(0xFFFFCC00)
    val SystemGreenLight = Color(0xFF34C759)
    val SystemMintLight = Color(0xFF00C7BE)
    val SystemTealLight = Color(0xFF30B0C7)
    val SystemCyanLight = Color(0xFF32ADE6)
    val SystemBlueLight = Color(0xFF007AFF)
    val SystemIndigoLight = Color(0xFF5856D6)
    val SystemPurpleLight = Color(0xFFAF52DE)
    val SystemPinkLight = Color(0xFFFF2D55)
    val SystemBrownLight = Color(0xFFA2845E)
    val SystemGrayLight = Color(0xFF8E8E93)
    val SystemGray2Light = Color(0xFFAEAEB2)
    val SystemGray3Light = Color(0xFFC7C7CC)
    val SystemGray4Light = Color(0xFFD1D1D6)
    val SystemGray5Light = Color(0xFFE5E5EA)
    val SystemGray6Light = Color(0xFFF2F2F7)

    // ==========================================
    // 1. 系统动态主色调 - 深色模式 (Dark Mode)
    // ==========================================
    val SystemRedDark = Color(0xFFFF453A)
    val SystemOrangeDark = Color(0xFFFF9F0A)
    val SystemYellowDark = Color(0xFFFFD60A)
    val SystemGreenDark = Color(0xFF30D158)
    val SystemMintDark = Color(0xFF66D4CF)
    val SystemTealDark = Color(0xFF40C8E0)
    val SystemCyanDark = Color(0xFF64D2FF)
    val SystemBlueDark = Color(0xFF0A84FF)
    val SystemIndigoDark = Color(0xFF5E5CE6)
    val SystemPurpleDark = Color(0xFFBF5AF2)
    val SystemPinkDark = Color(0xFFFF375F)
    val SystemBrownDark = Color(0xFFAC8E68)
    val SystemGrayDark = Color(0xFF8E8E93)
    val SystemGray2Dark = Color(0xFF636366)
    val SystemGray3Dark = Color(0xFF48484A)
    val SystemGray4Dark = Color(0xFF3A3A3C)
    val SystemGray5Dark = Color(0xFF2C2C2E)
    val SystemGray6Dark = Color(0xFF1C1C1E)

    // ==========================================
    // 2. 语义化界面层级与文本 - 浅色模式 (Light Mode)
    // 采用 Android AARRGGBB 标准通道定义
    // ==========================================
    val SystemBackgroundLight = Color(0xFFFFFFFF)
    val SecondarySystemBackgroundLight = Color(0xFFF2F2F7)
    val TertiarySystemBackgroundLight = Color(0xFFFFFFFF)
    val SystemGroupedBackgroundLight = Color(0xFFF2F2F7)
    val SecondarySystemGroupedBackgroundLight = Color(0xFFFFFFFF)
    val TertiarySystemGroupedBackgroundLight = Color(0xFFF2F2F7)
    val LabelLight = Color(0xFF000000)
    val SecondaryLabelLight = Color(0x993C3C43)       // 60% 不透明度
    val TertiaryLabelLight = Color(0x4C3C3C43)        // 30% 不透明度 (十进制 76)
    val QuaternaryLabelLight = Color(0x2E3C3C43)      // 18% 不透明度
    val PlaceholderTextLight = Color(0xFFC7C7CC)
    val SystemFillLight = Color(0x33787880)           // 20% 不透明度
    val SecondarySystemFillLight = Color(0x29787880)  // 16% 不透明度
    val TertiarySystemFillLight = Color(0x1F787880)   // 12% 不透明度
    val QuaternarySystemFillLight = Color(0x14787880) // 8% 不透明度
    val SeparatorLight = Color(0x57545456)            // 34% 不透明度
    val OpaqueSeparatorLight = Color(0xFFC6C6C8)
    val LinkLight = Color(0xFF007AFF)

    // ==========================================
    // 2. 语义化界面层级与文本 - 深色模式 (Dark Mode)
    // ==========================================
    val SystemBackgroundDark = Color(0xFF000000)
    val SecondarySystemBackgroundDark = Color(0xFF1C1C1E)
    val TertiarySystemBackgroundDark = Color(0xFF2C2C2E)
    val SystemGroupedBackgroundDark = Color(0xFF000000)
    val SecondarySystemGroupedBackgroundDark = Color(0xFF1C1C1E)
    val TertiarySystemGroupedBackgroundDark = Color(0xFF2C2C2E)
    val LabelDark = Color(0xFFFFFFFF)
    val SecondaryLabelDark = Color(0x99EBEBF5)       // 60% 不透明度
    val TertiaryLabelDark = Color(0x4CEBEBF5)        // 30% 不透明度 (十进制 76)
    val QuaternaryLabelDark = Color(0x2EEBEBF5)      // 18% 不透明度
    val PlaceholderTextDark = Color(0xFF7A7A7A)
    val SystemFillDark = Color(0x5C787880)           // 36% 不透明度
    val SecondarySystemFillDark = Color(0x52787880)  // 32% 不透明度
    val TertiarySystemFillDark = Color(0x3D787880)   // 24% 不透明度
    val QuaternarySystemFillDark = Color(0x30787880) // 19% 不透明度
    val SeparatorDark = Color(0x99545456)            // 60% 不透明度
    val OpaqueSeparatorDark = Color(0xFF38383A)
    val LinkDark = Color(0xFF0A84FF)

    // ==========================================
    // 3. 无障碍高对比度主色 - 浅色模式 (Light Mode)
    // ==========================================
    val AccessibleRedLight = Color(0xFFD70015)
    val AccessibleOrangeLight = Color(0xFFC93400)
    val AccessibleYellowLight = Color(0xFFB25000)
    val AccessibleGreenLight = Color(0xFF248A3D)
    val AccessibleMintLight = Color(0xFF0C817B)
    val AccessibleTealLight = Color(0xFF008299)
    val AccessibleCyanLight = Color(0xFF0071A4)
    val AccessibleBlueLight = Color(0xFF0040DD)
    val AccessibleIndigoLight = Color(0xFF3634A3)
    val AccessiblePurpleLight = Color(0xFF8944AB)
    val AccessiblePinkLight = Color(0xFFD30F45)
    val AccessibleBrownLight = Color(0xFF7F6545)
    val AccessibleGrayLight = Color(0xFF6C6C70)
    val AccessibleGray2Light = Color(0xFF8E8E93)
    val AccessibleGray3Light = Color(0xFFAEAEB2)
    val AccessibleGray4Light = Color(0xFFBCBCC0)
    val AccessibleGray5Light = Color(0xFFD8D8DC)
    val AccessibleGray6Light = Color(0xFFEBEBF0)

    // ==========================================
    // 3. 无障碍高对比度主色 - 深色模式 (Dark Mode)
    // ==========================================
    val AccessibleRedDark = Color(0xFFFF6961)
    val AccessibleOrangeDark = Color(0xFFFFB340)
    val AccessibleYellowDark = Color(0xFFFFD426)
    val AccessibleGreenDark = Color(0xFF30DB5B)
    val AccessibleMintDark = Color(0xFF66D4CF)
    val AccessibleTealDark = Color(0xFF5DE6FF)
    val AccessibleCyanDark = Color(0xFF70D7FF)
    val AccessibleBlueDark = Color(0xFF409CFF)
    val AccessibleIndigoDark = Color(0xFF7D7AFF)
    val AccessiblePurpleDark = Color(0xFFDA8FFF)
    val AccessiblePinkDark = Color(0xFFFF6482)
    val AccessibleBrownDark = Color(0xFFB59469)
    val AccessibleGrayDark = Color(0xFFAEAEB2)
    val AccessibleGray2Dark = Color(0xFF7C7C80)
    val AccessibleGray3Dark = Color(0xFF545456)
    val AccessibleGray4Dark = Color(0xFF444446)
    val AccessibleGray5Dark = Color(0xFF363638)
    val AccessibleGray6Dark = Color(0xFF242426)

    // ==========================================
    // 4. 桌面辅助主色 (Desktop Variants)
    // ==========================================
    val DesktopGreenLight = Color(0xFF28CD41)
    val DesktopTealLight = Color(0xFF59ADC4) // 已纠正原库 #59ADCC 笔误
    val DesktopCyanLight = Color(0xFF55BEF0)
    val DesktopGrayLight = Color(0xFF8E8E93)

    val DesktopGreenDark = Color(0xFF32D74B)
    val DesktopTealDark = Color(0xFF6AC4DC)
    val DesktopCyanDark = Color(0xFF5AC8F5)
    val DesktopGrayDark = Color(0xFF98989D) // 补齐桌面深色灰差异项

    // ==========================================
    // 5. 空间与氛围色彩 (Spatial Palette - 静态单模式)
    // ==========================================
    val SpatialRed = Color(0xFFFF453A)
    val SpatialOrange = Color(0xFFFF9F0A)
    val SpatialYellow = Color(0xFFFFD60A)
    val SpatialGreen = Color(0xFF32D74B)
    val SpatialMint = Color(0xFF66D4CF)
    val SpatialTeal = Color(0xFF6AC4DC)
    val SpatialCyan = Color(0xFF5AC8F5)
    val SpatialBlue = Color(0xFF0A84FF)
    val SpatialIndigo = Color(0xFF5E5CE6)
    val SpatialPurple = Color(0xFFBF5AF2)
    val SpatialPink = Color(0xFFFF375F)
    val SpatialBrown = Color(0xFFAC8E68)
    val SpatialGray = Color(0xFF98989D)
}

/**
 * 完整主题调色板数据类，支持深浅模式动态切换与无障碍扩展
 */
@Immutable
data class AppPalette(
    val isDark: Boolean,

    // 系统主色 (System Tints)
    val systemRed: Color,
    val systemOrange: Color,
    val systemYellow: Color,
    val systemGreen: Color,
    val systemMint: Color,
    val systemTeal: Color,
    val systemCyan: Color,
    val systemBlue: Color,
    val systemIndigo: Color,
    val systemPurple: Color,
    val systemPink: Color,
    val systemBrown: Color,
    val systemGray: Color,
    val systemGray2: Color,
    val systemGray3: Color,
    val systemGray4: Color,
    val systemGray5: Color,
    val systemGray6: Color,

    // 语义层级背景与文本 (UI Elements & Backgrounds)
    val systemBackground: Color,
    val secondarySystemBackground: Color,
    val tertiarySystemBackground: Color,
    val systemGroupedBackground: Color,
    val secondarySystemGroupedBackground: Color,
    val tertiarySystemGroupedBackground: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    val placeholderText: Color,
    val systemFill: Color,
    val secondarySystemFill: Color,
    val tertiarySystemFill: Color,
    val quaternarySystemFill: Color,
    val separator: Color,
    val opaqueSeparator: Color,
    val link: Color,

    // 无障碍高对比度主色 (Accessible Variants)
    val accessibleRed: Color,
    val accessibleOrange: Color,
    val accessibleYellow: Color,
    val accessibleGreen: Color,
    val accessibleMint: Color,
    val accessibleTeal: Color,
    val accessibleCyan: Color,
    val accessibleBlue: Color,
    val accessibleIndigo: Color,
    val accessiblePurple: Color,
    val accessiblePink: Color,
    val accessibleBrown: Color,
    val accessibleGray: Color,
    val accessibleGray2: Color,
    val accessibleGray3: Color,
    val accessibleGray4: Color,
    val accessibleGray5: Color,
    val accessibleGray6: Color,

    // 桌面辅助主色 (Desktop Variants)
    val desktopGreen: Color,
    val desktopTeal: Color,
    val desktopCyan: Color,
    val desktopGray: Color,

    // 空间氛围主色 (Spatial Tints - 静态单模式映射)
    val spatialRed: Color = AppTokens.SpatialRed,
    val spatialOrange: Color = AppTokens.SpatialOrange,
    val spatialYellow: Color = AppTokens.SpatialYellow,
    val spatialGreen: Color = AppTokens.SpatialGreen,
    val spatialMint: Color = AppTokens.SpatialMint,
    val spatialTeal: Color = AppTokens.SpatialTeal,
    val spatialCyan: Color = AppTokens.SpatialCyan,
    val spatialBlue: Color = AppTokens.SpatialBlue,
    val spatialIndigo: Color = AppTokens.SpatialIndigo,
    val spatialPurple: Color = AppTokens.SpatialPurple,
    val spatialPink: Color = AppTokens.SpatialPink,
    val spatialBrown: Color = AppTokens.SpatialBrown,
    val spatialGray: Color = AppTokens.SpatialGray,
)

val LightAppPalette = AppPalette(
    isDark = false,
    systemRed = AppTokens.SystemRedLight,
    systemOrange = AppTokens.SystemOrangeLight,
    systemYellow = AppTokens.SystemYellowLight,
    systemGreen = AppTokens.SystemGreenLight,
    systemMint = AppTokens.SystemMintLight,
    systemTeal = AppTokens.SystemTealLight,
    systemCyan = AppTokens.SystemCyanLight,
    systemBlue = AppTokens.SystemBlueLight,
    systemIndigo = AppTokens.SystemIndigoLight,
    systemPurple = AppTokens.SystemPurpleLight,
    systemPink = AppTokens.SystemPinkLight,
    systemBrown = AppTokens.SystemBrownLight,
    systemGray = AppTokens.SystemGrayLight,
    systemGray2 = AppTokens.SystemGray2Light,
    systemGray3 = AppTokens.SystemGray3Light,
    systemGray4 = AppTokens.SystemGray4Light,
    systemGray5 = AppTokens.SystemGray5Light,
    systemGray6 = AppTokens.SystemGray6Light,

    systemBackground = AppTokens.SystemBackgroundLight,
    secondarySystemBackground = AppTokens.SecondarySystemBackgroundLight,
    tertiarySystemBackground = AppTokens.TertiarySystemBackgroundLight,
    systemGroupedBackground = AppTokens.SystemGroupedBackgroundLight,
    secondarySystemGroupedBackground = AppTokens.SecondarySystemGroupedBackgroundLight,
    tertiarySystemGroupedBackground = AppTokens.TertiarySystemGroupedBackgroundLight,
    label = AppTokens.LabelLight,
    secondaryLabel = AppTokens.SecondaryLabelLight,
    tertiaryLabel = AppTokens.TertiaryLabelLight,
    quaternaryLabel = AppTokens.QuaternaryLabelLight,
    placeholderText = AppTokens.PlaceholderTextLight,
    systemFill = AppTokens.SystemFillLight,
    secondarySystemFill = AppTokens.SecondarySystemFillLight,
    tertiarySystemFill = AppTokens.TertiarySystemFillLight,
    quaternarySystemFill = AppTokens.QuaternarySystemFillLight,
    separator = AppTokens.SeparatorLight,
    opaqueSeparator = AppTokens.OpaqueSeparatorLight,
    link = AppTokens.LinkLight,

    accessibleRed = AppTokens.AccessibleRedLight,
    accessibleOrange = AppTokens.AccessibleOrangeLight,
    accessibleYellow = AppTokens.AccessibleYellowLight,
    accessibleGreen = AppTokens.AccessibleGreenLight,
    accessibleMint = AppTokens.AccessibleMintLight,
    accessibleTeal = AppTokens.AccessibleTealLight,
    accessibleCyan = AppTokens.AccessibleCyanLight,
    accessibleBlue = AppTokens.AccessibleBlueLight,
    accessibleIndigo = AppTokens.AccessibleIndigoLight,
    accessiblePurple = AppTokens.AccessiblePurpleLight,
    accessiblePink = AppTokens.AccessiblePinkLight,
    accessibleBrown = AppTokens.AccessibleBrownLight,
    accessibleGray = AppTokens.AccessibleGrayLight,
    accessibleGray2 = AppTokens.AccessibleGray2Light,
    accessibleGray3 = AppTokens.AccessibleGray3Light,
    accessibleGray4 = AppTokens.AccessibleGray4Light,
    accessibleGray5 = AppTokens.AccessibleGray5Light,
    accessibleGray6 = AppTokens.AccessibleGray6Light,

    desktopGreen = AppTokens.DesktopGreenLight,
    desktopTeal = AppTokens.DesktopTealLight,
    desktopCyan = AppTokens.DesktopCyanLight,
    desktopGray = AppTokens.DesktopGrayLight,
)

val DarkAppPalette = AppPalette(
    isDark = true,
    systemRed = AppTokens.SystemRedDark,
    systemOrange = AppTokens.SystemOrangeDark,
    systemYellow = AppTokens.SystemYellowDark,
    systemGreen = AppTokens.SystemGreenDark,
    systemMint = AppTokens.SystemMintDark,
    systemTeal = AppTokens.SystemTealDark,
    systemCyan = AppTokens.SystemCyanDark,
    systemBlue = AppTokens.SystemBlueDark,
    systemIndigo = AppTokens.SystemIndigoDark,
    systemPurple = AppTokens.SystemPurpleDark,
    systemPink = AppTokens.SystemPinkDark,
    systemBrown = AppTokens.SystemBrownDark,
    systemGray = AppTokens.SystemGrayDark,
    systemGray2 = AppTokens.SystemGray2Dark,
    systemGray3 = AppTokens.SystemGray3Dark,
    systemGray4 = AppTokens.SystemGray4Dark,
    systemGray5 = AppTokens.SystemGray5Dark,
    systemGray6 = AppTokens.SystemGray6Dark,

    systemBackground = AppTokens.SystemBackgroundDark,
    secondarySystemBackground = AppTokens.SecondarySystemBackgroundDark,
    tertiarySystemBackground = AppTokens.TertiarySystemBackgroundDark,
    systemGroupedBackground = AppTokens.SystemGroupedBackgroundDark,
    secondarySystemGroupedBackground = AppTokens.SecondarySystemGroupedBackgroundDark,
    tertiarySystemGroupedBackground = AppTokens.TertiarySystemGroupedBackgroundDark,
    label = AppTokens.LabelDark,
    secondaryLabel = AppTokens.SecondaryLabelDark,
    tertiaryLabel = AppTokens.TertiaryLabelDark,
    quaternaryLabel = AppTokens.QuaternaryLabelDark,
    placeholderText = AppTokens.PlaceholderTextDark,
    systemFill = AppTokens.SystemFillDark,
    secondarySystemFill = AppTokens.SecondarySystemFillDark,
    tertiarySystemFill = AppTokens.TertiarySystemFillDark,
    quaternarySystemFill = AppTokens.QuaternarySystemFillDark,
    separator = AppTokens.SeparatorDark,
    opaqueSeparator = AppTokens.OpaqueSeparatorDark,
    link = AppTokens.LinkDark,

    accessibleRed = AppTokens.AccessibleRedDark,
    accessibleOrange = AppTokens.AccessibleOrangeDark,
    accessibleYellow = AppTokens.AccessibleYellowDark,
    accessibleGreen = AppTokens.AccessibleGreenDark,
    accessibleMint = AppTokens.AccessibleMintDark,
    accessibleTeal = AppTokens.AccessibleTealDark,
    accessibleCyan = AppTokens.AccessibleCyanDark,
    accessibleBlue = AppTokens.AccessibleBlueDark,
    accessibleIndigo = AppTokens.AccessibleIndigoDark,
    accessiblePurple = AppTokens.AccessiblePurpleDark,
    accessiblePink = AppTokens.AccessiblePinkDark,
    accessibleBrown = AppTokens.AccessibleBrownDark,
    accessibleGray = AppTokens.AccessibleGrayDark,
    accessibleGray2 = AppTokens.AccessibleGray2Dark,
    accessibleGray3 = AppTokens.AccessibleGray3Dark,
    accessibleGray4 = AppTokens.AccessibleGray4Dark,
    accessibleGray5 = AppTokens.AccessibleGray5Dark,
    accessibleGray6 = AppTokens.AccessibleGray6Dark,

    desktopGreen = AppTokens.DesktopGreenDark,
    desktopTeal = AppTokens.DesktopTealDark,
    desktopCyan = AppTokens.DesktopCyanDark,
    desktopGray = AppTokens.DesktopGrayDark,
)

val LocalAppColors = staticCompositionLocalOf { LightAppPalette }

object AppTheme {
    val colors: AppPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkAppPalette else LightAppPalette
    CompositionLocalProvider(LocalAppColors provides palette) {
        content()
    }
}

/**
 * 经典纯色调色板 (79 款官方配件与表盘单色，无深浅分级)
 */
object ClassicSwatches {
    val White = Color(0xFFFFFFFF)
    val Red = Color(0xFFDF1125)
    val NeonPink = Color(0xFFFB212F)
    val ElectricPink = Color(0xFFFC3644)
    val Hibiscus = Color(0xFFFB0049)
    val Pink = Color(0xFFFD4154)
    val PinkCitrus = Color(0xFFEA383A)
    val DragonFruit = Color(0xFFF12E6D)
    val Pomegranate = Color(0xFFD8034F) // 纠正原库 Pomengrate 拼写
    val Camellia = Color(0xFFBF3138)
    val RedRose = Color(0xFFAB1438)
    val Plum = Color(0xFF81323D)
    val Orange = Color(0xFFFC4E12)
    val Clementine = Color(0xFFFD513B)
    val Apricot = Color(0xFFFC5C42)
    val Papaya = Color(0xFFFD7036)
    val Kumquat = Color(0xFFFD7447)
    val LightOrange = Color(0xFFFD820B)
    val Peach = Color(0xFFE0694E)
    val Flamingo = Color(0xFFCA6F59)
    val RoseGold = Color(0xFFE99475)
    val PinkSand = Color(0xFFFEB69C)
    val LightPink = Color(0xFFF19F9D)
    val VintageRose = Color(0xFFF29C98)
    val GrapeFruit = Color(0xFFFDA27E)
    val Cream = Color(0xFFFFE0AB)
    val MellowYellow = Color(0xFFF7F6AF)
    val CanaryYellow = Color(0xFFFFDA3A)
    val Pollen = Color(0xFFFECB4D)
    val Yellow = Color(0xFFE8C511)
    val FlashLight = Color(0xFFFAED0B)
    val LemonCream = Color(0xFFFFEB6D)
    val Flash = Color(0xFFDFF81E)
    val Green = Color(0xFF80E220)
    val SpearMint = Color(0xFF77EA7E)
    val Mint = Color(0xFFA2EC8E)
    val Beryl = Color(0xFFD0F2B1)
    val SeaFoam = Color(0xFFCFF1D6)
    val Turquoise = Color(0xFF91CEC2)
    val Blue = Color(0xFF1EA5FC)
    val LightBlue = Color(0xFF5BEAD9)
    val Cerulean = Color(0xFF87B9E7) // 纠正原库 Cerulian 拼写
    val SurfBlue = Color(0xFF1871AC)
    val PacificGreen = Color(0xFF0F6D8E)
    val BlueCobalt = Color(0xFF3A6B8E)
    val DarkTeal = Color(0xFF2C6184)
    val BlueHorizon = Color(0xFF1E538F)
    val DenimBlue = Color(0xFF4B709A)
    val LinenBlue = Color(0xFF455785)
    val AlaskanBlue = Color(0xFF525C73)
    val DeepNavy = Color(0xFF3A4565)
    val MidnightBlue = Color(0xFF383B65)
    val Purple = Color(0xFF8962F8)
    val UltraViolet = Color(0xFF5F41B2)
    val Lilac = Color(0xFFA990DD)
    val OceanBlue = Color(0xFF6273BD)
    val DelftBlue = Color(0xFF4662B2)
    val Indigo = Color(0xFF474E95)
    val LavenderGray = Color(0xFF757397) // 纠正原库 LavendarGrey 拼写
    val Lavender = Color(0xFFA58998)
    val Cocoa = Color(0xFF8B7D7D)
    val StormGrey = Color(0xFF578887)
    val Cactus = Color(0xFF5B7971)
    val PineGreen = Color(0xFF4E6C54)
    val CyprusGreen = Color(0xFF525D49)
    val NorthernBlue = Color(0xFF638A8D)
    val Azure = Color(0xFF788991)
    val MistBlue = Color(0xFFA7AB99)
    val DarkOlive = Color(0xFF7C7A66)
    val Khaki = Color(0xFF7A6E49)
    val SoftWhite = Color(0xFFD8C9AF)
    val AntiqueWhite = Color(0xFFCDA986)
    val YellowGold = Color(0xFFCC996D)
    val Gold = Color(0xFFB08053)
    val Camel = Color(0xFFA97D4F)
    val Walnut = Color(0xFFA17455)
    val Stone = Color(0xFFA28872)
    val Pebble = Color(0xFF9F8D7E)
    val CoastalGrey = Color(0xFF715D50)

    // 向前兼容历史笔误属性别名
    @Deprecated("拼写笔误纠正，请使用 Pomegranate", ReplaceWith("Pomegranate"))
    val Pomengrate: Color get() = Pomegranate

    @Deprecated("拼写笔误纠正，请使用 Cerulean", ReplaceWith("Cerulean"))
    val Cerulian: Color get() = Cerulean

    @Deprecated("拼写笔误纠正，请使用 LavenderGray", ReplaceWith("LavenderGray"))
    val LavendarGrey: Color get() = LavenderGray
}
