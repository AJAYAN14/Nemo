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



---

## 五、测试设置界面液态气泡菜单 Cresto 级玻璃着色器与高光升级台账

> **目标**：100% 还原 Cresto 原生 `glasense-ui` 的高级亚克力液态玻璃质感，将基础磨砂卡片彻底升级为基于 AGSL 贝塞尔明度映射、50dp 极柔模糊与垂直物理渐变高光的真实玻璃容器。

### 1. 改动与新增组件
* **新增组件**：`GlasenseMaterialShader.kt`
  * 移植 Cresto 核心 AGSL RuntimeShader 材质配方：
    * 三次贝塞尔曲线亮度重映射（`bezierMap`）+ 饱和度提升（1.5f）+ 微观抖动防断层噪点（Dither）；
    * 封装 `RegularLight`（浅色模式）与 `RegularDark`（深色模式）专属菜单材质配方；
    * 提供 `rememberGlasenseMaterialRenderEffectOrNull`，在 Android 13+（API 33+）上动态创建 RuntimeShader，低版本优雅降级。
* **升级渲染管线**：`NemoPopupMenu.kt`
  * **超大模糊与扩展采样**：Backdrop 效果升级为 `padding = 50.dp.toPx() * 2`（外扩 100dp 采样消除硬边与边缘黑斑）+ `blur(50.dp.toPx())` 深度高斯模糊 + `materialEffect` 贝塞尔材质着色注入；
  * **双阶物理边缘高光（Glass Highlight）**：边框升级为垂直多阶微光渐变描边（`Brush.verticalGradient`，顶部纯白 22%~55% 入射高光，中部 8%~20%，底部 2%~6% 隐去），勾勒真实玻璃物体的厚度与晶莹反光；
  * **阴影与微染色**：漫反射软阴影半径提升至 32dp（偏移 14dp），表面叠加 25%~32% 微染色保持文字选项可读性；
  * **编译验证**：全工程 `compileDebugKotlin` 验证通过（BUILD SUCCESSFUL，0 错误）。

---

## 六、主设置界面（SettingsScreen）学习目标项液态气泡菜单与微透分割线升级台账

> **目标**：响应用户指令，将主设置界面（`SettingsScreen.kt`）的“每日单词目标”、“每日语法目标”、“学习日重置时间”从原有的 BottomSheet 升级为与 Cresto 一致的真·液态磨砂气泡悬浮菜单（`NemoPopupMenu`），并且在菜单项与项之间加入优雅的 0.5dp 微透分割线。

### 1. 核心改造与组件架构
* **升级基础列表项组件**：`SettingsComponents.kt`
  * 为 `SquircleSettingItem` 两个公开重载及底层实现增加 `onClickWithBounds: ((Rect) -> Unit)? = null` 可选回调；
  * 列表项底层容器接入 `onGloballyPositioned`，实时捕获整行在窗口中的绝对矩形 `boundsInWindow()`，点击时精准回传自身屏幕位置；
  * 保持已有 `onClick: () -> Unit = {}` 参数默认值不变，100% 向后兼容全工程其他调用。
* **沉淀菜单项间微透分割线**：`NemoPopupMenu.kt`
  * 在 `state.items.forEachIndexed` 遍历中，在相邻选项之间自动注入 `0.5.dp` 的极细微透水平分割线（`HorizontalDivider`）；
  * 深浅色自适应透明度：深色模式 8% 白微光，浅色模式 6% 黑轻影，两侧带 14dp 内缩边距，与分割线对象（`NemoPopupMenuDivider`）实现智能防重互斥，极大提升层级节奏感与视觉秩序。
* **主设置界面接入与升级**：`SettingsScreen.kt`
  * 实例化 `val backdrop = rememberLayerBackdrop()`，将外层 `LazyColumn` 挂载为物理折射采样源 `.layerBackdrop(backdrop)`；
  * 移除“每日单词目标”、“每日语法目标”、“学习日重置时间”原有的 3 个 BottomSheet 对话框代码；
  * 将这三项的点击事件接入 `onClickWithBounds`，分别弹出对应选项（单词目标：5/10/20/30/50个；语法目标：5/10/15/20/25条；重置时间：0点/2点/4点(推荐)/5点/6点）；
  * 选项文本严格复用原有 `strings.xml` 资源，零硬编码；
  * 在页面根容器顶层统一挂载 `NemoPopupMenu`，享受就地停靠、贝塞尔曲线明度重映射、50dp 极柔模糊折射与微触觉反馈。

