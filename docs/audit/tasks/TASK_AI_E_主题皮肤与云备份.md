# 【AI E - 主题皮肤与云备份】专属任务执行工单

> **工单代号**：`AI_E`  
> **负责模块**：`:feature:settings`  
> **负责子域**：外观与数据备份（主题配色切换、配色弹窗、TTS 设置、云备份历史与数据恢复弹窗）  
> **待修改文件数**：`6` 个  
> **待提取硬编码总数**：`66` 处  
> **推荐追加的专属资源文件**：`feature/settings/src/main/res/values/strings_settings_theme_backup.xml`  
> **当前模块 R 引用**：`import com.jian.nemo.feature.settings.R`  
> **独立编译验证命令**：`./gradlew :feature:settings:compileDebugKotlin`  

---

## 一、执行硬约束纪律（执行前必读）

1. **严禁越界修改**：你只被授权修改本工单下方【第二章】所明确列出的文件。**严禁修改任何未被点名的文件**。
2. **独立资源文件隔离（防冲突关键）**：
   - 请将所有提取出的 `<string name="...">文案</string>` 写入/追加到专属文件：`feature/settings/src/main/res/values/strings_settings_theme_backup.xml`。
   - 如果该文件不存在，可直接新建；如果已存在则追加在 `<resources>` 标签内。
   - **严禁与其他 AI 共用或改动同一个 strings 文件，以杜绝 Git 合并冲突**！
3. **R 引用闭环**：在 Kotlin 源码中，统一导入 `com.jian.nemo.feature.settings.R`，通过 `stringResource(com.jian.nemo.feature.settings.R.string.xxx)` 或 `context.getString(...)` 取用，严禁跨模块引用其他 feature 模块未编译生成的 R 资源。
4. **命名规范**：遵循 `[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **独立编译验证**：修改完成后，在项目终端运行：
   ```bash
   ./gradlew :feature:settings:compileDebugKotlin
   ```
   必须确保无任何编译与语法报错。严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/CloudBackupHistoryScreen.kt`（共 3 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L81 | `OTHER` | `失败`<br>`异常` | `val isSuccess = !it.contains("失败") && !it.contains("异常")` |
| L82 | `OTHER` | `导入成功` | `val isRestoreSuccess = isSuccess && it.contains("导入成功")` |
| L494 | `DATE_FORMAT` | `yyyy年MM月dd日 HH:mm:ss` | `val formatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/ThemeSettingsScreen.kt`（共 12 处）

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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/TtsSettingsScreen.kt`（共 3 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L214 | `OTHER` | `这是日语语音测试。`<br>`これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewTts("这是日语语音测试。".let { "これは音声テストです。" }))` |
| L286 | `OTHER` | `これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewTts("これは音声テストです。"))` |
| L289 | `OTHER` | `これは音声テストです。` | `viewModel.onEvent(SettingsEvent.PreviewVoice(voiceName, "これは音声テストです。"))` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/RestoreSuccessDialog.kt`（共 12 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L55 | `OTHER` | `单词:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)` | `val wordRegex = Regex("""单词:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)""")` |
| L63 | `OTHER` | `语法:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)` | `val grammarRegex = Regex("""语法:\s*更新(\d+)\s*/\s*新增(\d+)\s*/\s*跳过(\d+)""")` |
| L71 | `OTHER` | `校验提示:` | `if (message.contains("校验提示:")) {` |
| L72 | `OTHER` | `校验提示:` | `validationNote = message.substringAfter("校验提示:").trim()` |
| L168 | `OTHER` | `档案恢复成功` | `text = "档案恢复成功",` |
| L178 | `OTHER` | `云端学习档案已安全同步至本机` | `text = "云端学习档案已安全同步至本机",` |
| L200 | `OTHER` | `词库档案` | `text = "词库档案",` |
| L206 | `OTHER` | `更新: ${stats.wordUpdate}  /  新增: ${stats.wordInsert}  /  跳过: ${stats.wordSkip}` | `text = "更新: ${stats.wordUpdate}  /  新增: ${stats.wordInsert}  /  跳过: ${stats.wordSkip}...` |
| L220 | `OTHER` | `语法档案` | `text = "语法档案",` |
| L226 | `OTHER` | `更新: ${stats.grammarUpdate}  /  新增: ${stats.grammarInsert}  /  跳过: ${stats.grammarSkip}` | `text = "更新: ${stats.grammarUpdate}  /  新增: ${stats.grammarInsert}  /  跳过: ${stats.gra...` |
| L239 | `OTHER` | `校验说明: $note` | `text = "校验说明: $note",` |
| L263 | `OTHER` | `开启学习之旅`<br>`完成` | `text = if (isFromLogin) "开启学习之旅" else "完成",` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/SettingsComponents.kt`（共 20 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L340 | `OTHER` | `每日单词目标` | `text = "每日单词目标",` |
| L355 | `OTHER` | `$goal 个单词` | `text = "$goal 个单词",` |
| L408 | `OTHER` | `每日语法目标` | `text = "每日语法目标",` |
| L423 | `OTHER` | `$goal 条语法` | `text = "$goal 条语法",` |
| L520 | `OTHER` | `学习日重置时间` | `text = "学习日重置时间",` |
| L526 | `OTHER` | `每天此时间后开始新的学习日` | `text = "每天此时间后开始新的学习日",` |
| L567 | `OTHER` | `凌晨 0:00 (午夜)` | `0 -&gt; "凌晨 0:00 (午夜)"` |
| L568 | `OTHER` | `凌晨 2:00` | `2 -&gt; "凌晨 2:00"` |
| L569 | `OTHER` | `凌晨 4:00 (推荐)` | `4 -&gt; "凌晨 4:00 (推荐)"` |
| L570 | `OTHER` | `凌晨 5:00` | `5 -&gt; "凌晨 5:00"` |
| L571 | `OTHER` | `凌晨 6:00` | `6 -&gt; "凌晨 6:00"` |
| L626 | `OTHER` | `云端语音`<br>`本地语音` | `if (voice.name.lowercase().contains("network")) "云端语音" else "本地语音"` |
| L658 | `OTHER` | `选择语音` | `text = "选择语音",` |
| L687 | `OTHER` | `未找到可用语音` | `text = "未找到可用语音",` |
| L841 | `OTHER` | `云端`<br>`本地` | `val tag = if (isNetwork) "云端" else "本地"` |
| L851 | `OTHER` | `系统默认` | `rawName.contains("language") -&gt; "系统默认"` |
| L852 | `OTHER` | `日语语音 $coreId` | `coreId.isNotEmpty() -&gt; "日语语音 $coreId"` |
| L853 | `OTHER` | `语音 ${coreId}` | `else -&gt; "语音 ${coreId}"` |
| L858 | `COMPOSE_UI` | `高质量` | `subtitle = if (voice.quality == "high" \|\| voice.quality == "very_high") "高质量" else ...` |
| L964 | `COMPOSE_UI` | `试听` | `contentDescription = "试听",` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/ThemeColorDialog.kt`（共 16 处）

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

## 三、完成验收检查单

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/settings/src/main/res/values/strings_settings_theme_backup.xml`。
- [ ] 成功执行 `./gradlew :feature:settings:compileDebugKotlin`，编译通过，0 Error。
