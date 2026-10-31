package com.jian.nemo.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Nemo 品牌语义色彩 (Semantic Colors)
// ============================================================================

val NemoPrimary = Color(0xFF007AFF)          // 主题经典蓝 (浅色规范交互色)
val NemoPrimaryDark = Color(0xFF0A84FF)      // 主题自发光蓝 (深色明度补偿发光色)
val NemoSecondary = Color(0xFF34C759)        // 辅助绿色 (规范绿)
val NemoSecondaryDark = Color(0xFF30D158)    // 辅助绿色 (深色高荧光绿)
val NemoText = Color(0xFF000000)             // 主文本颜色 (规范 Level 1: 100% 对比度)
val NemoTextLight = Color(0xFF8A8A8E)        // 浅文本颜色 (规范 Level 2: 60% 对比度)
val NemoDanger = Color(0xFFFF3B30)           // 危险/错误色 (规范红)
val NemoDangerDark = Color(0xFFFF453A)       // 危险/错误色 (深色自发光红)
val NemoWarning = Color(0xFFFF9500)          // 警告/提示橙 (规范橙)
val NemoWarningDark = Color(0xFFFF9F0A)      // 警告/提示橙 (深色琥珀橙)
val NemoGold = Color(0xFFFFD700)             // 金色/高亮色
val NemoYellow = Color(0xFFFFCC00)           // 系统黄色
val NemoPink = Color(0xFFFF2D55)             // 规范粉红
val NemoNeutralGray = Color(0xFF8E8E93)      // 中性灰基准 (Dynamic Color Spec 规范)

// ColorScheme 容器与对应前景配色 (Light)
val NemoPrimaryContainerLight = Color(0xFFE5F1FF)
val NemoOnPrimaryContainerLight = Color(0xFF004CB3)
val NemoSecondaryContainerLight = Color(0xFFDDF5E3)
val NemoOnSecondaryContainerLight = Color(0xFF0F5A24)
val NemoOnTertiaryContainerLight = Color(0xFF3C3C43)
val NemoErrorContainerLight = Color(0xFFFFE5E5)
val NemoOnErrorContainerLight = Color(0xFF991B1B)
val NemoSurfaceContainerLowLight = Color(0xFFF7F7FA)

// ColorScheme 容器与对应前景配色 (Dark)
val NemoPrimaryContainerDark = Color(0xFF00408A)
val NemoOnPrimaryContainerDark = Color(0xFFB3D9FF)
val NemoOnSecondaryDark = Color(0xFF003912)
val NemoSecondaryContainerDark = Color(0xFF135324)
val NemoOnSecondaryContainerDark = Color(0xFF82F79E)
val NemoOnTertiaryContainerDark = Color(0xFFEBEBF5)
val NemoOnErrorDark = Color(0xFF4A0002)
val NemoErrorContainerDark = Color(0xFF801015)
val NemoOnErrorContainerDark = Color(0xFFFFD4D4)
val NemoSurfaceContainerLowDark = Color(0xFF141416)
val NemoBottomBarContainerDark = Color(0xFF121212)
val NemoBottomBarContainerLight = Color(0xFFFAFAFA)

// 学习稳定性与热力图分桶配色 (统一收纳)
const val StabilityEarlyHex = "#EF4444"
const val StabilityDevelopingHex = "#3B82F6"
const val StabilityMatureHex = "#22C55E"
const val StabilityExpertHex = "#8B5CF6"
val StabilityEarly = Color(0xFFEF4444)
val StabilityDeveloping = Color(0xFF3B82F6)
val StabilityMature = Color(0xFF22C55E)
val StabilityExpert = Color(0xFF8B5CF6)
val HeatmapSubtleTextDark = Color(0xFF8B949E)
val HeatmapSubtleTextLight = Color(0xFF64748B)


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

// 容器与表面语义 (对齐 Dynamic Color System 规范 Level 0 ~ Level 3)
val NemoSurfaceBackground = Color(0xFFF2F2F7)       // 语义化页面背景 (Level 0: 微暖浅灰沉底)
val NemoSurfaceCard = Color(0xFFFFFFFF)             // 语义化卡片容器 (Level 1: 绝对纯白上浮)
val NemoSurfaceSubcard = Color(0xFFE5E5EA)          // 语义化二级表面 (Level 2: 下凹微灰)
val NemoSurfaceElevated = Color(0xFFD1D1D6)         // 语义化三级表面 (Level 3: 激活态/微型部件)
val NemoSurfaceBorder = Color(0xFFC6C6C8)           // 语义化边框/分割线 (规范标准发丝线)