### 2. 编译与规范核验
* **编译检查**：执行 `.\gradlew.bat compileDebugKotlin --quiet` 验证通过（BUILD SUCCESSFUL，0 错误）。
* **规范符合度**：
  * 严禁硬编码颜色字面量与 UI 界面文本：颜色全走系统语义及调色盘，文本 100% 收编至 `strings.xml`；
  * 严格遵循最小变更原则，不改动无关代码；
  * 注释纯净，杜绝任何元说明与口水套话。

---

## 七、液态气泡菜单超透亮水晶光学材质与边缘高光调优台账

> **目标**：响应用户指令（“优化一下，改成更透亮一点的”），彻底排查导致此前视觉上略显厚重、发白雾化的根因，对标 Cresto 原生清透玻璃质感，将气泡菜单全面调优为超透亮高采光水晶玻璃容器。

### 1. 核心调优与配方升级
* **去除厚重蒙层，还原 100% 穿透折射**：
  * 将原先覆盖在物理折射层之上的 `32%` 浅色白底与 `25%` 深色黑底（`glassTintOverlay`）大幅削减至仅 `6%` 呼吸感晶体透光层，消除类似磨砂塑料的泛白硬质感，让底层界面的背景色彩、卡片边缘与光影直接穿透玻璃体；
* **升级 AGSL 材质着色器为清透配方（Thin / Clear Recipe）**：
  * 在 `GlasenseMaterialShader.kt` 中将明度映射强度从偏沉重的 `0.75f` 收敛至轻盈通透的 `0.58f`，采用更平缓自然的 Thin 贝塞尔明度映射曲线；
  * 明度补偿微调为 `extraBrightness = 0.12f`，使折射出的背景像素明亮清晰、晶莹透光；
* **强化晶体边缘垂直高光与清爽阴影**：
  * 顶部物理入射高光提升至 70% 纯白亮光（`Color.White.copy(alpha = 0.70f)`），中部 25%，底部 8%，勾勒出宛如宝石切割面的晶莹反光与立体厚度；
  * 浅色漫反射软阴影从 15% 优化至 8%，整体更轻灵清爽，无灰暗脏污感；
  * 0.5dp 微透分割线调整为 5% 黑轻影（浅色）/ 8% 白微光（深色），与清澈玻璃背景无缝融为一体。

### 2. 编译与验收验证
* **构建验证**：执行 `.\gradlew.bat compileDebugKotlin --quiet` 验证通过（BUILD SUCCESSFUL，0 错误）。

---

## 八、气泡菜单 iOS 原生级双层复合阴影与表面张力非对称弹性动效台账

> **目标**：彻底拉齐与 iOS 官方系统组件最后的 5% 细节差异，将气泡菜单从常规单层阴影与各向同性缩放，全面升级为 iOS 标准的“双层复合物理投影（Ambient + Key）”与“表面张力非对称物理弹性动效（Asymmetric Spring）”。

### 1. 核心升级与工程落地
* **iOS 标准双层复合物理阴影（Dual Composite Shadows）**：
  * **第一层（贴身微环境光阴影 Ambient Shadow）**：8dp 模糊半径 + 2dp Y 轴微偏移（深色 35% 黑，浅色 4% 黑），精细锁紧卡片边缘立体轮廓，解决超大单一投影造成的边缘发虚问题；
  * **第二层（深远扩散主漫反射投影 Key Shadow）**：26dp 模糊半径 + 12dp Y 轴主偏移（深色 45% 黑，浅色 8% 黑），在三维空间中将悬浮气泡自然托起；
  * 在 `drawBehind` 的同一离屏 Canvas 树中分阶段绘制并融合，兼具轮廓精密感与空间深邃感。
