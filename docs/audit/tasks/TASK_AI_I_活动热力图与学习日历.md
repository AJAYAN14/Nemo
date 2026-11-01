# 【AI I - 活动热力图与学习日历】专属重构任务工单

> **工单代号**：`AI_I`  
> **所属模块**：`:feature:statistics`  
> **业务范围**：画廊级学习热力图卡片、成就海报分享导出、学习日历月/周视图打卡  
> **待修改源码文件数**：`5` 个  
> **待提取硬编码总条数**：`49` 处  
> **专属独立资源文件（防冲突）**：`feature/statistics/src/main/res/values/strings_statistics_calendar.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.statistics.R`  
> **独立编译验证命令**：`./gradlew :feature:statistics:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/statistics/src/main/res/values/strings_statistics_calendar.xml`。
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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/ActivityHeatmapScreen.kt`（共 11 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/ActivityHeatmapViewModel.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L89 | `OTHER` | `初识` | `createBucket("young_early", "初识", allItems.count { it.stability &lt; 3f }, totalCount...` |
| L90 | `OTHER` | `熟悉` | `createBucket("young_developing", "熟悉", allItems.count { it.stability &gt;= 3f && it.s...` |
| L91 | `OTHER` | `稳固` | `createBucket("mature", "稳固", allItems.count { it.stability &gt;= 21f && it.stability ...` |
| L92 | `OTHER` | `长效` | `createBucket("expert", "长效", allItems.count { it.stability &gt;= 90f }, totalCount, S...` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/calendar/LearningCalendarScreen.kt`（共 21 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/components/HeatmapSharePoster.kt`（共 9 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/components/LearningHeatmapCard.kt`（共 4 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L95 | `OTHER` | `一`<br>`三`<br>`五`<br>`日` | `private val WEEKDAYS = listOf("一", "", "三", "", "五", "", "日")` |
| L96 | `OTHER` | `1月`<br>`2月`<br>`3月`<br>`4月`<br>`5月`<br>`6月`<br>`7月`<br>`8月`<br>`9月`<br>`10月`<br>`11月`<br>`12月` | `private val MONTHS = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10...` |
| L602 | `OTHER` | `少` | `text = "少",` |
| L621 | `OTHER` | `多` | `text = "多",` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/statistics/src/main/res/values/strings_statistics_calendar.xml`。
- [ ] 成功执行 `./gradlew :feature:statistics:compileDebugKotlin`，模块独立编译通过，0 错误。
