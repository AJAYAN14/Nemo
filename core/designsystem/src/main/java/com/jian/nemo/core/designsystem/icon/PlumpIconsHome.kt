package com.jian.nemo.core.designsystem.icon

/**
 * 首页与概览分支 Plump Duo 图标路径注册表
 *
 * 覆盖：今日学习达成徽章、首页统计指示等。
 * 几何基准：严格遵循 Lucide 原生 24×24 网格规范。
 */
internal val PlumpIconsHomeRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. BadgeCheck (新学达成 / 成就解锁) Lucide badge-check.svg
    PlumpIcon.BadgeCheck to PlumpIconPathDefinition(
        fillPathData = "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z",
        strokePathData = "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z M16 9l-5.5 5.5L8 12",
        fillAlpha = 0.22f
    )
)