* **表面张力非对称物理弹性形变动效（Asymmetric Spring Animation）**：
  * 将缩放动画拆解为 `scaleXAni`（X 轴初态 0.65f，`dampingRatio = 0.82f`，`stiffness = 420f`）与 `scaleYAni`（Y 轴初态 0.45f，`dampingRatio = 0.76f`，`stiffness = 400f`）；
  * 展开瞬间呈现极其细腻的表面张力形变（横向先伸展、纵向弹性回弹泵出），模拟物理液体气泡脱离锚点瞬间的有机生命感。

### 2. 构建与规范核验
* **构建验证**：执行 `.\gradlew.bat compileDebugKotlin --quiet` 验证通过（BUILD SUCCESSFUL，0 错误）。

---

## 九、气泡菜单 22dp 黄金光斑与 100% 零遮挡纯净折射调优台账

> **目标**：彻底解决此前因模糊半径过大（50dp）导致光影被抹成死板单色、以及因贝塞尔映射压暗导致的“发闷不够透亮”问题，全面调优为 22dp 黄金光斑清晰穿透与零背景遮挡的水晶玻璃。

### 1. 核心重构与调优落地
* **模糊半径回归 iOS 黄金光斑档位（`22dp`）**：
  * 将模糊半径从抹平光影的 `50dp` 调整为黄金光斑档位的 `22dp`（外扩采样 `44dp`）；
  * 恢复了底层卡片边缘与图文的光晕轮廓，背后的光线与明暗起伏能够自然斑斓地透射进玻璃体中，透亮感大增。
* **彻底拔除 AGSL 渲染层的实色背景遮盖**：
  * 在 `drawBackdrop` 渲染层中移除 `.background(glassTintOverlay)`，100% 开放底层 GPU 帧缓冲的纯粹折射光。
* **升级 AGSL 贝塞尔曲线为高采光晶体配方**：
  * 引入 `CrystalLightCurve = (0.92f, 0.95f, 0.98f, 0.96f)`，将亮度映射最高点拉升至 0.98，彻底解决纯白被压暗的问题；
  * `mapIntensity` 收敛至 `0.50f`（一半底色采光 + 一半贝塞尔提亮），`saturation` 提升至 `1.45x`，`extraBrightness` 提升至 `0.16f`；
  * 确保 RenderEffect 顺序先 `blur(22.dp)` 再注入 `materialEffect` 提纯，保证色彩鲜活灵动。

### 2. 构建与验证
* **构建验证**：执行 `.\gradlew.bat compileDebugKotlin --quiet` 验证通过（BUILD SUCCESSFUL，0 错误）。

---

## 十、气泡菜单移植 AndroidLiquidGlass 玻璃游乐场矩形管线台账

> **目标**：响应用户明确指定，将气泡菜单彻底换装为 `E:\Android\AndroidLiquidGlass` 项目中“玻璃游乐场（Glass Playground）”悬浮矩形同款渲染管线，并将模糊半径严格设为滑块中值 16dp。

### 1. 核心管线重构与落地
* **`AndroidLiquidGlass` 玻璃游乐场矩形同款渲染链**：
  * **色彩提纯（`vibrancy()`）**：接入 Kyant 官方纯净色彩饱和度与明度提纯，彻底消除底层像素被高斯模糊后发脏变暗的弊端；
  * **模糊半径精准拉到一半（`16.dp`）**：严格对应游乐场滑块（0~32dp）的一半中值 `blur(16.dp.toPx())`，背景物体既拥有柔美光斑，又绝不抹平成单色死板；
  * **边缘物理透镜折射弯曲（`lens(...)`）**：注入真实凸透镜光学形变（`refractionHeight = 0.2f * minDimension * 0.5f`，`refractionAmount = 0.2f * minDimension`，`depthEffect = true`），底层元素在穿透气泡边缘时会像透过真实液态水滴一样产生物理折射形变；
  * **内置晶莹高光（`Highlight.Plain`）**：完美接入游乐场矩形原汁原味的宝石切面微反光；
  * **100% 零实色遮挡**：完全无背景底色干扰，水光透亮感拉满。
* **双层阴影与非对称动效完美融合**：
  * 继续保留上一阶段调优完成的 iOS 标准双层复合物理阴影（Ambient 8dp + Key 26dp）与表面张力非对称弹性形变动效，整套气泡菜单呈现顶级的物理水感。

