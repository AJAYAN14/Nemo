# Skills 技能目录

> **版本：** v2.1.0 | **技能总数：** 23 | **最后更新：** 2026-05-12

本目录包含项目中使用的自定义技能模块，用于扩展 AI 助手的功能。

## 技能体系总览

完整的技能体系文档请查看：[SKILL-SYSTEM-OVERVIEW.md](./SKILL-SYSTEM-OVERVIEW.md)

## 技能列表（按工作流程排序）

### 阶段一：探索与需求

#### 1. brainstorm - 头脑风暴技能

**功能说明：** 本技能用于在处理问题、BUG时调用，在进行重大修改、更新、复杂任务时进行头脑风暴，通过系统性地探索代码库、收集各Agent的专业意见，最终整合出多个可行方案供用户选择。

**触发关键词：** 问题、BUG、错误、异常、故障、应该、可以、可能、需要、修改、更新、调整、重构、改进等。

**详细文档：** [brainstorm/SKILL.md](./brainstorm/SKILL.md)

#### 2. requirements-fractal - 分形式需求分析技能

**功能说明：** 本技能采用分形递归思想，从用户的初步想法开始，通过多层级、逐步细化的方式将概念转化为可落地的需求文档。

**分形层级：**
- L0：原始想法
- L1：需求分类
- L2：需求清单
- L3：详细描述
- L4：验收条件（可选）
- L5：测试用例（可选）

**详细文档：** [requirements-fractal/SKILL.md](./requirements-fractal/SKILL.md)

---

### 阶段二：设计与规划

#### 3. fractal-designer - 分形式设计器

**功能说明：** 本技能采用分形递归思想，从用户的初步想法开始，通过多层级、逐步细化的方式将概念转化为可落地的设计方案，生成完善的标准设计文档集。每个技术分叉点、冲突点、需要决策或有疑问的地方都必须询问用户。

**分形层级：**
- L0：初步想法
- L1：需求分析
- L2：方案设计
- L3：详细设计
- L4：实现设计
- L5+：持续细化

**详细文档：** [fractal-designer/SKILL.md](./fractal-designer/SKILL.md)

#### 4. interface-design - 界面设计技能

**功能说明：** 此技能用于构建具有独特风格的高质量前端界面。适用于构建Web组件、页面、工件或应用程序。拒绝通用AI审美，生成具有创造性的代码。

**专注领域：** Dashboard、管理面板、应用工具（非营销页面）

**详细文档：** [interface-design/SKILL.md](./interface-design/SKILL.md)

#### 5. frontend-design - 前端构建技能

**功能说明：** 此技能用于构建具有独特风格的高质量前端界面。将设计方案转化为具有独特美感和卓越用户体验的前端代码。拒绝千篇一律的AI审美。

**详细文档：** [frontend-design/SKILL.md](./frontend-design/SKILL.md)

#### 6. api-design-principles - API设计原则

**功能说明：** 掌握REST和GraphQL API设计原则，构建直观、可扩展和可维护的API。用于设计新API、审查API规范或建立API设计标准。

**支持范式：** REST + GraphQL

**详细文档：** [api-design-principles/SKILL.md](./api-design-principles/SKILL.md)

#### 7. database-schema-designer - 数据库Schema设计

**功能说明：** 为SQL和NoSQL数据库设计健壮、可扩展的数据库模式。提供规范化指南、索引策略、迁移模式、约束设计和性能优化。

**详细文档：** [database-schema-designer/SKILL.md](./database-schema-designer/SKILL.md)

#### 8. graph-theory-fractal - 分形式图论分析技能

**功能说明：** 本技能采用分形递归思想结合图论思想，从系统/模块分析开始，通过多层级、逐步细化的方式识别节点、定义边、分析图结构、提出优化建议。

**分形层级：**
- L0：图主题定义
- L1：节点识别
- L2：边的定义
- L3：图的分析
- L4：优化建议（可选）

