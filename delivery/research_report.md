# AICoding 架构设计 · 行业调研报告

> 本文档为《AICoding 架构设计》核心产物之一，定位为**行业调研报告（research_report）**。
> 上游输入：主理人转交的用户诉求（Nemo 日语词汇学习 App 的 LearningViewModel.kt 模块化瘦身与架构解耦）；
> 下游输出：驱动 `business-architect`（业务架构师）的行业调研判断，最终落入《高层架构设计》的 §3 行业调研章节。

> **工具说明**：由 `research-analyst`（研究分析师 - 查有据）负责产出，经 G2 自动校验与人工审核通过后方可进入下游消费。
> **结构纪律**：全文按「事实 → 对比 → 建议 → 风险」四段式组织，严禁四段之间倒序或跳段。

---

## 0. 元信息：修订记录

```yaml
标题: Nemo - LearningViewModel 模块化瘦身重构 - 行业调研报告 v1.0
版本: v1.0
状态: Reviewing
创建日期: 2026-10-29
最后更新: 2026-10-29
调研人: research-analyst（查有据）
审核人:
  - team-lead（主理人）

关联文档:
  上游输入:
    - 用户诉求: 对 Nemo 项目 LearningViewModel.kt（2443 行）进行模块化瘦身与架构解耦
    - 调研目标: 为 business-architect 的重构方案提供行业最佳实践依据
    - material_digest.md: G1 已通过，D1~D10 共 10 份资料摘要 + X1~X10 共 10 条冲突记录
  下游产出:
    - 高层架构设计 §3 行业调研: 将由 business-architect 整合到此章节
```

| 版本 | 日期 | 作者 | 变更内容 | 评审状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-29 | research-analyst（查有据） | 初稿，覆盖 §1~§7 全部章节，5 家标杆，5 维度加权矩阵 | Reviewing |

---

## 1. 调研问题收敛

> 调研启动前，先围绕用户诉求收拢为明确的调研问题集合，确保调研不偏离当前项目背景。

### 1.1 原始调研种子

> 从用户诉求和 material_digest.md 中提取需要调研验证的论题，逐条给出调研优先级。

| 编号 | 待验证论题 | 来源（用户诉求要点 / material_digest） | 调研优先级 | 备注 |
| --- | --- | --- | --- | --- |
| S1 | Android 社区在大型 ViewModel 职责分离上有哪些被广泛验证的模式（Delegate / StateHolder / Reducer / Processor），各模式的适用边界与利弊 | 用户诉求 §8 方向 1 + 方向 2；material_digest D1 §1（21 个构造参数、12 类职责） | 高 | 核心调研方向，直接决定重构策略 |
| S2 | 主流 MVI 框架（Mosby MVI / Orbit MVI / MVIKotlin）如何处理 Reducer 与副作用分离，对 Nemo 的 UDF 单向数据流有何借鉴 | 用户诉求 §8 方向 1；material_digest D1 §6（onEvent 集中分发 30+ 事件） | 高 | MVI 是项目现有架构范式 |
| S3 | 在 Jetpack Compose + Hilt + Coroutines/Flow 技术栈下，拆分 ViewModel 职责时如何保持单一可观察状态源（StateFlow）与对外方法签名不变 | 用户诉求 §8 方向 3；material_digest D1 §3（_uiState: MutableStateFlow 核心状态源）、D7（Hilt DI 模块） | 高 | 核心技术栈约束 |
| S4 | 保持对外接口兼容（StateFlow / SharedFlow + 公开方法签名）前提下的增量重构策略与安全验证方法 | 用户诉求核心约束"零逻辑与语义变更、对外接口与调用方兼容"；material_digest D1 §6（onEvent 公开方法） | 高 | 零破坏性是硬约束 |
| S5 | 针对 Word / Grammar 双模式完全同构的会话加载 / 评分 / 队列逻辑，泛型合并与策略模式的最佳实践 | 用户诉求 §8 方向 5；material_digest X6（loadWordSession / loadGrammarSession 完全同构）、X3（自动翻面配置对称性）、X4（processSrsUpdate / processRelearningGraduation 结构相似）、X5（buryCurrentItem / handleLeech 逻辑重复） | 高 | material_digest 已识别 4 条同构冲突 |

### 1.2 调研问题收敛

> 将 §1.1 的种子收敛为 5 个可执行的调研问题。每条问题必须明确调研对象、调研目标和产出预期。

| 编号 | 调研问题 | 调研对象 | 调研目标 | 预期产出 | 关联种子 |
| --- | --- | --- | --- | --- | --- |
| Q1 | Android 社区在大型 ViewModel 职责分离上有哪些被广泛验证的模式（Delegate / StateHolder / Reducer / Processor / Coordinator），各模式的适用边界、利弊与对 Nemo 12 类职责拆分的可借鉴度 | Google 官方架构文档 + Android Architecture Blueprints + Now in Android + 社区实践文章 | 模式清单 + 适用边界 + 对 Nemo 的可借鉴度评估 | 职责分离模式对比表 | S1 |
| Q2 | 主流 MVI 框架（Mosby MVI / Orbit MVI / MVIKotlin / Mavericks）如何处理 State Container、Reducer 与 Side Effect 的分离，对 Nemo 现有 onEvent 集中分发 + StateFlow 单一状态源有何借鉴 | Orbit MVI + MVIKotlin + Mavericks + Mosby MVI | 框架的职责分离机制 + 与 Nemo UDF 的契合度 | 框架对比矩阵 | S2 |
| Q3 | 在 Jetpack Compose + Hilt + Coroutines/Flow 技术栈下，拆分 ViewModel 职责时如何保持单一可观察状态源（StateFlow）与对外方法签名不变，有哪些被验证的委托 / 组合模式 | Now in Android + Orbit MVI + Hilt ViewModel 文档 + Compose 状态管理实践 | 技术栈约束下的职责拆分方案 + 接口兼容策略 | 接口兼容的拆分方案 | S3 |
| Q4 | 保持对外接口兼容（StateFlow / SharedFlow + 公开方法签名）前提下的增量重构策略与安全验证方法有哪些行业实践，如何保证每步重构零逻辑与语义变更 | Android 重构社区实践 + Now in Android 架构演进 + 零破坏性重构方法论 | 增量重构步骤 + 安全验证方法 | 增量重构策略清单 | S4 |
| Q5 | 针对 Word / Grammar 双模式完全同构的会话加载 / 评分 / 队列逻辑（material_digest X6/X3/X4/X5），泛型合并与策略模式的最佳实践是什么，如何在 Kotlin sealed class + 泛型约束下实现类型安全的双模式统一 | Kotlin 泛型最佳实践 + SessionLoader 已有泛型实现（D8 §3）+ SrsItem 接口 + sealed class 模式合并策略 | 双模式统一的泛型 / 策略方案 + 类型安全保障 | 双模式统一方案 | S5 |

---

## 2. 事实：标杆系统盘点和方案详述

> **四段式「事实」段**。只陈列调研发现的事实，不做引申建议或边界裁决。

### 2.1 行业标杆清单

> 完整盘点调研覆盖的所有标杆系统，给出标签化画像。

**硬指标**：≥ 3 家；至少包含 1 家头部 SaaS 代表 + 1 家开源/自研代表。

> 说明：Android ViewModel 架构模式领域不存在传统意义的"SaaS 产品"，本报告将"头部代表"界定为——Google 官方架构（行业标准制定者）和 Airbnb（头部互联网公司生产级框架）。全部标杆均为开源项目，满足"开源/自研代表"要求。

| 编号 | 标杆系统 | 厂商 / 社区 | 部署形态 | 场景覆盖 | 技术亮点 | 商业模式 | 调研来源 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | Android Architecture Blueprints | Google（官方） | 开源仓库 | Android App 架构模式展示（TODO App） | Compose + ViewModel per screen + Flow + Hilt + 单 Activity + Navigation Compose | 开源（Apache 2.0） | https://github.com/android/architecture-samples |
| B2 | Now in Android（NiA） | Google（官方） | 开源仓库 + Play Store 上线 | 生产级 Android App（新闻聚合）完整架构示例 | 三层架构（data/domain/UI）+ UDF + StateFlow sealed interface + Hilt + 模块化 | 开源（Apache 2.0） | https://github.com/android/nowinandroid |
| B3 | Orbit MVI | Orbit MVI 社区 | 开源库（KMP） | MVI 状态管理框架（Android/iOS/桌面） | ContainerHost 委托模式 + intent/reduce/sideEffect 分离 + 内外部状态分离 | 开源（Apache 2.0） | https://github.com/orbit-mvi/orbit-mvi |
| B4 | Mavericks（原 MvRx） | Airbnb（头部公司） | 开源库 | Android 状态管理框架（生产级） | MavericksState + setState/withState + invalidate 自动响应 + Redux 启发 | 开源（Apache 2.0） | https://github.com/airbnb/mavericks |
| B5 | MVIKotlin | arkivanov（社区） | 开源库（KMP） | MVI 框架（Kotlin Multiplatform） | Store/Executor/Bootstrapper/Reducer 四组件分离 + Decompose 生态 + 时间旅行调试 | 开源（Apache 2.0） | https://github.com/arkivanov/MVIKotlin |

