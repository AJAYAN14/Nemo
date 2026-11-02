# 全工程子界面顶部四阶垂直渐变遮罩与 48dp 液态玻璃顶栏改造台账

> **基准规范**：严格遵循 `ActivityHeatmapScreen.kt` 与 `docs/experience/04-悬浮液态玻璃顶栏遮罩范式与实验室深浅色排版避坑.md` 的黄金范式。
> **核心四要素**：
> 1. 页面根容器采用 `Box` 并接入 `backgroundColor` / `containerColor`；
> 2. 动态读取安全边距：`statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`，`navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()`；
> 3. 创建物理折射源：`val backdrop = rememberLayerBackdrop()`，底层滚动容器挂载 `.layerBackdrop(backdrop)`，顶部内边距设置 `top = statusBarTop + 60.dp + 16.dp` 使内容自然穿透折射；
> 4. 悬浮固定顶栏：`Brush.verticalGradient(listOf(0.95f, 0.75f, 0.35f, Transparent))` 四阶垂直渐变保护遮罩，内部调用 `CommonHeader(backdrop = backdrop, backgroundColor = Color.Transparent, backButtonSize = 48.dp)`，辅助操作均升级为圆形 48dp `FrostedLiquidGlassButton`。

---

## 一、本次改造与验收清单（共 15 个文件，覆盖 18 个全屏滚动子界面）

| 序号 | 模块 | 文件路径 | 界面类型 | 改造前缺陷 | 改造后状态与验证 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | `:feature:statistics` | `feature/statistics/presentation/dashboard/LeechManagementScreen.kt` | 复学清单 | 实底顶栏硬切内容、无物理折射源 | 48dp液态玻璃顶栏 + 四阶渐变遮罩 + 贯穿折射，编译通过 |
| 2 | `:feature:collection` | `feature/collection/favorites/FavoriteGrammarsScreen.kt` | 收藏语法列表 | 常态与多选模式顶栏均为实底硬切 | 常态/多选顶栏均悬浮化 + 48dp液态玻璃按钮 + 穿透列表，编译通过 |
| 3 | `:feature:collection` | `feature/collection/favorites/FavoriteQuestionsScreen.kt` | 收藏题目列表 | 常态与多选模式顶栏均为实底硬切 | 常态/多选顶栏均悬浮化 + 48dp液态玻璃按钮 + 穿透列表，编译通过 |
| 4 | `:feature:collection` | `feature/collection/mistakes/WrongGrammarsScreen.kt` | 错题语法列表 | 常态与多选模式顶栏均为实底硬切 | 常态/多选顶栏均悬浮化 + 48dp液态玻璃按钮 + 穿透列表，编译通过 |
| 5 | `:feature:library` | `feature/library/presentation/category/CategoryClassificationScreen.kt` | 词库分类大厅 | 实底顶栏硬切网格内容 | 48dp液态玻璃顶栏 + 四阶渐变遮罩 + 贯穿网格，编译通过 |
| 6 | `:feature:library` | `feature/library/presentation/expression/ExpressionScreen.kt` | 地道表达大厅 / 表达浏览器 | 两个界面均使用实底顶栏硬切 | 两个子屏均实现四阶渐变遮罩与 48dp 液态玻璃顶栏，编译通过 |
| 7 | `:feature:user` | `feature/user/AccountManagementScreen.kt` | 账号管理 | backdrop 挂载在空 Box 上无真实折射 | 挂载到实际滚动 Column + 四阶渐变悬浮遮罩，编译通过 |
| 8 | `:feature:test` | `feature/test/presentation/ability/ListeningComprehensionScreen.kt` | 听力专项主屏 / 历史记录 | 实底 Scaffold + 普通 IconButton | 主屏与历史均完成四阶渐变遮罩 + 48dp液态玻璃历史按钮，编译通过 |
| 9 | `:feature:test` | `feature/test/presentation/ability/VerbConjugationScreen.kt` | 动词变形主屏 / 历史记录 | 实底 Scaffold + 普通 IconButton | 主屏与历史均完成四阶渐变遮罩 + 48dp液态玻璃历史按钮，编译通过 |
| 10 | `:feature:test` | `feature/test/presentation/ability/WordClozeScreen.kt` | 完形填空主屏 / 历史记录 | 实底 Scaffold + 普通 IconButton | 主屏与历史均完成四阶渐变遮罩 + 48dp液态玻璃操作组，编译通过 |
| 11 | `:feature:learning` | `feature/learning/presentation/ai/AIExerciseDetailScreen.kt` | AI单题评测详情 | 实底 Column 堆叠 + 硬切内容 | 48dp液态玻璃顶栏 + 四阶渐变遮罩 + 贯穿滚动，编译通过 |
| 12 | `:feature:learning` | `feature/learning/presentation/components/guide/RatingGuideScreen.kt` | SRS评分指南 | 实底 Surface 堆叠 + 硬切内容 | 48dp液态玻璃顶栏 + 四阶渐变遮罩 + 贯穿滚动，编译通过 |
| 13 | `:feature:statistics` | `feature/statistics/components/GenericStatisticsScreen.kt` | 通用统计下钻列表 | Scaffold 实底包裹 + 缺少遮罩 | 48dp CommonHeader + 顶部四阶渐变遮罩 + LayerBackdrop 下钻穿透，编译通过 |
| 14 | `:feature:library` | `feature/library/presentation/category/PosWordsScreen.kt` | 词性单词列表 | 遗留旧 TopAppBar 实底容器 | 升级为 48dp CommonHeader + 四阶渐变遮罩 + 穿透列表，编译通过 |
| 15 | `:feature:library` | `feature/library/presentation/partofspeech/PartOfSpeechScreen.kt` | 词性选择大厅 | 遗留旧 TopAppBar 实底容器 | 升级为 48dp CommonHeader + 四阶渐变遮罩 + 穿透网格，编译通过 |

