# 【AI K - 词库大厅与分类词汇】专属重构任务工单

> **工单代号**：`AI_K`  
> **所属模块**：`:feature:library`  
> **业务范围**：词库资源大厅、分类词汇、词性筛选、词汇与语法详情页、惯用句速查  
> **待修改源码文件数**：`9` 个  
> **待提取硬编码总条数**：`25` 处  
> **专属独立资源文件（防冲突）**：`feature/library/src/main/res/values/strings_library.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.library.R`  
> **独立编译验证命令**：`./gradlew :feature:library:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/library/src/main/res/values/strings_library.xml`。
   - 若文件不存在可直接新建；若已存在则追加在根 `<resources>` 标签内。
   - **严禁向公共或其它 strings 文件写入，确保你的改动与其他 AI 0 冲突**！
3. **模块内 R 引用闭环**：Kotlin 源码中统一通过 `stringResource(com.jian.nemo.feature.library.R.string.xxx)` 取用，**严禁跨模块引用外部 feature 模块的 R 资源**。
4. **规范 Key 命名**：`[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **严禁口水注释与黑盒脚本**：严禁在源码中留下“根据要求提取”等元注释；严禁使用黑盒正则脚本批处理，必须保持代码安全。
6. **单模块独立编译验证**：修改完成后，在终端直接运行：
   ```bash
   ./gradlew :feature:library:compileDebugKotlin
   ```
   编译通过且无任何报错即代表本工单顺利交付！严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/category/CategoryWordsViewModel.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L91 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/category/PosWordsViewModel.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L54 | `OTHER` | `加载失败: ${e.message}` | `errorMessage = "加载失败: ${e.message}"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/detail/GrammarDetailViewModel.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L137 | `OTHER` | `反馈成功，感谢您的反馈！` | `_successMessage.value = "反馈成功，感谢您的反馈！"` |
| L141 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L145 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L146 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/detail/WordDetailViewModel.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L177 | `OTHER` | `反馈成功，感谢您的反馈！` | `_successMessage.value = "反馈成功，感谢您的反馈！"` |
| L181 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L185 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L186 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/expression/ExpressionScreen.kt`（共 8 处硬编码）

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

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/expression/ExpressionViewModel.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L93 | `UI_STATE_OR_PROMPT` | `加载数据失败` | `error = e.message ?: "加载数据失败"` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/list/GrammarListScreen.kt`（共 3 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L375 | `OTHER` | `失败` | `type = if (uiState.syncMessage?.contains("失败") == true)` |
| L379 | `OTHER` | `失败` | `icon = if (uiState.syncMessage?.contains("失败") == true)` |
| L444 | `OTHER` | `文` | `text = "文",` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/list/WordListScreen.kt`（共 2 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L374 | `OTHER` | `失败` | `type = if (uiState.syncMessage?.contains("失败") == true)` |
| L378 | `OTHER` | `失败` | `icon = if (uiState.syncMessage?.contains("失败") == true)` |

### 📄 `feature/library/src/main/java/com/jian/nemo/feature/library/presentation/partofspeech/PartOfSpeechViewModel.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L49 | `OTHER` | `加载失败: ${e.message}` | `errorMessage = "加载失败: ${e.message}"` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/library/src/main/res/values/strings_library.xml`。
- [ ] 成功执行 `./gradlew :feature:library:compileDebugKotlin`，模块独立编译通过，0 错误。
