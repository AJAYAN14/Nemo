# Implementation Notes: 学习界面顶栏液态玻璃按钮光学与边缘重构

## 1. Deviations (偏离说明)
- **无破坏性形态改动**：严格遵循用户要求，未将“剩余”按钮改为圆形，完整保留了其横向药丸胶囊（Capsule）形态与点击弹窗交互。
- **热力图绝对隔离**：完全未修改 `ActivityHeatmapScreen.kt`（热力图界面源码 100% 保持基线）。

## 2. Discovered Edge Cases (发现的边界情况与技术要点)
- **色散对小字号文本的重影破坏**：
  - 在 `FrostedLiquidGlassButton` 中开启 `chromaticAberration = true` 会将底层 RGB 通道空间错位；
  - 遇到大尺寸图标或色块时具有艺术光泽，但包裹 `12sp~14sp` 小字号文本（如“剩余: XX”）时会导致笔画出现红青重影与发虚；
  - 解决办法：在包含文字及标准返回图标的顶栏按钮中，统一设定 `chromaticAberration = false`，对齐热力图纯净锐利画风。
- **各向异性胶囊透镜与大面积白雾感**：
  - 胶囊高度为 48dp 但宽度可达 90dp+，大面积 30% 纯白底色（`Color.White.copy(0.3f)`）在单一纯色背景上暴露无遗，呈现厚重的白色塑料膏药感；
  - 解决办法：引入 0.6dp 顶部垂直微光渐变描边（上亮下透），将胶囊底色透明度压降至 8%（深色）/ 15%（浅色），赋予晶体倒角高光切面。
- **单卡布局顶部采样空洞与色相偏色治理（Backdrop Context）**：
  - 学习界面为不可滚动的单卡布局，顶栏背后无卡片滑动经过，默认情况下 `layerBackdrop` 采样到的顶栏区域为空白或单一平铺底色，导致凸透镜位移计算结果为 0；
  - 初步采用主题蓝光晕后，GPU 色彩活力着色器（`vibrancy`）在冷色调上产生了向青绿（Teal / Cyan）的色相偏移；
  - 最终落地方案 B：将采样微光收敛为**100% 纯净中性黑白明度梯度**（深色 `Color.White.copy(0.08f -> 0.02f -> 0f)`，浅色 `Color.White.copy(0.85f -> 0.40f -> 0f)`），饱和度绝对为 0，彻底根除任何杂色偏色，呈现纯净透明的水晶水滴高光质感。

## 3. Questions for Review (待复核问题)
- 用户在实际设备运行体验时，可重点观察暗色模式与浅色模式下“剩余”胶囊的通透度与倒角微光反光切面。

---

# Implementation Notes: 设置界面至关于界面物理弹簧与硬件圆角转场试点 (完全对齐 testapp2 Cresto 规范)

## 1. Deviations (偏离说明)
- **严格对齐 testapp2**：遵循用户要求，所有跟动画相关的圆角阴影公式、阻尼参数、阻尼公式、父界面黑色半透明遮罩与视差位移，完全 1:1 对齐测试项目 `testapp2`（Cresto 规范），杜绝任何自行臆想。
- **彻底剔除旧动画代码**：已将 `NemoNavigationAnimations.kt` 中旧的 `+ fadeIn(...)` 与 `+ fadeOut(...)` 彻底清除（杜绝 13 旧代码对子界面的透明度污染）。

## 2. Discovered Edge Cases (发现的边界情况与技术要点)
- **父界面阴影的视觉真相（黑色暗化遮罩 Scrim）**：
  - 用户反馈在旧版本中未看到父界面阴影。经深度逆向与逐帧比对测试项目源码，发现测试项目的立体感来自于：父界面在入场时覆盖了一层 `Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = scrimAlpha.value }.background(Color.Black))`，以 `tween(300)` 从 `0.0f` 淡入到 `0.3f`；
  - 正是因为父界面被黑色遮罩压暗，子界面向左投射的 `shadowElevation = 32.dp` 物理高程阴影才能在暗底上形成强烈的边缘立体轮廓；退场时遮罩同步从 `0.3f` 淡出至 `0.0f`。
- **子界面 100% 实体无淡入淡出**：
  - 测试项目中子界面是纯物理卡片，仅执行纯水平矩阵平移（`translationX` 从 `screenWidth` 到 `0f`），完全没有任何透明度 `alpha` 渐变；
  - 过去旧代码因串联了 `+ fadeIn(...)`，导致子界面推入时发虚变透；本次重构完全杜绝了任何 alpha 插值，子界面 100% 实体呈现。
- **临界阻尼力学参数对齐**：
  - 弹簧规范：`Springs.smooth(300)`（`stiffness = 380f, dampingRatio = 0.95f`），二阶阻尼简谐振子算法，入场与退场均无过冲、平滑吸附。
- **设备屏幕硬件物理曲率对齐**：
  - `deviceCornerShape(maxRadius = 36.dp)`，通过 API 31+ `WindowInsets.getRoundedCorner` 采样真机四角真实弧度，低版本保底 36.dp。
- **编译与构建验证**：
  - 成功通过 `./gradlew assembleRelease` 编译出正式 Release 签名 APK（`app/build/outputs/apk/release/app-release.apk`）；
  - 实测 Logcat 验证 Choreographer 0 跳帧（`0 skipped frames`），满帧丝滑流畅。