---

## 二、关键技术发现与边界处理

1. **多选顶栏适配**：收藏与错题界面均具备多选模式（`isSelectionMode`），多选顶栏同样置于四阶垂直渐变遮罩容器中，返回与批量删除按钮均统一采用 `FrostedLiquidGlassButton(shape = CircleShape, modifier = Modifier.size(48.dp))`。
2. **Backdrop 采样源正确挂载**：Backdrop 必须绑定到承载实际内容渲染的滚动容器（如 `Column`、`LazyColumn` 或 `LazyVerticalGrid`）上，保证顶栏液态玻璃按钮在各种内容滚动经过时产生真实自然的物理毛玻璃折射。
3. **内容安全间距统一避让**：顶部滚动内边距统一采用 `statusBarTop + 60.dp + 16.dp`，底部统一补充 `navBarBottom + 24.dp`，彻底杜绝内容被顶栏遮挡或被系统手势导航条遮蔽的问题。
4. **编译与类型约束**：`rememberLayerBackdrop()` 返回类型为 `com.kyant.backdrop.backdrops.LayerBackdrop`，在子组件之间传参时必须严格使用该类型，避免使用内部私有接口 `Backdrop`。


---

## 三、测试设置界面基础设置项液态气泡悬浮菜单改造台账

> **背景**：参考 Cresto 项目的 `GlasenseMenu` 液态气泡悬浮菜单架构，将测试设置页（`TestSettingsScreen.kt`）中基础设置各项的底部抽屉（BottomSheet）改造成就地弹出的高质感液态磨砂气泡菜单。

### 1. 改动与新增组件
* **新增组件**：`NemoPopupMenu.kt`
  * 基于 `RoundedRectangle(18.dp)` G2 连续平滑曲率与 `com.kyant.backdrop` 物理透镜折射（20dp 模糊 + vibrancy 活力色彩）；
  * 智能四角锚点停靠算法（`pickPlacement`）：动态感知状态栏、导航栏与输入法 IME 安全边界，优先靠右侧对齐展开；
  * 物理弹簧缩放动画 `spring(0.8f, 450f)` + `HapticFeedbackType.TextHandleMove` 清脆微触觉反馈；
  * 支持单选模式（`dismissOnClick = true`，点击立即生效并关闭）与多选模式（`dismissOnClick = false`，连续勾选不打断，点击外部遮罩统一完成并生效）。
* **升级行容器**：`PremiumSettingsComponents.kt`
  * `PremiumSettingRow` 增加 `onClickWithBounds: (Rect) -> Unit` 重载，通过 `onGloballyPositioned` 捕获点击项在窗口中的绝对位置并传出。
* **升级分区绑定**：`TestSettingsSections.kt`
  * `BasicSettingsSection` 将各项（题目数量、时间限制、题目来源、错题移除、测试内容、单词/语法等级）的点击事件挂载为传递绝对坐标。
* **升级主屏**：`TestSettingsScreen.kt`
  * 题目数量、时间限制支持预设档位气泡单选，末尾包含分割线与“自定义…”动作项无缝衔接已有数字输入 Dialog；
  * 题目来源、错题移除、测试内容改为纯单行轻量文本项，去除冗余副标题；
  * 单词等级与语法等级支持“全部等级”与“N1~N5”的多选勾选切换；
  * 文本完全收编至 `strings.xml`，零硬编码。

