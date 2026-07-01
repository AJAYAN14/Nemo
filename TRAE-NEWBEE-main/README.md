# TRAE-NEWBEE 项目

> **版本**: v2.1.0 | **技能总数**: 23 | **最后更新**: 2026-05-12

这个项目是一个日常经验集合，包含了常用的技能、智能体和其他工具，用于扩展 AI 助手的功能。

## 项目结构

```
TRAE-NEWBEE/
├── skills/              # 技能模块目录
│   ├── brainstorm/                     # 头脑风暴技能
│   ├── requirements-fractal/           # 分形式需求分析技能
│   ├── fractal-designer/               # 分形式设计器
│   ├── interface-design/               # 界面设计技能
│   ├── frontend-design/                # 前端构建技能
│   ├── api-design-principles/          # API设计原则
│   ├── database-schema-designer/       # 数据库Schema设计
│   ├── graph-theory-fractal/           # 分形式图论分析技能
│   ├── task-scheduler-fractal/         # 分形式项目任务规划器
│   ├── fullstack-developer/            # 全栈开发实施
│   ├── cache-components/               # Next.js缓存优化
│   ├── bug-hunter-fractal/             # 分形式问题排查技能
│   ├── refactor-fractal/               # 分形式代码重构技能
│   ├── test-design-fractal/            # 分形式测试用例设计技能
│   ├── frontend-ui-test/               # 前端界面操作测试
│   ├── pr-creator/                     # PR创建技能
│   ├── code-reviewer/                  # 代码审查技能
│   ├── frontend-code-review/           # 前端专项审查
│   ├── full-review-repair-fractal/     # 分形式全维度代码审查与修复
│   ├── knowledge-fractal/              # 分形式知识体系构建技能
│   ├── auto-task-experience-summarizer/ # 自动任务经验总结器
│   ├── skill-development/              # 技能开发（元技能）
│   ├── chat-to-skill/                  # 聊天转技能（元技能）
│   ├── full-review-repair/             # 全维度代码审查与修复
│   ├── SKILL-SYSTEM-OVERVIEW.md        # 技能体系总览
│   └── README.md                       # 技能模块文档
├── docs/                # 文档目录
└── README.md            # 项目根文档
```

## 技能体系总览

项目当前包含 **23个技能**，按照工作流程分为四个阶段和元层级：

### 阶段一：探索与需求
- **brainstorm** - 头脑风暴
- **requirements-fractal** - 分形式需求分析

### 阶段二：设计与规划
- **fractal-designer** - 分形式设计器 v2.0
- **interface-design** - 界面设计
- **frontend-design** - 前端构建
- **api-design-principles** - API设计原则
- **database-schema-designer** - 数据库Schema设计
- **graph-theory-fractal** - 分形式图论分析
- **task-scheduler-fractal** - 分形式项目任务规划器

### 阶段三：实施与质量
- **fullstack-developer** - 全栈开发实施
- **cache-components** - Next.js缓存优化
- **bug-hunter-fractal** - 分形式问题排查
- **refactor-fractal** - 分形式代码重构
- **test-design-fractal** - 分形式测试用例设计
- **frontend-ui-test** - 前端UI测试
- **pr-creator** - PR创建
- **code-reviewer** - 代码审查
- **frontend-code-review** - 前端专项审查
- **full-review-repair-fractal** - 分形式全维度审查修复

### 阶段四：知识沉淀与交付
- **knowledge-fractal** - 分形式知识体系构建
- **auto-task-experience-summarizer** - 自动经验总结器

### 元层级（技能体系维护）
- **skill-development** - 技能开发
- **chat-to-skill** - 聊天转技能

详细的技能体系说明请查看 [skills/SKILL-SYSTEM-OVERVIEW.md](./skills/SKILL-SYSTEM-OVERVIEW.md)，技能模块的详细文档请查看 [skills/README.md](./skills/README.md)。

## 技能分类

### 按功能分类（23个技能）

| 分类 | 技能名称 | 说明 |
|------|---------|------|
| **分形核心类** (8) | requirements-fractal, fractal-designer, task-scheduler-fractal, bug-hunter-fractal, refactor-fractal, test-design-fractal, graph-theory-fractal, knowledge-fractal | 分形递归、L0-L4层级、自相似模式、AskUserQuestion决策 |
| **设计与构建类** (4) | interface-design, frontend-design, api-design-principles, database-schema-designer | 领域专家型、设计系统驱动、最佳实践指导 |
| **执行与交付类** (4) | fullstack-developer, pr-creator, code-reviewer, frontend-code-review | 核心执行力、代码产出、质量把关 |
| **工具执行类** (2) | frontend-ui-test, cache-components | 具体操作执行、自动化验证 |
| **协作支撑类** (2) | brainstorm, full-review-repair-fractal | 多Agent协作、方案整合、系统化流程 |
| **自动化类** (1) | auto-task-experience-summarizer | 全程自动触发、经验积累、跨任务复用 |
| **元技能类** (2) | skill-development, chat-to-skill | 创建、维护或生成技能资产 |

