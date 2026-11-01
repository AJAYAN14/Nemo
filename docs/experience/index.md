# Nemo 开发经验与避坑索引

> 本文档汇总 Nemo 项目在架构重构、算法实现、UI 动效与交互流程中沉淀的技术经验与避坑复盘。

---

## 活跃经验清单

| 序号 | 经验文档 | 涉及模块 / 关键词 | 核心原则与收获 | 状态 |
| :---: | :--- | :--- | :--- | :--- |
| 01 | [会话结算统计重复计算与互斥三分类修复](file:///e:/Android/Nemo/docs/experience/01-会话结算统计重复计算与互斥三分类修复.md) | `StatsTimerDelegate`, `LearningFinishedContent`, `LearningUiState` / 结算统计、重学去重、一词两算 | **互斥分类原则**：包含关系子集必须做互斥划分，杜绝重复计算；派生统计统一收敛在 UiState 作为单一数据源。 | 活跃 |
| 02 | [自适应应用图标扩展与SVG转换避坑](file:///e:/Android/Nemo/docs/experience/02-自适应应用图标扩展与SVG转换避坑.md) | `AppIconManager`, `ThemeSettingsScreen`, `VectorDrawable` / 自适应图标、SVG转换、动态别名切换 | **纯净矢量与原子替换原则**：SVG统一收敛为绝对坐标pathData；别名切换先起后禁以锁住桌面位置。 | 活跃 |
| 03 | [动态色彩架构与深浅模式明度阶梯优化避坑](file:///e:/Android/Nemo/docs/experience/03-动态色彩架构与深浅模式明度阶梯优化避坑.md) | `Theme.kt`, `Color.kt`, `MainActivity.kt` / OLED纯黑、明度阶梯、光学补偿、窗口底色 | **OLED纯黑底色与表面亮度阶梯公理**：纯黑为底逐级提亮表达纵深，深浅模式强调色光学明度补偿，窗口与画布底色一体化。 | 活跃 |
| 04 | [悬浮液态玻璃顶栏遮罩范式与实验室深浅色排版避坑](file:///e:/Android/Nemo/docs/experience/04-悬浮液态玻璃顶栏遮罩范式与实验室深浅色排版避坑.md) | `ActivityHeatmapScreen`, `CommonHeader`, `CornerLabScreen` / 液态玻璃顶栏、四阶渐变遮罩、物理折射、曲率梳、深浅色自适应 | **黄金范式单一来源公理**：全应用高阶动效与悬浮折射顶栏必须统一继承热力图标杆实现，严禁后续 AI 各自臆造参数；源码排查严禁黑盒外部脚本。 | 活跃 |
| 05 | [Compose与ViewModel文本硬编码收编避坑](file:///e:/Android/Nemo/docs/experience/05-Compose硬编码文本收编与ViewModel上下文避坑.md) | `AIReadingScreen`, `AIReadingViewModel`, `CustomTextToolbar` / 字符串提取、remember非组合闭包、@ApplicationContext、独占资源文件 | **单一数据源国际化与资源独占原则**：ViewModel提示统一通过ApplicationContext收编；并行重构资源文件独占隔离，文案100%忠实源码零臆造。 | 活跃 |
| 05 | [多AI并行UI字符串收编与ViewModel上下文避坑](file:///e:/Android/Nemo/docs/experience/05-多AI并行UI字符串收编与ViewModel上下文避坑.md) | `AIWorkshopScreen`, `AIWorkshopViewModel`, `strings_learning_ai_workshop.xml` / 专属资源隔离、ViewModel上下文、零臆造原则 | **文本原始忠实公理与专属资源独占原则**：多AI并行重构采用独立资源分片防冲突，严格逐字提取源码字面量绝不生造，ViewModel统一经由ApplicationContext解析。 | 活跃 |
| 06 | [词库大厅UI文本收编与ViewModel上下文注入避坑](file:///e:/Android/Nemo/docs/experience/06-词库大厅UI文本收编与ViewModel上下文注入避坑.md) | `CategoryWordsViewModel`, `PosWordsViewModel`, `ExpressionScreen`, `strings_library.xml` / 专属资源分片、Hilt上下文注入、@StringRes延迟消费 | **文本源码绝对忠实公理与资源解耦延迟解析原则**：严禁主观编造UI文本，非Composable仅传递@StringRes ID延迟解析，ViewModel通过ApplicationContext保持纯净。 | 活跃 |
| 07 | [G2连续曲率圆角与液态玻璃SDF透镜着色器适配避坑](file:///e:/Android/Nemo/docs/experience/07-G2连续曲率圆角与液态玻璃SDF透镜着色器适配避坑.md) | `Lens.kt`, `HighlightStyle.kt`, `RoundedRectangularShape` / G2连续曲率、Backdrop、SDF着色器、透镜折射、高光边缘 | **自定义图形抽象接口贯穿全栈原则**：底层图形学着色器几何解析必须强绑定Shape体系；引入新Shape必须端到端覆盖Shader切点解析，杜绝断言硬中断。 | 活跃 |
| 08 | [SVG紧凑语法解析与iOS风格图标加权避坑](file:///e:/Android/Nemo/docs/experience/08-SVG紧凑语法解析与iOS风格图标加权避坑.md) | `ic_lucide_*.xml`, `SquircleSettingItem`, `SettingsScreen.kt` / SVG紧凑压缩语法、Arc命令Flag粘连、PathParser兼容性、Squircle单色图标加权2.5 | **格式规范化与字重光学适配原则**：矢量路径必须严格遵守 Android AAPT 空格规范，杜绝省略粘连语法；小尺寸单色线面图标采用光学加权（Semibold 2.5）保障视觉力量感与秩序感。 | 活跃 |
| 09 | [设置项双轨风格统一与GoogleFonts原生图标2比3比例规范](file:///e:/Android/Nemo/docs/experience/09-设置项双轨风格统一与GoogleFonts原生图标2比3比例规范.md) | `SettingsScreen.kt`, `SettingsComponents.kt`, `Icons.Rounded` / Google Fonts 原生实心图标、2:3黄金比例公理、G2连续平滑曲率、单源同色单容器架构 | **官方单一来源与场景边界公理**：全项目图标一律采用 Google Fonts 原生实心 Rounded；42/28 仅限主设置项列表，严禁泛化至空状态 100x100 等其它非设置项场景；双轨风格单源同色单容器复用，杜绝第三方混杂库与分支冗余代码。 | 活跃 |
| 10 | [单词与语法列表官方原生图标与双轨单容器架构避坑](file:///e:/Android/Nemo/docs/experience/10-单词与语法列表官方原生图标与双轨单容器架构避坑.md) | `WordListScreen.kt`, `GrammarListScreen.kt`, `LibraryListComponents.kt` / Google Fonts 原生实心图标、防一刀切、双轨单容器计算属性、调色盘收敛 | **2:3比例公理与场景防一刀切原则**：空状态底块72dp对应48dp图标（2:3），保留卡片原有50dp紧凑信息密度；双轨单源同色单容器复用，全链路收纳至标准系统色盘。 | 活跃 |
| 11 | [今日统计与历史统计双轨图标与调色盘重构避坑指南](file:///e:/Android/Nemo/docs/experience/11-今日统计与历史统计双轨图标与调色盘重构避坑指南.md) | `StatisticsScreen.kt`, `HistoricalStatisticsScreen.kt`, `LevelBreakdownDialog.kt` / Google Fonts 原生实心图标、36dp紧凑卡片、G2连续平滑曲率、单源同色单容器、IosColors调色盘 | **2:3黄金比例与场景阶梯公理**：紧凑指标卡片36dp底块对应24dp图标（2:3），空状态48dp底块对应32dp图标（2:3）；单容器计算属性复用，全量收拢至原生Material图标与标准调色盘。 | 活跃 |
| 12 | [遗忘曲线与学习日历双轨2比3微卡片与调色板重构避坑指南](file:///e:/Android/Nemo/docs/experience/12-遗忘曲线与学习日历双轨2比3微卡片与调色板重构避坑指南.md) | `ForgettingCurveScreen.kt`, `ForgettingCurveChart.kt`, `LearningCalendarScreen.kt`, `ActivityRingsComponent.kt` / Google Fonts 原生实心图标、24dp微阶梯底框、2:3黄金比例、顶栏四阶渐变遮罩黄金范式、调色盘收拢 | **2:3黄金比例严防一刀切与黄金范式单一来源公理**：紧凑2x2微卡片选用24dp底框配16dp原生图标（2:3），底框使用G2平滑圆角；顶栏全面遵循48dp大液态玻璃按钮与四阶渐变遮罩范式；色彩完全收编至调色盘动态主色。 | 活跃 |
| 13 | [动词活用与AI模块双轨风格与GoogleFonts原生图标2比3比例落地经验](file:///e:/Android/Nemo/docs/experience/13-动词活用与AI模块双轨风格与GoogleFonts原生图标2比3比例落地经验.md) | `VerbConjugationHandbookScreen.kt`, `VerbConjugationScreen.kt`, `AbilityWorkshopScreen.kt`, `AIWorkshopScreen.kt`, `AIHistoryScreen.kt`, `AIReadingScreen.kt`, `AIReadingHistoryScreen.kt` / Google Fonts 原生实心图标、2:3 黄金比例阶梯、双轨单容器架构、IosColors 调色盘收敛 | **官方图标纯正化与多阶梯2:3黄金比例公理**：彻底根除第三方杂合图标，全面收拢至原生Material图标；依场景严谨选用84dp/72dp/48dp/42dp/36dp/30dp多阶梯（均为2:3比例），G2连续平滑曲率；单容器复用架构尺寸绝对恒定；色彩100%忠实系统标准调色盘。 | 活跃 |

---

## 经验编写与维护规范
1. **四列格式**：每篇经验文件必须包含四列对照表（“坑是什么 → 怎么避 → 相关工具/文件 → 统一原则”）。
2. **单一数据源**：新发现的技术坑点与最佳实践需在本文档登记索引。
3. **归档机制**：当活跃经验文件超过 5 份时进行阶段性归纳与归档。
