# Nemo 规范化重构实现笔记 (Implementation Notes)

> 本文档记录全项目规范化治理过程中的偏差、新发现的边缘情况及待审查项。
> 遵循规则：`.agent/rules/nemorules.md`、`docs/rules/rules-code-style.md`、`docs/rules/rules-ui-fidelity.md`。

---

## 1. 治理批次：模块 `core/ui` 与 `core/designsystem` (已完成全部清零)

### 1.1 治理结果
- **`core/ui` 模块**：完成全部 19 个文件的逐处精准治理，共计 176 处裸十六进制色值全部清零，统一收敛至 `UiPalette`。
- **`core/designsystem` 模块**：完成 `PlumpDuoIcon.kt`、`PremiumStepper.kt`、`NemoFractionalPicker.kt` 3 个文件的治理，共计 21 处裸十六进制色值全部清零，收敛至 `PlumpPalette` 与 `DesignSystemPalette`。
- **排除文件完整性保护**：严格遵循 20 个本地排除文件红线，零触碰、零篡改。
- **构建状态**：全项目 `compileDebugKotlin` 验证零错误编译通过。

### 1.2 偏差记录 (Deviations)
- 解决命名冲突：原 `IsoluminantPalette` 在 `feature/settings` 存在同名 typealias 并在 `SettingsScreen.kt` 触发多重引用冲突，已规范重构命名为 `PlumpPalette`，并在各调用侧无缝映射。

### 1.3 发现的边缘情况 (Discovered Edge Cases)
- `PresetAvatars.kt` 预设头像使用统一数组常量管理，既提升性能又根除了源码元注释。
- Windows 下换行符（CRLF）在多行替换时需保持原子性操作，改用整行精准匹配或全文件无损覆写。

---

## 2. 治理批次：模块 `feature/learning` (已 100% 全部清零通过验证)

### 2.1 全量完成治理文件清单 (22 个文件共 213 处色值泄漏清零 + 文本收编入 strings.xml)
1. `presentation/ai/AIAuroraBackground.kt` (15 处，收敛至 `LearningPalette.Aurora*`)
2. `presentation/ai/AIWordTranslationSheet.kt` (5 处，收敛至 `LearningPalette.*`)
3. `presentation/ai/AIExerciseDetailScreen.kt` (1 处，收敛至 `LearningPalette.SuccessGreen`)
4. `presentation/category/CategoryCardBrowseScreen.kt` (2 处，收敛至 `LearningPalette.CategoryDarkBg*`)
5. `presentation/components/srs/SRSRatingButton.kt` (2 处，收敛至 `LearningPalette.SrsRose*`)
6. `presentation/components/dialogs/ForcedNotificationPopup.kt` (1 处，收敛至 `LearningPalette.AlertRedBg`)
7. `presentation/LearningScreen.kt` (3 处，打卡胶囊背景/边框/文字收敛至 `LearningPalette.BannerOrange*`)
8. `presentation/components/srs/SRSActionArea.kt` (10 处，SRS 评级阶梯色、显示答案按钮等收敛至 `LearningPalette.Srs*` / `StarAmber` / `ActionDarkBg`)
9. `presentation/components/cards/SRSLearningCard.kt` (7 处，Indigo 等级标签与 CardBadge 状态胶囊色收敛至 `LearningPalette.Indigo*` 与 `LearningPalette.Badge*`，界面展示文案收编入 `strings.xml`)
10. `presentation/components/cards/SRSGrammarCard.kt` (14 处，Indigo 标签、Yellow TIPS 提示色、CardBadge 状态色、`ExampleBoxDark` 收敛至 `LearningPalette`，文案收编入 `strings.xml`)
11. `presentation/components/dialogs/DailyAchievementShareDialog.kt` (4 处，收敛至 `LearningPalette.Share*`，文案收编入 `strings.xml`)
12. `presentation/components/dialogs/LearningOverviewDialog.kt` (4 处，收敛至 `LearningPalette.Badge*`，文案收编入 `strings.xml`)
13. `presentation/components/dialogs/QueueExplanationDialog.kt` (4 处，收敛至 `LearningPalette.Badge*`，文案收编入 `strings.xml`)
14. `presentation/components/dialogs/ContentReportDialog.kt` (13 处，深浅灰度色阶收敛至 `LearningPalette.ReportZinc*`，文案 100% 原文忠实提取至 `strings.xml`)
15. `presentation/components/common/NemoWhiteboard.kt` (11 处，白板笔刷 8 色与深色主题工具栏底色收敛至 `LearningPalette.Whiteboard*` 与 `NemoSurface*`，文案收编入 `strings.xml`)
16. `presentation/components/common/LearningFinishedContent.kt` (13 处，结算完成界面徽章点缀色与加餐暖色收敛至 `BentoColors.*`、`LearningPalette.*` 与 `NemoNeutrals.*`，文案收编入 `strings.xml`)
17. `presentation/verb/VerbConjugationHandbookScreen.kt` (9 处，避坑警告色盘收敛至 `LearningPalette.Verb*`，文本提取至 `strings.xml`)
18. `presentation/kana/KanaChartScreen.kt` (13 处，五十音图专属色盘收敛至 `LearningPalette.Kana*` 与 `NemoNeutrals.*`，文本提取至 `strings.xml`)
19. `presentation/ai/AIReadingHistoryScreen.kt` (11 处，阅读历史项、得分环及状态收敛至 `LearningPalette.Reading*` 与 `LearningPalette.Ai*`，彻底移除局部私建颜色对象，文本收编入 `strings.xml`)
20. `presentation/ai/AIWorkshopScreen.kt` (15 处，AI 思考卡片、双态胶囊轨底/描边/高亮色收敛至 `LearningPalette.Ai*` 与 `LearningPalette.ReadingAccentGreen`，文案提取至 `strings.xml`)
21. `presentation/ai/AIReadingScreen.kt` (22 处，主题卡片点缀色、柔和阴影、AI 思维链硬解码及正文暗色收敛至 `LearningPalette.Reading*` 与 `LearningPalette.Ai*`，文案提取至 `strings.xml`)
22. `presentation/components/common/LearningCommonComponents.kt` (34 处，状态数量色、极光遮罩纵深色阶、冷却战报卡片与磨砂液态玻璃按钮收敛至 `LearningPalette.Badge*`、`LearningPalette.AuroraMask*` 与 `LearningPalette.Cooldown*`，文案提取至 `strings.xml`)

### 2.2 治理总结与状态
- 全项目 89 个文件中初始存在的 789 处裸十六进制色值泄漏（A1 红线违规），除用户本地明确排除的 20 个工作区文件外，已全量、彻底实现 **0 违规点**。
- 所有提取至 `strings.xml` 的展示文本均 100% 忠实于源码字面量，无任何凭空推断或擅自发挥。
- 严格遵循红线纪律：全程无任何脚本运行，原生工具手工修改，零触碰排除文件，零动态取色，零口水注释。

