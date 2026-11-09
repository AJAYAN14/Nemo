# 18-液态玻璃菜单原生手势引擎复用与Compose单向事件闭环避坑指南

> 沉淀 Nemo 项目在对标复刻 iOS 原生液态毛玻璃菜单（`expo-glass-custom-menu`）时，解决 Compose 子项事件竞争截断、长按原地盲区以及流体果冻（Jelly）拉伸手势完全还原的核心经验。

---

## 避坑复盘对照表

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **Compose 子项 `.clickable` 抢占消费拖拽事件**：在 Compose 内部为每个菜单项设置独立点击事件，长按拖动时子项判定离开点击热区触发“取消点击”，重置按下状态并清空高亮板，导致手势激烈打架与高亮闪烁。 | **原生单层 Overlay 全局接管公理**：彻底废弃 Compose 内部细碎子项手势监听，由顶层统一的 `GlassMenuOverlay`（Native ViewGroup）在 `onTouchEvent` 中单源捕获全局 `MotionEvent`，并由原生 `RenderNode` 进行无重组录制渲染。 | `GlassMenuOverlay.kt`, `GlassMenuSession.kt`, `NemoLiquidMenu.kt` | **全局单一眼球捕获公理**：流体物理菜单的手势与渲染必须是单一真理源（Single Source of Truth），严禁拆解到各子控件进行多头竞争。 |
| **长按触发按钮滑选被 `wasExpandedAtDown` 逻辑短路**：在触发按钮上检测拖拽时，使用了按下瞬间的收起状态标记，导致用户长按按钮直接向下滑选时，滑选逻辑被前置条件全盘拦截，松手无法触发选中。 | **无差别单次滑选手势穿透**：触发按钮在 `ACTION_DOWN` 瞬间即调用 `open()` 拉起会话，后续滑动直接穿透至 `session.onTouch(MOVE)` 进行双曲正切流体形变与选项吸附，支持“按住-下滑-松手触发”一气呵成。 | `NemoGlassMenuView.kt`, `GlassMenuSession.kt` | **连续手势闭环原则**：菜单的展开与滑选属于单一连续动作，严禁人为设定状态闸门切断手势传递链条。 |
| **原地长按手势盲区与物理微膨胀丢失**：原实现错误地将 `onPointerDown` 包裹在 `distSq > 10dp` 阈值内，导致手指在选项或按钮上原地长按不动时，手势系统完全不认为发生了触控，流体受压与聚光光晕无法展现。 | **DOWN 事件零延迟响应机制**：无论是否发生位移，只要发生 `ACTION_DOWN`，立即执行 `jelly.down(x, y)` 启动弹簧受力计算与高光光斑流动，同时以 `immediate = true` 吸附高亮板。 | `GlassJelly.kt`, `GlassMenuSession.kt` | **物理触控即时性原则**：物理材质仿真必须对受力做出即时反馈，位移阈值仅用于区分长列表滚动与行内微移，绝不能用于阻断按压物理计算。 |
| **渲染位移坐标系与几何碰撞热区脱节**：渲染层容器使用全局绝对屏幕坐标平移，而手势命中计算却使用相对形变差值叠加局部坐标，导致菜单边缘在果冻拉伸形变时手指与吸附热区出现物理错位。 | **单源坐标体系统一对齐**：统一使用相对于菜单窗口的全局绝对坐标计算 `rowAt(x, y)` 与 `nearestRow(x, y, slack)`，与底层 Shader 的 `state.incoming` 保持严格空间同步。 | `GlassMenuSession.kt`, `GlassMenuContent.kt` | **几何与着色空间完全重合原则**：视觉渲染变形与手势几何命中必须基于同一份动态物理 Bounds，严防计算双轨割裂。 |

---

## 落地架构与实现全景

```
[LearnHeaderMoreMenuButton] (Compose 层)
           │
           ▼
[NemoNativeLiquidMenuTrigger] ──> [AndroidView]
                                         │
                                         ▼
                             [NemoGlassMenuView] (纯原生触发器)
                                         │  ACTION_DOWN
                                         ▼
                             [GlassMenuOverlay.obtain] (DecorView 顶层挂载)
                                         │
                         ┌───────────────┴───────────────┐
                         ▼                               ▼
               [GlassMenuSession]               [GlassMenuContent]
               ├── onTouch() 原生分发           ├── 动态测量与排版
               ├── GlassJelly (果冻物理拉伸)    ├── TextPaint 绘制
               ├── GlassSpring (阻尼弹簧回弹)   ├── GlassIcons (矢量图标)
               └── RenderNode 120fps 录制       └── Plate (高亮吸附板)
```

---

## 总结
通过完全复用参考工程中经过工业级调优的纯原生核心引擎，彻底解耦了 Compose 的手势拦截机制，在保证业务逻辑零破坏的前提下，实现了与原工程 100% 毫无二致的触觉、视觉与流体物理动效。