### 2.2 标杆方案详述

> 每家标杆逐一展开；每段区分「已核实的事实」与「推断/假设」。

#### 2.2.1 B1 - Android Architecture Blueprints

| 维度 | 内容 | 置信度 |
| --- | --- | --- |
| 产品定位 | Google 官方维护的 Android 架构模式示例集合，展示不同架构工具和模式的最佳实践 | 已核实 |
| 目标用户 | 中级 Android 开发者和初学者（寻找可测试、可维护的 App 结构方式），高级开发者快速参考 | 已核实 |
| 核心能力 | Compose UI + 每 screen 一个 ViewModel + Flow/coroutines 响应式 + Hilt DI + 单 Activity + Navigation Compose + Room 本地数据源 + mock/prod product flavor + 单元/集成/E2E 测试 | 已核实 |
| 架构特点 | presentation layer = Compose screen（View）+ ViewModel per screen/feature；reactive UIs using Flow；data layer = repository + 两数据源（Room local + fake remote）；不使用集中式 onEvent 事件分发，用户交互通过 ViewModel 方法直接调用 | 已核实 |
| 部署形态 | 开源 GitHub 仓库，可 clone 后用 Android Studio 打开运行 | 已核实 |
| 集成方式 | 无框架引入，纯 Jetpack 标准组件（Compose/ViewModel/Flow/Hilt/Room/Navigation） | 已核实 |
| 定价模式 | 开源免费（Apache 2.0） | 已核实 |
| 优势 | Google 官方背书，行业标准；技术栈与 Nemo 项目完全一致（Compose+Hilt+Flow+MVI）；测试覆盖完整；无框架引入成本 | 综合归纳 |
| 局限 | TODO App 场景过于简单，未覆盖 God ViewModel 拆分场景；每个 ViewModel 职责单一，未展示复杂 ViewModel 的职责分离模式；不提供 MVI 的 intent/reduce 显式分离 | 已核实 + 推断 |
| 对本项目的参考价值 | 提供了"每 screen 一个 ViewModel + repository 公共 API + Flow 响应式 + Hilt DI"的标准范式，可作为 Nemo 重构后的目标架构基线；但其简单场景未直接解答 God ViewModel 如何拆分为多个委托组件的问题 | 推断 |

#### 2.2.2 B2 - Now in Android（NiA）

| 维度 | 内容 | 置信度 |
| --- | --- | --- |
| 产品定位 | Google 官方维护的完全功能性 Android App，使用 Kotlin + Jetpack Compose 构建，遵循 Android 设计和开发最佳实践，作为开发者的参考实现 | 已核实 |
| 目标用户 | Android 开发者（学习最新 Android 开发实践和官方架构） | 已核实 |
| 核心能力 | 完整三层架构（data layer / domain layer / UI layer）+ UDF 单向数据流 + sealed interface UI state + StateFlow 热流 + Hilt DI + 模块化 + WorkManager 同步 + Room + Proto DataStore + Retrofit | 已核实 |
| 架构特点 | data layer = offline-first repository（source of truth）+ 数据源（Room/Proto DataStore/Retrofit）；domain layer = use cases（operator fun invoke，组合和转换 repository 数据流，简化 ViewModel 重复逻辑）；UI layer = Compose + ViewModel，ViewModel 接收 use cases/repositories 的 cold flows → combine/map → stateIn 转换为 StateFlow 热流；UI state 用 sealed interface 建模（Loading/Success）；用户交互通过常规方法调用（lambda 传入 UI），非集中式 onEvent | 已核实 |
| 部署形态 | 开源 GitHub 仓库 + Play Store 上线（prodRelease 变体） | 已核实 |
| 集成方式 | 纯 Jetpack 标准组件 + Hilt，无第三方 MVI 框架 | 已核实 |
| 定价模式 | 开源免费（Apache 2.0） | 已核实 |
| 优势 | Google 官方生产级示例，持续维护；三层架构与 Nemo 项目现有 core(domain/data/ui) + feature 模块化结构一致；domain layer use cases 模式与 Nemo 已抽取的 8 个 domain 类方向一致；UDF + StateFlow + sealed interface UI state 完全契合 Nemo 的 MVI 架构；事件处理直接调用 repository（非集中式 onEvent），职责更清晰 | 已核实 + 综合归纳 |
| 局限 | 每个 screen 一个 ViewModel，未展示单个复杂 ViewModel 内部如何拆分为多个委托组件；domain layer 明确"不含事件处理 use cases"（事件由 UI layer 直接调用 repositories），与 Nemo 的 onEvent 集中分发模式不同；NiA 的 ViewModel 普遍较轻量，未遇到 Nemo 2443 行的规模问题 | 已核实 + 推断 |
| 对本项目的参考价值 | 三层架构 + domain use cases + UDF + StateFlow sealed interface 是 Nemo 重构的目标范式；NiA 证明了"domain layer use cases 简化 ViewModel 重复逻辑"的有效性（与 Nemo 已抽取 8 个 domain 类方向一致）；NiA 的"用户交互通过常规方法调用而非集中式 onEvent"可作为评估 Nemo onEvent 模式是否需要调整的参考 | 推断 |

#### 2.2.3 B3 - Orbit MVI

| 维度 | 内容 | 置信度 |
| --- | --- | --- |
| 产品定位 | 类型安全的 MVI 框架，面向 Kotlin Multiplatform（Android/iOS/桌面），采用 Redux/MVI 启发的单向数据流，在 MVVM 基础上增强状态管理，官方定位为"MVVM+" | 已核实 |
| 目标用户 | 需要 MVI 状态管理的 Kotlin Multiplatform 开发者，特别是希望将状态管理与副作用分离的团队 | 已核实 |
| 核心能力 | ContainerHost 委托模式 + OrbitContainer 状态容器 + intent 块（意图分发入口）+ reduce 块（纯函数状态变更）+ postSideEffect（副作用独立处理）+ 内外部状态分离 + 多平台 SavedState + 生命周期安全 Flow 收集 + orbit-test 测试工具 | 已核实 |
| 架构特点 | ViewModel 实现 OrbitContainerHost 接口（泛型参数 STATE 与 SIDE_EFFECT）成为状态持有者；实际状态管理委托给 OrbitContainer 实例（container 字段持有）；状态变更只能通过 reduce 块完成；副作用通过 postSideEffect 独立处理与状态变更分离；ContainerHost 是接口非类，任何类实现即可获得 Orbit 能力（不强制使用 Android ViewModel）；支持内部状态（ViewModel 完整状态）与外部状态（对外暴露部分状态）分离 | 已核实 |
| 部署形态 | 开源库（Maven Central），模块化分发：orbit-core（核心状态管理）+ orbit-viewmodel（Android ViewModel 集成）+ orbit-compose（Compose 支持）+ orbit-test（测试工具） | 已核实 |
| 集成方式 | Gradle 依赖引入 + 实现 ContainerHost 接口 + ViewModel.orbitContainer 工厂函数创建容器 | 已核实 |
| 定价模式 | 开源免费（Apache 2.0） | 已核实 |
| 优势 | ContainerHost 委托模式直接解决 ViewModel 职责拆分问题（状态管理委托给独立容器）；intent/reduce/sideEffect 三分离完美对应 MVI 的关注点分离；内外部状态分离可解决对外接口兼容（内部完整状态 vs 外部暴露状态）；接口非类设计灵活（不强制继承体系）；KMP 多平台支持；类型安全 + 测试工具完善 | 已核实 + 综合归纳 |
| 局限 | 引入第三方框架，与 Nemo 现有纯 StateFlow 模式存在范式差异；sideEffect 需要映射到 Nemo 现有的 SharedFlow/事件模式；现有 domain 层 8 个类需要适配 intent 模式；社区普及度不如 Google 官方架构 | 已核实 + 推断 |
| 对本项目的参考价值 | ContainerHost 委托模式是解决 Nemo God ViewModel 职责拆分的核心借鉴点——可将 LearningViewModel 的 12 类职责委托给多个 delegate 组件，每个 delegate 持有状态更新能力；intent/reduce/sideEffect 分离思想可指导 Nemo 将 onEvent 集中分发重构为 intent 分发 + reduce 状态更新 + sideEffect 副作用处理；内外部状态分离可指导 Nemo 保持对外 StateFlow 接口不变同时内部拆分状态管理 | 推断 |

