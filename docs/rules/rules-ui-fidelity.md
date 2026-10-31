# Nemo UI 视觉与资源规范纪律（Rules）

> **适用对象**：本仓库的所有 AI 与人工开发工作（含代码生成、UI 界面构建、重构与代码审查）  
> **配套文档**：`.agent/rules/nemorules.md`（行为硬约束）· `docs/rules/rules-code-style.md`（代码风格与注释）· `docs/design/Dynamic_Color_System_Spec.md`（深浅色规范）· `docs/design/标题与卡片对齐设计规范.md`（布局排版规范）

---

## 0. 核心总纲

> **设计规范是视觉呈现的唯一法典。**  
> **在业务代码中随意硬编码颜色、文案与布局参数，视同破坏工程资产完整性。**

---

## 1. 颜色纪律（集中管理 + 规范对齐）

### 1.1 唯一合法居所
全工程中，色值字面量 `Color(0x...)` 的唯一合法居所为：
**`:core:designsystem` 的 `theme/Color.kt`**

```text
core/designsystem/src/main/java/com/jian/nemo/core/designsystem/theme/
├── Color.kt             # 全项目唯一允许出现 Color(0x...) 裸色值字面量的文件
├── Typography.kt        # 字体排版与命名 TextStyle（引用 Color.kt 常量或语义槽位）
└── Theme.kt             # 主题与 ColorScheme 组装（引用 Color.kt 常量，内部零裸色值）
```

- **业务代码绝对禁令**：除 `core/designsystem/**/theme/Color.kt` 外，任何 Feature 模块、组件 Composable、Screen 或 ViewModel 中，**严禁出现 `Color(0x` 裸十六进制字面量**（违规直接阻断）。
- **取用规范（优先级从高到低）**：
  1. 优先映射 `MaterialTheme.colorScheme` 语义槽位（`primary`、`surface`、`background`、`error` 等）；
  2. 品牌特定色/特定卡片色盘：直接引用 `Color.kt` 中声明的命名语义常量（如 `NemoPrimary`、`NemoDanger`、`TestResultPalette`）；
  3. 系统透明与中性锚点：仅允许使用标准常量（如 `Color.Transparent`、`Color.White`、`Color.Black`）。

### 1.2 严禁动态取色与色盘自动推导（硬性红线）
- **严禁 Android 系统壁纸动态取色（Dynamic Color）**：
  - 严禁在 `Theme.kt` 或任何主题入口中调用 `dynamicLightColorScheme()` 或 `dynamicDarkColorScheme()`。
  - 严禁在主题 Composable（如 `NemoTheme`）中保留默认的 `dynamicColor: Boolean = true` 开关参数。
  - 必须彻底禁用系统壁纸色彩提取，确保全平台无论在任何 Android 版本上，均稳定采用 Nemo 的纯正视觉调色。
