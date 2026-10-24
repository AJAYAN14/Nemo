package com.jian.nemo.core.designsystem.icon

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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 设置项与功能入口 Plump Duo 双色丰满图标类型枚举
 *
 * 采用现代线面立体设计规范：
 * 1. 结构层：精炼标准、几何网格严格统一的矢量线条（基于 Lucide 原生 24x24 坐标系）；
 * 2. 视觉层：取消传统沉重的纯色方块背景，采用“主色高对比圆润描边 + 局部柔光浅色面填充”的双色层次；
 * 3. 适用场景：设置列表项、状态指示与功能入口等界面元素的通透呈现。
 */
enum class PlumpIcon {
    // --- 1. 系统与通用设置分支 (PlumpIconsSettings.kt) ---
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
    CheckUpdate,      // 检查更新
    Moon,             // 开启深色模式 / 月亮
    Sun,              // 结束深色模式 / 太阳
    IconStyle,        // 图标风格选择 / 几何图形

    // --- 2. 语音与媒体参数分支 (PlumpIconsMedia.kt) ---
    Mic,              // 日语语音 / 麦克风
    Languages,        // 中文语音 / 多语言
    Gauge,            // 语速调节 / 速度仪表盘
    MusicNote,        // 音调调节 / 音乐音符

    // --- 3. 用户与账户安全分支 (PlumpIconsUser.kt) ---
    UserPen,          // 修改用户名 / 用户资料编辑
    Mail,             // 修改邮箱 / 电子信件
    KeyRound,         // 重置密码 / 安全密钥
    UserX,            // 注销账户 / 移除用户

    // --- 4. 进度与统计分支 (PlumpIconsProgress.kt) ---
    Gamepad,          // 专项训练 / 游戏手柄
    Calendar,         // 学习日历 / 日历看板
    ChartPie,         // 今日统计 / 饼图概览
    History,          // 历史统计 / 历史时钟
    TrendingDown,     // 遗忘曲线 / 记忆衰减趋势
    SpellCheck,       // 单词列表 / 拼写检查
    LayoutGrid,       // 专项词汇 / 布局网格
    Sparkles,         // 地道表达 / 闪耀光芒
    Flame,            // 复学清单 / 火焰难点

    // --- 5. 测试与练习分支 (PlumpIconsTest.kt) ---
    CircleX,          // 我的错题 / 圆形叉号
    Star,             // 我的收藏 / 收藏星标
    ListChecks,       // 选择题 / 多项清单
    Keyboard,         // 手打题 / 打字键盘
    GalleryVerticalEnd, // 卡片题 / 纵向层叠卡片
    ArrowDownUp,      // 排序题 / 上下双向排序
    Infinity,         // 能力工坊 / 无穷进阶

    // --- 6. 首页与概览分支 (PlumpIconsHome.kt) ---
    BadgeCheck,       // 新学达成 / 成就解锁徽章
    Trophy,           // 热力图 / 单日最佳 / 荣誉奖杯
    BookOpen,         // AI 阅读 / 主题选择 / 展开书本
    Play,             // 开始学习 / 播放三角形
    ChevronsRight,    // 继续学习 / 推进双前锋
    Trees,            // 日常生活 / 双生森林
    Landmark,         // 传统文化 / 历史地标神殿
    Utensils,         // 美食料理 / 刀叉餐具
    Compass,          // 名胜风光 / 罗盘指针
    Briefcase,        // 商务工作 / 公文包
    GraduationCap,    // JLPT 级别 / 学士帽
    TrendingUp,       // 日均学习 / 上升趋势
    BrainCircuit,     // 全库记忆全景 / 脑回路神经元
    Zap,              // 累计活跃 / 能量闪电

    // --- 7. 专项分类与 AI 工坊分支 (PlumpIconsCategory.kt) ---
    Activity,          // 动词类 / 活力脉冲
    SlidersHorizontal, // 副词类 / 横向调节滑块
    Link2,             // 连体词 / 链接纽带
    Workflow,          // 接续词 / 工作流转
    Megaphone,         // 感叹词 / 扩音喇叭
    Puzzle,            // 助词 / 拼图标记
    ArrowRight,        // 接头词 / 向右前锋
    ArrowLeft,         // 接尾词 / 向左后撤
    Quote,             // 表达·固定句 / 引用语录
    Globe,             // 外来语 / 环球语言
    Cpu,               // 工坊 AI 芯片 / 计算单元
    CircleQuestion,    // 工坊帮助指南 / 疑问标识
    RotateCcw,         // 工坊换一题 / 逆时针重置
    Lightbulb,         // 工坊提示灯泡 / 灵感启迪
    SearchX,           // 工坊空状态 / 无检索结果
    Search,            // 搜索放大镜 / 检索输入
    X                  // 清除与关闭 / 叉号标记
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
internal data class PlumpIconPathDefinition(
    val fillPathData: String?,
    val strokePathData: String,
    val fillAlpha: Float = 0.22f,
    val strokeWidth: Float = 2.1f
)

/**
 * 全局图标聚合注册表（按业务域由各分片注册表自动合并，杜绝单文件臃肿）
 */
private val PlumpIconRegistry: Map<PlumpIcon, PlumpIconPathDefinition> by lazy {
    PlumpIconsSettingsRegistry +
        PlumpIconsMediaRegistry +
        PlumpIconsUserRegistry +
        PlumpIconsProgressRegistry +
        PlumpIconsTestRegistry +
        PlumpIconsHomeRegistry +
        PlumpIconsCategoryRegistry
}

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
                PathParser().parsePathString(it).toPath()
            } catch (_: Exception) {
                null
            }
        }
    }

    val strokeComposePath: Path? = remember(def.strokePathData) {
        try {
            PathParser().parsePathString(def.strokePathData).toPath()
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
