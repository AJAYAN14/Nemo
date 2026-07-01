# sample-output

下面是一份中间 outline 样例，用于说明 `chat-to-skill` 脚本的目标输出方向。

```json
{
  "source_files": [
    "chat-1.md"
  ],
  "goal": [
    "将多轮聊天提炼为可复用技能包",
    "在生成前完成严格脱敏"
  ],
  "triggers": [
    "根据聊天记录创建技能",
    "把聊天记录整理成 skill"
  ],
  "stable_steps": [
    "读取聊天源",
    "识别轮次结构",
    "执行脱敏归一化",
    "抽取稳定模式",
    "生成中间 outline",
    "组装技能包",
    "更新技能总览",
    "执行校验"
  ],
  "decision_points": [
    "是否输出完整技能包",
    "脱敏强度选择",
    "示例是重写还是省略",
    "是否需要最小脚本"
  ],
  "validation_steps": [
    "检查目录结构完整性",
    "检查触发词质量",
    "检查示例是否通用化",
    "检查是否残留敏感实体",
    "检查脚本是否可生成 outline"
  ],
  "pitfalls": [
    "直接把原聊天拼成 SKILL",
    "示例残留真实路径",
    "把一次性 bug 当成长期规则",
    "忽略总览更新"
  ],
  "anonymization_notes": [
    "用通用占位符替换真实项目名、路径、文件名和标题",
    "保留方法论和目录角色，不保留可回溯实体"
  ],
  "skill_assets": [
    "SKILL.md",
    "references/",
    "examples/",
    "scripts/"
  ]
}
```
