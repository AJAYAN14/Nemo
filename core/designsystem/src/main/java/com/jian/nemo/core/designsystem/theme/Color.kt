package com.jian.nemo.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Material 3 基础色板 (Tokens)
// ============================================================================

// Light Theme Colors
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)

val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val BackgroundLight = Color(0xFFFFFBFE)
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFBFE)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)

val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4D0)

// Dark Theme Colors
val PrimaryDark = Color(0xFFD0BCFF)
val OnPrimaryDark = Color(0xFF381E72)
val PrimaryContainerDark = Color(0xFF4F378B)
val OnPrimaryContainerDark = Color(0xFFEADDFF)

val SecondaryDark = Color(0xFFCCC2DC)
val OnSecondaryDark = Color(0xFF332D41)
val SecondaryContainerDark = Color(0xFF4A4458)
val OnSecondaryContainerDark = Color(0xFFE8DEF8)

val TertiaryDark = Color(0xFFEFB8C8)
val OnTertiaryDark = Color(0xFF492532)
val TertiaryContainerDark = Color(0xFF633B48)
val OnTertiaryContainerDark = Color(0xFFFFD8E4)

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

val BackgroundDark = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)

val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)

// ============================================================================
// Nemo 品牌语义色彩 (Semantic Colors)
// ============================================================================

val NemoPrimary = Color(0xFF0E68FF)          // 主题蓝色
val NemoSecondary = Color(0xFF4CAF50)        // 辅助绿色
val NemoText = Color(0xFF333333)             // 主文本颜色
val NemoTextLight = Color(0xFF888888)        // 浅文本颜色
val NemoDanger = Color(0xFFE53935)           // 危险/错误色
val NemoWarning = Color(0xFFFBC02D)          // 警告色
val NemoGold = Color(0xFFFFD700)             // 金色/高亮色

// 测试结算页原始配色（集中管理）
object TestResultPalette {
    val CorrectCardBgDark = Color(0xFF0B5F49)
    val CorrectCardBgLight = Color(0xFFE8F5E9)
    val CorrectCardContentDark = Color(0xFF6EE7B7)
    val CorrectCardContentLight = Color(0xFF2E7D32)

    val WrongCardBgDark = Color(0xFF922626)
    val WrongCardBgLight = Color(0xFFFEE2E2)
    val WrongCardContentDark = Color(0xFFFCA5A5)
    val WrongCardContentLight = Color(0xFFB91C1C)

    val TimeCardBgDark = Color(0xFF8A4318)
    val TimeCardBgLight = Color(0xFFFFF8E1)
    val TimeCardContentDark = Color(0xFFFCD34D)
    val TimeCardContentLight = Color(0xFFF57F17)

    val DistributionCardBgDark = Color(0xFF263548)
    val DistributionCardBgLight = Color(0xFFFFFFFF)
    val DistributionTitleDark = Color(0xFFF8FAFC)
    val DistributionTitleLight = Color(0xFF1E293B)

    val WordAccentDark = Color(0xFF60A5FA)
    val WordAccentLight = Color(0xFF3B82F6)
    val GrammarAccentDark = Color(0xFFA78BFA)
    val GrammarAccentLight = Color(0xFF8B5CF6)
    val SecondaryTextDark = Color(0xFFA5B4C6)
    val SecondaryTextLight = Color(0xFF64748B)
    val DividerDark = Color(0xFF475569)
    val DividerLight = Color(0xFFCBD5E1)
}

// ==============================================
// 主题接口定义
// ==============================================
interface NemoTheme {
    val Background: Color
    val CardBg: Color
    val TitleText: Color
    val DescText: Color
    val IconBg: Color
    val Primary: Color
    val Secondary: Color
    val Tertiary: Color
    val Accent: Color
}

// ==============================================
// 主题 1：陶土棕 (Clay Earth) - 温暖质感
// ==============================================

object ClayTheme : NemoTheme {
    override val Background = Color(0xFFFFF7ED)  // 整体大背景 (淡奶油色) (注意：极浅，仅作背景用)
    override val CardBg = Color(0xFFFFFFFF)      // 卡片背景 (纯白)
    override val TitleText = Color(0xFF44403C)   // 标题文字 (深灰褐)
    override val DescText = Color(0xFF78716C)    // 副标题/描述文字 (中灰褐)
    override val IconBg = Color(0xFFFFEDD5)      // 图标背景圆圈 (淡橙色)
    
