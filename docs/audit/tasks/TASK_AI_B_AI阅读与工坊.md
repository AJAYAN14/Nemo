# 【AI B - AI 阅读器与 AI 工坊】专属任务执行工单

> **工单代号**：`AI_B`  
> **负责模块**：`:feature:learning`  
> **负责子域**：AI 探索阅读与 AI 工坊（分级阅读、词汇解析、极光背景、浮动工具栏）  
> **待修改文件数**：`7` 个  
> **待提取硬编码总数**：`52` 处  
> **推荐追加的专属资源文件**：`feature/learning/src/main/res/values/strings_learning_ai.xml`  
> **当前模块 R 引用**：`import com.jian.nemo.feature.learning.R`  
> **独立编译验证命令**：`./gradlew :feature:learning:compileDebugKotlin`  

---

## 一、执行硬约束纪律（执行前必读）

1. **严禁越界修改**：你只被授权修改本工单下方【第二章】所明确列出的文件。**严禁修改任何未被点名的文件**。
2. **独立资源文件隔离（防冲突关键）**：
   - 请将所有提取出的 `<string name="...">文案</string>` 写入/追加到专属文件：`feature/learning/src/main/res/values/strings_learning_ai.xml`。
   - 如果该文件不存在，可直接新建；如果已存在则追加在 `<resources>` 标签内。
   - **严禁与其他 AI 共用或改动同一个 strings 文件，以杜绝 Git 合并冲突**！
