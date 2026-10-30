# 架构重构实施记录（Implementation Notes）

> 本文件记录《LearningViewModel 模块化瘦身重构》实施过程中的偏差记录、新发现的边界情况与待复核问题。
> 保持与当前代码实现完全同步。

---

## 一、Markdown 文档审查与未实现清单（Gap Analysis）

基于 `delivery/` 产物（高层架构设计、系统设计、UserStory）及 `.workbuddy/memory/2026-10-29.md` 的对照审查：

### 1. MVP（第一阶段，P0）
- [x] **F1~F10**: 7 个 Delegate 组件（Settings, SessionLifecycle, Rating, QueueNavigation, Undo, CardOps, StatsTimer）与 StateUpdater 提取完成，`LearningViewModel` 瘦身至 287 行（≤400 目标达成）。
- [x] **F11**: 对外状态与事件接口（`val uiState: StateFlow<LearningUiState>`, `fun onEvent(event: LearningEvent)`）100% 兼容。
- [x] **F12 / N1 (US-12)**: 行为契约测试（`LearningBehaviorContractTest`）已实现并 100% 通过（覆盖评分防抖 100ms、导航防抖 400ms、跨天重置、提前学习容差 Learn Ahead、撤销恢复 5 大场景）。
- [x] **工程配置**: `feature:learning` 引入 `mockk` 与 `turbine` 测试依赖（已声明并通过编译）。

### 2. 第二阶段（P1: 重复消除与架构规范化）
- [x] **F16 (US-16 / R6 / X2)**: `LearningSessionStatsManager` 改为 DI 注入（`@Inject constructor + @ApplicationContext`），消除 VM 中硬编码 `new` 和 Context 依赖（已实施并通过编译与单测验证）。
- [x] **F15 (US-15 / R5 / X5)**: `CardOpsDelegate` 中 `buryCurrentItem` 和 `handleLeech(bury_today)` 合并抽取公共 `buryItem` 方法（已实施并通过编译与单测验证）。
- [x] **F14 (US-14 / R4 / X4)**: `RatingDelegate` 中 `processSrsUpdate` 和 `processRelearningGraduation` 合并抽取公共 `processGraduation`（已实施并通过编译与单测验证）。
- [x] **F13 (US-13 / R3 / X6)**: `SessionLifecycleDelegate` 中 `loadWordSession` 与 `loadGrammarSession` 泛型合并为 `loadSession<T : SrsItem>`（已实施并通过编译与单测验证）。

### 3. 独立 Delegate 单元测试（N2 / Q3）
- [x] **RatingDelegateTest**: 覆盖 quality 5 熟词速通、quality < 3 忘记与失败重入队 (Lapse)、quality 3-4 掌握毕业、Leech 钉子户以及 Processing 防抖保护 5 大分支（100% 通过）。
- [x] **CardOpsDelegateTest**: 覆盖手动暂停 (`suspendCurrentItem`)、今日暂缓 (`buryCurrentItem`)、钉子户 Leech 两大处置分支 (`bury_today` vs `skip`)（100% 通过）。
- [x] **UndoDelegateTest**: 覆盖快照保存、撤销完整回滚（数据库、内存状态、UI 面与动画方向）、Processing 状态保护、清空快照（100% 通过）。
- [x] **QueueNavigationDelegateTest**: 覆盖 `handleSelectionResult` 三分支 (Empty/Wait/Next)、`resumeFromWaiting` 冷却恢复、前后平滑导航与越界保护、四色角标统计（100% 通过）。
- [x] **SessionLifecycleDelegateTest**: 覆盖退出学习彻底重置状态、学习模式切换并触发新会话启动、完整会话加载时序与状态装填（100% 通过）。
- [x] **测试集总览**: `feature:learning` 模块共 23 个独立单元测试全部 100% 通过，覆盖率与行为契约完整落地。

### 4. 完整版（P2）
- [x] **F18 (US-18 / R8 / X7~X10)**: 遗留废弃字段清理已完成：
  - X7: `selectedLevel` 默认值由 `"N5"` 统一为与运行时一致的 `"GLOBAL"`。
  - X8: 移除未使用的 `LearningStatus.NoMoreWords` 枚举值。
  - X9: 移除未完全实现的预留字段 `sessionQueue`。
  - X10: 移除未使用的统计字段 `sessionCorrectCount`。
  - 经全局静态引用审查与 12 模块 `assembleDebug` 全量构建，无任何编译或调用影响。
