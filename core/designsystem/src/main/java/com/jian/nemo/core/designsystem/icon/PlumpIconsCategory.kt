package com.jian.nemo.core.designsystem.icon

/**
 * 专项分类与 AI 工坊分支 Plump Duo 图标路径注册表
 *
 * 覆盖：
 * - 专项训练分类：动词(Activity)、副词(SlidersHorizontal)、连体词(Link2)、接续词(Workflow)、
 *   感叹词(Megaphone)、助词(Puzzle)、接头词(ArrowRight)、接尾词(ArrowLeft)、表达·固定句(Quote)、外来语(Globe)
 * - AI 例文工坊：自定义芯片(Cpu)、帮助指南(CircleQuestion)、换一题(RotateCcw)、提示灯泡(Lightbulb)、空状态(SearchX)
 *
 * 几何基准：严格遵循 Lucide 原生 24×24 网格规范。
 */
internal val PlumpIconsCategoryRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. Activity (动词类) Lucide activity.svg
    PlumpIcon.Activity to PlumpIconPathDefinition(
        fillPathData = "M2 12h2.49a2 2 0 0 0 1.93-1.46l2.35-8.36a.25.25 0 0 1 .48 0l5.52 19.64a.25.25 0 0 0 .48 0l2.35-8.36A2 2 0 0 1 19.52 12H22v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2z",
        strokePathData = "M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2",
        fillAlpha = 0.16f
    ),

    // 2. SlidersHorizontal (副词类) Lucide sliders-horizontal.svg
    PlumpIcon.SlidersHorizontal to PlumpIconPathDefinition(
        fillPathData = "M12 3h4v4h-4z M6 10h4v4H6z M14 17h4v4h-4z",
        strokePathData = "M10 5H3 M12 19H3 M14 3v4 M16 17v4 M21 12h-9 M21 19h-5 M21 5h-7 M8 10v4 M8 12H3",
        fillAlpha = 0.24f
    ),

    // 3. Link2 (连体词) Lucide link-2.svg
    PlumpIcon.Link2 to PlumpIconPathDefinition(
        fillPathData = "M9 17H7A5 5 0 0 1 7 7h2z M15 7h2a5 5 0 1 1 0 10h-2z",
        strokePathData = "M9 17H7A5 5 0 0 1 7 7h2 M15 7h2a5 5 0 1 1 0 10h-2 M8 12h8",
        fillAlpha = 0.20f
    ),

    // 4. Workflow (接续词) Lucide workflow.svg
    PlumpIcon.Workflow to PlumpIconPathDefinition(
        fillPathData = "M5 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M15 13h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z",
        strokePathData = "M5 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M7 11v4a2 2 0 0 0 2 2h4 M15 13h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z",
        fillAlpha = 0.22f
    ),

    // 5. Megaphone (感叹词) Lucide megaphone.svg
    PlumpIcon.Megaphone to PlumpIconPathDefinition(
        fillPathData = "M11 6a13 13 0 0 0 8.4-2.8A1 1 0 0 1 21 4v12a1 1 0 0 1-1.6.8A13 13 0 0 0 11 14H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z",
        strokePathData = "M11 6a13 13 0 0 0 8.4-2.8A1 1 0 0 1 21 4v12a1 1 0 0 1-1.6.8A13 13 0 0 0 11 14H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z M6 14a12 12 0 0 0 2.4 7.2 2 2 0 0 0 3.2-2.4A8 8 0 0 1 10 14 M8 6v8",
        fillAlpha = 0.22f
    ),

    // 6. Puzzle (助词) Lucide puzzle.svg
    PlumpIcon.Puzzle to PlumpIconPathDefinition(
        fillPathData = "M15.39 4.39a1 1 0 0 0 1.68-.474 2.5 2.5 0 1 1 3.014 3.015 1 1 0 0 0-.474 1.68l1.683 1.682a2.414 2.414 0 0 1 0 3.414L19.61 15.39a1 1 0 0 1-1.68-.474 2.5 2.5 0 1 0-3.014 3.015 1 1 0 0 1 .474 1.68l-1.683 1.682a2.414 2.414 0 0 1-3.414 0L8.61 19.61a1 1 0 0 0-1.68.474 2.5 2.5 0 1 1-3.014-3.015 1 1 0 0 0 .474-1.68l-1.683-1.682a2.414 2.414 0 0 1 0-3.414L4.39 8.61a1 1 0 0 1 1.68.474 2.5 2.5 0 1 0 3.014-3.015 1 1 0 0 1-.474-1.68l1.683-1.682a2.414 2.414 0 0 1 3.414 0z",
        strokePathData = "M15.39 4.39a1 1 0 0 0 1.68-.474 2.5 2.5 0 1 1 3.014 3.015 1 1 0 0 0-.474 1.68l1.683 1.682a2.414 2.414 0 0 1 0 3.414L19.61 15.39a1 1 0 0 1-1.68-.474 2.5 2.5 0 1 0-3.014 3.015 1 1 0 0 1 .474 1.68l-1.683 1.682a2.414 2.414 0 0 1-3.414 0L8.61 19.61a1 1 0 0 0-1.68.474 2.5 2.5 0 1 1-3.014-3.015 1 1 0 0 0 .474-1.68l-1.683-1.682a2.414 2.414 0 0 1 0-3.414L4.39 8.61a1 1 0 0 1 1.68.474 2.5 2.5 0 1 0 3.014-3.015 1 1 0 0 1-.474-1.68l1.683-1.682a2.414 2.414 0 0 1 3.414 0z",
        fillAlpha = 0.22f
    ),

    // 7. ArrowRight (接头词) Lucide arrow-right.svg
    PlumpIcon.ArrowRight to PlumpIconPathDefinition(
        fillPathData = "M12 5l7 7-7 7z",
        strokePathData = "M5 12h14 M12 5l7 7-7 7",
        fillAlpha = 0.22f
    ),

    // 8. ArrowLeft (接尾词) Lucide arrow-left.svg
    PlumpIcon.ArrowLeft to PlumpIconPathDefinition(
        fillPathData = "M12 19l-7-7 7-7z",
        strokePathData = "M12 19l-7-7 7-7 M19 12H5",
        fillAlpha = 0.22f
    ),

    // 9. Quote (表达·固定句) Lucide quote.svg
    PlumpIcon.Quote to PlumpIconPathDefinition(
        fillPathData = "M16 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z M5 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z",
        strokePathData = "M16 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z M5 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z",
        fillAlpha = 0.22f
    ),

    // 10. Globe (外来语) Lucide globe.svg
    PlumpIcon.Globe to PlumpIconPathDefinition(
        fillPathData = "M22 12a10 10 0 1 0-20 0 10 10 0 1 0 20 0z",
        strokePathData = "M22 12a10 10 0 1 0-20 0 10 10 0 1 0 20 0z M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20 M2 12h20",
        fillAlpha = 0.16f
    ),

    // 11. Cpu (工坊自定义 AI 芯片) Lucide cpu.svg
    PlumpIcon.Cpu to PlumpIconPathDefinition(
        fillPathData = "M9 8h6a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z",
        strokePathData = "M12 20v2 M12 2v2 M17 20v2 M17 2v2 M2 12h2 M2 17h2 M2 7h2 M20 12h2 M20 17h2 M20 7h2 M7 20v2 M7 2v2 M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M9 8h6a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z",
        fillAlpha = 0.25f
    ),

    // 12. CircleQuestion (工坊帮助指南) Lucide circle-question-mark.svg
    PlumpIcon.CircleQuestion to PlumpIconPathDefinition(
        fillPathData = "M22 12a10 10 0 1 0-20 0 10 10 0 1 0 20 0z",
        strokePathData = "M22 12a10 10 0 1 0-20 0 10 10 0 1 0 20 0z M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3 M12 17h.01",
        fillAlpha = 0.16f
    ),

    // 13. RotateCcw (工坊换一题刷新) Lucide rotate-ccw.svg
    PlumpIcon.RotateCcw to PlumpIconPathDefinition(
        fillPathData = "M12 3a9 9 0 1 1-9 9c0-.5.04-1 .12-1.5z",
        strokePathData = "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8 M3 3v5h5",
        fillAlpha = 0.16f
    ),

    // 14. Lightbulb (工坊提示灯泡) Lucide lightbulb.svg
    PlumpIcon.Lightbulb to PlumpIconPathDefinition(
        fillPathData = "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5 H9z",
        strokePathData = "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5 M9 18h6 M10 22h4",
        fillAlpha = 0.22f
    ),

    // 15. SearchX (工坊无数据空状态) Lucide search-x.svg
    PlumpIcon.SearchX to PlumpIconPathDefinition(
        fillPathData = "M19 11a8 8 0 1 0-16 0 8 8 0 1 0 16 0z",
        strokePathData = "m13.5 8.5-5 5 M8.5 8.5 5 5 M19 11a8 8 0 1 0-16 0 8 8 0 1 0 16 0z M21 21l-4.3-4.3",
        fillAlpha = 0.18f
    ),

    // 16. Search (搜索放大镜) Lucide search.svg
    PlumpIcon.Search to PlumpIconPathDefinition(
        fillPathData = "M19 11a8 8 0 1 0-16 0 8 8 0 1 0 16 0z",
        strokePathData = "M19 11a8 8 0 1 0-16 0 8 8 0 1 0 16 0z M21 21l-4.34-4.34",
        fillAlpha = 0.20f
    ),

    // 17. X (清除/关闭叉号) Lucide x.svg
    PlumpIcon.X to PlumpIconPathDefinition(
        fillPathData = null,
        strokePathData = "M18 6L6 18 M6 6l12 12",
        fillAlpha = 0f
    )
)
