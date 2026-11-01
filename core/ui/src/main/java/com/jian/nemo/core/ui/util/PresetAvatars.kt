package com.jian.nemo.core.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.designsystem.theme.UiPalette
import com.jian.nemo.core.ui.R

/**
 * 预设头像管理器
 * 提供一组预设的渐变色头像供用户选择
 */
object PresetAvatars {

    /**
     * 预设头像数据类
     * @param id 唯一标识
     * @param nameRes 头像名称资源 ID
     * @param colors 渐变色列表
     * @param emoji 代表emoji（可选）
     */
    data class PresetAvatar(
        val id: String,
        @StringRes val nameRes: Int,
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
            nameRes = R.string.core_ui_preset_avatar_sunset,
            colors = UiPalette.Presets.Sunset,
            emoji = "🌅"
        ),
        PresetAvatar(
            id = "ocean",
            nameRes = R.string.core_ui_preset_avatar_ocean,
            colors = UiPalette.Presets.Ocean,
            emoji = "🌊"
        ),
        PresetAvatar(
            id = "forest",
            nameRes = R.string.core_ui_preset_avatar_forest,
            colors = UiPalette.Presets.Forest,
            emoji = "🌲"
        ),
        PresetAvatar(
            id = "lavender",
            nameRes = R.string.core_ui_preset_avatar_lavender,
            colors = UiPalette.Presets.Lavender,
            emoji = "💜"
        ),

        // 现代系列
        PresetAvatar(
            id = "mint",
            nameRes = R.string.core_ui_preset_avatar_mint,
            colors = UiPalette.Presets.Mint,
            emoji = "🍃"
        ),
        PresetAvatar(
            id = "coral",
            nameRes = R.string.core_ui_preset_avatar_coral,
            colors = UiPalette.Presets.Coral,
            emoji = "🪸"
        ),
        PresetAvatar(
            id = "golden",
            nameRes = R.string.core_ui_preset_avatar_golden,
            colors = UiPalette.Presets.Golden,
            emoji = "✨"
        ),
        PresetAvatar(
            id = "aurora",
            nameRes = R.string.core_ui_preset_avatar_aurora,
            colors = UiPalette.Presets.Aurora,
            emoji = "🌌"
        ),

        // 温暖系列
        PresetAvatar(
            id = "peach",
            nameRes = R.string.core_ui_preset_avatar_peach,
            colors = UiPalette.Presets.Peach,
            emoji = "🍑"
        ),
        PresetAvatar(
            id = "autumn",
            nameRes = R.string.core_ui_preset_avatar_autumn,
            colors = UiPalette.Presets.Autumn,
            emoji = "🍂"
        ),

        // 清新系列
        PresetAvatar(
            id = "sky",
            nameRes = R.string.core_ui_preset_avatar_sky,
            colors = UiPalette.Presets.Sky,
            emoji = "☁️"
        ),
        PresetAvatar(
            id = "lemon",
            nameRes = R.string.core_ui_preset_avatar_lemon,
            colors = UiPalette.Presets.Lemon,
            emoji = "🍋"
        ),

        // 神秘系列
        PresetAvatar(
            id = "midnight",
            nameRes = R.string.core_ui_preset_avatar_midnight,
            colors = UiPalette.Presets.Midnight,
            emoji = "🌙"
        ),
        PresetAvatar(
            id = "galaxy",
            nameRes = R.string.core_ui_preset_avatar_galaxy,
            colors = UiPalette.Presets.Galaxy,
            emoji = "🌟"
        ),

        // 活力系列
        PresetAvatar(
            id = "tropical",
            nameRes = R.string.core_ui_preset_avatar_tropical,
            colors = UiPalette.Presets.Tropical,
            emoji = "🏖️"
        ),
        PresetAvatar(
            id = "cherry",
            nameRes = R.string.core_ui_preset_avatar_cherry,
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

/**
 * 预设头像显示名称扩展属性 (Composable)
 */
val PresetAvatars.PresetAvatar.name: String
    @Composable
    get() = stringResource(nameRes)

/**
 * 预设头像显示名称获取方法 (非 Composable)
 */
fun PresetAvatars.PresetAvatar.getName(context: Context): String = context.getString(nameRes)
