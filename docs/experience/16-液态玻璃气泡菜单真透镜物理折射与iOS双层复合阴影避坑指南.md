# 液态玻璃气泡菜单真透镜物理折射与 iOS 双层复合阴影避坑指南

> 本文档记录气泡菜单从“伪磨砂发闷”彻底演进为“真·物理透镜折射（`lens` + `vibrancy` + 16dp 黄金光斑 + `Highlight.Plain` 晶体高光 + iOS 双层复合阴影）”全过程中的物理光学原理、底层渲染避坑经验与工程落地原则。

---

## 核心避坑对照表 (四列格式)

| 坑是什么 (Problem) | 怎么避 (Workaround / Solution) | 相关工具 / 文件 | 统一原则 (Design Axiom) |
| :--- | :--- | :--- | :--- |
| **超大模糊（50dp）将光影熨平成“单色死板”**<br>试图通过加大模糊半径（50dp）营造朦胧感，结果将背后图文起伏与明暗边缘彻底抹平成无对比度的均匀死色，人类大脑无法感知透光，误判为“厚重塑料板”。 | **收敛至黄金光斑档位（`16dp`）**：严格设定模糊半径为滑块中值 `blur(16.dp.toPx())`，既能柔化底层文字干扰，又能清晰保留卡片与图标的柔美光晕，让底层光影自然穿透上来。 | `NemoPopupMenu.kt`<br>`BackdropEffectScope`<br>`blur(16.dp)` | **环境光斑可辨性公理**：真实透明玻璃的感知依赖于背后物体光斑轮廓的隐约穿透；严禁使用超过 24dp 的超大模糊将背景彻底抹成均一纯色。 |
| **实色微染色蒙层（`glassTintOverlay`）扼杀透光率**<br>在模糊折射层之上额外叠加一层 25%~32% 的黑/白不透明底色，导致光线透射率直接被砍掉三分之一，产生类似磨砂塑料的泛白雾感或发脏沉闷感。 | **100% 零实色遮挡纯净物理折射**：彻底拔除 AGSL / Backdrop 渲染层的 `.background(glassTintOverlay)`，让 GPU 帧缓冲合成的穿透光线零损耗直达眼睛，水光水感拉满。 | `NemoPopupMenu.kt`<br>`DrawBackdropModifier.kt` | **无杂质纯净光学折射原则**：液态玻璃容器表面严禁叠加超过 10% 的实色遮罩，透明质感必须交由物理折射与色彩活力着色器完全掌管。 |
| **单层大半径漫反射投影导致边缘发虚漂浮**<br>仅使用一层 32dp 的深远大投影时，卡片边缘由于缺乏近距离暗度锚定，产生“边缘发虚”、“与背景缺乏咬合感”、“像贴纸一样浮空”的失真感。 | **构建 iOS 标准双层复合物理阴影**：<br>① **贴身微环境光（Ambient）**：8dp 模糊 + 2dp 偏移（深色 35% 黑，浅色 4% 黑），精细锁紧边缘立体切面；<br>② **扩散主漫反射（Key）**：26dp 模糊 + 12dp 偏移，托起三维悬浮空气感。 | `NemoPopupMenu.kt`<br>`BlurMaskFilter`<br>`drawBehind` | **双层复合空间层次公理**：高阶悬浮弹窗必须拆解为紧致环境阴影（锁边）与漫反射投影（悬浮）双层管线，杜绝单层大投影导致的边缘失真虚化。 |
| **外部人工 Shader 压暗失真 vs 原生物理透镜折射弯曲**<br>外部 Shader 若仅作贝塞尔明度映射，缺乏真实的物理厚度形变与凸透镜光学弯折，底层元素划过边缘时毫无流动感，显得呆板僵硬。 | **接入原生 `vibrancy()` + `lens(...)` 凸透镜折射管线**：调用项目原生的 `vibrancy()` 提纯饱和度与明度，并配置 `lens(refractionHeight = 0.2f * minDimension * 0.5f, refractionAmount = 0.2f * minDimension, depthEffect = true)`，赋予防边缘露底与水滴般的光线弯折。 | `LiquidGlassButtons.kt`<br>`Lens.kt`<br>`ColorFilter.kt` | **透镜物理形变真实性公理**：液态玻璃不仅是模糊，核心在于透镜（Lens）边缘对光线的折射率弯折；全项目液态玻璃必须优先消费项目内置的透镜算法。 |
| **机械式各向同性缩放动画缺乏生命张力**<br>使用单一 `scale` 各向同性缩放（X 与 Y 轴同步从 0.4 到 1.0），弹窗如同刚性物体机械放大，缺乏水滴/气泡自锚点泵出的有机流体质感。 | **表面张力非对称弹性形变动效**：将动画拆解为 `scaleXAni`（初态 0.65f，`dampingRatio = 0.82f`）与 `scaleYAni`（初态 0.45f，`dampingRatio = 0.76f`），呈现横向先展、纵向弹性拉伸泵出的物理表面张力感。 | `NemoPopupMenu.kt`<br>`Animatable`<br>`spring` | **流体表面张力动效公理**：液态视觉元件的进入动效必须具备非对称物理形变（Asymmetric Deformation），通过差异化阻尼模拟自然界流体泵出。 |

---

## 落地规范检查台账

1. **项目原生光学参数对标**：
   - 色彩活力：`vibrancy()`（消除高斯卷积后的混色灰暗）
   - 模糊半径：严格对标游乐场滑块中值 `16.dp`（`blur(16.dp.toPx())`）
   - 边缘透镜折射：`lens(refractionHeight = 0.1f * minDimension, refractionAmount = 0.2f * minDimension, depthEffect = true)`
   - 晶体高光：`Highlight.Plain`
2. **结构与阴影层次**：
   - 双层阴影：`Ambient` (8dp, dy 2dp) + `Key` (26dp, dy 12dp)
   - 项间分割线：`0.5.dp` 微透水平线（浅色 5% 黑轻影，深色 8% 白微光，两侧内缩 14dp）
   - 外框曲率：全量采用 G2 连续平滑曲率 `RoundedRectangle(18.dp)`
3. **编译与质量保障**：
   - 全工程 `./gradlew compileDebugKotlin` 0 警告中断、0 错误，顺利通过构建（BUILD SUCCESSFUL）。
