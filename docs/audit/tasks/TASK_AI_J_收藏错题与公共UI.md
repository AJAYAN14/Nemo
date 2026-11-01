# 【AI J - 收藏错题与公共 UI】专属任务执行工单

> **工单代号**：`AI_J`  
> **负责模块**：`:feature:collection & :core:ui`  
> **负责子域**：错题收藏与公共组件（收藏夹、错题生词本、头像裁剪编辑弹窗、浮动词汇背景动效）  
> **待修改文件数**：`7` 个  
> **待提取硬编码总数**：`18` 处  
> **推荐追加的专属资源文件**：`对应模块自身的 res/values/strings_xxx.xml`  
> **当前模块 R 引用**：`import 对应模块专属 R`  
> **独立编译验证命令**：`./gradlew :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin`  

---

## 一、执行硬约束纪律（执行前必读）

1. **严禁越界修改**：你只被授权修改本工单下方【第二章】所明确列出的文件。**严禁修改任何未被点名的文件**。
2. **独立资源文件隔离（防冲突关键）**：
   - 请将所有提取出的 `<string name="...">文案</string>` 写入/追加到专属文件：`对应模块自身的 res/values/strings_xxx.xml`。
   - 如果该文件不存在，可直接新建；如果已存在则追加在 `<resources>` 标签内。
   - **严禁与其他 AI 共用或改动同一个 strings 文件，以杜绝 Git 合并冲突**！
3. **R 引用闭环**：在 Kotlin 源码中，统一导入 `对应模块专属 R`，通过 `stringResource(对应模块专属 R.string.xxx)` 或 `context.getString(...)` 取用，严禁跨模块引用其他 feature 模块未编译生成的 R 资源。
4. **命名规范**：遵循 `[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **独立编译验证**：修改完成后，在项目终端运行：
   ```bash
   ./gradlew :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin
   ```
   必须确保无任何编译与语法报错。严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `core/ui/src/main/java/com/jian/nemo/core/ui/component/animation/AnimatedWordBackground.kt`（共 3 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L43 | `OTHER` | `桜`<br>`夢`<br>`愛`<br>`花見` | `"こんにちは", "ありがとう", "日本語", "桜", "夢", "愛", "花見",` |
| L44 | `OTHER` | `友達`<br>`時間`<br>`赤い`<br>`緑`<br>`犬`<br>`知る`<br>`大きい` | `"友達", "時間", "赤い", "緑", "犬", "知る", "大きい",` |
| L45 | `OTHER` | `小さい`<br>`新しい`<br>`難しい`<br>`楽しい`<br>`静か`<br>`見る` | `"小さい", "新しい", "難しい", "楽しい", "静か", "見る"` |

### 📄 `core/ui/src/main/java/com/jian/nemo/core/ui/component/avatar/AvatarEditDialog.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L335 | `EXCEPTION_ERROR` | `文件保存后不存在` | `exception = Exception("文件保存后不存在")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoriteGrammarsViewModel.kt`（共 2 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L61 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L98 | `UI_STATE_OR_PROMPT` | `批量取消收藏失败: ${e.message}` | `it.copy(error = "批量取消收藏失败: ${e.message}")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoritesViewModel.kt`（共 3 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L74 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L103 | `UI_STATE_OR_PROMPT` | `取消收藏失败: ${result.exception.message}` | `it.copy(error = "取消收藏失败: ${result.exception.message}")` |
| L123 | `UI_STATE_OR_PROMPT` | `批量取消收藏失败: ${e.message}` | `it.copy(error = "批量取消收藏失败: ${e.message}")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/MistakesViewModel.kt`（共 5 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L82 | `UI_STATE_OR_PROMPT` | `加载单词错题失败: ${e.message}` | `error = "加载单词错题失败: ${e.message}"` |
| L135 | `UI_STATE_OR_PROMPT` | `删除失败: ${result.exception.message}` | `it.copy(error = "删除失败: ${result.exception.message}")` |
| L155 | `UI_STATE_OR_PROMPT` | `删除失败: ${result.exception.message}` | `it.copy(error = "删除失败: ${result.exception.message}")` |
| L175 | `UI_STATE_OR_PROMPT` | `清空失败: ${result.exception.message}` | `it.copy(error = "清空失败: ${result.exception.message}")` |
| L195 | `UI_STATE_OR_PROMPT` | `清空失败: ${result.exception.message}` | `it.copy(error = "清空失败: ${result.exception.message}")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/WrongGrammarsViewModel.kt`（共 2 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L68 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L106 | `UI_STATE_OR_PROMPT` | `删除失败: ${e.message}` | `error = "删除失败: ${e.message}"` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/WrongWordsViewModel.kt`（共 2 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L88 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L118 | `UI_STATE_OR_PROMPT` | `删除失败: ${e.message}` | `error = "删除失败: ${e.message}"` |

---

## 三、完成验收检查单

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `对应模块自身的 res/values/strings_xxx.xml`。
- [ ] 成功执行 `./gradlew :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin`，编译通过，0 Error。
