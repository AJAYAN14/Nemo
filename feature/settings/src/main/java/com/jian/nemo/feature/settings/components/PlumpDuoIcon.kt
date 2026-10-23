package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser

/**
 * 设置项 Plump Duo 双色丰满图标类型枚举
 *
 * 采用现代线面立体设计规范：
 * 1. 结构层：精炼标准、几何网格严格统一的矢量线条；
 * 2. 视觉层：取消传统沉重的纯色方块背景，采用“主色高对比圆润描边 + 局部柔光浅色面填充”的双色层次；
 * 3. 适用场景：设置列表项、状态指示与功能入口等界面元素的通透呈现。
 */
enum class PlumpIcon {
    User,             // 登录 / 注册
    CloudUpload,      // 备份到云端
    CloudBackup,      // 云端备份历史
    ThemePalette,     // 主题外观
    DailyGoalTarget,  // 每日单词目标
    GrammarLayers,    // 每日语法目标
    StudyClock,       // 学习时间段
    RandomShuffle,    // 随机抽取
    MemoryBrain,      // 记忆算法
    AiBot,            // AI 助手 / 设置
    AudioVolume,      // 语音发音
    ExportDownload,   // 导出数据
    ImportUpload,     // 导入数据
    ResetTrash,       // 重置进度
    IconLab,          // 图标实验室
    LiquidLab,        // 液态玻璃实验室
    GitHub,           // GitHub 项目
    CheckUpdate       // 检查更新
}

/**
 * 感知均匀等亮度调色板 (Isoluminant / Perceptually Uniform Palette)
 * 基于 OKLCH 感知色彩模型构建：
 * 统一锁定亮度 Lightness L = 62%，统一锁定色度 Chroma C = 0.165。
 * 每一个色彩拥有完全相等的视觉重量感（Visual Weight）与能量级，杜绝某些图标刺眼、某些图标发暗的失衡问题。
 */
object IsoluminantPalette {
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

    /**
     * 根据色相角动态生成感知均匀等亮度色彩 (OKLCH 色彩模型)
     *
     * 保持感知亮度与色度绝对一致，允许色相角在 0° ~ 360° 之间连续取值，
     * 为新模块提供无限的专属等亮度色彩扩展能力，避免颜色写死或全 App 重复。
     *
     * @param hueDegrees 色相角度 (0.0f ~ 360.0f)
     * @param lightness 统一感知亮度 (默认 0.62f)
     * @param chroma 统一鲜艳度 (默认 0.165f)
     */
    fun fromOklch(
        hueDegrees: Float,
        lightness: Float = 0.62f,
        chroma: Float = 0.165f
    ): Color {
        val hRad = Math.toRadians(hueDegrees.toDouble())
        val a = chroma * kotlin.math.cos(hRad)
        val b = chroma * kotlin.math.sin(hRad)

        val lPrime = lightness + 0.3963377774 * a + 0.2158037573 * b
        val mPrime = lightness - 0.1055613458 * a - 0.0638541728 * b
        val sPrime = lightness - 0.0894841775 * a - 1.2914855480 * b

        val l = lPrime * lPrime * lPrime
        val m = mPrime * mPrime * mPrime
        val s = sPrime * sPrime * sPrime

        val rLin = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
        val gLin = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
        val bLin = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

        fun gamma(x: Double): Float {
            val clamped = x.coerceIn(0.0, 1.0)
            val v = if (clamped <= 0.0031308) {
                12.92 * clamped
            } else {
                1.055 * Math.pow(clamped, 1.0 / 2.4) - 0.055
            }
            return v.toFloat().coerceIn(0f, 1f)
        }

        return Color(
            red = gamma(rLin),
            green = gamma(gLin),
            blue = gamma(bLin),
            alpha = 1.0f
        )
    }
}

/**
 * 内部图标路径定义元数据
 */
private data class PlumpIconPathDefinition(
    val fillPathData: String?,
    val strokePathData: String,
    val fillAlpha: Float = 0.22f,
    val strokeWidth: Float = 2.1f
)

