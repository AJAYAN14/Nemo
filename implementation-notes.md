# 实施笔记：学习日历 #099 活动三环与月度日历改造

## 目标与范围
- 在 Nemo 学习日历中引入 Lucian UX/UI Prompt #099（Activity rings that lap）经典三环组件与动效。
- 解决月度视图与详细记录的整合问题，实现周/月双模式下大三环与微型三环的优雅联动。

## 关键设计决策
1. **三环业务定义（经典 3 环）**：
   - 外环（#FA2D62 桃红）：新学单词（实际 / 每日单词目标）
   - 中环（#7BD33A 青绿）：新学语法（实际 / 每日语法目标）
   - 内环（#18C6E8 湖蓝）：复习达成率（实际已复习 / (待复习 + 已复习)）
2. **月度视图解决方案**：
   - 保留 [周 | 月] 分段切换；
   - 周模式：顶部大三环 + 右侧指标卡 + 底部 7 天胶囊选择器（含迷你三环）；
   - 月模式：顶部大三环 + 右侧指标卡 + 底部 7×5/6 整月日历网格，每个日期单元格均呈现 Apple Fitness 经典微型三环；
   - 无论周还是月，点击任意日期，大三环平滑旋转补间到所选日数据，支持超圈投影与合环发光动画；
   - 周期总结联动：周模式显示本周合环天数，月模式显示本月合环天数。
3. **页面层级精简**：
   - 移除原有重叠的旧版“今日概览”卡片，新学与复习核心数据均在大三环与指标卡完整展现；
   - 核心活动卡片置顶；
   - 底部保留轻量级“详细记录”面板。

## 实施详情
- **新增文件**：
  - `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/calendar/ActivityRingsComponent.kt`
    - `ActivityRingsCanvas`：实现同心三环、超圈尾部阴影投射（shadowed tip）、冲破合环微闪、单环高亮聚焦（其余环透明度降至 22%）。
    - `MiniActivityRings`：轻量微型三环 Canvas，用于周胶囊与月历网格小单元格。
    - `ActivityRingItemRow`：右侧交互式指标项与数字动画。
    - `ActivityWeekPicker`：7 天胶囊周选择器。
    - `ActivityMonthGrid`：月历网格与月份切换。
    - `ActivityRingsCard`：整卡联动容器。
- **修改文件**：
  - `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/calendar/LearningCalendarViewModel.kt`
    - 增加 `recordsMap` 加载与 `loadMonthRecords`，支持月度全量三环数据。
  - `feature/statistics/src/main/java/com/jian/nemo/feature/statistics/calendar/LearningCalendarScreen.kt`
    - 接入 `ActivityRingsCard`，精简移除冗余的 `TodaySummaryCard`，保留底部的 `DayDetailPanel`。

## 动效与无障碍支持
- 采用 1100ms 立方缓动曲线（CubicBezierEasing 0.2, 0.8, 0.2, 1.0），三环分别延迟 0ms, 110ms, 220ms 错落展开。
- 支持 `prefers-reduced-motion`（减弱动态效果）：自动检测系统动画缩放，当开启减弱动画时，动画时长降为 1ms，无缝呈现最终状态。
- 深色与浅色模式适配：根据 `MaterialTheme` 自适应暗色与亮色表面、边框阴影。