#### 2.2.4 B4 - Mavericks（原 MvRx）

| 维度 | 内容 | 置信度 |
| --- | --- | --- |
| 产品定位 | Airbnb 的 Android 框架，官方定位"Android on Autopilot"，用于 Airbnb 几乎全部产品开发，目标是从最简单到最复杂的屏幕都能高效构建 | 已核实 |
| 目标用户 | Android 开发者（特别是使用 Fragment + 传统 View 体系的团队），Airbnb 内部大规模使用 | 已核实 |
| 核心能力 | MavericksState（immutable data class）+ MavericksViewModel（泛型参数 STATE）+ setState{ copy(...) } 状态更新 + withState{ state -> } 状态读取 + invalidate() 自动响应状态变化 + fragmentViewModel() ViewModel 获取 | 已核实 |
| 架构特点 | Redux 启发的状态管理：状态是 immutable data class，通过 setState 纯函数更新（copy 语义），UI 通过 invalidate() 自动响应状态变化；MavericksViewModel 继承体系，状态管理与 ViewModel 生命周期绑定 | 已核实 |
| 部署形态 | 开源库（Maven Central），Gradle 依赖引入 | 已核实 |
| 集成方式 | 继承 MavericksViewModel（泛型参数 STATE）+ 定义 MavericksState data class + Fragment 实现 MavericksView | 已核实 |
| 定价模式 | 开源免费（Apache 2.0） | 已核实 |
| 优势 | Airbnb 生产级大规模验证（头部公司代表）；setState/withState 模式简洁直观；immutable state + 纯函数更新符合 UDF；测试友好 | 已核实 + 综合归纳 |
| 局限 | 继承体系（MavericksViewModel）与 Nemo 现有 ViewModel 继承体系冲突；偏向 Fragment + 传统 View 体系（invalidate 模式），与 Jetpack Compose 集成需要适配层；setState 替换 StateFlow 需要改造对外接口，违背零破坏性约束；文档站点（airbnb.io/mavericks）在本轮调研中无法访问验证详细能力 | 已核实 + 推断 |
| 对本项目的参考价值 | setState/withState 的"immutable state + 纯函数 copy 更新"思想可借鉴（Nemo 已用 StateFlow + data class copy 实现类似语义）；但 MavericksViewModel 继承体系与 Nemo 现有架构不兼容，不建议引入框架本身；其大规模生产验证证明了 immutable state 模式的可靠性 | 推断 |

#### 2.2.5 B5 - MVIKotlin

| 维度 | 内容 | 置信度 |
| --- | --- | --- |
| 产品定位 | 可扩展的 MVI 框架，面向 Kotlin Multiplatform，提供强大的调试工具（日志和时间旅行），官方声明"feature complete，积极维护必要更新" | 已核实 |
| 目标用户 | Kotlin Multiplatform 开发者，特别是需要严格 MVI 架构和调试工具的团队；与 Decompose 组件框架配合使用 | 已核实 |
| 核心能力 | Store（状态容器，single source of truth for State）+ Executor（执行意图和副作用）+ Bootstrapper（初始化）+ Reducer（纯函数状态变更）+ Bindings（生命周期感知的输入输出连接）+ 时间旅行调试 + 日志 | 已核实 |
| 架构特点 | MVIKotlin 不强制任何特定架构，核心职责是提供 State 的 single source of truth；Store 是核心组件，内部由 Executor + Reducer + Bootstrapper 组合；UI 抽象提供高效更新（diffing）；Bindings 提供生命周期感知连接；可增量引入（从小功能开始逐步扩展）；不强制特定响应式框架（Reaktive 和 Coroutines 扩展分别提供） | 已核实 |
| 部署形态 | 开源库（Maven Central），模块化分发，KMP 多平台 | 已核实 |
| 集成方式 | Gradle 依赖引入 + 创建 Store（组合 Executor/Reducer/Bootstrapper）+ Bindings 连接 UI | 已核实 |
| 定价模式 | 开源免费（Apache 2.0） | 已核实 |
| 优势 | Store/Executor/Bootstrapper/Reducer 四组件分离最严格，职责边界最清晰；可增量引入（小功能试点后扩展）；KMP 多平台；时间旅行调试强大；feature complete 稳定；与 Decompose 配合可构建组件化架构 | 已核实 + 综合归纳 |
| 局限 | Store/Executor/Reducer 概念体系与 Nemo 现有 ViewModel + StateFlow 范式差异最大，学习曲线最陡；Bindings 需要映射到 Compose 状态收集，适配成本高；引入后需全面改造 ViewModel 结构，违背零破坏性约束；社区普及度低于 Orbit MVI | 已核实 + 推断 |
| 对本项目的参考价值 | Store/Executor/Reducer 的"状态容器 + 执行器 + 纯函数 reducer"分离思想可借鉴（指导 Nemo 将 ViewModel 拆分为状态持有者 + 业务执行器 + 状态 reducer）；但不建议引入框架本身（范式迁移成本过高，违背零破坏性约束）；可增量引入的特性可作为"未来演进方向"参考 | 推断 |

### 2.3 关键技术能力横向事实

> 不评分、不排序，仅按能力维度横陈各方案事实。

| 能力维度 | B1 Android Architecture Blueprints | B2 Now in Android | B3 Orbit MVI | B4 Mavericks | B5 MVIKotlin | 说明 / 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| ViewModel 职责拆分模式 | 每 screen 一个 ViewModel，未展示内部拆分 | 每 screen 一个 ViewModel，domain use cases 简化 ViewModel 逻辑 | ContainerHost 委托模式，状态管理委托给 OrbitContainer | MavericksViewModel 继承体系，setState/withState 模式 | Store/Executor/Reducer 四组件分离 | B1 README；B2 ArchitectureLearningJourney.md；B3 README；B4 README；B5 README |
| 状态管理机制 | MutableStateFlow + data class | StateFlow（stateIn 转换）+ sealed interface UI state | OrbitContainer + reduce 块纯函数 + StateFlow | MavericksState + setState copy | Store + Reducer 纯函数 | 同上 |
| 单向数据流（UDF） | Flow 响应式，events flow down / data flows up | UDF（events flow down, data flows up, Kotlin Flows） | intent → reduce → state flow（显式 UDF） | setState → invalidate（隐式 UDF） | intent → Store → Reducer → state（显式 UDF） | 同上 |
| 副作用分离 | 未显式分离（方法内直接处理） | 未显式分离（ViewModel 方法直接调 repository） | postSideEffect 独立处理 | 未显式分离 | Executor 处理副作用 | B2 明确"domain layer 不含事件处理 use cases"；B3 README |
| 对外接口兼容性 | 标准 StateFlow + 方法调用 | 标准 StateFlow + 方法调用（lambda 传入 UI） | ContainerHost 接口，内外部状态分离可保持对外接口 | MavericksViewModel 继承体系，setState 替换 StateFlow 需改造接口 | Store 接口，Bindings 需映射到 Compose | B3 README（内外部状态 PR #294）；B4 README |
| Hilt DI 兼容性 | 原生兼容（Blueprints 使用 Hilt） | 原生兼容（NiA 使用 Hilt） | 兼容（ContainerHost 接口，ViewModel 可 @HiltViewModel 注入） | 需适配（MavericksViewModel 继承体系与 Hilt ViewModel 集成需自定义） | 兼容（Store 可通过 Hilt 注入，但需手动连接） | B1 README（"Dependency injection using Hilt"）；B2 ArchitectureLearningJourney.md；B3 README；B4 README |
| 增量引入能力 | 不适用（是示例 App 非框架） | 不适用（是示例 App 非框架） | 支持（模块化，可逐步引入 orbit-core/viewmodel/compose） | 不支持（需全面替换 ViewModel 基类） | 支持（"可增量引入，从小功能开始逐步扩展"） | B3 README（模块化）；B5 README（"can be introduced incrementally"） |
| Compose 集成 | 原生（Blueprints 使用 Compose） | 原生（NiA 使用 Compose） | 原生（orbit-compose 模块） | 需适配（偏向 Fragment + View，invalidate 模式） | 需适配（Bindings 映射到 Compose collectAsState） | 各 README |
| 测试支持 | 单元/集成/E2E 测试 | 单元/集成测试 | orbit-test 模块（awaitItem/expectInternalState/expectExternalState） | withState 测试 | 时间旅行调试 + 日志 | 各 README |
| 模块化架构 | 每 screen/feature 独立 | 三层 + 模块化（:app + :core + :feature） | 框架自身模块化（core/viewmodel/compose/test） | 单一框架 | 框架自身模块化 + Decompose 组件化 | B1 README；B2 ArchitectureLearningJourney.md |