private val PlumpIconRegistry: Map<PlumpIcon, PlumpIconPathDefinition> = mapOf(
    // 1. User
    PlumpIcon.User to PlumpIconPathDefinition(
        fillPathData = "M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 Z M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2 Z",
        strokePathData = "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2 M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0",
        fillAlpha = 0.24f
    ),

    // 2. CloudUpload
    PlumpIcon.CloudUpload to PlumpIconPathDefinition(
        fillPathData = "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242 4.5 4.5 0 0 1-4.21 3.758H7a4.5 4.5 0 0 1-3-7.1z",
        strokePathData = "M12 12v9 M8 16l4-4 4 4 M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242 4.5 4.5 0 0 1-4.21 3.758H7a4.5 4.5 0 0 1-3-7.1z",
        fillAlpha = 0.24f
    ),

    // 3. CloudBackup
    PlumpIcon.CloudBackup to PlumpIconPathDefinition(
        fillPathData = "M21 15.251A4.5 4.5 0 0 0 17.5 8h-1.79A7 7 0 1 0 3 13.607 Z",
        strokePathData = "M21 15.251A4.5 4.5 0 0 0 17.5 8h-1.79A7 7 0 1 0 3 13.607 M7 11v4h4 M8 19a5 5 0 0 0 9-3 4.5 4.5 0 0 0-4.5-4.5 4.82 4.82 0 0 0-3.41 1.41L7 15",
        fillAlpha = 0.22f
    ),

    // 4. ThemePalette
    PlumpIcon.ThemePalette to PlumpIconPathDefinition(
        fillPathData = "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z",
        strokePathData = "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z M13.5 6.5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M17.5 10.5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M6.5 12.5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M8.5 7.5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0",
        fillAlpha = 0.22f
    ),

    // 5. DailyGoalTarget
    PlumpIcon.DailyGoalTarget to PlumpIconPathDefinition(
        fillPathData = "M18 12a6 6 0 1 1-12 0 6 6 0 0 1 12 0 Z M13.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0 Z",
        strokePathData = "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0 M18 12a6 6 0 1 1-12 0 6 6 0 0 1 12 0 M14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0",
        fillAlpha = 0.26f
    ),

    // 6. GrammarLayers
    PlumpIcon.GrammarLayers to PlumpIconPathDefinition(
        fillPathData = "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z",
        strokePathData = "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12 M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17",
        fillAlpha = 0.25f
    ),

    // 7. StudyClock
    PlumpIcon.StudyClock to PlumpIconPathDefinition(
        fillPathData = "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0 Z",
        strokePathData = "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0 M12 6v6l4 2",
        fillAlpha = 0.22f
    ),

    // 8. RandomShuffle
    PlumpIcon.RandomShuffle to PlumpIconPathDefinition(
        fillPathData = "M7 6a4 4 0 0 1 4 4v4a4 4 0 0 1-4 4H3V6z",
        strokePathData = "m18 14 4 4-4 4 M18 2 22 6 18 10 M2 18h1.973a4 4 0 0 0 3.3-1.7l5.454-8.6a4 4 0 0 1 3.3-1.7H22 M2 6h1.972a4 4 0 0 1 3.6 2.2 M22 18h-6.041a4 4 0 0 1-3.3-1.8l-.359-.45",
        fillAlpha = 0.18f
    ),

    // 9. MemoryBrain
    PlumpIcon.MemoryBrain to PlumpIconPathDefinition(
        fillPathData = "M17.707 6.294A3 3 0 1 0 12 5a3 3 0 1 0-5.707 1.294 4 4 0 0 0-2.526 5.77 4 4 0 0 0 2 7.465 4 4 0 0 0 7.967.517 4 4 0 0 0 7.967-.517 4 4 0 0 0 2-7.465 4 4 0 0 0-2.526-5.77 Z",
        strokePathData = "M12 18V5 M15 13a4.17 4.17 0 0 1-3-4 4.17 4.17 0 0 1-3 4 M17.707 6.294A3 3 0 1 0 12 5a3 3 0 1 0-5.707 1.294 M17.996 5.125a4 4 0 0 1 2.526 5.77 M18 18a4 4 0 0 0 2-7.465 M19.967 17.482a4 4 0 1 1-7.967.517 4 4 0 1 1-7.967-.517 M6 18a4 4 0 0 1-2-7.465 M6.004 5.125a4 4 0 0 0-2.526 5.77",
        fillAlpha = 0.22f
    ),

    // 10. AiBot
    PlumpIcon.AiBot to PlumpIconPathDefinition(
        fillPathData = "M6 8h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2 Z",
        strokePathData = "M12 8V4H8 M6 8h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2 M2 14h2 M20 14h2 M15 13v2 M9 13v2",
        fillAlpha = 0.22f
    ),

    // 11. AudioVolume
    PlumpIcon.AudioVolume to PlumpIconPathDefinition(
        fillPathData = "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z",
        strokePathData = "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z M16 9a5 5 0 0 1 0 6 M19.364 18.364a9 9 0 0 0 0-12.728",
        fillAlpha = 0.24f
    ),

    // 12. ExportDownload
    PlumpIcon.ExportDownload to PlumpIconPathDefinition(
        fillPathData = "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 Z",
        strokePathData = "M12 15V3 M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7 10l5 5 5-5",
        fillAlpha = 0.22f
    ),

    // 13. ImportUpload
    PlumpIcon.ImportUpload to PlumpIconPathDefinition(
        fillPathData = "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 Z",
        strokePathData = "M12 3v12 M17 8 12 3 7 8 M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
        fillAlpha = 0.22f
    ),

    // 14. ResetTrash
    PlumpIcon.ResetTrash to PlumpIconPathDefinition(
        fillPathData = "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 Z",
        strokePathData = "M10 11v6 M14 11v6 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M3 6h18 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
        fillAlpha = 0.20f
    ),

    // 15. IconLab (FlaskConical)
    PlumpIcon.IconLab to PlumpIconPathDefinition(
        fillPathData = "M6.453 15h11.094l2.198 4.02A2 2 0 0 1 18 22H6a2 2 0 0 1-1.755-2.96L6.453 15z",
        strokePathData = "M14 2v6a2 2 0 0 0 .245.96l5.51 10.08A2 2 0 0 1 18 22H6a2 2 0 0 1-1.755-2.96l5.51-10.08A2 2 0 0 0 10 8V2 M6.453 15h11.094 M8.5 2h7",
        fillAlpha = 0.32f
    ),

    // 16. LiquidLab (Droplets)
    PlumpIcon.LiquidLab to PlumpIconPathDefinition(
        fillPathData = "M7 16.3c2.2 0 4-1.83 4-4.05 0-1.16-.57-2.26-1.71-3.19S7.29 6.75 7 5.3c-.29 1.45-1.14 2.84-2.29 3.76S3 11.1 3 12.25c0 2.22 1.8 4.05 4 4.05z",
        strokePathData = "M7 16.3c2.2 0 4-1.83 4-4.05 0-1.16-.57-2.26-1.71-3.19S7.29 6.75 7 5.3c-.29 1.45-1.14 2.84-2.29 3.76S3 11.1 3 12.25c0 2.22 1.8 4.05 4 4.05z M12.56 6.6A10.97 10.97 0 0 0 14 3.02c.5 2.5 2 4.9 4 6.5s3 3.5 3 5.5a6.98 6.98 0 0 1-11.91 4.97",
        fillAlpha = 0.26f
    ),

    // 17. GitHub
    PlumpIcon.GitHub to PlumpIconPathDefinition(
        fillPathData = "M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0 1 12 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0 0 22 12.017C22 6.484 17.522 2 12 2z",
        strokePathData = "M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0 1 12 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0 0 22 12.017C22 6.484 17.522 2 12 2z",
        fillAlpha = 0.20f
    ),

    // 18. CheckUpdate (RefreshCw)
    PlumpIcon.CheckUpdate to PlumpIconPathDefinition(
        fillPathData = "M12 21a9 9 0 1 1 9-9 9 9 0 0 1-9 9 Z",
        strokePathData = "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8 M21 3v5h-5 M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16 M8 16H3v5",
        fillAlpha = 0.16f
    )
)