### 2. 构建与核验
* **构建验证**：执行 `.\gradlew.bat compileDebugKotlin --quiet` 验证通过（BUILD SUCCESSFUL，0 错误）。


---

## 十一、学习界面操作菜单液态玻璃真透镜折射与同级兄弟采样改造台账

> **目标**：对齐 `E:\Android\testapp2` 标杆实现，将学习界面（`LearningScreen.kt`）顶部的更多操作菜单（`LearnHeaderMoreMenu.kt`）彻底换装为同款液态玻璃物理折射架构。取消全屏窗口模糊与暗色蒙层，保持底层 100% 原始清晰，仅菜单面板本身具备高阶液态透镜折射与局部磨砂，菜单内部功能项完全忠实保留原样。

### 1. 核心架构与技术规范落地
* **同级兄弟采样架构（Sibling Architecture）**：
  * 将学习界面的实色背景、顶部氛围光与页面主体内容整体装入 `Box.fillMaxSize().layerBackdrop(learningBackdrop)`，确保整屏 1080x2400 像素全量实心录制，彻底根除透明空洞导致的模糊卷积边缘发黑撕裂；
  * 菜单面板作为同级顶层兄弟节点挂载，直接采样 `learningBackdrop`，杜绝子树递归采样与 RenderNode 循环崩溃风险。
* **取消全屏背景模糊与暗色压暗**：
  * 菜单展开时，整个底层窗口与卡片不施加任何全屏高斯模糊与黑色压暗蒙层，全屏仅铺设一层纯透明点击拦截层，点击空白区域即平滑关闭；
  * 底层卡片内容 100% 原始清晰暴露于菜单透镜之下，形成鲜明的通透物理折射对照。
* **`testapp2` 标杆液态玻璃材质落地**：
  * **22.dp G2 连续平滑曲率**：四周采用 `RoundedRectangle(22.dp)` 高阶连续圆角；
  * **12.dp 黄金甜点位模糊**：`blur(12f.dp.toPx())` 既抹平底层文字突兀感，又完整保留水滴折射弧光；
  * **响应式尺寸等比透镜**：`lens(refractionHeight = 0.10f * minDimension, refractionAmount = 0.20f * minDimension, depthEffect = true, chromaticAberration = false)`，厚度与强度跟随短边自适应，保障文字清晰可读；
  * **光学色彩增艳提亮**：`colorControls(brightness = 0.05f/0.10f, saturation = 1.35f)` 替代死白漆涂层；
  * **物理高光与微光描边**：`Highlight.Plain` 配合 0.6dp 垂直渐变高光描边，微透白雾降至 4%~6%，彻底拔除原生 elevation 黑色脏影；
  * **右上角非对称流体弹性形变动效**：变换原点设在右上角触发按钮处（`TransformOrigin(0.88f, 0.05f)`），配合 Spring 弹簧曲线实现流畅的流体膨胀与收缩。
* **菜单内部业务原样无损保持**：
  * 打分指引、搁置、斩除、报错、自动朗读开关、延迟显示设置、自动翻卡设置、草稿画板、深浅色模式切换等 9 项功能文字、图标（Google Fonts 原生实心图标）、调色盘（`IosColors`）与 `LiquidToggle` 开关 100% 保持原状。

### 2. 涉及文件
* `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/common/LearnHeaderMoreMenu.kt`
* `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/LearningScreen.kt`
* `implementation-notes.md`


---

## 十三、全应用右上角菜单全面统一换装液态玻璃与圆润三横/✕形变动效台账

> **目标**：响应用户指令（“帮我把其他的界面的右上角 menu 也改成一样的效果 /goal 改到一个不剩为止”），在核心通用组件库与各业务模块中进行彻底排查与重构，将全项目所有右上角菜单统一升级为同款高阶液态玻璃材质与圆润三横/✕几何弹性形变动效，实现 0 遗漏覆盖。

