# Glasense 液态磨砂气泡菜单移植与 AGSL 材质着色器避坑指南

> 本文档记录将 Cresto 高级亚克力液态磨砂气泡菜单（`GlasenseMenu`）移植到 Nemo 项目中，融合 AGSL 贝塞尔明度映射材质着色器、50dp 极柔模糊、双阶物理高光及跨版本渲染适配过程中的关键避坑经验与架构原则。

---

## 核心避坑对照表 (四列格式)

| 坑是什么 (Problem) | 怎么避 (Workaround / Solution) | 相关工具 / 文件 | 统一原则 (Design Axiom) |
| :--- | :--- | :--- | :--- |
| **Compose 版本未公开 `containerSize` 导致编译失败**<br>直接调用 `LocalWindowInfo.current.containerSize` 获取窗口视口尺寸时，由于当前 Compose 基础库版本尚未完全开放该属性，触发 `Unresolved reference 'containerSize'` 阻断编译。 | **采用 LocalConfiguration 稳健转换像素**：使用稳定通用的 `LocalConfiguration.current.screenWidthDp.dp` 与 `screenHeightDp.dp`，配合 `LocalDensity` 计算窗口物理视口 `IntSize`，彻底杜绝跨版本 API 破坏。 | `NemoPopupMenu.kt`<br>`LocalConfiguration`<br>`LocalDensity` | **视口尺寸单一稳定来源公理**：全项目浮层与锚点计算，统一通过 `LocalConfiguration.current` 计算屏幕安全边界，严禁直接依赖平台未完全公开的试验性接口。 |
| **触觉反馈枚举版本兼容断层（`ContextClick` 符号缺失）**<br>直接调用实验性触觉枚举 `HapticFeedbackType.ContextClick` 导致编译报错。 | **全项目收敛至 `TextHandleMove` 标准轻微触感**：统一采用 Android Compose 官方标准常量 `HapticFeedbackType.TextHandleMove`，既能提供清脆细腻的就地操作物理震感，又确保端到端编译 100% 稳定通过。 | `NemoPopupMenu.kt`<br>`LocalHapticFeedback` | **原生轻触觉单一来源公理**：全应用列表点击与气泡菜单展开，统一采用 `HapticFeedbackType.TextHandleMove`，严禁随意引入未经准入的特定版本触觉枚举。 |
| **Compose Paint 与 Android 原生 Paint 类型推导歧义**<br>在 Compose 中使用 `androidx.compose.ui.graphics.Paint().nativePaint` 易因扩展方法与平台类型推导断层导致 `Cannot infer type for this parameter` 编译中断。 | **别名显式引用 `android.graphics.Paint`**：使用 `import android.graphics.Paint as AndroidPaint`，在 `remember(shadowRadiusPx)` 中显式构造，直接配置 `isAntiAlias` 与 `BlurMaskFilter`，在 Canvas 原生画布上稳定绘制漫反射软阴影。 | `NemoPopupMenu.kt`<br>`AndroidPaint`<br>`BlurMaskFilter` | **图形学底层原生直用原则**：凡涉及到原生 Canvas、BlurMaskFilter 及底层渲染滤镜的自绘逻辑，一律显式声明 `android.graphics.Paint`，杜绝 Compose 包装类类型推导陷阱。 |
| **50dp 超大模糊半径导致边缘露底黑斑与硬切断裂**<br>直接设置 `blur(50.dp.toPx())` 时，当气泡菜单处于卡片边缘或屏幕边界时，因周围缺乏有效像素，高斯卷积核采样到透明像素产生黑色脏斑或生硬缝隙。 | **Backdrop 显式注入外扩采样（`padding = 100dp`）**：在 `effects` 块中配置 `padding = 50.dp.toPx() * 2` 向外扩展采样源，并在底层列表确保背景底色完整填充，让 50dp 大散射自然过渡不露底。 | `NemoPopupMenu.kt`<br>`BackdropEffectScope`<br>`DrawBackdropModifier.kt` | **大半径模糊采样外扩公理**：凡使用 24dp 以上大半径高斯散射效果，必须显式配置双倍 radius 的采样 padding，消除边缘像素塌陷。 |
| **普通 Blur 背景发灰发浑 vs AGSL 贝塞尔明度映射通透感**<br>传统的纯色半透明叠加上模糊后，浅色模式容易发灰发脏，深色模式容易暗沉发闷，无法呈现出如同 macOS/iOS 般的晶莹通透液态玻璃质感。 | **移植 AGSL 贝塞尔曲线亮度映射着色器（Material Recipes）**：引入三次贝塞尔曲线（`bezierMap`）对底层像素亮度进行非线性重映射，配合饱和度增益（1.5f）与微观抖动降噪（Dither），在 Android 13+（API 33+）上动态注入 `RuntimeShader`，低版本优雅平滑降级。 | `GlasenseMaterialShader.kt`<br>`NemoPopupMenu.kt`<br>`RuntimeShader` | **材质着色器物理真实原则**：真实磨砂玻璃绝非简单的半透明叠色，必须通过非线性明度映射（Bezier Luma Mapping）与微观饱和度补偿，方可唤醒环境光的通透折射。 |

---

## 落地规范检查台账

1. **材质着色器与深浅模式适配**：
   - 浅色模式（Light Mode）：调用 `GlasenseMenuRegularLight`（曲线 `Vec4(0.9f, 0.83f, 0.925f, 0.815f)`，亮度 `+0.1f`，饱和度 `1.5f`），背景提亮为晶莹纸质白磨砂。
   - 深色模式（Dark Mode）：调用 `GlasenseMenuRegularDark`（曲线 `Vec4(0.16f, 0.26f, 0.1f, 0.1f)`，亮度 `0f`，饱和度 `1.5f`），背景智能压暗为沉浸深灰磨砂。
2. **双阶物理边缘高光（Glass Highlight）**：
   - 浅色模式：`Brush.verticalGradient(0f to 55% White, 1f to 6% Black)`
   - 深色模式：`Brush.verticalGradient(0f to 22% White, 1f to 2% White)`
   - 外框圆角：全量严格绑定 G2 连续平滑曲率 `RoundedRectangle(18.dp)`。
3. **就地停靠智能算法（`pickPlacement`）**：
   - 动态感知状态栏、导航栏与软键盘 IME 安全边距；
   - 优先靠右展开（RightTop / RightBottom），与设置项右侧 Value 对齐；
   - 缩放动画变换原点（TransformOrigin）自适应停靠角，迸发生长感极其自然。
4. **编译与质量保障**：
   - 全工程 `./gradlew compileDebugKotlin` 0 警告中断、0 错误，顺利通过构建（BUILD SUCCESSFUL）。
