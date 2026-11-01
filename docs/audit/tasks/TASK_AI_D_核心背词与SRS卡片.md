# 【AI D - 核心背词与 SRS 卡片】专属重构任务工单

> **工单代号**：`AI_D`  
> **所属模块**：`:feature:learning`  
> **业务范围**：背词模式选择、SRS 单词/语法卡片交互、卡片撤回、会话结算报告  
> **待修改源码文件数**：`11` 个  
> **待提取硬编码总条数**：`40` 处  
> **专属独立资源文件（防冲突）**：`feature/learning/src/main/res/values/strings_learning_core.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.learning.R`  
> **独立编译验证命令**：`./gradlew :feature:learning:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/learning/src/main/res/values/strings_learning_core.xml`。
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

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/LearningMode.kt`（共 2 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L22 | `OTHER` | `单词` | `is Word -&gt; "单词"` |
| L23 | `OTHER` | `语法` | `is Grammar -&gt; "语法"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/LearningViewModel.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L262 | `OTHER` | `反馈成功，感谢您的反馈！` | `_uiState.update { it.copy(successMessage = "反馈成功，感谢您的反馈！") }` |
| L266 | `OTHER` | `您已经提交过对此类型问题的反馈了，请勿重复提交。` | `message.contains("unique_user_item_report") -&gt; "您已经提交过对此类型问题的反馈了，请勿重复提交。"` |
| L270 | `OTHER` | `网络连接失败，请检查网络后重试。` | `message.contains("HTTP request to", ignoreCase = true) -&gt; "网络连接失败，请检查网络后重试。"` |
| L271 | `OTHER` | `反馈失败: $message` | `else -&gt; "反馈失败: $message"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/cards/SRSGrammarCard.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L527 | `OTHER` | `一`<br>`二`<br>`三`<br>`四`<br>`五`<br>`六`<br>`七`<br>`八`<br>`九`<br>`十` | `val chineseNumbers = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/cards/WordCard.kt`（共 16 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
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

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/common/LearningFinishedContent.kt`（共 2 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L98 | `UI_STATE_OR_PROMPT` | `今日任务达成！` | `title: String = "今日任务达成！",` |
| L99 | `UI_STATE_OR_PROMPT` | `坚持就是胜利，明天继续加油` | `subtitle: String = "坚持就是胜利，明天继续加油",` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/components/srs/SRSActionArea.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L195 | `OTHER` | `显示答案`<br>`显示答案 ($countdownText)` | `text = if (canShowAnswer) "显示答案" else "显示答案 ($countdownText)",` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/CardOpsDelegate.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L34 | `OTHER` | `已埋藏顽固卡片: ${item.displayName} (今日不再出现，明日自动回归)` | `buryItem(item, "已埋藏顽固卡片: ${item.displayName} (今日不再出现，明日自动回归)")` |
| L38 | `UI_STATE_OR_PROMPT` | `已冻结顽固卡片: ${item.displayName} (累计遗忘 $totalLapses 次，已移出复习)` | `it.copy(error = "已冻结顽固卡片: ${item.displayName} (累计遗忘 $totalLapses 次，已移出复习)")` |
| L56 | `UI_STATE_OR_PROMPT` | `已暂停: ${currentItem.displayName}` | `it.copy(error = "已暂停: ${currentItem.displayName}")` |
| L67 | `OTHER` | `已暂缓: ${currentItem.displayName} (今日不再出现)` | `buryItem(currentItem, "已暂缓: ${currentItem.displayName} (今日不再出现)")` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/RatingDelegate.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L187 | `UI_STATE_OR_PROMPT` | `评分异常: ${e.message}` | `error = "评分异常: ${e.message}"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/SessionLifecycleDelegate.kt`（共 7 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L140 | `UI_STATE_OR_PROMPT` | `启动失败: ${e.message}` | `error = "启动失败: ${e.message}"` |
| L183 | `OTHER` | `词`<br>`语法` | `val unit = if (mode == LearningMode.Word) "词" else "语法"` |
| L220 | `OTHER` | `太棒了！当前待复习积压已全部出清` | `BonusMode.ReviewBacklog -&gt; "太棒了！当前待复习积压已全部出清"` |
| L221 | `OTHER` | `暂无更多未学新$unit` | `BonusMode.NewItems -&gt; "暂无更多未学新$unit"` |
| L233 | `OTHER` | `待复习`<br>`新` | `val modeLabel = if (bonusMode == BonusMode.ReviewBacklog) "待复习" else "新"` |
| L235 | `OTHER` | `已为你加载剩余全部 ${bonusItems.size} 个$modeLabel$unit` | `it.copy(successMessage = "已为你加载剩余全部 ${bonusItems.size} 个$modeLabel$unit")` |
| L287 | `UI_STATE_OR_PROMPT` | `启动加餐失败: ${e.message}` | `error = "启动加餐失败: ${e.message}"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/delegate/UndoDelegate.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L160 | `UI_STATE_OR_PROMPT` | `撤销失败: ${e.message}` | `error = "撤销失败: ${e.message}"` |

### 📄 `feature/learning/src/main/java/com/jian/nemo/feature/learning/presentation/home/components/HomeComponents.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L50 | `OTHER` | `銉嶃儮` | `bgText: String = "銉嶃儮",` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/learning/src/main/res/values/strings_learning_core.xml`。
- [ ] 成功执行 `./gradlew :feature:learning:compileDebugKotlin`，模块独立编译通过，0 错误。
