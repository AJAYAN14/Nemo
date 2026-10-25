# Implementation Notes: 今日统计与历史统计界面 Plump Duo 与等亮度调色板改造

## 任务背景与目标
按照 `docs/design/图标设计与等亮度调色板开发规范指南.md` 规范，对「今日统计」与「历史统计」界面进行改造：
1. 不要接入系统主题色，完整保留当前界面原有的配色作为默认方案（`isPlumpDuo = false` 时 100% 保持原有配色和图标不变）；
2. 增配一套 Plump Duo 方案，通过 `val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO` 进行判断；
3. 当 `isPlumpDuo` 为 true 时，将强调色切换为 `IsoluminantPalette` 对应的等亮度颜色（图标换为 `PlumpDuoIcon`，且取消沉重圆角方块背景）；为 false 时 100% 保持原有配色和图标不变；
4. 箭头、加减号等辅助中性控件保持灰色（`onSurfaceVariant`），禁止滥用彩色。

## 涉及与修改文件
1. [`feature/statistics/.../StatisticsScreen.kt`](file:///C:/Users/AJIAN14/.gemini/antigravity/worktrees/Nemo/add_plump_duo_icon_style/feature/statistics/src/main/java/com/jian/nemo/feature/statistics/StatisticsScreen.kt)
   - 注入 `LocalSettingIconStyle` 贯穿顶层与所有组件。
   - 顶部概览四卡片（`OverallStatisticsDashboard` & `StatCard`）：在 `isPlumpDuo = true` 时，去除图标圆形底块，使用居中透明容器并渲染 `PlumpDuoIcon`（今日新学词与今日新学语法使用 `BadgeCheck`，今日复习词与今日复习语法使用 `GrammarLayers`）；卡片背景使用超浅微光透明层；强调色分别映射为 `SkyIndigo`、`EmeraldGreen`、`WarmAmber`、`LilacViolet`。
   - 空状态组件（`EmptyStatisticsState`）：在 `isPlumpDuo = true` 时取消外层 `Surface` 圆角底块，换用居中透明容器并渲染 48dp `PlumpDuoIcon`（单词为 `Languages`，语法为 `ListChecks`，强调色为 `EmeraldGreen`）。
   - 动态多彩头像（`StatisticsItemRow` & `StatisticsListCard`）：在 `isPlumpDuo = true` 时自适应切换为 8 色等亮度调色板 `PlumpAvatarColors`；来源标签（新学/复习）强调色联动为 `SkyIndigo` / `EmeraldGreen`；JLPT 级别标签保持中性灰。
   - 展开折叠按钮：在 `isPlumpDuo = true` 时，文字使用 `SkyIndigo`，辅助中性指示箭头严格保持中性灰 `MaterialTheme.colorScheme.onSurfaceVariant`。
2. [`feature/statistics/.../StatisticsViewModel.kt`](file:///C:/Users/AJIAN14/.gemini/antigravity/worktrees/Nemo/add_plump_duo_icon_style/feature/statistics/src/main/java/com/jian/nemo/feature/statistics/StatisticsViewModel.kt)
   - 在 `StatisticsUiState` 增加 `settingIconStyle` 字段，并在 `init` 中监听 `settingsRepository.settingIconStyleFlow`，实时同步给 Compose UI。
3. [`feature/statistics/.../HistoricalStatisticsScreen.kt`](file:///C:/Users/AJIAN14/.gemini/antigravity/worktrees/Nemo/add_plump_duo_icon_style/feature/statistics/src/main/java/com/jian/nemo/feature/statistics/HistoricalStatisticsScreen.kt)
   - 注入 `LocalSettingIconStyle` 贯穿界面。
   - 累计学习汇总卡片（`HistoricalSummaryCard` & `StatItem`）：在 `isPlumpDuo = true` 时，单词总数与语法总数大字及背景分别切换为 `IsoluminantPalette.SkyIndigo` 与 `IsoluminantPalette.EmeraldGreen`。
   - 空状态卡片：配置 `PlumpIcon.Languages` 与 `PlumpIcon.ListChecks`，等亮度强调色为 `EmeraldGreen`。
   - 等级分布弹窗触发：传入等亮度主题色 `SkyIndigo` 与 `EmeraldGreen`。
4. [`feature/statistics/.../HistoricalStatisticsViewModel.kt`](file:///C:/Users/AJIAN14/.gemini/antigravity/worktrees/Nemo/add_plump_duo_icon_style/feature/statistics/src/main/java/com/jian/nemo/feature/statistics/HistoricalStatisticsViewModel.kt)
   - 在 `HistoricalStatisticsUiState` 增加 `settingIconStyle` 字段，并在 `init` 中监听 `settingsRepository.settingIconStyleFlow`。
5. [`feature/statistics/.../components/LevelBreakdownDialog.kt`](file:///C:/Users/AJIAN14/.gemini/antigravity/worktrees/Nemo/add_plump_duo_icon_style/feature/statistics/src/main/java/com/jian/nemo/feature/statistics/components/LevelBreakdownDialog.kt)
   - 在 `isPlumpDuo = true` 时，N5~N1 级别颜色统一切换为标准等亮度阶梯（`EmeraldGreen`, `LakesideCyan`, `SkyIndigo`, `WarmAmber`, `RoseRuby`）；关闭按钮保持灰色中性控件。

## 偏差说明 (Deviations)
- 无偏差。完全按规范与提示词约束执行。

## 发现与处理的边界情况 (Discovered Edge Cases)
1. **中性色箭头防污染**：`StatisticsListCard` 中「展开查看更多 / 收起」在 MD3 模式下原为主题主色，但在 Plump Duo 模式下，根据指南铁律（禁止滥用彩色），箭头图标严格切换为中性色 `MaterialTheme.colorScheme.onSurfaceVariant`，文字使用等亮度色 `SkyIndigo`。
2. **纯色背景块消除**：今日统计顶部卡片原有 38dp 圆形色块，空状态原有 64dp 圆角方块；根据设计规范第 2 条，Plump Duo 模式下将其彻底置为无背景透明对齐容器，由双色线面自身的微光填充呈现通透立体层次。
3. **工作区环境变量与配置**：git worktree 初始未包含 `.gitignore` 排除的 `gradle.properties`，从主分支目录补全后，通过 `.\gradlew :feature:statistics:compileDebugKotlin` 与 `.\gradlew :app:compileDebugKotlin` 双重验证，全模块 100% 编译通过。
