# 02-自适应应用图标扩展与SVG转换避坑

> 记录外部 SVG 转换为 Android 原生 VectorDrawable 矢量图、自适应桌面图标（Adaptive Icon）多别名配置及动态切换时的避坑实践。

---

## 核心复盘与避坑对照

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **外部 SVG 复杂标签兼容性**：SVG 中常包含带有旋转变换与圆角的 `<rect>`、`<circle>` 标签，Android 原生 `VectorDrawable` 不支持部分 SVG 特性或会导致构建解析报错。 | 提取几何变换矩阵将 `<rect>` 与 `<circle>` 转换为标准的绝对坐标 `android:pathData`，底层平铺纯色路径替代挖空路径。 | `convert_svg.py`, `core/designsystem/src/main/res/drawable/` | **纯净矢量原则**：Android 矢量资源统一收敛为标准 `pathData`，杜绝依赖平台不支持的标签与动态变换。 |
| **自适应图标主体过大溢出安全区**：原始插画占满画布（>80%），Android 系统自适应图标仅展示中心 66dp（约 60%）区域，导致桌面 Launcher 裁切时主体严重溢出变形。 | 背景平铺铺底保持 100% 覆盖；主体放入 `<group>` 等比收敛至 0.62~0.64 并精准居中微调，使主体严格处于 66dp 安全圈内，四周保留舒适呼吸区（对齐微信、抖音规范）。 | `ic_launcher_frog_cheer_foreground.xml`, `ic_launcher_frog_ninja_foreground.xml` | **安全视口原则**：自适应图标前景主体必须控制在中心 66dp 安全区域内，绝不可全图撑满。 |
| **UI 预览写死放大导致二次贴边**：设置界面为了兼容历史小图标而硬编码 `graphicsLayer(scale = 1.5f)`，导致标准自适应图标在卡片中被二次放大贴边。 | 预览组件解耦 `imageScale` 参数，历史无底色小图标维持 `1.5f`，标准自适应图标采用 `1.0f` 真实 1:1 还原桌面实际视觉。 | `ThemeSettingsScreen.kt` | **真实还原原则**：桌面图标设置项预览必须忠实呈现桌面上的真实呈现比例。 |
| **模块间资源依赖隔离断层**：Compose 设置界面在 `feature:settings` 依赖 `core:designsystem`，而清单与启动别名在 `app` 模块，单边放置会导致资源找不到。 | 将前景矢量图同步放置在 `core:designsystem` 与 `app` 的 `drawable` 目录，兼顾 UI 模块的 R 资源引用与启动别名声明。 | `feature/settings`, `core/designsystem`, `app/src/main/AndroidManifest.xml` | **就近与共享原则**：公共设计资源入库 `core:designsystem`，主包运行时资源入库 `app`。 |
| **桌面别名切换打散图标排版**：切换桌面图标时若先禁用旧组件后启用新组件，部分手机桌面会判定应用已卸载，导致桌面文件夹与排列位置重置。 | 采用同步紧凑原子操作：先启用目标组件，再迭代禁用其他所有别名组件，手机桌面即判定为“原地就地替换”。 | `feature/settings/src/main/java/.../AppIconManager.kt` | **原子就地替换原则**：清单别名切换始终“先起后禁”，保障用户桌面布局与快捷方式稳定。 |

---

## 涉及文件清单
- `core/designsystem/src/main/res/drawable/ic_launcher_frog_cheer_foreground.xml`
- `core/designsystem/src/main/res/drawable/ic_launcher_frog_ninja_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_frog_cheer_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_frog_ninja_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_frog_cheer.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_frog_ninja.xml`
- `app/src/main/AndroidManifest.xml`
- `core/domain/src/main/java/com/jian/nemo/core/domain/repository/SettingsRepository.kt`
- `feature/settings/src/main/java/com/jian/nemo/feature/settings/util/AppIconManager.kt`
- `feature/settings/src/main/java/com/jian/nemo/feature/settings/ThemeSettingsScreen.kt`