---

## 3. 对比：对比矩阵与加权评分

> **四段式「对比」段**。在 §2 的事实基础上建立对比矩阵，赋予权重并打分。

### 3.1 对比矩阵

> **每行权重之和 = 1.00**。评估维度与权重根据本次调研问题调整，权重理由基于用户诉求核心约束。

| 评估维度 | 权重 | 权重理由 | B1 得分 | B2 得分 | B3 得分 | B4 得分 | B5 得分 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 场景契合度 | 0.30 | 用户诉求核心是 God ViewModel（2443 行、12 类职责）拆分，标杆与该场景的契合度最重要。考量标杆是否直接展示 ViewModel 内部职责拆分模式、是否与 Nemo 的 Kotlin+Compose+MVI+Hilt+Flow 技术栈一致 | 4 | 4 | 5 | 4 | 4 |
| 合规可控性 | 0.25 | 用户诉求硬约束"零逻辑与语义变更、对外接口与调用方兼容（StateFlow/SharedFlow + 公开方法签名）"，标杆方案是否能保证接口兼容、是否可控 | 5 | 5 | 4 | 3 | 3 |
| 集成难度（反向） | 0.20 | Nemo 已有 Hilt DI + domain 层 8 个已抽取类 + 纯 StateFlow 模式，标杆与现有架构的集成难度越低越好 | 4 | 4 | 3 | 2 | 2 |
| 技术成熟度 | 0.15 | 标杆方案的成熟度、社区验证程度、持续维护状态 | 5 | 5 | 4 | 5 | 4 |
| 成本（反向） | 0.10 | 引入标杆方案的成本（框架引入 / 学习 / 迁移），越低越好 | 4 | 4 | 3 | 2 | 2 |
| **加权总分** | **1.00** | — | **4.40** | **4.40** | **4.00** | **3.30** | **3.15** |

**评分标尺**：每项 1~5 分，1 = 严重不符合，3 = 基本满足但存在明显局限，5 = 完美契合。

**逐项打分依据**：

- **场景契合度（0.30）**：
  - B1（4）：技术栈完全一致（Compose+Hilt+Flow），但 TODO App 场景过简单，未展示 ViewModel 内部职责拆分
  - B2（4）：技术栈完全一致，生产级 App + 三层架构 + domain use cases 可参考，但每 screen 一个 ViewModel 未展示内部拆分
  - B3（5）：ContainerHost 委托模式直接解决 ViewModel 职责拆分，intent/reduce/sideEffect 三分离完美对应 MVI，与 Nemo 技术栈兼容
  - B4（4）：setState/withState 模式成熟，但偏向 Fragment+View，未直接展示 Compose 下的 ViewModel 内部拆分
  - B5（4）：Store/Executor/Reducer 四组件分离最严格，可拆分复杂 ViewModel 为多个 Store，但概念体系过重

- **合规可控性（0.25）**：
  - B1（5）：标准 StateFlow + 方法调用，完全可控接口兼容，无框架引入
  - B2（5）：标准 StateFlow + sealed interface UI state + 方法调用，完全可控，无框架引入
  - B3（4）：委托模式可保持对外 StateFlow 接口，内外部状态分离可解决接口兼容，但引入框架需适配 sideEffect→SharedFlow 映射
  - B4（3）：MavericksViewModel 继承体系与现有 ViewModel 冲突，setState 替换 StateFlow 需改造对外接口
  - B5（3）：Store 接口替换 StateFlow 需适配层，Bindings 需映射到 Compose，对外接口需改造

- **集成难度反向（0.20）**：
  - B1（4）：与 Hilt 原生兼容，无需引入框架，但需自行设计拆分方案
  - B2（4）：与 Hilt 原生兼容，domain use cases 模式与已有 domain 层一致，但需自行设计拆分方案
  - B3（3）：需引入 Orbit 框架，与 Hilt 集成需配置，现有 domain 层 8 个类需适配 intent 模式
  - B4（2）：需引入 Mavericks 框架，继承体系与 Hilt ViewModel 集成需自定义，现有 StateFlow 需全面替换
  - B5（2）：需引入 MVIKotlin 框架，Store/Executor/Reducer 概念体系差异大，学习曲线陡

- **技术成熟度（0.15）**：
  - B1（5）：Google 官方，行业标准
  - B2（5）：Google 官方生产级 App，Play Store 上线，持续维护
  - B3（4）：社区活跃，KMP 支持，但不如 Google 官方普及
  - B4（5）：Airbnb 生产级，大规模验证
  - B5（4）：feature complete，持续维护，Decompose 生态

- **成本反向（0.10）**：
  - B1（4）：无需引入框架，只借鉴模式，成本低
  - B2（4）：借鉴模式，无框架引入
  - B3（3）：引入新框架，学习成本中等，迁移需改造 ViewModel 结构
  - B4（2）：引入新框架，大规模迁移，范式差异大
  - B5（2）：引入新框架，范式迁移成本最高，学习曲线最陡

### 3.2 评分结论

> 基于 §3.1 加权总分，形成分层结论。每层结论必须引用得分作为依据。

- **优先借鉴**：B1 Android Architecture Blueprints（4.40）+ B2 Now in Android（4.40）— 理由：Google 官方架构标杆，技术栈与 Nemo（Kotlin+Compose+MVI+Hilt+Flow）完全一致，合规可控性 5/5（标准 StateFlow + 方法调用，完全可控接口兼容，无框架引入成本），集成难度反向 4/5（与 Hilt 原生兼容，domain use cases 模式与已有 domain 层一致）。B2 的三层架构 + domain use cases + sealed interface UI state 是 Nemo 重构的目标范式，B1 提供标准 Compose+Hilt+Flow 基线。两者共同提供"目标架构基线 + domain 层抽取方向验证"。

- **部分借鉴**：B3 Orbit MVI（4.00）— 借鉴点：ContainerHost 委托模式（状态管理委托给独立容器，解决 ViewModel 职责拆分）、intent/reduce/sideEffect 三分离（指导 onEvent 重构为 intent 分发 + reduce 状态更新 + sideEffect 副作用处理）、内外部状态分离（保持对外 StateFlow 接口不变同时内部拆分状态管理）。不借鉴的部分：不引入 Orbit 框架本身（避免范式迁移成本和 sideEffect→SharedFlow 映射适配）。理由：场景契合度 5/5（唯一直接展示 ViewModel 内部职责拆分的标杆），合规可控性 4/5（委托模式可保持对外接口但需适配），但集成难度反向仅 3/5（需引入框架）。

- **不借鉴（否决）**：B4 Mavericks（3.30）+ B5 MVIKotlin（3.15）— 否决理由：B4 的 MavericksViewModel 继承体系与 Nemo 现有 ViewModel 冲突，setState 替换 StateFlow 需改造对外接口（合规可控性 3/5），偏向 Fragment+View 与 Compose 集成需适配（集成难度反向 2/5）；B5 的 Store/Executor/Reducer 概念体系与现有 ViewModel+StateFlow 范式差异最大，学习曲线最陡（集成难度反向 2/5），Bindings 需映射到 Compose（合规可控性 3/5）。两者均违背"零破坏性重构"核心约束。Mavericks 的"immutable state + 纯函数 copy 更新"思想和 MVIKotlin 的"Store/Reducer 分离"概念可作为理论参考，但不引入框架本身。

### 3.3 方案组合分析

> 调研发现单一标杆无法覆盖 Nemo God ViewModel 拆分的全部需求，需要组合借鉴。

| 组合方式 | 覆盖哪些能力 | 未覆盖能力 | 组合复杂度 | 总体成本估算 |
| --- | --- | --- | --- | --- |
| B2 三层架构基线 + B3 ContainerHost 委托模式（不引入框架，借鉴委托思想） | 目标架构基线（UDF+StateFlow+sealed interface）+ domain use cases 方向验证 + ViewModel 内部职责委托拆分模式 + intent/reduce/sideEffect 分离思想 | B2/B1 均未展示 ViewModel 内部如何具体拆分为多个 delegate 组件的工程细节（需 business-architect 基于 B3 委托思想设计） | 中 | 低（无框架引入，只借鉴模式，迁移成本约 1~2 人月用于 LearningViewModel 2443 行的委托化拆分） |