val NemoSurfaceBackgroundDark = Color(0xFF000000)   // 深色页面背景 (Level 0: OLED 纯黑 0% 亮度)
val NemoSurfaceCardDark = Color(0xFF1C1C1E)         // 深色卡片容器 (Level 1: 深石板灰 ~11% 亮度)
val NemoSurfaceSubcardDark = Color(0xFF2C2C2E)      // 深色二级表面 (Level 2: 浮动石板灰 ~17% 亮度)
val NemoSurfaceElevatedDark = Color(0xFF3A3A3C)     // 深色三级表面 (Level 3: 高亮浮层/激活态 ~23% 亮度)
val NemoSurfaceBorderDark = Color(0xFF38383A)       // 深色边框/分割线 (规范微光灰)

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

    // 分段控件深色表面
    val SegmentTrackDark = Color(0xFF121214)
    val SegmentThumbDark = Color(0xFF55555A)
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

// ============================================================================
// 卡片配对与测验色板 (Card Matching Palette)
// ============================================================================
object CardMatchingPalette {
    val PairLight = listOf(
        Color(0xFFF97316), // 珊瑚橙
        Color(0xFF10B981), // 翡翠绿
        Color(0xFF3B82F6), // 湛蓝
        Color(0xFFF59E0B), // 琥珀黄
        Color(0xFF8B5CF6), // 亮紫
        Color(0xFF06B6D4), // 青空蓝
        Color(0xFFEC4899), // 玫瑰粉
        Color(0xFF14B8A6)  // 绿松石
    )

    val PairDark = listOf(
        Color(0xFFFB923C), // 珊瑚橙
        Color(0xFF34D399), // 翡翠绿
        Color(0xFF60A5FA), // 湛蓝
        Color(0xFFFBBF24), // 琥珀黄
        Color(0xFFA78BFA), // 亮紫
        Color(0xFF22D3EE), // 青空蓝
        Color(0xFFF472B6), // 玫瑰粉
        Color(0xFF2DD4BF)  // 绿松石
    )
}

// ============================================================================
// 表达与惯用句分类色板 (Expression Category Palette)
// ============================================================================
object ExpressionCategoryPalette {
    val OrangeLight = Color(0xFFFF6B4A)
    val EmeraldLight = Color(0xFF10B981)
    val PinkLight = Color(0xFFEC4899)
    val CyanLight = Color(0xFF06B6D4)
    val AmberLight = Color(0xFFF59E0B)
    val PurpleLight = Color(0xFFA855F7)

    val OrangeDark = Color(0xFFFF8A65)
    val EmeraldDark = Color(0xFF34D399)
    val PinkDark = Color(0xFFF472B6)
    val CyanDark = Color(0xFF22D3EE)
    val AmberDark = Color(0xFFFBBF24)
    val PurpleDark = Color(0xFFC084FC)
}


// ============================================================================
// 排序测验色板 (Sorting Quiz Palette)
// ============================================================================
object SortingPalette {
    val MacaronColors = listOf(
        Color(0xFF818CF8),
        Color(0xFFF472B6),
        Color(0xFFFBBF24),
        Color(0xFF34D399),
        Color(0xFF22D3EE),
        Color(0xFFA78BFA)
    )

    val BackgroundGradientLight = Color(0xFFF5F8FF)
    val ContainerBgLight = Color(0xFFE0E7FF)
    val ContainerBorderLight = Color(0xFFCBD5E1)
    val PlaceholderText = Color(0xFF312E81)
    val OptionsTrayBgLight = Color(0xFFF8FAFF)
}


// ============================================================================
// 统计与活动环色板 (Statistics & Activity Rings Palette)
// ============================================================================
object ActivityRingPalette {
    val MovePink = Color(0xFFFA2D62)
    val ExerciseGreen = Color(0xFF7BD33A)
    val StandCyan = Color(0xFF18C6E8)
    val SurfaceWarmLight = Color(0xFFF5F3EF)
    val MetricCardBgLight = Color(0xFFF6F4EF)
}

object HeatmapPalette {
    val Level0 = Color(0xFFF1F5F9)
    val Level1 = Color(0xFFFFD7D5)
    val Level2 = Color(0xFFFFA39E)
    val Level3 = Color(0xFFFF4D4F)
    val Level4 = Color(0xFFCF1322)

    val Level0Dark = Color(0xFF1E2430)
    val Level1Dark = Color(0xFF3F2020)
    val Level2Dark = Color(0xFF742929)
    val Level3Dark = Color(0xFFBD3131)
    val Level4Dark = Color(0xFFF04A4A)