    override val Primary = Color(0xFFEA580C)     // 主色调 (深焦糖色)
    override val Secondary = Color(0xFFD97706)   // 辅助色 (琥珀金)
    override val Tertiary = Color(0xFFEF4444)    // 点缀色 (柔红)
    
    override val Accent = Color(0xFFF97316)      // 选中状态边框/指示器 (活力橙)
}

// ==============================================
// 主题 2：蔷薇粉 (Rose Quartz) - 温婉优雅
// ==============================================

object RoseTheme : NemoTheme {
    override val Background = Color(0xFFFFF1F2)  // 整体大背景 (极淡粉白) (注意：极浅，仅作背景用)
    override val CardBg = Color(0xFFFFFFFF)      // 卡片背景 (纯白)
    override val TitleText = Color(0xFF1E293B)   // 标题文字 (深蓝灰)
    override val DescText = Color(0xFF64748B)    // 副标题/描述文字 (中蓝灰)
    override val IconBg = Color(0xFFFFE4E6)      // 图标背景圆圈 (淡玫瑰色)
    
    override val Primary = Color(0xFFF43F5E)     // 主色调 (玫瑰红)
    override val Secondary = Color(0xFFEC4899)   // 辅助色 (亮粉色)
    override val Tertiary = Color(0xFFF87171)    // 点缀色 (珊瑚红)
    
    override val Accent = Color(0xFFFB7185)      // 选中状态边框/指示器 (柔粉色)
}

// ==============================================
// 主题 3：海洋蓝 (Ocean Blue) - 沉稳智慧
// ==============================================
object OceanTheme : NemoTheme {
    override val Background = Color(0xFFF0F9FF)  // 整体大背景 (天青白)
    override val CardBg = Color(0xFFFFFFFF)      // 卡片背景 (纯白)
    override val TitleText = Color(0xFF0C4A6E)   // 标题文字 (深海蓝)
    override val DescText = Color(0xFF0369A1)    // 副标题/描述文字 (中海蓝)
    override val IconBg = Color(0xFFE0F2FE)      // 图标背景圆圈 (浅天蓝)
    
    override val Primary = Color(0xFF0284C7)     // 主色调 (海洋蓝)
    override val Secondary = Color(0xFF38BDF8)   // 辅助色 (天空蓝)
    override val Tertiary = Color(0xFF0EA5E9)    // 点缀色 (亮天蓝)
    
    override val Accent = Color(0xFF0284C7)      // 选中状态边框/指示器 (海洋蓝)
}

// ==============================================
// 主题 4：森林绿 (Forest Green) - 生机勃勃
// ==============================================
object ForestTheme : NemoTheme {
    override val Background = Color(0xFFF0FDF4)  // 整体大背景 (薄荷白)
    override val CardBg = Color(0xFFFFFFFF)      // 卡片背景 (纯白)
    override val TitleText = Color(0xFF14532D)   // 标题文字 (深森林绿)
    override val DescText = Color(0xFF15803D)    // 副标题/描述文字 (中绿)
    override val IconBg = Color(0xFFDCFCE7)      // 图标背景圆圈 (嫩绿)
    
    override val Primary = Color(0xFF16A34A)     // 主色调 (翠绿)
    override val Secondary = Color(0xFF4ADE80)   // 辅助色 (草绿)
    override val Tertiary = Color(0xFF22C55E)    // 点缀色 (亮绿)
    
    override val Accent = Color(0xFF16A34A)      // 选中状态边框/指示器 (翠绿)
}

// ==============================================
// 主题 5：薰衣草紫 (Lavender) - 优雅神秘
// ==============================================
object LavenderTheme : NemoTheme {
    override val Background = Color(0xFFF5F3FF)  // 整体大背景 (淡紫白)
    override val CardBg = Color(0xFFFFFFFF)      // 卡片背景 (纯白)
    override val TitleText = Color(0xFF4C1D95)   // 标题文字 (深紫)
    override val DescText = Color(0xFF6D28D9)    // 副标题/描述文字 (中紫)
    override val IconBg = Color(0xFFEDE9FE)      // 图标背景圆圈 (浅紫)
    