---

## 4. 建议：取舍决策支持

> **四段式「建议」段**。基于 §2 事实 + §3 对比，给出可被 `business-architect` 直接采用的建议。本节是建议而非最终裁决，最终边界由业务架构师冻结。

### 4.1 自研 / 采购 / 复用边界建议

| 能力项 | 建议方式 | 建议依据 | 候选方案 / 系统 | 关键前提 |
| --- | --- | --- | --- | --- |
| ViewModel 内部职责委托拆分 | 自研（借鉴 B3 委托模式思想） | B3 ContainerHost 委托模式场景契合度 5/5，但引入框架本身违背零破坏性约束（合规可控性 4/5 需适配 sideEffect），因此借鉴委托模式思想但自研 delegate 组件 | B3 Orbit MVI 的 ContainerHost + intent/reduce/sideEffect 概念 | 需 business-architect 设计 delegate 组件如何持有 uiState 更新能力（通过共享 MutableStateFlow 引用或回调接口） |
| 三层架构 + domain use cases 演进 | 复用（已有底座） | B2 Now in Android 证明三层架构 + domain use cases 有效简化 ViewModel，Nemo 已有 core(domain/data/ui) + feature 模块化和 8 个 domain 类（D8），方向一致 | B2 NiA 的 domain layer use cases 模式 | 需评估现有 8 个 domain 类的接口是否需要调整以更好支持 delegate 调用 |
| UI state sealed interface 建模 | 复用（已有底座） | B2 用 sealed interface 建模 UI state（Loading/Success），Nemo 已有 LearningStatus 枚举（8 种状态）+ LearningUiState data class，可逐步演进 | B2 NiA 的 sealed interface UI state 模式 | 需评估 LearningUiState 40+ 字段是否需要分组为多个 sealed sub-state（business-architect 决策） |
| MVI 框架引入 | 不采购（否决） | B4 Mavericks（3.30）+ B5 MVIKotlin（3.15）合规可控性 3/5、集成难度反向 2/5，违背零破坏性约束 | — | — |
| Word/Grammar 双模式统一 | 自研（泛型合并 + 策略模式） | material_digest X6 指出 loadWordSession/loadGrammarSession 完全同构，D8 §3 SessionLoader 已有泛型约束（T : SrsItem）实现，可进一步合并 ViewModel 层重复代码 | D8 SessionLoader 的泛型模式 + Kotlin sealed class + 策略对象 | 需定义统一的 SrsItem 接口约束（Word/Grammar 均实现）+ 策略对象区分模式特定逻辑 |

### 4.2 MVP 范围建议

> 对用户诉求中的 P0/P1 功能给出"是否可在 MVP 内实现"的调研侧建议。此处 MVP 指"重构的第一阶段交付"。

| 功能（对齐用户诉求） | 建议 MVP？ | 理由 |
| --- | --- | --- |
| R1 ViewModel 内部职责委托化拆分（将 12 类职责拆分为 delegate 组件） | ✅（第一阶段） | B3 ContainerHost 委托模式已验证可行性，Nemo 已有 domain 层 8 个类作为拆分基础，迁移路径清晰（提取 delegate → 保持 onEvent 转发 → 逐步迁移内部逻辑） |
| R2 保持对外接口兼容（StateFlow + onEvent 签名不变） | ✅（贯穿全部阶段） | B1/B2 标准 StateFlow + 方法调用模式完全可控，委托拆分不影响对外接口（LearningViewModel 保持 @HiltViewModel + uiState StateFlow + onEvent 方法） |
| R3 Word/Grammar 双模式统一（loadWordSession/loadGrammarSession 合并） | ✅（第二阶段） | D8 SessionLoader 已有泛型实现基础，X6 明确两方法完全同构，合并风险可控（类型替换 + 策略对象） |
| R4 processSrsUpdate / processRelearningGraduation 合并（X4） | ✅（第二阶段） | 两者结构高度相似（乐观更新 UI → removeCurrentAndMoveNext → persistRatingItemAsync），仅 SRS 字段来源不同，可抽取公共方法 + 差异化参数 |
| R5 buryCurrentItem / handleLeech(bury_today) 合并（X5） | ✅（第二阶段） | 逻辑高度重复，唯一差异是提示文案和调用上下文，可抽取公共方法 |
| R6 LearningSessionStatsManager 改为 DI 注入（X2） | ✅（第二阶段） | 其他 7 个 domain 类均 @Inject constructor，StatsManager 直接 new 破坏 DI 一致性，改为 @Inject 注入 Context 即可 |
| R7 自动翻面配置 Word/Grammar 统一（X3） | ❌（完整版，建议推迟） | X3 指出显示答案等待用参数化统一但自动翻面用完全分离的事件和方法，统一涉及 LearningEvent sealed interface 改造 + settings Flow 合并，影响面较大，建议 business-architect 评估后决定是否纳入 |
| R8 遗留字段清理（X7 selectedLevel / X8 NoMoreWords / X9 sessionQueue / X10 sessionCorrectCount） | ❌（完整版，建议推迟） | 遗留字段清理涉及 UI 层和数据层验证，建议在委托化拆分完成、测试覆盖充分后单独处理 |

### 4.3 技术栈参考建议

| 技术层 | 推荐方案 | 替代方案 | 选择理由 |
| --- | --- | --- | --- |
| ViewModel 职责拆分模式 | 委托模式（Delegate Pattern）——借鉴 B3 ContainerHost 思想，自研 delegate 组件 | Reducer/Processor 模式——借鉴 B5 Store/Reducer 概念 | 委托模式与 Nemo 现有 ViewModel+StateFlow 范式兼容度最高，无需引入框架，渐进式拆分风险可控；Reducer 模式概念更严格但范式迁移成本过高（B5 集成难度 2/5） |
| 状态管理 | StateFlow + immutable data class copy（保持现状） | Mavericks setState / Orbit reduce | StateFlow 已是 B1/B2 官方推荐标准，Nemo 已在使用，无需替换；Mavericks/Orbit 的 setState/reduce 语义可参考但不引入 |
| 副作用分离 | 方法内直接处理 + viewModelScope.launch（保持现状，按需拆分到 delegate） | Orbit postSideEffect + SharedFlow | 现有模式与 B1/B2 一致，副作用拆分到 delegate 内部即可；Orbit postSideEffect 需引入框架和 SharedFlow 映射 |
| DI 集成 | Hilt @Inject constructor（保持现状，delegate 组件通过 @Inject 注入） | 手动构造 / Koin | Hilt 与 B1/B2 完全一致，Nemo 已在使用，delegate 组件 @Inject 注入零额外成本 |
| Word/Grammar 双模式统一 | 泛型方法（泛型约束 T : SrsItem）+ 策略对象（借鉴 D8 SessionLoader 已有泛型） | sealed class when 分发（保持现状） | D8 SessionLoader 已证明泛型模式可行（X6 两方法完全同构），泛型合并消除重复且类型安全；sealed class when 分发维持重复代码 |
| 增量重构验证 | 单元测试 + 接口契约测试（保持 onEvent + uiState 签名不变） | 机械化重构工具（如 Android Studio Refactor） | 零破坏性重构需要测试保障每步无逻辑变更，接口契约测试验证对外签名不变；机械化工具辅助但不替代测试 |

---

## 5. 风险与待确认项

> **四段式「风险」段**。列出调研中发现的主要风险、不确定信息、待业务架构师进一步裁决的依赖项。

### 5.1 主要风险清单