### 1. 核心架构与落地范围
* **通用形变菜单底座升级（`core/ui/component/common/NemoMorphMenu.kt`）**：
  * **触发按钮升级**：内置 `AnimatedMenuCloseIcon`，默认呈现圆润饱满的三条水平圆角横线（`StrokeCap.Round`，2.4dp 粗细），展开时中线收缩隐去、上下横线平滑靠拢并分别旋转 45° 与 -45° 咬合成圆润的 ✕，关闭时弹性舒展复原，支持再次点击平滑收起；
  * **材质升级**：面板卡片全面换装 `RoundedRectangle(22.dp)` 高阶连续圆角、0.6dp 垂直微光渐变高光描边（`borderBrush`）；接入 `drawBackdrop` 12dp 黄金模糊、动态比例凸透镜物理折射（`lens`）与着色器光学提亮增艳（`colorControls`），微透白雾降至 4%~6% 无原生 elevation 脏影；
  * **高度包裹不滚动**：移除固定高度与滚动条限制，面板依据菜单项自然包裹，紧凑清爽展开。
* **业务模块全面改造覆盖**：
  * **收藏夹与错题本（`feature/collection`）**：
    * `CollectionActionMenu.kt` 自动继承升级后的通用底座，在 `FavoritesScreen` 与 `MistakesScreen` 顶部栏中完美呈现圆润三横/✕动效与液态折射材质，保留全屏纯透明手势拦截。
  * **词库与语法库列表（`feature/library`）**：
    * `LibraryListComponents.kt`（`LibraryLevelSelectionMenu`）：右上角等级筛选入口移除旧版图标写死限制，无缝接入圆润三横/✕形变动效与同款液态玻璃折射面板，在 `WordListScreen` 与 `GrammarListScreen` 中同步生效。
  * **测试与答题模块（`feature/test`）**：
    * `TestHeaderComponents.kt`：所有答题测试界面顶栏（`TestHeaderActions`）自动继承升级；
    * `TestHeader.kt`：彻底重构淘汰遗留的原生 `IconButton` + `NemoDropdownMenu`，全面接入统一的 `NemoMorphMenu`。

### 2. 涉及文件列表
* `core/ui/src/main/java/com/jian/nemo/core/ui/component/common/NemoMorphMenu.kt`
* `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/components/LibraryListComponents.kt`
* `feature/test/src/main/java/com/jian/nemo/feature/test/components/TestHeader.kt`
* `implementation-notes.md`

---

## 十四、Popup 浮层垂直锚点与 ✕ 按钮穿模遮挡修复台账

> **目标**：修复错题本等界面中，右上角圆形 ✕ 按钮与弹出菜单面板右上角发生重叠穿模、导致第一行文字被遮挡的问题。

### 1. 根因分析
* 在 `core/ui/component/common/NemoMorphMenu.kt` 中，`Popup` 容器挂在大小为 `buttonSize`（48.dp）的根 Box 内。
* 原先 `Popup` 的属性为 `alignment = Alignment.TopEnd` 且 `offset = IntOffset(0, 0)`。
* 这导致 Popup 面板的顶部 Y 坐标与 48dp 按钮的顶部 Y 坐标完全对齐，面板卡片直接覆盖在按钮身下，使得圆形的 ✕ 按钮直接印在面板右上角，遮挡了第一行选项（“清除所有错题”）。

### 2. 解决方案
* 在 `NemoMorphMenu.kt` 中计算动态垂直下移像素：
  `val yOffsetPx = with(LocalDensity.current) { (buttonSize + 8.dp).roundToPx() }`
* 将 `Popup` 的偏移量设置为 `offset = IntOffset(0, yOffsetPx)`。
* 确保弹出面板严格在圆形触发按钮下方 8dp 处自然展开，两者上下错落分离，彻底消除遮挡穿模。


### 3. 核心踩坑与稳定性防护（RenderNode 递归死锁治理）
* **陷阱**：在底层录制节点 `Box.layerBackdrop(learningBackdrop)` 的子树内部，如果子组件按钮（如 `LearnHeaderMoreMenuButton`）也传入了 `backdrop = learningBackdrop`，按钮内部的 `drawBackdrop` 会在绘制管线中递归请求父级节点的 DisplayList，导致 Skia `RenderNode::prepareTreeImpl` 递归 512 层引发 Stack Overflow（SIGSEGV 11 崩溃）。
* **对策**：严格遵守同级兄弟采样准则——处于录制画布内部的常规按钮保持 `backdrop = null`，作为标准磨砂按钮工作；只有位于画布外部、同级浮层位置的 `LearnHeaderMoreMenuOverlay` 才能接收并采样 `learningBackdrop`，从而实现 100% 稳定的真透镜折射效果。


