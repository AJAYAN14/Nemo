# 【AI L - 收藏错题与公共 UI 动效】专属重构任务工单

> **工单代号**：`AI_L`  
> **所属模块**：`:feature:collection & :core:ui`  
> **业务范围**：生词本与语法收藏夹、错题本回顾、头像裁剪编辑弹窗、浮动文字背景动效  
> **待修改源码文件数**：`7` 个  
> **待提取硬编码总条数**：`18` 处  
> **专属独立资源文件（防冲突）**：`各模块自身的 res/values/strings_xxx.xml`  
> **当前模块 R 类引用**：`import 对应模块专属 R`  
> **独立编译验证命令**：`./gradlew :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin`  

---

## 一、多 AI 并行防报错铁律（必须严格服从）

1. **严禁越界修改源码**：你被严格授权**仅能修改下方【第二章】清单中明确指名的文件**。任何未列出的文件一律禁止改动。
2. **专属资源文件独占（防合并冲突关键）**：
   - 本工单所有提取的 `<string name="...">` 必须全部写入或追加至你的专属文件：`各模块自身的 res/values/strings_xxx.xml`。
   - 若文件不存在可直接新建；若已存在则追加在根 `<resources>` 标签内。
   - **严禁向公共或其它 strings 文件写入，确保你的改动与其他 AI 0 冲突**！
3. **模块内 R 引用闭环**：Kotlin 源码中统一通过 `stringResource(对应模块专属 R.string.xxx)` 取用，**严禁跨模块引用外部 feature 模块的 R 资源**。
4. **规范 Key 命名**：`[模块]_[页面/组件]_[类型]_[描述]`，类型前缀统一为 `title_`, `label_`, `btn_`, `dialog_`, `format_`, `empty_`, `toast_`。
5. **严禁口水注释与黑盒脚本**：严禁在源码中留下“根据要求提取”等元注释；严禁使用黑盒正则脚本批处理，必须保持代码安全。
6. **单模块独立编译验证**：修改完成后，在终端直接运行：
   ```bash
   ./gradlew :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin
   ```
   编译通过且无任何报错即代表本工单顺利交付！严禁执行 `adb uninstall`，严禁安装 Debug APK。

---

## 二、待修改源码文件与逐行排查清单

### 📄 `core/ui/src/main/java/com/jian/nemo/core/ui/component/animation/AnimatedWordBackground.kt`（共 3 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L43 | `OTHER` | `桜`<br>`夢`<br>`愛`<br>`花見` | `"こんにちは", "ありがとう", "日本語", "桜", "夢", "愛", "花見",` |
| L44 | `OTHER` | `友達`<br>`時間`<br>`赤い`<br>`緑`<br>`犬`<br>`知る`<br>`大きい` | `"友達", "時間", "赤い", "緑", "犬", "知る", "大きい",` |
| L45 | `OTHER` | `小さい`<br>`新しい`<br>`難しい`<br>`楽しい`<br>`静か`<br>`見る` | `"小さい", "新しい", "難しい", "楽しい", "静か", "見る"` |

### 📄 `core/ui/src/main/java/com/jian/nemo/core/ui/component/avatar/AvatarEditDialog.kt`（共 1 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L335 | `EXCEPTION_ERROR` | `文件保存后不存在` | `exception = Exception("文件保存后不存在")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoriteGrammarsViewModel.kt`（共 2 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L61 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L98 | `UI_STATE_OR_PROMPT` | `批量取消收藏失败: ${e.message}` | `it.copy(error = "批量取消收藏失败: ${e.message}")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/favorites/FavoritesViewModel.kt`（共 3 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L74 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L103 | `UI_STATE_OR_PROMPT` | `取消收藏失败: ${result.exception.message}` | `it.copy(error = "取消收藏失败: ${result.exception.message}")` |
| L123 | `UI_STATE_OR_PROMPT` | `批量取消收藏失败: ${e.message}` | `it.copy(error = "批量取消收藏失败: ${e.message}")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/MistakesViewModel.kt`（共 5 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L82 | `UI_STATE_OR_PROMPT` | `加载单词错题失败: ${e.message}` | `error = "加载单词错题失败: ${e.message}"` |
| L135 | `UI_STATE_OR_PROMPT` | `删除失败: ${result.exception.message}` | `it.copy(error = "删除失败: ${result.exception.message}")` |
| L155 | `UI_STATE_OR_PROMPT` | `删除失败: ${result.exception.message}` | `it.copy(error = "删除失败: ${result.exception.message}")` |
| L175 | `UI_STATE_OR_PROMPT` | `清空失败: ${result.exception.message}` | `it.copy(error = "清空失败: ${result.exception.message}")` |
| L195 | `UI_STATE_OR_PROMPT` | `清空失败: ${result.exception.message}` | `it.copy(error = "清空失败: ${result.exception.message}")` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/WrongGrammarsViewModel.kt`（共 2 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L68 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L106 | `UI_STATE_OR_PROMPT` | `删除失败: ${e.message}` | `error = "删除失败: ${e.message}"` |

### 📄 `feature/collection/src/main/java/com/jian/nemo/feature/collection/mistakes/WrongWordsViewModel.kt`（共 2 处硬编码）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L88 | `UI_STATE_OR_PROMPT` | `加载失败: ${e.message}` | `error = "加载失败: ${e.message}"` |
| L118 | `UI_STATE_OR_PROMPT` | `删除失败: ${e.message}` | `error = "删除失败: ${e.message}"` |

---

## 三、验收标准自查

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `各模块自身的 res/values/strings_xxx.xml`。
- [ ] 成功执行 `./gradlew :feature:collection:compileDebugKotlin :core:ui:compileDebugKotlin`，模块独立编译通过，0 错误。
