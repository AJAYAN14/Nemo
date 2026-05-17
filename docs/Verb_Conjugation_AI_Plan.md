# AI 动词活用训练模块 需求规划与技术实施方案

| 版本 | 日期 | 状态 | 负责人 |
| :--- | :--- | :--- | :--- |
| v2.1 | 2026-05-17 | 确认待执行 | Antigravity (PM & Developer) |

---

## 1. 背景与改造目标

### 1.1 背景
当前的动词活用界面（`VerbConjugationScreen.kt`）使用硬编码的 5 道精选题。虽然体验流畅，但题库固定，用户在反复练习后会产生记忆效用，无法起到真正的“活用”训练目的。

### 1.2 改造目标
引入 AI 实时出题机制。建立一个**工业级抗幻觉、防崩溃、低成本**的动态出题系统。
允许用户自由选择 JLPT 日语等级（N5-N1），系统从本地词库中筛选出对应等级的动词喂给 AI 进行定制出题。系统具备严密的容错重试、过采样筛选与缓存机制，并且将作答历史完整保存至本地数据库，融入全站学习统计。

---

## 2. 用户旅程与 UI 架构设计 (UI/UX & State)

### 2.1 状态机重构 (Sealed Interface)
为了应对大模型请求的复杂异步过程及偶发故障，我们将彻底废弃原本易错的平铺式 State，重构为严格的密封接口状态机：
```kotlin
sealed interface VerbUiState {
    data object Loading : VerbUiState 
    data object ApiNotConfigured : VerbUiState 
    data object LevelSelecting : VerbUiState 
    data object Generating : VerbUiState 
    data class Ready(
        val questions: List<VerbConjugationQuestion>,
        val currentIndex: Int = 0,
        val selectedOptionIndex: Int? = null,
        val isAnswered: Boolean = false
    ) : VerbUiState
    data class Finished(val correctCount: Int, val totalCount: Int) : VerbUiState
    data class Error(val message: String) : VerbUiState
}
```

### 2.2 关键交互流程
1. **开始前等级选择弹窗**：进入模块后首先弹出 `ModalBottomSheet`，提供 `N5` 到 `N1` 五个等级卡片供选择。
2. **API 未配置占位引导**：若未配置 API Key，展示“未连接 AI 大脑”引导页，配有“去配置”按钮一键跳转。
3. **出题 Loading 状态**：AI 批量出题时，展示 iOS 风格的加载微动效，文案提示：“AI 正在结合您的词库准备题目中...”。
4. **答题与解析反馈**：支持 TTS 发音和 `FuriganaText` 汉字注音。答题后弹出底部反馈栏，进行实时解析。

---

## 3. 防崩溃容错层与技术架构 (Robustness & Architecture)

大模型在实际应用中常出现输出不稳定或幻觉。为保证线上运行**零白屏**，我们引入多级安全防护。

### 3.1 本地动词筛选与冗余生成（过采样）机制
1. 调用 `WordRepository` 获取本地动词，根据选择等级和 `isDelisted == false` 进行过滤。
2. 随机抽取 5 个动词的 `spelling`, `hiragana`, `chinese` 封装为上下文喂给大模型。
3. **过采样**：要求 AI 一次性生成 **8 道题**（而不是恰好 5 道）。
4. 收到题目后，本地进行极其严格的“坏题剔除”，最终只要能成功洗出 **5 道合格好题** 即可呈现。