---

## 十二、气泡菜单自适应高度与圆润三横/✕连续形变动效改造台账

> **目标**：响应用户指令，取消气泡菜单的滚动条与最大高度限制，恢复为紧凑自适应高度；将触发按钮图标升级为“圆润三横”，并在展开与关闭时实现平滑的几何弹性形变（三横 <-> ✕）。

### 1. 核心技术落地
* **取消菜单内部滚动限制**：
  * 移除 `LearnHeaderMoreMenuOverlay` 内写死的 `heightIn(max = 520.dp)` 限制；
  * 移除 `Column` 上的 `.verticalScroll(rememberScrollState())` 滚动修饰符；
  * 面板高度自然包裹全部 9~10 个菜单项，保持紧凑排版，一屏完整展现，彻底告别局部滚动。
* **圆润三横与 ✕ 连续平滑形变动效（`AnimatedMenuCloseIcon`）**：
  * 采用纯 Compose Canvas 几何线条插值驱动（`StrokeCap.Round` 圆角端点，线条粗细 2.4dp）；
  * 展开动效：中线向中心收缩淡出，上下两条横线顺畅向中心聚拢并分别旋转 45° 与 -45°，平滑咬合交织为圆润的“✕”；
  * 收起动效：两根对角线旋转舒展回水平位置，中间横线由中心向外平滑展开，弹性复原为饱满的三横汉堡图标；
  * 动效使用 Spring 弹簧曲线驱动，状态由 `isExpanded` 双向同步。

### 2. 涉及文件
* `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/common/LearnHeaderMoreMenu.kt`
* `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/LearningScreen.kt`
* `implementation-notes.md`


---

## 十三、全应用右上角菜单全面统一换装液态玻璃与圆润三横/✕形变动效台账

> **目标**：响应用户指令（“帮我把其他的界面的右上角 menu 也改成一样的效果 /goal 改到一个不剩为止”），在核心通用组件库与各业务模块中进行彻底排查与重构，将全项目所有右上角菜单统一升级为同款高阶液态玻璃材质与圆润三横/✕几何弹性形变动效，实现 0 遗漏覆盖。

### 1. 核心架构与落地范围
* **通用形变菜单底座升级（`core/ui/component/common/NemoMorphMenu.kt`）**：
  * **触发按钮升级**：内置 `AnimatedMenuCloseIcon`，默认呈现圆润饱满的三条水平圆角横线（`StrokeCap.Round`，2.4dp 粗细），展开时中线收缩隐去、上下横线平滑靠拢并分别旋转 45° 与 -45° 咬合成圆润的 ✕，关闭时弹性舒展复原，支持再次点击平滑收起；
  * **材质升级**：面板卡片全面换装 `RoundedRectangle(22.dp)` 高阶连续圆角、0.6dp 垂直微光渐变高光描边（`borderBrush`）；接入 `drawBackdrop` 12dp 黄金模糊、动态比例凸透镜物理折射（`lens`）与着色器光学提亮增艳（`colorControls`），微透白雾降至 4%~6% 无原生 elevation 脏影；
  * **高度包裹不滚动**：移除固定高度与滚动条限制，面板依据菜单项自然包裹，紧凑清爽展开。
* **业务模块全面改造覆盖**：
  * **收藏夹与错题本（`feature/collection`）**：
    * `CollectionActionMenu.kt` 自动继承升级后的通用底座，在 `FavoritesScreen` 与 `MistakesScreen` 顶部栏中完美呈现圆润三横/✕动效与液态折射材质，保留全屏纯透明手势拦截。
  * **词库与语法库列表（`feature/library`）**：
    * `LibraryListComponents.kt`（`LibraryLevelSelectionMenu`）：右上角等级筛选入口移除旧版图标写死限制，无缝接入圆润三横/✕形变动效与同款液态玻璃折射面板，在 `WordListScreen` 与 `GrammarListScreen` 中同步生效。
  * **测试与答题模块（`feature/test`）**：
    * `TestHeaderComponents.kt`：所有答题测试界面顶栏（`TestHeaderActions`）自动继承升级；
    * `TestHeader.kt`：彻底重构淘汰遗留的原生 `IconButton` + `NemoDropdownMenu`，全面接入统一的 `NemoMorphMenu`。