3. **R 引用闭环**：在 Kotlin 源码中，统一导入 `com.jian.nemo.feature.learning.R`，通过 `stringResource(com.jian.nemo.feature.learning.R.string.xxx)` 或 `context.getString(...)` 取用，严禁跨模块引用其他 feature 模块未编译生成的 R 资源。
4. **命名规范**：遵循 `[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **独立编译验证**：修改完成后，在项目终端运行：
   ```bash
   ./gradlew :feature:learning:compileDebugKotlin
   ```
   必须确保无任何编译与语法报错。严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingHistoryScreen.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L181 | `DATE_FORMAT` | `MM月dd日 HH:mm` | `val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingScreen.kt`（共 22 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L334 | `OTHER` | `日常生活` | `name = "日常生活",` |
| L335 | `COMPOSE_UI` | `温暖生活琐事` | `subtitle = "温暖生活琐事",` |
| L343 | `OTHER` | `传统文化` | `name = "传统文化",` |
| L344 | `COMPOSE_UI` | `和风传统美学` | `subtitle = "和风传统美学",` |
| L352 | `OTHER` | `民间故事` | `name = "民间故事",` |
| L353 | `COMPOSE_UI` | `经典奇幻物语` | `subtitle = "经典奇幻物语",` |
| L361 | `OTHER` | `美食料理` | `name = "美食料理",` |
| L362 | `COMPOSE_UI` | `地道烟火风味` | `subtitle = "地道烟火风味",` |
| L370 | `OTHER` | `名胜风光` | `name = "名胜风光",` |
| L371 | `COMPOSE_UI` | `四季列岛巡礼` | `subtitle = "四季列岛巡礼",` |
| L379 | `OTHER` | `商务工作` | `name = "商务工作",` |
| L380 | `COMPOSE_UI` | `职场严谨敬语` | `subtitle = "职场严谨敬语",` |
| L1129 | `OTHER` | `分析阅读偏好：明确选定主题与目标难度级别` | `"分析阅读偏好：明确选定主题与目标难度级别",` |
| L1130 | `OTHER` | `检索主题语境：搜寻地道自然的日式生活场景` | `"检索主题语境：搜寻地道自然的日式生活场景",` |
| L1131 | `OTHER` | `挑选核心词汇：筛选适合当前水平的重点假名与单词` | `"挑选核心词汇：筛选适合当前水平的重点假名与单词",` |
| L1132 | `OTHER` | `构建文章脉络：规划段落层级与情节展开走向` | `"构建文章脉络：规划段落层级与情节展开走向",` |
| L1133 | `OTHER` | `组织日文段落：生成自然流畅、符合母语表达习惯的句子` | `"组织日文段落：生成自然流畅、符合母语表达习惯的句子",` |
| L1134 | `OTHER` | `优化语法表达：校验用词准确性与文段连贯度` | `"优化语法表达：校验用词准确性与文段连贯度",` |
| L1135 | `OTHER` | `对照中文翻译：生成忠实地道的中文译文与段落对照` | `"对照中文翻译：生成忠实地道的中文译文与段落对照",` |
| L1136 | `OTHER` | `设计阅读测验：围绕短文核心细节生成理解问答题` | `"设计阅读测验：围绕短文核心细节生成理解问答题",` |
| L1137 | `OTHER` | `整理重点词条：提炼生词列表、假名读音与释义` | `"整理重点词条：提炼生词列表、假名读音与释义",` |
| L1138 | `OTHER` | `完成排版校验：梳理段落布局，准备开启阅读训练` | `"完成排版校验：梳理段落布局，准备开启阅读训练"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingViewModel.kt`（共 6 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L31 | `OTHER` | `日常生活` | `val readingTheme: String = "日常生活",` |
| L255 | `UI_STATE_OR_PROMPT` | `请先在配置中心添加 AI 模型`<br>`只有一个配置，无法切换` | `it.copy(error = if (configs.isEmpty()) "请先在配置中心添加 AI 模型" else "只有一个配置，无法切换")` |
| L320 | `UI_STATE_OR_PROMPT` | `API 密钥未配置，请前往设置配置！` | `_uiState.update { it.copy(error = "API 密钥未配置，请前往设置配置！") }` |
| L361 | `UI_STATE_OR_PROMPT` | `生成短文失败，请重试: ${error.localizedMessage ?: error.message}` | `error = "生成短文失败，请重试: ${error.localizedMessage ?: error.message}"` |
| L372 | `OTHER` | `API 密钥未配置` | `_uiState.update { it.copy(translationError = "API 密钥未配置") }` |
| L409 | `OTHER` | `翻译失败: ${error.localizedMessage ?: error.message}` | `translationError = "翻译失败: ${error.localizedMessage ?: error.message}"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIWorkshopScreen.kt`（共 10 处）

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

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIWorkshopViewModel.kt`（共 5 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L313 | `UI_STATE_OR_PROMPT` | `请先在配置中心添加 AI 模型`<br>`只有一个配置，无法切换` | `it.copy(error = if (configs.isEmpty()) "请先在配置中心添加 AI 模型" else "只有一个配置，无法切换")` |
| L345 | `UI_STATE_OR_PROMPT` | `请先在设置中配置 API Key` | `_uiState.update { it.copy(error = "请先在设置中配置 API Key") }` |
| L396 | `UI_STATE_OR_PROMPT` | `本地暂无 $difficulty 等级的语法数据，请先同步或切换到自由模式` | `error = "本地暂无 $difficulty 等级的语法数据，请先同步或切换到自由模式",` |
| L465 | `UI_STATE_OR_PROMPT` | `获取题目失败: ${e.message}` | `_uiState.update { it.copy(error = "获取题目失败: ${e.message}", isLoading = false) }` |
| L506 | `UI_STATE_OR_PROMPT` | `评分失败: ${e.message}` | `_uiState.update { it.copy(error = "评分失败: ${e.message}", isLoading = false) }` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/components/AIAuroraBackground.kt`（共 5 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L38 | `OTHER` | `经典极光` | `name = "经典极光",` |
| L46 | `OTHER` | `霓虹天际` | `name = "霓虹天际",` |
| L54 | `OTHER` | `极昼破晓` | `name = "极昼破晓",` |
| L62 | `OTHER` | `冰川青碧` | `name = "冰川青碧",` |
| L70 | `OTHER` | `暮色紫霞` | `name = "暮色紫霞",` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/components/CustomTextToolbar.kt`（共 3 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L40 | `OTHER` | `复制` | `menu.add(0, MENU_ITEM_COPY, 0, "复制")` |
| L43 | `OTHER` | `全选` | `menu.add(0, MENU_ITEM_SELECT_ALL, 1, "全选")` |
| L46 | `OTHER` | `翻译` | `menu.add(0, MENU_ITEM_TRANSLATE, 2, "翻译")` |

---

## 三、完成验收检查单

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/learning/src/main/res/values/strings_learning_ai.xml`。
- [ ] 成功执行 `./gradlew :feature:learning:compileDebugKotlin`，编译通过，0 Error。