**详细文档：** [graph-theory-fractal/SKILL.md](./graph-theory-fractal/SKILL.md)

#### 9. task-scheduler-fractal - 分形式项目任务规划器

**功能说明：** 本技能采用分形思想，通过多层级、逐步细化的方式制定详细、可执行的项目任务计划文档。特别支持A/B组并行开发模式，解决文档过多的问题。

**分形层级：**
- L0：项目目标
- L1：阶段划分
- L2：任务分组
- L3：任务条目
- L4：执行步骤（可选）

**详细文档：** [task-scheduler-fractal/SKILL.md](./task-scheduler-fractal/SKILL.md)

---

### 阶段三：实施与质量

#### 10. fullstack-developer - 全栈开发实施

**功能说明：** 现代Web开发专业知识，涵盖React、Node.js、数据库和全栈架构。用于构建Web应用、开发API、创建前端、设置数据库、部署Web应用。

**技术栈：** React、Next.js、Express、REST API、GraphQL、MongoDB、PostgreSQL、Prisma等

**详细文档：** [fullstack-developer/SKILL.md](./fullstack-developer/SKILL.md)

#### 11. cache-components - Next.js缓存优化

**功能说明：** Next.js缓存组件和部分预渲染（PPR）的专家指导。在Next.js项目中自动激活（检测到cacheComponents: true时）。实现"use cache"指令、配置缓存生命周期、标记缓存数据等。

**详细文档：** [cache-components/SKILL.md](./cache-components/SKILL.md)

#### 12. bug-hunter-fractal - 分形式问题排查技能

**功能说明：** 本技能采用分形递归思想，从问题现象开始，通过多层级、逐步细化的方式定位问题根源。每个决策点、分叉点、需要确认的地方都必须询问用户，每一步排查内容都会保存到文档。

**分形层级：**
- L0：现象描述
- L1：领域判断
- L2：模块定位
- L3：文件定位
- L4：代码定位（可选）
- L5：根因分析（可选）

**详细文档：** [bug-hunter-fractal/SKILL.md](./bug-hunter-fractal/SKILL.md)

#### 13. refactor-fractal - 分形式代码重构技能

**功能说明：** 本技能采用分形递归思想，从重构目标开始，通过多层级、逐步细化的方式制定重构计划并执行。

**分形层级：**
- L0：重构目标
- L1：范围确定
- L2：策略选择
- L3：计划制定
- L4：执行验证（可选）

**详细文档：** [refactor-fractal/SKILL.md](./refactor-fractal/SKILL.md)

#### 14. test-design-fractal - 分形式测试用例设计技能

**功能说明：** 本技能采用分形递归思想，从测试目标开始，通过多层级、逐步细化的方式设计测试用例。每个决策点、分叉点、需要确认的地方都必须询问用户，每一步测试设计内容都会保存到文档。

**分形层级：**
- L0：测试目标
- L1：测试类型
- L2：测试范围
- L3：测试场景
- L4：测试用例

**详细文档：** [test-design-fractal/SKILL.md](./test-design-fractal/SKILL.md)

#### 15. frontend-ui-test - 前端界面操作测试

**功能说明：** 此技能用于进行前端界面操作测试，包括：
- 检测并启动前端后端开发服务器
- 使用浏览器访问页面
- 截取页面截图和获取页面快照
- 点击页面元素
- 对比操作前后的变化
- 循环执行多轮测试并输出测试报告

**使用场景：** 前端代码修改后的验证、后端功能修改后的前端验证、UI界面调整后的效果检查等。

**注意：** 需要使用能添加图片的多模态模型。

**详细文档：** [frontend-ui-test/SKILL.md](./frontend-ui-test/SKILL.md)

#### 16. pr-creator - PR创建技能

**功能说明：** 此技能用于引导创建遵循仓库规范的高质量Pull Request。确保PR遵循模板、使用常规提交格式，并且永不推送到受保护分支。