    val DotGlowLight = Color(0xFFFFFFFF)
    val DotGlowDark = Color(0xFFFFECEB)
}

object StatisticsPalette {
    val AlertGold = Color(0xFFFFD60A)
    val CompositeOrange = Color(0xFFFF9800)
    val EarlyRed = Color(0xFFEF4444)
    val DevelopingBlue = Color(0xFF3B82F6)
    val MatureGreen = Color(0xFF22C55E)
    val ExpertPurple = Color(0xFF8B5CF6)
    val GrowthAccentGold = Color(0xFFF4B73F)

    val EmeraldTextLight = Color(0xFF059669)
    val EmeraldTextDark = Color(0xFF34D399)
    val EmeraldBgLight = Color(0xFFECFDF5)

    val PosterBgDark = Color(0xFF171827)
    val PosterBgLight = Color(0xFFF4F6FA)
    val PosterCardBgDark = Color(0xFF202136)
    val PosterCardSubtleBgDark = Color(0xFF1A1B2D)
    val PosterCardBorderDark = Color(0xFF2E2F48)
    val PosterFlameOrange = Color(0xFFFF7A00)

    val PosterMiniHeatmapDark = listOf(
        Color(0xFF24253A),
        Color(0xFF1E3A8A),
        Color(0xFF2563EB),
        Color(0xFF60A5FA),
        Color(0xFF93C5FD)
    )

    val PosterMiniHeatmapLight = listOf(
        Color(0xFFE2E8F0),
        Color(0xFF93C5FD),
        Color(0xFF3B82F6),
        Color(0xFF1D4ED8),
        Color(0xFF172554)
    )
}

object BrandPalette {
    val GoogleYellow = Color(0xFFFBBC05)
    val GoogleRed = Color(0xFFEA4335)
    val GoogleGreen = Color(0xFF34A853)
    val GoogleBlue = Color(0xFF4285F4)
}

object CaptchaPalette {
    val DarkColors = listOf(
        Color(0xFF60A5FA),
        Color(0xFF34D399),
        Color(0xFFF472B6),
        Color(0xFFFBBF24),
        Color(0xFFA78BFA),
        Color(0xFF38BDF8),
        Color(0xFF4ADE80)
    )

    val LightColors = listOf(
        Color(0xFF1D4ED8),
        Color(0xFF047857),
        Color(0xFFBE185D),
        Color(0xFFB45309),
        Color(0xFF6D28D9),
        Color(0xFF0369A1),
        Color(0xFF15803D)
    )

    val QuestionMarkDark = Color(0xFFF87171)
    val QuestionMarkLight = Color(0xFFDC2626)
    val EqualSignDark = Color(0xFFFBBF24)
    val EqualSignLight = Color(0xFFD97706)
    val OperatorDark = Color(0xFF38BDF8)
    val OperatorLight = Color(0xFF0284C7)

    val BgDark = Color(0xFF1E1E24)
    val BgLight = Color(0xFFEEF2F6)
    val UnfocusedBorderLight = Color(0xFFE2E8F0)
}

object UserPalette {
    val AuthBgDark = Color(0xFF0D0C11)
    val AuthBgLight = Color(0xFFF7F9FC)
    val ProfileSetupBgDark = Color(0xFF100F14)

    val PrimaryGlassTintDark = Color(0xFF2563EB)
    val PrimaryGlassTintLight = Color(0xFF0E68FF)

    val FieldContainerDark = Color(0xFF16151B)
    val FieldBorderSlate = Color(0xFFE2E8F0)
    val SlatePlaceholder = Color(0xFF94A3B8)

    val TextSlate900 = Color(0xFF0F172A)
    val TextSlate800 = Color(0xFF1E293B)
    val TextSlate700 = Color(0xFF334155)
    val TextSlate500 = Color(0xFF64748B)

    val CapsuleTextDark = Color(0xFF7DD3FC)
    val CapsuleTextLight = Color(0xFF2563EB)

    val DialogBodyDark = Color(0xFF8E8E93)
    val DialogBodyLight = Color(0xFF6E6E73)
    val DialogContainerDark = Color(0xFF1C1C1E)
    val DialogBorderDark = Color(0xFF3A3A3C)
    val DialogBorderLight = Color(0xFFC6C6C8)

    val AvatarBorderDark = Color(0xFF1E1D24)
    val PrimaryBtnBgDark = Color(0xFFF4F4F6)
}

object SettingsPalette {
    val AboutPaperBgDark = Color(0xFF131722)
    val AboutPaperBgLight = Color(0xFFFAF6EE)
    val AboutLightBeamDark = Color(0xFF38BDF8)
    val AboutNoiseLight = Color(0xFF6B4527)

