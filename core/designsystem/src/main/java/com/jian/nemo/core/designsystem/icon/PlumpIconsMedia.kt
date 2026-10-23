package com.jian.nemo.core.designsystem.icon

/**
 * 语音与音频媒体分支 Plump Duo 图标路径注册表
 *
 * 覆盖：TTS 语音参数、语言选择、语速仪表盘、音调音乐等音频媒体类功能图标。
 * 几何基准：严格遵循 Lucide 原生 24×24 网格规范。
 */
internal val PlumpIconsMediaRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. Mic (语音参数 - 日语语音) Lucide mic.svg
    PlumpIcon.Mic to PlumpIconPathDefinition(
        fillPathData = "M12 2a3 3 0 0 1 3 3v7a3 3 0 0 1-6 0V5a3 3 0 0 1 3-3z",
        strokePathData = "M12 19v3 M19 10v2a7 7 0 0 1-14 0v-2 M12 2a3 3 0 0 1 3 3v7a3 3 0 0 1-6 0V5a3 3 0 0 1 3-3z",
        fillAlpha = 0.24f
    ),

    // 2. Languages (语音参数 - 中文语音) Lucide languages.svg
    PlumpIcon.Languages to PlumpIconPathDefinition(
        fillPathData = "M17 12l5 10H12z",
        strokePathData = "m5 8 6 6 M4 14l6-6 2-3 M2 5h12 M7 2h1 M22 22l-5-10-5 10 M14 18h6",
        fillAlpha = 0.22f
    ),

    // 3. Gauge (语音参数 - 语速调节) Lucide gauge.svg
    PlumpIcon.Gauge to PlumpIconPathDefinition(
        fillPathData = "M3.34 19a10 10 0 1 1 17.32 0L12 14z",
        strokePathData = "m12 14 4-4 M3.34 19a10 10 0 1 1 17.32 0",
        fillAlpha = 0.20f
    ),

    // 4. MusicNote (语音参数 - 音调调节) Lucide eighth-notes.svg
    PlumpIcon.MusicNote to PlumpIconPathDefinition(
        fillPathData = "M21 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z M9 19a3 3 0 1 1-6 0 3 3 0 0 1 6 0z",
        strokePathData = "M21 16V3a1 1 0 0 0-1.28-.96l-10 2.917a1 1 0 0 0-.72.96V19 M21 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0 M9 19a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
        fillAlpha = 0.26f
    )
)