### 按复杂度分类

| 复杂度 | 技能名称 | 说明 |
|--------|---------|------|
| **简单** | frontend-ui-test, auto-task-experience-summarizer | 单一功能，自动化流程 |
| **中等** | chat-to-skill, full-review-repair, brainstorm, interface-design, frontend-design, api-design-principles, database-schema-designer, cache-components, pr-creator, code-reviewer, frontend-code-review, fullstack-developer, skill-development | 多阶段流程或专项领域技能 |
| **复杂** | bug-hunter-fractal, fractal-designer, full-review-repair-fractal, graph-theory-fractal, knowledge-fractal, refactor-fractal, requirements-fractal, task-scheduler-fractal, test-design-fractal | 分形递归，多层级决策流程 |

## 核心设计思想

### 分形递归思想

本项目中多个技能采用**分形递归思想**作为核心设计理念：

- **纵向细分**：从宏观到微观，逐层深入（L0→L1→L2→L3→L4）
- **横向拆分**：同一层级内拆分为多个并行任务，提高效率
- **自相似模式**：每个层级都遵循相同的工作模式，降低认知负担
- **用户决策**：每个决策点都询问用户，确保方向正确

### 双维度分形模型

```
纵向细分（深度）
    ↓
L0 宏观级
   ├── [横向拆分] 方向A（并行）
   ├── [横向拆分] 方向B（并行）  ← 横向拆分（广度）
   └── [横向拆分] 方向C（并行）
   ↓
L1 中观级
   ├── [横向拆分] 子方向A（并行）
   └── [横向拆分] 子方向B（并行）
   ↓
L2 微观级
   ↓
L3 原子级
   ↓
L4 可选深度
```

## 完整工作流示例

```
1. [auto-task-experience-summarizer] ← 读取相关历史经验

2. [brainstorm]
   输入："我们需要实现一个XXX功能"
   输出：3套技术方案（快速/平衡/长远）

3. [requirements-fractal]  ← 消费 brainstorm 输出
   输出：SRS + RTM + 用户故事

4. [fractal-designer]  ← 消费 requirements-fractal 输出
   ├─ [graph-theory-fractal]  ← 并行：架构分析辅助设计
   ├─ [api-design-principles] ← 并行：API接口设计
   └─ [database-schema-designer] ← 并行：数据模型设计
   输出：标准设计文档集（6大类文档）

5. [interface-design]  ← 消费 fractal-designer UI/UX设计
   输出：界面设计方案 + Design Tokens

6. [task-scheduler-fractal]  ← 消费 fractal-designer 输出
   输出：可执行任务计划（含DoD）

7. [fullstack-developer]  ← 消费任务计划
   ├─ [frontend-design]  ← 界面实现（消费 Design Tokens）
   │   └─ [cache-components]  ← Next.js项目自动介入缓存优化
   ├─ [frontend-ui-test]  ← 每次前端修改后自动验证
   ├─ [bug-hunter-fractal]  ← 发现问题时调用
   └─ [refactor-fractal]  ← 需要重构时调用

8. [test-design-fractal]  ← 消费 SRS + 设计文档
   输出：测试用例集

9. [pr-creator]  ← 开发完成
   输出：Pull Request

10. [code-reviewer]  ← PR创建后自动触发
    └─ [frontend-code-review]  ← 并行：前端专项审查
    输出：审查报告

11. [full-review-repair-fractal]  ← 消费所有测试和审查结果
    输出：审查修复报告

12. [knowledge-fractal]  ← 消费以上所有产出
    输出：项目知识体系文档

13. [auto-task-experience-summarizer]  ← 自动触发
    输出：本次任务经验总结
```

## 最近更新

- 添加 chat-to-skill 聊天转技能
- 添加 interface-design 界面设计
- 添加 frontend-design 前端构建
- 添加 api-design-principles API设计原则
- 添加 database-schema-designer 数据库Schema设计
- 添加 fullstack-developer 全栈开发实施
- 添加 cache-components Next.js缓存优化
- 添加 pr-creator PR创建创建
- 添加 code-reviewer 代码审查技能
- 添加 frontend-code-review 前端专项审查
- 添加 skill-development 技能开发元技能
- 技能总数达到达到23个（v2.1.0版本）
- 完善所有分形式技能的横向拆分机制
- 统一技能文档格式和结构
- 添加技能分类说明
- 添加核心设计思想说明

## 环境说明

- 我使用的是国服，国际服需要按需调整
- 技能模块的详细文档在 `skills/README.md`
- 技能体系体系总览在 `skills/SKILL-SYSTEM-OVERVIEW.md`
