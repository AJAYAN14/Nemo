# 词库大厅 UI 文本收编与 ViewModel 上下文注入避坑

> 本文档记录【AI K】在执行 `:feature:library` 模块（词库大厅与分类词汇）UI 文本硬编码清零、多 AI 并行资源隔离以及 ViewModel 字符串获取架构中的避坑经验与规范沉淀。

---

## 核心避坑四列对照表

| 坑是什么（Problem） | 怎么避（Solution） | 相关工具 / 文件 | 统一原则（Principle） |
| :--- | :--- | :--- | :--- |
| **多 AI 并行资源文件合并冲突**：多个 Agent 同时向公共 `strings.xml` 写入资源，导致合并与构建中断。 | **专属资源独占分片**：为本任务区设立独立的 `strings_library.xml`，所有新增资源闭环管理，互不干扰。 | `feature/library/src/main/res/values/strings_library.xml` | **专属资源独占原则**：多任务并行时必须采用独立分片资源文件，杜绝公共资源文件并发写入冲突。 |
| **ViewModel 内部展示文本获取失范**：误传 Activity 上下文造成内存泄漏，或直接在 ViewModel 硬编码字符串。 | **ApplicationContext 规范注入**：通过 Hilt 注入 `@ApplicationContext private val context: Context`，统一调用 `context.getString(R.string.xxx, args)`。 | `CategoryWordsViewModel.kt`, `PosWordsViewModel.kt`, `WordDetailViewModel.kt`, `GrammarDetailViewModel.kt`, `PartOfSpeechViewModel.kt` | **无泄漏上下文公理**：ViewModel 只能持有 Application 级别生命周期的 Context，严禁持有 Activity 引用。 |
| **非 Composable 顶层函数直接调用 Compose API**：在普通顶层辅助函数中试图直接调用 `stringResource` 导致编译失败。 | **@StringRes 资源 ID 延迟消费**：辅助函数仅负责业务逻辑分支判断，返回 `@StringRes Int` 资源 ID，在 Composable 内部统一通过 `stringResource(resId)` 消费。 | `ExpressionScreen.kt`（`getCategoryShortNameRes`） | **资源解耦与延迟解析原则**：业务映射逻辑与 UI 消费环境解耦，非 Composable 仅返回语义资源 ID。 |
| **硬编码清理漏网与文案臆造偏差**：人工逐行排查容易产生遗漏，或重构时 AI 擅自润色改动文案内容破坏一致性。 | **双向验证与零臆造纪律**：100% 提取源码原有字面量原汁原味入库；使用只读 Python 自动化脚本全量深度扫描复测，确保 0 硬编码遗漏。 | `scratch/audit_hardcoded_strings.py`, `strings_library.xml` | **文本源码绝对忠实公理**：资源化提取必须完全忠实于原工程文本字面量，严禁主观创作或润色文本。 |

---

## 经验总结与后续开发指引

1. **单模块 R 资源引用闭环**：所有 Kotlin 源码统一引用 `com.jian.nemo.feature.library.R.string.xxx`，不跨模块依赖其它 Feature 的资源，保持模块独立性。
2. **多语言与参数化占位符**：对于动态错误或数量统计信息，严格使用标准占位符（如 `%1$s`），杜绝字符串拼接。
3. **扫描脚本常态化复测**：在交付前使用无害只读脚本对整个包路径进行中文正则排查，确认排除日志白名单后无任何遗漏。
