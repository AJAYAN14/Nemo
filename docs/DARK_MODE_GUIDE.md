# Nemo 项目深色模式设计与开发规范

本指南旨在规范 Nemo 项目中各界面的深色模式（Dark Mode）实现，确保全平台视觉体验的一致性、层次感和可读性。

---

## 1. 核心检测逻辑

在 Composable 组件中，始终基于 `MaterialTheme.colorScheme.background` 的亮度来判断当前是否处于深色模式，而非硬编码判断。

```kotlin
val colorScheme = MaterialTheme.colorScheme
val isDark = colorScheme.background.luminance() < 0.5f
```

## 2. 语义化颜色映射

严禁在 Composable 逻辑中直接使用 `BentoColors.xxx`。必须定义一组动态变量进行语义化转换：

| 语义变量 | 浅色模式（Light） | 深色模式（Dark） | 适用场景 |
| :--- | :--- | :--- | :--- |
| `backgroundColor` | `BentoColors.BgBase` | `colorScheme.background` | 页面最底层背景 |
| `surfaceColor` | `BentoColors.Surface` | `colorScheme.surfaceContainer` | 卡片、对话框背景 |
| `textMain` | `BentoColors.TextMain` | `colorScheme.onSurface` | 主要标题、正文 |
| `textSub` | `BentoColors.TextSub` | `colorScheme.onSurfaceVariant` | 次要描述、说明文本 |
| `textMuted` | `BentoColors.TextMuted` | `onSurfaceVariant.copy(0.6f)` | 禁用或极弱的导出文本 |
| `dividerColor` | `BentoColors.BgBase` | `outlineVariant.copy(0.2f)` | 分割线、进度条底色 |

## 3. 视觉深度与阴影（Z-Axis）

在深色模式下，传统的“物理阴影”难以观察且容易造成视觉脏乱。

- **浅色模式**：使用 `shadowElevation = 2.dp`（或更大）配合默认阴影色。
- **深色模式**：设置 `shadowElevation = 0.dp`。
- **层次表现**：通过容器颜色等级（如 `surfaceContainerLow` 到 `surfaceContainerHigh`）来表现深度，而不是靠阴影。

```kotlin
Surface(
    shape = RoundedCornerShape(26.dp),
    color = surfaceColor,
    shadowElevation = if (isDark) 0.dp else 2.dp
)
```

## 4. 图标与半透明处理

- **图标颜色**：直接使用 `BentoColors` 中的主题色（如 `Primary`）通常在深色模式下过于刺眼。建议使用语义色变量或调节透明度。
- **背景遮罩**：深色模式下的半透明遮罩应使用 `surfaceColor` 或 `Black` 的变体。
- **Avatar/边框**：深色模式下的边框颜色应降低对比度，例如使用 `textMuted.copy(alpha = 0.3f)`。

## 5. 模式切换按钮 (Bento Style)

对于局部切换按钮（如单词/语法切换）：
- 选中项背景在深色模式下应使用 `surfaceContainerHigh`。
- 未选中项文本颜色在深色模式下应使用 `onSurfaceVariant`。

## 6. 开发清单 (Checklist)

1. [ ] 导入 `androidx.compose.ui.graphics.luminance`。
2. [ ] 定义 `isDark` 局部变量。
3. [ ] 将硬编码的 `BentoColors` 引用重构为动态语义变量。
4. [ ] 检查并适配 `shadowElevation`。
5. [ ] 验证圆角组件（如 `Surface`）在深色背景下的轮廓是否清晰。
6. [ ] 确保 `dividerColor` 或 `trackColor` 在深色背景下有足够的辨识度。

---

*参考实现：`HomeScreen.kt`, `ProgressDashboardScreen.kt`*