    val DialogBgDark = Color(0xFF202020)
    val DialogCardBgDark = Color(0xFF2A2A2A)
    val DialogCardBgLight = Color(0xFFF7F8FA)

    val BackupGlassTint = Color(0xFF0088FF)
    val AlgorithmCardBgDark = Color(0xFF1E2028)
    val CyanMint = Color(0xFF00C4B5)
    val CoralRose = Color(0xFFF43F5E)

    val LiquidOrange = Color(0xFFFF8D28)
    val LiquidCyanGradStart = Color(0xFF00C6FF)
    val LiquidBlueGradEnd = Color(0xFF0072FF)
    val LiquidPinkGradStart = Color(0xFFFF2A85)
    val LiquidPurpleGradEnd = Color(0xFF8A2387)
    val LiquidAmberGradStart = Color(0xFFFF8008)
    val LiquidGoldGradEnd = Color(0xFFFFC837)
    val LiquidGreen = Color(0xFF2ECC71)

    val ThemeElectricCyan = Color(0xFF00E5FF)
    val ThemeBrightTeal = Color(0xFF1DE9B6)
    val ThemeBrightOrangeRed = Color(0xFFFF3D00)
    val ThemeAppleGreen = Color(0xFF4CD964)

    val StudioCardBgDark = Color(0xFF13171F)
    val StudioBorderDark = Color(0x2294A3B8)
    val StudioBorderLight = Color(0x140F172A)
}

object AIPalette {
    val DeepSeek = Color(0xFF0E68FF)
    val DeepSeekAlt = Color(0xFF1E88E5)
    val Doubao = Color(0xFF00B4D8)
    val Glm = Color(0xFF3762FF)
    val Kimi = Color(0xFF007AFF)
    val Mimo = Color(0xFFFF6A00)
    val MimoAlt = Color(0xFFFF6700)
    val Qwen = Color(0xFF615CED)
    val ClaudeAmber = Color(0xFFD97706)
    val ClaudeRust = Color(0xFFE05C2B)
    val OpenAI = Color(0xFF10A37F)
    val Gemini = Color(0xFFAF52DE)
    val GeminiAlt = Color(0xFF6E56CF)
    val CustomPurple = Color(0xFF8B5CF6)

    val StudioCardBgDark = Color(0xFF13171F)
    val StudioCardBgLight = Color(0xFFFFFFFF)
    val StudioPillBgDark = Color(0xFF1C222C)
    val StudioPillBgLight = Color(0xFFF1F5F9)
    val StudioInputContainerDark = Color(0xFF141720)
    val StudioInputContainerLight = Color(0xFFF8FAFC)
}

object UiPalette {
    val ShadowLight = Color(0x0D000000)
    val ShadowDark = Color(0x26000000)
    val PureBlack = Color(0xFF000000)
    val PureWhite = Color(0xFFFFFFFF)

    val DialogDarkBg = Color(0xFF1C1C1E)
    val DialogAltDarkBg = Color(0xFF1E1E1E)
    val DialogControlBgDark = Color(0xFF2B2B2E)
    val DialogControlBgLight = Color(0xFFF1F5F9)
    val DialogDarkBorder = Color(0xFF383838)
    val DialogLightBorder = Color(0xFFE2E8F0)
    val DialogSlateTextDark = Color(0xFF94A3B8)
    val DialogSlateTextLight = Color(0xFF64748B)
    val DialogDarkActiveBg = Color(0xFF262626)
    val DialogBlackText = Color(0xFF18181B)
    val DialogBlueTintBgDark = Color(0xFF1E293B)
    val DialogBlueTintBgLight = Color(0xFFEFF6FF)

    val BonusOrange = Color(0xFFFF5500)
    val BonusWarmBgDark = Color(0xFF332014)
    val BonusWarmBgLight = Color(0xFFFFF1E6)
    val BonusCardBgDark = Color(0xFF202024)
    val BonusAccentCardDark = Color(0xFF2D2520)

    val AvatarInnerBorderDark = Color(0xFF2C2C2E)
    val AvatarSystemBlueLight = Color(0xFF007AFF)
    val AvatarSystemBlueDark = Color(0xFF0A84FF)
    val AvatarSystemGreenLight = Color(0xFF34C759)
    val AvatarSystemGreenDark = Color(0xFF30D158)
    val AvatarSystemPurpleLight = Color(0xFFAF52DE)
    val AvatarSystemPurpleDark = Color(0xFFBF5AF2)
    val AvatarSystemRedLight = Color(0xFFFF3B30)
    val AvatarSystemRedDark = Color(0xFFFF453A)