**详细文档：** [pr-creator/SKILL.md](./pr-creator/SKILL.md)

#### 17. code-reviewer - 代码审查技能

**功能说明：** 此技能用于对GitHub PR和本地变更进行专业且彻底的代码审查。涵盖正确性、可维护性、安全性、性能和项目规范合规性。

**详细文档：** [code-reviewer/SKILL.md](./code-reviewer/SKILL.md)

#### 18. frontend-code-review - 前端专项审查

**功能说明：** 当用户要求审查前端文件（如.tsx、.ts）时触发。支持待变更审查和专注文件审查，同时应用检查清单规则。

**审查重点：** 样式、性能、可访问性、浏览器兼容性

**详细文档：** [frontend-code-review/SKILL.md](./frontend-code-review/SKILL.md)

#### 19. full-review-repair-fractal - 分形式全维度代码审查与修复

**功能说明：** 本技能采用分形递归思想，从宏观到微观逐步深入执行完整的项目代码审查、修复和测试流程。每个审查层级都遵循相同的自相似模式，结合全维度评估要点。

**分形层级：**
- L0：项目级
- L1：领域级
- L2：模块级
- L3：文件级
- L4：代码级（可选）

**详细文档：** [full-review-repair-fractal/SKILL.md](./full-review-repair-fractal/SKILL.md)

---

### 阶段四：知识沉淀与交付

#### 20. knowledge-fractal - 分形式知识体系构建技能

**功能说明：** 本技能采用分形递归思想，从知识主题开始，通过多层级、逐步细化的方式构建知识体系，可用于写帮助文档或总结 achievement。

**分形层级：**
- L0：知识主题
- L1：知识分类
- L2：知识结构
- L3：知识内容
- L4：详细展开（可选）

**详细文档：** [knowledge-fractal/SKILL.md](./knowledge-fractal/SKILL.md)

#### 21. auto-task-experience-summarizer - 自动任务经验总结器

**功能说明：** 本技能用于自动读取与总结任务执行经验，包括：
- 任务执行过程中的关键步骤
- 遇到的问题及解决方案
- 优化建议
- 任务流程记录
- 任务类型分类
- 经验类别标注

**触发时机：** 任务执行开始前、任务执行完成后、需要参考或记录任务经验时。

**详细文档：** [auto-task-experience-summarizer/SKILL.md](./auto-task-experience-summarizer/SKILL.md)

---

### 元层级（技能体系维护）

#### 22. skill-development - 技能开发

**功能说明：** 此技能用于为TRAE Code插件创建有效的技能。用于创建技能、向插件添加技能、编写新技能、改进技能描述、组织技能内容或需要技能结构、渐进式披露或技能开发最佳实践指导。

**详细文档：** [skill-development/SKILL.md](./skill-development/SKILL.md)

#### 23. chat-to-skill - 聊天转技能

**功能说明：** 此技能用于把单个聊天文件或聊天目录提炼为完整技能包，包括：
- 先对原始聊天做严格脱敏
- 抽取稳定流程、决策点和验证动作
- 生成中间 outline
- 组装 `SKILL.md`、`references/`、`examples/`、`scripts/`
- 更新技能体系文档

**使用场景：** 需要把历史聊天记录沉淀为可复用技能、需要从多轮对话抽取方法论、需要在不暴露项目身份信息的前提下生成技能资产时。

**详细文档：** [chat-to-skill/SKILL.md](./chat-to-skill/SKILL.md)

---

### 其他技能

#### 24. full-review-repair - 全维度代码审查与修复

**功能说明：** 本技能用于执行完整的项目代码审查、修复和测试流程，确保代码质量和功能完整性。

**工作流程：**
```
后端审查（包括数据库）→ 修复 → 前端审查（包括UI）→ 修复 → 前端测试
```