    override val Primary = Color(0xFF7C3AED)     // 主色调 (紫罗兰)
    override val Secondary = Color(0xFFA78BFA)   // 辅助色 (淡紫)
    override val Tertiary = Color(0xFF8B5CF6)    // 点缀色 (亮紫)
    
    override val Accent = Color(0xFF7C3AED)      // 选中状态边框/指示器 (紫罗兰)
}

// 容器与表面语义 (重命名自原有的 Learning/Test/App 专用色)
val NemoSurfaceBackground = Color(0xFFF4F6F9)   // 语义化的页面背景
val NemoSurfaceCard = Color(0xFFFFFFFF)         // 语义化的卡片容器
val NemoSurfaceBorder = Color(0xFFE0E0E0)       // 语义化的边框颜色

val NemoSurfaceBackgroundDark = Color(0xFF1C1B1F) // 深色页面背景
val NemoSurfaceCardDark = Color(0xFF2B2930)       // 深色卡片容器

// Rating Guide Screen (HTML restore)
val RatingGuideTitleText = Color(0xFF0F172A)
val RatingGuideCoreText = Color(0xFF475569)
val RatingGuideBodyText = Color(0xFF334155)

val RatingGuideBadgeRoseText = Color(0xFFE11D48)
val RatingGuideBadgeRoseBg = Color(0xFFFFE4E6)
val RatingGuideBadgeRoseTextDark = Color(0xFFFDA4AF)
val RatingGuideBadgeRoseBgDark = Color(0xFF4C0519)
val RatingGuideBadgeOrangeText = Color(0xFFEA580C)
val RatingGuideBadgeOrangeBg = Color(0xFFFFEDD5)
val RatingGuideBadgeOrangeTextDark = Color(0xFFFDBA74)
val RatingGuideBadgeOrangeBgDark = Color(0xFF7C2D12)
val RatingGuideBadgeBlueText = Color(0xFF2563EB)
val RatingGuideBadgeBlueBg = Color(0xFFDBEAFE)
val RatingGuideBadgeBlueTextDark = Color(0xFF93C5FD)
val RatingGuideBadgeBlueBgDark = Color(0xFF1E3A8A)
val RatingGuideBadgeEmeraldText = Color(0xFF059669)
val RatingGuideBadgeEmeraldBg = Color(0xFFD1FAE5)
val RatingGuideBadgeEmeraldTextDark = Color(0xFF6EE7B7)
val RatingGuideBadgeEmeraldBgDark = Color(0xFF064E3B)

val RatingGuideAdviceBg = Color(0xFFEEF2FF)
val RatingGuideAdviceText = Color(0xFF4338CA)

val RatingGuidePrimaryButton = Color(0xFF4F46E5)

// ============================================================================
// 弃用别名 (Legacy Aliases) - 用于维持向后兼容性
// ============================================================================

@Deprecated("Use NemoSurfaceBackground", ReplaceWith("NemoSurfaceBackground"))
val LearningScreenBackground = NemoSurfaceBackground
@Deprecated("Use NemoSurfaceBackground", ReplaceWith("NemoSurfaceBackground"))
val TestScreenBg = NemoSurfaceBackground

@Deprecated("Use NemoSurfaceCard", ReplaceWith("NemoSurfaceCard"))
val LearningCardBackground = NemoSurfaceCard
@Deprecated("Use NemoSurfaceCard", ReplaceWith("NemoSurfaceCard"))
val TestCardBg = NemoSurfaceCard

@Deprecated("Use NemoPrimary", ReplaceWith("NemoPrimary"))
val TestPrimary = NemoPrimary
@Deprecated("Use NemoText", ReplaceWith("NemoText"))
val TestText = NemoText

@Deprecated("Use Color(0xFFE6F0FF)")
val NemoBlueLight = Color(0xFFE6F0FF)
@Deprecated("Use NemoSurfaceBorder", ReplaceWith("NemoSurfaceBorder"))
val NemoBorderColor = NemoSurfaceBorder
@Deprecated("Use NemoPrimary", ReplaceWith("NemoPrimary"))
val NemoBlue = NemoPrimary

