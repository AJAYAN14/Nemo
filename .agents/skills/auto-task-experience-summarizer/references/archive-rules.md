# 经验文件归档规则详解

## 触发条件

**硬性规则**：活跃经验文件超过 **5份** 时必须整理归档。

### 如何判断"活跃经验文件"

| 文件类型 | 是否算活跃 | 说明 |
|----------|-----------|------|
| `exp-YYYYMMDD-*.md` | ✅ 是 | 按日期命名的单次经验文件 |
| `exp-*.md`（主题总结） | ✅ 是 | 主题总结文档（≤5份） |
| `archive/exp-*.md` | ❌ 否 | 已归档文件 |

## 归档流程（6步）

### 第1步：扫描活跃文件

扫描 `docs/experience/` 目录，找出所有非归档的活跃文件：

```powershell
# 示例扫描结果
docs/experience/
├── index.md
├── exp-20260501-log-analysis.md           # 活跃
├── exp-20260502-config-fix.md             # 活跃
├── exp-20260502-ui-audit.md               # 活跃
├── exp-20260503-ui-deep-review.md         # 活跃
├── exp-20260503-dataflow-audit.md         # 活跃
├── exp-20260503-p0-p4-verification.md     # 活跃（第6份，触发归档）
├── exp-backend-architecture.md            # 活跃主题
└── archive/                               # 归档目录
```

### 第2步：按主题关联性分组

**分组原则**：相似主题优先合并

| 主题 | 相关文件 |
|------|----------|
| **配置管理** | exp-20260501-log-analysis.md、exp-20260502-config-fix.md |
| **UI系统** | exp-20260502-ui-audit.md、exp-20260503-ui-deep-review.md |
| **数据流** | exp-20260503-dataflow-audit.md、exp-20260503-p0-p4-verification.md |

### 第3步：合并为主题总结文档

将相似主题的经验文件合并为一份主题总结文档，命名格式：
`exp-{主题名}.md`

**合并要求**：
- 保留四列表格格式
- 去重重复内容
- 提炼核心要点
- 保留所有相关文件的引用

**示例合并结果**：
```
docs/experience/exp-config-management.md  # 合并配置相关
docs/experience/exp-ui-system.md           # 合并UI相关
docs/experience/exp-data-flow.md           # 合并数据流相关
```

### 第4步：控制活跃主题总数 ≤ 5份

检查合并后的主题总结文档总数：

| 当前主题 | 状态 |
|----------|------|
| exp-backend-architecture.md | 保持 |
| exp-frontend-development.md | 保持 |
| exp-agent-system.md | 保持 |
| exp-config-management.md | ✅ 新增（合并结果） |
| exp-ui-system.md | ✅ 新增（合并结果） |
| exp-data-flow.md | ⚠️ 超过5份，需要进一步合并 |

**处理方式**：如果超过5份，将最相近的两个主题再次合并。

### 第5步：移动原始文件到 archive/

将被合并的原始经验文件移动到 `docs/experience/archive/` 目录：

```
docs/experience/archive/
├── exp-20260501-log-analysis.md
├── exp-20260502-config-fix.md
├── exp-20260502-ui-audit.md
├── exp-20260503-ui-deep-review.md
├── exp-20260503-dataflow-audit.md
└── exp-20260503-p0-p4-verification.md
```

### 第6步：更新 index.md

在 `docs/experience/index.md` 中：
1. 添加新的主题总结文档条目
2. 标记已归档的原始文件（在 archive/ 目录下）
3. 更新统计数据

## 归档目录结构建议

```
docs/experience/
├── index.md                                    # 总索引
├── exp-backend-architecture.md                # 活跃主题1
├── exp-frontend-development.md               # 活跃主题2
├── exp-agent-system.md                        # 活跃主题3
├── exp-config-management.md                   # 活跃主题4
├── exp-ui-system.md                           # 活跃主题5
└── archive/                                    # 归档目录
    ├── 2026-04/                               # 按月份归档
    │   ├── exp-20260401-xxx.md
    │   └── exp-20260415-xxx.md
    └── 2026-05/
        ├── exp-20260501-xxx.md
        └── exp-20260512-xxx.md
```

**可选优化**：按月份子目录组织归档文件。

## 合并策略示例

### 示例场景

假设活跃文件：
1. exp-20260501-agent-performance.md
2. exp-20260502-agent-refactor.md
3. exp-20260503-frontend-bugfix.md
4. exp-20260504-frontend-optimization.md
5. exp-20260505-backend-api.md
6. exp-20260506-backend-database.md （第6份，触发归档）

### 分组结果

| 主题 | 文件 |
|------|------|
| **Agent系统** | exp-20260501-agent-performance.md、exp-20260502-agent-refactor.md |
| **前端开发** | exp-20260503-frontend-bugfix.md、exp-20260504-frontend-optimization.md |
| **后端开发** | exp-20260505-backend-api.md、exp-20260506-backend-database.md |

### 合并后

活跃主题（3份，≤5）：
- exp-agent-system.md（合并2份）
- exp-frontend-development.md（合并2份）
- exp-backend-architecture.md（合并2份）

归档：6份原始文件移动到 archive/

## index.md 更新示例

```markdown
## 主题经验索引

### 活跃主题总结（3份）
| 主题 | 文档 | 整合来源数 | 核心内容 |
|------|------|-----------|----------|
| Agent系统 | [exp-agent-system.md](exp-agent-system.md) | 2 | Agent性能优化、重构经验 |
| 前端开发 | [exp-frontend-development.md](exp-frontend-development.md) | 2 | 前端bug修复、性能优化 |
| 后端架构 | [exp-backend-architecture.md](exp-backend-architecture.md) | 2 | API设计、数据库优化 |

### 已归档文件
- [exp-20260501-agent-performance.md](archive/exp-20260501-agent-performance.md)
- [exp-20260502-agent-refactor.md](archive/exp-20260502-agent-refactor.md)
- ...（其余归档文件）
```

## 判断何时需要 AskUserQuestion

如果遇到以下情况，使用 AskUserQuestion 询问用户：

1. **主题分组不明确**：某个经验文件不知道归到哪个主题
2. **是否合并不确定**：两个主题是否应该合并
3. **归档方式选择**：是否按月份子目录组织归档
4. **主题命名**：新的主题总结文档该用什么名字

**示例问题**：
```
发现6份活跃经验文件，需要归档。我建议按以下方式分组：
- Agent系统：exp-20260501、exp-20260502
- 前端开发：exp-20260503、exp-20260504
- 后端开发：exp-20260505、exp-20260506

是否同意？或者有其他分组建议？
```
