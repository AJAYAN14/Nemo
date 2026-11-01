# 经验复盘 05：多 AI 并行 UI 字符串收编与 ViewModel 上下文避坑

> 标签：`UI字符串收编`, `ViewModel上下文`, `多AI并行隔离`, `零臆造原则`, `Hilt`

---

## 一、背景与问题

在多 AI 协同开展全工程 UI 字符串硬编码治理任务（`AI_A` 至 `AI_L`）中，不同 AI 负责各自子功能模块。治理过程中存在三大潜在陷阱：
1. **多 AI 资源合并冲突**：多个 AI 同时修改同一个全局 `strings.xml` 必然导致 Git 代码冲突与合并灾难。
2. **ViewModel 字符串解析歧义**：ViewModel 无生命周期 Composable 上下文，无法调用 `stringResource`；若直接使用硬编码或传递错误上下文可能引发内存泄漏。
3. **文案臆造失真**：AI 易自行修饰或总结文案，导致收编后的文本与原产品设计脱节。

---

## 二、避坑复盘对照表

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
|---|---|---|---|
| 多个 AI 并行修改公共 `strings.xml` 造成合并冲突与构建报错 | 采用“专属独立资源文件”隔离策略（如 `strings_learning_ai_workshop.xml`），模块内按业务边界切分，实现 0 冲突合并 | `feature/learning/src/main/res/values/strings_learning_ai_workshop.xml` | **专属资源独占原则**：多 Agent 并行重构必须拥有独占的命名空间或资源分片文件，杜绝全局单点争用。 |
| ViewModel 缺乏 Composable 上下文无法直接调用 `stringResource` | 通过 Hilt 注入 `@ApplicationContext context: Context`，在非组合层统一通过 `context.getString(R.string.xxx)` 取用，含参数文本采用标准占位符 `%1$s` | `AIWorkshopViewModel.kt`, `@ApplicationContext` | **非组合上下文单一定位原则**：ViewModel 层文案严禁裸字符串，统一经由 ApplicationContext 获取或向 UI 层暴露 StringRes ID。 |
| 提取时自行篡改或脑补文案，破坏原始业务语义 | 编写只读 Python 脚本排查并严格提取双引号原文字面量，XML 文本一字不改逐字收编 | `scratch/audit_ai_c.py`, `strings_learning_ai_workshop.xml` | **文本原始忠实公理**：国际化收编仅做结构化归宿迁移，绝对禁止二次加工或臆造文案。 |
| 批量正则替换破坏 Kotlin 括号与语义结构 | 严禁黑盒外部脚本修改源码，仅允许脚本用于只读审查，源码修改使用受控的透明原生工具与单模块编译验证 | `./gradlew :feature:learning:compileDebugKotlin` | **审查自动化与修改可控原则**：审查工具脚本化，代码修改结构化，模块编译闭环化。 |

---

## 三、验证结果

1. **静态审查**：任务区内 `AIWorkshopScreen.kt`、`AIWorkshopViewModel.kt`、`AIAuroraBackground.kt` 硬编码检出数降为 0 处。
2. **构建验证**：执行 `./gradlew :feature:learning:compileDebugKotlin` 独立编译验证成功，0 错误交付。
