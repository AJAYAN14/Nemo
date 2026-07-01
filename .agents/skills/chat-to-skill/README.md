# chat-to-skill

将多轮聊天记录提炼为可复用 Skill 资产的 Meta-Skill。

## 简介

`chat-to-skill` 是一个专门用于将聊天记录、对话经验转化为可复用技能包的元技能。它能够从真实的项目对话中提取稳定模式，经过严格脱敏后，组装成标准化的技能资产。

## 核心能力

- ✅ **严格脱敏**：自动替换项目名、路径、业务信息等敏感内容
- ✅ **模式抽取**：识别高频任务、决策点、验证动作
- ✅ **技能组装**：生成标准结构的技能包（SKILL.md + references + examples + scripts）
- ✅ **噪音过滤**：剔除一次性 bug、临时环境细节等无关内容

## 使用场景

当你需要将以下内容转化为技能时使用：

- 💬 多轮聊天记录中的方法论
- 🛠️ 反复出现的操作流程
- 📋 需要形成规范的决策模式
- 🎯 可复用的问题解决方案

## 项目结构

```
chat-to-skill/
├── SKILL.md                          # 技能核心文档
├── README.md                         # 项目说明
├── examples/                         # 通用示例
│   ├── sample-chat.md
│   └── sample-output.md
├── references/                       # 参考文档
│   ├── extraction-checklist.md
│   └── summary-patterns.md
├── scripts/                          # 自动化工具
│   └── extract_skill_outline.py
└── tests/                            # 测试用例
    └── test_extract_skill_outline.py
```

## 快速开始

### 1. 在 TRAE 中使用

确保已安装技能后，使用以下触发短语：

- "把聊天记录整理成 skill"
- "根据聊天记录创建技能"
- "从对话提炼 SKILL"
- "把 chat 目录总结成技能"

### 2. 手动运行脚本

```bash
python scripts/extract_skill_outline.py <chat-file.md>
```

## 工作流

### 阶段一：聊天提炼
1. 读取输入源（文件/目录）
2. 识别对话轮次
3. 执行严格脱敏
4. 抽取稳定模式
5. 清理噪音
6. 产出中间 outline

### 阶段二：技能组装
1. 生成 frontmatter
2. 生成 SKILL.md
3. 拆分长说明到 references/
4. 填充通用示例到 examples/
5. 运行脚本验证
6. 更新技能系统总览

## 脱敏规范

**改写示例**：
- ❌ `E:\Project\TRAE-NEWBEE\skills\chat-to-skill`
- ✅ `workspace/chat/source.md`

- ❌ `TRAE-NEWBEE 项目`
- ✅ `目标项目`

- ❌ `客户 A 的订单系统`
- ✅ `目标业务场景`

## 验证清单

- [ ] 目录结构完整
- [ ] frontmatter 包含强触发词
- [ ] 使用指令式表达
- [ ] 示例为通用重写版本
- [ ] 无真实项目信息残留
- [ ] 脚本可正常运行

## 开发

### 运行测试

```bash
cd tests
python -m pytest test_extract_skill_outline.py -v
```

### 贡献

本技能遵循技能开发规范，如需改进请参考 `skill-development` 技能。

## 许可证

本技能作为技能系统的一部分，遵循相同的许可条款。

## 相关链接

- [技能系统总览](../SKILL-SYSTEM-OVERVIEW.md)
- [技能开发规范](../skill-development/SKILL.md)