### 3.2 容错解析层：`safeParseVerbQuestions`
在获取到 API 字符串后，必须依次经过安全管道：
1. **Markdown 清洗**：正则去除可能多包的 ```json 标签。
2. **JSON Repair**：尝试修复尾部截断。
3. **Schema Validate**：结构化映射，拦截缺失字段。
4. **Fallback 重试**：若好题数量不足 5 道，静默发起 1 次自动重试。

### 3.3 本地二次校验拦截：`validateQuestion`
每道题必须通过本地严格检查，否则视为“坏题”：
*   **选项校验**：`options` 不多不少刚好 4 个，且互相去重。
*   **越界拦截**：`correctIndex` 必须在 `0..3` 范围内，且指向的答案必须与 AI 声明的标准答案一致。
*   **同源检验**：4个选项必须判定为原动词的合法变形，绝对屏蔽无关幻觉选项。

### 3.4 成本优化与缓存 (Cache Mechanism)
为防止反复进出页面导致高频刷 Token：
*   内存中构建 `Map<CacheKey, CachedQuestions>`，`CacheKey = MD5(selectedLevel + wordRawIds.sorted().joinToString())`。
*   设定短效缓存周期（如 30 分钟），命中后秒开，极大降低 API 成本。

---

## 4. Prompt 核心出题策略 (Strict Prompting)

我们在 `AIClient.kt` 中构建的专属系统 Prompt 具备以下最高级别强约束：

1.  **词汇硬约束**：给你传入的这 5 个日语动词，**必须且仅能全部被使用，每个动词被使用一次且仅一次，绝对禁止遗漏或重复调用某一个词**。
2.  **语法等级不超纲**：题干语法必须绝对符合 `{difficulty}` 的大纲水平，严禁超纲。
3.  **变形覆盖面约束（13 种类型）**：8 道题考察的动词活用变形类型必须尽量相互独立，**绝对禁止连续重复考察同一种活用**。考察范围仅限以下 13 种：
    1.  **辞书形（基本形/原形）** 2. **ます形** 3. **ない形** 4. **た形** 5. **て形**
    6.  **意志形** 7. **命令形** 8. **禁止形** 9. **ば形（假定形）** 10. **可能形**
    11. **被动形** 12. **使役形** 13. **使役被动形**
4.  **干扰项约束**：
    *   **唯一正确答案**：根据设问语境，选填该动词正确的活用形式。
    *   **高迷惑性干扰项**：另外 3 个错误选项**必须且仅能是该动词的其他不同活用变形**。选项互不重复，在语境中语法不通但本身是合法变形，用以全面考察活用语感。
5.  **格式安全约束**：其他汉字必须用 `汉字[假名]` 注音格式，设问处用 `____`。返回纯 JSON，严禁附带解释文本。

---

## 5. 历史记录落库方案 (Persistence)

复用现有表结构实现零开销数据沉淀，无缝对接全局统计与热力图：
*   **数据库表**：`test_records` (由 `TestRecordEntity` 映射)
*   **字段设定**：
    *   `testMode` = `"verb_conjugation_ai"`
    *   `correctAnswers` = 用户的正确题数
    *   `totalQuestions` = `5`
    *   `date` = 当前 Epoch Day

---

## 6. 待修改文件清单与分工

本次升级完美契合最小变更原则，不破坏既有架构：

### 6.1 [MODIFY] [AIClient.kt](file:///e:/AndroidProjects/Nemo/core/data/src/main/java/com/jian/nemo/core/data/util/AIClient.kt)
*   新增 `generateVerbConjugationQuestions` 挂起方法及 `safeParseVerbQuestions` 解析层。
*   构建基于缓存机制的请求通道。
*   编写融入高级防幻觉约束的 `VerbConjugationPrompt`。

### 6.2 [MODIFY] [VerbConjugationViewModel.kt](file:///e:/AndroidProjects/Nemo/feature/test/src/main/java/com/jian/nemo/feature/test/presentation/ability/VerbConjugationViewModel.kt)
*   注入 `WordRepository`、`SettingsRepository` 与 `TestRecordDao`。
*   将扁平 UI State 升级为 `sealed interface VerbUiState`。
*   实现等级选择过滤逻辑、过采样坏题剔除逻辑（`validateQuestion`）以及容错重试。
*   结算后调用 `TestRecordDao.insert` 保存历史记录。

### 6.3 [MODIFY] [VerbConjugationScreen.kt](file:///e:/AndroidProjects/Nemo/feature/test/src/main/java/com/jian/nemo/feature/test/presentation/ability/VerbConjugationScreen.kt)
*   支持状态机模式渲染（处理 `LevelSelecting` 弹窗, `Loading` 骨架动画, `ApiNotConfigured` 引导）。
*   正常展示并对接 `Ready` 态的数据进行 TTS 与 UI 响应。

---

## 7. 验证方案

1. **未配置 API Key 验证**：
   - 清除 API Key，确认页面弹出等级选择；选定后，确认正确显示“未连接 AI 大脑”占位界面并能正常跳转。
2. **AI 生成正确性及容错验证**：
   - 配置 API Key，选定等级。验证骨架屏及 Loading 动效。
   - 验证最终展示的 5 题，考察词汇精确对准了由本地过滤的 5 个动词。
   - 断点查验后台确有过采样 8 题行为，并成功完成坏题拦截。
   - 验证选项 100% 遵守“同源变形”的干扰项设计。
3. **数据落库与统计验证**：
   - 答对/错若干题后进入结算页，退出模块并前往统计页。验证当天测试历史数量及正确率相应增长，且学习热力图正常更新。
