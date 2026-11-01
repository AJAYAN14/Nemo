# 【AI G - 活动热力图与日历】专属任务执行工单

> **工单代号**：`AI_G`  
> **负责模块**：`:feature:statistics`  
> **负责子域**：活动热力图与打卡打卡（热力图矩阵、打卡海报分享生成、学习日历月/周视图）  
> **待修改文件数**：`5` 个  
> **待提取硬编码总数**：`49` 处  
> **推荐追加的专属资源文件**：`feature/statistics/src/main/res/values/strings_statistics_calendar.xml`  
> **当前模块 R 引用**：`import com.jian.nemo.feature.statistics.R`  
> **独立编译验证命令**：`./gradlew :feature:statistics:compileDebugKotlin`  

---

## 一、执行硬约束纪律（执行前必读）

1. **严禁越界修改**：你只被授权修改本工单下方【第二章】所明确列出的文件。**严禁修改任何未被点名的文件**。
2. **独立资源文件隔离（防冲突关键）**：
   - 请将所有提取出的 `<string name="...">文案</string>` 写入/追加到专属文件：`feature/statistics/src/main/res/values/strings_statistics_calendar.xml`。
   - 如果该文件不存在，可直接新建；如果已存在则追加在 `<resources>` 标签内。
   - **严禁与其他 AI 共用或改动同一个 strings 文件，以杜绝 Git 合并冲突**！
3. **R 引用闭环**：在 Kotlin 源码中，统一导入 `com.jian.nemo.feature.statistics.R`，通过 `stringResource(com.jian.nemo.feature.statistics.R.string.xxx)` 或 `context.getString(...)` 取用，严禁跨模块引用其他 feature 模块未编译生成的 R 资源。
4. **命名规范**：遵循 `[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **独立编译验证**：修改完成后，在项目终端运行：
   ```bash
   ./gradlew :feature:statistics:compileDebugKotlin
   ```
   必须确保无任何编译与语法报错。严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/ActivityHeatmapScreen.kt`（共 11 处）

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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/ActivityHeatmapViewModel.kt`（共 4 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L89 | `OTHER` | `初识` | `createBucket("young_early", "初识", allItems.count { it.stability &lt; 3f }, totalCount...` |
| L90 | `OTHER` | `熟悉` | `createBucket("young_developing", "熟悉", allItems.count { it.stability &gt;= 3f && it.s...` |
| L91 | `OTHER` | `稳固` | `createBucket("mature", "稳固", allItems.count { it.stability &gt;= 21f && it.stability ...` |
| L92 | `OTHER` | `长效` | `createBucket("expert", "长效", allItems.count { it.stability &gt;= 90f }, totalCount, S...` |

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/calendar/LearningCalendarScreen.kt`（共 21 处）

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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/components/HeatmapSharePoster.kt`（共 9 处）

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

### 📄 `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/presentation/components/LearningHeatmapCard.kt`（共 4 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L95 | `OTHER` | `一`<br>`三`<br>`五`<br>`日` | `private val WEEKDAYS = listOf("一", "", "三", "", "五", "", "日")` |
| L96 | `OTHER` | `1月`<br>`2月`<br>`3月`<br>`4月`<br>`5月`<br>`6月`<br>`7月`<br>`8月`<br>`9月`<br>`10月`<br>`11月`<br>`12月` | `private val MONTHS = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10...` |
| L602 | `OTHER` | `少` | `text = "少",` |
| L621 | `OTHER` | `多` | `text = "多",` |

---

## 三、完成验收检查单

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/statistics/src/main/res/values/strings_statistics_calendar.xml`。
- [ ] 成功执行 `./gradlew :feature:statistics:compileDebugKotlin`，编译通过，0 Error。
