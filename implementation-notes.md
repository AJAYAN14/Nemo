# Implementation Notes

## 任务背景
用户要求把主题外观设置中“自动切换模式”的选中状态，由单选框的中间实心圆点改为“勾”。经交互确认，选用方案为**实心圆底白勾**（选中时为主题色实心圆，中心显示白色对勾，符合 iOS / 现代移动端设计规范）。同时用户要求：“可以的话，把项目所有的这种改成勾的”。

## 调查与现状分析
1. `ThemeSettingsScreen.kt`：
   - `StrategyOptionItem`（自动切换模式：跟随系统 / 定时深色模式）：使用了 M3 `RadioButton`（实心圆点）。
   - `IconStyleOptionRow`（图标风格弹窗：Material Design 3 / Plump Duo）：使用了 M3 `RadioButton`（实心圆点）。
2. `AIReadingScreen.kt`：
   - 题目作答单选选项在未提交前使用了 `Icons.Rounded.RadioButtonChecked`（圆圈带实心圆点）。
3. 错题与收藏模块（`feature/collection`）：
   - 之前在 5 个文件中私有实现了 `NemoRoundCheckbox`（22.dp 实心圆底白勾）。
4. 解决方案：
   - 在 `core:ui` 模块（`core/ui/src/main/java/com/jian/nemo/core/ui/component/common/NemoRoundCheckbox.kt`）建立全局公共的 `NemoRoundCheckbox` 组件，带平滑色彩过渡与对勾缩放弹性动效。
   - 替换 `ThemeSettingsScreen.kt` 中 `StrategyOptionItem` 和 `IconStyleOptionRow` 的 `RadioButton`。
   - 替换 `AIReadingScreen.kt` 中的 `RadioButtonChecked`/`RadioButtonUnchecked`。
   - （可选）替换各模块内私有重复声明。

## 发现的边界情况与偏离记录 (Deviations & Discovered Edge Cases)
- 视觉设计演进：用户进一步明确按 iOS 原生规范实现单选列表。根据 iOS Human Interface Guidelines，设置等单选列表中采用“右侧纯蓝色对勾，未选中留白”的最简视觉设计，去除外层灰色空心圆圈噪音，同时与项目中 `SettingsComponents.kt` 内的原生单选列表规范全面对齐。实心圆底白勾组件 `NemoRoundCheckbox` 保留在 `:core:ui` 供批量编辑/多选场景复用。