// Standardized Nemo Colors (Aliased to IosColors for consistency)
val NemoOrange = IosColors.Orange
val NemoTeal = IosColors.Teal
val NemoPurple = IosColors.Purple
val NemoIndigo = IosColors.Indigo
val NemoRed = IosColors.Red
val NemoCyan = IosColors.Cyan

@Deprecated("Use NemoSurfaceBackgroundDark", ReplaceWith("NemoSurfaceBackgroundDark"))
val LearningScreenBackgroundDark = NemoSurfaceBackgroundDark
@Deprecated("Use NemoSurfaceCardDark", ReplaceWith("NemoSurfaceCardDark"))
val LearningCardBackgroundDark = NemoSurfaceCardDark

@Deprecated("Use NemoSecondary", ReplaceWith("NemoSecondary"))
val NemoGreen = NemoSecondary

// iOS 风格色彩别名 (重定向至 IosColors 对象)
@Deprecated("Use IosColors.Red", ReplaceWith("IosColors.Red"))
val iOSRed = IosColors.Red
@Deprecated("Use IosColors.Orange", ReplaceWith("IosColors.Orange"))
val iOSOrange = IosColors.Orange
@Deprecated("Use IosColors.Green", ReplaceWith("IosColors.Green"))
val iOSGreen = IosColors.Green
@Deprecated("Use IosColors.Teal", ReplaceWith("IosColors.Teal"))
val iOSTeal = IosColors.Teal
@Deprecated("Use IosColors.Blue", ReplaceWith("IosColors.Blue"))
val iOSBlue = IosColors.Blue
@Deprecated("Use IosColors.Indigo", ReplaceWith("IosColors.Indigo"))
val iOSIndigo = IosColors.Indigo
@Deprecated("Use IosColors.Purple", ReplaceWith("IosColors.Purple"))
val iOSPurple = IosColors.Purple
@Deprecated("Use IosColors.Gray", ReplaceWith("IosColors.Gray"))
val iOSGray = IosColors.Gray
@Deprecated("Use IosColors.Cyan", ReplaceWith("IosColors.Cyan"))
val iOSCyan = IosColors.Cyan

// 深色模式变体
@Deprecated("Use IosColors.RedDark", ReplaceWith("IosColors.RedDark"))
val iOSRedDark = IosColors.RedDark
@Deprecated("Use IosColors.OrangeDark", ReplaceWith("IosColors.OrangeDark"))
val iOSOrangeDark = IosColors.OrangeDark
@Deprecated("Use IosColors.GreenDark", ReplaceWith("IosColors.GreenDark"))
val iOSGreenDark = IosColors.GreenDark
@Deprecated("Use IosColors.TealDark", ReplaceWith("IosColors.TealDark"))
val iOSTealDark = IosColors.TealDark
@Deprecated("Use IosColors.BlueDark", ReplaceWith("IosColors.BlueDark"))
val iOSBlueDark = IosColors.BlueDark
@Deprecated("Use IosColors.IndigoDark", ReplaceWith("IosColors.IndigoDark"))
val iOSIndigoDark = IosColors.IndigoDark
@Deprecated("Use IosColors.PurpleDark", ReplaceWith("IosColors.PurpleDark"))
val iOSPurpleDark = IosColors.PurpleDark
@Deprecated("Use IosColors.GrayDark", ReplaceWith("IosColors.GrayDark"))
val iOSGrayDark = IosColors.GrayDark
@Deprecated("Use IosColors.CyanDark", ReplaceWith("IosColors.CyanDark"))
val iOSCyanDark = IosColors.CyanDark

// Chart 色彩别名 (重定向至 ChartColors 对象)
@Deprecated("Use ChartColors.FreshRed", ReplaceWith("ChartColors.FreshRed"))
val FreshRed = ChartColors.FreshRed
@Deprecated("Use ChartColors.FreshBlue", ReplaceWith("ChartColors.FreshBlue"))
val FreshBlue = ChartColors.FreshBlue
@Deprecated("Use ChartColors.FreshGreen", ReplaceWith("ChartColors.FreshGreen"))
val FreshGreen = ChartColors.FreshGreen


