# 09-设置项双轨风格统一与GoogleFonts原生图标2比3比例规范

## 背景与痛点 (Context & Problem)

在近期 Nemo 设置页面的视觉迭代中，经历了一系列设计尝试与技术排坑：
1. **多源第三方图标混杂**：引入了外部第三方杂合 SVG 图标，部分压缩 SVG 中包含紧凑省略语法（如 Arc 指令中 Flag 与坐标粘连 `0 1012 5`），导致 Android 原生 PathParser 解析失败、图形飞出视口；同时不同来源图标在笔触、粗细、圆角及内边缘留白上完全脱节。
2. **比例失衡与拥挤感**：初始图标尺寸（22dp）在纯色高饱和底块中显得中心缩小空荡；后改为 28dp 时，两套风格的代码却分裂成两段 `if-else` 分支，造成 Compose 容器代码冗余，且 MD3 模式下参数一度飘移。
3. **实心与线框形态不搭**：混用了实心 Rounded 与线框 Outline（如 `DeleteOutline` 与 `FileDownload` 同屏），导致视觉分量轻重不一。

经过与用户的深度对齐与推敲，确立了以 **Google Fonts 原生 Rounded 系列为基石、严格 2:3 尺寸比例公理、G2 连续平滑曲率、单源同色单容器架构** 的终极解决方案。

---

## 四列经验对照表 (Pitfall Avoidance Matrix)

| 坑是什么 (Pitfall) | 怎么避 (Avoidance Strategy) | 相关工具 / 文件 | 统一原则 (Unified Principle) |
| :--- | :--- | :--- | :--- |
| **第三方矢量语法畸变与画风分裂**<br>引入 Lucide、Streamline 等外部库，导致 AAPT 紧凑坐标解析异常飞线，各设置项粗细与设计语言支离破碎。 | **全面收编至 Google Fonts 官方原生库**：统一采用 `androidx.compose.material.icons.rounded.*`。官方确实缺少的极特殊第三方品牌（如 GitHub）走严格白名单准入并进行空格规范化展开。 | `SettingsScreen.kt`<br>`material-icons-extended`<br>`Icons.Rounded.*` | **官方优先与单一来源公理**：全项目图标一律以 Google Fonts 原生实心 Rounded 为基准，杜绝外部混杂图标库。 |
| **底块留白失衡与空荡/拥挤**<br>图标尺寸随意设定（22dp、24dp、32dp），导致在实心底色下单薄空旷，或在浅色底框下顶格拥挤。 | **死守 2:3 黄金比例公理**：容器 42dp 底块时，图标尺寸必须严格为 `42 * (2/3) = 28dp`，四周留白精准锁定为 7dp。无论何种模式，图标物理尺寸恒定不变。 | `SettingsComponents.kt`<br>`SquircleSettingItem` | **2:3 黄金比例视觉公理**：所有带底块的图标容器，图标尺寸与底块边长必须严格遵守 2:3 比例，杜绝随心所欲设定参数。 |
| **切点折痕与生硬圆弧**<br>使用常规 `RoundedCornerShape`，在连续高阶曲率与大屏幕下切点处呈现明显的直线与圆弧转折痕迹。 | **必须使用 G2 连续平滑曲率**：底块 Shape 必须调用 `RoundedRectangle(12.dp)`（底层默认绑定 `RoundedCornerStyle.Continuous` 三阶贝塞尔拟合）。 | `RoundedRectangle.kt`<br>`ContinuousCurvature...CornerBuilder.kt` | **G2 平滑连续曲率公理**：全应用圆角一律采用 G2 连续平滑曲率，消除二阶导数突变折痕。 |
| **双轨风格分裂与冗余分支代码**<br>为“柔和微透”与“活力纯色”写了两套 `Box` 和 `Icon`，导致组件代码量翻倍，颜色传递易产生偏差。 | **单容器计算属性复用范式**：提取 `isSoftTranslucent` 状态，`containerBg = if (isSoft) iconColor.copy(0.15f) else iconColor`，`iconTint = if (isSoft) iconColor else White`，单一 `Box` + 单一 `Icon` 渲染。 | `SettingsComponents.kt`<br>`LocalSettingIconStyle` | **单源同色单容器架构原则**：双轨外观必须共享同一图标资产与同一基准色彩，通过计算属性单容器实现，严禁代码重复分裂。 |
| **实心与线框图标混杂造成的轻重不一**<br>列表中同时出现实心填充图标（如 `Memory`）与空心线框图标（如 `DeleteOutline`），导致视觉分量参差不齐。 | **全页统一为实心 Rounded 图标**：全面替换为实心版（如 `Icons.Rounded.Delete`、`Icons.Rounded.SmartToy`），保证每个底块内的正负形面积与视觉力量感均等。 | `SettingsScreen.kt`<br>`Icons.Rounded.Delete`<br>`Icons.Rounded.SmartToy` | **视觉权重均质化原则**：同层级列表图标必须保持相同的视觉形态（一律实心 Filled），严禁实心与空心线框混用。 |
| **教条式一刀切滥用 42dp**<br>AI 误将“设置主页的 42dp”当成全局写死参数，强行把弹窗、紧凑卡片、标签等原本 36dp/30dp/24dp 的微型容器撑大为 42dp，破坏原有紧凑排版与信息密度。 | **尊重组件场景层级，死守 2:3 相对比例**：区分标准级 (42dp/28dp)、紧凑级 (36dp/24dp)、微型级 (30dp/20dp)、极小级 (24dp/16dp)。严禁盲目放大容器，统一按 `图标 = 容器 * (2/3)` 阶梯适配。 | `docs/design/设置项与图标双轨系统设计规范.md` | **场景层级尊重公理与相对比例原则**：42dp 仅为大列表项基准，严禁跨场景一刀切；各层级容器尺寸恪守既有密度，仅按 2:3 计算图标。 |
| **误将 28/42 或 2:3 套用到页面空状态等大容器**<br>AI 误读规范，将 28/42dp 当成全应用唯一标准，盲目将页面级空状态（如 `100dp × 100dp` 容器配 56dp 图标等情感化占位卡片）擅自缩小为 42dp 或 72dp，破坏页面既有设计层级与情感化表达。 | **设立绝对红线豁免区，既定大尺寸神圣不可侵犯**：页面级空状态占位大视图（如 100×100）、全屏插图容器与特定业务看板完全豁免；凡未被用户明确指令要求调整的既有组件，严禁 AI 擅自改动尺寸！ | `LeechManagementScreen.kt`<br>`WordListScreen.kt`<br>`.agent/rules/nemorules.md` | **场景边界公理与豁免区不可侵犯原则**：28/42 仅限特定设置项与列表项，绝非全项目万能模板；已有大尺寸空状态容器一律保持原样，严禁篡改！ |

---

## 落地与复核标准 (Checklist)

1. [x] **图标来源检查**：全页无任何第三方临时 svg/xml，全部采用 `Icons.Rounded.*`（特定白名单除外）。
2. [x] **尺寸与比例**：容器为 42dp，图标为 28dp（精确 2:3），留白为 7dp。
3. [x] **曲率连续性**：底块统一调用 `RoundedRectangle(12.dp)`，严禁 `RoundedCornerShape`。
4. [x] **单容器架构**：`SquircleSettingItem` 内部只有一个 `Box` 与一个 `Icon`，无冗余 `if-else` 分支。
5. [x] **色彩映射一致**：柔和微透底为 `15% iconColor`，图标为 `iconColor`；活力纯色底为 `100% iconColor`，图标为 `White`。