    val NotifyInfoGradStartDark = Color(0xFF3D3A50)
    val NotifyInfoGradEndDark = Color(0xFF2B2930)
    val NotifyInfoGradStartLight = Color(0xFF0E68FF)
    val NotifyInfoGradEndLight = Color(0xFF4A90D9)

    val NotifySuccessGradStartDark = Color(0xFF2D4A3D)
    val NotifySuccessGradEndDark = Color(0xFF1E3A2F)
    val NotifySuccessGradStartLight = Color(0xFF34C759)
    val NotifySuccessGradEndLight = Color(0xFF28A745)

    val NotifyWarnGradStartDark = Color(0xFF4A3D2D)
    val NotifyWarnGradEndDark = Color(0xFF3A2F1E)
    val NotifyWarnGradStartLight = Color(0xFFFF9500)
    val NotifyWarnGradEndLight = Color(0xFFE68A00)

    val NotifyErrorGradStartDark = Color(0xFF4A2D2D)
    val NotifyErrorGradEndDark = Color(0xFF3A1E1E)
    val NotifyErrorGradStartLight = Color(0xFFFF3B30)
    val NotifyErrorGradEndLight = Color(0xFFE53935)

    val LiquidBrandBlue = Color(0xFF0088FF)
    val LiquidLightAccent = Color(0xFF0091FF)
    val LiquidPurple = Color(0xFF8B5CF6)
    val LiquidCyanMint = Color(0xFF00C4B5)
    val LiquidTabsBgLight = Color(0xFFFAFAFA)
    val LiquidTabsBgDark = Color(0xFF121212)
    val LiquidTrackLight = Color(0xFF787878)
    val LiquidTrackDark = Color(0xFF787880)

    val SplashGlowSky = Color(0xFF0EA5E9)
    val SplashGlowTeal = Color(0xFF2DD4BF)
    val SplashGlowLightBlue = Color(0xFF38BDF8)
    val SplashGlowOrange = Color(0xFFFB923C)
    val SplashShapeRose = Color(0xFFFB7185)
    val SplashShapeViolet = Color(0xFFA78BFA)
    val SplashShapeBlue = Color(0xFF60A5FA)
    val SplashFgDark = Color(0xFFF5F5F7)
    val SplashFgLight = Color(0xFF0A0A0A)

    val DefaultAvatarPalette = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6),
        Color(0xFF26C6DA), Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF8BC34A),
        Color(0xFFDCE775), Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFB74D),
        Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE), Color(0xFF78909C)
    )

    val AvatarGlowGradient = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF6366F1)
    )

    object Presets {
        val Sunset = listOf(Color(0xFFFF6B6B), Color(0xFFFFAA00), Color(0xFFFFD93D))
        val Ocean = listOf(Color(0xFF06BEE1), Color(0xFF0B4F6C), Color(0xFF01BAEF))
        val Forest = listOf(Color(0xFF06D6A0), Color(0xFF118AB2), Color(0xFF073B4C))
        val Lavender = listOf(Color(0xFF9D4EDD), Color(0xFF7209B7), Color(0xFFC77DFF))
        val Mint = listOf(Color(0xFF06FFA5), Color(0xFF00D9FF), Color(0xFF00B4D8))
        val Coral = listOf(Color(0xFFFF6B9D), Color(0xFFC9184A), Color(0xFFFF8FA3))
        val Golden = listOf(Color(0xFFFFBE0B), Color(0xFFFB5607), Color(0xFFFF006E))
        val Aurora = listOf(Color(0xFF7209B7), Color(0xFF3A0CA3), Color(0xFF4361EE))
        val Peach = listOf(Color(0xFFFFB5A7), Color(0xFFFCD5CE), Color(0xFFF8EDEB))
        val Autumn = listOf(Color(0xFFD00000), Color(0xFFDC2F02), Color(0xFFE85D04))
        val Sky = listOf(Color(0xFF48CAE4), Color(0xFF00B4D8), Color(0xFF90E0EF))
        val Lemon = listOf(Color(0xFFFFFF3F), Color(0xFFFFEE32), Color(0xFFFDD835))
        val Midnight = listOf(Color(0xFF03045E), Color(0xFF023E8A), Color(0xFF0077B6))
        val Galaxy = listOf(Color(0xFF240046), Color(0xFF3C096C), Color(0xFF5A189A))
        val Tropical = listOf(Color(0xFFFF006E), Color(0xFFFB5607), Color(0xFFFFBE0B))
        val Cherry = listOf(Color(0xFFFFB3C6), Color(0xFFFF8FAB), Color(0xFFFFC2D1))
    }
}