### 2. 涉及文件列表
* `core/ui/src/main/java/com/jian/nemo/core/ui/component/common/NemoMorphMenu.kt`
* `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/components/LibraryListComponents.kt`
* `feature/test/src/main/java/com/jian/nemo/feature/test/components/TestHeader.kt`
* `implementation-notes.md`

---

## 十四、Popup 浮层垂直锚点与 ✕ 按钮穿模遮挡修复台账

> **目标**：修复错题本等界面中，右上角圆形 ✕ 按钮与弹出菜单面板右上角发生重叠穿模、导致第一行文字被遮挡的问题。

### 1. 根因分析
* 在 `core/ui/component/common/NemoMorphMenu.kt` 中，`Popup` 容器挂在大小为 `buttonSize`（48.dp）的根 Box 内。
* 原先 `Popup` 的属性为 `alignment = Alignment.TopEnd` 且 `offset = IntOffset(0, 0)`。
* 这导致 Popup 面板的顶部 Y 坐标与 48dp 按钮的顶部 Y 坐标完全对齐，面板卡片直接覆盖在按钮身下，使得圆形的 ✕ 按钮直接印在面板右上角，遮挡了第一行选项（“清除所有错题”）。

### 2. 解决方案
* 在 `NemoMorphMenu.kt` 中计算动态垂直下移像素：
  `val yOffsetPx = with(LocalDensity.current) { (buttonSize + 8.dp).roundToPx() }`
* 将 `Popup` 的偏移量设置为 `offset = IntOffset(0, yOffsetPx)`。
* 确保弹出面板严格在圆形触发按钮下方 8dp 处自然展开，两者上下错落分离，彻底消除遮挡穿模。



---

## 十五、我的错题与我的收藏 Menu 全面对齐学习界面实现台账

> **目标**：以学习界面的 Menu（`LearnHeaderMoreMenu.kt`）为黄金基准，在绝对不改动学习界面代码的前提下，彻底将“我的错题”和“我的收藏”界面的 Menu 细节（触感、内缩胶囊按压、排版字号、自适应微光分割线）1:1 完全对齐。

### 1. 核心改进点与对齐细节
1. **触感反馈（Haptics）100% 对齐**：
   * 在 `NemoLiquidMenuItem` 的点击事件中补齐 `haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)`，使错题本与收藏夹的每一个菜单条目均具备与学习界面完全一致的清脆震动反馈。
2. **按压高亮动效与圆角边界（Surface 内缩胶囊）100% 对齐**：
   * 彻底摒弃贴边 `Row` + `clip` 的粗糙高亮实现；
   - 换装为学习界面同款架构：外层增加 `padding(horizontal = 6.dp, vertical = 2.dp)` 保护边距，采用 `Surface(shape = RoundedRectangle(12.dp))` 并在按压时动态显隐 `onSurface.copy(alpha = 0.08f)`，内部再设置 `padding(horizontal = 12.dp, vertical = 10.dp)`；
   - 呈现出两边不贴边框、精致悬浮的岛式内缩胶囊按压质感。
3. **字体排版与规格 100% 对齐**：
   * 统一为 `MaterialTheme.typography.titleMedium`，并显式指定 `fontSize = 15.sp`、`FontWeight.Medium`。
4. **自适应微光分割线 100% 对齐**：
   * 在 `NemoLiquidMenu.kt` 中封装公共的 `NemoLiquidMenuDivider`；
   * 采用深浅色自适应半透明黑白微光算法（深色 `White 0.12f`，浅色 `Black 0.08f`），粗细严格统一为 `0.5.dp`，外边距统一为 `padding(horizontal = 12.dp, vertical = 2.dp)`。
5. **面板容器尺寸与纵向边距 100% 对齐**：
   * `NemoLiquidMenuOverlay` 面板宽度对齐为 `min = 230.dp, max = 260.dp`，内部列表容器垂直边距对齐为 `padding(vertical = 6.dp)`。

