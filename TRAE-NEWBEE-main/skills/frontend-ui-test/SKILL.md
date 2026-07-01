---
name: frontend-ui-test
description: 前端界面操作测试技能：启动前端服务器、访问页面、截图和快照、对比变化。每次前端代码修改后必须调用。及时上网搜索学习新技术和方法。触发关键词：测试前端、UI测试、界面测试；使用范围：前端代码修改后的验证、UI调整后的效果检查。
version: 2.0.0
---

# Frontend UI Test v2.0 - 前端界面操作测试技能

## 技能执行流程图

```mermaid
flowchart TB
    START(["开始"]) --> TIME["记录当前时间"]
    TIME --> CHECK{"前后端<br/>服务已启动?"}
    CHECK -->|否| START_SRV["启动开发服务器<br/>后端 / 前端"]
    CHECK -->|是| NAV
    START_SRV --> NAV["导航到目标页<br/>等待加载完成"]

    NAV --> SNAP1["获取截图+快照<br/>browser_take_screenshot<br/>+ browser_snapshot"]
    SNAP1 --> READ1["读取截图内容<br/>AI分析视觉细节"]

    READ1 --> INTERACT["执行交互操作<br/>点击/输入/导航"]
    INTERACT --> WAIT["等待页面响应"]
    WAIT --> SNAP2["获取操作后截图+快照"]
    SNAP2 --> READ2["读取并对比变化<br/>截图对比 + DOM差异"]

    READ2 --> MORE{"更多测试?"}
    MORE -->|是| INTERACT
    MORE -->|否| REPORT["输出完整测试报告<br/>含问题清单"]

    REPORT --> CLEAN["清理截图文件<br/>_screenshot/目录"]
    CLEAN --> OUTPUT(["UI测试报告"])

    style START fill:#4CAF50
    style OUTPUT fill:#2196F3
```

## 技能概述

通过浏览器自动化进行**截图+快照双重对比**，验证前端界面的视觉和DOM结构正确性。

- **双通道验证**：截图（视觉对比）+ 快照（DOM结构验证）
- **AI驱动对比**：利用AI看图能力识别真实用户界面，针对性调整
- **循环多轮**：支持多页面、多操作的连续测试

## 核心工作流程

### 1. 启动与准备
- 记录**当前时间**
- 检查并启动前后端开发服务器（如未运行）
- 后端默认端口 `6123`，前端默认端口 `6124`

### 2. 页面访问与初始状态捕获
- 使用 MCP integrated_browser 服务导航到目标页
- 等待页面完全加载
- **必须同时获取** browser_snapshot 和 browser_take_screenshot
- AI 自行读取截图并分析视觉细节

### 3. 交互操作与变化检测
- 点击按钮或其他交互元素
- 等待响应
- 再次获取截图+快照
- 对比操作前后的**视觉变化**和**DOM结构差异**

### 4. 循环测试
- 根据测试需求循环执行步骤2-3
- 每轮包含完整的截图+快照+对比

### 5. 报告输出与清理
- 汇总所有轮次的测试结果
- 记录发现的问题（附截图证据）
- 清理 `_screenshot/` 目录下的临时文件

### 6. 技术搜索

遇到以下情况时使用 `WebSearch`：
- 新的浏览器自动化工具或框架
- 特殊UI组件的测试方法
- 性能测试或可访问性测试的最佳实践

## 关键规则

- **每次操作记录时间戳**
- **必须同时使用** browser_snapshot 和 browser_take_screenshot，不可只用其一
- 截图保存至 `项目根目录/_screenshot/`，测试完成后**必须清理**
- **Search Agent 只用于搜索**：无写文件权限，不做文档修改/分析
- app类型路由必须从首页导航进入目标页
- 出现问题时检查：浏览器控制台、后台终端、网络请求
- 子Agent无MCP权限：主Agent用MCP integrated_browser；子Agent用Playwright MCP

---

## 注意事项

- **Search Agent 仅限搜索操作**
- 不要偷懒！每轮测试都必须完整执行截图+快照+对比流程
- 截图和快照都需要AI自行获取并对比，不依赖人工对比
- 有疑问时使用 AskUserQuestion 询问用户

---

## 技能协作接口

```
[前端代码修改 / 后端影响前端] → [frontend-ui-test] → [full-review-repair-fractal]
```

**本角色**：前端修改后的实际界面验证工具。

| 触发场景 | 调用方 | 输出 |
|----------|--------|------|
| 前端代码修改后 | 开发实施 | UI测试报告(含截图) |
| API变更影响渲染 | 开发实施 | DOM快照差异报告 |
| bug修复后 | bug-hunter-fractal | 回归验证报告 |

**协作约束**：主Agent用MCP integrated_browser；子Agent用Playwright MCP