object PlumpPalette {
    val SkyIndigo = Color(0xFF4C82F6)    // 晴空蓝 (Hue 255°)
    val LakesideCyan = Color(0xFF009CB2) // 湖光青 (Hue 195°)
    val LilacViolet = Color(0xFF906EE8)  // 丁香紫 (Hue 295°)
    val WarmAmber = Color(0xFFD67700)    // 暖金琥珀 (Hue 70°)
    val EmeraldGreen = Color(0xFF229E65) // 翡翠竹青 (Hue 145°)
    val RoseRuby = Color(0xFFE44F6D)     // 蔷薇绯红 (Hue 15°)
    val SeaMarine = Color(0xFF1D8FDF)    // 蔚蓝海晶 (Hue 225°)
    val CoralFlame = Color(0xFFDE5545)   // 赤焰珊瑚 (Hue 30°)
    val GlazeMint = Color(0xFF009F8C)    // 青碧琉璃 (Hue 175°)
    val OceanBlue = Color(0xFF3B86E8)    // 深海青蓝 (Hue 240°)
}

object DesignSystemPalette {
    val StepperOrange = Color(0xFFFF5500)
    val StepperSlateText = Color(0xFF1E293B)
    val StepperBtnDark = Color(0xFF262626)
    val StepperBtnLight = Color(0xFFF1F5F9)
    val StepperBorderDark = Color(0xFF383838)
    val StepperBorderLight = Color(0xFFE2E8F0)

    val PickerIndicatorDark = Color(0xFF94A3B8)
    val PickerIndicatorLight = Color(0xFF64748B)
    val PickerTickDark = Color(0xFF334155)
    val PickerTickLight = Color(0xFFCBD5E1)
    val PickerSubTickDark = Color(0xFF1E293B)
    val PickerSubTickLight = Color(0xFFE2E8F0)
}

object LearningPalette {
    val AuroraClassic1 = Color(0xFF5227FF)
    val AuroraClassic2 = Color(0xFF7CFF67)
    val AuroraNordic1 = Color(0xFF6D28D9)
    val AuroraNordic2 = Color(0xFFEC4899)
    val AuroraNordic3 = Color(0xFF06B6D4)
    val AuroraPolar1 = Color(0xFF3A29FF)
    val AuroraPolar2 = Color(0xFFFF94B4)
    val AuroraPolar3 = Color(0xFFFF3232)
    val AuroraGlacier1 = Color(0xFF0EA5E9)
    val AuroraGlacier2 = Color(0xFF10B981)
    val AuroraGlacier3 = Color(0xFF6366F1)
    val AuroraDusk1 = Color(0xFFF43F5E)
    val AuroraDusk2 = Color(0xFFA855F7)
    val AuroraDusk3 = Color(0xFF3B82F6)

    val DrawerBgDark = Color(0xFF06060E)
    val DrawerBorderLight = Color(0x18000000)
    val SkyLight = Color(0xFF7DD3FC)
    val AccentRed = Color(0xFFEF4444)

    val SuccessGreen = Color(0xFF4CAF50)
    val CategoryDarkBg1 = Color(0xFF121212)
    val CategoryDarkBg2 = Color(0xFF1E1E1E)

    val SrsRoseTextDark = Color(0xFFFDA4AF)
    val SrsRoseTextLight = Color(0xFFBE123C)
    val SrsRoseBgDark = Color(0xFF4C0519)
    val SrsRoseBgLight = Color(0xFFFFE4E6)

    val SrsOrangeTextDark = Color(0xFFFDBA74)
    val SrsOrangeTextLight = Color(0xFFEA580C)
    val SrsOrangeBgDark = Color(0xFF7C2D12)
    val SrsOrangeBgLight = Color(0xFFFFF7ED)

    val SrsBlueTextDark = Color(0xFF93C5FD)
    val SrsBlueTextLight = Color(0xFF2563EB)
    val SrsBlueBgDark = Color(0xFF1E3A8A)
    val SrsBlueBgLight = Color(0xFFEFF6FF)

    val SrsEmeraldTextDark = Color(0xFF6EE7B7)
    val SrsEmeraldTextLight = Color(0xFF059669)
    val SrsEmeraldBgDark = Color(0xFF064E3B)
    val SrsEmeraldBgLight = Color(0xFFECFDF5)

    val StarAmber = Color(0xFFFACC15)
    val ActionDarkBg = Color(0xFF111827)
    val ActionMutedText = Color(0xFF6B7280)

