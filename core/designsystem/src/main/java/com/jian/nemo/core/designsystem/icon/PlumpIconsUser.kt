package com.jian.nemo.core.designsystem.icon

/**
 * 用户与账户安全分支 Plump Duo 图标路径注册表
 *
 * 覆盖：修改用户名、修改邮箱、重置密码密钥、注销账户危险区等用户安全类功能图标。
 * 几何基准：严格遵循 Lucide 原生 24×24 网格规范。
 */
internal val PlumpIconsUserRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. UserPen (账户管理 - 修改用户名) Lucide user-pen.svg
    PlumpIcon.UserPen to PlumpIconPathDefinition(
        fillPathData = "M14 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z M21.378 16.626a1 1 0 0 0-3.004-3.004l-4.01 4.012a2 2 0 0 0-.506.854l-.837 2.87a.5.5 0 0 0 .62.62l2.87-.837a2 2 0 0 0 .854-.506z",
        strokePathData = "M11.5 15H7a4 4 0 0 0-4 4v2 M21.378 16.626a1 1 0 0 0-3.004-3.004l-4.01 4.012a2 2 0 0 0-.506.854l-.837 2.87a.5.5 0 0 0 .62.62l2.87-.837a2 2 0 0 0 .854-.506z M14 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0",
        fillAlpha = 0.22f
    ),

    // 2. Mail (账户管理 - 修改邮箱) Lucide mail.svg
    PlumpIcon.Mail to PlumpIconPathDefinition(
        fillPathData = "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        strokePathData = "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M22 7l-8.991 5.727a2 2 0 0 1-2.009 0L2 7",
        fillAlpha = 0.22f
    ),

    // 3. KeyRound (账户管理 - 重置密码) Lucide key-round.svg
    PlumpIcon.KeyRound to PlumpIconPathDefinition(
        fillPathData = "M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z",
        strokePathData = "M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z M17 7.5a.5.5 0 1 1-1 0 .5.5 0 0 1 1 0",
        fillAlpha = 0.22f
    ),

    // 4. UserX (账户管理 - 注销账户) Lucide user-x.svg
    PlumpIcon.UserX to PlumpIconPathDefinition(
        fillPathData = "M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2z",
        strokePathData = "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M17 8l5 5 M22 8l-5 5",
        fillAlpha = 0.24f
    )
)
