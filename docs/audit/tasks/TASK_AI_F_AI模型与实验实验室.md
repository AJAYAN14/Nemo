# 【AI F - AI 模型与实验实验室】专属任务执行工单

> **工单代号**：`AI_F`  
> **负责模块**：`:feature:settings`  
> **负责子域**：AI 模型工作室与液态玻璃实验室（模型配置卡片、温度滑块、Prompt 说明、物理折射调试）  
> **待修改文件数**：`4` 个  
> **待提取硬编码总数**：`45` 处  
> **推荐追加的专属资源文件**：`feature/settings/src/main/res/values/strings_settings_lab.xml`  
> **当前模块 R 引用**：`import com.jian.nemo.feature.settings.R`  
> **独立编译验证命令**：`./gradlew :feature:settings:compileDebugKotlin`  

---

## 一、执行硬约束纪律（执行前必读）

1. **严禁越界修改**：你只被授权修改本工单下方【第二章】所明确列出的文件。**严禁修改任何未被点名的文件**。
2. **独立资源文件隔离（防冲突关键）**：
   - 请将所有提取出的 `<string name="...">文案</string>` 写入/追加到专属文件：`feature/settings/src/main/res/values/strings_settings_lab.xml`。
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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/AIModelStudioScreen.kt`（共 1 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L146 | `OTHER` | `配置`<br>`模型` | `val nextName = if (config.name.isBlank() \|\| config.name.endsWith("配置") \|\| config....` |

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

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/LiquidGlassLabScreen.kt`（共 12 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L98 | `COMPOSE_UI` | `液态玻璃实验室` | `title = "液态玻璃实验室",` |
| L130 | `OTHER` | `按住按钮可自由拖拽到不同背景上对比透镜折射` | `text = "按住按钮可自由拖拽到不同背景上对比透镜折射",` |
| L149 | `OTHER` | `全透明液态按钮` | `"全透明液态按钮",` |
| L167 | `OTHER` | `带底色液态按钮` | `"带底色液态按钮",` |
| L186 | `OTHER` | `彩色液态按钮 (蓝)` | `"彩色液态按钮 (蓝)",` |
| L205 | `OTHER` | `彩色液态按钮 (橙)` | `"彩色液态按钮 (橙)",` |
| L226 | `COMPOSE_UI` | `返回` | `contentDescription = "返回",` |
| L368 | `OTHER` | `物理透镜光学折射样本` | `text = "物理透镜光学折射样本",` |
| L396 | `OTHER` | `リアルタイム屈折レンダリング` | `text = "リアルタイム屈折レンダリング",` |
| L426 | `OTHER` | `透镜厚度` | `text = "透镜厚度",` |
| L438 | `OTHER` | `折射范围` | `text = "折射范围",` |
| L450 | `OTHER` | `实时帧率` | `text = "实时帧率",` |

### 📄 `feature/settings/src/main/java/com/jian/nemo/feature/settings/components/AIModelStudioComponents.kt`（共 14 处）

| 行号 | 场景类型 | 硬编码文本字面量 | 原始代码片段 |
|---|---|---|---|
| L168 | `OTHER` | `深度求索 · V4 旗舰推理与高性价比` | `tagline = "深度求索 · V4 旗舰推理与高性价比",` |
| L176 | `OTHER` | `豆包` | `name = "豆包",` |
| L177 | `OTHER` | `字节跳动 · Seed 2.1 旗舰与 Evolving 持续演进` | `tagline = "字节跳动 · Seed 2.1 旗舰与 Evolving 持续演进",` |
| L185 | `OTHER` | `智谱 GLM` | `name = "智谱 GLM",` |
| L186 | `OTHER` | `智谱 AI · GLM-5.3 旗舰推理与 Agent 编码` | `tagline = "智谱 AI · GLM-5.3 旗舰推理与 Agent 编码",` |
| L195 | `OTHER` | `Moonshot AI · K3 旗舰 1M 多模态与长程编程` | `tagline = "Moonshot AI · K3 旗舰 1M 多模态与长程编程",` |
| L204 | `OTHER` | `小米大模型 · V2.6 旗舰推理与端云协同` | `tagline = "小米大模型 · V2.6 旗舰推理与端云协同",` |
| L212 | `OTHER` | `通义千问` | `name = "通义千问",` |
| L213 | `OTHER` | `阿里云百炼 · qwen3.8-max 旗舰与双轨生态` | `tagline = "阿里云百炼 · qwen3.8-max 旗舰与双轨生态",` |
| L224 | `OTHER` | `Anthropic · 5.x 旗舰体系与自适应思考链` | `tagline = "Anthropic · 5.x 旗舰体系与自适应思考链",` |
| L233 | `OTHER` | `GPT-6 旗舰梯队 · Astra 深度推理与 Sol 智能体` | `tagline = "GPT-6 旗舰梯队 · Astra 深度推理与 Sol 智能体",` |
| L242 | `OTHER` | `Google DeepMind · Argon 旗舰推理与百万上下文` | `tagline = "Google DeepMind · Argon 旗舰推理与百万上下文",` |
| L250 | `OTHER` | `自定义` | `name = "自定义",` |
| L251 | `OTHER` | `接入任意兼容 OpenAI 协议的接口与模型` | `tagline = "接入任意兼容 OpenAI 协议的接口与模型",` |

---

## 三、完成验收检查单

- [ ] 本工单列出的所有源码文件已完成文本替换，不再包含裸中文字符串字面量。
- [ ] 所有提取的字符串均已按标准命名写入 `feature/settings/src/main/res/values/strings_settings_lab.xml`。
- [ ] 成功执行 `./gradlew :feature:settings:compileDebugKotlin`，编译通过，0 Error。