// ============================================================================
// 辅助色彩 (Utility Colors)
// ============================================================================

// 图表色彩规范
object ChartColors {
    val FreshRed = Color(0xFFEF5350)
    val FreshPink = Color(0xFFEC407A)
    val FreshPurple = Color(0xFFAB47BC)
    val FreshBlue = Color(0xFF42A5F5)
    val FreshTeal = Color(0xFF26A69A)
    val FreshGreen = Color(0xFF66BB6A)
    val FreshLime = Color(0xFF9CCC65)
    val FreshYellow = Color(0xFFFFCA28)
    val FreshOrange = Color(0xFFFFA726)
}

// iOS 视觉风格色彩 (已隔离，不再全局直接建议使用)
object IosColors {
    val Red = Color(0xFFFF3B30)
    val Orange = Color(0xFFFF9500)
    val Green = Color(0xFF34C759)
    val Teal = Color(0xFF5AC8FA)
    val Blue = Color(0xFF007AFF)
    val Indigo = Color(0xFF5856D6)
    val Purple = Color(0xFFAF52DE)
    val Gray = Color(0xFF8E8E93)
    val Cyan = Color(0xFF32ADE6)
    val Pink = Color(0xFFFF2D55)
    val Yellow = Color(0xFFFFCC00)
    val Mint = Color(0xFF00C7BE)

    // 深色模式变体
    val RedDark = Color(0xFFFF453A)
    val OrangeDark = Color(0xFFFF9F0A)
    val GreenDark = Color(0xFF30D158)
    val TealDark = Color(0xFF64D2FF)
    val BlueDark = Color(0xFF0A84FF)
    val IndigoDark = Color(0xFF5E5CE6)
    val PurpleDark = Color(0xFFBF5AF2)
    val GrayDark = Color(0xFF98989D)
    val CyanDark = Color(0xFF64D2FF)
}


// ============================================================================
// 词性分类色彩 (Category Colors)
// ============================================================================
object NemoCategoryColors {
    // 浅色模式 (Premium Pastel System)
    val CardVerbBgLight = Color(0xFFF0F7FF)     // Azure Blue (更通透)
    val CardVerbTextLight = Color(0xFF007AFF)    // iOS Blue 风格
    val CardAdjIBgLight = Color(0xFFF2FBF2)      // Emerald Soft
    val CardAdjITextLight = Color(0xFF34C759)     // Vibrant Green
    val CardAdjNaBgLight = Color(0xFFFFF7EB)     // Soft Peach
    val CardAdjNaTextLight = Color(0xFFFF9500)    // iOS Orange
    val CardNounBgLight = Color(0xFFF9F5FF)      // Lavender Mist
    val CardNounTextLight = Color(0xFF5856D6)     // iOS Indigo
    val CardAdvBgLight = Color(0xFFF0FAF9)       // Mint Water
    val CardAdvTextLight = Color(0xFF00C7BE)      // iOS Mint
    val CardRentaiBgLight = Color(0xFFF0F9FF)    // Sky Blue Mist (更清爽的蓝)
    val CardRentaiTextLight = Color(0xFF0EA5E9)    // Sky Blue 500
    val CardConjBgLight = Color(0xFFFEFBE8)      // Lemon Chiffon
    val CardConjTextLight = Color(0xFFEAB308)     // Modern Gold
    val CardFixBgLight = Color(0xFFEEF2FF)       // Indigo Wash (柔和靛蓝)
    val CardFixTextLight = Color(0xFF6366F1)      // Indigo 500
    val CardKataBgLight = Color(0xFFFFF5F7)      // Rose Quartz
    val CardKataTextLight = Color(0xFFFF2D55)     // Premium Pink
    val CardIdiomBgLight = Color(0xFFFFF2F2)     // Ruby Blush
    val CardIdiomTextLight = Color(0xFFFF3B30)    // System Red
    val CardKeigoBgLight = Color(0xFFFEF7E6)     // Sand Gold
    val CardKeigoTextLight = Color(0xFFD97706)     // Amber 600
    val CardSoundBgLight = Color(0xFFFBFCF0)     // Lime Fizz
    val CardSoundTextLight = Color(0xFF65A30D)     // Lime 600

