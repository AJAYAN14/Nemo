# 【AI E - 系统设置与算法调优】专属重构任务工单

> **工单代号**：`AI_E`  
> **所属模块**：`:feature:settings`  
> **业务范围**：常规偏好设置、高级学习设置、FSRS 算法记忆参数卡片、关于页  
> **待修改源码文件数**：`5` 个  
> **待提取硬编码总条数**：`43` 处  
> **专属独立资源文件（防冲突）**：`feature/settings/src/main/res/values/strings_settings_basic.xml`  
> **当前模块 R 类引用**：`import com.jian.nemo.feature.settings.R`  
> **独立编译验证命令**：`./gradlew :feature:settings:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`feature/settings/src/main/res/values/strings_settings_basic.xml`。
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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AboutViewModel.kt`（共 2 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L51 | `OTHER` | `暂无云端更新日志` | `errorMessage = if (config == null) "暂无云端更新日志" else null` |
| L58 | `OTHER` | `获取更新日志失败` | `errorMessage = e.message ?: "获取更新日志失败"` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AdvancedLearningSettingsScreen.kt`（共 10 处硬编码）

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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/SettingsScreen.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L84 | `UI_STATE_OR_PROMPT` | `失败`<br>`异常` | `snackbarType = if (it.contains("失败") \|\| it.contains("异常")) NemoSnackbarType.ERROR e...` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/SettingsViewModel.kt`（共 14 处硬编码）

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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/MemoryAlgorithmComponents.kt`（共 16 处硬编码）

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

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/settings/src/main/res/values/strings_settings_basic.xml`。
- [ ] 成功执行 `./gradlew :feature:settings:compileDebugKotlin`，模块独立编译通过，0 错误。
