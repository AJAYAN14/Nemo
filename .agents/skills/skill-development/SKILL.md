---
name: Skill Development
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
version: 2.0.0
---

## 技能执行流程图

```mermaid
flowchart TB
    START(["触发"]) --> TIME["记录当前时间"]
    TIME --> MODE{"操作模式?"}

    MODE -->|创建新技能| CREATE["Step 1-8 流程<br/>理解→规划→结构→编辑→验证"]
    MODE -->|改进现有| IMPROVE["分析反馈→优化description<br/>更新协作接口"]
    MODE -->|批量维护| BATCH["统一格式/更新关系<br/>补充缺失对接点"]

    CREATE --> WRITE["写入SKILL.md + references/"]
    IMPROVE --> WRITE
    BATCH --> WRITE

    WRITE --> UPDATE["更新 SKILL-SYSTEM-OVERVIEW.md"]
    UPDATE --> VERIFY{"验证通过?"}
    VERIFY -->|是| OUTPUT(["输出: 技能文件"])
    VERIFY →|否| FIX["修复问题"] --> VERIFY

    style START fill:#fce4ec
    style OUTPUT fill:#4CAF50
```

# Skill Development for TRAE Code Plugins

This skill provides guidance for creating effective skills for TRAE Code plugins.

## About Skills

Skills are modular, self-contained packages that extend TRAE's capabilities by providing
specialized knowledge, workflows, and tools. Think of them as "onboarding guides" for specific
domains or tasks—they transform TRAE from a general-purpose agent into a specialized agent
equipped with procedural knowledge that no model can fully possess.

### What Skills Provide

1. Specialized workflows - Multi-step procedures for specific domains
2. Tool integrations - Instructions for working with specific file formats or APIs
3. Domain expertise - Company-specific knowledge, schemas, business logic
4. Bundled resources - Scripts, references, and assets for complex and repetitive tasks

### Anatomy of a Skill

Every skill consists of a required SKILL.md file and optional bundled resources:

```
skill-name/
├── SKILL.md (required)
│   ├── YAML frontmatter metadata (required)
│   │   ├── name: (required)
│   │   └── description: (required)
│   └── Markdown instructions (required)
└── Bundled Resources (optional)
    ├── scripts/          - Executable code (Python/Bash/etc.)
    ├── references/       - Documentation intended to be loaded into context as needed
    └── assets/           - Files used in output (templates, icons, fonts, etc.)
```

#### SKILL.md (required)

**Metadata Quality:** The `name` and `description` in YAML frontmatter determine when TRAE will use the skill. Be specific about what the skill does and when to use it. Use the third-person (e.g. "This skill should be used when..." instead of "Use this skill when...").

#### Bundled Resources (optional)

##### Scripts (`scripts/`)

Executable code (Python/Bash/etc.) for tasks that require deterministic reliability or are repeatedly rewritten.

- **When to include**: When the same code is being rewritten repeatedly or deterministic reliability is needed
- **Example**: `scripts/rotate_pdf.py` for PDF rotation tasks
- **Benefits**: Token efficient, deterministic, may be executed without loading into context
- **Note**: Scripts may still need to be read by TRAE for patching or environment-specific adjustments

##### References (`references/`)

Documentation and reference material intended to be loaded as needed into context to inform TRAE's process and thinking.

- **When to include**: For documentation that TRAE should reference while working
- **Examples**: `references/finance.md` for financial schemas, `references/mnda.md` for company NDA template, `references/policies.md` for company policies, `references/api_docs.md` for API specifications
- **Use cases**: Database schemas, API documentation, domain knowledge, company policies, detailed workflow guides
- **Benefits**: Keeps SKILL.md lean, loaded only when TRAE determines it's needed
- **Best practice**: If files are large (>10k words), include grep search patterns in SKILL.md
- **Avoid duplication**: Information should live in either SKILL.md or references files, not both. Prefer references files for detailed information unless it's truly core to the skill—this keeps SKILL.md lean while making information discoverable without hogging the context window. Keep only essential procedural instructions and workflow guidance in SKILL.md; move detailed reference material, schemas, and examples to references files.

