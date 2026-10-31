package com.jian.nemo.core.ui.util

import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.designsystem.theme.UiPalette

/**
 * 预设头像管理器
 * 提供一组预设的渐变色头像供用户选择
 */
object PresetAvatars {

    /**
     * 预设头像数据类
     * @param id 唯一标识
     * @param name 头像名称
     * @param colors 渐变色列表
     * @param emoji 代表emoji（可选）
     */
    data class PresetAvatar(
        val id: String,
        val name: String,
        val colors: List<Color>,
        val emoji: String? = null
    )

    /**
     * 预设头像列表
     */
    val presets = listOf(
        // 经典系列
        PresetAvatar(
            id = "sunset",
            name = "日落",
            colors = UiPalette.Presets.Sunset,
            emoji = "🌅"
        ),
        PresetAvatar(
            id = "ocean",
            name = "海洋",
            colors = UiPalette.Presets.Ocean,
            emoji = "🌊"
        ),
        PresetAvatar(
            id = "forest",
            name = "森林",
            colors = UiPalette.Presets.Forest,
            emoji = "🌲"
        ),
        PresetAvatar(
            id = "lavender",
            name = "薰衣草",
            colors = UiPalette.Presets.Lavender,
            emoji = "💜"
        ),

        // 现代系列
        PresetAvatar(
            id = "mint",
            name = "薄荷",
            colors = UiPalette.Presets.Mint,
            emoji = "🍃"
        ),
        PresetAvatar(
            id = "coral",
            name = "珊瑚",
            colors = UiPalette.Presets.Coral,
            emoji = "🪸"
        ),
        PresetAvatar(
            id = "golden",
            name = "金色",
            colors = UiPalette.Presets.Golden,
            emoji = "✨"
        ),
        PresetAvatar(
            id = "aurora",
            name = "极光",
            colors = UiPalette.Presets.Aurora,
            emoji = "🌌"
        ),

        // 温暖系列
        PresetAvatar(
            id = "peach",
            name = "蜜桃",
            colors = UiPalette.Presets.Peach,
            emoji = "🍑"
        ),
        PresetAvatar(
            id = "autumn",
            name = "秋叶",
            colors = UiPalette.Presets.Autumn,
            emoji = "🍂"
        ),

        // 清新系列
        PresetAvatar(
            id = "sky",
            name = "天空",
            colors = UiPalette.Presets.Sky,
            emoji = "☁️"
        ),
        PresetAvatar(
            id = "lemon",
            name = "柠檬",
            colors = UiPalette.Presets.Lemon,
            emoji = "🍋"
        ),

        // 神秘系列
        PresetAvatar(
            id = "midnight",
            name = "午夜",
            colors = UiPalette.Presets.Midnight,
            emoji = "🌙"
        ),
        PresetAvatar(
            id = "galaxy",
            name = "星系",
            colors = UiPalette.Presets.Galaxy,
            emoji = "🌟"
        ),

        // 活力系列
        PresetAvatar(
            id = "tropical",
            name = "热带",
            colors = UiPalette.Presets.Tropical,
            emoji = "🏖️"
        ),
        PresetAvatar(
            id = "cherry",
            name = "樱花",
            colors = UiPalette.Presets.Cherry,
            emoji = "🌸"
        )
    )

    /**
     * 根据ID获取预设头像
     */
    fun getById(id: String): PresetAvatar? {
        return presets.find { it.id == id }
    }

    /**
     * 获取随机预设头像
     */
    fun getRandom(): PresetAvatar {
        return presets.random()
    }

    /**
     * 根据用户名获取推荐的预设头像
     * （基于用户名哈希，确保同一用户总是相同的推荐）
     */
    fun getRecommendedForUser(username: String): PresetAvatar {
        val hash = username.hashCode()
        val index = kotlin.math.abs(hash) % presets.size
        return presets[index]
    }

    // ========================================================================
    // 协议定义
    // ========================================================================

    const val PRESET_PREFIX = "preset:"

    /**
     * 检查路径是否为预设头像协议
     */
    fun isPreset(path: String?): Boolean = path?.startsWith(PRESET_PREFIX) == true

    /**
     * 从协议路径中解析预设ID
     */
    fun getPresetId(path: String?): String? {
        if (!isPreset(path)) return null
        return path?.removePrefix(PRESET_PREFIX)
    }

    /**
     * 生成预设头像协议路径
     */
    fun createPresetPath(id: String): String = "$PRESET_PREFIX$id"
}
