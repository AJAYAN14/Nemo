---
name: brainstorm
description: 当用户描述功能异常、错误行为或提出疑问时，当需要进行架构调整、大规模重构或复杂功能开发时调用本技能。通过探索代码库，各Agent提出方案，主Agent整合方案供用户选择。及时上网搜索学习新技术和方法。触发关键词：问题、BUG、错误、异常、故障、应该、可以、可能、需要、修改、更新、调整、重构、改进、技术选型、方案对比、架构设计、功能开发、系统优化；使用范围：用户提出问题或报告BUG时、重大修改或系统更新时、复杂功能开发时、技术选型决策时、架构调整或重构时、需要多方案对比时。
version: 2.0.0
---

# 头脑风暴技能 v2.0

## 技能执行流程图

```mermaid
flowchart TB
    START(["开始"]) --> TIME["记录当前时间"]
    TIME --> GOAL["1. 明确任务目标<br/>范围+量化标准"]

    GOAL --> EXPLORE["2. 按功能区域探索代码库"]
    EXPLORE --> SA1["Search Agent: 后端架构"]
    EXPLORE --> SA2["Search Agent: 前端架构"]
    EXPLORE --> SA3["Search Agent: 数据库"]
    EXPLORE --> SA4["Search Agent: 配置与工具"]

    SA1 --> COLLECT["3. 收集探索报告汇总"]
    SA2 --> COLLECT
    SA3 --> COLLECT
    SA4 --> COLLECT

    COLLECT --> WEB{"涉及新技术?"}
    WEB -->|是| SEARCH["WebSearch搜索<br/>最新技术方案"]
    SEARCH --> AGENTS
    WEB -->|否| AGENTS["4. 专业Agent阅读报告<br/>提出各3个方案"]

    AGENTS --> INTEGRATE["5. 主Agent整合方案<br/>多维度评估(0-10分)"]
    INTEGRATE --> SHOW["6. 向用户展示方案<br/>AskUserQuestion选择"]
    SHOW --> RECORD["7. 记录用户选择<br/>含时间戳"]
    RECORD --> OUTPUT(["输出选定方案"])

    style START fill:#4CAF50
    style OUTPUT fill:#2196F3
    style SEARCH fill:#9C27B0
```

```mermaid
sequenceDiagram
    participant User
    participant Main as 主Agent
    participant SA as Search Agent(仅搜索)
    participant Expert as 专业Agent
    participant Web as WebSearch

    User->>Main: 描述问题/需求
    Main->>Main: 记录时间戳 + 明确目标

    par 并行探索
        Main->>SA: 后端架构探索
        Main->>SA: 前端架构探索
        Main->>SA: 数据库探索
        Main->>SA: 配置工具探索
    end
    SA-->>Main: 探索报告（只读不修改）

    alt 涉及技术选型
        Main->>Web: 搜索最新技术方案
        Web-->>Main: 技术调研结果
    end

    Main->>Expert: 发送完整探索报告
    Expert-->>Main: 3个方案(快速/平衡/长远)
    Main->>Main: 整合评估 + 推荐指数
    Main->>User: AskUserQuestion 展示方案
    User->>Main: 选择方案
    Main->>Main: 记录决策 + 时间戳
```

## 技能概述

通过系统性探索代码库、收集专业意见，整合出多个可行方案供用户选择。

- **多Agent协作**：Search Agent 探索 + 专业Agent提方案 + 主Agent整合
- **三方案制**：每个专业Agent提出快速解决/均衡/长远计划 三种方案
- **及时搜索**：涉及技术选型时使用 WebSearch 学习最新方法
- **客观中立**：所有方案同等权重评估，基于推荐指数(0-10)辅助决策

## 核心工作流程

### 1. 启动与目标定义
- 记录**当前时间**作为时间戳
- 与用户结构化沟通，明确任务目标、范围和验收标准
- 建立量化的成功标准

### 2. 代码库探索（Search Agent）

按功能区域并行调用 Search Agent 执行**纯检索**任务：

| 区域 | 覆盖内容 |
|------|----------|
| 后端架构 | 路由、服务层、数据访问、中间件、API |
| 前端架构 | 组件、页面、路由、状态管理、构建 |
| 数据库 | 表结构、索引、迁移、查询 |
| 配置工具 | 环境配置、依赖管理、CI/CD |

**Search Agent 限制规则**：
- ⚠️ **Search Agent 只负责搜索和读取文件，没有写文件权限**
- ⚠️ **不要用 Search Agent 做修改文档、分析总结等需要写操作的任务**
- Search Agent 返回：核心概述、关键文件列表、架构关系、任务关联分析

### 3. 报告汇总与技术搜索
- 整合所有探索报告为综合报告
- 如涉及**新技术/新方法**，使用 `WebSearch` 搜索最新方案
- 将搜索结果匹配到具体需求场景

### 4. 专业Agent方案生成
- 选择相关专业Agent，发送完整探索报告
- 每个Agent提出 **3个方案**（快速/均衡/长远）
- 每个方案包含：实现步骤、技术选型、风险评估、工作量预估
- **约束**：方案阶段禁止修改任何代码

### 5. 方案整合与展示
- 多维度评估：可行性、创新性、风险、契合度、成本效益
- 给出 **0-10分推荐指数**
- 使用 `AskUserQuestion` 向用户展示并等待选择

### 6. 决策记录
- 记录用户最终选择及理由
- 包含**时间戳**和方案追溯ID
- 确认是否需要调整或组合方案

## 关键规则

- **Search Agent 只用于搜索**：无写文件权限，不做文档修改/分析总结
- **每个决策点必须**使用 AskUserQuestion 让用户选择
- 涉及技术时**必须**使用 WebSearch 搜索最新方法
- **每次操作记录时间戳**
- 保持客观中立，不偏向任何方案
- 完成的工作写到 `docs/achievement/achievement-{日期}.md`
- 未完成的工作写到 `docs/todo.md`

---

## 参考资源

### Reference Files

详细工作流程请查阅：

- **`references/workflow-details.md`** — 功能区域划分标准、Search Agent报告格式、专业Agent方案模板、方案整合评估维度、子Agent身份确认协议

---

## 注意事项

- 不要删除代码，而是注释掉
- **Search Agent 仅限搜索操作**，绝不分配文档修改或深度分析任务给它
- 给予用户充分选择权，不预设答案
- 如果遇到分叉点或决策点，**必须**使用 AskUserQuestion 工具询问用户

---

## 技能协作接口

### 在技能体系中的定位

```
[用户问题/想法] → [brainstorm] → [requirements-fractal / fractal-designer / refactor-fractal]
```

**本角色**：项目/功能的探索入口，通过多Agent协作生成多套方案供用户选择。

### 下游输出

| 输出内容 | 消费者 | 消费方式 |
|----------|--------|----------|
| 选定方案 + 推荐理由 | requirements-fractal | 作为 L0 需求目标的基础 |
| 代码库探索报告 | fractal-designer | 辅助技术选型和架构决策 |
| 重构方案集 | refactor-fractal | 直接作为重构目标输入 |
