# Compose 与 ViewModel 文本硬编码收编避坑

> 本文档记录在对 Compose UI 与 ViewModel 进行文本硬编码收编时的典型坑点与最佳实践。

---

## 经验对照表

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **Compose `remember` 闭包内非法调用 Composable 函数**：在 `remember { listOf(ThemeCardInfo(name = stringResource(...))) }` 内部调用 `stringResource` 会引发 `@Composable invocations can only happen from the context of an @Composable function` 编译阻断。 | 对于静态数据模型列表，直接在 Composable 上下文创建列表（`val themes = listOf(...)`）或在 `remember` 外部先通过 `stringResource` 读取文案。轻量数据对象重组开销可忽略，且能实时响应系统语言切换。 | `AIReadingScreen.kt`, Jetpack Compose Runtime | **Composable 上下文纯净原则**：`remember` 的计算 lambda 属于非组合普通闭包，严禁在其中隐式嵌套 Composable 函数调用。 |
| **ViewModel 内部硬编码提示文案与状态默认值**：在 ViewModel 或 UiState 中直接给 `readingTheme` 等状态赋中文硬编码默认值，导致多语言环境下无法本地化。 | ViewModel 构造注入 `@ApplicationContext context: Context`，在 `init` 块中通过 `context.getString(R.string.xxx)` 安全赋初始值；提示文案与动态异常统一采用 `context.getString(R.string.xxx, arg)` 标准占位符格式化。 | `AIReadingViewModel.kt`, Hilt `@ApplicationContext` | **单一数据源国际化原则**：ViewModel 中涉及任何面向用户的状态与异常提示，一律通过 Application Context 统一收敛至 `strings.xml`。 |
| **多 AI 并行重构时的资源文件命名与合并冲突**：多个 Agent 同时向公共 `strings.xml` 写入时极易引发 Git 合并冲突与键名重叠。 | 严格实行“工单专属独立资源文件”隔离策略（如 `strings_learning_ai_reading.xml`），键名严格遵循 `[模块]_[页面/组件]_[类型]_[描述]` 规范。 | `strings_learning_ai_reading.xml`, Android Resources | **模块资源空间独占隔离原则**：并行重构时必须划分专属独立资源文件，从根源切断冲突链路。 |
| **臆造展示文本导致业务偏离**：提取硬编码时自行润色或改动原有中文字面量，破坏既有交互一致性。 | 严格以源码既有字符串字面量为准，原汁原味 1:1 提取，严禁擅自改写或自行编造文本。 | Python 静态审查脚本, Git Diff | **源码真实性唯一法典原则**：重构仅改变代码架构与组织形式，严禁擅自改变原有业务文案与行为。 |
