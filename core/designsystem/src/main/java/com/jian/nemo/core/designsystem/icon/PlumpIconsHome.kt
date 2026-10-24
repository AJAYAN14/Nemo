package com.jian.nemo.core.designsystem.icon

/**
 * 首页与概览分支 Plump Duo 图标路径注册表
 *
 * 覆盖：今日学习达成徽章、首页统计指示、热力图、AI阅读主题卡片等。
 * 几何基准：严格遵循 Lucide 原生 24×24 网格规范。
 */
internal val PlumpIconsHomeRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. BadgeCheck (新学达成 / 成就解锁) Lucide badge-check.svg
    PlumpIcon.BadgeCheck to PlumpIconPathDefinition(
        fillPathData = "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z",
        strokePathData = "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z M16 9l-5.5 5.5L8 12",
        fillAlpha = 0.22f
    ),

    // 2. Trophy (热力图入口 / 单日最佳 / 荣誉奖杯) Lucide trophy.svg
    PlumpIcon.Trophy to PlumpIconPathDefinition(
        fillPathData = "M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z",
        strokePathData = "M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z M6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3 M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3 M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2 M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2 M4 22h16",
        fillAlpha = 0.22f
    ),

    // 3. BookOpen (AI阅读入口 / 主题选择 / 展开书本) Lucide book-open.svg
    PlumpIcon.BookOpen to PlumpIconPathDefinition(
        fillPathData = "M20.001 19A2 2 0 0 0 22 17V5a2 2 0 0 0-1.999-2L16 3.002A5 5 0 0 0 12 5a5 5 0 0 0-4-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 1.999 2H8a5 5 0 0 1 4 2 5 5 0 0 1 4-2z",
        strokePathData = "M12 5v16 M20.001 19A2 2 0 0 0 22 17V5a2 2 0 0 0-1.999-2L16 3.002A5 5 0 0 0 12 5a5 5 0 0 0-4-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 1.999 2H8a5 5 0 0 1 4 2 5 5 0 0 1 4-2z",
        fillAlpha = 0.18f
    ),

    // 4. Play (开始学习 / 播放三角形) Lucide play.svg
    PlumpIcon.Play to PlumpIconPathDefinition(
        fillPathData = "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z",
        strokePathData = "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z",
        fillAlpha = 0.25f
    ),

    // 5. ChevronsRight (继续学习 / 推进双前锋) Lucide chevrons-right.svg
    PlumpIcon.ChevronsRight to PlumpIconPathDefinition(
        fillPathData = "M6 17l5-5-5-5z M13 17l5-5-5-5z",
        strokePathData = "M6 17l5-5-5-5 M13 17l5-5-5-5",
        fillAlpha = 0.22f
    ),

    // 6. Trees (日常生活 / 双生森林) Lucide trees.svg
    PlumpIcon.Trees to PlumpIconPathDefinition(
        fillPathData = "M10 10v.2A3 3 0 0 1 8.9 16H5a3 3 0 0 1-1-5.8V10a3 3 0 0 1 6 0Z M12 19h8.3a1 1 0 0 0 .7-1.7L18 14h.3a1 1 0 0 0 .7-1.7L16 9h.2a1 1 0 0 0 .8-1.7L13 3l-1.4 1.5z",
        strokePathData = "M10 10v.2A3 3 0 0 1 8.9 16H5a3 3 0 0 1-1-5.8V10a3 3 0 0 1 6 0Z M7 16v6 M13 19v3 M12 19h8.3a1 1 0 0 0 .7-1.7L18 14h.3a1 1 0 0 0 .7-1.7L16 9h.2a1 1 0 0 0 .8-1.7L13 3l-1.4 1.5",
        fillAlpha = 0.20f
    ),

    // 7. Landmark (传统文化 / 历史地标神殿) Lucide landmark.svg
    PlumpIcon.Landmark to PlumpIconPathDefinition(
        fillPathData = "M11.119 2.205a2 2 0 0 1 1.762 0l7.84 3.846A.5.5 0 0 1 20.5 7h-17a.5.5 0 0 1-.22-.949z",
        strokePathData = "M11.119 2.205a2 2 0 0 1 1.762 0l7.84 3.846A.5.5 0 0 1 20.5 7h-17a.5.5 0 0 1-.22-.949z M10 18v-7 M14 18v-7 M18 18v-7 M6 18v-7 M3 22h18",
        fillAlpha = 0.22f
    ),

    // 8. Utensils (美食料理 / 刀叉餐具) Lucide utensils.svg
    PlumpIcon.Utensils to PlumpIconPathDefinition(
        fillPathData = "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2z M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Z",
        strokePathData = "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2 M7 2v20 M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7",
        fillAlpha = 0.22f
    ),

    // 9. Compass (名胜风光 / 罗盘指针) Lucide compass.svg
    PlumpIcon.Compass to PlumpIconPathDefinition(
        fillPathData = "M16.24 7.76l-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z",
        strokePathData = "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm4.24 5.76-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z",
        fillAlpha = 0.24f
    ),

    // 10. Briefcase (商务工作 / 公文包) Lucide briefcase.svg
    PlumpIcon.Briefcase to PlumpIconPathDefinition(
        fillPathData = "M4 6h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z",
        strokePathData = "M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16 M4 6h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z",
        fillAlpha = 0.20f
    ),

    // 11. GraduationCap (JLPT 级别 / 学士帽) Lucide graduation-cap.svg
    PlumpIcon.GraduationCap to PlumpIconPathDefinition(
        fillPathData = "M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z",
        strokePathData = "M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z M22 10v6 M6 12.5V16a6 3 0 0 0 12 0v-3.5",
        fillAlpha = 0.22f
    ),

    // 12. TrendingUp (日均学习 / 上升趋势) Lucide trending-up.svg
    PlumpIcon.TrendingUp to PlumpIconPathDefinition(
        fillPathData = "M22 7l-8.5 8.5-5-5L2 17v5h20z",
        strokePathData = "M16 7h6v6 M22 7l-8.5 8.5-5-5L2 17",
        fillAlpha = 0.16f
    ),

    // 13. BrainCircuit (全库记忆全景 / 脑回路神经元) Lucide brain-circuit.svg
    PlumpIcon.BrainCircuit to PlumpIconPathDefinition(
        fillPathData = "M6.294 6.294A3 3 0 1 1 12 5v13a4 4 0 1 1-7.967-.518z",
        strokePathData = "M12 13h4 M12 18h6a2 2 0 0 1 2 2v1 M12 8h8 M12 9a4.17 4.17 0 0 1-3 4 M16 8V5a2 2 0 0 1 2-2 M6 18a4 4 0 0 1-2-7.465 M6.004 5.125a4 4 0 0 0-2.526 5.77 M6.294 6.294A3 3 0 1 1 12 5v13a4 4 0 1 1-7.967-.518 M16 13a0.5 0.5 0 1 0 0.001 0 M18 3a0.5 0.5 0 1 0 0.001 0 M20 21a0.5 0.5 0 1 0 0.001 0 M20 8a0.5 0.5 0 1 0 0.001 0",
        fillAlpha = 0.20f
    ),

    // 14. Zap (累计活跃 / 能量闪电) Lucide zap.svg
    PlumpIcon.Zap to PlumpIconPathDefinition(
        fillPathData = "M15.914 4a1.5 1.5 0 0 0-2.474-1.561l-9 9A1.5 1.5 0 0 0 5.5 14h4.002a.5.5 0 0 1 .471.666L8.086 20a1.5 1.5 0 0 0 2.475 1.56l9-9A1.5 1.5 0 0 0 18.5 10h-3.997a.5.5 0 0 1-.472-.667z",
        strokePathData = "M15.914 4a1.5 1.5 0 0 0-2.474-1.561l-9 9A1.5 1.5 0 0 0 5.5 14h4.002a.5.5 0 0 1 .471.666L8.086 20a1.5 1.5 0 0 0 2.475 1.56l9-9A1.5 1.5 0 0 0 18.5 10h-3.997a.5.5 0 0 1-.472-.667z",
        fillAlpha = 0.22f
    )
)
