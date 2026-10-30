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
- 视觉设计重明确：根据用户最新明确的要求与参考图片（`media_1793360166879_a3af3de1.jpg`），明确单选框及选项指示器（非仅勾无底，非未选留白），统一采用：
  1. 选中状态：实心主题色圆底 + 居中白色对勾，且严格动态适配当前 MaterialTheme 动态主题色；
  2. 未选中状态：灰色轮廓空心圆圈（无对勾，灰色外边框，内部透明）。
- 落地规范：
  - `:core:ui` 中的 `NemoRoundCheckbox` 作为全局基础规范组件，边框与底色加入平滑过渡与弹性缩放，并添加 Light / Dark 模式 Compose Previews。
  - `ThemeSettingsScreen.kt` 的 `StrategyOptionItem` 和 `IconStyleOptionRow` 接入 `NemoRoundCheckbox`（20.dp），并提供对应 Compose Previews。
  - `AIReadingScreen.kt` 的 `QuestionCard` 单选作答选项接入 `NemoRoundCheckbox`（18.dp，内部对勾 12.dp），并提供 Compose Previews。