- **深浅色模式必须 100% 遵照设计规范**：
  - 应用深色（Dark Mode）与浅色（Light Mode）的色彩架构、表面明度阶梯（Level 0 至 Level 3）、前景对比度及强调色明度补偿，**必须严格遵循 [`docs/design/Dynamic_Color_System_Spec.md`](file:///E:/Android/Nemo/docs/design/Dynamic_Color_System_Spec.md)**，严禁凭空臆造未经定义的中介色。

---

## 2. 文本资源纪律（收编 strings.xml，严禁硬编码）

### 2.1 基本规则
- 所有面向用户的界面展示文本（含 `Text`、`contentDescription`、Toast/Snackbar、按钮、标签、标题、弹窗与占位符文案）**一律收编进各模块的 `res/values/strings.xml`**。
- **严禁在 Compose 渲染函数中直接硬编码中文或英文展示字符串字面量**。
- Composable 上下文一律通过 `stringResource(R.string.xxx)` 取用；非组合上下文（如 ViewModel/工具类）通过 `context.getString(R.string.xxx)` 取用。
- 含有动态参数的文案：必须在 XML 中使用标准占位符（如 `%1$d`、`%1$s`），并在 Kotlin 中通过 `stringResource(R.string.xxx, arg)` 传参。

### 2.2 资源 Key 命名格式
严格遵循 [`docs/todo/01_UI字符串规范化与国际化迁移待办.md`](file:///E:/Android/Nemo/docs/todo/01_UI字符串规范化与国际化迁移待办.md) 确立的命名规范：

**格式：`[模块]_[页面/组件]_[类型]_[描述]`**

| 类型代码 | 对应场景 | 命名示例 |
|---|---|---|
| `title_` | 页面大标题、卡片分组标题 | `home_bento_learn_title` |
| `label_` | 辅助标签、状态描述、进度说明 | `home_bento_review_progress_label` |
| `btn_` | 按钮点击操作文案 | `home_btn_continue_learning` |
| `dialog_` | 弹窗标题、确认正文、操作提示 | `dialog_confirm_exit_content` |
| `format_` | 含有格式化占位符的文案 | `format_daily_goal_hint` |
| `empty_` | 列表或卡片空状态说明 | `home_review_empty_tip` |

### 2.3 硬编码例外白名单（仅以下允许留在 `.kt` 文件）
1. 单元测试与仪器化测试代码（`src/test` / `src/androidTest`）以及开发断言。
2. 日志打印输出（`Log.d`、Timber 等调试字符串）。
3. Navigation 类型安全路由的 `@Serializable` 路由标识类名（如 `data object LearningRoute`）。
4. 正则表达式、MIME 类型、本地存储文件名、Preferences 的 Key 名。
5. **语言选择界面的“语言自称名”**（如 `English`、`简体中文`、`日本語`）：属于语言固定元数据，不随系统语言环境动态切换。

---

## 3. 页面标题与卡片对齐规范（设计路由指针）

> **纪律原则**：规则文档不堆砌具体排版参数，开发者与 AI 必须按需主动查阅专有设计规范。

- **按需查阅红线**：
  凡涉及到页面分组标题、卡片容器对齐、左右安全边距、内部内容微缩进以及亲密性纵向节奏排版等 UI 开发时，**严禁自行设定或猜测间距参数**。
- **强制对齐目标**：
  必须主动查阅并严格遵循 **[`docs/design/标题与卡片对齐设计规范.md`](file:///E:/Android/Nemo/docs/design/标题与卡片对齐设计规范.md)**，按该文档规定的水平基准与间距阶梯依规实现。

---

## 4. 自动化审计清单（提交前机器与人工核验）

凡命中以下任何一条红线，视同违规代码，阻断入库：

| 编号 | 检查项 | 违规判据（命中即阻断） | 处置级别 |
|---|---|---|---|
| **A1** | 色值泄漏 | 除 `core/designsystem/**/theme/Color.kt` 外，任何 `.kt` 出现 `Color(0x` | 🔴 阻断 |
| **A2** | 模板污染 | 源码中出现 `Purple80`、`Purple40`、`Pink40` 等 Android 默认模板色名 | 🔴 阻断 |
| **A3** | 文本硬编码 | Composable 内 `Text("` 直接编写中文/英文展示字面量（不在白名单内） | 🔴 阻断 |
| **A4** | 动态取色泄漏 | 代码中出现 `dynamicLightColorScheme` / `dynamicDarkColorScheme` / `dynamicColor` | 🔴 阻断 |
| **A5** | 标题/卡片对齐失范 | 未遵循 `docs/design/标题与卡片对齐设计规范.md` 造成分组标题与卡片边缘脱焦错位 | 🟠 Review 拦截 |

---

## 5. 正反示例速查

```kotlin
// ❌ 违规 1：业务层出现裸色值 + 字符串硬编码 + 未遵照规范设定随意边距
Text(
    text = "继续学习",
    color = Color(0xFF007AFF), // 违规 A1：色值泄漏！
    modifier = Modifier.padding(14.dp) // 违规 A5：未按规范查阅对齐基准
)

// ❌ 违规 2：主题开启动态取色
@Composable
fun NemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // 违规 A4：严禁动态取色！
    content: @Composable () -> Unit
) { ... }

// ✅ 合规 1：文案收编 strings.xml、颜色走主题槽位或语义 Token
Text(
    text = stringResource(R.string.home_btn_continue_learning),
    color = MaterialTheme.colorScheme.primary, // 或 NemoPrimary
    style = MaterialTheme.typography.titleMedium,
    modifier = Modifier.padding(horizontal = 16.dp) // 严格遵循标题与卡片对齐设计规范
)

// ✅ 合规 2：主题锁死固定静态深浅配色，彻底禁用动态取色
@Composable
fun NemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NemoDarkColorScheme else NemoLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NemoTypography,
        content = content
    )
}
```

> **给 AI 的最终纪律**：写 UI 代码前自问三个问题——“颜色在 Color.kt 里收敛了吗？文案在 strings.xml 里定义了吗？标题和卡片间距对照设计规范查过了吗？”凡未按规实现的 UI，一律拒绝提交。
