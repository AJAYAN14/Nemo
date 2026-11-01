# Nemo 项目 UI 文本硬编码审查报告与多 AI 并行整改方案

> **审查时间**：2026-11-01  
> **审查方式**：静态受控只读脚本扫描（零黑盒修改、零源文件写入）  
> **遵循规范**：`.agent/rules/nemorules.md` · `docs/rules/rules-ui-fidelity.md` · `docs/todo/01_UI字符串规范化与国际化迁移待办.md`  
> **文档路径**：`docs/audit/01_UI文本硬编码审查与多AI并行整改方案.md`

---

## 一、全项目硬编码文本审查统计大盘

### 1.1 总体数据概览

- **项目有效扫描源码文件总数（排除 bin/build/test）**：`88` 个
- **🎯 面向 UI 界面的待整改文件总数**：`65` 个
- **🎯 面向 UI 界面的硬编码文本总条数**：`439` 处
- **🛡️ 底层协议/异常/Prompt 隔离文件总数（无需纳入 UI strings）**：`23` 个（共 `110` 处）

### 1.2 UI 各模块分布明细表

| 负责 AI | 负责模块 | 模块定位 | 硬编码文件数 | 硬编码文本总数 | 对应资源文件 |
|---|---|---|---|---|---|
| **AI A** | `:feature:learning` | 负责 Nemo 最核心的日常背词... | `19` | `146` 处 | `feature/learning/src/main/res/values/strings.xml` |
| **AI B** | `:feature:settings` | 负责系统偏好设置、液态玻璃实验室... | `16` | `159` 处 | `feature/settings/src/main/res/values/strings.xml` |
| **AI C** | `:feature:statistics` | 负责画廊级热力图打卡、打卡海报生... | `14` | `91` 处 | `feature/statistics/src/main/res/values/strings.xml` |
| **AI D** | `:feature:library`, `:feature:collection`, `:core:ui` | 负责词库大厅分类、词性筛选、收藏... | `16` | `43` 处 | `各模块自身的 res/values/strings.xml` |

---

## 二、多 AI 并行协同架构与防冲突避坑总则（核心保障）

> **核心目标**：实现 AI A、B、C、D 在同时跑、独立修改各自模块时，**零 Git 冲突、零依赖错乱、独立编译 100% 成功通过**。

### 2.1 防冲突四大刚性原则

1. **模块物理隔离原则（Module Boundary Isolation）**：
   - AI A、B、C、D 各自独占指定的 Gradle 模块。
   - **严禁越界修改**：AI A 绝对不得触碰 `:feature:settings`，AI B 绝对不得触碰 `:feature:learning`，以此类推。

2. **资源文件私有内聚原则（Private Strings.xml Ownership）**：
   - 每个 AI **只能向自己所负责模块的 `res/values/strings.xml` 中追加 `<string>` 标签**。
   - 严禁多个 AI 共同编辑同一个 `strings.xml` 文件，从根源上杜绝文件合并冲突。

3. **R 引用本地闭环原则（Local R Reference Rule）**：
   - 在各模块的 `.kt` 文件中引用字符串资源时，必须引入**当前模块专属的 R 类**：
     - `:feature:learning` 使用 `import com.jian.nemo.feature.learning.R`
     - `:feature:settings` 使用 `import com.jian.nemo.feature.settings.R`
     - `:feature:statistics` 使用 `import com.jian.nemo.feature.statistics.R`
     - `:feature:library` 使用 `import com.jian.nemo.feature.library.R`
     - `:feature:collection` 使用 `import com.jian.nemo.feature.collection.R`
     - `:core:ui` 使用 `import com.jian.nemo.core.ui.R`
   - **绝对严禁跨模块引用其他 feature 模块未发布的 `R.string.xxx`**（防止出现 `Unresolved reference: string` 导致编译失败）。

4. **独立单元编译验证原则（Independent Compilation Verification）**：
   - 每个 AI 完成自己模块的整改后，**仅需执行自己模块的编译检查命令**（例如 `./gradlew :feature:learning:compileDebugKotlin`）。
   - 编译检查只要自己模块通过即代表合格，不受其他 AI 进度快慢的影响。
   - **严禁执行 `adb uninstall`、严禁生成或安装 Debug APK**（严格服从 `nemorules.md` 第 3 条）。


### 2.2 字符串 Key 命名规范标准（统一规范）

命名格式统一遵循：**`[模块]_[页面/组件]_[类型]_[描述]`**

| 类型代码 | 对应控件 / 场景 | 命名示例 |
|---|---|---|
| `title_` | 页面大标题、卡片分组标题 | `learning_verb_handbook_title` |
| `label_` | 状态标签、辅助说明文本、进度标签 | `settings_ai_model_temperature_label` |
| `btn_` | 按钮点击文本、操作项 | `statistics_btn_share_poster` |
| `dialog_` | 弹窗标题、弹窗确认正文、操作提示 | `settings_dialog_theme_confirm` |
| `format_` | 含有格式化占位符的文案 | `statistics_format_day_streak`（如：`坚持 %1$d 天`） |
| `empty_` | 列表或卡片空状态说明 | `collection_empty_wrong_words` |
| `toast_` | 提示条或 Toast 简短反馈 | `settings_toast_export_success` |


---

## 三、多 AI 一键分发提示词（Prompt 模板）

> ⚠️ **给用户的分发说明**：
> 按照您的要求，以下提示词中**已明确指明本文档的路径，绝不把几十个文件路径塞在提示词里**。
> 您只需将对应区块的代码直接复制给对应的 AI（AI A / AI B / AI C / AI D），它们会主动读取本文档中的专属章节并依规执行！


### 📋 【AI A】专属一键复制提示词

```text
你是 Nemo 项目专属重构执行器【AI A】。
本次任务目标：消除你所负责模块内的所有 UI 文本硬编码，将其规范化收编至 strings.xml。

【硬性工作指令】：
1. 请首先完整读取方案文档：docs/audit/01_UI文本硬编码审查与多AI并行整改方案.md
2. 找到文档中专属于你的章节：【第四章 - AI A 专属任务区】。
3. 严格按照该章节列出的待修改文件清单、行号与文案，逐一将硬编码文本提取并追加到对应模块的 strings.xml 中。
4. 严格遵守文档第二章中的【防冲突四大原则】和【Key 命名规范】，严禁跨模块改动，严禁使用外部黑盒脚本，严禁添加口水套话注释。
5. 修改完成后，在终端执行你的专属独立编译验证命令：
   ./gradlew :feature:learning:compileDebugKotlin
   确保编译通过且无任何语法错误。
```

### 📋 【AI B】专属一键复制提示词

```text
你是 Nemo 项目专属重构执行器【AI B】。
本次任务目标：消除你所负责模块内的所有 UI 文本硬编码，将其规范化收编至 strings.xml。

【硬性工作指令】：
1. 请首先完整读取方案文档：docs/audit/01_UI文本硬编码审查与多AI并行整改方案.md
2. 找到文档中专属于你的章节：【第四章 - AI B 专属任务区】。
3. 严格按照该章节列出的待修改文件清单、行号与文案，逐一将硬编码文本提取并追加到对应模块的 strings.xml 中。
4. 严格遵守文档第二章中的【防冲突四大原则】和【Key 命名规范】，严禁跨模块改动，严禁使用外部黑盒脚本，严禁添加口水套话注释。
5. 修改完成后，在终端执行你的专属独立编译验证命令：
   ./gradlew :feature:settings:compileDebugKotlin
   确保编译通过且无任何语法错误。
```

### 📋 【AI C】专属一键复制提示词

```text
你是 Nemo 项目专属重构执行器【AI C】。
本次任务目标：消除你所负责模块内的所有 UI 文本硬编码，将其规范化收编至 strings.xml。

【硬性工作指令】：
1. 请首先完整读取方案文档：docs/audit/01_UI文本硬编码审查与多AI并行整改方案.md
2. 找到文档中专属于你的章节：【第四章 - AI C 专属任务区】。
3. 严格按照该章节列出的待修改文件清单、行号与文案，逐一将硬编码文本提取并追加到对应模块的 strings.xml 中。
4. 严格遵守文档第二章中的【防冲突四大原则】和【Key 命名规范】，严禁跨模块改动，严禁使用外部黑盒脚本，严禁添加口水套话注释。
5. 修改完成后，在终端执行你的专属独立编译验证命令：
   ./gradlew :feature:statistics:compileDebugKotlin
   确保编译通过且无任何语法错误。
```

### 📋 【AI D】专属一键复制提示词

```text
你是 Nemo 项目专属重构执行器【AI D】。
本次任务目标：消除你所负责模块内的所有 UI 文本硬编码，将其规范化收编至 strings.xml。

【硬性工作指令】：
1. 请首先完整读取方案文档：docs/audit/01_UI文本硬编码审查与多AI并行整改方案.md
2. 找到文档中专属于你的章节：【第四章 - AI D 专属任务区】。
3. 严格按照该章节列出的待修改文件清单、行号与文案，逐一将硬编码文本提取并追加到对应模块的 strings.xml 中。
4. 严格遵守文档第二章中的【防冲突四大原则】和【Key 命名规范】，严禁跨模块改动，严禁使用外部黑盒脚本，严禁添加口水套话注释。
5. 修改完成后，在终端执行你的专属独立编译验证命令：
   ./gradlew :feature:library:compileDebugKotlin :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin
   确保编译通过且无任何语法错误。
```

---

## 四、各 AI 专属任务区与详细文件排查清单

