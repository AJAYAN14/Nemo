# 07 - G2 连续曲率圆角与液态玻璃 SDF 透镜着色器适配避坑

> 记录 Nemo 全应用迁移至 G2 连续曲率平滑圆角（Squircle）过程中，底层 Backdrop 着色器引擎类型判断遗漏导致运行时崩溃的排查复盘与统一准则。

---

## 一、 四列对照总结

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **运行时闪退 `UnsupportedOperationException`**：当组件接入 G2 连续圆角（`RoundedRectangle` / `Capsule` / `UnevenRoundedRectangle`）后，点击带有磨砂液态玻璃透镜（`lens`）或高光（`highlight`）的组件时，应用直接抛出异常崩溃。 | **底层着色器引擎必须实现全形状类型覆盖**：自定义 Shape 架构必须同步在 Backdrop 库解析几何曲率半径的入口（`Lens.kt`、`HighlightStyle.kt`）添加 `is RoundedRectangularShape` 分支，提取真实切点半径供 AGSL SDF 着色器使用。 | `Lens.kt`<br>`HighlightStyle.kt`<br>`RoundedRectangularShape.kt`<br>`adb logcat -s AndroidRuntime:E` | **自定义图形抽象接口贯穿全栈原则**：上层 UI 形状抽象与底层图形学着色器几何解析必须强绑定；架构演进时绝不可只改上层组件而忽视底层着色器的类型向下断言。 |

---

## 二、 事故现象与堆栈日志

在完成全工程 G2 连续圆角改造并将 Release APK 安装至真实设备后，用户在操作具有液态玻璃透镜效果的组件（如顶栏 SRS 状态 Chip、液态玻璃按钮）时，应用瞬间崩溃退出。
通过 `adb logcat -s AndroidRuntime:E` 捕获到确凿致命堆栈：

```text
FATAL EXCEPTION: main
Process: com.jian.nemo, PID: 18300
java.lang.UnsupportedOperationException: Only RoundedRectangularShape or CornerBasedShape is supported in lens effects.
	at com.kyant.backdrop.effects.LensKt.throwUnsupportedSDFException(Lens.kt:107)
	at com.kyant.backdrop.effects.LensKt.access$getCornerRadii(Lens.kt:63)
	at com.kyant.backdrop.effects.LensKt.lens(Lens.kt:30)
	at com.jian.nemo.core.ui.component.liquid.LiquidGlassButtonsKt...
```

---

## 三、 根因机制分析

1. **类型分支遗漏**：
   `Lens.kt` 的 `cornerRadii` 获取函数通过 `when (val shape = shape)` 解析 AndroidX 原生形状：
   ```kotlin
   // 修复前：仅匹配 AndroidX 旧标准
   when (val shape = shape) {
       is AbsoluteRoundedCornerShape -> { ... }
       is CornerBasedShape -> { ... }
       else -> null // 致命分支：抛出 UnsupportedOperationException
   }
   ```
2. **高阶形状不属于 `CornerBasedShape`**：
   全工程 G2 连续曲率平滑形状（`RoundedRectangle`、`Capsule`、`UnevenRoundedRectangle`）是基于三次贝塞尔方程实现的平滑过渡，实现了 `:core:designsystem` 中的抽象接口 `RoundedRectangularShape`，而非 AndroidX 的四分之一正圆 `CornerBasedShape`。
3. **断言失败与崩溃**：
   在组件调用 `lens(...)` 效果时，由于未能匹配到类型分支，直接进入 `else -> null` 分支并触发 `throwUnsupportedSDFException()`，引发主线程致命崩溃。

---

## 四、 彻底解决方案

在 [`Lens.kt`](file:///E:/Android/Nemo/core/ui/src/main/java/com/kyant/backdrop/effects/Lens.kt#L63) 与 [`HighlightStyle.kt`](file:///E:/Android/Nemo/core/ui/src/main/java/com/kyant/backdrop/highlight/HighlightStyle.kt#L115) 中，扩展对 `RoundedRectangularShape` 的多态支持：

```kotlin
is RoundedRectangularShape -> {
    val size = size
    val maxRadius = size.minDimension / 2f
    val corners = shape.corners(size, layoutDirection, this)
    floatArrayOf(
        corners.topLeft.fastCoerceAtMost(maxRadius),
        corners.topRight.fastCoerceAtMost(maxRadius),
        corners.bottomRight.fastCoerceAtMost(maxRadius),
        corners.bottomLeft.fastCoerceAtMost(maxRadius)
    )
}
```

---

## 五、 统一架构原则

1. **图形抽象必须保持端到端一致性**：引入新的 Shape 体系时，必须同步扫描所有消费该 Shape 的底层模块（不仅是 Compose Modifier，更包含自定义 Shader、AGSL 运行时、Canvas DrawScope、图形图层）。
2. **拒绝抛出硬中断异常**：图形底层着色器在遇到未识别形状时，应提供合理的几何降级方案（如退化为全圆角或标准矩形外框），杜绝直接抛出不可恢复的运行时崩溃。
