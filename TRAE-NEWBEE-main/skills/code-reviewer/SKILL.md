---
name: code-reviewer
description: This skill should be used when the user asks to "review code", "review a pull request", "check PR", or needs code quality assessment. Supports both GitHub PR review (using MCP tools) and local changes review (staged or working tree). Covers correctness, maintainability, security, performance, and adherence to project standards.
version: 2.0.0
---

## 技能执行流程图

```mermaid
flowchart TB
    START(["触发"]) --> TIME["记录当前时间"]
    TIME --> MODE{"审查模式?"}

    MODE -->|远程PR| PR["获取PR信息<br/>GitHub MCP工具"]
    MODE -->|本地变更| LOCAL["读取staged/working tree<br/>文件差异"]

    PR --> ANALYZE["分析变更内容<br/>文件列表+差异"]
    LOCAL --> ANALYZE

    ANALYZE --> REVIEW["结构化审查<br/>Issues/Suggestions/Nitpicks"]
    REVIEW --> FRONTEND{"前端文件?"}
    FRONTEND -->|是| FCR["frontend-code-review<br/>前端专项深度审查"]
    FRONTEND →|否| OUTPUT
    FCR --> OUTPUT

    OUTPUT(["输出: 审查报告<br/>Approved/Request Changes"])

    style START fill:#4CAF50
    style OUTPUT fill:#2196F3
```

# Code Reviewer

This skill guides the agent in conducting professional and thorough code reviews for both local development and remote Pull Requests.

## Workflow

### 1. Determine Review Target
*   **Remote PR**: If the user provides a PR number or URL (e.g., "Review PR #123"), target that remote PR.
*   **Local Changes**: If no specific PR is mentioned, or if the user asks to "review my changes", target the current local file system states (staged and unstaged changes).

### 2. Preparation

#### For Remote PRs:
1.  **Checkout**: Use the GitHub CLI to checkout the PR.
    ```bash
    gh pr checkout <PR_NUMBER>
    ```
2.  **Preflight**: Execute the project's standard verification suite to catch automated failures early.
    ```bash
    npm run preflight
    ```
3.  **Context**: Read the PR description and any existing comments to understand the goal and history.

#### For Local Changes:
1.  **Identify Changes**:
    *   Check status: `git status`
    *   Read diffs: `git diff` (working tree) and/or `git diff --staged` (staged).
2.  **Preflight (Optional)**: If the changes are substantial, ask the user if they want to run `npm run preflight` before reviewing.

### 3. In-Depth Analysis
Analyze the code changes based on the following pillars:

*   **Correctness**: Does the code achieve its stated purpose without bugs or logical errors?
*   **Maintainability**: Is the code clean, well-structured, and easy to understand and modify in the future? Consider factors like code clarity, modularity, and adherence to established design patterns.
*   **Readability**: Is the code well-commented (where necessary) and consistently formatted according to our project's coding style guidelines?
*   **Efficiency**: Are there any obvious performance bottlenecks or resource inefficiencies introduced by the changes?
*   **Security**: Are there any potential security vulnerabilities or insecure coding practices?
*   **Edge Cases and Error Handling**: Does the code appropriately handle edge cases and potential errors?
*   **Testability**: Is the new or modified code adequately covered by tests (even if preflight checks pass)? Suggest additional test cases that would improve coverage or robustness.

### 4. Provide Feedback

#### Structure
*   **Summary**: A high-level overview of the review.
*   **Findings**:
    *   **Critical**: Bugs, security issues, or breaking changes.
    *   **Improvements**: Suggestions for better code quality or performance.
    *   **Nitpicks**: Formatting or minor style issues (optional).
*   **Conclusion**: Clear recommendation (Approved / Request Changes).

#### Tone
*   Be constructive, professional, and friendly.
*   Explain *why* a change is requested.
*   For approvals, acknowledge the specific value of the contribution.

### 5. Cleanup (Remote PRs only)
*   After the review, ask the user if they want to switch back to the default branch (e.g., `main` or `master`).

---

## 技能协作接口

### 在技能体系中的定位

```
[pr-creator 创建PR] → [code-reviewer 审查] → [full-review-repair-fractal / refactor-fractal]
[开发本地变更] ────→ [code-reviewer 审查] → [修复建议]
```

**本角色**：代码质量守门员，对 GitHub PR 和本地变更进行结构化审查，确保代码符合项目规范。

### 上游输入（触发条件）

| 触发场景 | 来源 | 说明 |
|----------|------|------|
| PR 创建完成 | pr-creator | 自动触发审查流程 |
| 本地变更待提交 | 开发实施 / fullstack-developer | 手动或自动触发审查 |
| 重构完成后 | refactor-fractal | 确保重构代码质量 |
| 全面审查阶段 | full-review-repair-fractal | 作为审查子环节 |

### 下游输出

| 输出内容 | 消费者 | 使用方式 |
|----------|--------|----------|
| 审查报告（Approved/Request Changes） | pr-creator | 决定PR是否可合并 |
| 问题清单 + 修复建议 | full-review-repair-fractal | 驱动问题修复 |
| 重构建议 | refactor-fractal | 驱动架构级改进 |
| 前端专项问题 | frontend-code-review | 前端代码深度审查 |

### 协作协议

#### ← pr-creator
- **触发时机**：PR创建后自动调用
- **输入数据**：PR URL、分支信息、变更文件列表
- **输出格式**：结构化审查报告（Issues/Suggestions/Nitpicks）

#### → full-review-repair-fractal
- **传递内容**：按严重程度分类的问题清单
- **集成方式**：Request Changes 时的问题成为修复任务

#### ↔ frontend-code-review
- **分工**：code-reviewer 负责通用审查；frontend-code-review 负责前端专项深度审查
- **协作时机**：涉及 .tsx/.ts/.css 文件时并行调用

### 审查模式

| 模式 | 适用场景 | 特点 |
|------|----------|------|
| 远程PR审查 | GitHub Pull Request | 使用MCP工具获取PR信息、发表评论 |
| 本地变更审查 | 未提交的代码改动 | 直接读取文件差异进行分析 |

### 协作约束

- **客观性**：审查意见必须具体、有依据，避免主观判断
- **建设性**：每个 Request Change 必须附带修复建议
- **安全性**：远程PR审查绝不推送到 main 分支
- **Accuracy**: Don't check boxes for tasks you haven't done.
- ⚠️ **Search Agent 只用于搜索**：无写文件权限，不做文档修改/分析
- **每次操作记录时间戳**，涉及新审查工具或模式时使用 WebSearch