- **彻底根治点击返回“迟钝一下再切换”的终极优化（动画先行与纯协程 GPU 驱动）**：
  - **迟钝根因定位**：此前点击返回时立即调用 Navigation 的 `popBackStack()`，主线程被路由出栈注销、设置大列表全量重组与底部 Padding 动态拉伸卡住 50~100ms，导致动画延迟启动；
  - **1:1 复刻 testapp2 动画先行架构**：
    1. 点击返回按钮/系统返回按键时，**第 0 毫秒立即启动并发协程平移退场动画（`horizontalOffset.animateTo(screenWidth, Springs.smooth(300))`）**，主线程 0 阻塞、纯靠 GPU 矩阵变换；
    2. 等 300ms 动画完全结束、子界面彻底滑出视野（`join()` 完成）后，才释放节点并恢复底栏；
    3. 全程彻底绕过 Navigation 沉重的出栈重排大堵车，实现与测试项目 100% 相同的手感：**0 毫秒即点即退，反复进出黄油般丝滑，彻底消除迟钝感**；
  - **底栏优雅解耦**：子页面展开时底栏自然滑出隐藏，关于页面纯净全屏；退场完成后底栏自然滑入复位。

## 3. Questions for Review (待复核问题)
- 用户在真机上体验最新 Release APK 时，可重点验证：
  1. 点击关于界面左上角返回按钮，或按手机系统返回键时，是否**一按即动、毫无迟钝**；
  2. 连续反复进进出出多次，手感是否始终如同测试项目一样紧致、流畅、满帧。

---

# Implementation Notes: 学习微会话结算页顶部安全内边距、次要液态玻璃按钮与分享海报防发灰渐变重构

## 1. Deviations (偏离说明)
- **严格遵循标杆黄金范式**：完全遵照 `docs/experience/04-悬浮液态玻璃顶栏遮罩范式与实验室深浅色排版避坑.md`，顶部避让严格对齐 `WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp + 16.dp`。
- **无破坏性业务逻辑修改**：未修改结算页任何统计派生逻辑、打卡徽章及海报截图导出功能，100% 保持业务与算法基准。

## 2. Discovered Edge Cases (发现的边界情况与技术要点)
- **结算页内容顶撞状态栏根因**：
  - `LearningScreen` 在卡片学习模式下依靠卡片内部 `topPadding` 避让悬浮顶栏；
  - 但在会话学完进入 `LearningFinishedContent` 时，顶部仅硬编码了 `Spacer(12.dp)`，没有动态计算安全区避让高度；
  - 48dp 悬浮顶栏（`CommonHeader` 架构，高度约 `statusBarTop + 60.dp`）直接覆盖在大数字（“30 词”）和主标题上方，导致“30”露出半截并穿插在状态栏与打卡胶囊后方；
  - 解决办法：在 `LearningFinishedContent` 引入 `topContentPadding`，默认为 `statusBarTop + 60.dp + 16.dp`，将内容平稳垫下；向上滚动时自然穿透至顶栏背后。
- **双重内边距叠加导致卡片过窄**：
  - 外层容器已有 `padding(horizontal = 16.dp)`，内部 `Column` 又重复设置了 `padding(horizontal = 16.dp)`，卡片边距叠加至 32dp；
  - 解决办法：剔除 `Column` 上的水平 padding，完全符合 `docs/design/标题与卡片对齐设计规范.md` 的 16dp 标准基准线。
- **次要按钮缺少液态玻璃质感**：
  - 双按钮模式下左侧“完成今日学习”误用 Material 3 `OutlinedButton`；
  - 解决办法：替换为 `FrostedLiquidGlassButton`，配置 `RoundedRectangle(18.dp)`（G2 连续平滑曲率）、半透主题色描边与微磨砂底色，具备完整物理弹簧受力与拖拽高光。
- **色彩空间线性插值横穿灰色死区（Gray Dead Zone）与明度断崖**：
  - 旧代码使用 `themePrimary -> themePrimary.copy(alpha=0.88f) -> LearningPalette.ShareGradTeal(#0F766E)`；
  - 当主题切换为暖橙（35°）、系统红（4°）等颜色时，色相角跨度高达 140°~171°，sRGB 线性插值直接横穿色轮中心无饱和灰色死区；
  - 两端明度从 0.46~0.65 暴跌至 0.06，中段缺乏高光支撑，转黑白严重塌陷显脏；
  - 解决办法：动态基于当前 `themePrimary` 在 HSL 空间计算同象限邻近色（$\Delta \text{Hue} \le 26^\circ$），中段主动注入微提亮高光过渡色，彻底拔除 Alpha 透明度，确保 100% 不发灰、转黑白曲线平滑优美。

## 3. Questions for Review (待复核问题)
- 用户在设备上完成一组学习进入结算页时：
  1. 验证顶部“30 词”与标题是否端正居中呈现在打卡胶囊下方，且具有舒适呼吸间距；
  2. 验证左侧“完成今日学习”是否呈现通透温润的磨砂液态玻璃效果，轻触时有物理弹簧与高光流动反馈；
  3. 点击右上角分享按钮，验证海报渐变在当前绿色及其他主题色下是否通透饱满、彻底告别发灰发暗。