- [x] **测试支持与文档**: 生成完整测试排查教程 [docs/学习模块测试教程.md](file:///c:/Users/AJIAN14/WorkBuddy/Worktrees/Nemo/main-4a07b69b/docs/学习模块测试教程.md)，覆盖 23 项自动化单测及 15 大真机实测环节（修正单步撤销描述、埋藏提示样式，并补齐跳题评分、语法专属、纠错反馈、白板拼写、生命周期挂起与加餐边界）。

---

## 二、偏差记录（Deviations Log）

| 编号 | 涉及组件 / 场景 | 设计文档描述 | 实际实现方案 | 偏离原因与正当性 |
| :--- | :--- | :--- | :--- | :--- |
| D-01 | Delegate 构造方式 | 设计 §3.4 曾提及 delegate 可被 Hilt 注入 | VM 内部手动实例化各 delegate | 多个 delegate 需共享同一个 `LearningSessionState` 实例与 `StateUpdater`，Hilt 无法自动绑定同一个 VM 实例内共享的状态容器。 |
| D-02 | QND↔STD 构造循环 | 模块调用图存在双向依赖 | 使用 `queueNavigationDelegate.statsTimerDelegate = statsTimerDelegate` 延迟注入打破环 | 保证构造期单向初始化，避免构造期死循环。 |
| D-03 | StatsManager 注入演进 | 现状在 VM 中 `new LearningSessionStatsManager(context)` | 改造为 `@Inject constructor(@ApplicationContext context)`，VM 注入后分发给 STD | 规范 DI 依赖，从 VM 与 STD 中剥离无用的 `Context` 引用。 |

---

## 三、新发现的边界情况（Discovered Edge Cases）

1. **Context 冗余引用**：
   - 发现 `StatsTimerDelegate` 构造函数接收了 `context: Context`，但在其方法体内完全没有被使用。
   - `LearningViewModel` 原先仅为了实例化 `sessionStatsManager` 而注入 `@ApplicationContext context: Context`。将 `LearningSessionStatsManager` 改为 `@Inject` 后，VM 和 STD 均可完全移除 `Context` 参数。
2. **测试依赖**：
   - `gradle/libs.versions.toml` 中已有 `mockk = "1.13.13"` 和 `turbine = "1.2.0"`，但 `feature/learning/build.gradle.kts` 的 `testImplementation` 尚未声明，需添加以支持行为契约测试。
3. **会话结算统计一词两算（Double-Counting）**：
   - 复习卡片在会话中点击“重来”后，既被统计在 `sessionReviewCount`（今日复习去重全集）中，又被记录进 `sessionRelearnCount`（攻克子集）。
   - 结算页面原先采用 `sessionNewCount + sessionReviewCount + sessionRelearnCount`，导致总词数虚增（如 10 词变为 11 词）。
   - 现已重构为互斥三分类体系：`sessionTotalCount = sessionNewCount + maxOf(sessionReviewCount, sessionRelearnCount)`，旧词拆解为“顺畅巩固”（`sessionReviewCount - sessionRelearnCount`）与“反复攻克”（`sessionRelearnCount`），总数严密保真，三段百分比加和严格为 100%。详细复盘见 [docs/experience/01-会话结算统计重复计算与互斥三分类修复.md](file:///c:/Users/AJIAN14/WorkBuddy/Worktrees/Nemo/main-4a07b69b/docs/experience/01-会话结算统计重复计算与互斥三分类修复.md)。

---

## 四、待复核问题（Questions for Review）

1. 行为契约测试中对外部 Repository（如 `WordRepository` / `GrammarRepository`）是采用 Mockk 还是配合 Fake 数据源？（当前实现采用 Mockk 保持快速轻量单元测试，50 个单测 5 秒全过）。
2. **F17 自动翻面事件统一 (X3)**：目前依约保留双事件（`LearningEvent.ToggleAutoRevealAnswer` 与 `LearningEvent.ToggleGrammarAutoRevealAnswer` 分立），确保下游 Compose UI 零改动。是否需要在后续版本与 UI 层一同重构，收敛为单事件？
3. **卡片操作通知胶囊类型**：当前埋藏 (Bury) 与暂停 (Suspend) 沿用历史基线赋值给 `uiState.error`（在 UI 渲染为黄色/红色 Warning/Error 胶囊）。是否期望在后续升级为 `uiState.successMessage`（渲染为绿色成功通知）？