##### Assets (`assets/`)

Files not intended to be loaded into context, but rather used within the output TRAE produces.

- **When to include**: When the skill needs files that will be used in the final output
- **Examples**: `assets/logo.png` for brand assets, `assets/slides.pptx` for PowerPoint templates, `assets/frontend-template/` for HTML/React boilerplate, `assets/font.ttf` for typography
- **Use cases**: Templates, images, icons, boilerplate code, fonts, sample documents that get copied or modified
- **Benefits**: Separates output resources from documentation, enables TRAE to use files without loading them into context

### Progressive Disclosure Design Principle

Skills use a three-level loading system to manage context efficiently:

1. **Metadata (name + description)** - Always in context (~100 words)
2. **SKILL.md body** - When skill triggers (<5k words)
3. **Bundled resources** - As needed by TRAE (Unlimited*)

*Unlimited because scripts can be executed without reading into context window.

## Skill Creation Process

To create a skill, follow the "Skill Creation Process" in order, skipping steps only if there is a clear reason why they are not applicable.

### Step 1: Understanding the Skill with Concrete Examples

Skip this step only when the skill's usage patterns are already clearly understood. It remains valuable even when working with an existing skill.

To create an effective skill, clearly understand concrete examples of how the skill will be used. This understanding can come from either direct user examples or generated examples that are validated with user feedback.

For example, when building an image-editor skill, relevant questions include:

- "What functionality should the image-editor skill support? Editing, rotating, anything else?"
- "Can you give some examples of how this skill would be used?"
- "I can imagine users asking for things like 'Remove the red-eye from this image' or 'Rotate this image'. Are there other ways you imagine this skill being used?"
- "What would a user say that should trigger this skill?"

To avoid overwhelming users, avoid asking too many questions in a single message. Start with the most important questions and follow up as needed for better effectiveness.

Conclude this step when there is a clear sense of the functionality the skill should support.

### Step 2: Planning the Reusable Skill Contents

To turn concrete examples into an effective skill, analyze each example by:

1. Considering how to execute on the example from scratch
2. Identifying what scripts, references, and assets would be helpful when executing these workflows repeatedly

Example: When building a `pdf-editor` skill to handle queries like "Help me rotate this PDF," the analysis shows:

1. Rotating a PDF requires re-writing the same code each time
2. A `scripts/rotate_pdf.py` script would be helpful to store in the skill

Example: When designing a `frontend-webapp-builder` skill for queries like "Build me a todo app" or "Build me a dashboard to track my steps," the analysis shows:

1. Writing a frontend webapp requires the same boilerplate HTML/React each time
2. An `assets/hello-world/` template containing the boilerplate HTML/React project files would be helpful to store in the skill

Example: When building a `big-query` skill to handle queries like "How many users have logged in today?" the analysis shows:

1. Querying BigQuery requires re-discovering the table schemas and relationships each time
2. A `references/schema.md` file documenting the table schemas would be helpful to store in the skill

**For TRAE Code plugins:** When building a hooks skill, the analysis shows:
1. Developers repeatedly need to validate hooks.json and test hook scripts
2. `scripts/validate-hook-schema.sh` and `scripts/test-hook.sh` utilities would be helpful
3. `references/patterns.md` for detailed hook patterns to avoid bloating SKILL.md

To establish the skill's contents, analyze each concrete example to create a list of the reusable resources to include: scripts, references, and assets.

### Step 3: Create Skill Structure

For TRAE Code plugins, create the skill directory structure:

```bash
mkdir -p plugin-name/skills/skill-name/{references,examples,scripts}
touch plugin-name/skills/skill-name/SKILL.md
```

**Note:** Unlike the generic skill-creator which uses `init_skill.py`, plugin skills are created directly in the plugin's `skills/` directory with a simpler manual structure.

### Step 4: Edit the Skill

When editing the (newly-created or existing) skill, remember that the skill is being created for another instance of TRAE to use. Focus on including information that would be beneficial and non-obvious to TRAE. Consider what procedural knowledge, domain-specific details, or reusable assets would help another TRAE instance execute these tasks more effectively.

