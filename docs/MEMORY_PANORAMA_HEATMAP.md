# 记忆全景热力图规范

## 目标
实现一个热力图视图，用 FSRS 的 stability 分层展示全库记忆状态分布。

## 范围
- 覆盖全部学习项目（单词 + 语法）。
- 按稳定性分层展示分布。
- 提供全库记忆健康度的快速快照。

## 定义
- Stability：FSRS 稳定性数值，单位为天。
- 分层区间：
  - 初识（Young - Early）：0 到 <3 天
  - 熟悉（Young - Developing）：3 到 <21 天
  - 稳固（Mature）：21 到 <90 天
  - 长效（Expert/Mastery）：90 天以上

## 数据契约
热力图组件需要以下聚合数据：

- totalCount: number
- buckets:
  - id: string (young_early | young_developing | mature | expert)
  - label: string
  - count: number
  - ratio: number (count / totalCount)
  - color: string (hex)

推荐颜色：
- young_early: #EF4444
- young_developing: #3B82F6
- mature: #10B981
- expert: #059669

## 聚合规则
1. 仅统计有有效 stability 的项目。
2. 排除已删除项目。
3. 仅使用当前 stability（不使用预测值）。
4. 若 stability 缺失，仅在产品要求时放入 "Unknown" 桶；默认剔除并记录缺失数量用于 QA。

## 热力图布局
- 展示 4 个主分层为卡片网格（手机 2x2，平板/桌面 1x4）。
- 每个卡片包含：
  - 分层名称
  - 数量
  - 占比（百分比）
- 卡片底色使用对应分层颜色。
- 可选：在卡片内用迷你条形或点密度表示占比。

## 集成位置
- 页面路径：feature/statistics/src/main/java/com/jian/nemo/feature/statistics/ActivityHeatmapScreen.kt
- 在该页面新增一个「记忆全景」卡片，和现有学习热力图内容同级。
- 建议位置：热力图主卡片下方、数据高光区块上方，保持信息层级清晰。
- 卡片内容：标题 + 4 分层小方块 + 分层说明入口（可复用右上角 info）。

## 交互
- 点击卡片进入该分层的筛选列表。
- 长按显示 tooltip，包含分层定义和稳定性区间。
- 右上角提供信息按钮，简要解释 FSRS stability 的含义。

## 空状态
- totalCount == 0 时：展示友好的空状态与开始学习 CTA。
- 数据获取失败：展示重试入口并显示错误摘要。

## 性能
- 聚合结果在数据刷新时计算并缓存。
- 复习事件或同步完成后触发 UI 更新。

## 埋点（可选）
- memory_panorama_viewed
- memory_panorama_bucket_clicked (bucket_id)
- memory_panorama_info_opened

## 验收标准
- 分层边界正确：
  - stability == 0 => young_early
  - stability == 2.999 => young_early
  - stability == 3 => young_developing
  - stability == 20.999 => young_developing
  - stability == 21 => mature
  - stability == 89.999 => mature
  - stability == 90 => expert
- 各分层计数与占比之和等于 totalCount。
- 颜色与分层配色一致。
- 分层筛选与区间规则一致。
