package com.jian.nemo.core.designsystem.icon

/**
 * 进度与统计分支 Plump Duo 图标路径注册表
 *
 * 覆盖：专项训练、学习日历、今日统计、历史统计、遗忘曲线、单词列表、专项词汇、地道表达、复学清单等。
 * 几何基准：严格遵循 Lucide 原生 24×24 网格规范。
 */
internal val PlumpIconsProgressRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. Gamepad (专项训练) Lucide gamepad-2.svg
    PlumpIcon.Gamepad to PlumpIconPathDefinition(
        fillPathData = "M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z",
        strokePathData = "M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z M6 11h4 M8 9v4 M15 12h.01 M18 10h.01",
        fillAlpha = 0.20f
    ),

    // 2. Calendar (学习日历) Lucide calendar.svg
    PlumpIcon.Calendar to PlumpIconPathDefinition(
        fillPathData = "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        strokePathData = "M8 2v3 M16 2v3 M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M3 9h18",
        fillAlpha = 0.18f
    ),

    // 3. ChartPie (今日统计) Lucide chart-pie.svg
    PlumpIcon.ChartPie to PlumpIconPathDefinition(
        fillPathData = "M21 12c.552 0 1.005-.449.95-.998a10 10 0 0 0-8.953-8.951c-.55-.055-.998.398-.998.95v8a1 1 0 0 0 1 1z",
        strokePathData = "M21 12c.552 0 1.005-.449.95-.998a10 10 0 0 0-8.953-8.951c-.55-.055-.998.398-.998.95v8a1 1 0 0 0 1 1z M21.21 15.89A10 10 0 1 1 8 2.83",
        fillAlpha = 0.26f
    ),

    // 4. History (历史统计) Lucide rotate-ccw-clock.svg
    PlumpIcon.History to PlumpIconPathDefinition(
        fillPathData = "M12 3a9 9 0 1 1-9 9c0-.5.04-1 .12-1.5z",
        strokePathData = "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8 M3 3v5h5 M12 7v5l4 2",
        fillAlpha = 0.16f
    ),

    // 5. TrendingDown (遗忘曲线) Lucide trending-down.svg
    PlumpIcon.TrendingDown to PlumpIconPathDefinition(
        fillPathData = "M2 7l6.5 6.5 5-5L22 17v4H2z",
        strokePathData = "M16 17h6v-6 M22 17l-8.5-8.5-5 5L2 7",
        fillAlpha = 0.18f
    ),

    // 6. SpellCheck (单词列表) Lucide spell-check.svg
    PlumpIcon.SpellCheck to PlumpIconPathDefinition(
        fillPathData = "M6 12l4-8 4 8z",
        strokePathData = "m20 15-5.5 5.5L12 18 M4 16 10 4l5.115 10.23 M6 12h8",
        fillAlpha = 0.24f
    ),

    // 7. LayoutGrid (专项词汇) Lucide layout-grid.svg
    PlumpIcon.LayoutGrid to PlumpIconPathDefinition(
        fillPathData = "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
        strokePathData = "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
        fillAlpha = 0.25f
    ),

    // 8. Sparkles (地道表达) Lucide sparkles.svg
    PlumpIcon.Sparkles to PlumpIconPathDefinition(
        fillPathData = "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z",
        strokePathData = "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z M20 2v4 M22 4h-4 M4 22a2 2 0 1 0 0-4 2 2 0 0 0 0 4z",
        fillAlpha = 0.22f
    ),

    // 9. Flame (复学清单) Lucide flame.svg
    PlumpIcon.Flame to PlumpIconPathDefinition(
        fillPathData = "M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4z",
        strokePathData = "M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4",
        fillAlpha = 0.24f
    )
)
