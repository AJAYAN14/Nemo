# 【AI G - 云备份与积压控制】专属重构任务工单

> **工单代号**：`AI_G`  
> **所属模块**：`:feature:settings`  
> **业务范围**：云端备份历史、备份数据恢复成功弹窗、积压复习任务控制器卡片、设置通用组件  
> **待修改源码文件数**：`4` 个  
> **待提取硬编码总条数**：`40` 处  
> **专属独立资源文件（防冲突）**：`feature/settings/src/main/res/values/strings_settings_backup.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.settings.R`  
> **独立编译验证命令**：`./gradlew :feature:settings:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/settings/src/main/res/values/strings_settings_backup.xml`。
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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/CloudBackupHistoryScreen.kt`（共 3 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L81 | `OTHER` | `失败`<br>`异常` | `val isSuccess = !it.contains("失败") && !it.contains("异常")` |
| L82 | `OTHER` | `导入成功` | `val isRestoreSuccess = isSuccess && it.contains("导入成功")` |
| L494 | `DATE_FORMAT` | `yyyy年MM月dd日 HH:mm:ss` | `val formatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/BacklogControllerCard.kt`（共 5 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L165 | `OTHER` | `自定义` | `text = if (isCustomBatch) "$sessionBatchSize" else "自定义",` |
| L203 | `OTHER` | `不限`<br>`40 词`<br>`60 词`<br>`80 词` | `val reviewLimitOptions = listOf(0 to "不限", 40 to "40 词", 60 to "60 词", 80 to "80 词")` |
| L265 | `OTHER` | `$maxDailyReviews 词`<br>`自定义` | `text = if (isCustomReviewLimit) "$maxDailyReviews 词" else "自定义",` |
| L330 | `OTHER` | `$it 词` | `presetLabelFormatter = { "$it 词" },` |
| L353 | `OTHER` | `不限`<br>`$it 词` | `presetLabelFormatter = { if (it == 0) "不限" else "$it 词" },` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/RestoreSuccessDialog.kt`（共 12 处硬编码）

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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/SettingsComponents.kt`（共 20 处硬编码）

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

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/settings/src/main/res/values/strings_settings_backup.xml`。
- [ ] 成功执行 `./gradlew :feature:settings:compileDebugKotlin`，模块独立编译通过，0 错误。
