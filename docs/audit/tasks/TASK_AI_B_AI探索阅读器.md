# 【AI B - AI 探索阅读器】专属重构任务工单

> **工单代号**：`AI_B`  
> **所属模块**：`:feature:learning`  
> **业务范围**：AI 日语分级文章阅读、查词解析弹窗、长文阅读历史  
> **待修改源码文件数**：`4` 个  
> **待提取硬编码总条数**：`32` 处  
> **专属独立资源文件（防冲突）**：`feature/learning/src/main/res/values/strings_learning_ai_reading.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.learning.R`  
> **独立编译验证命令**：`./gradlew :feature:learning:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/learning/src/main/res/values/strings_learning_ai_reading.xml`。
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

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingHistoryScreen.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L181 | `DATE_FORMAT` | `MM月dd日 HH:mm` | `val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingScreen.kt`（共 22 处硬编码）

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

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingViewModel.kt`（共 6 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L31 | `OTHER` | `日常生活` | `val readingTheme: String = "日常生活",` |
| L255 | `UI_STATE_OR_PROMPT` | `请先在配置中心添加 AI 模型`<br>`只有一个配置，无法切换` | `it.copy(error = if (configs.isEmpty()) "请先在配置中心添加 AI 模型" else "只有一个配置，无法切换")` |
| L320 | `UI_STATE_OR_PROMPT` | `API 密钥未配置，请前往设置配置！` | `_uiState.update { it.copy(error = "API 密钥未配置，请前往设置配置！") }` |
| L361 | `UI_STATE_OR_PROMPT` | `生成短文失败，请重试: ${error.localizedMessage ?: error.message}` | `error = "生成短文失败，请重试: ${error.localizedMessage ?: error.message}"` |
| L372 | `OTHER` | `API 密钥未配置` | `_uiState.update { it.copy(translationError = "API 密钥未配置") }` |
| L409 | `OTHER` | `翻译失败: ${error.localizedMessage ?: error.message}` | `translationError = "翻译失败: ${error.localizedMessage ?: error.message}"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/components/CustomTextToolbar.kt`（共 3 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L40 | `OTHER` | `复制` | `menu.add(0, MENU_ITEM_COPY, 0, "复制")` |
| L43 | `OTHER` | `全选` | `menu.add(0, MENU_ITEM_SELECT_ALL, 1, "全选")` |
| L46 | `OTHER` | `翻译` | `menu.add(0, MENU_ITEM_TRANSLATE, 2, "翻译")` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/learning/src/main/res/values/strings_learning_ai_reading.xml`。
- [ ] 成功执行 `./gradlew :feature:learning:compileDebugKotlin`，模块独立编译通过，0 错误。
