# 容器变化过渡动画重构与 NavHost 物理冲突避坑指南

> 本文档记录 Nemo 在重构测试主界面（`TestDashboardScreen`）容器变换动效（Container Transform）过程中的核心暗坑剖析与工业级解决方案，深度融合 Google 官方顶级项目 Jetsnack 与 SSCompose-SharedElement 的设计范式。

---

## 经验总结表

| 坑是什么 (现象与根因) | 怎么避 (具体应对方案) | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **NavHost 水平位移动画与 SharedBounds 空间展开打架**：当主界面卡片通过 `sharedBounds` 向全屏膨胀展开时，NavHost 的 `exitTransition` 强行将主界面向左位移 1/4 屏幕（`-fullWidth / 4`），双重相悖的运动矢量叠加在同一图层，导致卡片在飞行过程中剧烈拉扯抖动和重影。 | 在 `NemoNavHost.kt` 中建立 `isContainerTransformTarget(route)` 路由协调机制。凡是进入或退出容器形变目标路由时，**强制抑制主界面的页面级水平平移**（`slideOutHorizontally` / `popEnterTransition`），改为原地平滑淡入淡出（`fadeOut(FastOutSlowInEasing)`），让主容器保持物理原位静止，完全把运动舞台交给共享卡片。 | `NemoNavHost.kt`, `NavDestination.kt` | **协同转场与单一运动源公理**：当存在共享容器形变时，NavHost 层级必须让渡位移控制权，禁止多重几何坐标系在同一物理区域发生位移撕裂。 |
| **粗暴链式 `.clip(shape)` 导致过渡期圆角失真断裂**：在 `sharedBounds` 外部直接链式调用 `.clip(shape)`，在转场插值过程中覆盖层（Overlay）无法动态计算圆角插值，导致点击瞬间卡片圆角直接被目标页面的直角强行裁切。 | 彻底废除外部链式硬裁切，在 `sharedBounds` 内部通过 `clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(animatedRadius))` 进行图层级动态裁剪；并通过 `animatedVisibilityScope.transition.animateDp` 深度绑定 `EnterExitState`，实现卡片端 `24.dp -> 0.dp -> 24.dp` 与屏幕端 `24.dp -> 0.dp -> 24.dp` 的毫秒级平滑曲率渐变。 | `SharedTransitionContext.kt`, `TestDashboardScreen.kt` | **图层动态裁切与曲率插值公理**：容器形变过程中的圆角过渡必须依赖共享图层 OverlayClip 与状态机动态驱动，杜绝静态外部 Clip。 |
| **固定时长 Tween 导致形变缺乏物理弹性质感**：使用普通的 `tween(300)` 线性或贝塞尔插值，无法模拟真实物理世界中实体卡片被手指点开时由内向外的惯性张力与阻尼吸收。 | 引入两套专业物理阻尼弹簧：**空间弹簧**（`spatialExpressiveSpring`，阻尼比 0.8，刚度 380f，提供微弱呼吸感与紧致张力）与**非空间弹簧**（`nonSpatialExpressiveSpring`，阻尼比 1.0，刚度 1600f，保证内容淡入淡出干净利落不拖泥带水）。 | `SharedTransitionContext.kt` | **物理动力学拟真原则**：界面空间几何形变必须遵循现实物理阻尼规律，非空间属性（如 Alpha）高频利落切换。 |
| **目标界面外层挂载混乱导致测量塌陷**：部分界面将容器形变修饰符挂在带有系统栏 Padding 的 `Scaffold` 上，导致展开首帧因 WindowInsets 延迟计算出现瞬间下跳或抖动。 | 将 `nemoScreenContainer` 统一锚定在全屏底层最外层的纯净 `Box` 或 `Surface` 上，确保几何测量与设备物理屏幕完全等高并消除首帧下陷。 | `FavoritesScreen.kt`, `MistakesScreen.kt`, `TestSettingsScreen.kt`, `AbilityWorkshopScreen.kt` | **容器承接锚点纯净公理**：屏幕级容器形变承接者必须作用于无副作用的根 Surface/Box，杜绝与动态 Insets 混合绑在复合脚手架上。 |
| **NavHost 作用域下强转 EnterExitState 导致必现 ClassCastException 闪退**：在 Navigation 的 `composable` 块中，`animatedVisibilityScope.transition` 的真实泛型为 `NavBackStackEntry`。若在修饰符中调用 `animateDp { enterExit: EnterExitState -> ... }`，Compose 运行时强转失败直接闪退。 | 移除不必要的 `animateDp` 状态驱动，将形变裁切纯净交由 `sharedBounds` 的 `clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(cornerRadius))` 负责，杜绝泛型强转与重组开销。 | `SharedTransitionContext.kt` | **底层动画状态中立原则**：在 NavHost 跨路由共享作用域内，严禁将导航状态机强转为普通视图可见性状态机。 |
| **转场期间 Backdrop 高频重采样导致严重掉帧卡顿**：目标全屏界面底层若挂载 Backdrop 模糊着色器，在卡片高频缩放形变展开的 300ms 内，GPU 每一帧重复计算全屏高斯模糊与透镜折射，帧率暴跌。 | 在子界面层引入 `rememberIsTransitionSettled()`，将模糊采样设为 `layerBackdrop(backdrop, enabled = isTransitionSettled)`，在转场运行的 300ms 内熔断 Backdrop 模糊，完全就位后瞬间唤醒。 | `SubScreenTransitionModifier.kt`, `FavoritesScreen.kt` 等 | **转场算力熔断公理**：在物理容器高频形变转场期间，必须熔断着色器的大开销环境采样，将全部 GPU 算力让渡给插值动画。 |

---

## 最佳实践标准范式 (Golden Paradigm)

### 1. 卡片源端 (Card Origin)
```kotlin
DashboardTile(
    modifier = Modifier
        .weight(1f)
        .nemoCardContainer(
            key = "container_mistakes",
            cornerRadius = 24.dp
        ),
    ...
)
```

### 2. 屏幕承接端 (Screen Destination)
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(backgroundColor)
        .nemoScreenContainer(
            key = "container_mistakes",
            cardCornerRadius = 24.dp
        )
) {
    // 页面实际内容
}
```

### 3. 导航转场协同 (NavHost Coordination)
```kotlin
val mainExitTransition: ... = {
    HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
    if (isContainerTransformTarget(targetRoute)) {
        fadeOut(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
    } else if (targetRoute !in mainScreenRoutes) {
        NemoNavigationAnimations.exitTransition()
    } else {
        BottomNavTransition.exitTransition()
    }
}
```
