package com.jian.nemo.core.designsystem.icon

/**
 * 测试与练习分支 Plump Duo 图标路径注册表
 *
 * 覆盖：我的错题、我的收藏、选择题、手打题、卡片题、排序题、能力工坊等。
 * 几何基准：严格遵循 Lucide 原生 24×24 网格规范。
 */
internal val PlumpIconsTestRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. CircleX (我的错题) Lucide circle-x.svg
    PlumpIcon.CircleX to PlumpIconPathDefinition(
        fillPathData = "M12 2a10 10 0 1 1 0 20 10 10 0 0 1 0-20z",
        strokePathData = "M12 2a10 10 0 1 1 0 20 10 10 0 0 1 0-20z M15 9l-6 6 M9 9l6 6",
        fillAlpha = 0.18f
    ),

    // 2. Star (我的收藏) Lucide star.svg
    PlumpIcon.Star to PlumpIconPathDefinition(
        fillPathData = "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
        strokePathData = "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
        fillAlpha = 0.24f
    ),

    // 3. ListChecks (选择题) Lucide list-checks.svg
    PlumpIcon.ListChecks to PlumpIconPathDefinition(
        fillPathData = "M5 10a3 3 0 1 1 0-6 3 3 0 0 1 0 6z M5 20a3 3 0 1 1 0-6 3 3 0 0 1 0 6z",
        strokePathData = "M13 5h8 M13 12h8 M13 19h8 M3 7l2 2 4-4 M3 17l2 2 4-4",
        fillAlpha = 0.22f
    ),

    // 4. Keyboard (手打题) Lucide keyboard.svg
    PlumpIcon.Keyboard to PlumpIconPathDefinition(
        fillPathData = "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        strokePathData = "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M6 8h.01 M10 8h.01 M14 8h.01 M18 8h.01 M8 12h.01 M12 12h.01 M16 12h.01 M7 16h10",
        fillAlpha = 0.18f
    ),

    // 5. GalleryVerticalEnd (卡片题) Lucide gallery-vertical-end.svg
    PlumpIcon.GalleryVerticalEnd to PlumpIconPathDefinition(
        fillPathData = "M5 10h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z",
        strokePathData = "M7 2h10 M5 6h14 M5 10h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z",
        fillAlpha = 0.22f
    ),

    // 6. ArrowDownUp (排序题) Lucide arrow-down-up.svg
    PlumpIcon.ArrowDownUp to PlumpIconPathDefinition(
        fillPathData = "M3 16l4 4 4-4z M21 8l-4-4-4 4z",
        strokePathData = "m3 16 4 4 4-4 M7 20V4 m14 4-4-4-4 4 M17 4v16",
        fillAlpha = 0.25f
    ),

    // 7. Infinity (能力工坊) Lucide infinity.svg
    PlumpIcon.Infinity to PlumpIconPathDefinition(
        fillPathData = "M6 16c5 0 7-8 12-8a4 4 0 0 1 0 8c-5 0-7-8-12-8a4 4 0 1 0 0 8",
        strokePathData = "M6 16c5 0 7-8 12-8a4 4 0 0 1 0 8c-5 0-7-8-12-8a4 4 0 1 0 0 8",
        fillAlpha = 0.20f
    )
)
