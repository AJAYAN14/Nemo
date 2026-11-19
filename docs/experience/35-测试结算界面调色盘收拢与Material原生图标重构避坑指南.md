# 测试结算界面调色盘收拢与Material原生图标重构避坑指南

> 本文档总结 Nemo 测试结算界面重构中，彻底拔除历史混杂孤岛色板（`TestResultPalette` 中的 Tailwind / Material 2 残留）、全量收拢至 `docs/design/color-palette` 规范体系与 Material 3 语义槽位、升级官方原生 Material 实心图标（`Icons.Rounded.*`）的避坑经验。

---

## 1. 核心技术问题与背景

1. **孤立调色板与多重历史设计残留**：
   原先测试结算界面的三大指标卡片（答对、答错、用时）使用了集中在 `TestResultPalette` 中的历史色值。答对使用了深沉墨绿（`#0B5F49`）与 MD2 浅绿（`#E8F5E9`），答错使用了 Tailwind 风格暗红（`#922626`）与浅红（`#FEE2E2`），用时使用了 Tailwind / MD2 黄（`#8A4318` / `#FFF8E1`）。整体在现代 OLED / 高分屏上显得沉闷脏深，与项目核心规范 `docs/design/color-palette` 及 `docs/design/Dynamic_Color_System_Spec.md` 严重脱节。
2. **副文本硬编码 Tailwind 灰阶破坏环境对比度**：
   说明文字使用了 `SecondaryTextDark`（`#A5B4C6`）与 `SecondaryTextLight`（`#64748B`），为静态固定灰阶，无法契合 MaterialTheme 动态深浅模式的 60% 对比度二级副文本（`secondaryLabel`）规范。
3. **内容分布条与光晕色调偏离原生体系**：
   词汇与语法比例条及右下角环境辅助光晕采用了 Tailwind Blue 与 Violet，而非规范定义的 `systemBlue` 与 `systemPurple`。
4. **图标库风格分裂与口水注释残留**：
   原界面混用了 `Icons.Default.*` 与 `Icons.Filled.*`，并在源码中残留了诸如 `// 原生奖杯 SVG` 的防御性口水注释。

---

## 2. 解决方案与实施策略

1. **指标卡片直通 M3 语义容器槽位**：
   - 答对卡片直接接入 `MaterialTheme.colorScheme.secondaryContainer` 与 `onSecondaryContainer`（浅色 `#DDF5E3` / 深色 `#135324`，高可读前景深浅自适应）；
   - 答错卡片直接接入 `MaterialTheme.colorScheme.errorContainer` 与 `onErrorContainer`（浅色 `#FFE5E5` / 深色 `#801015`，高可读前景深浅自适应）；
   - 用时卡片接入符合 docs 规范 `systemOrange` 体系的标准琥珀暖橙容器与自发光前景；
   - 底层 `TestResultPalette` 集中色板全面同步重构，使得依赖该调色板的全局其他弹窗与题目组件同步纯净规范化。
2. **副文本收归 `onSurfaceVariant`**：
   将 `textSub` 统一收拢为 `MaterialTheme.colorScheme.onSurfaceVariant`（对应浅色 `#8A8A8E` / 深色 `#8E8E93`），完美达成 60% 对比度二级文本自适应。
3. **主色调统一至 `systemBlue` 与 `systemPurple`**：
   词汇比例条使用 `systemBlue`（`0xFF007AFF` / `0xFF0A84FF`），语法比例条及环境光晕使用 `systemPurple`（`0xFFAF52DE` / `0xFFBF5AF2`）。
4. **图标全面收拢至 Google Fonts 官方原生实心 Rounded**：
   - 关键卡片图标替换为 `Icons.Rounded.Check`、`Icons.Rounded.Close`、`Icons.Rounded.AccessTime`；
   - 顶部勋章升级为 `Icons.Rounded.EmojiEvents`、`Icons.Rounded.Star`、`Icons.Rounded.CheckCircle`、`Icons.AutoMirrored.Rounded.MenuBook`；
   - 战报图标升级为 `Icons.Rounded.Whatshot`；
   - 彻底清除源代码中的元说明与口水套话注释。

---

## 3. 四列对照避坑台账

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **局部孤岛色板导致色彩发脏断层**<br>使用历史硬编码 `TestResultPalette`，杂合 Tailwind 与 MD2 旧色，破坏全局系统质感。 | 坚决重构孤岛色值，优先直通 `MaterialTheme.colorScheme` 语义容器槽位（`secondaryContainer`、`errorContainer`）与标准系统主色。 | `Color.kt`<br>`TestResultComponent.kt`<br>`Dynamic_Color_System_Spec.md` | **规范单一来源与语义槽位优先公理**：全应用色彩一律收拢至系统调色盘与 Material 3 动态语义槽位，杜绝孤岛自造杂合色。 |
| **硬编码 Slate 灰阶导致副文本在暗色下可读性劣化**<br>副文本使用硬编码静态 Slate 灰，无法跟随主题自适应衰减。 | 拔除静态灰阶，统一映射至 `MaterialTheme.colorScheme.onSurfaceVariant`（`secondaryLabel` 60% 对比度）。 | `TestResultComponent.kt`<br>`Theme.kt` | **环境光自适应与层级对比度原则**：文本色必须依赖动态语义层级映射，浅色抑光高对比、深色发光不疲劳。 |
| **图标库系列混用且存在多余口水注释**<br>代码中使用 Default/Filled 杂合图标，且源码残留 `// 原生奖杯 SVG` 等防御性元注释。 | 全量统一为官方原生实心 `Icons.Rounded.*` 系列，严格清理所有非技术解释性元注释。 | `Icons.Rounded.*`<br>`rules-code-style.md`<br>`nemorules.md` | **图标官方纯正化与注释纯净纪律**：全项目图标一律选用官方实心 Rounded 系列；源码禁止存在任何元说明或口水套话。 |
