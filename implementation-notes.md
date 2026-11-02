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
