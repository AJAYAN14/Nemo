# 动词活用与 AI 模块双轨风格与 Google Fonts 原生图标 2:3 比例落地经验

> 本文档记录动词活用（手册与练习）、AI 工坊（主页与历史）、AI 阅读（主页与历史）等模块在推进 UI 规范对齐、Google Fonts 原生实心图标纯正化、2:3 黄金比例阶梯适配、双轨单容器架构与系统调色盘（`IosColors`）收敛过程中的关键避坑经验。

---

## 核心避坑对照表 (四列格式)

| 坑是什么 (Problem) | 怎么避 (Workaround / Solution) | 相关工具 / 文件 | 统一原则 (Design Axiom) |
| :--- | :--- | :--- | :--- |
| **第三方杂合图标散落**<br>历史代码中引用了已废弃的第三方矢量图标与调色板，不仅破坏了应用整体的纯正 Google Fonts 风格，且数据模型中存在过时的冗余字段。 | **彻底清理第三方依赖并收拢至原生实心图标**：全量清除未经验证的第三方图标引用；从 `GrammarData` 中移除废弃冗余字段；全量采用 Google Fonts 原生 Material 实心图标（`Icons.Rounded.*` 与 `Icons.AutoMirrored.Rounded.*`），色彩统一收敛至标准 `IosColors`。 | `VerbConjugationHandbookScreen.kt`<br>`AIWorkshopScreen.kt`<br>`AIReadingScreen.kt`<br>`AIReadingHistoryScreen.kt` | **官方图标纯正化与调色盘单源公理**：全项目图标一律收编至 Google Fonts 原生 Material 图标系列；色彩唯一真实来源为 `:core:designsystem` 的 `Color.kt` / `IosColors`，彻底杜绝杂合第三方库。 |
| **盲目统一尺寸破坏多层级信息密度**<br>动词手册的分类卡片、行内发音按钮、空状态底座和 Hero 引导卡片视觉层级各异，若教条式一刀切改为 42dp，将严重破坏紧凑卡片的信息密度或削弱大 Hero 的视觉张力。 | **精准落实多阶梯 2:3 黄金比例尺寸公理**：<br>1. **Hero / 重点警告底座**：84dp 底座配 56dp 图标（2:3）；<br>2. **空状态底座**：72dp 底座配 48dp 图标（2:3）；<br>3. **工坊练习大卡片**：48dp 底座配 32dp 图标（2:3）；<br>4. **标准主题 / 历史卡片**：42dp 底座配 28dp 图标（2:3）；<br>5. **行内操作 / 发音按钮**：36dp 底座配 24dp 图标（2:3）；<br>6. **紧凑标题装饰 / 折叠箭头**：30dp 底座配 20dp 图标（2:3）。全量使用 G2 连续平滑曲率 `RoundedRectangle`。 | `VerbConjugationScreen.kt`<br>`AbilityWorkshopScreen.kt`<br>`AIWorkshopScreen.kt`<br>`AIReadingScreen.kt` | **2:3 黄金比例公理与严防一刀切原则**：带有背景色块底框的图标必须严格满足 `图标 = 底座 * 2/3`，留白为 `底座 * 1/6`；根据界面层级与密度依规选用对应阶梯，严禁无脑一刀切。 |
| **双轨风格分裂分支代码冗余**<br>历史代码在处理“柔和微透”（SOFT_TRANSLUCENT）与“活力纯色”（VIBRANT_SOLID）时，曾采用分裂写法，不仅代码冗余度高，且风格切换时图标尺寸和形状存在跳变。 | **单容器计算属性复用架构**：统一为单 `Surface`/`Box` + 单 `Icon` 结构，将风格差异完全收敛至局部计算属性：<br>1. `containerBg = if (isSoftTranslucent) iconColor.copy(alpha = 0.15f) else iconColor`<br>2. `iconTint = if (isSoftTranslucent) iconColor else Color.White`<br>两套风格下图标物理尺寸与底框圆角曲率绝对恒定。 | `AbilityWorkshopScreen.kt`<br>`AIReadingScreen.kt`<br>`AIReadingHistoryScreen.kt`<br>`VerbConjugationHandbookScreen.kt` | **单源同色与单容器复用公理**：双轨外观风格必须共享完全相同的同一份图标资产与基准颜色（`iconColor`），底层一律采用单容器计算属性，两套风格切换物理尺寸绝对零跳变。 |
| **答题状态与类型标签硬编码散落色**<br>AI 历史与 AI 阅读历史卡片中的答题状态标签、分数展示标签使用了散落的裸色值或已废弃的调色板色值，缺乏对深色模式的对比度适配。 | **规范化映射至 IosColors 深浅自适应色阶**：将全部标签色彩（绿色正确、红色错误、橙色进行中、蓝紫类别）收编至 `IosColors.Green` / `GreenDark`、`IosColors.Red` / `RedDark`、`IosColors.Orange` / `OrangeDark` 等，浅色模式下背景设为 `0.10f` 透明度，深色模式下背景设为 `0.16f` 透明度以保障最佳对比度与层次感。 | `AIHistoryScreen.kt`<br>`AIReadingHistoryScreen.kt` | **动态色彩阶梯与深浅自适应原则**：界面强调色必须使用规范系统调色盘，并通过深浅色阶（如 `GreenDark`）与半透明基底实现暗黑模式无障碍高对比度阅读。 |

---

## 落地规范检查台账

1. **图标资产**：100% Google Fonts 原生 Material 实心图标（`Icons.Rounded.*` / `Icons.AutoMirrored.Rounded.*`），0 处第三方杂合库残留。
2. **2:3 黄金比例**：
   - 84dp 底座 -> 56dp 图标（2:3，留白 14dp）
   - 72dp 底座 -> 48dp 图标（2:3，留白 12dp）
   - 48dp 底座 -> 32dp 图标（2:3，留白 8dp）
   - 42dp 底座 -> 28dp 图标（2:3，留白 7dp）
   - 36dp 底座 -> 24dp 图标（2:3，留白 6dp）
   - 30dp 底座 -> 20dp 图标（2:3，留白 5dp）
3. **曲率规范**：色块底框统一使用 G2 连续平滑曲率（`RoundedRectangle`）。
4. **双轨架构**：单容器（单 Surface/Box + 单 Icon）计算属性复用，尺寸恒定。
5. **色彩规范**：全量收敛至 `IosColors` 与标准调色盘，零裸色值字面量硬编码。