### 2. 涉及文件列表
* `core/ui/src/main/java/com/jian/nemo/core/ui/component/common/NemoLiquidMenu.kt`
* `feature/collection/src/main/java/com/jian/nemo/feature/collection/components/CollectionActionMenu.kt`
* `implementation-notes.md`

---

## 十六、全项目通用液态悬浮菜单底座沉淀与错题/收藏对齐台账

> **目标**：以学习界面 Menu（`LearnHeaderMoreMenu.kt`）为全项目黄金标准，将其高阶液态透镜折射骨架无损下沉至 `:core:ui` 建立全项目通用底座（`NemoLiquidMenu.kt`），保持错题与收藏业务逻辑 100% 完整，消除旧版弹窗 UI 并彻底对齐学习界面。

### 1. 核心技术点与对齐细节
1. **通用底座标准补齐**：
   * 在 `NemoLiquidMenu.kt` 中补齐 `NemoLiquidMenuToggleItem`（集成 `LiquidToggle`），为全项目后续界面的菜单开关提供统一底座；
   * 在 `NemoLiquidMenuButton` 中对齐 `chromaticAberration = false`，确保触发按钮在各级背景下保持清晰纯净；
   * 固化 `AnimatedMenuCloseIcon`（圆润三横 <-> ✕）平滑弹性形变。
2. **Backdrop 采样范式 100% 对齐（消除递归崩溃隐患）**：
   * 学习界面的核心稳定准则是：处于 `layerBackdrop` 画布子树内部的触发按钮传入 `backdrop = null`，仅同级浮层传入 `backdrop` 采样；
   * 修正 `FavoritesScreen.kt` 与 `MistakesScreen.kt` 中 `CollectionActionButton` 的参数，将 `backdrop = backdrop` 纠正为 `backdrop = null`，浮层 `CollectionActionOverlay` 接收 `backdrop` 实施真透镜折射。
3. **业务逻辑 100% 纯净保留**：
   * 错题和收藏的“清除所有”、“清除单词”、“清除语法”以及三个二次确认弹窗（`NemoDialog`）逻辑原封不动，仅将原本的旧版弹出式 UI 彻底替换为学习界面同款的高阶液态玻璃外壳。

### 2. 涉及文件列表
* `core/ui/src/main/java/com/jian/nemo/core/ui/component/common/NemoLiquidMenu.kt`
* `feature/collection/src/main/java/com/jian/nemo/feature/collection/components/CollectionActionMenu.kt`
* `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoritesScreen.kt`
* `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/MistakesScreen.kt`
* `implementation-notes.md`

---

## 十七、错题与收藏 Menu 底层画布实底录制与光学折射参数对齐台账

> **目标**：以学习界面（`LearningScreen.kt` 与 `LearnHeaderMoreMenu.kt`）的光学折射渲染管线为基准，将“我的错题”与“我的收藏”界面的 Menu 折射底图采样完整度（消除透明空洞）与折射物理参数扩展性 1:1 彻底对齐。

### 1. 核心改进点与对齐细节
1. **底层录制画布实底对齐（消除透明空洞）**：
   * 在 `FavoritesScreen.kt` 与 `MistakesScreen.kt` 的 `layerBackdrop(backdrop)` 画布内部最底层，新增铺设全屏实底 `Box.fillMaxSize().background(backgroundColor)`；
   * 彻底解决原先实色背景位于画布外层、导致无卡片空白区域录入 Alpha=0 透明像素的问题，使透镜 Shader 折射出的微透光斑与学习界面一致纯净、饱满。
2. **折射物理参数暴露与透传对齐**：
   * 在 `CollectionActionOverlay` 中补齐 `blurRadius`（默认 12.dp）、`refractionHeightRatio`（默认 0.10f）、`refractionAmountRatio`（默认 0.20f）参数定义，并完整透传给底座 `NemoLiquidMenuOverlay`；
   * 保持错题/收藏业务逻辑与二次确认弹窗 100% 原始不变。

### 2. 涉及文件列表
* `feature/collection/src/main/java/com/jian/nemo/feature/collection/components/CollectionActionMenu.kt`
* `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoritesScreen.kt`
* `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/MistakesScreen.kt`
* `implementation-notes.md`