### 4.1 【AI A 专属任务区】

- **负责模块**：`:feature:learning`
- **目标资源文件**：`feature/learning/src/main/res/values/strings.xml`
- **涉及文件总数**：`19` 个
- **待整改硬编码总数**：`146` 处
- **独立编译验证命令**：`./gradlew :feature:learning:compileDebugKotlin`

#### 待整改源码文件及详细行号清单：

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/LearningMode.kt` (共 2 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L22 | `OTHER` | `单词` | `is Word -&gt; "单词"` |
| L23 | `OTHER` | `语法` | `is Grammar -&gt; "语法"` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/LearningViewModel.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L262 | `OTHER` | `反馈成功，感谢您的反馈！` | `_uiState.update { it.copy(successMessage = "反馈成功，感谢您的反馈！") }` |
| L266 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L270 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L271 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingHistoryScreen.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L181 | `DATE_FORMAT` | `MM月dd日 HH:mm` | `val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingScreen.kt` (共 22 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
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

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIReadingViewModel.kt` (共 6 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L31 | `OTHER` | `日常生活` | `val readingTheme: String = "日常生活",` |
| L255 | `UI_STATE_OR_PROMPT` | `请先在配置中心添加 AI 模型`<br>`只有一个配置，无法切换` | `it.copy(error = if (configs.isEmpty()) "请先在配置中心添加 AI 模型" else "只有一个配置，无法切换")` |
| L320 | `UI_STATE_OR_PROMPT` | `API 密钥未配置，请前往设置配置！` | `_uiState.update { it.copy(error = "API 密钥未配置，请前往设置配置！") }` |
| L361 | `UI_STATE_OR_PROMPT` | `生成短文失败，请重试: ${error.localizedMessage ?: error.message}` | `error = "生成短文失败，请重试: ${error.localizedMessage ?: error.message}"` |
| L372 | `OTHER` | `API 密钥未配置` | `_uiState.update { it.copy(translationError = "API 密钥未配置") }` |
| L409 | `OTHER` | `翻译失败: ${error.localizedMessage ?: error.message}` | `translationError = "翻译失败: ${error.localizedMessage ?: error.message}"` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIWorkshopScreen.kt` (共 10 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
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

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/AIWorkshopViewModel.kt` (共 5 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L313 | `UI_STATE_OR_PROMPT` | `请先在配置中心添加 AI 模型`<br>`只有一个配置，无法切换` | `it.copy(error = if (configs.isEmpty()) "请先在配置中心添加 AI 模型" else "只有一个配置，无法切换")` |
| L345 | `UI_STATE_OR_PROMPT` | `请先在设置中配置 API Key` | `_uiState.update { it.copy(error = "请先在设置中配置 API Key") }` |
| L396 | `UI_STATE_OR_PROMPT` | `本地暂无 $difficulty 等级的语法数据，请先同步或切换到自由模式` | `error = "本地暂无 $difficulty 等级的语法数据，请先同步或切换到自由模式",` |
| L465 | `UI_STATE_OR_PROMPT` | `获取题目失败: ${e.message}` | `_uiState.update { it.copy(error = "获取题目失败: ${e.message}", isLoading = false) }` |
| L506 | `UI_STATE_OR_PROMPT` | `评分失败: ${e.message}` | `_uiState.update { it.copy(error = "评分失败: ${e.message}", isLoading = false) }` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/components/AIAuroraBackground.kt` (共 5 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L38 | `OTHER` | `经典极光` | `name = "经典极光",` |
| L46 | `OTHER` | `霓虹天际` | `name = "霓虹天际",` |
| L54 | `OTHER` | `极昼破晓` | `name = "极昼破晓",` |
| L62 | `OTHER` | `冰川青碧` | `name = "冰川青碧",` |
| L70 | `OTHER` | `暮色紫霞` | `name = "暮色紫霞",` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/ai/components/CustomTextToolbar.kt` (共 3 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L40 | `OTHER` | `复制` | `menu.add(0, MENU_ITEM_COPY, 0, "复制")` |
| L43 | `OTHER` | `全选` | `menu.add(0, MENU_ITEM_SELECT_ALL, 1, "全选")` |
| L46 | `OTHER` | `翻译` | `menu.add(0, MENU_ITEM_TRANSLATE, 2, "翻译")` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/cards/SRSGrammarCard.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L527 | `OTHER` | `一`<br>`二`<br>`三`<br>`四`<br>`五`<br>`六`<br>`七`<br>`八`<br>`九`<br>`十` | `val chineseNumbers = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/cards/WordCard.kt` (共 16 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L426 | `OTHER` | `动词`<br>`动` | `p.contains("动词") \|\| p.endsWith("动") -&gt;` |
| L430 | `OTHER` | `名词`<br>`名` | `p.contains("名词") \|\| p == "名" -&gt;` |
| L434 | `OTHER` | `形容词` | `p.contains("形容词") && (p.contains("い") \|\| p.contains("1")) -&gt;` |
| L438 | `OTHER` | `形容词`<br>`形动` | `p.contains("形容词") \|\| p.contains("形动") \|\| p.contains("な") -&gt;` |
| L442 | `OTHER` | `副词`<br>`副` | `p.contains("副词") \|\| p == "副" -&gt;` |
| L446 | `OTHER` | `接续` | `p.contains("接续") -&gt;` |
| L450 | `OTHER` | `连体` | `p.contains("连体") -&gt;` |
| L454 | `OTHER` | `外来`<br>`片假名` | `p.contains("外来") \|\| p.contains("片假名") -&gt;` |
| L458 | `OTHER` | `熟语`<br>`惯用` | `p.contains("熟语") \|\| p.contains("惯用") -&gt;` |
| L462 | `OTHER` | `敬语` | `p.contains("敬语") -&gt;` |
| L466 | `OTHER` | `声`<br>`态` | `p.contains("声") \|\| p.contains("态") -&gt;` |
| L470 | `OTHER` | `固定`<br>`句` | `p.contains("固定") \|\| p.contains("句") -&gt;` |
| L487 | `OTHER` | `食べる` | `japanese = "食べる",` |
| L489 | `OTHER` | `吃` | `chinese = "吃",` |
| L491 | `OTHER` | `动词` | `pos = "动词",` |
| L492 | `OTHER` | `ご飯を食べる` | `example1 = "ご飯を食べる",` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/common/LearningFinishedContent.kt` (共 2 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L98 | `UI_STATE_OR_PROMPT` | `今日任务达成！` | `title: String = "今日任务达成！",` |
| L99 | `UI_STATE_OR_PROMPT` | `坚持就是胜利，明天继续加油` | `subtitle: String = "坚持就是胜利，明天继续加油",` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/srs/SRSActionArea.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L195 | `OTHER` | `显示答案`<br>`显示答案 ($countdownText)` | `text = if (canShowAnswer) "显示答案" else "显示答案 ($countdownText)",` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/CardOpsDelegate.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L34 | `OTHER` | `已埋藏顽固卡片: ${item.displayName} (今日不再出现，明日自动回归)` | `buryItem(item, "已埋藏顽固卡片: ${item.displayName} (今日不再出现，明日自动回归)")` |
| L38 | `UI_STATE_OR_PROMPT` | `已冻结顽固卡片: ${item.displayName} (累计遗忘 $totalLapses 次，已移出复习)` | `it.copy(error = "已冻结顽固卡片: ${item.displayName} (累计遗忘 $totalLapses 次，已移出复习)")` |
| L56 | `UI_STATE_OR_PROMPT` | `已暂停: ${currentItem.displayName}` | `it.copy(error = "已暂停: ${currentItem.displayName}")` |
| L67 | `OTHER` | `已暂缓: ${currentItem.displayName} (今日不再出现)` | `buryItem(currentItem, "已暂缓: ${currentItem.displayName} (今日不再出现)")` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/RatingDelegate.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L187 | `UI_STATE_OR_PROMPT` | `评分异常: ${e.message}` | `error = "评分异常: ${e.message}"` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/SessionLifecycleDelegate.kt` (共 7 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L140 | `UI_STATE_OR_PROMPT` | `启动失败: ${e.message}` | `error = "启动失败: ${e.message}"` |
| L183 | `OTHER` | `词`<br>`语法` | `val unit = if (mode == LearningMode.Word) "词" else "语法"` |
| L220 | `OTHER` | `太棒了！当前待复习积压已全部出清` | `BonusMode.ReviewBacklog -&gt; "太棒了！当前待复习积压已全部出清"` |
| L221 | `OTHER` | `暂无更多未学新$unit` | `BonusMode.NewItems -&gt; "暂无更多未学新$unit"` |
| L233 | `OTHER` | `待复习`<br>`新` | `val modeLabel = if (bonusMode == BonusMode.ReviewBacklog) "待复习" else "新"` |
| L235 | `OTHER` | `已为你加载剩余全部 ${bonusItems.size} 个$modeLabel$unit` | `it.copy(successMessage = "已为你加载剩余全部 ${bonusItems.size} 个$modeLabel$unit")` |
| L287 | `UI_STATE_OR_PROMPT` | `启动加餐失败: ${e.message}` | `error = "启动加餐失败: ${e.message}"` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/UndoDelegate.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L160 | `UI_STATE_OR_PROMPT` | `撤销失败: ${e.message}` | `error = "撤销失败: ${e.message}"` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/home/components/HomeComponents.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L50 | `OTHER` | `銉嶃儮` | `bgText: String = "銉嶃儮",` |

##### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/verb/VerbConjugationHandbookScreen.kt` (共 54 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L153 | `OTHER` | `动词在字典里的原始形态。用于普通体的现在时/将来时，或连接特定语法（如「～ことができる」）。` | `usage = "动词在字典里的原始形态。用于普通体的现在时/将来时，或连接特定语法（如「～ことができる」）。",` |
| L160 | `OTHER` | `一类动词 (五段)`<br>`词尾必定是「う」段假名。`<br>`書く (かく / 写)`<br>`飲む (のむ / 喝)` | `GrammarRule("一类动词 (五段)", "词尾必定是「う」段假名。", listOf("書く (かく / 写)", "飲む (のむ / 喝)")),` |
| L161 | `OTHER` | `二类动词 (一段)`<br>`词尾必定是「る」，且倒数第二个音在「い」或「え」段。`<br>`食べる (たべる / 吃)`<br>`見る (みる / 看)` | `GrammarRule("二类动词 (一段)", "词尾必定是「る」，且倒数第二个音在「い」或「え」段。", listOf("食べる (たべる / 吃)", "見る (み...` |
| L162 | `OTHER` | `三类动词 (不规则)`<br>`只有两个特定动词。`<br>`する (做)`<br>`来る (くる / 来)` | `GrammarRule("三类动词 (不规则)", "只有两个特定动词。", listOf("する (做)", "来る (くる / 来)"))` |
| L169 | `OTHER` | `日语中最基础、最礼貌的表达方式（敬体）。` | `usage = "日语中最基础、最礼貌的表达方式（敬体）。",` |
| L176 | `OTHER` | `一类动词`<br>`词尾「う」段假名变同行「い」段假名 + ます。`<br>`書く → 書きます (かきます)`<br>`飲む → 飲みます (のみます)` | `GrammarRule("一类动词", "词尾「う」段假名变同行「い」段假名 + ます。", listOf("書く → 書きます (かきます)", "飲む → 飲みます ...` |
| L177 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + ます。`<br>`食べる → 食べます (たべます)`<br>`見る → 見ます (みます)` | `GrammarRule("二类动词", "去掉词尾的「る」 + ます。", listOf("食べる → 食べます (たべます)", "見る → 見ます (みます)")),` |
| L178 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来ます (きます)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → します", "来る → 来ます (きます)"))` |
| L185 | `OTHER` | `用于表示动作的先后顺序、原因、中顿，或与「～てください」连用表示请求。一类动词有音便现象。` | `usage = "用于表示动作的先后顺序、原因、中顿，或与「～てください」连用表示请求。一类动词有音便现象。",` |
| L193 | `OTHER` | `一类动词` | `"一类动词",` |
| L194 | `OTHER` | `【促音便】う,つ,る结尾 → って\n【拨音便】ぬ,ぶ,む结尾 → んで\n【イ音便】く,ぐ结尾 → いて/いで\n【特殊】す结尾 → して。行く(いく) → 行って(いって)` | `"【促音便】う,つ,る结尾 → って\n【拨音便】ぬ,ぶ,む结尾 → んで\n【イ音便】く,ぐ结尾 → いて/いで\n【特殊】す结尾 → して。行く(いく) → 行って(...` |
| L195 | `OTHER` | `買う(かう) → 買って(かって)`<br>`飲む(のむ) → 飲んで(のんで)`<br>`書く(かく) → 書いて(かいて)`<br>`話す(はなす) → 話して(はなして)` | `listOf("買う(かう) → 買って(かって)", "飲む(のむ) → 飲んで(のんで)", "書く(かく) → 書いて(かいて)", "話す(はなす) → 話して(...` |
| L197 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + て。`<br>`食べる → 食べて (たべて)`<br>`見る → 見て (みて)` | `GrammarRule("二类动词", "去掉词尾的「る」 + て。", listOf("食べる → 食べて (たべて)", "見る → 見て (みて)")),` |
| L198 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来て (きて)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → して", "来る → 来て (きて)"))` |
| L205 | `OTHER` | `表达动作已经发生（普通体的过去式）。变形规则与「て形」完全一致，只需把て/で换成た/だ。` | `usage = "表达动作已经发生（普通体的过去式）。变形规则与「て形」完全一致，只需把て/で换成た/だ。",` |
| L212 | `OTHER` | `一类动词`<br>`依照「て形」的音便规则，将尾音换成 た/だ。`<br>`買う → 買った (かった)`<br>`飲む → 飲んだ (のんだ)`<br>`書く → 書いた (かいた)` | `GrammarRule("一类动词", "依照「て形」的音便规则，将尾音换成 た/だ。", listOf("買う → 買った (かった)", "飲む → 飲んだ (のんだ...` |
| L213 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + た。`<br>`食べる → 食べた (たべた)`<br>`見る → 見た (みた)` | `GrammarRule("二类动词", "去掉词尾的「る」 + た。", listOf("食べる → 食べた (たべた)", "見る → 見た (みた)")),` |
| L214 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来た (きた)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → した", "来る → 来た (きた)"))` |
| L221 | `OTHER` | `表达否定意义（普通体），或连接「～ない对ください」（请不要做...）。` | `usage = "表达否定意义（普通体），或连接「～ない对ください」（请不要做...）。",` |
| L228 | `OTHER` | `一类动词`<br>`词尾「う」段变同行「あ」段 + ない。（※注意：词尾为「う」时变「わ」，不是「あ」）`<br>`書く → 書かない (かかない)`<br>`買う → 買わない (かわない / 特殊)` | `GrammarRule("一类动词", "词尾「う」段变同行「あ」段 + ない。（※注意：词尾为「う」时变「わ」，不是「あ」）", listOf("書く → 書かない (...` |
| L229 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + ない。`<br>`食べる → 食べない (たべない)`<br>`見る → 見ない (みない)` | `GrammarRule("二类动词", "去掉词尾的「る」 + ない。", listOf("食べる → 食べない (たべない)", "見る → 見ない (みない)")),` |
| L230 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来ない (こない)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → しない", "来る → 来ない (こない)"))` |
| L237 | `OTHER` | `表达说话人的决心、意愿，或提议“一起做某事吧”。` | `usage = "表达说话人的决心、意愿，或提议“一起做某事吧”。",` |
| L244 | `OTHER` | `一类动词`<br>`词尾「う」段变同行「お」段 + う。`<br>`書く → 書こう (かこう)`<br>`行く → 行こう (いこう)` | `GrammarRule("一类动词", "词尾「う」段变同行「お」段 + う。", listOf("書く → 書こう (かこう)", "行く → 行こう (いこう)"))...` |
| L245 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + よう。`<br>`食べる → 食べよう (たべよう)`<br>`見る → 見よう (みよう)` | `GrammarRule("二类动词", "去掉词尾的「る」 + よう。", listOf("食べる → 食べよう (たべよう)", "見る → 見よう (みよう)")),` |
| L246 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来よう (こよう)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → しよう", "来る → 来よう (こよう)"))` |
| L253 | `OTHER` | `强硬地命令他人做某事，常用于男性粗鲁用语、标语、紧急情况。` | `usage = "强硬地命令他人做某事，常用于男性粗鲁用语、标语、紧急情况。",` |
| L260 | `OTHER` | `一类动词`<br>`词尾「う」段变同行「え」段。`<br>`書く → 書け (かけ)`<br>`行く → 行け (いけ)` | `GrammarRule("一类动词", "词尾「う」段变同行「え」段。", listOf("書く → 書け (かけ)", "行く → 行け (いけ)")),` |
| L261 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + ろ。`<br>`食べる → 食べろ (たべろ)`<br>`見る → 見ろ (みろ)` | `GrammarRule("二类动词", "去掉词尾的「る」 + ろ。", listOf("食べる → 食べろ (たべろ)", "見る → 見ろ (みろ)")),` |
| L262 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来い (こい)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → しろ / せよ", "来る → 来い (こい)"))` |
| L269 | `OTHER` | `强硬地命令他人“不准做某事”。` | `usage = "强硬地命令他人“不准做某事”。",` |
| L276 | `OTHER` | `所有动词`<br>`直接在【辞书形 (原形)】后 + な。`<br>`書く → 書くな (かくな)`<br>`食べる → 食べるな (たべるな)`<br>`来る → 来るな (くるな)` | `GrammarRule("所有动词", "直接在【辞书形 (原形)】后 + な。", listOf("書く → 書くな (かくな)", "食べる → 食べるな (たべるな...` |
| L283 | `OTHER` | `表示假定条件，即“如果……的话”。` | `usage = "表示假定条件，即“如果……的话”。",` |
| L290 | `OTHER` | `一类动词`<br>`词尾「う」段变同行「え」段 + ば。`<br>`書く → 書けば (かけば)`<br>`行く → 行けば (いけば)` | `GrammarRule("一类动词", "词尾「う」段变同行「え」段 + ば。", listOf("書く → 書けば (かけば)", "行く → 行けば (いけば)"))...` |
| L291 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + れば。`<br>`食べる → 食べれば (たべれば)`<br>`見る → 見れば (みれば)` | `GrammarRule("二类动词", "去掉词尾的「る」 + れば。", listOf("食べる → 食べれば (たべれば)", "見る → 見れば (みれば)")),` |
| L292 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来れば (くれば)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → すれば", "来る → 来れば (くれば)"))` |
| L299 | `OTHER` | `表示能力或客观条件允许，即“能够做某事”。` | `usage = "表示能力或客观条件允许，即“能够做某事”。",` |
| L306 | `OTHER` | `一类动词`<br>`词尾「う」段变同行「え」段 + る。`<br>`書く → 書ける (かける)`<br>`話す → 話せる (はなせる)` | `GrammarRule("一类动词", "词尾「う」段变同行「え」段 + る。", listOf("書く → 書ける (かける)", "話す → 話せる (はなせる)")...` |
| L307 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + られる。`<br>`食べる → 食べられる`<br>`見る → 見られる` | `GrammarRule("二类动词", "去掉词尾的「る」 + られる。", listOf("食べる → 食べられる", "見る → 見られる")),` |
| L308 | `OTHER` | `三类动词`<br>`特殊记忆。する 的可能形是一个全新的词。`<br>`来る → 来られる (こられる)` | `GrammarRule("三类动词", "特殊记忆。する 的可能形是一个全新的词。", listOf("する → できる", "来る → 来られる (こられる)"))` |
| L315 | `OTHER` | `表示动作的承受者（被做某事），也可表示受害或间接尊敬。` | `usage = "表示动作的承受者（被做某事），也可表示受害或间接尊敬。",` |
| L322 | `OTHER` | `一类动词`<br>`词尾「う」段变同行「あ」段 + れる。（※尾音「う」变「わ」）`<br>`書く → 書かれる (かかれる)`<br>`言う → 言われる (いわれる)` | `GrammarRule("一类动词", "词尾「う」段变同行「あ」段 + れる。（※尾音「う」变「わ」）", listOf("書く → 書かれる (かかれる)", "言う...` |
| L323 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + られる。（形式与可能形完全一致）`<br>`食べる → 食べられる`<br>`見る → 見られる` | `GrammarRule("二类动词", "去掉词尾的「る」 + られる。（形式与可能形完全一致）", listOf("食べる → 食べられる", "見る → 見られる")...` |
| L324 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来られる (こられる)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → される", "来る → 来られる (こられる)"))` |
| L331 | `OTHER` | `表示让某人做某事，或者允许某人做某事。` | `usage = "表示让某人做某事，或者允许某人做某事。",` |
| L338 | `OTHER` | `一类动词`<br>`词尾「う」段变同行「あ」段 + せる。（※尾音「う」变「わ」）`<br>`書く → 書かせる (かかせる)`<br>`買う → 買わせる (かわせる)` | `GrammarRule("一类动词", "词尾「う」段变同行「あ」段 + せる。（※尾音「う」变「わ」）", listOf("書く → 書かせる (かかせる)", "買う...` |
| L339 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + させる。`<br>`食べる → 食べさせる`<br>`見る → 見させる` | `GrammarRule("二类动词", "去掉词尾的「る」 + させる。", listOf("食べる → 食べさせる", "見る → 見させる")),` |
| L340 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来させる (こさせる)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → させる", "来る → 来させる (こさせる)"))` |
| L347 | `OTHER` | `表示被迫做某事（“被逼着不得不做……”），是使役形 + 被动形的结合。` | `usage = "表示被迫做某事（“被逼着不得不做……”），是使役形 + 被动形的结合。",` |
| L355 | `OTHER` | `一类动词` | `"一类动词",` |
| L356 | `OTHER` | `【小白必杀技】把「ない形」的「ない」去掉，换成「される」即可。（※词尾是「す」时只能加「させられる」）` | `"【小白必杀技】把「ない形」的「ない」去掉，换成「される」即可。（※词尾是「す」时只能加「させられる」）",` |
| L357 | `OTHER` | `書く → [書かない] → 書かされる (被逼写)`<br>`話す → [話さない] → 話させられる (特殊)` | `listOf("書く → [書かない] → 書かされる (被逼写)", "話す → [話さない] → 話させられる (特殊)")` |
| L359 | `OTHER` | `二类动词`<br>`去掉词尾的「る」 + させられる。`<br>`食べる → 食べさせられる (被逼着吃)` | `GrammarRule("二类动词", "去掉词尾的「る」 + させられる。", listOf("食べる → 食べさせられる (被逼着吃)")),` |
| L360 | `OTHER` | `三类动词`<br>`特殊记忆。`<br>`来る → 来させられる (こさせられる)` | `GrammarRule("三类动词", "特殊记忆。", listOf("する → させられる", "来る → 来させられる (こさせられる)"))` |

---

### 4.2 【AI B 专属任务区】

- **负责模块**：`:feature:settings`
- **目标资源文件**：`feature/settings/src/main/res/values/strings.xml`
- **涉及文件总数**：`16` 个
- **待整改硬编码总数**：`159` 处
- **独立编译验证命令**：`./gradlew :feature:settings:compileDebugKotlin`

#### 待整改源码文件及详细行号清单：

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AboutViewModel.kt` (共 2 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L51 | `OTHER` | `暂无云端更新日志` | `errorMessage = if (config == null) "暂无云端更新日志" else null` |
| L58 | `OTHER` | `获取更新日志失败` | `errorMessage = e.message ?: "获取更新日志失败"` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AdvancedLearningSettingsScreen.kt` (共 10 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L226 | `COMPOSE_UI` | `新卡片初学阶梯` | `title = "新卡片初学阶梯",` |
| L227 | `COMPOSE_UI` | `新知识首次记忆时的短时巩固序列 (单位: 分钟)` | `subtitle = "新知识首次记忆时的短时巩固序列 (单位: 分钟)",` |
| L235 | `COMPOSE_UI` | `遗忘重学阶梯` | `title = "遗忘重学阶梯",` |
| L236 | `COMPOSE_UI` | `遗忘卡片重新激活时的强化复习序列 (单位: 分钟)` | `subtitle = "遗忘卡片重新激活时的强化复习序列 (单位: 分钟)",` |
| L288 | `COMPOSE_UI` | `记忆算法调校` | `title = "记忆算法调校",` |
| L315 | `COMPOSE_UI` | `恢复默认设置` | `contentDescription = "恢复默认设置",` |
| L340 | `COMPOSE_UI` | `恢复默认算法配置` | `title = "恢复默认算法配置",` |
| L341 | `OTHER` | `确认恢复` | `confirmText = "确认恢复",` |
| L342 | `OTHER` | `取消` | `dismissText = "取消",` |
| L362 | `OTHER` | `确定要将记忆算法配置恢复为系统黄金推荐值吗？` | `text = "确定要将记忆算法配置恢复为系统黄金推荐值吗？",` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AIModelStudioScreen.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L146 | `OTHER` | `配置`<br>`模型` | `val nextName = if (config.name.isBlank() \|\| config.name.endsWith("配置") \|\| config....` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AISettingsViewModel.kt` (共 18 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L88 | `OTHER` | `OpenAI 默认配置` | `"openai" -&gt; "OpenAI 默认配置"` |
| L89 | `OTHER` | `Gemini 默认配置` | `"gemini" -&gt; "Gemini 默认配置"` |
| L90 | `OTHER` | `DeepSeek 默认配置` | `"deepseek" -&gt; "DeepSeek 默认配置"` |
| L91 | `OTHER` | `自定义 默认配置` | `else -&gt; "自定义 默认配置"` |
| L165 | `OTHER` | `未命名配置` | `val finalName = editing.name.trim().ifEmpty { "未命名配置" }` |
| L173 | `OTHER` | `配置别名已存在，请换一个别名` | `_uiState.update { it.copy(testResult = AITestResult(false, "配置别名已存在，请换一个别名")) }` |
| L180 | `OTHER` | `API 密钥已配置过，请勿重复添加` | `_uiState.update { it.copy(testResult = AITestResult(false, "API 密钥已配置过，请勿重复添加")) }` |
| L210 | `OTHER` | `API Key 不能为空` | `_uiState.update { it.copy(testResult = AITestResult(false, "API Key 不能为空")) }` |
| L215 | `OTHER` | `未命名配置` | `val finalName = editing.name.trim().ifEmpty { "未命名配置" }` |
| L223 | `OTHER` | `配置别名已存在，请换一个别名` | `_uiState.update { it.copy(testResult = AITestResult(false, "配置别名已存在，请换一个别名")) }` |
| L230 | `OTHER` | `API 密钥已配置过，请勿重复添加` | `_uiState.update { it.copy(testResult = AITestResult(false, "API 密钥已配置过，请勿重复添加")) }` |
| L249 | `OTHER` | `连接成功` | `AITestResult(true, "连接成功")` |
| L251 | `OTHER` | `未知错误` | `val msg = result.exceptionOrNull()?.message ?: "未知错误"` |
| L262 | `OTHER` | `未命名配置` | `val finalName = editing.name.trim().ifEmpty { "未命名配置" }` |
| L281 | `OTHER` | `API Key 为空，无法测试` | `_uiState.update { it.copy(testResult = AITestResult(false, "API Key 为空，无法测试")) }` |
| L297 | `OTHER` | `「${config.name}」API 连接正常 · 测试通过` | `AITestResult(true, "「${config.name}」API 连接正常 · 测试通过")` |
| L299 | `OTHER` | `连接失败` | `val msg = result.exceptionOrNull()?.message ?: "连接失败"` |
| L300 | `OTHER` | `「${config.name}」测试失败: $msg` | `AITestResult(false, "「${config.name}」测试失败: $msg")` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/CloudBackupHistoryScreen.kt` (共 3 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L81 | `OTHER` | `失败`<br>`异常` | `val isSuccess = !it.contains("失败") && !it.contains("异常")` |
| L82 | `OTHER` | `导入成功` | `val isRestoreSuccess = isSuccess && it.contains("导入成功")` |
| L494 | `DATE_FORMAT` | `yyyy年MM月dd日 HH:mm:ss` | `val formatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/LiquidGlassLabScreen.kt` (共 12 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L98 | `COMPOSE_UI` | `液态玻璃实验室` | `title = "液态玻璃实验室",` |
| L130 | `OTHER` | `按住按钮可自由拖拽到不同背景上对比透镜折射` | `text = "按住按钮可自由拖拽到不同背景上对比透镜折射",` |
| L149 | `OTHER` | `全透明液态按钮` | `"全透明液态按钮",` |
| L167 | `OTHER` | `带底色液态按钮` | `"带底色液态按钮",` |
| L186 | `OTHER` | `彩色液态按钮 (蓝)` | `"彩色液态按钮 (蓝)",` |
| L205 | `OTHER` | `彩色液态按钮 (橙)` | `"彩色液态按钮 (橙)",` |
| L226 | `COMPOSE_UI` | `返回` | `contentDescription = "返回",` |
| L368 | `OTHER` | `物理透镜光学折射样本` | `text = "物理透镜光学折射样本",` |
| L396 | `OTHER` | `リアルタイム屈折レンダリング` | `text = "リアルタイム屈折レンダリング",` |
| L426 | `OTHER` | `透镜厚度` | `text = "透镜厚度",` |
| L438 | `OTHER` | `折射范围` | `text = "折射范围",` |
| L450 | `OTHER` | `实时帧率` | `text = "实时帧率",` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/SettingsScreen.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L84 | `UI_STATE_OR_PROMPT` | `失败`<br>`异常` | `snackbarType = if (it.contains("失败") \|\| it.contains("异常")) NemoSnackbarType.ERROR e...` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/SettingsViewModel.kt` (共 14 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L308 | `OTHER` | `基础词库正在准备中，请稍候恢复` | `_uiState.update { it.copy(toastMessage = "基础词库正在准备中，请稍候恢复") }` |
| L353 | `OTHER` | `导入过程中发生异常: ${e.message}` | `_uiState.update { it.copy(toastMessage = "导入过程中发生异常: ${e.message}") }` |
| L366 | `OTHER` | `备份过于频繁，请稍后再试` | `_uiState.update { it.copy(toastMessage = "备份过于频繁，请稍后再试") }` |
| L375 | `OTHER` | `云端备份成功` | `_uiState.update { it.copy(toastMessage = "云端备份成功") }` |
| L378 | `OTHER` | `备份失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "备份失败: ${e.message}") }` |
| L395 | `OTHER` | `备份列表已刷新` | `toastMessage = "备份列表已刷新"` |
| L400 | `OTHER` | `获取备份列表失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "获取备份列表失败: ${e.message}") }` |
| L422 | `OTHER` | `预览失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "预览失败: ${e.message}", pendingRestoreFileName...` |
| L435 | `OTHER` | `基础词库正在准备中，请稍候恢复` | `_uiState.update { it.copy(toastMessage = "基础词库正在准备中，请稍候恢复") }` |
| L443 | `OTHER` | `失败`<br>`异常` | `val isSuccess = !message.contains("失败") && !message.contains("异常")` |
| L447 | `OTHER` | `导入成功` | `restoreSuccessMessage = if (isSuccess && message.contains("导入成功")) message else null` |
| L452 | `OTHER` | `恢复失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "恢复失败: ${e.message}", restoreSuccessMessage ...` |
| L539 | `OTHER` | `每日目标已更新，将在下一个学习日生效` | `toastMessage = "每日目标已更新，将在下一个学习日生效"` |
| L554 | `OTHER` | `每日语法目标已更新，将在下一个学习日生效` | `toastMessage = "每日语法目标已更新，将在下一个学习日生效"` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/ThemeSettingsScreen.kt` (共 12 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L786 | `COMPOSE_UI` | `跟随系统` | `title = "跟随系统",` |
| L787 | `COMPOSE_UI` | `根据系统设置自动切换深浅色主题` | `subtitle = "根据系统设置自动切换深浅色主题",` |
| L792 | `COMPOSE_UI` | `定时深色模式` | `title = "定时深色模式",` |
| L793 | `COMPOSE_UI` | `在指定时间段开启深色模式` | `subtitle = "在指定时间段开启深色模式",` |
| L808 | `COMPOSE_UI` | `跟随系统` | `title = "跟随系统",` |
| L809 | `COMPOSE_UI` | `根据系统设置自动切换深浅色主题` | `subtitle = "根据系统设置自动切换深浅色主题",` |
| L814 | `COMPOSE_UI` | `定时深色模式` | `title = "定时深色模式",` |
| L815 | `COMPOSE_UI` | `在指定时间段开启深色模式` | `subtitle = "在指定时间段开启深色模式",` |
| L831 | `OTHER` | `经典圆角彩色背景底框，单色聚焦图标` | `description = "经典圆角彩色背景底框，单色聚焦图标",` |
| L838 | `OTHER` | `现代无背景通透设计，双色线面与等亮度调色板` | `description = "现代无背景通透设计，双色线面与等亮度调色板",` |
| L854 | `OTHER` | `经典圆角彩色背景底框，单色聚焦图标` | `description = "经典圆角彩色背景底框，单色聚焦图标",` |
| L861 | `OTHER` | `现代无背景通透设计，双色线面与等亮度调色板` | `description = "现代无背景通透设计，双色线面与等亮度调色板",` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/TtsSettingsScreen.kt` (共 3 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L214 | `OTHER` | `这是日语语音测试。`<br>`これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewTts("这是日语语音测试。".let { "これは音声テストです。" }))` |
| L286 | `OTHER` | `これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewTts("これは音声テストです。"))` |
| L289 | `OTHER` | `これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewVoice(voiceName, "これは音声テストです。"))` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/AIModelStudioComponents.kt` (共 14 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L168 | `OTHER` | `深度求索 · V4 旗舰推理与高性价比` | `tagline = "深度求索 · V4 旗舰推理与高性价比",` |
| L176 | `OTHER` | `豆包` | `name = "豆包",` |
| L177 | `OTHER` | `字节跳动 · Seed 2.1 旗舰与 Evolving 持续演进` | `tagline = "字节跳动 · Seed 2.1 旗舰与 Evolving 持续演进",` |
| L185 | `OTHER` | `智谱 GLM` | `name = "智谱 GLM",` |
| L186 | `OTHER` | `智谱 AI · GLM-5.3 旗舰推理与 Agent 编码` | `tagline = "智谱 AI · GLM-5.3 旗舰推理与 Agent 编码",` |
| L195 | `OTHER` | `Moonshot AI · K3 旗舰 1M 多模态与长程编程` | `tagline = "Moonshot AI · K3 旗舰 1M 多模态与长程编程",` |
| L204 | `OTHER` | `小米大模型 · V2.6 旗舰推理与端云协同` | `tagline = "小米大模型 · V2.6 旗舰推理与端云协同",` |
| L212 | `OTHER` | `通义千问` | `name = "通义千问",` |
| L213 | `OTHER` | `阿里云百炼 · qwen3.8-max 旗舰与双轨生态` | `tagline = "阿里云百炼 · qwen3.8-max 旗舰与双轨生态",` |
| L224 | `OTHER` | `Anthropic · 5.x 旗舰体系与自适应思考链` | `tagline = "Anthropic · 5.x 旗舰体系与自适应思考链",` |
| L233 | `OTHER` | `GPT-6 旗舰梯队 · Astra 深度推理与 Sol 智能体` | `tagline = "GPT-6 旗舰梯队 · Astra 深度推理与 Sol 智能体",` |
| L242 | `OTHER` | `Google DeepMind · Argon 旗舰推理与百万上下文` | `tagline = "Google DeepMind · Argon 旗舰推理与百万上下文",` |
| L250 | `OTHER` | `自定义` | `name = "自定义",` |
| L251 | `OTHER` | `接入任意兼容 OpenAI 协议的接口与模型` | `tagline = "接入任意兼容 OpenAI 协议的接口与模型",` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/BacklogControllerCard.kt` (共 5 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L165 | `OTHER` | `自定义` | `text = if (isCustomBatch) "$sessionBatchSize" else "自定义",` |
| L203 | `OTHER` | `不限`<br>`40 词`<br>`60 词`<br>`80 词` | `val reviewLimitOptions = listOf(0 to "不限", 40 to "40 词", 60 to "60 词", 80 to "80 词")` |
| L265 | `OTHER` | `$maxDailyReviews 词`<br>`自定义` | `text = if (isCustomReviewLimit) "$maxDailyReviews 词" else "自定义",` |
| L330 | `OTHER` | `$it 词` | `presetLabelFormatter = { "$it 词" },` |
| L353 | `OTHER` | `不限`<br>`$it 词` | `presetLabelFormatter = { if (it == 0) "不限" else "$it 词" },` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/MemoryAlgorithmComponents.kt` (共 16 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L157 | `OTHER` | `清爽微负荷` | `targetRetention &lt; 0.82f -&gt; "清爽微负荷" to SettingsPalette.CyanMint` |
| L158 | `OTHER` | `黄金平衡态` | `targetRetention &lt;= 0.91f -&gt; "黄金平衡态" to IosColors.Purple` |
| L159 | `OTHER` | `极客高密度` | `else -&gt; "极客高密度" to IosColors.OrangeDark` |
| L565 | `OTHER` | `80% 宽松`<br>`85% 平衡`<br>`90% 黄金`<br>`95% 冲刺` | `listOf(0.80f to "80% 宽松", 0.85f to "85% 平衡", 0.90f to "90% 黄金", 0.95f to "95% 冲刺").fo...` |
| L667 | `COMPOSE_UI` | `添加阶段` | `contentDescription = "添加阶段",` |
| L673 | `OTHER` | `增加阶梯` | `text = "增加阶梯",` |
| L706 | `COMPOSE_UI` | `新增复习阶段` | `title = "新增复习阶段",` |
| L773 | `COMPOSE_UI` | `删除该阶段` | `contentDescription = "删除该阶段",` |
| L790 | `UI_STATE_OR_PROMPT` | `调整复习阶段时长` | `title: String = "调整复习阶段时长",` |
| L791 | `UI_STATE_OR_PROMPT` | `设定记忆间隔时长（支持分位加减与手写输入）` | `subtitle: String = "设定记忆间隔时长（支持分位加减与手写输入）",` |
| L809 | `OTHER` | `确认保存` | `confirmText = "确认保存",` |
| L820 | `OTHER` | `$step 分钟` | `val mins = step.toIntOrNull() ?: return "$step 分钟"` |
| L822 | `OTHER` | `$mins 分钟` | `mins &lt; 60 -&gt; "$mins 分钟"` |
| L823 | `OTHER` | `${mins / 60} 小时` | `mins &lt; 1440 -&gt; "${mins / 60} 小时"` |
| L824 | `OTHER` | `${mins / 1440} 天` | `else -&gt; "${mins / 1440} 天"` |
| L930 | `OTHER` | `$count 次` | `text = "$count 次",` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/RestoreSuccessDialog.kt` (共 12 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L55 | `OTHER` | `单词:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)` | `val wordRegex = Regex("""单词:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)""")` |
| L63 | `OTHER` | `语法:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)` | `val grammarRegex = Regex("""语法:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)""")` |
| L71 | `OTHER` | `校验提示:` | `if (message.contains("校验提示:")) {` |
| L72 | `OTHER` | `校验提示:` | `validationNote = message.substringAfter("校验提示:").trim()` |
| L168 | `OTHER` | `档案恢复成功` | `text = "档案恢复成功",` |
| L178 | `OTHER` | `云端学习档案已安全同步至本机` | `text = "云端学习档案已安全同步至本机",` |
| L200 | `OTHER` | `词库档案` | `text = "词库档案",` |
| L206 | `OTHER` | `更新: ${stats.wordUpdate}  /  新增: ${stats.wordInsert}  /  跳过: ${stats.wordSkip}` | `text = "更新: ${stats.wordUpdate}  /  新增: ${stats.wordInsert}  /  跳过: ${stats.wordSkip}...` |
| L220 | `OTHER` | `语法档案` | `text = "语法档案",` |
| L226 | `OTHER` | `更新: ${stats.grammarUpdate}  /  新增: ${stats.grammarInsert}  /  跳过: ${stats.grammarSkip}` | `text = "更新: ${stats.grammarUpdate}  /  新增: ${stats.grammarInsert}  /  跳过: ${stats.gra...` |
| L239 | `OTHER` | `校验说明: $note` | `text = "校验说明: $note",` |
| L263 | `OTHER` | `开启学习之旅`<br>`完成` | `text = if (isFromLogin) "开启学习之旅" else "完成",` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/SettingsComponents.kt` (共 20 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L340 | `OTHER` | `每日单词目标` | `text = "每日单词目标",` |
| L355 | `OTHER` | `$goal 个单词` | `text = "$goal 个单词",` |
| L408 | `OTHER` | `每日语法目标` | `text = "每日语法目标",` |
| L423 | `OTHER` | `$goal 条语法` | `text = "$goal 条语法",` |
| L520 | `OTHER` | `学习日重置时间` | `text = "学习日重置时间",` |
| L526 | `OTHER` | `每天此时间后开始新的学习日` | `text = "每天此时间后开始新的学习日",` |
| L567 | `OTHER` | `凌晨 0:00 (午夜)` | `0 -&gt; "凌晨 0:00 (午夜)"` |
| L568 | `OTHER` | `凌晨 2:00` | `2 -&gt; "凌晨 2:00"` |
| L569 | `OTHER` | `凌晨 4:00 (推荐)` | `4 -&gt; "凌晨 4:00 (推荐)"` |
| L570 | `OTHER` | `凌晨 5:00` | `5 -&gt; "凌晨 5:00"` |
| L571 | `OTHER` | `凌晨 6:00` | `6 -&gt; "凌晨 6:00"` |
| L626 | `OTHER` | `云端语音`<br>`本地语音` | `if (voice.name.lowercase().contains("network")) "云端语音" else "本地语音"` |
| L658 | `OTHER` | `选择语音` | `text = "选择语音",` |
| L687 | `OTHER` | `未找到可用语音` | `text = "未找到可用语音",` |
| L841 | `OTHER` | `云端`<br>`本地` | `val tag = if (isNetwork) "云端" else "本地"` |
| L851 | `OTHER` | `系统默认` | `rawName.contains("language") -&gt; "系统默认"` |
| L852 | `OTHER` | `日语语音 $coreId` | `coreId.isNotEmpty() -&gt; "日语语音 $coreId"` |
| L853 | `OTHER` | `语音 ${coreId}` | `else -&gt; "语音 ${coreId}"` |
| L858 | `COMPOSE_UI` | `高质量` | `subtitle = if (voice.quality == "high" \|\| voice.quality == "very_high") "高质量" else ...` |
| L964 | `COMPOSE_UI` | `试听` | `contentDescription = "试听",` |

##### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/ThemeColorDialog.kt` (共 16 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L103 | `OTHER` | `默认` | `ThemeColor("默认", UserPalette.PrimaryGlassTintLight),` |
| L104 | `OTHER` | `蔷薇红` | `ThemeColor("蔷薇红", IosColors.Pink),` |
| L105 | `OTHER` | `活力橙` | `ThemeColor("活力橙", IosColors.Orange),` |
| L106 | `OTHER` | `明快黄` | `ThemeColor("明快黄", IosColors.Yellow),` |
| L107 | `OTHER` | `薄荷绿` | `ThemeColor("薄荷绿", SettingsPalette.ThemeAppleGreen),` |
| L108 | `OTHER` | `天空蓝` | `ThemeColor("天空蓝", IosColors.Teal),` |
| L109 | `OTHER` | `经典蓝` | `ThemeColor("经典蓝", IosColors.Blue),` |
| L110 | `OTHER` | `薰衣草紫` | `ThemeColor("薰衣草紫", IosColors.Indigo),` |
| L111 | `OTHER` | `罗兰紫` | `ThemeColor("罗兰紫", IosColors.Purple),` |
| L112 | `OTHER` | `荧光青` | `ThemeColor("荧光青", SettingsPalette.ThemeElectricCyan),` |
| L113 | `OTHER` | `亮青绿` | `ThemeColor("亮青绿", SettingsPalette.ThemeBrightTeal),` |
| L114 | `OTHER` | `炽热红` | `ThemeColor("炽热红", SettingsPalette.ThemeBrightOrangeRed)` |
| L130 | `COMPOSE_UI` | `环形配色选择器` | `title = "环形配色选择器",` |
| L131 | `OTHER` | `确定` | `confirmText = "确定",` |
| L132 | `OTHER` | `取消` | `dismissText = "取消",` |
| L209 | `OTHER` | `当前选择：$activeColorName` | `text = "当前选择：$activeColorName",` |

---

### 4.3 【AI C 专属任务区】

- **负责模块**：`:feature:statistics`
- **目标资源文件**：`feature/statistics/src/main/res/values/strings.xml`
- **涉及文件总数**：`14` 个
- **待整改硬编码总数**：`91` 处
- **独立编译验证命令**：`./gradlew :feature:statistics:compileDebugKotlin`

#### 待整改源码文件及详细行号清单：

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/ActivityHeatmapScreen.kt` (共 11 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L104 | `OTHER` | `年度回顾` | `HeatmapSectionTitle("年度回顾")` |
| L122 | `OTHER` | `记忆全景` | `HeatmapSectionTitle("记忆全景")` |
| L134 | `OTHER` | `数据高光` | `HeatmapSectionTitle("数据高光")` |
| L238 | `OTHER` | `天` | `unit = "天",` |
| L239 | `OTHER` | `最长 $longestStreak 天` | `subLabel = "最长 $longestStreak 天",` |
| L251 | `OTHER` | `天` | `unit = "天",` |
| L252 | `OTHER` | `坚持不懈` | `subLabel = "坚持不懈",` |
| L274 | `OTHER` | `项` | `unit = "项",` |
| L287 | `OTHER` | `项` | `unit = "项",` |
| L288 | `OTHER` | `超过平均`<br>`稳步前进` | `subLabel = if (todayCount &gt;= dailyAverage && dailyAverage &gt; 0) "超过平均" else "稳步前...` |
| L505 | `OTHER` | ` 项` | `suffix = " 项",` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/ActivityHeatmapViewModel.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L89 | `OTHER` | `初识` | `createBucket("young_early", "初识", allItems.count { it.stability &lt; 3f }, totalCount...` |
| L90 | `OTHER` | `熟悉` | `createBucket("young_developing", "熟悉", allItems.count { it.stability &gt;= 3f && it.s...` |
| L91 | `OTHER` | `稳固` | `createBucket("mature", "稳固", allItems.count { it.stability &gt;= 21f && it.stability ...` |
| L92 | `OTHER` | `长效` | `createBucket("expert", "长效", allItems.count { it.stability &gt;= 90f }, totalCount, S...` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/HistoricalStatisticsScreen.kt` (共 9 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L119 | `OTHER` | `累计学习` | `StatisticsSectionTitle("累计学习")` |
| L133 | `OTHER` | `已学单词 (${words.size})` | `StatisticsSectionTitle("已学单词 (${words.size})")` |
| L147 | `UI_STATE_OR_PROMPT` | `暂无单词学习记录` | `message = "暂无单词学习记录",` |
| L148 | `COMPOSE_UI` | `去学习词汇，积累你的专属词库吧` | `subtitle = "去学习词汇，积累你的专属词库吧",` |
| L161 | `OTHER` | `已学语法 (${grammars.size})` | `StatisticsSectionTitle("已学语法 (${grammars.size})")` |
| L175 | `UI_STATE_OR_PROMPT` | `暂无语法学习记录` | `message = "暂无语法学习记录",` |
| L176 | `COMPOSE_UI` | `去学习语法，掌握核心句型表达吧` | `subtitle = "去学习语法，掌握核心句型表达吧",` |
| L229 | `OTHER` | `词` | `itemUnit = "词",` |
| L243 | `OTHER` | `条` | `itemUnit = "条",` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/HistoricalStatisticsViewModel.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L271 | `DATE_FORMAT` | `yyyy年M月d日` | `val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日")` |
| L297 | `OTHER` | `${LocalDate.ofEpochDay(epochDay).format(formatter)} 达成` | `"${LocalDate.ofEpochDay(epochDay).format(formatter)} 达成"` |
| L299 | `OTHER` | `已达成` | `"已达成"` |
| L302 | `OTHER` | `已达成` | `"已达成"` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/StatisticsScreen.kt` (共 9 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L113 | `OTHER` | `单词 (${words.size})` | `StatisticsSectionTitle("单词 (${words.size})")` |
| L126 | `UI_STATE_OR_PROMPT` | `该日期没有学习任何单词` | `message = "该日期没有学习任何单词",` |
| L127 | `COMPOSE_UI` | `完成新学或复习后会在此展示` | `subtitle = "完成新学或复习后会在此展示",` |
| L140 | `OTHER` | `语法 (${grammars.size})` | `StatisticsSectionTitle("语法 (${grammars.size})")` |
| L153 | `UI_STATE_OR_PROMPT` | `该日期没有学习任何语法` | `message = "该日期没有学习任何语法",` |
| L154 | `COMPOSE_UI` | `完成语法学习后会在此展示` | `subtitle = "完成语法学习后会在此展示",` |
| L316 | `OTHER` | `收起`<br>`展开查看更多 (剩余 $remainingCount 项)` | `text = if (isFullyExpanded) "收起" else "展开查看更多 (剩余 $remainingCount 项)",` |
| L462 | `OTHER` | `新学` | `"新学",` |
| L470 | `OTHER` | `复习` | `"复习",` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/StatisticsViewModel.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L125 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/calendar/LearningCalendarScreen.kt` (共 21 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L116 | `OTHER` | `月视图`<br>`周视图` | `text = if (monthMode) "月视图" else "周视图",` |
| L154 | `OTHER` | `月度活动日历`<br>`周进度与目标` | `CalendarSectionTitle(if (monthMode) "月度活动日历" else "周进度与目标")` |
| L171 | `OTHER` | `详细记录` | `CalendarSectionTitle("详细记录")` |
| L406 | `OTHER` | `一`<br>`二`<br>`三`<br>`四`<br>`五`<br>`六`<br>`日` | `val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")` |
| L463 | `OTHER` | `${selectedCal.get(Calendar.MONTH) + 1}月${selectedCal.get(Calendar.DAY_OF_MONTH)}日` | `val dateText = "${selectedCal.get(Calendar.MONTH) + 1}月${selectedCal.get(Calendar.DAY...` |
| L470 | `OTHER` | `今天 · $dateText` | `text = if(selectedCal.timeInMillis == todayEpochDay * 86400000L) "今天 · $dateText" els...` |
| L528 | `OTHER` | `一`<br>`二`<br>`三`<br>`四`<br>`五`<br>`六`<br>`日` | `val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")` |
| L664 | `OTHER` | `${selMonth}月${selDay}日` | `val dateText = "${selMonth}月${selDay}日"` |
| L671 | `OTHER` | `今天 · $dateText` | `text = if(selectedCal.timeInMillis == todayCal.timeInMillis) "今天 · $dateText" else da...` |
| L878 | `OTHER` | `待复习单词`<br>`个` | `list.add(MetricDetailItem(ActivityRingColors.MovePink, "待复习单词", reviewWordsValue, "个"...` |
| L879 | `OTHER` | `待复习语法`<br>`条` | `list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "待复习语法", reviewGrammarVal...` |
| L880 | `OTHER` | `今日已复习单词`<br>`个` | `list.add(MetricDetailItem(ActivityRingColors.StandCyan, "今日已复习单词", todayActualReviewW...` |
| L881 | `OTHER` | `今日已复习语法`<br>`条` | `list.add(MetricDetailItem(ActivityRingColors.StandCyan, "今日已复习语法", todayActualReviewG...` |
| L883 | `OTHER` | `今日新学单词`<br>`个` | `list.add(MetricDetailItem(ActivityRingColors.MovePink, "今日新学单词", newWordsValue, "个"))` |
| L884 | `OTHER` | `今日新学语法`<br>`条` | `list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "今日新学语法", newGrammarValue...` |
| L888 | `OTHER` | `预计复习单词`<br>`个` | `list.add(MetricDetailItem(ActivityRingColors.MovePink, "预计复习单词", reviewWordsValue, "个...` |
| L889 | `OTHER` | `预计复习语法`<br>`条` | `list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "预计复习语法", reviewGrammarVa...` |
| L892 | `OTHER` | `已学单词`<br>`个` | `list.add(MetricDetailItem(ActivityRingColors.MovePink, "已学单词", newWordsValue, "个"))` |
| L893 | `OTHER` | `已学语法`<br>`条` | `list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "已学语法", newGrammarValue, ...` |
| L894 | `OTHER` | `已复习单词`<br>`个` | `list.add(MetricDetailItem(ActivityRingColors.StandCyan, "已复习单词", reviewWordsValue, "个...` |
| L895 | `OTHER` | `已复习语法`<br>`条` | `list.add(MetricDetailItem(ActivityRingColors.StandCyan, "已复习语法", reviewGrammarValue, ...` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/components/GenericStatisticsScreen.kt` (共 5 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L38 | `OTHER` | `还没有学习任何单词` | `emptyWordMessage: String = "还没有学习任何单词",` |
| L39 | `OTHER` | `还没有学习任何语法` | `emptyGrammarMessage: String = "还没有学习任何语法"` |
| L41 | `OTHER` | `单词`<br>`语法` | `val tabItems = listOf("单词", "语法")` |
| L75 | `OTHER` | `单词` | `type = "单词",` |
| L81 | `OTHER` | `语法` | `type = "语法",` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/components/HeatmapSharePoster.kt` (共 9 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L124 | `DATE_FORMAT` | `yyyy年M月d日` | `text = SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date()),` |
| L340 | `OTHER` | `天` | `unit = "天",` |
| L342 | `OTHER` | `坚持不懈` | `tag = "坚持不懈",` |
| L354 | `OTHER` | `项` | `unit = "项",` |
| L356 | `OTHER` | `$bestDayStr 巅峰` | `tag = "$bestDayStr 巅峰",` |
| L376 | `OTHER` | `项/天` | `unit = "项/天",` |
| L378 | `OTHER` | `稳步向前` | `tag = "稳步向前",` |
| L392 | `OTHER` | `FSRS 算法` | `tag = "FSRS 算法",` |
| L648 | `OTHER` | `$totalCount 项` | `text = "$totalCount 项",` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/components/LearningHeatmapCard.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L95 | `OTHER` | `一`<br>`三`<br>`五`<br>`日` | `private val WEEKDAYS = listOf("一", "", "三", "", "五", "", "日")` |
| L96 | `OTHER` | `1月`<br>`2月`<br>`3月`<br>`4月`<br>`5月`<br>`6月`<br>`7月`<br>`8月`<br>`9月`<br>`10月`<br>`11月`<br>`12月` | `private val MONTHS = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10...` |
| L602 | `OTHER` | `少` | `text = "少",` |
| L621 | `OTHER` | `多` | `text = "多",` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/curve/ForgettingCurveData.kt` (共 5 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L14 | `OTHER` | `全库综合`<br>`全量已学项目的综合记忆抗遗忘水平` | `COMPOSITE("全库综合", StatisticsPalette.CompositeOrange, "全量已学项目的综合记忆抗遗忘水平"),` |
| L15 | `OTHER` | `初识新词`<br>`刚学习1~2次，海马体临时印记，极易遗忘` | `YOUNG_EARLY("初识新词", StatisticsPalette.EarlyRed, "刚学习1~2次，海马体临时印记，极易遗忘"),` |
| L16 | `OTHER` | `发展巩固`<br>`建立初步记忆突触，处于快速生长期` | `YOUNG_DEVELOPING("发展巩固", StatisticsPalette.DevelopingBlue, "建立初步记忆突触，处于快速生长期"),` |
| L17 | `OTHER` | `深度稳固`<br>`已进入长时记忆区，具备极高抗遗忘韧性` | `MATURE("深度稳固", StatisticsPalette.MatureGreen, "已进入长时记忆区，具备极高抗遗忘韧性"),` |
| L18 | `OTHER` | `永久长效`<br>`深度固化为直觉记忆，接近母语级永久留存` | `EXPERT("永久长效", StatisticsPalette.ExpertPurple, "深度固化为直觉记忆，接近母语级永久留存")` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/curve/ForgettingCurveViewModel.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L27 | `OTHER` | `7天` | `SHORT(7, "7天"),` |
| L28 | `OTHER` | `30天` | `MEDIUM(30, "30天"),` |
| L29 | `OTHER` | `90天` | `LONG(90, "90天"),` |
| L30 | `OTHER` | `365天` | `EXTENDED(365, "365天")` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/dashboard/LeechManagementViewModel.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L100 | `OTHER` | `单词已恢复至学习队列` | `_uiState.update { it.copy(successMessage = "单词已恢复至学习队列") }` |
| L102 | `UI_STATE_OR_PROMPT` | `恢复失败` | `_uiState.update { it.copy(error = "恢复失败") }` |
| L111 | `OTHER` | `语法已恢复至学习队列` | `_uiState.update { it.copy(successMessage = "语法已恢复至学习队列") }` |
| L113 | `UI_STATE_OR_PROMPT` | `恢复失败` | `_uiState.update { it.copy(error = "恢复失败") }` |

##### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/util/ShareBitmapHelper.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L48 | `OTHER` | `分享学习成就` | `context.startActivity(Intent.createChooser(intent, "分享学习成就"))` |

---

### 4.4 【AI D 专属任务区】

- **负责模块**：`:feature:library`, `:feature:collection`, `:core:ui`
- **目标资源文件**：`各模块自身的 res/values/strings.xml`
- **涉及文件总数**：`16` 个
- **待整改硬编码总数**：`43` 处
- **独立编译验证命令**：`./gradlew :feature:library:compileDebugKotlin :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin`

#### 待整改源码文件及详细行号清单：

##### 📄 `core/ui/src/main/java/com/jian/nemo/core/ui/component/animation/AnimatedWordBackground.kt` (共 3 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L43 | `OTHER` | `桜`<br>`夢`<br>`愛`<br>`花見` | `"こんにちは", "ありがとう", "日本語", "桜", "夢", "愛", "花見",` |
| L44 | `OTHER` | `友達`<br>`時間`<br>`赤い`<br>`緑`<br>`犬`<br>`知る`<br>`大きい` | `"友達", "時間", "赤い", "緑", "犬", "知る", "大きい",` |
| L45 | `OTHER` | `小さい`<br>`新しい`<br>`難しい`<br>`楽しい`<br>`静か`<br>`見る` | `"小さい", "新しい", "難しい", "楽しい", "静か", "見る"` |

##### 📄 `core/ui/src/main/java/com/jian/nemo/core/ui/component/avatar/AvatarEditDialog.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L335 | `EXCEPTION_ERROR` | `文件保存后不存在` | `exception = Exception("文件保存后不存在")` |

##### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoriteGrammarsViewModel.kt` (共 2 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L61 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L98 | `UI_STATE_OR_PROMPT` | `批量取消收藏失败: ${e.message}` | `it.copy(error = "批量取消收藏失败: ${e.message}")` |

##### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoritesViewModel.kt` (共 3 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L74 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L103 | `UI_STATE_OR_PROMPT` | `取消收藏失败: ${result.exception.message}` | `it.copy(error = "取消收藏失败: ${result.exception.message}")` |
| L123 | `UI_STATE_OR_PROMPT` | `批量取消收藏失败: ${e.message}` | `it.copy(error = "批量取消收藏失败: ${e.message}")` |

##### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/MistakesViewModel.kt` (共 5 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L82 | `UI_STATE_OR_PROMPT` | `加载单词错题失败: ${e.message}` | `error = "加载单词错题失败: ${e.message}"` |
| L135 | `UI_STATE_OR_PROMPT` | `删除失败: ${result.exception.message}` | `it.copy(error = "删除失败: ${result.exception.message}")` |
| L155 | `UI_STATE_OR_PROMPT` | `删除失败: ${result.exception.message}` | `it.copy(error = "删除失败: ${result.exception.message}")` |
| L175 | `UI_STATE_OR_PROMPT` | `清空失败: ${result.exception.message}` | `it.copy(error = "清空失败: ${result.exception.message}")` |
| L195 | `UI_STATE_OR_PROMPT` | `清空失败: ${result.exception.message}` | `it.copy(error = "清空失败: ${result.exception.message}")` |

##### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/WrongGrammarsViewModel.kt` (共 2 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L68 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L106 | `UI_STATE_OR_PROMPT` | `删除失败: ${e.message}` | `error = "删除失败: ${e.message}"` |

##### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/WrongWordsViewModel.kt` (共 2 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L88 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L118 | `UI_STATE_OR_PROMPT` | `删除失败: ${e.message}` | `error = "删除失败: ${e.message}"` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/category/CategoryWordsViewModel.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L91 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/category/PosWordsViewModel.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L54 | `OTHER` | `加载失败: ${e.message}` | `errorMessage = "加载失败: ${e.message}"` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/detail/GrammarDetailViewModel.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L137 | `OTHER` | `反馈成功，感谢您的反馈！` | `_successMessage.value = "反馈成功，感谢您的反馈！"` |
| L141 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L145 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L146 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/detail/WordDetailViewModel.kt` (共 4 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L177 | `OTHER` | `反馈成功，感谢您的反馈！` | `_successMessage.value = "反馈成功，感谢您的反馈！"` |
| L181 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L185 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L186 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/expression/ExpressionScreen.kt` (共 8 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L74 | `OTHER` | `固` | `"collocation" -&gt; "固"` |
| L75 | `DATE_FORMAT` | `句` | `"sentence_pattern" -&gt; "句"` |
| L76 | `OTHER` | `惯` | `"idiom" -&gt; "惯"` |
| L77 | `OTHER` | `四` | `"four_character_idiom" -&gt; "四"` |
| L78 | `OTHER` | `框` | `"semi_fixed_template" -&gt; "框"` |
| L79 | `OTHER` | `群` | `"collocation_group" -&gt; "群"` |
| L80 | `OTHER` | `语` | `else -&gt; "语"` |
| L517 | `COMPOSE_UI` | `箭头旋转` | `label = "箭头旋转"` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/expression/ExpressionViewModel.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L93 | `UI_STATE_OR_PROMPT` | `加载数据失败` | `error = e.message ?: "加载数据失败"` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/list/GrammarListScreen.kt` (共 3 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L375 | `OTHER` | `失败` | `type = if (uiState.syncMessage?.contains("失败") == true)` |
| L379 | `OTHER` | `失败` | `icon = if (uiState.syncMessage?.contains("失败") == true)` |
| L444 | `OTHER` | `文` | `text = "文",` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/list/WordListScreen.kt` (共 2 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L374 | `OTHER` | `失败` | `type = if (uiState.syncMessage?.contains("失败") == true)` |
| L378 | `OTHER` | `失败` | `icon = if (uiState.syncMessage?.contains("失败") == true)` |

##### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/partofspeech/PartOfSpeechViewModel.kt` (共 1 处)
| 行号 | 类型 | 硬编码文案字面量 | 原始代码片段 |
|---|---|---|---|
| L49 | `OTHER` | `加载失败: ${e.message}` | `errorMessage = "加载失败: ${e.message}"` |

---

## 五、底层与协议模块隔离说明（无需纳入 UI strings）

在静态代码扫描中，以下模块亦包含中文字符串，但经深度语义审查，**严禁交由 AI 盲目提取为 UI strings.xml**，保持分层纯净：

1. **`:core:domain`（13 个文件，18 处）**：
   - **内容本质**：纯 Kotlin 领域层中的内部异常（如 `IllegalArgumentException("单词不存在: wordId=$wordId")`）、SRS 算法日志（`logger.info("FSRS 目标保留率更新...")`）、离线回退词性常数列表等。
   - **架构隔离原因**：Clean Architecture 中 Domain 模块严禁依赖 Android 平台框架与 Android Context；强行引入 `R.string` 将破坏跨平台纯 Kotlin 架构。

2. **`:core:data`（8 个文件，89 处）**：
   - **内容本质**：大语言模型 API 的提示词协议模板（如 `LLMRepositoryImpl.kt` 中的 System Prompt、Prompt Few-shot 样例、JSON Schema 约束）、以及底层 GZIP 解压魔数校验与 Room 数据库导入校验。
   - **协议隔离原因**：大模型 Prompt 模板属于网络协议与算法模型上下文（Prompt Engineering），并非客户端 UI 界面展示文本；放入 Android strings.xml 会破坏提示词动态插值与参数格式。

3. **`:core:common`（2 个文件，3 处）**：
   - **内容本质**：`DateTimeUtils` 的内部日期格式化工具及下载器的底层网络异常。


---

## 六、多 AI 协作推荐执行顺序与验收标准

1. **执行模式**：用户可同时开启 4 个 AI 对话窗口，分别粘贴第三章中的【AI A】、【AI B】、【AI C】、【AI D】提示词。
2. **单模块验收**：各 AI 严格在各自窗口跑完后，必须执行指定的 `compileDebugKotlin` 验证无编译错误。
3. **全局最终验收**：当 4 个 AI 均完成任务并合并代码后，在项目根目录运行全工程编译验证：
   ```bash
   ./gradlew compileReleaseKotlin
   ```
   验证所有模块均编译成功、零未解析引用，且全项目无任何新增裸字符串违规！