#### Start with Reusable Skill Contents

To begin implementation, start with the reusable resources identified above: `scripts/`, `references/`, and `assets/` files. Note that this step may require user input. For example, when implementing a `brand-guidelines` skill, the user may need to provide brand assets or templates to store in `assets/`, or documentation to store in `references/`.

Also, delete any example files and directories not needed for the skill. Create only the directories you actually need (references/, examples/, scripts/).

#### Update SKILL.md

**Writing Style:** Write the entire skill using **imperative/infinitive form** (verb-first instructions), not second person. Use objective, instructional language (e.g., "To accomplish X, do Y" rather than "You should do X" or "If you need to do X"). This maintains consistency and clarity for AI consumption.

**Description (Frontmatter):** Use third-person format with specific trigger phrases:

```yaml
---
name: Skill Name
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
version: 0.1.0
---
```

**Good description examples:**
```yaml
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
Ask: "Review my skill and check if it follows best practices"
```

The skill-reviewer agent will check description quality, content organization, and progressive disclosure.

### Step 6: Iterate

After testing the skill, users may request improvements. Often this happens right after using the skill, with fresh context of how the skill performed.

**Iteration workflow:**
1. Use the skill on real tasks
2. Notice struggles or inefficiencies
3. Identify how SKILL.md or bundled resources should be updated
4. Implement changes and test again

**Common improvements:**
- Strengthen trigger phrases in description
- Move long sections from SKILL.md to references/
- Add missing examples or scripts
- Clarify ambiguous instructions
- Add edge case handling

## Plugin-Specific Considerations

### Skill Location in Plugins

Plugin skills live in the plugin's `skills/` directory:

```
my-plugin/
├── .TRAE-plugin/
│   └── plugin.json
├── commands/
├── agents/
└── skills/
    └── my-skill/
        ├── SKILL.md
        ├── references/
        ├── examples/
        └── scripts/
```

### Auto-Discovery

TRAE Code automatically discovers skills:
- Scans `skills/` directory
- Finds subdirectories containing `SKILL.md`
- Loads skill metadata (name + description) always
- Loads SKILL.md body when skill triggers
- Loads references/examples when needed

### No Packaging Needed

Plugin skills are distributed as part of the plugin, not as separate ZIP files. Users get skills when they install the plugin.

### Testing in Plugins

Test skills by installing plugin locally:

```bash
# Test with --plugin-dir
cc --plugin-dir /path/to/plugin

# Ask questions that should trigger the skill
# Verify skill loads correctly
```

## Examples from Plugin-Dev

Study the skills in this plugin as examples of best practices:

**hook-development skill:**
- Excellent trigger phrases: "create a hook", "add a PreToolUse hook", etc.
- Lean SKILL.md (1,651 words)
- 3 references/ files for detailed content
- 3 examples/ of working hooks
- 3 scripts/ utilities

**agent-development skill:**
- Strong triggers: "create an agent", "agent frontmatter", etc.
- Focused SKILL.md (1,438 words)
- References include the AI generation prompt from TRAE Code
- Complete agent examples

**plugin-settings skill:**
- Specific triggers: "plugin settings", ".local.md files", "YAML frontmatter"
- References show real implementations (multi-agent-swarm, ralph-wiggum)
- Working parsing scripts

Each demonstrates progressive disclosure and strong triggering.

## Progressive Disclosure in Practice

### What Goes in SKILL.md

**Include (always loaded when skill triggers):**
- Core concepts and overview
- Essential procedures and workflows
- Quick reference tables
- Pointers to references/examples/scripts
- Most common use cases

**Keep under 3,000 words, ideally 1,500-2,000 words**

### What Goes in references/

**Move to references/ (loaded as needed):**
- Detailed patterns and advanced techniques
- Comprehensive API documentation
- Migration guides
- Edge cases and troubleshooting
- Extensive examples and walkthroughs

**Each reference file can be large (2,000-5,000+ words)**

### What Goes in examples/

**Working code examples:**
- Complete, runnable scripts
- Configuration files
- Template files
- Real-world usage examples

**Users can copy and adapt these directly**

### What Goes in scripts/

