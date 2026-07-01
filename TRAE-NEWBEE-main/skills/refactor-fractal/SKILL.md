---
name: refactor-fractal
description: 分形式代码重构技能，通过递归细化的方式从目标到具体操作层层推进。及时上网搜索学习新技术和方法。触发关键词：重构、代码重构、分形重构、重构优化、技术债务；使用范围：代码需要重构时、技术债务需要清理时、架构需要优化时。
version: 2.0.0
---

# Refactor Fractal v2.0 - 分形式代码重构技能

## 技能执行流程图

```mermaid
flowchart TB
    START(["开始"]) --> TIME["记录当前时间"]
    TIME --> L0["L0 重构目标<br/>为什么重构/目标效果"]

    L0 --> DEC0{"决策点?"}
    DEC0 -->|有| ASK0["AskUserQuestion<br/>动机/目标/约束"]
    ASK0 --> REC0["记录决策+时间戳"]
    REC0 --> SPLIT0
    DEC0 -->|无| SPLIT0["推荐L1模块拆分"]

    SPLIT0 --> L1["L1 功能模块级<br/>多子Agent并行"]
    L1 --> MOD["模块A/B/C..."]
    MOD --> L2["L2 子模块级<br/>并行"]
    L2 --> L3["L3 文件级<br/>并行"]
    L3 --> L4{"需要代码级?"}
    L4 -->|是| L4_EXEC["L4 代码级"]
    L4 -->|否| FINAL
    L4_EXEC --> FINAL["总结+四重验证"]

    FINAL --> WEB{"需搜索方案?"}
    WEB -->|是| SEARCH["WebSearch<br/>最新重构方法"]
    SEARCH --> SUB
    WEB -->|否| SUB["子Agent独立验证"]
    SUB --> PASS{"通过?"}
    PASS -->|是| OUTPUT(["输出重构计划"])
    PASS -->|否| FIX["修复"] --> EXTRA["额外验证"] --> SUB

    OUTPUT --> EXP["auto-task-experience-summarizer"]

    style START fill:#4CAF50
    style OUTPUT fill:#2196F3
    style SEARCH fill:#9C27B0
    style SUB fill:#F44336
```

## 技能概述

采用**分形递归** + **横向拆分**，从重构目标开始逐层制定并执行重构计划。

- **纵向**：L0(目标) → L1(模块) → L2(子模块) → L3(文件) → L4(代码)
- **横向**：同级多Agent并行分析各模块
- **安全原则**：不删除代码，只注释掉旧代码
- **及时搜索**：涉及重构策略时 WebSearch 查找最佳实践

## 核心工作流程

### 1. 启动
- 记录**当前时间**
- 创建总文档：`docs/refactor/refactor-fractal-{YYYYMMDD}.md`
- 收集重构动机、目标和约束条件

### 2. 逐层递归（自相似模式）

```
层级N分析 → 决策点 → AskUserQuestion → 记录(含时间戳)
→ 推荐横向拆分 → 确认 → 保存文档 → 判断是否深入下一层
```

### 3. 技术搜索

涉及以下情况时使用 `WebSearch`：
- 大型架构重构（如单体→微服务）
- 不熟悉的重构模式或反模式
- 需要最新的工具链或自动化方案

### 4. 四重验证 + 子Agent独立执行

正向（重构→文档）、反向（文档→重构）、正确性、一致性验证。
修复后**必须进行额外一轮验证**。

## 关键规则

- **严格按层级推进**，不得跳级
- **每个决策点必须**使用 AskUserQuestion
- 涉及重构策略时**必须**使用 WebSearch
- **每次操作记录时间戳**
- **Search Agent 只用于搜索**：无写文件权限，不做文档修改/分析
- **不要删除代码，而是注释掉**
- 完成的工作写到 `docs/achievement/achievement-{日期}.md`

---

## 参考资源

### Reference Files

- **`references/refactor-details.md`** — 文档模板结构、各层级详细内容、重构策略与搜索方向

---

## 注意事项

- **Search Agent 仅限搜索操作**，绝不分配文档修改任务
- 给予用户充分选择权，不预设答案
- 同级任务并行执行提高效率
- 如果遇到分叉点或决策点，**必须**使用 AskUserQuestion 工具询问用户

---

## 技能协作接口

```
[brainstorm / bug-hunter-fractal] → [refactor-fractal] → [开发实施 / full-review-repair-fractal]
                                      ↑
                              [graph-theory-fractal]
```

**本角色**：系统性规划代码重构，将根因/方案转化为可执行的重构计划。

### 上游输入 | 下游输出

| 上游 | 输入 | 下游 | 输出 |
|------|------|------|------|
| brainstorm | 三套重构方案 | 开发实施 | 重构计划文档 |
| bug-hunter-fractal | 根因报告 | full-review-repair-fractal | 影响范围 |
| graph-theory-fractal | 依赖关系图 | | |
