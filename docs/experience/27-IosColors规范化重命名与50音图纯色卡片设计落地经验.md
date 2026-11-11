# 27 - IosColors 规范化重命名与 50 音图纯色卡片设计落地经验

> 本文档记录了将全工程色彩体系中带有平台限定命名的 `IosColors` 全量迁移至标准 `SystemColors`，以及 50 音图假名卡片按调色盘实现同一行同色纯色块设计的实战避坑与落地规范。

---

## 避坑复盘与四列对照表

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **平台特定命名残留与历史别名冗余**：色彩命名带 `Ios` 前缀容易产生平台语义偏见，且伴随大量过时的历史别名（如 `@Deprecated val iOSRed`），导致命名混乱。 | 统一收拢为中立、通用的 **`SystemColors`**，严格对齐 `docs/design/color-palette/COLORS.md` 系统动态主色调标准；原子化清理无引用的过时历史别名。 | [`Color.kt`](file:///e:/Android/Nemo/core/designsystem/src/main/java/com/jian/nemo/core/designsystem/theme/Color.kt), 全工程 69 个调用处 | **中立系统命名与单一真实源公理**：设计系统底层代号一律脱离特定外部系统私有名词，保持架构自洽与纯净。 |
| **纯色背景多色冲突与视觉统一性**：若个别高明度色相（如黄色行）单独切换为深色字，会导致全屏网格文字色彩分裂，破坏界面排版的纯净与一致感。 | 尊重视觉一致性原则，将全量假名卡片文字统一反白（`Color.White`，罗马音 `Color.White.copy(0.85f)`），播放状态统一纯白发光描边，确保全屏视觉秩序高度统一。 | [`KanaChartScreen.kt`](file:///e:/Android/Nemo/feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/kana/KanaChartScreen.kt) | **视觉秩序与全量统一反白原则**：高密度网格纯色块界面中，文字颜色应保持全局绝对一致，杜绝局部分裂。 |
| **行数与调色盘取模重复**：50 音图清音共有 11 行（含“ん行”），原数组仅 10 种颜色，取模会导致首尾两行出现同色冲突。 | 将 `CategoryRowColors` 完备扩充至 11 色（引入 `SystemColors.Red`），深浅模式各阶梯严格对齐深色自发光光学补偿色（`RedDark`, `IndigoDark` 等）。 | [`KanaChartScreen.kt`](file:///e:/Android/Nemo/feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/kana/KanaChartScreen.kt) | **行列语义对齐与色彩不重复原则**：多行分类色彩映射必须全量覆盖业务行数，保障各行视觉层级与区隔明确。 |
| **占位符卡片视觉噪点过多**：占位卡片（如 `(い)`, `(え)`）与空白格若处理失当，在浓郁纯色卡片网格中会显得突兀杂乱。 | 占位符卡片采用对应行纯色 10%~15% 的低不透明度微透填充配合弱化同色边框；空白格保持微弱同色虚线，实现整齐有序。 | [`KanaChartScreen.kt`](file:///e:/Android/Nemo/feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/kana/KanaChartScreen.kt) | **层级降噪与网格秩序感公理**：次要占位元素通过弱化同色系半透明实现降噪，不破坏主网格节奏。 |

---

## 关键成果与影响面

1. **核心重构**：
   - 全工程完成 `IosColors` -> `SystemColors` 原子化平滑替换，彻底清除包含 `iOS` 命名字符的 20 处遗留废弃别名。
   - 全工程 69 个 Kotlin 源文件编译验证 100% 通过（`BUILD SUCCESSFUL`）。
2. **50 音图纯色设计落地**：
   - 清音 11 行、浊音 5 行、拗音 11 行每行均独占调色盘标准系统色彩。
   - 假名卡片背景填充浓郁纯色，支持明度动态判定与深浅模式自发光补偿，兼具极强的活力感与清晰的阅读体验。