    // 深色模式 (Subtle Glow System)
    val CardVerbBgDark = Color(0xFF071A2E)
    val CardVerbTextDark = Color(0xFF64B5F6)
    val CardAdjIBgDark = Color(0xFF091E0F)
    val CardAdjITextDark = Color(0xFF81C784)
    val CardAdjNaBgDark = Color(0xFF2E1A05)
    val CardAdjNaTextDark = Color(0xFFFFB74D)
    val CardNounBgDark = Color(0xFF1A0B2E)
    val CardNounTextDark = Color(0xFFBA68C8)
    val CardAdvBgDark = Color(0xFF051B18)
    val CardAdvTextDark = Color(0xFF4DB6AC)
    val CardRentaiBgDark = Color(0xFF082F49)
    val CardRentaiTextDark = Color(0xFF38BDF8)
    val CardConjBgDark = Color(0xFF1E1605)
    val CardConjTextDark = Color(0xFFFFD54F)
    val CardFixBgDark = Color(0xFF1E1B4B)
    val CardFixTextDark = Color(0xFF818CF8)
    val CardKataBgDark = Color(0xFF1E070F)
    val CardKataTextDark = Color(0xFFF48FB1)
    val CardIdiomBgDark = Color(0xFF240606)
    val CardIdiomTextDark = Color(0xFFE57373)
    val CardKeigoBgDark = Color(0xFF1E1405)
    val CardKeigoTextDark = Color(0xFFFFB74D)
    val CardSoundBgDark = Color(0xFF1A1E05)
    val CardSoundTextDark = Color(0xFFDCE775)
}

// ============================================================================
// 中性色板 (Neutral Palette - Tailwind-like)
// ============================================================================
object NemoNeutrals {
    val Gray50 = Color(0xFFF9FAFB)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray200 = Color(0xFFE5E7EB)
    val Gray300 = Color(0xFFD1D5DB)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray600 = Color(0xFF4B5563)
    val Gray700 = Color(0xFF374151)
    val Gray800 = Color(0xFF1F2937)
    val Gray900 = Color(0xFF111827)

    // Dark Mode Specific Texts
    val DarkTextPrimary = Color(0xFFE6E1E5)
    val DarkTextSecondary = Color(0xFFCAC4D0)

    // Accents
    val Blue600 = Color(0xFF2563EB)
}

// ============================================================================
// Bento UI 专供色彩 (HTML 原型)
// ============================================================================
object BentoColors {
    val BgBase = Color(0xFFF1F5F9)         // 极其柔和的灰蓝色背景
    val Surface = Color(0xFFFFFFFF)        // 纯白便当盒卡片

    val Primary = Color(0xFFF97316)        // 元气橙/珊瑚橘 - 充满活力的主色调
    val PrimaryHover = Color(0xFFEA580C)
    val PrimaryLight = Color(0xFFFFEDD5)
    
    val GrammarPrimary = Color(0xFF059669) // 翠绿色，语法模式主题色
    val GrammarPrimaryLight = Color(0xFFD1FAE5)

    val TextMain = Color(0xFF0F172A)       // 深石板黑
    val TextSub = Color(0xFF64748B)
    val TextMuted = Color(0xFF94A3B8)

    // 功能点缀色
    val AccentBlue = Color(0xFF3B82F6)
    val AccentGreen = Color(0xFF10B981)
    val AccentOrange = Color(0xFFF59E0B)
    val AccentPurple = Color(0xFF8B5CF6)
    
    // 图标特定背景色
    val IconBgOrange = Color(0xFFFFF7ED)
    val IconBgGreen = Color(0xFFECFDF5)
    val IconBgBlue = Color(0xFFEFF6FF)
    val IconBgPurple = Color(0xFFF5F3FF)
}


// ============================================================================
// Flat UI 经典配色 (用于动效还原)
// ============================================================================
object FlatUiColors {
    val Blue = Color(0xFF3498DB)
    val Green = Color(0xFF2ECC71)
    val Red = Color(0xFFE74C3C)
    val Purple = Color(0xFF9B59B6)
    val Yellow = Color(0xFFF1C40F)
    val Orange = Color(0xFFE67E22)
    val Dark = Color(0xFF34495E)
    val Background = Color(0xFFECF0F1)
}
