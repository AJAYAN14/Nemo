# 【AI D - 基础设置与学习算法】专属任务执行工单

> **工单代号**：`AI_D`  
> **负责模块**：`:feature:settings`  
> **负责子域**：基础设置（设置主页、高级学习偏好、FSRS 算法调优、积压控制器、关于页）  
> **待修改文件数**：`9` 个  
> **待提取硬编码总数**：`81` 处  
> **推荐追加的专属资源文件**：`feature/settings/src/main/res/values/strings_settings_core.xml`  
> **当前模块 R 引用**：`import com.jian.nemo.feature.settings.R`  
> **独立编译验证命令**：`./gradlew :feature:settings:compileDebugKotlin`  

---

## 一、执行硬约束纪律（执行前必读）

1. **严禁越界修改**：你只被授权修改本工单下方【第二章】所明确列出的文件。**严禁修改任何未被点名的文件**。
2. **独立资源文件隔离（防冲突关键）**：
   - 请将所有提取出的 `<string name="...">文案</string>` 写入/追加到专属文件：`feature/settings/src/main/res/values/strings_settings_core.xml`。
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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AboutViewModel.kt`（共 2 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L51 | `OTHER` | `暂无云端更新日志` | `errorMessage = if (config == null) "暂无云端更新日志" else null` |
| L58 | `OTHER` | `获取更新日志失败` | `errorMessage = e.message ?: "获取更新日志失败"` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AdvancedLearningSettingsScreen.kt`（共 10 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L226 | `COMPOSE_UI` | `新卡片初学阶梯` | `title = "新卡片初学阶梯",` |
| L227 | `COMPOSE_UI` | `新知识首次记忆时的短时巩固序列 (单位: 分钟)` | `subtitle = "新知识首次记忆时的短时巩固序列 (单位: 分钟)",` |
| L235 | `COMPOSE_UI` | `遗忘重学阶梯` | `title = "遗忘重学阶梯",` |
| L236 | `COMPOSE_UI` | `遗忘卡片重新激活时的强化复习序列 (单位: 分钟)` | `subtitle = "遗忘卡片重新激活时的强化复习序列 (单位: 分钟)",` |
| L288 | `COMPOSE_UI` | `记忆算法调校` | `title = "记忆算法调校",` |
| L315 | `COMPOSE_UI` | `恢复默认设置` | `contentDescription = "恢复默认设置",` |
| L340 | `COMPOSE_UI` | `恢复默认算法配置` | `title = "恢复默认算法配置",` |
| L341 | `OTHER` | `确认恢复` | `confirmText = "确认恢复",` |
| L342 | `OTHER` | `取消` | `dismissText = "取消",` |
| L362 | `OTHER` | `确定要将记忆算法配置恢复为系统黄金推荐值吗？` | `text = "确定要将记忆算法配置恢复为系统黄金推荐值吗？",` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AISettingsViewModel.kt`（共 18 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L88 | `OTHER` | `OpenAI 默认配置` | `"openai" -&gt; "OpenAI 默认配置"` |
| L89 | `OTHER` | `Gemini 默认配置` | `"gemini" -&gt; "Gemini 默认配置"` |
| L90 | `OTHER` | `DeepSeek 默认配置` | `"deepseek" -&gt; "DeepSeek 默认配置"` |
| L91 | `OTHER` | `自定义 默认配置` | `else -&gt; "自定义 默认配置"` |
| L165 | `OTHER` | `未命名配置` | `val finalName = editing.name.trim().ifEmpty { "未命名配置" }` |
| L173 | `OTHER` | `配置别名已存在，请换一个别名` | `_uiState.update { it.copy(testResult = AITestResult(false, "配置别名已存在，请换一个别名")) }` |
| L180 | `OTHER` | `API 密钥已配置过，请勿重复添加` | `_uiState.update { it.copy(testResult = AITestResult(false, "API 密钥已配置过，请勿重复添加")) }` |
| L210 | `OTHER` | `API Key 不能为空` | `_uiState.update { it.copy(testResult = AITestResult(false, "API Key 不能为空")) }` |
| L215 | `OTHER` | `未命名配置` | `val finalName = editing.name.trim().ifEmpty { "未命名配置" }` |
| L223 | `OTHER` | `配置别名已存在，请换一个别名` | `_uiState.update { it.copy(testResult = AITestResult(false, "配置别名已存在，请换一个别名")) }` |
| L230 | `OTHER` | `API 密钥已配置过，请勿重复添加` | `_uiState.update { it.copy(testResult = AITestResult(false, "API 密钥已配置过，请勿重复添加")) }` |
| L249 | `OTHER` | `连接成功` | `AITestResult(true, "连接成功")` |
| L251 | `OTHER` | `未知错误` | `val msg = result.exceptionOrNull()?.message ?: "未知错误"` |
| L262 | `OTHER` | `未命名配置` | `val finalName = editing.name.trim().ifEmpty { "未命名配置" }` |
| L281 | `OTHER` | `API Key 为空，无法测试` | `_uiState.update { it.copy(testResult = AITestResult(false, "API Key 为空，无法测试")) }` |
| L297 | `OTHER` | `「${config.name}」API 连接正常 · 测试通过` | `AITestResult(true, "「${config.name}」API 连接正常 · 测试通过")` |
| L299 | `OTHER` | `连接失败` | `val msg = result.exceptionOrNull()?.message ?: "连接失败"` |
| L300 | `OTHER` | `「${config.name}」测试失败: $msg` | `AITestResult(false, "「${config.name}」测试失败: $msg")` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/SettingsScreen.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L84 | `UI_STATE_OR_PROMPT` | `失败`<br>`异常` | `snackbarType = if (it.contains("失败") \|\| it.contains("异常")) NemoSnackbarType.ERROR e...` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/SettingsViewModel.kt`（共 14 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L308 | `OTHER` | `基础词库正在准备中，请稍候恢复` | `_uiState.update { it.copy(toastMessage = "基础词库正在准备中，请稍候恢复") }` |
| L353 | `OTHER` | `导入过程中发生异常: ${e.message}` | `_uiState.update { it.copy(toastMessage = "导入过程中发生异常: ${e.message}") }` |
| L366 | `OTHER` | `备份过于频繁，请稍后再试` | `_uiState.update { it.copy(toastMessage = "备份过于频繁，请稍后再试") }` |
| L375 | `OTHER` | `云端备份成功` | `_uiState.update { it.copy(toastMessage = "云端备份成功") }` |
| L378 | `OTHER` | `备份失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "备份失败: ${e.message}") }` |
| L395 | `OTHER` | `备份列表已刷新` | `toastMessage = "备份列表已刷新"` |
| L400 | `OTHER` | `获取备份列表失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "获取备份列表失败: ${e.message}") }` |
| L422 | `OTHER` | `预览失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "预览失败: ${e.message}", pendingRestoreFileName...` |
| L435 | `OTHER` | `基础词库正在准备中，请稍候恢复` | `_uiState.update { it.copy(toastMessage = "基础词库正在准备中，请稍候恢复") }` |
| L443 | `OTHER` | `失败`<br>`异常` | `val isSuccess = !message.contains("失败") && !message.contains("异常")` |
| L447 | `OTHER` | `导入成功` | `restoreSuccessMessage = if (isSuccess && message.contains("导入成功")) message else null` |
| L452 | `OTHER` | `恢复失败: ${e.message}` | `_uiState.update { it.copy(toastMessage = "恢复失败: ${e.message}", restoreSuccessMessage ...` |
| L539 | `OTHER` | `每日目标已更新，将在下一个学习日生效` | `toastMessage = "每日目标已更新，将在下一个学习日生效"` |
| L554 | `OTHER` | `每日语法目标已更新，将在下一个学习日生效` | `toastMessage = "每日语法目标已更新，将在下一个学习日生效"` |

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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/BacklogControllerCard.kt`（共 5 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L165 | `OTHER` | `自定义` | `text = if (isCustomBatch) "$sessionBatchSize" else "自定义",` |
| L203 | `OTHER` | `不限`<br>`40 词`<br>`60 词`<br>`80 词` | `val reviewLimitOptions = listOf(0 to "不限", 40 to "40 词", 60 to "60 词", 80 to "80 词")` |
| L265 | `OTHER` | `$maxDailyReviews 词`<br>`自定义` | `text = if (isCustomReviewLimit) "$maxDailyReviews 词" else "自定义",` |
| L330 | `OTHER` | `$it 词` | `presetLabelFormatter = { "$it 词" },` |
| L353 | `OTHER` | `不限`<br>`$it 词` | `presetLabelFormatter = { if (it == 0) "不限" else "$it 词" },` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/MemoryAlgorithmComponents.kt`（共 16 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L157 | `OTHER` | `清爽微负荷` | `targetRetention &lt; 0.82f -&gt; "清爽微负荷" to SettingsPalette.CyanMint` |
| L158 | `OTHER` | `黄金平衡态` | `targetRetention &lt;= 0.91f -&gt; "黄金平衡态" to IosColors.Purple` |
| L159 | `OTHER` | `极客高密度` | `else -&gt; "极客高密度" to IosColors.OrangeDark` |
| L565 | `OTHER` | `80% 宽松`<br>`85% 平衡`<br>`90% 黄金`<br>`95% 冲刺` | `listOf(0.80f to "80% 宽松", 0.85f to "85% 平衡", 0.90f to "90% 黄金", 0.95f to "95% 冲刺").fo...` |
| L667 | `COMPOSE_UI` | `添加阶段` | `contentDescription = "添加阶段",` |
| L673 | `OTHER` | `增加阶梯` | `text = "增加阶梯",` |
| L706 | `COMPOSE_UI` | `新增复习阶段` | `title = "新增复习阶段",` |
| L773 | `COMPOSE_UI` | `删除该阶段` | `contentDescription = "删除该阶段",` |
| L790 | `UI_STATE_OR_PROMPT` | `调整复习阶段时长` | `title: String = "调整复习阶段时长",` |
| L791 | `UI_STATE_OR_PROMPT` | `设定记忆间隔时长（支持分位加减与手写输入）` | `subtitle: String = "设定记忆间隔时长（支持分位加减与手写输入）",` |
| L809 | `OTHER` | `确认保存` | `confirmText = "确认保存",` |
| L820 | `OTHER` | `$step 分钟` | `val mins = step.toIntOrNull() ?: return "$step 分钟"` |
| L822 | `OTHER` | `$mins 分钟` | `mins &lt; 60 -&gt; "$mins 分钟"` |
| L823 | `OTHER` | `${mins / 60} 小时` | `mins &lt; 1440 -&gt; "${mins / 60} 小时"` |
| L824 | `OTHER` | `${mins / 1440} 天` | `else -&gt; "${mins / 1440} 天"` |
| L930 | `OTHER` | `$count 次` | `text = "$count 次",` |

---

## 三、完成验收检查单

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/settings/src/main/res/values/strings_settings_core.xml`。
- [ ] 成功执行 `./gradlew :feature:settings:compileDebugKotlin`，编译通过，0 Error。
