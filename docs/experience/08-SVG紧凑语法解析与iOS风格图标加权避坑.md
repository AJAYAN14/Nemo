# 08-SVG紧凑语法解析与iOS风格图标加权避坑

> 记录外部 SVG 转换为 Android 原生 VectorDrawable 时紧凑语法导致的 Path 解析错乱、以及 iOS 风格 Squircle 纯色底框单色图标字重加权的工程避坑实践。

---

## 核心复盘与避坑对照

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **SVG 紧凑省略语法与 Android PathParser 冲突**：部分 SVG 图标（如 `brain.svg`）采用 Web 压缩语法，将圆弧命令 `A`/`a` 的单字符标志位（Flag）与坐标数字粘连（如 `A 3 3 0 1012 5`、`0 11-7.967`）。Android 平台 PathParser 无法容错粘连 flag，会将 `1012` 误解析为 X 坐标，导致圆弧严重错位、线条交叉飞出视口。 | 建立标准 SVG Path Normalizer 分词器，对 Arc 指令的 7 项标准参数（`rx ry rotation large-arc sweep x y`）及正负数、小数点严格补全单个空格与前导 0，生成严格符合 Android AAPT2 规范的 `pathData`。 | `core/designsystem/src/main/res/drawable/ic_lucide_*.xml` | **格式规范化与防粘连原则**：Android 原生矢量路径必须保证命令与所有参数用标准空格隔开，严禁直接照搬 Web 压缩紧凑语法。 |
| **小尺寸单色线框图标视觉分量单薄**：当图标置于 32dp 彩色 Squircle 纯色底框内并收敛至 18dp 时，默认的 2.0px 细描边在移动设备高分屏与亮色底对比下会显单薄、缺乏系统级秩序感。 | 对小尺寸嵌入式单色图标进行光学字重加权，统一将 `android:strokeWidth` 从 `2.0`（Regular）平滑提升至 `2.5`（Semibold），兼顾孔洞细节与扎实力量感；容器统一采用 42dp G2 连续曲率超椭圆（`RoundedRectangle(12.dp)`），图标尺寸设定为 28dp，严格对齐 **2 : 3** 黄金饱满比例。 | `core/designsystem/src/main/res/drawable/ic_lucide_*.xml`, `SquircleSettingItem` | **字重光学适配与黄金比例原则**：小尺寸纯色容器内的反白矢量图标必须配合光学加权（Semibold 2.5 描边），底框采用 G2 连续曲率平滑圆角，图标与色块保持 2:3 紧凑饱满度。 |
| **双轨风格维护与设计系统解耦**：单一硬编码样式无法满足 Material Design 3 经典大圆角与 iOS 极简紧凑 Squircle 双风格并存的用户诉求。 | 在 `SquircleSettingItem` 底层根据全局 `iconStyle` 做双轨分支自适应渲染（MD3 模式呈现 42dp 浅底+彩色图标；iOS 模式呈现 32dp 纯色底+纯白图标），由统一的 Lucide 矢量资源单一数据源驱动。 | `feature/settings/.../SettingsComponents.kt`, `SettingsScreen.kt` | **单一数据源多态呈现原则**：基础矢量资产保持唯一纯净，呈现层依据主题风格自适应分流，实现真正的无缝双轨维护。 |

---

## 涉及文件清单
- `core/designsystem/src/main/res/drawable/ic_lucide_*.xml` (17 款规范化矢量资源)
- `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/SettingsComponents.kt`
- `feature/settings/src/main/java/com/jian/nemo/feature/settings/SettingsScreen.kt`
- `docs/experience/index.md`