    val AlertRedBg = Color(0xFFFFEBEE)

    val BannerOrangeBgDark = Color(0xFF2E1F14)
    val BannerOrangeBgLight = Color(0xFFFFF7ED)
    val BannerOrangeBorderDark = Color(0xFF6B3A1E)
    val BannerOrangeBorderLight = Color(0xFFFFEDD5)
    val BannerOrangeTextDark = Color(0xFFFF8A50)
    val BannerOrangeTextLight = Color(0xFFEA580C)

    val IndigoBgDark = Color(0xFF1E1B4B)
    val IndigoBgLight = Color(0xFFEEF2FF)
    val IndigoBorderDark = Color(0xFF3730A3)
    val IndigoBorderLight = Color(0xFFE0E7FF)
    val IndigoTextDark = Color(0xFFA5B4FC)
    val IndigoTextLight = Color(0xFF4F46E5)

    val BadgeNewBgDark = Color(0xFF1E3A8A)
    val BadgeNewBgLight = Color(0xFFE0EDFF)
    val BadgeNewTextDark = Color(0xFFBFDBFE)
    val BadgeNewTextLight = Color(0xFF1D4ED8)

    val BadgeLearningBgDark = Color(0xFF083344)
    val BadgeLearningBgLight = Color(0xFFE0F2FE)
    val BadgeLearningTextDark = Color(0xFF22D3EE)
    val BadgeLearningTextLight = Color(0xFF0891B2)

    val BadgeReviewBgDark = Color(0xFF14532D)
    val BadgeReviewBgLight = Color(0xFFDCFCE7)
    val BadgeReviewTextDark = Color(0xFFBBF7D0)
    val BadgeReviewTextLight = Color(0xFF166534)

    val BadgeRelearnBgDark = Color(0xFF7C2D12)
    val BadgeRelearnBgLight = Color(0xFFFFEDD5)
    val BadgeRelearnTextDark = Color(0xFFFED7AA)
    val BadgeRelearnTextLight = Color(0xFF9A3412)

    val YellowTipsBgDark = Color(0xFF713F12)
    val YellowTipsBgLight = Color(0xFFFEFCE8)
    val YellowTipsBorderDark = Color(0xFF92400E)
    val YellowTipsBorderLight = Color(0xFFFEF3C7)
    val YellowTipsIconDark = Color(0xFFFDE047)
    val YellowTipsIconLight = Color(0xFFCA8A04)
    val YellowTipsTextDark = Color(0xFFFEF08A)
    val YellowTipsTextLight = Color(0xFF92400E)

    val ExampleBoxDark = Color(0xFF2D2D2D)

    val ShareGradTeal = Color(0xFF0F766E)
    val ShareSlateText = Color(0xFF0F172A)
    val ShareSubText = Color(0xFF64748B)

    val ReportZinc900 = Color(0xFF18181B)
    val ReportZinc800 = Color(0xFF27272A)
    val ReportZinc700 = Color(0xFF3F3F46)
    val ReportZinc600 = Color(0xFF52525B)
    val ReportZinc500 = Color(0xFF71717A)
    val ReportZinc400 = Color(0xFFA1A1AA)
    val ReportZinc200 = Color(0xFFE4E4E7)
    val ReportZinc100 = Color(0xFFF4F4F5)

    // 白板笔刷色值 (NemoWhiteboard)
    val WhiteboardPenPearlWhite = Color(0xFFF8FAFC)
    val WhiteboardPenNeonRed = Color(0xFFFF5252)
    val WhiteboardPenNeonBlue = Color(0xFF40C4FF)
    val WhiteboardPenMintGreen = Color(0xFF69F0AE)
    val WhiteboardPenInkBlack = Color(0xFF1E1E1E)
    val WhiteboardPenPureRed = Color(0xFFE53935)
    val WhiteboardPenLakeBlue = Color(0xFF1E88E5)
    val WhiteboardPenGrassGreen = Color(0xFF43A047)
    val WhiteboardIconDark = Color(0xFF333333)

    // 结算完成页面 (LearningFinishedContent)
    val FinishedDeepOrange = Color(0xFFFF5722)
    val FinishedWarmBgDark = Color(0xFF2B1F17)
    val FinishedWarmBgLight = Color(0xFFFFF7ED)

    // 动词变形手册避坑警告色盘 (VerbConjugationHandbookScreen)
    val VerbBanRedLight = Color(0xFFDC2626)
    val VerbBanRedDark = Color(0xFFEF4444)
    val VerbBanBgLight = Color(0xFFFEF2F2)
    val VerbBanBgDark = Color(0xFF7F1D1D)