| 编号 | 风险描述 | 触发条件 | 影响范围 | 严重程度 | 缓解建议 |
| --- | --- | --- | --- | --- | --- |
| R-01 | 委托模式拆分后 delegate 组件之间的状态共享复杂度上升——LearningViewModel 当前通过单一 _uiState MutableStateFlow 集中管理 40+ 状态字段，拆分为多个 delegate 后需解决"多 delegate 更新同一 StateFlow"的并发与一致性问题 | 多个 delegate 组件同时持有 _uiState 引用并发更新 | 状态不一致、并发写入冲突、难以调试的状态竞态 | 高 | 1. delegate 组件通过统一的"状态更新接口"（如 StateUpdater 接口）更新 uiState，而非直接持有 MutableStateFlow 引用；2. 使用 Mutex 或 Channel 序列化状态更新；3. 每个 delegate 负责独立的状态子域，减少交叉更新；4. business-architect 在边界冻结时明确状态更新权限划分 |
| R-02 | onEvent 集中分发（30+ 事件）重构为 delegate 分发后，事件路由表可能引入新的间接层——LearningViewModel.onEvent 当前用 when 分发 30+ 种 LearningEvent，拆分后需保持 onEvent 签名不变同时将事件路由到正确的 delegate | onEvent 方法内部改为 delegate 分发但路由逻辑错误 | 事件丢失或路由到错误 delegate，导致用户交互无响应或行为异常 | 高 | 1. 保持 onEvent 方法签名不变，内部改为 `when(event) { is StartLearning -> sessionDelegate.handleStart(event); ... }` 的路由转发；2. 编写事件路由表单元测试，覆盖全部 30+ 事件类型；3. 重构期间保持 onEvent 为唯一事件入口，delegate 不直接暴露给 UI 层；4. 分阶段迁移——先迁移低风险事件（如 toggle* 系列），再迁移核心事件（如 rate/navigate） |
| R-03 | B3 Orbit MVI 的 ContainerHost 委托模式借鉴为"自研 delegate"后，可能缺乏框架级的约束保障——Orbit 框架通过 reduce 块强制状态变更只能通过纯函数完成，自研 delegate 无此约束，可能出现 delegate 内部直接修改状态或产生隐藏副作用 | delegate 组件内部绕过统一状态更新接口直接修改 uiState | 状态变更难以追踪、UDF 单向数据流被破坏、调试困难 | 中 | 1. 在 code review 中强制检查 delegate 是否通过 StateUpdater 接口更新状态；2. 编写 delegate 架构约束文档（类比 Orbit 的 reduce 约束）；3. 可考虑用 Kotlin委托属性或 sealed class 限制状态更新入口；4. business-architect 在系统设计阶段明确 delegate 的状态更新规约 |
| R-04 | Word/Grammar 双模式泛型合并（X6）涉及 LearningItem sealed class 的类型约束——LearningItem 有 WordItem(word: Word) 和 GrammarItem(grammar: Grammar) 两个子类，泛型合并需要 Word 和 Grammar 实现统一的 SrsItem 接口，但两者字段不完全对称 | Word/Grammar 的 SrsItem 接口实现不完整或字段映射错误 | 泛型合并后类型安全失效、运行时异常、SRS 计算错误 | 中 | 1. 先审计 Word 和 Grammar 的字段差异，定义最小 SrsItem 接口（仅包含 SRS 必需字段：id/repetitionCount/interval/stability/difficulty/nextReviewDate/lastReviewedDate/firstLearnedDate/lastModifiedTime/type）；2. 参考 D8 SessionLoader 已有的泛型约束（T : SrsItem）；3. 泛型合并后编写类型安全测试；4. 分步合并——先合并 loadWordSession/loadGrammarSession，再合并 processSrsUpdate/processRelearningGraduation |
| R-05 | 零破坏性重构的"零逻辑与语义变更"约束在 2443 行代码的迁移中难以完全保证——LearningViewModel 包含大量隐式行为依赖（如 init 块 18+ 个 Flow collector 的执行顺序、防抖常量 NAVIGATION_DEBOUNCE_MS/RATING_DEBOUNCE_MS 的时序依赖、_sessionLockedDay 跨天保护逻辑） | 迁移过程中遗漏隐式行为依赖或改变了执行时序 | 用户可感知的行为差异（导航防抖失效、评分竞态、跨天会话重置异常） | 高 | 1. 迁移前完整梳理 init 块 18+ 个 Flow collector 的依赖关系和执行顺序；2. 编写行为契约测试覆盖关键时序场景（评分防抖、导航防抖、跨天重置、Learn Ahead Limit）；3. 每步迁移后运行完整测试套件；4. 采用"提取 delegate → 保持 onEvent 转发 → 逐步迁移内部逻辑 → 每步保持接口不变"的增量策略；5. business-architect 在重构方案中明确"不可变更的行为契约"清单 |

### 5.2 待确认项（需主理人 / 业务方反馈）

> 调研中因外部信息不可得而暂不能确认的事实。

| 编号 | 待确认项 | 不确定性说明 | 若无法确认的备选路径 |
| --- | --- | --- | --- |
| U-01 | Orbit MVI 框架的内外部状态分离（PR #294）的具体 API 设计与使用模式 | GitHub README 提及该特性但详细文档（orbit-mvi 官网）在本轮调研中因网络问题未能完整访问验证，仅从 README 摘要获知概念 | 1. 由 business-architect 在系统设计阶段自行设计 delegate 的内外部状态分离方案（借鉴概念不依赖详细 API）；2. 后续补充访问 https://orbit-mvi.org 验证 |
| U-02 | Mavericks 框架与 Jetpack Compose 的集成适配方案 | Mavericks README 展示的示例基于 Fragment + invalidate() 模式，详细文档站点（airbnb.io/mavericks）在本轮调研中无法访问验证 Compose 集成能力 | Mavericks 已在评分结论中否决（3.30 分，不借鉴），此待确认项不影响最终建议；如需补充验证可后续访问 https://airbnb.io/mavericks |
| U-03 | Mosby MVI（Hannes Dorfmann）的详细 ViewState/Reducer 机制 | Mosby GitHub 仓库（3.1.1 版本）提供基础信息，但博客文章系列（hannesdorfmann.com/android/mosby3-mvi-1~3）在本轮调研中因网络问题未能访问 | Mosby 未纳入正式标杆清单（已用 B1-B5 五家覆盖），其 ViewState/Reducer 概念与 B3 Orbit MVI 的 reduce 块语义一致，可通过 B3 的已核实信息覆盖 |
| U-04 | Nemo 项目 LearningViewModel 的现有测试覆盖率和测试基础设施状况 | material_digest.md 未包含测试文件摘要（D1~D10 均为源码/配置/文档，无测试文件），无法确认零破坏性重构的测试保障基线 | 由 business-architect 在重构方案中要求"先补齐关键行为契约测试再开始迁移"，或由 knowledge-ingest-engineer 补充摘要测试文件 |

### 5.3 需业务架构持续关注的依赖项

> 调研中发现但不由 `research-analyst` 裁决的下游问题。

| 编号 | 依赖项 | 说明 | 建议关注阶段 |
| --- | --- | --- | --- |
| D-01 | delegate 组件的状态更新权限划分——多个 delegate 如何安全地更新同一 _uiState MutableStateFlow | 调研建议委托模式拆分，但 delegate 间的状态更新规约需 business-architect 在边界冻结时明确（见 R-01 缓解建议） | 高层架构设计 §5.2（接口设计） |
| D-02 | LearningEvent sealed interface 是否需要调整——onEvent 当前用 when 分发 30+ 事件，委托化后事件路由表是否需要分组或引入 sealed sub-interface | X3 指出 Word/Grammar 自动翻面事件完全对称分离，统一涉及 LearningEvent 改造；business-architect 需评估是否在重构中调整事件模型 | 高层架构设计 §4（业务边界） |
| D-03 | LearningUiState 40+ 字段是否需要分组为多个 sealed sub-state——当前单一 data class 承载全部状态，委托化后可能需要按职责域分组 | B2 NiA 用 sealed interface 建模 UI state（Loading/Success），Nemo 可参考按职责域分组（如 SessionState/SettingsState/StatsState） | 高层架构设计 §5.1（状态设计） |
| D-04 | LearningSessionStatsManager 改为 DI 注入（X2）后的 Context 依赖处理——StatsManager 使用 SharedPreferences（nemo_learning_time_prefs），需 Context，Hilt 注入需 @ApplicationContext | X2 指出 StatsManager 是唯一未走 DI 的 domain 类，改造涉及 Hilt @ApplicationContext 注入配置 | 高层架构设计 §5.2（DI 配置） |
| D-05 | 遗留字段清理（X7 selectedLevel / X8 NoMoreWords / X9 sessionQueue / X10 sessionCorrectCount）的时机——是否纳入本轮重构或单独处理 | X7-X10 均为遗留未使用/不一致字段，清理涉及 UI 层和数据层验证，business-architect 需决定纳入范围 | 高层架构设计 §4（重构范围） |

---

## 6. 关键来源目录

> 集中列出全部调研所使用的公开资料、官方文档、社区仓库、分析报告等。每条来源不低于 URL 粒度，关键来源给出具体章节。

**硬指标**：
- ≥ 3 条来源，至少覆盖每家标杆。
- 关键数据须指定来源段落/图表位置。