**评估范围：**
- 后端：接口规范、业务逻辑、异常处理、安全防护
- 数据库：SQL 规范、索引优化、事务处理、数据一致性
- 前端：代码规范、渲染性能、交互逻辑、UI 还原度
- UI：布局合理性、视觉效果、用户体验、响应式适配

**使用场景：** 项目阶段性审查、新功能开发完成后、修复bug后、用户要求进行全面代码审查和测试时。

**详细文档：** [full-review-repair/SKILL.md](./full-review-repair/SKILL.md)

#### 25. skill-creator - 技能创建器

**功能说明：** 用于创建新的技能模块。当用户想要创建或添加任何技能时，必须调用此技能。

**使用场景：** 创建新技能时。

**详细文档：** [skill-development/references/skill-creator-original.md](./skill-development/references/skill-creator-original.md)

## 目录结构

```
skills/
├── brainstorm/
│   └── SKILL.md
├── requirements-fractal/
│   └── SKILL.md
├── fractal-designer/
│   └── SKILL.md
├── interface-design/
│   └── SKILL.md
├── frontend-design/
│   └── SKILL.md
├── api-design-principles/
│   └── SKILL.md
├── database-schema-designer/
│   └── SKILL.md
├── graph-theory-fractal/
│   └── SKILL.md
├── task-scheduler-fractal/
│   └── SKILL.md
├── fullstack-developer/
│   └── SKILL.md
├── cache-components/
│   └── SKILL.md
├── bug-hunter-fractal/
│   └── SKILL.md
├── refactor-fractal/
│   └── SKILL.md
├── test-design-fractal/
│   └── SKILL.md
├── frontend-ui-test/
│   └── SKILL.md
├── pr-creator/
│   └── SKILL.md
├── code-reviewer/
│   └── SKILL.md
├── frontend-code-review/
│   └── SKILL.md
├── full-review-repair-fractal/
│   └── SKILL.md
├── knowledge-fractal/
│   └── SKILL.md
├── auto-task-experience-summarizer/
│   └── SKILL.md
├── skill-development/
│   └── SKILL.md
├── chat-to-skill/
│   ├── SKILL.md
│   ├── references/
│   ├── examples/
│   └── scripts/
├── full-review-repair/
│   └── SKILL.md
├── SKILL-SYSTEM-OVERVIEW.md
├── img_v3_02102_25f5153c-0b12-46e8-9a85-d7b312d1a65g.jpg
├── img_v3_02102_3b7d3893-7b0a-4fca-ba88-27d3882c075g.jpg
├── img_v3_02102_d7e4a52d-e59d-41f9-84a5-989f657b8f7g.jpg
├── img_v3_02vv_78e65a44-baa5-4a9b-9624-f275a4c5ebeg.jpg
├── img_v3_02vv_a7eb49fb-934a-4b36-ab04-e8c0db32881g.jpg
└── README.md (本文件)
```

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

## 使用说明

每个技能模块都包含一个 `SKILL.md` 文件，其中定义了：
- 技能的名称和描述
- 功能说明和使用步骤
- 适用场景
- 工具要求

在项目中使用技能时，AI 助手会根据用户的请求自动选择并调用相应的技能模块。

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
   ├── [横向拆分] 方向B（并行） ← 横向拆分（广度）
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

## 最近更新

- 添加 interface-design 界面设计技能
- 添加 frontend-design 前端构建技能
- 添加 api-design-principles API设计原则
- 添加 database-schema-designer 数据库Schema设计
- 添加 fullstack-developer 全栈开发实施
- 添加 cache-components Next.js缓存优化
- 添加 pr-creator PR创建技能
- 添加 code-reviewer 代码审查技能
- 添加 frontend-code-review 前端专项审查
- 添加 skill-development 技能开发元技能
- 添加 chat-to-skill 聊天转技能
- 技能总数达到23个（v2.1.0版本）
- 完善所有分形式技能的横向拆分机制
- 统一技能文档格式和结构
- 添加技能分类说明
- 添加核心设计思想说明
