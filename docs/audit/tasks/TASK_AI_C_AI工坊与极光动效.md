# 【AI C - AI 工坊与极光动效】专属重构任务工单

> **工单代号**：`AI_C`  
> **所属模块**：`:feature:learning`  
> **业务范围**：AI 题目生成工坊、工坊交互状态、极光背景物理折射动效  
> **待修改源码文件数**：`3` 个  
> **待提取硬编码总条数**：`20` 处  
> **专属独立资源文件（防冲突）**：`feature/learning/src/main/res/values/strings_learning_ai_workshop.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.learning.R`  
> **独立编译验证命令**：`./gradlew :feature:learning:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/learning/src/main/res/values/strings_learning_ai_workshop.xml`。
   - 若文件不存在可直接新建；若已存在则追加在根 `<resources>` 标签内。
   - **严禁向公共或其它 strings 文件写入，确保你的改动与其他 AI 0 冲突**！
3. **模块内 R 引用闭环**：Kotlin 源码中统一通过 `stringResource(com.jian.nemo.feature.learning.R.string.xxx)` 取用，**严禁跨模块引用外部 feature 模块的 R 资源**。
4. **规范 Key 命名**：`[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **严禁口水注释与黑盒脚本**：严禁在源码中留下“根据要求提取”等元注释；严禁使用黑盒正则脚本批处理，必须保持代码安全。
6. **单模块独立编译验证**：修改完成后，在终端直接运行：
   ```bash
   ./gradlew :feature:learning:compileDebugKotlin
   ```
   编译通过且无任何报错即代表本工单顺利交付！严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIWorkshopScreen.kt`（共 10 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L1462 | `OTHER` | `解析练习要求：明确目标日语等级与句型考察重点` | `"解析练习要求：明确目标日语等级与句型考察重点",` |
| L1463 | `OTHER` | `检索例句语境：匹配地道贴切的日式生活表达场景` | `"检索例句语境：匹配地道贴切的日式生活表达场景",` |
| L1464 | `OTHER` | `构建句子骨架：确立谓语动词时态与助词搭配网络` | `"构建句子骨架：确立谓语动词时态与助词搭配网络",` |
| L1465 | `OTHER` | `梳理接续规则：校验形容词、副词与动词连用变形` | `"梳理接续规则：校验形容词、副词与动词连用变形",` |
| L1466 | `OTHER` | `生成候选译句：兼顾敬体与简体的自然语感表达` | `"生成候选译句：兼顾敬体与简体的自然语感表达",` |
| L1467 | `OTHER` | `优化语法表达：消除中式日语痕迹，提升文段地道度` | `"优化语法表达：消除中式日语痕迹，提升文段地道度",` |
| L1468 | `OTHER` | `构建对照译文：生成精准贴合语境的中文参考翻译` | `"构建对照译文：生成精准贴合语境的中文参考翻译",` |
| L1469 | `OTHER` | `拟定重点解析：提炼关键语法条目、用法与辨析要点` | `"拟定重点解析：提炼关键语法条目、用法与辨析要点",` |
| L1470 | `OTHER` | `准备评分模型：建立多维度语法准确度与流利度基准` | `"准备评分模型：建立多维度语法准确度与流利度基准",` |
| L1471 | `OTHER` | `完成题目装配：格式化题目与交互结构，准备呈现` | `"完成题目装配：格式化题目与交互结构，准备呈现"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIWorkshopViewModel.kt`（共 5 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L313 | `UI_STATE_OR_PROMPT` | `请先在配置中心添加 AI 模型`<br>`只有一个配置，无法切换` | `it.copy(error = if (configs.isEmpty()) "请先在配置中心添加 AI 模型" else "只有一个配置，无法切换")` |
| L345 | `UI_STATE_OR_PROMPT` | `请先在设置中配置 API Key` | `_uiState.update { it.copy(error = "请先在设置中配置 API Key") }` |
| L396 | `UI_STATE_OR_PROMPT` | `本地暂无 $difficulty 等级的语法数据，请先同步或切换到自由模式` | `error = "本地暂无 $difficulty 等级的语法数据，请先同步或切换到自由模式",` |
| L465 | `UI_STATE_OR_PROMPT` | `获取题目失败: ${e.message}` | `_uiState.update { it.copy(error = "获取题目失败: ${e.message}", isLoading = false) }` |
| L506 | `UI_STATE_OR_PROMPT` | `评分失败: ${e.message}` | `_uiState.update { it.copy(error = "评分失败: ${e.message}", isLoading = false) }` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/components/AIAuroraBackground.kt`（共 5 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L38 | `OTHER` | `经典极光` | `name = "经典极光",` |
| L46 | `OTHER` | `霓虹天际` | `name = "霓虹天际",` |
| L54 | `OTHER` | `极昼破晓` | `name = "极昼破晓",` |
| L62 | `OTHER` | `冰川青碧` | `name = "冰川青碧",` |
| L70 | `OTHER` | `暮色紫霞` | `name = "暮色紫霞",` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/learning/src/main/res/values/strings_learning_ai_workshop.xml`。
- [ ] 成功执行 `./gradlew :feature:learning:compileDebugKotlin`，模块独立编译通过，0 错误。