| 编号 | 来源类型 | 标题 / 名称 | URL / 路径 | 相关章节 | 最后访问日期 |
| --- | --- | --- | --- | --- | --- |
| SR-01 | 开源仓库 | Android Architecture Samples（GitHub README） | https://github.com/android/architecture-samples | B1, §2.2.1, §2.3 | 2026-10-29 |
| SR-02 | 架构文档 | Now in Android - Architecture Learning Journey | https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md | B2, §2.2.2, §2.3, §4.1, §4.3 | 2026-10-29 |
| SR-03 | 开源仓库 | Now in Android（GitHub README） | https://github.com/android/nowinandroid | B2, §2.1, §2.2.2 | 2026-10-29 |
| SR-04 | 开源仓库 | Orbit MVI（GitHub README） | https://github.com/orbit-mvi/orbit-mvi | B3, §2.2.3, §2.3, §3.2, §4.1 | 2026-10-29 |
| SR-05 | 开源仓库 | Mavericks - Android on Autopilot（GitHub README） | https://github.com/airbnb/mavericks | B4, §2.2.4, §2.3 | 2026-10-29 |
| SR-06 | 开源仓库 | MVIKotlin（GitHub README） | https://github.com/arkivanov/MVIKotlin | B5, §2.2.5, §2.3 | 2026-10-29 |
| SR-07 | 开源仓库 | Mosby MVP/MVI Library（GitHub README） | https://github.com/sockeqwe/mosby | §2.2.3 对比参考, U-03 | 2026-10-29 |
| SR-08 | 项目资料 | material_digest.md（G1 已通过，D1~D10 共 10 份资料摘要 + X1~X10 冲突记录） | .workbuddy/output/material_digest.md | §1.1, §2.2, §4.1, §4.2, §5.1, §5.3 | 2026-10-29 |
| SR-09 | 官方文档（未能访问） | Android Architecture Guide（developer.android.com/topic/architecture） | https://developer.android.com/topic/architecture | §2.2.1, §2.2.2 引用（内容通过 B1/B2 README 间接核实） | 2026-10-29（访问需 Google 登录，未能直接获取） |
| SR-10 | 官方文档（未能访问） | Android ViewModel 文档（developer.android.com/topic/libraries/architecture/viewmodel） | https://developer.android.com/topic/libraries/architecture/viewmodel | §2.2.1, §2.2.2 引用（内容通过 B1/B2 README 间接核实） | 2026-10-29（访问需 Google 登录，未能直接获取） |

**来源置信度说明**：
- SR-01~SR-08：已通过 WebFetch 直接获取并核实，置信度"已核实"
- SR-09~SR-10：developer.android.com 需 Google OAuth 登录，本轮调研未能直接访问，但 B1（architecture-samples）和 B2（NiA）的 README 均引用并遵循该官方架构指南，核心内容通过 B1/B2 间接核实，置信度"推断（来源：B1/B2 README 引用）"
- SR-07（Mosby）：仅获取 GitHub README 基础信息，博客文章系列（hannesdorfmann.com/android/mosby3-mvi-1~3）因网络问题未能访问，Mosby 未纳入正式标杆清单，仅作对比参考

---

## 7. 硬指标清单

> 汇总本模板所有章节的硬指标，供自动校验与人工审核使用。

| 章节 | 硬指标项 | 当前状态 | 备注 |
| --- | --- | --- | --- |
| §1 | 调研问题已收敛为 ≥ 3 条可执行问题 | ✅ | 5 条（Q1~Q5），每条明确调研对象/目标/产出 |
| §2.1 | 标杆系统 ≥ 3 家，含 ≥ 1 家头部 SaaS | ✅ | 5 家（B1~B5），B2 Google 官方 + B4 Airbnb 为头部代表 |
| §2.1 | 标杆系统 ≥ 1 家开源或自研代表 | ✅ | 5 家全部开源（Apache 2.0） |
| §2.2 | 每家标杆有独立详述卡片 | ✅ | B1~B5 共 5 张详述卡片，每张 10 个维度 + 置信度标注 |
| §2.3 | 关键能力横向事实无遗漏 | ✅ | 10 个能力维度横向对比 |
| §3.1 | 对比矩阵含 5 维度 + 权重 + 评分 | ✅ | 5 维度，权重和 = 0.30+0.25+0.20+0.15+0.10 = 1.00 |
| §3.2 | 评分结论含优先/部分/不借鉴三层 | ✅ | 优先：B1+B2（4.40）；部分：B3（4.00）；不借鉴：B4（3.30）+B5（3.15） |
| §4.1 | 自研/采购/复用边界有明确建议 | ✅ | 5 条能力项建议 |
| §4.2 | MVP 范围建议与用户诉求对齐 | ✅ | 8 项功能（R1~R8）对齐用户诉求核心约束 |
| §5.1 | 主要风险 ≥ 3 条，有缓解建议 | ✅ | 5 条风险（R-01~R-05），每条含触发条件/影响范围/严重程度/缓解建议 |
| §6 | 关键来源可追溯（URL / 章节） | ✅ | 10 条来源（SR-01~SR-10），全部 URL 粒度，标注置信度 |
| 全文 | 明确区分事实 / 推断 / 建议与风险 | ✅ | §2 事实段标注置信度（已核实/推断/综合归纳）；§3 对比段；§4 建议段明确"建议非裁决"；§5 风险段 |
| 全文 | 不存在编造来源或占位符 | ✅ | 无尖括号占位符、示例前缀、日期占位符或待验证标记残留；SR-09/SR-10 标注"未能访问"非伪造 |

---

## 附录 A：自检报告（中间确认协议自检记录）

> 按中间确认协议 §2.4 要求，在关键章节产出后插入自检。以下为四次自检的完整记录。

### A.1 第一次自检（§1 调研问题收敛后）

**自检时间**：2026-10-29，§1 完成后

**§2.1 触发标准 #1（方案分歧型）判定**：
- 条件 1（≥2 种方案）：调研问题收敛本身不是方案选择，用户诉求第 8 节已明确给出 5 个调研方向，收敛是对诉求的忠实转译 → 不成立
- 条件 2（影响下游产出）：调研问题收敛影响标杆候选名单，但用户诉求已给出 5 个方向和标杆候选建议，不构成新的方案分歧 → 不成立
- 条件 3（用户诉求未明确选择）：用户诉求第 8 节明确给出 5 个调研方向，我收敛的 5 个问题忠实对应 → 不成立
- **结论：未命中 §2.1**

**§2.3 反向验证 3 问**：

| 问题 | 答案与证据 |
| --- | --- |
| Q1：这个决策若 3 个月后推翻，工程返工成本可控吗？ | 返工范围 = research_report.md §1 调研问题表 + §2 标杆清单 + §3 对比矩阵 + §4 建议 + §5 风险（整份报告需重做调研章节）；切换成本约 1~2 人日（重新搜索 + 重写报告）。在调研阶段产物范围内，不涉及代码返工，**可控** |
| Q2：用户/客户/监管能感知到吗？ | 调研问题收敛是内部调研方向决策，用户/客户/监管不直接感知（感知的是最终的重构方案）。判断依据：调研报告是 business-architect 的上游输入，不直接面向用户/客户/监管。**感知不到** |
| Q3：与用户原始诉求中显式提及的能力是否一致？ | 用户诉求第 8 节明确给出 5 个调研方向（"调研应聚焦以下方向：1. Android ViewModel 模块化模式…2. God ViewModel 重构策略…3. Compose + Hilt + Flow 架构下的职责拆分…4. 零破坏性重构方法…5. Word/Grammar 双模式统一"），我收敛的 5 个问题（Q1~Q5）忠实对应。**一致** |

**最终结论**：三项均未命中 §2.2，无需发起中间确认。

### A.2 第二次自检（§2.1 标杆清单完成后）

**自检时间**：2026-10-29，§2.1 完成后

**§2.1 触发标准 #1（方案分歧型）判定**：
- 条件 1（≥2 种方案）：标杆候选选择存在多种可能（6+ 候选可纳入 5 家），但用户诉求第 8 节已给出 B1-B5 标杆候选建议"不限定，可调整"，我选择 B1-B5 忠实对应 → 选择哪些标杆不是影响下游业务边界/接口契约/资源选型的方案分歧
- 条件 2（影响下游产出）：标杆选择影响 §2 详述和 §3 对比，但不影响 business-architect 的业务边界冻结（那是基于调研建议做裁决）
- 条件 3（用户诉求未明确选择）：用户诉求第 8 节明确给出标杆候选建议，我选了 B1-B5 → 不成立
- **结论：未命中 §2.1**