### 2. 边界情况与验证
* **屏幕尺寸兼容**：通过 `LocalConfiguration.current` 动态计算屏幕视口尺寸，替代未完全公开的 `LocalWindowInfo.current.containerSize`，保证跨版本安全运行；
* **阴影跨版本渲染**：使用 `android.graphics.Paint` 配合 `BlurMaskFilter` 实现 24dp 漫反射软阴影；
* **编译检查**：全项目 `compileDebugKotlin` 验证零报错通过。

---

## 四、全项目输入框调色盘收敛与 G2 连续平滑曲率圆角改造台账

> **目标**：全面核验排查 Nemo 全项目中所有输入框（TextField、OutlinedTextField、BasicTextField、搜索栏等），审计其色彩是否符合 `docs/` 设计规范调色盘；检查输入框圆角是否为 G2 连续平滑曲率（`RoundedRectangle`），未达标项一律改造升级。

### 1. 改动与收敛清单（共 9 个文件，涉及 13 处输入框）

| 序号 | 模块 | 文件路径 | 输入框组件与用途 | 改造前状态 | 改造后状态与规范对齐 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | `:core:ui` | `core/ui/component/list/GenericExpandableListScreen.kt` | 搜索栏 `OutlinedTextField` | 遗漏 `shape` 参数（降级为系统默认普通圆角） | 补全 `shape = RoundedRectangle(12.dp)`，全项目 G2 圆角率达 100% |
| 2 | `:core:designsystem` | `core/designsystem/component/PremiumStepper.kt` | 数字步进器输入框 `BasicTextField` | 默认 `accentColor` 使用非标硬编码色 `#FF5500` | 默认参数置为 `Color.Unspecified`，深浅模式动态映射 `IosColors.Orange`（浅）/ `IosColors.OrangeDark`（深）光学发光补偿 |
| 3 | `:core:ui` | `core/ui/component/discoverybar/DiscoveryBar.kt` | 顶部发现搜索栏 `BasicTextField` | 默认输入文字与光标硬编码为黑底 `Color.Black` | 动态判断 `isDark` 切换黑白文字与光标，占位符对齐 35% 白半透规范 |
| 4 | `:feature:user` | `feature/user/component/LoginComponents.kt` | 登录账号/密码框 `AppStyleTextField` | 边框与光标使用非标灰色与不统一高亮色 | 聚焦边框与光标收编至 `MaterialTheme.colorScheme.primary`，未聚焦边框收编至 `outline`，底色收编至 `surface` |
| 5 | `:feature:user` | `feature/user/component/RegisterCaptchaDialog.kt` | 注册验证码框 `OutlinedTextField` | 颠倒的明度补偿与非标色 | 边框与光标统一收拢至 `primary` 与 `outline` 语义槽位 |
| 6 | `:feature:learning` | `feature/learning/presentation/components/dialogs/ContentReportDialog.kt` | 内容举报理由输入框 `OutlinedTextField` | 使用非标 Tailwind 颜色（`ReportZinc800/100/700` 等） | 全量迁移至 `MaterialTheme.colorScheme.surfaceContainerHigh/surfaceVariant`、`primary`、`outline`、`onSurface` |
| 7 | `:feature:settings` | `feature/settings/AISettingsScreen.kt` | AI 设置 API Key / URL / 模型框 `AISettingTextField` | 深色模式下占位提示色偏暗沉隐形 | 占位符优化为 `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.6f else 0.7f)` |
| 8 | `:feature:settings` | `feature/settings/components/AIModelStudioComponents.kt` | AI 实验室配置名、API Key、URL、自定义模型框（4 处 `OutlinedTextField`） | 样式分散、占位提示色与文字色未统一 | 统一接入 `StudioThemePalette`（映射 `NemoSurfaceCard`、`NemoSurfaceSubcard`、`NemoSurfaceBorder` 等），圆角全部为 `RoundedRectangle(14.dp/16.dp)` |
| 9 | `:feature:test` | `feature/test/presentation/ability/WordClozeScreen.kt` | 单词填空格子（激活边框与呼吸光标） | 深色模式下未适配橙色发光补偿 | 激活边框与光标在深色模式下统一升级为 `IosColors.OrangeDark`，明度对比度显著提升 |

### 2. 关键设计与架构准则
1. **语义化槽位映射优先公理**：Composable 界面输入框颜色优先消费 `MaterialTheme.colorScheme.*`，零编译依赖障碍，深浅模式由系统自动分发。
2. **G2 平滑连续曲率公理**：全应用输入控件容器与边框 100% 绑定 `RoundedRectangle`，彻底消除普通切角。
3. **深浅光学明度补偿公理**：强调色在深色底色下必须使用系统专属发光色阶（`OrangeDark`），保障无障碍阅读。
4. **编译验证**：全工程 `compileDebugKotlin` 0 警告中断、0 错误，顺利通过构建（BUILD SUCCESSFUL）。