**Utility scripts:**
- Validation tools
- Testing helpers
- Parsing utilities
- Automation scripts

**Should be executable and documented**

## Writing Style Requirements

### Imperative/Infinitive Form

Write using verb-first instructions, not second person:

**Correct (imperative):**
```
To create a hook, define the event type.
Configure the MCP server with authentication.
Validate settings before use.
```

**Incorrect (second person):**
```
You should create a hook by defining the event type.
You need to configure the MCP server.
You must validate settings before use.
```

### Third-Person in Description

The frontmatter description must use third person:

**Correct:**
```yaml
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
description: "Agent 技能开发与规范设计。用于编写新技能、扩展功能、组织渐进式披露结构与优化提示词。"
---

## 技能协作接口

### 在技能体系中的定位

```
[需要创建/改进技能时] → [skill-development] → [技能文件写入 .trae/skills/]
                                          ↓
                              [更新 SKILL-SYSTEM-OVERVIEW.md]
```

**本角色**：技能体系的元技能（Meta-Skill），负责创建、改进和维护其他所有技能。不参与常规项目流水线，但在技能体系维护、优化和扩展时被调用。

### 触发条件

| 触发场景 | 说明 |
|----------|------|
| 创建新技能 | 用户需要新的专业能力领域 |
| 改进现有技能 | 技能使用中发现不足需要优化 |
| 技能体系维护 | 批量更新协作接口、统一格式等 |
| 技能调试 | 某个技能触发不准确或行为异常 |

### 输出产物

| 输出内容 | 位置 | 消费者 |
|----------|------|--------|
| 新 SKILL.md | `.trae/skills/{name}/SKILL.md` | 技能系统自动发现 |
| 参考文档 | `.trae/skills/{name}/references/` | 技能按需加载 |
| 资源文件 | `.trae/skills/{name}/assets/` | 技能输出使用 |
| 系统总览更新 | `SKILL-SYSTEM-OVERVIEW.md` | 全局参考 |

### 与技能体系的关系

| 关系类型 | 对象 | 说明 |
|----------|------|------|
| **创建者** | 所有技能 | 负责创建和初始化每个技能 |
| **维护者** | 所有技能 | 负责改进和优化现有技能 |
| **协调者** | SKILL-SYSTEM-OVERVIEW | 维护技能体系的整体架构文档 |
| **被参考者** | skill-creator | skill-development 是 skill-creator 的完整版 |

### 协作协议

#### 创建新技能流程
1. 使用 Skill Development 的 Step 1-8 流程
2. 创建后必须添加 `## 技能协作接口` 章节
3. 更新 `SKILL-SYSTEM-OVERVIEW.md` 注册新技能
4. 验证技能可被系统正确发现和加载

#### 改进现有技能流程
1. 分析使用反馈和触发准确度
2. 按 Skill Development 原则优化 description 和内容
3. 补充或更新 `## 技能协作接口` 章节
4. 同步更新 `SKILL-SYSTEM-OVERVIEW.md` 中的定位信息

#### 批量维护场景
- 统一协作接口格式
- 更新跨技能引用关系
- 补充缺失的上下游对接点

### 设计原则应用

本技能自身严格遵循以下原则（同时也指导其他技能的设计）：

| 原则 | 应用方式 |
|------|----------|
| **Progressive Disclosure** | SKILL.md 保持精简（1500-2000词），详细内容放 references/ |
| **Third-Person Description** | 所有技能的 frontmatter description 使用第三人称 |
| **Imperative Writing** | SKILL.md 正文使用祈使句/动词开头 |
| **Strong Triggers** | description 包含具体用户查询短语 |
| **Resource References** | SKILL.md 中明确引用 references/ 和 assets/ 文件 |

### 协作约束

- **元层级操作**：本技能的操作对象是其他技能，而非项目代码
- **不干预执行**：不参与具体项目的技能执行流程
- **向后兼容**：修改现有技能时保持触发兼容性
- ⚠️ **Search Agent 只用于搜索**：无写文件权限，不做文档修改/分析
- **每次操作记录时间戳**，涉及新技能模式时使用 WebSearch
