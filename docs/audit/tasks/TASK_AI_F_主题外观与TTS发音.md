# 【AI F - 主题外观与 TTS 发音】专属重构任务工单

> **工单代号**：`AI_F`  
> **所属模块**：`:feature:settings`  
> **业务范围**：主题模式切换、深浅配色选择弹窗、TTS 语音引擎配置  
> **待修改源码文件数**：`3` 个  
> **待提取硬编码总条数**：`31` 处  
> **专属独立资源文件（防冲突）**：`feature/settings/src/main/res/values/strings_settings_theme_tts.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.settings.R`  
> **独立编译验证命令**：`./gradlew :feature:settings:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/settings/src/main/res/values/strings_settings_theme_tts.xml`。
   - 若文件不存在可直接新建；若已存在则追加在根 `<resources>` 标签内。
   - **严禁向公共或其它 strings 文件写入，确保你的改动与其他 AI 0 冲突**！
3. **模块内 R 引用闭环**：Kotlin 源码中统一通过 `stringResource(com.jian.nemo.feature.settings.R.string.xxx)` 取用，**严禁跨模块引用外部 feature 模块的 R 资源**。
4. **规范 Key 命名**：`[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **严禁口水注释与黑盒脚本**：严禁在源码中留下“根据要求提取”等元注释；严禁使用黑盒正则脚本批处理，必须保持代码安全。
6. **单模块独立编译验证**：修改完成后，在终端直接运行：
   ```bash
   ./gradlew :feature:settings:compileDebugKotlin
   ```
   编译通过且无任何报错即代表本工单顺利交付！严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/ThemeSettingsScreen.kt`（共 12 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L786 | `COMPOSE_UI` | `跟随系统` | `title = "跟随系统",` |
| L787 | `COMPOSE_UI` | `根据系统设置自动切换深浅色主题` | `subtitle = "根据系统设置自动切换深浅色主题",` |
| L792 | `COMPOSE_UI` | `定时深色模式` | `title = "定时深色模式",` |
| L793 | `COMPOSE_UI` | `在指定时间段开启深色模式` | `subtitle = "在指定时间段开启深色模式",` |
| L808 | `COMPOSE_UI` | `跟随系统` | `title = "跟随系统",` |
| L809 | `COMPOSE_UI` | `根据系统设置自动切换深浅色主题` | `subtitle = "根据系统设置自动切换深浅色主题",` |
| L814 | `COMPOSE_UI` | `定时深色模式` | `title = "定时深色模式",` |
| L815 | `COMPOSE_UI` | `在指定时间段开启深色模式` | `subtitle = "在指定时间段开启深色模式",` |
| L831 | `OTHER` | `经典圆角彩色背景底框，单色聚焦图标` | `description = "经典圆角彩色背景底框，单色聚焦图标",` |
| L838 | `OTHER` | `现代无背景通透设计，双色线面与等亮度调色板` | `description = "现代无背景通透设计，双色线面与等亮度调色板",` |
| L854 | `OTHER` | `经典圆角彩色背景底框，单色聚焦图标` | `description = "经典圆角彩色背景底框，单色聚焦图标",` |
| L861 | `OTHER` | `现代无背景通透设计，双色线面与等亮度调色板` | `description = "现代无背景通透设计，双色线面与等亮度调色板",` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/TtsSettingsScreen.kt`（共 3 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L214 | `OTHER` | `这是日语语音测试。`<br>`これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewTts("这是日语语音测试。".let { "これは音声テストです。" }))` |
| L286 | `OTHER` | `これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewTts("これは音声テストです。"))` |
| L289 | `OTHER` | `これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewVoice(voiceName, "これは音声テストです。"))` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/ThemeColorDialog.kt`（共 16 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L103 | `OTHER` | `默认` | `ThemeColor("默认", UserPalette.PrimaryGlassTintLight),` |
| L104 | `OTHER` | `蔷薇红` | `ThemeColor("蔷薇红", IosColors.Pink),` |
| L105 | `OTHER` | `活力橙` | `ThemeColor("活力橙", IosColors.Orange),` |
| L106 | `OTHER` | `明快黄` | `ThemeColor("明快黄", IosColors.Yellow),` |
| L107 | `OTHER` | `薄荷绿` | `ThemeColor("薄荷绿", SettingsPalette.ThemeAppleGreen),` |
| L108 | `OTHER` | `天空蓝` | `ThemeColor("天空蓝", IosColors.Teal),` |
| L109 | `OTHER` | `经典蓝` | `ThemeColor("经典蓝", IosColors.Blue),` |
| L110 | `OTHER` | `薰衣草紫` | `ThemeColor("薰衣草紫", IosColors.Indigo),` |
| L111 | `OTHER` | `罗兰紫` | `ThemeColor("罗兰紫", IosColors.Purple),` |
| L112 | `OTHER` | `荧光青` | `ThemeColor("荧光青", SettingsPalette.ThemeElectricCyan),` |
| L113 | `OTHER` | `亮青绿` | `ThemeColor("亮青绿", SettingsPalette.ThemeBrightTeal),` |
| L114 | `OTHER` | `炽热红` | `ThemeColor("炽热红", SettingsPalette.ThemeBrightOrangeRed)` |
| L130 | `COMPOSE_UI` | `环形配色选择器` | `title = "环形配色选择器",` |
| L131 | `OTHER` | `确定` | `confirmText = "确定",` |
| L132 | `OTHER` | `取消` | `dismissText = "取消",` |
| L209 | `OTHER` | `当前选择：$activeColorName` | `text = "当前选择：$activeColorName",` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/settings/src/main/res/values/strings_settings_theme_tts.xml`。
- [ ] 成功执行 `./gradlew :feature:settings:compileDebugKotlin`，模块独立编译通过，0 错误。
