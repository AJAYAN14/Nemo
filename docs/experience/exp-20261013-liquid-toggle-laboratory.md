# 经验总结：实验室与液态玻璃开关组件集成

## 1. 经验总览

| 坑是什么 | 怎么避 | 相关工具/文件 | 统一原则 |
| --- | --- | --- | --- |
| 外部多平台 Compose 库组件强依赖外部 Backdrop 上下文，直接引入易导致缺少容器环境时渲染异常或调用繁琐 | 移植时提供默认 Backdrop 兜底重载方法，同时无缝兼容外部传入与无参直接调用 | [LiquidToggle.kt](file:///e:/Android/Nemo/core/ui/src/main/java/com/jian/nemo/core/ui/component/liquid/LiquidToggle.kt) | 基础 UI 组件需具备独立可用性，避免强制耦合特定外层容器 |
| 实验性特性与全局配置混杂，导致界面与配置逻辑难以向未来扩展 | 界面命名采用通用的 `LaboratoryScreen` 而非特定功能名；配置项采用枚举配合自适应组件 `NemoAdaptiveToggle` 隔离具体实现 | [LaboratoryScreen.kt](file:///e:/Android/Nemo/feature/settings/src/main/java/com/jian/nemo/feature/settings/LaboratoryScreen.kt), [NemoAdaptiveToggle.kt](file:///e:/Android/Nemo/core/ui/src/main/java/com/jian/nemo/core/ui/component/common/NemoAdaptiveToggle.kt) | 实验室作为开放式孵化容器，单一特性通过适配器模式对业务透明生效 |
| 两个按钮互斥时，若采用“拦截关闭”会导致用户想关掉当前项时产生“卡死/无法操作”的糟糕体验；添加过多“已启用”标签容易干扰纯粹效果展示 | 采用跷跷板双向反转联动：关 1 自动开 2，关 2 自动开 1；保持列表项平等干净，不添加多余的生效样式指示 | [LaboratoryScreen.kt](file:///e:/Android/Nemo/feature/settings/src/main/java/com/jian/nemo/feature/settings/LaboratoryScreen.kt) | 互斥开关必须双向反转联动，界面专注呈现效果本身，不添加冗余生效标记 |
| 移植原版胶囊液态开关时，若仅由滑块处理手势，用户点击底轨空白处将无任何响应导致卡顿感 | 严格按 1:1 还原原版 64dp×28dp 底轨和 40dp×24dp 饱满胶囊滑块规范，并在外层轨道增加全区域透明点击响应 | [LiquidToggle.kt](file:///e:/Android/Nemo/core/ui/src/main/java/com/jian/nemo/core/ui/component/liquid/LiquidToggle.kt) | 复杂手势自定义开关必须确保底轨全区域可点击，兼顾阻尼拖拽与轻点切换 |