/**
 * Plump Duo 双色丰满矢量图标渲染组件
 *
 * 将目标矢量图标渲染为具备“饱满主色描边 + 局部柔光浅色填充”的高品质双层微质感图形。
 *
 * @param icon 目标图标定义
 * @param color 图标主色调 (自动衍生深色轮廓与感知一致的浅色底面)
 * @param modifier 修饰符
 * @param size 渲染尺寸规格 (默认 28.dp 丰满视觉规格)
 */
@Composable
fun PlumpDuoIcon(
    icon: PlumpIcon,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val def = PlumpIconRegistry[icon] ?: return

    val fillComposePath: Path? = remember(def.fillPathData) {
        def.fillPathData?.let {
            try {
                PathParser.createPathFromPathData(it).asComposePath()
            } catch (_: Exception) {
                null
            }
        }
    }

    val strokeComposePath: Path? = remember(def.strokePathData) {
        try {
            PathParser.createPathFromPathData(def.strokePathData).asComposePath()
        } catch (_: Exception) {
            null
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val scaleFactor = this.size.minDimension / 24f
        scale(scaleFactor, Offset.Zero) {
            // 1. 绘制 Plump Duo 浅色软填充底面 (Soft Fill Surface)
            if (fillComposePath != null) {
                drawPath(
                    path = fillComposePath,
                    color = color.copy(alpha = def.fillAlpha),
                    style = Fill
                )
            }

            // 2. 绘制 Lucide 原生饱满描边层 (Bold Round Stroke)
            if (strokeComposePath != null) {
                drawPath(
                    path = strokeComposePath,
                    color = color,
                    style = Stroke(
                        width = def.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
