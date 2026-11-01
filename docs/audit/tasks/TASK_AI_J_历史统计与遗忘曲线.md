# 【AI J - 历史统计与遗忘曲线】专属重构任务工单

> **工单代号**：`AI_J`  
> **所属模块**：`:feature:statistics`  
> **业务范围**：历史学习数据概览、FSRS 遗忘曲线衰减模型与预测、顽疾词（Leech）队列恢复与分享位图  
> **待修改源码文件数**：`9` 个  
> **待提取硬编码总条数**：`42` 处  
> **专属独立资源文件（防冲突）**：`feature/statistics/src/main/res/values/strings_statistics_history.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.statistics.R`  
> **独立编译验证命令**：`./gradlew :feature:statistics:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/statistics/src/main/res/values/strings_statistics_history.xml`。
   - 若文件不存在可直接新建；若已存在则追加在根 `<resources>` 标签内。
   - **严禁向公共或其它 strings 文件写入，确保你的改动与其他 AI 0 冲突**！
3. **模块内 R 引用闭环**：Kotlin 源码中统一通过 `stringResource(com.jian.nemo.feature.statistics.R.string.xxx)` 取用，**严禁跨模块引用外部 feature 模块的 R 资源**。
4. **规范 Key 命名**：`[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **严禁口水注释与黑盒脚本**：严禁在源码中留下“根据要求提取”等元注释；严禁使用黑盒正则脚本批处理，必须保持代码安全。
6. **单模块独立编译验证**：修改完成后，在终端直接运行：
   ```bash
   ./gradlew :feature:statistics:compileDebugKotlin
   ```
   编译通过且无任何报错即代表本工单顺利交付！严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/HistoricalStatisticsScreen.kt`（共 9 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/HistoricalStatisticsViewModel.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L271 | `DATE_FORMAT` | `yyyy年M月d日` | `val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日")` |
| L297 | `OTHER` | `${LocalDate.ofEpochDay(epochDay).format(formatter)} 达成` | `"${LocalDate.ofEpochDay(epochDay).format(formatter)} 达成"` |
| L299 | `OTHER` | `已达成` | `"已达成"` |
| L302 | `OTHER` | `已达成` | `"已达成"` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/StatisticsScreen.kt`（共 9 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/StatisticsViewModel.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L125 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/components/GenericStatisticsScreen.kt`（共 5 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L38 | `OTHER` | `还没有学习任何单词` | `emptyWordMessage: String = "还没有学习任何单词",` |
| L39 | `OTHER` | `还没有学习任何语法` | `emptyGrammarMessage: String = "还没有学习任何语法"` |
| L41 | `OTHER` | `单词`<br>`语法` | `val tabItems = listOf("单词", "语法")` |
| L75 | `OTHER` | `单词` | `type = "单词",` |
| L81 | `OTHER` | `语法` | `type = "语法",` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/curve/ForgettingCurveData.kt`（共 5 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L14 | `OTHER` | `全库综合`<br>`全量已学项目的综合记忆抗遗忘水平` | `COMPOSITE("全库综合", StatisticsPalette.CompositeOrange, "全量已学项目的综合记忆抗遗忘水平"),` |
| L15 | `OTHER` | `初识新词`<br>`刚学习1~2次，海马体临时印记，极易遗忘` | `YOUNG_EARLY("初识新词", StatisticsPalette.EarlyRed, "刚学习1~2次，海马体临时印记，极易遗忘"),` |
| L16 | `OTHER` | `发展巩固`<br>`建立初步记忆突触，处于快速生长期` | `YOUNG_DEVELOPING("发展巩固", StatisticsPalette.DevelopingBlue, "建立初步记忆突触，处于快速生长期"),` |
| L17 | `OTHER` | `深度稳固`<br>`已进入长时记忆区，具备极高抗遗忘韧性` | `MATURE("深度稳固", StatisticsPalette.MatureGreen, "已进入长时记忆区，具备极高抗遗忘韧性"),` |
| L18 | `OTHER` | `永久长效`<br>`深度固化为直觉记忆，接近母语级永久留存` | `EXPERT("永久长效", StatisticsPalette.ExpertPurple, "深度固化为直觉记忆，接近母语级永久留存")` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/curve/ForgettingCurveViewModel.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L27 | `OTHER` | `7天` | `SHORT(7, "7天"),` |
| L28 | `OTHER` | `30天` | `MEDIUM(30, "30天"),` |
| L29 | `OTHER` | `90天` | `LONG(90, "90天"),` |
| L30 | `OTHER` | `365天` | `EXTENDED(365, "365天")` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/dashboard/LeechManagementViewModel.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L100 | `OTHER` | `单词已恢复至学习队列` | `_uiState.update { it.copy(successMessage = "单词已恢复至学习队列") }` |
| L102 | `UI_STATE_OR_PROMPT` | `恢复失败` | `_uiState.update { it.copy(error = "恢复失败") }` |
| L111 | `OTHER` | `语法已恢复至学习队列` | `_uiState.update { it.copy(successMessage = "语法已恢复至学习队列") }` |
| L113 | `UI_STATE_OR_PROMPT` | `恢复失败` | `_uiState.update { it.copy(error = "恢复失败") }` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/util/ShareBitmapHelper.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L48 | `OTHER` | `分享学习成就` | `context.startActivity(Intent.createChooser(intent, "分享学习成就"))` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/statistics/src/main/res/values/strings_statistics_history.xml`。
- [ ] 成功执行 `./gradlew :feature:statistics:compileDebugKotlin`，模块独立编译通过，0 错误。
