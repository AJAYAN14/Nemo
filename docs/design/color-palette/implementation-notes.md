# 色彩规范库调整与修复实施笔记 (implementation-notes.md)

## 1. 实施目标与决策基准

针对审查报告中确认的 6 大类核心问题进行全面代码与文档级修复：
1. **多源仓库色值冲突**：统一采用现代标准规范库（`kunal 库` 与 `phpcolor 库`）为基准，并在文档中详尽标注 `innei 库` 和 `brillaint 库` 的历史或个人偏差。
2. **源仓库源码 Bug 修正**：更正桌面标准蓝绿（`desktopTeal`）浅色 Hex 为 `#59ADC4`（对应其真实 RGB `89, 173, 196`），消除 `phpcolor 库` 的原始笔误 `#59ADCC`。
3. **补全桌面深色灰**：补全 `desktopGray`（浅色 `#8E8E93`，深色 `#98989D`）。
4. **跨文件完整性补齐**：在 `AppColors.kt` 中补齐全部 18 个无障碍高对比色、4 个桌面色、13 个空间氛围色；在 `colors.xml` 与 `colors-night.xml` 中补齐桌面色与空间色。
5. **通道排列与透明度修正**：`COLORS.md` 表头与内容明确区分 Android ARGB 规范（`#AARRGGBB`）与 Web RGBA 规范（`#RRGGBBAA`）；三级文本统一采用 `30%` 透明度（Android Hex `#4C`，十进制 76）。
6. **英文拼写修正与向前兼容**：纠正 `Pomegranate`、`Cerulean`、`LavenderGray` 拼写，并在 Kotlin 与 XML 中保留旧命名别名保证向下兼容。
7. **违规词脱敏**：全文严格杜绝任何品牌违规词汇。

---

## 2. 变更记录 (Deviations & Discovered Edge Cases)

| 变更项 | 涉及文件 | 原状态 | 修复后状态 | 决策依据与理由 |
| :--- | :--- | :--- | :--- | :--- |
| `desktopTeal` Hex | `colors.json`, `COLORS.md`, `colors.xml`, `AppColors.kt` | 浅色 `#59ADCC` | 浅色 `#59ADC4` | 89, 173, 196 的真实 Hex 为 `#59ADC4`，原库桌面端配置中存在源码笔误。 |
| `desktopGray` 补全 | 全文件 | 缺失深色灰色差异项 | 补全浅色 `#8E8E93` / 深色 `#98989D` | 桌面规范中深色灰为 `#98989D`，与移动端深色灰 `#8E8E93` 存在差异。 |
| 无障碍高对比色 | `AppColors.kt` | 完全缺失 (0 项) | 完整补齐全部 18 项（浅色 + 深色） | 保证 Compose 架构与 Android XML 资源 100% 对齐。 |
| 桌面色与空间色 | `colors.xml`, `colors-night.xml`, `AppColors.kt` | 缺失 | 完整定义与实现 | 消除跨文件定义断层。 |
| 语义透明色格式 | `COLORS.md`, `colors.json` | 标注 ARGB 但实际为 `RRGGBBAA` | 明确标注 Android `#AARRGGBB` 并附注 Web 规范 | 彻底消除 Android 端解析导致的通道倒置隐患。 |
| 三级文本透明度 | `colors.json`, `COLORS.md` | `#3C3C434D` (77) | `#4C3C3C43` (76) | 与 `colors.xml` 和 Compose `0x4C3C3C43` 保持 100% 一致。 |
| 拼写笔误纠正 | `AppColors.kt`, `colors.xml`, `colors.json`, `COLORS.md` | `Pomengrate`, `Cerulian`, `LavendarGrey` | `Pomegranate`, `Cerulean`, `LavenderGray` (附带兼容别名) | 修复源库历史笔误，保障专业性与向前兼容。 |

---

## 3. 待核实与复核结果 (Review Verification)

- `colors.xml` 最终有效颜色项：153 项（含兼容别名 156 项）
- `colors-night.xml` 最终有效颜色项：58 项
- `AppColors.kt` 包含：Tokens（浅色/深色各 58 项，空间色 13 项，经典单色 79 项）
- `colors.json` 包含 6 个分类，统一 Schema 结构，含多源差异对比注释。
- `COLORS.md` 包含全量对照表及源仓库差异对照表。