    val VerbWarningBgDark = Color(0xFF3B1E1E)
    val VerbWarningBgLight = Color(0xFFFFF1F2)
    val VerbWarningBorderDark = Color(0xFF7F1D1D)
    val VerbWarningBorderLight = Color(0xFFFFE4E6)
    val VerbWarningChipDark = Color(0xFF4C1D1D)
    val VerbWarningChipLight = Color(0xFFFFE4E6)
    val VerbWarningTextDark = Color(0xFFFDA4AF)
    val VerbWarningTextLight = Color(0xFFBE123C)
    val VerbWarningRose = Color(0xFF9F1239)

    // 五十音图专供色盘 (KanaChartScreen)
    val KanaChipBgLight = Color(0xFFDFF4FF)
    val KanaAccentLight = Color(0xFF0EA5A8)
    val KanaQuickNavBaseLight = Color(0xFFF3FAFF)
    val KanaQuickNavSelectedLight = Color(0xFFD7F1FF)
    val KanaTabBgLight = Color(0xFFEAF6FF)
    val KanaIndigoAccent = Color(0xFF4F46E5)
    val KanaPinkAccent = Color(0xFFDB2777)
    val KanaSlateDark = Color(0xFF334155)
    val KanaSlateSub = Color(0xFF64748B)

    // AI 系列页面（AIReading / AIReadingHistory / AIWorkshop）
    val ReadingIconBgRed = Color(0xFFFEE2E2)
    val ReadingAccentRed = Color(0xFFEF4444)
    val ReadingIconBgGreen = Color(0xFFD1FAE5)
    val ReadingAccentGreen = Color(0xFF10B981)
    val ReadingIconBgOrange = Color(0xFFFEF3C7)
    val ReadingAccentOrange = Color(0xFFD97706)
    val ReadingIconBgPink = Color(0xFFFCE7F3)
    val ReadingAccentPink = Color(0xFFEC4899)
    val ReadingIconBgIndigo = Color(0xFFEEF2FF)
    val ReadingAccentIndigo = Color(0xFF6366F1)

    val AiCoralOrange = Color(0xFFFB923C)
    val AiDeepOrange = Color(0xFFEA580C)
    val AiAmberYellow = Color(0xFFFBBF24)
    val AiSkyBlueDark = Color(0xFF38BDF8)
    val AiRoyalBlueLight = Color(0xFF2563EB)
    val AiSlateBg = Color(0xFFFAFAFB)
    val AiTrackBgLight = Color(0xFFF1F5F9)
    val AiBorderLight = Color(0xFFE2E8F0)
    val AiSlateText = Color(0xFF0F172A)
    val AiMutedText = Color(0xFF64748B)

    val ShadowDark = Color(0x26000000)
    val ShadowLight = Color(0x0C000000)
    val ReadingBorderLight = Color(0x0D000000)
    val ReadingDarkShadow = Color(0x20000000)
    val ReadingLightShadow = Color(0x08000000)
    val ReadingDarkText = Color(0xFF2C3E50)

    // 学习通用组件 (LearningCommonComponents)
    val AuroraMaskDark1 = Color(0x33090714)
    val AuroraMaskDark2 = Color(0x05090714)
    val AuroraMaskDark3 = Color(0x66090714)
    val AuroraMaskLight1 = Color(0x66FFFFFF)
    val AuroraMaskLight2 = Color(0x10FFFFFF)
    val AuroraMaskLight3 = Color(0x55F8FAFC)
    val AuroraMaskLight4 = Color(0x88F1F5F9)

    val Slate300 = Color(0xFFCBD5E1)
    val Slate600 = Color(0xFF475569)
    val CooldownIndigoDark = Color(0xFF1E1B4B)
    val CooldownCardBgDark = Color(0xB2120F24)
    val CooldownCardBgLight = Color(0xEBFFFFFF)
    val CooldownCardBorderDark = Color(0x38A78BFA)
    val CooldownCardBorderLight = Color(0x297C3AED)
    val CooldownPurpleTextDark = Color(0xFFC4B5FD)
    val CooldownPurpleTextLight = Color(0xFF7C3AED)
    val CooldownBadgeBg = Color(0x26F59E0B)
    val CooldownBadgeText = Color(0xFFFBBF24)
    val CooldownStatBgDark = Color(0x0DFFFFFF)
    val CooldownStatBgLight = Color(0xF2F1F5F9)
    val CooldownMutedDark = Color(0xFF94A3B8)
    val RestButtonSurfaceDark = Color(0xFF262038)
    val AccentOrange = Color(0xFFF59E0B)
}



