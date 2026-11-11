# Compose 嵌套动画可见性与 ViewModel 跨路由时序避坑指南

> 本文档总结 Nemo 在 Compose 动画嵌套与 ViewModel 跨路由导航时遇到的首帧时序差与动效误触发问题，沉淀开发避坑准则。

---

## 避坑对照表

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **嵌套动画子项独立触发导致误展开闪烁**：外层 `AnimatedVisibility` 正在退出收起时，内层 `AnimatedVisibility` 仅判断自身字段导致在数据更新瞬间判定为可见，触发展开进入动画，在屏幕上出现“冒出显示然后随外层收缩消失”的视觉 Bug。 | **父子可见性全链路收敛原则**：所有嵌套在父级折叠容器中的子动画控件，其 `visible` 判定必须无条件以父容器的显示状态（如 `isAuto`）作为前置布尔与运算条件（如 `isAuto && strategy == SCHEDULED`），杜绝子项在外层关闭退出时独立触发展开。 | `ThemeSettingsScreen.kt`, `AnimatedVisibility` | **复合状态约束公理**：从属子视图的进入动画绝不得脱离父视图的生命周期状态独立判定。 |
| **ViewModel 跨路由重建与首帧异步时序差**：Navigation 二级子页面默认新建独立的 ViewModel，首帧渲染采用数据类默认初始值（如 `AUTO`），数十毫秒后 DataStore 异步读取完成变为真实数据（非 `AUTO`），导致界面在首帧和次帧之间发生状态跳变并触发收起动画。 | **父级 ViewModel 安全共享与热状态直达原则**：对于由父级页面（如设置大厅）导航进入的专属设置子界面（如主题外观），优先复用父级路由已在内存中就绪的 ViewModel 实例；若独立进入则降级使用独立实例，彻底消除初次进入时的冷启动数据延迟。 | `NemoNavHost.kt`, `SettingsViewModel.kt`, `hiltViewModel(parentEntry)` | **单一数据源热缓存公理**：同一设置上下文的连续导航必须复用已就绪的内存数据流，严禁让子路由重复经历冷流加载导致的初态跳动。 |

---

## 核心实现范式

### 1. 嵌套动画可见性守卫

```kotlin
// ❌ 错误示范：内层未绑定外层条件，外层退出时内层因数据更新独立触发展开
AnimatedVisibility(
    visible = uiState.darkModeStrategy == DarkModeStrategy.SCHEDULED
)

// ✅ 正确范式：内层复合绑定父状态，父状态为 false 时自始至终绝对不展开
AnimatedVisibility(
    visible = isAuto && uiState.darkModeStrategy == DarkModeStrategy.SCHEDULED,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically()
)
```

### 2. Navigation 子路由安全共享已就绪 ViewModel

```kotlin
composable(NavDestination.THEME_SETTINGS) { backStackEntry ->
    val parentEntry = remember(backStackEntry) {
        try {
            navController.getBackStackEntry(NavDestination.SETTINGS)
        } catch (_: Exception) {
            null
        }
    }
    val viewModel: SettingsViewModel = if (parentEntry != null) {
        hiltViewModel(parentEntry)
    } else {
        hiltViewModel()
    }
    SubScreenContainer {
        ThemeSettingsScreen(
            onBack = { navController.popBackStack() },
            viewModel = viewModel
        )
    }
}
```
