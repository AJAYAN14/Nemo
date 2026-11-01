# 【AI I - 词库大厅与分类】专属任务执行工单

> **工单代号**：`AI_I`  
> **负责模块**：`:feature:library`  
> **负责子域**：词库资源大厅（分类词汇浏览、词性筛选、词汇与语法详情页、惯用句速查）  
> **待修改文件数**：`9` 个  
> **待提取硬编码总数**：`25` 处  
> **推荐追加的专属资源文件**：`feature/library/src/main/res/values/strings_library.xml`  
> **当前模块 R 引用**：`import com.jian.nemo.feature.library.R`  
> **独立编译验证命令**：`./gradlew :feature:library:compileDebugKotlin`  

---

## 一、执行硬约束纪律（执行前必读）

1. **严禁越界修改**：你只被授权修改本工单下方【第二章】所明确列出的文件。**严禁修改任何未被点名的文件**。
2. **独立资源文件隔离（防冲突关键）**：
   - 请将所有提取出的 `<string name="...">文案</string>` 写入/追加到专属文件：`feature/library/src/main/res/values/strings_library.xml`。
   - 如果该文件不存在，可直接新建；如果已存在则追加在 `<resources>` 标签内。
   - **严禁与其他 AI 共用或改动同一个 strings 文件，以杜绝 Git 合并冲突**！
3. **R 引用闭环**：在 Kotlin 源码中，统一导入 `com.jian.nemo.feature.library.R`，通过 `stringResource(com.jian.nemo.feature.library.R.string.xxx)` 或 `context.getString(...)` 取用，严禁跨模块引用其他 feature 模块未编译生成的 R 资源。
4. **命名规范**：遵循 `[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **独立编译验证**：修改完成后，在项目终端运行：
   ```bash
   ./gradlew :feature:library:compileDebugKotlin
   ```
   必须确保无任何编译与语法报错。严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/category/CategoryWordsViewModel.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L91 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/category/PosWordsViewModel.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L54 | `OTHER` | `加载失败: ${e.message}` | `errorMessage = "加载失败: ${e.message}"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/detail/GrammarDetailViewModel.kt`（共 4 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L137 | `OTHER` | `反馈成功，感谢您的反馈！` | `_successMessage.value = "反馈成功，感谢您的反馈！"` |
| L141 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L145 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L146 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/detail/WordDetailViewModel.kt`（共 4 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L177 | `OTHER` | `反馈成功，感谢您的反馈！` | `_successMessage.value = "反馈成功，感谢您的反馈！"` |
| L181 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L185 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L186 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/expression/ExpressionScreen.kt`（共 8 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L74 | `OTHER` | `固` | `"collocation" -&gt; "固"` |
| L75 | `DATE_FORMAT` | `句` | `"sentence_pattern" -&gt; "句"` |
| L76 | `OTHER` | `惯` | `"idiom" -&gt; "惯"` |
| L77 | `OTHER` | `四` | `"four_character_idiom" -&gt; "四"` |
| L78 | `OTHER` | `框` | `"semi_fixed_template" -&gt; "框"` |
| L79 | `OTHER` | `群` | `"collocation_group" -&gt; "群"` |
| L80 | `OTHER` | `语` | `else -&gt; "语"` |
| L517 | `COMPOSE_UI` | `箭头旋转` | `label = "箭头旋转"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/expression/ExpressionViewModel.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L93 | `UI_STATE_OR_PROMPT` | `加载数据失败` | `error = e.message ?: "加载数据失败"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/list/GrammarListScreen.kt`（共 3 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L375 | `OTHER` | `失败` | `type = if (uiState.syncMessage?.contains("失败") == true)` |
| L379 | `OTHER` | `失败` | `icon = if (uiState.syncMessage?.contains("失败") == true)` |
| L444 | `OTHER` | `文` | `text = "文",` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/list/WordListScreen.kt`（共 2 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L374 | `OTHER` | `失败` | `type = if (uiState.syncMessage?.contains("失败") == true)` |
| L378 | `OTHER` | `失败` | `icon = if (uiState.syncMessage?.contains("失败") == true)` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/partofspeech/PartOfSpeechViewModel.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L49 | `OTHER` | `加载失败: ${e.message}` | `errorMessage = "加载失败: ${e.message}"` |

---

## 三、完成验收检查单

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/library/src/main/res/values/strings_library.xml`。
- [ ] 成功执行 `./gradlew :feature:library:compileDebugKotlin`，编译通过，0 Error。