**§2.3 反向验证 3 问**：

| 问题 | 答案与证据 |
| --- | --- |
| Q1：标杆选择若 3 个月后推翻，工程返工成本可控吗？ | 返工范围 = research_report.md §2 详述 + §3 对比矩阵 + §4 建议的相关引用；切换成本约 1~2 人日（重新搜索 + 重写）。在调研阶段产物范围内，不涉及代码返工，**可控** |
| Q2：用户/客户/监管能感知到吗？ | 标杆选择是内部调研选型决策，用户/客户/监管不直接感知。判断依据：调研报告是 business-architect 上游输入，不面向用户。**感知不到** |
| Q3：与用户原始诉求中显式提及的能力是否一致？ | 用户诉求第 8 节给出"B1: Google Android Architecture Blueprints / B2: Mosby MVI / Orbit MVI / MVIKotlin / B3: Now in Android / B4: Android 官方 ViewModel 文档 / B5: Refactoring God Objects"标杆候选建议"不限定，可调整"，我选了 B1-B5（将 B2 拆分为 Orbit MVI + Mavericks，将 B3 提升为 NiA，将 B4 官方文档降级为间接来源因需 Google 登录，将 B5 God Objects 重构实践融入 §4 建议和 §5 风险）。**一致（忠实对应用户建议范围，按可访问性调整）** |

**最终结论**：三项均未命中 §2.2，无需发起中间确认。

### A.3 第三次自检（§3.1 权重设定前）

**自检时间**：2026-10-29，§3.1 权重设定前

**§2.1 触发标准 #1（方案分歧型）判定**：
- 条件 1（≥2 种方案）：权重分配存在多种合理方案（默认权重 vs 重设权重），但权重设定是调研分析的专业判断部分，有明确项目约束依据（零破坏性→合规可控性权重提高、God ViewModel 拆分→场景契合度最重要）
- 条件 2（影响下游产出）：权重影响最终建议，但最终建议明确标注为"建议非裁决"，business-architect 有最终裁决权
- 条件 3（用户诉求未明确选择）：用户诉求未指定权重，但权重设定的依据（合规可控性 0.25 高于默认 0.20）忠实反映了用户诉求中"零破坏性重构、对外接口兼容"核心约束
- **关键验证**：按默认权重（场景 0.30 / 技术成熟 0.20 / 集成难度 0.15 / 成本 0.15 / 合规 0.20）打分排名 = B1(4.40)=B2(4.40)>B3(4.00)>B4(3.40)>B5(3.20)，与重设权重下排名 B1(4.40)=B2(4.40)>B3(4.00)>B4(3.30)>B5(3.15) **一致，排名不反转**
- **结论：未命中 §2.1（权重重设不构成"按默认权重打分会反转推荐排名"的关键分歧）**

**§2.3 反向验证 3 问**：

| 问题 | 答案与证据 |
| --- | --- |
| Q1：权重设定若 3 个月后推翻，工程返工成本可控吗？ | 返工范围 = §3 矩阵 + §3.2 结论 + §4 建议引用；切换成本不足 1 人日（重算分值 + 重写结论）。在调研阶段产物范围内，**可控** |
| Q2：用户/客户/监管能感知到吗？ | 权重是内部评估参数，用户/客户/监管不直接感知。判断依据：权重不改变产品形态/交互/合同/合规属性。**感知不到** |
| Q3：与用户原始诉求中显式提及的能力是否一致？ | 用户诉求未显式提及权重设定（这是调研专业判断范畴），但权重依据（合规可控性权重提高至 0.25）忠实反映用户诉求核心约束"零逻辑与语义变更、对外接口与调用方兼容"。**一致（权重依据忠实反映核心约束）** |

**最终结论**：三项均未命中 §2.2，无需发起中间确认。

### A.4 第四次自检（§5.2 待确认项整理时，最后一次完整复核）

**自检时间**：2026-10-29，§5.2 完成后

**§2.1 触发标准 #1（方案分歧型）判定**：
- §5.2 待确认项（U-01~U-04）均为"外部信息不可得"的事实性问题，不涉及方案分歧
- U-01（Orbit 内外部状态分离 API）虽影响 §4.1 建议的"自研 delegate"设计，但已给出备选路径（business-architect 自行设计），不阻塞当前建议产出
- **结论：未命中 §2.1**

**§2.3 反向验证 3 问**：

| 问题 | 答案与证据 |
| --- | --- |
| Q1：调研报告整体产出若 3 个月后推翻，工程返工成本可控吗？ | 返工范围 = research_report.md 整份文档（§1~§7 + 附录）；切换成本约 2~3 人日（重新调研 + 重写）。但调研报告是 business-architect 的上游输入，若 business-architect 已基于此报告完成高层架构设计，推翻调研报告将导致高层架构设计连带返工（返工范围扩大至下游产物）。不过当前阶段 G2 尚未通过，business-architect 未开始消费，**当前可控** |
| Q2：调研报告整体产出，用户/客户/监管能感知到吗？ | 调研报告是内部架构设计文档，用户/客户/监管不直接感知。但调研报告的建议会传导为 business-architect 的重构方案，最终影响用户可感知的重构结果（LearningViewModel 行为不变但内部结构变化）。当前阶段用户感知的是"重构方案待定"，**感知不到调研报告本身** |
| Q3：调研报告整体产出与用户原始诉求中显式提及的能力是否一致？ | 用户诉求核心约束"零逻辑与语义变更、对外接口与调用方兼容、遵循现代 Android 架构（关注点分离、单一职责、UDF）"——调研报告的优先借鉴结论（B1+B2 官方架构 + B3 委托模式）完全契合这些约束。用户诉求要求"出方案前必须对项目有足够了解"——调研报告基于 material_digest.md 的 D1~D10 完整摘要和 X1~X10 冲突记录，覆盖了 LearningViewModel 2443 行的全部 49 条方法摘要。**一致** |

**最终结论**：三项均未命中 §2.2，无需发起中间确认。

---

## 附录 B：调研方法论与工具说明

### 调研流程

| 步骤 | 动作 | 落入章节 |
| --- | --- | --- |
| Step 0 | 读取模板 + agent 定义 + 中间确认协议 + material_digest.md | — |
| Step 1 | 从用户诉求和 material_digest 收敛调研问题 | §1 |
| Step 2 | WebFetch 获取 5 家标杆的 GitHub README / 架构文档 | §2 |
| Step 3 | 建立 5 维度加权对比矩阵，打分 | §3 |
| Step 4 | 基于对比结论给出自研/采购/复用建议 | §4 |
| Step 5 | 识别风险 + 整理待确认项 + 标注下游依赖 | §5 |
| Step 6 | 汇总来源 + 硬指标自检 + 全文定稿 | §6, §7 |
| Step 7 | 四次中间确认自检（§1/§2.1/§3.1/§5.2 后） | 附录 A |

### 工具与来源限制说明

本轮调研使用 WebFetch 工具获取公开资料。遇到的限制：
1. developer.android.com（Android 官方架构/ViewModel 文档）需 Google OAuth 登录，无法直接访问——核心内容通过 B1（architecture-samples）和 B2（NiA）的 README 间接核实，两者均明确引用并遵循该官方指南
2. hannesdorfmann.com（Mosby MVI 博客系列）和 airbnb.io/mavericks（Mavericks 详细文档）因网络问题无法访问——Mosby 未纳入正式标杆（B1-B5 已覆盖），Mavericks 已在评分中否决，不影响最终建议
3. WebSearch 工具本轮持续 fetch failed，改用 WebFetch 直接获取已知 URL 的 GitHub 仓库和文档

所有无法直接访问的来源已在 §6 来源目录和 §5.2 待确认项中如实标注，未伪造任何来源。

---

> **decision**: "调研报告完成，证据链闭合，下游可进入"
> 
> 本报告基于 5 家标杆（B1 Android Architecture Blueprints / B2 Now in Android / B3 Orbit MVI / B4 Mavericks / B5 MVIKotlin）的已核实事实，建立了 5 维度加权对比矩阵（权重和 = 1.00），形成三层评分结论（优先：B1+B2 4.40 / 部分：B3 4.00 / 不借鉴：B4 3.30 + B5 3.15），识别 5 条主要风险（含缓解建议）和 4 项待确认项（含备选路径）。全部来源可追溯至公开 URL。四次中间确认自检均未命中触发标准，无需发起中间确认。调研报告为 business-architect 的建议输入（非裁决），最终业务边界由 business-architect 冻结。
