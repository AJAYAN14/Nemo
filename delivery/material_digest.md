# AICoding 架构设计 · 资料摘要

> 本文档做一件事：**精读主理人转交的全部原始资料，逐份、逐章节做出摘要**——后面任何人拿到这份摘要，都能通过章节号快速定位回原始文件的对应位置。

> 上游输入：主理人转交的全部原始资料（Kotlin 源码 / Gradle / Markdown）；
> 产出者：`knowledge-ingest-engineer`（知识摄入工程师 - 闻资料），经 G1 校验与人工审核通过后交付。

---

## 0. 元信息

```yaml
标题: Nemo - LearningViewModel 模块化瘦身重构 - 资料摘要 v0.1
版本: v0.1
状态: Draft
创建日期: 2026-10-29
整理人: knowledge-ingest-engineer（闻资料）
审核人:
  - team-lead（主理人）

原始资料清单:
  - D1 LearningViewModel.kt: 重构目标文件，2443 行，承担 12 类职责的臃肿 ViewModel
  - D2 LearningEvent.kt: UI 事件 sealed interface，30+ 种用户交互事件
  - D3 LearningUiState.kt: UI 状态 data class + LearningStatus 枚举 + SlideDirection
  - D4 LearningModels.kt: LearningItem sealed class + CardBadge 枚举
  - D5 LearningExt.kt: toLearningItems 扩展 + cardBadge 属性 + parseSteps
  - D6 LearningMode.kt: 学习模式 sealed class（Word/Grammar）
  - D7 LearningModule.kt: Hilt DI 模块（仅绑定 CategoryLearningPreferences）
  - D8 domain 层已抽取类（8 个文件）: LearningQueueManager/LearningScheduler/SessionLoader/LearningSessionPolicy/LearningSessionStatsManager/LearningTtsManager/LearningUndoHelper/SrsIntervalPreview
  - D9 README.md: 项目背景，Nemo 日语词汇学习 App，模块化+MVI 架构
  - D10 settings.gradle.kts: 模块结构，core(domain/data/ui/designsystem/common) + feature(learning/library/user/settings/collection/statistics/test)
```

| 版本 | 日期 | 作者 | 变更内容 |
| --- | --- | --- | --- |
| v0.1 | 2026-10-29 | knowledge-ingest-engineer（闻资料） | 初稿，逐份精读 D1~D10 共 10 份资料 |

---

## 1. 资料清单

> 列出全部原始资料，每份标注解析状态。解析失败或跳过的必须注明原因。

| 编号 | 文件名 | 类型 | 来源 | 解析状态 | 说明 |
| --- | --- | --- | --- | --- | --- |
| D1 | LearningViewModel.kt | kotlin | feature/learning/presentation | 已解析 | 2443 行，重构目标核心文件，@HiltViewModel，承担事件分发/设置监听/会话加载/评分调度/队列管理/撤销/统计持久化/TTS/反馈/钉子户/模式切换/导航共 12 类职责 |
| D2 | LearningEvent.kt | kotlin | feature/learning/presentation | 已解析 | 247 行，sealed interface 定义全部 UI 事件，含会话启动/卡片交互/评分/导航/TTS/设置切换/反馈/模式等 30+ 种事件 |
| D3 | LearningUiState.kt | kotlin | feature/learning/presentation | 已解析 | 143 行，data class 定义 MVI 单一状态源，含 40+ 状态字段；LearningStatus 枚举 8 种状态；SlideDirection 枚举 |
| D4 | LearningModels.kt | kotlin | feature/learning/presentation | 已解析 | 54 行，LearningItem sealed class 统一封装 Word/Grammar；CardBadge 枚举 4 种卡片状态标记 |
| D5 | LearningExt.kt | kotlin | feature/learning/presentation | 已解析 | 49 行，toLearningItems 扩展函数、cardBadge 扩展属性（含历史数据容错修正）、parseSteps 步进配置解析 |
| D6 | LearningMode.kt | kotlin | feature/learning/presentation | 已解析 | 26 行，sealed class 区分 Word/Grammar 两种学习模式 |
| D7 | LearningModule.kt | kotlin | feature/learning/di | 已解析 | 21 行，Hilt DI 模块，仅绑定 CategoryLearningPreferences 接口；domain 层类通过 @Inject constructor 自动注入 |
| D8 | domain 层 8 个 .kt 文件 | kotlin | feature/learning/domain | 已解析 | 已抽取的 8 个领域类：LearningQueueManager(281行)/LearningScheduler(155行)/SessionLoader(242行)/LearningSessionPolicy(218行)/LearningSessionStatsManager(244行)/LearningTtsManager(71行)/LearningUndoHelper(79行)/SrsIntervalPreview(126行) |
| D9 | README.md | markdown | 项目根目录 | 已解析 | 170 行，项目背景与技术栈说明，Nemo 日语词汇学习 App，Kotlin+Compose+MVI+Hilt+Room+Supabase |
| D10 | settings.gradle.kts | gradle | 项目根目录 | 已解析 | 42 行，模块结构定义：:app + :core(common/domain/data/designsystem/ui) + :feature(collection/learning/library/statistics/test/user/settings) |

**类型枚举**：`docx` / `pdf` / `pptx` / `xlsx`（本项目资料均为 Kotlin 源码 / Gradle / Markdown，类型列标注为实际格式）

---

## 2. 资料内容摘要

> 逐份文档按自身章节结构做摘要。每条摘要标注章节号（`D编号，§章节`），后面任何人想核实某个点，直接定位回原文对应位置即可。

### D1：LearningViewModel.kt

> 重构目标核心文件，2443 行 @HiltViewModel，承担学习模块全部业务逻辑 — 来源：`feature/learning/presentation`

| 章节 | 内容摘要 |
| --- | --- |
| D1，§1类声明与构造参数 (L59-101) | `@HiltViewModel class LearningViewModel @Inject constructor`，注入 21 个依赖：5 个 Word UseCase（GetNewWords/GetDueWords/GetLearningWords/UpdateWord/GetTodayLearnedWordsCount）、5 个 Grammar UseCase（同构）、4 个 Repository（Settings/StudyRecord/Word/Grammar）、1 个 Service（SrsCalculator）、8 个 Domain 类（LearningSessionPolicy/SrsIntervalPreview/SessionLoader/LearningQueueManager/LearningScheduler/LearningUndoHelper/LearningTtsManager）、外加 ContentReportRepository、CloudBackupManager、ReviewLogRepository、GetLearningStatsUseCase。构造参数极度臃肿，是职责过载的首要信号。 |
| D1，§2companion object 常量 (L102-111) | 3 个防抖/容差常量：`NAVIGATION_DEBOUNCE_MS=400L`（导航防抖）、`RATING_DEBOUNCE_MS=100L`（评分防抖）、`SHOW_ANSWER_GRACE_MS=150L`（显示答案边界容差，避免倒计时到点仍被拦截）。 |
| D1，§3私有状态字段 (L113-153) | 核心状态源 `_uiState: MutableStateFlow<LearningUiState>`；6 个内部追踪 Map/Set：`_learningSteps`（ItemId->StepIndex 学习阶段）、`_learningDueTimes`（ItemId->DueTime 到期时间）、`_lapseCounts: MutableStateFlow<Map<Int,Int>>`（失败次数，StateFlow 保证并发安全）、`_requeuedItems: mutableSetOf<Int>`（重入队标记）、`_sessionLockedDay: Long?`（会话锁定学习日，零点跨天保护）；`sessionStatsManager = LearningSessionStatsManager(context)`（结算战报管理器，直接 new 而非注入）；7 个可变配置变量（`_resetHour/_leechThreshold/_leechAction/_learningStepsConfig/_relearningStepsConfig/_learnAheadLimitMs/_showAnswerDelayMs`）；3 个 Job（`ratingJob/navigationJob/loadingJob`）。 |
| D1，§4init 设置监听块 (L155-335) | init 块启动 18+ 个 `viewModelScope.launch` 协程监听 SettingsRepository 的 Flow：深色模式、图标风格、打卡统计(Streak)、Leech 阈值、Leech 处置动作、TTS 事件观察、每日目标(Word/Grammar 分开)、失败次数(Word/Grammar 分开)、学习步进/重学步进、提前学习限制、自动朗读、显示答案等待开关与时长(Word/Grammar 各 4 个)、自动翻面开关与时长(Word/Grammar 各 4 个)。每个 Flow collector 都包含模式判断逻辑（仅当 learningMode 匹配时更新）。 |
| D1，§5handleDailyGoalChange (L340-342) | 私有方法，仅更新 `_uiState.dailyGoal`。注释说明"新目标将在下个学习日或下一次新建会话时生效"，但 init 块中 dailyGoalFlow/grammarDailyGoalFlow 的 collector 已内联实现了相同逻辑（L202-217），此方法实际未被调用（死代码）。 |
| D1，§6onEvent 事件分发 (L347-428) | 公开方法 `fun onEvent(event: LearningEvent)`，用 `when` 分发 30+ 种 LearningEvent：StartLearning→startLearning()、StartBonusLearning→startBonusLearning()、ChangeLearningMode→changeLearningMode()、FlipCard/ShowAnswer→showAnswer()、RateWord/RateGrammar→rate(quality)、NavigateNext/Prev/GoToIndex→导航、Undo→undo()、ResumeFromWaiting→resumeFromWaiting()、SuspendCurrent/BuryCurrent→对应方法、ExitLearning→exitLearning()、PauseTimer/ResumeTimer→计时器、ToggleGrammarDetail→toggleGrammarDetail()、LoadPreviousGrammar/LoadNextGrammar→navigatePrev/Next()、MasterGrammar→rate(5)、SpeakWord/SpeakExample→learningTtsManager、Toggle*系列→对应 toggle 方法、ReportContentError→handleReportError()、SetDarkMode/CycleDarkMode→settingsRepository、ToggleWhiteboard→直接更新 uiState。是所有用户交互的唯一入口。 |
| D1，§7handleReportError (L430-461) | 私有方法，处理内容错误反馈。关闭对话框→获取当前 itemId/itemType→调用 `contentReportRepository.reportContentError`→成功显示"反馈成功"，失败按消息内容分类友好提示（重复提交 unique_user_item_report / 网络错误 connection closed/resolve host/timeout / 其他）。 |
| D1，§8startLearning (L466-537) | 私有方法，启动学习会话。流程：清空撤销快照→取消 loadingJob→设 Loading 状态→获取 dailyGoal（按模式）→获取 resetHour 并锁定 _sessionLockedDay→获取 completedToday（按模式调 UseCase）→loadSavedSession→加载 _lapseCounts→loadWordSession/loadGrammarSession→handleSessionLoadResult。异常处理：CancellationException 重抛，其他异常设 Error 状态。 |
| D1，§9startBonusLearning (L542-687) | 私有方法，启动加餐学习（消化积压/学习新词双模式）。流程：清撤销→设 Loading→获取 completedToday/dailyGoal/isRandom→按 bonusMode 取数：ReviewBacklog 调 getDueWords/Grammars + partitionOverdueItems 取 activeItems.take(count)，NewItems 调 getNewWords/Grammars.take(count)→空列表直接 SessionCompleted→不足数量提示→清空 _learningSteps/_learningDueTimes/_requeuedItems→checkAndRestoreOrReset+resumeTimer→设 Learning 状态（含全部统计字段）→armShowAnswerDelay+updateQueueBadgeCounts→saveSessionState。 |
| D1，§10loadSavedSession (L689-704) | 私有 suspend 方法，从 SettingsRepository 读取已保存会话（Word: getWordSession / Grammar: getGrammarSession），转换为 `SavedSession` data class（ids/index/level/steps/waitingUntil/isAnswerShown）。 |
| D1，§11loadWordSession (L706-749) | 私有 suspend 方法，委托 `sessionLoader.loadSession<Word>` 加载单词会话。传入 9 个 lambda（getItemsByIds/getDueItems/getNewItems/getLearningItems/getItemId/filterByLevel/isItemNew）和 5 个配置参数（sessionBatchSize/backlogStrategy/maxDailyReviews/todayReviewedCount/todayEpochDay）。filterByLevel 固定返回 true（全局进阶不再按等级过滤），isItemNew 判断 `repetitionCount == 0`。 |
| D1，§12loadGrammarSession (L751-794) | 私有 suspend 方法，与 loadWordSession 完全同构，仅将 Word 替换为 Grammar、Word Repository 替换为 Grammar Repository。存在大量重复代码。 |
| D1，§13handleSessionLoadResult (L796-961) | 私有泛型方法 `<T> handleSessionLoadResult`，处理三种 SessionLoadResult：**Restored**→恢复 _learningSteps/_learningDueTimes/_requeuedItems、checkAndRestoreOrReset+resumeTimer、设 Learning/Waiting 状态（含 waitingUntil 判断）、恢复全部统计字段；**NewSession**→清空内部状态、设 Learning 状态、saveSessionState 保存初始会话；**Completed**→clearSession+checkAndRestoreOrReset+completeSession+触发 cloudBackupManager.performImmediateAutoBackup+syncTodayCounts+设 SessionCompleted。末尾统一异步加载今日待复习与明日到期预测计数（Word/Grammar 分别调 repository.getDueWordsCount/getDueGrammarsCount）。 |
| D1，§14showAnswer (L969-993) | 私有方法，显示答案。前置检查：Processing 状态拒绝、showAnswerAvailableAt 未到时间且超出 grace 容差拒绝。设 isAnswerShown=true/isCardFlipped=true/showAnswerAvailableAt=0L。自动朗读：isAutoAudioEnabled 且 Word 模式时调 learningTtsManager.speakWord(hiragana, chinese)。末尾 saveCurrentSessionState（记录 isAnswerShown=true）。 |
| D1，§15toggle* 系列设置方法 (L995-1080) | 8 个私有方法：toggleAutoPlayAudio/toggleShowAnswerDelay/cycleShowAnswerDelayDuration/setShowAnswerDelayMs（Word+Grammar 通过 isGrammar 参数区分）、toggleAutoRevealAnswer/cycleAutoRevealAnswerDuration/setAutoRevealAnswerMs（Word 专用）、toggleGrammarAutoRevealAnswer/cycleGrammarAutoRevealAnswerDuration/setGrammarAutoRevealAnswerMs（Grammar 专用）。cycle* 方法用 when 硬编码档位循环（如 showAnswerDelay: 2000→3000→4000→5000→2000）。全部委托 settingsRepository 持久化。 |
| D1，§16armShowAnswerDelay (L1082-1102) | 私有方法，装填显示答案延迟计时。前置检查：已显示答案/无待处理项/非 Learning 状态时清零返回。计算当前卡片冷却锁定期（dueTime-now > learnAheadLimitMs 时 isCooling）。delayTarget = isShowAnswerDelayEnabled ? now + showAnswerDelayMs : 0。coolingTarget = isCooling ? dueTime : 0。availableAt = max(delayTarget, coolingTarget)，写入 uiState.showAnswerAvailableAt。 |
| D1，§17saveUndoSnapshot (L1107-1133) | 私有方法，保存撤销快照。获取当前 LearningItem→构建 UndoSnapshot（含 originalWord/originalGrammar/wordList/grammarList/currentIndex/currentGrammarIndex/learningSteps/lapseCounts/requeuedItems/learningDueTimes/completedToday/completedThisSession/sessionProcessedCount/wasNew）→委托 learningUndoHelper.saveSnapshot→syncUndoAvailability。 |
| D1，§18syncUndoAvailability (L1138-1141) | 私有方法，从 learningUndoHelper.canUndo() 读取，更新 uiState.canUndo。单一真相源为 LearningUndoHelper。 |
| D1，§19undo (L1146-1228) | 私有方法，撤销上一次评分。前置检查 Processing 拒绝。popSnapshot 获取快照→立即 syncUndoAvailability→协程内：设 Processing→恢复数据库（updateWordUseCase/updateGrammarUseCase + studyRecordRepository 增减计数）→恢复内部 Map/Set→恢复 UI 状态（isAnswerShown=true/isCardFlipped=true/BACKWARD 方向）→updateQueueBadgeCounts+syncUndoAvailability+saveCurrentSessionState。异常设 Error。 |
| D1，§20rate (L1235-1362) | 核心私有方法，统一处理 Word/Grammar 评分。防抖：Processing 拒绝，ratingJob.cancel。流程：saveUndoSnapshot→设 Processing→handleSkippedItemsOnRating（跳过卡片顺延）→构建 LearningItem（含 step/dueTime）→sessionStatsManager.onItemRated→按 quality 分支：**quality==5** 熟词速通→_learningSteps.remove+processSrsUpdate；**quality<3** 忘记→applyLapsePenalty+learningScheduler.scheduleFailure+handleScheduleResult；**quality 3-4** 掌握→isLearning 时 schedulePass+handleScheduleResult，否则 processSrsUpdate。末尾 delay(RATING_DEBOUNCE_MS)。异常设 Error。 |
| D1，§21applyLapsePenalty (L1370-1405) | 私有方法，纯内存计算失败惩罚后的 Item（不写库，避免阻塞切卡动画）。调 srsCalculator.calculate(item.word/grammar, quality, today)→copy 出更新后的 Word/Grammar（type=3 标记重学、更新 interval/repetitionCount/stability/difficulty/nextReviewDate/lastReviewedDate/firstLearnedDate/lastModifiedTime）→返回更新后的 LearningItem。 |
| D1，§22processRelearningGraduation (L1413-1460) | 私有方法，重学毕业处理。此时 Interval 已在 Lapse 时被惩罚过。构建更新后的 Word/Grammar（type=2 毕业进入复习、interval 取 coerceAtLeast(1)、repetitionCount+1、nextReviewDate=today+interval）。乐观更新 UI（completedThisSession+1、completedToday 新词才+1、sessionProcessedCount+1）→removeCurrentAndMoveNext→persistRatingItemAsync(isGraduation=true)。 |
| D1，§23handleScheduleResult (L1465-1524) | 私有 suspend 方法，处理调度结果三种分支：**Leech**→handleLeech；**Graduate**→_learningSteps.remove，区分新卡毕业(processSrsUpdate) vs 重学毕业(processRelearningGraduation，!isNew && quality<5)；**Requeue**→_learningSteps[id]=nextStepIndex、更新 item.type、乐观更新 sessionProcessedCount+1、reQueueAtRelativeOffset、persistRatingItemAsync(isGraduation=false)。 |
| D1，§24processSrsUpdate (L1531-1585) | 私有方法，SRS 更新与毕业。调 srsCalculator.calculate→构建更新后的 Word/Grammar（type=2 复习、全部 SRS 字段更新）。乐观更新 UI（completedThisSession+1、completedToday 新词才+1、sessionProcessedCount+1）→removeCurrentAndMoveNext→persistRatingItemAsync(isGraduation=true)。与 processRelearningGraduation 结构高度相似，区别在于 SRS 字段来源（srsCalculator vs 手动 copy）。 |
| D1，§25persistRatingItemAsync (L1590-1697) | 私有方法，后台异步持久化评分结果。`viewModelScope.launch(Dispatchers.IO)`。Word 分支：updateWordUseCase→失败 return→isLapse 时 incrementWordLapse→isGraduation 时 incrementLearnedWords/ReviewedWords、addTodayTestedWordId、quality<=2 时 addTodayWrongWordId、插入 ReviewLog、srsCalculator.notifyReviewCompleted→syncTodayCounts。Grammar 分支完全同构。 |
| D1，§26reQueueAtRelativeOffset (L1702-1754) | 私有方法，对齐 Anki 的卡片重入队。保存 _learningSteps[id]=step、_learningDueTimes[id]=dueTime。按模式调用 learningQueueManager.reorderQueueAnkiStyle（传入 wordList/grammarList、currentIndex、itemToRequeue、getDueTime lambda）→learningQueueManager.selectNextItem→handleSelectionResult。 |
| D1，§27resumeFromWaiting (L1759-1803) | 私有方法，从等待状态恢复。清 waitingUntil→按模式调 sortAnkiStyle(now=Long.MAX_VALUE) 强制全部到期→移除所有 _learningDueTimes→构建 QueueSelectionResult.Next(0, first)→handleSelectionResult。 |
| D1，§28removeCurrentAndMoveNext (L1808-1850) | 私有方法，移出已毕业卡片并平滑移动到下一个。按模式：learningQueueManager.removeCurrent→sortAnkiStyle→selectNextItem→handleSelectionResult。 |
| D1，§29handleSelectionResult (L1852-1959) | 私有泛型方法 `<T> handleSelectionResult`，处理队列选择结果三种分支：**Empty**→completeSession；**Wait**→设 Waiting 状态+waitingUntil+saveSessionState；**Next**→更新 uiState（新列表/newIndex/currentWord|Grammar/isAnswerShown=false/isCardFlipped=false/ratingIntervals/SlideDirection.FORWARD）→armShowAnswerDelay+saveSessionState。含 Learn Ahead 检测（dueTime > now 时移除 _learningDueTimes 标记提前学习）。末尾 updateQueueBadgeCounts。 |
| D1，§30completeSession (L1964-1993) | 私有方法，完成会话。sessionStatsManager.completeSession→设 SessionCompleted 状态（清空 currentWord/Grammar/wordList/grammarList、showAnswerAvailableAt=0/waitingUntil=0/sessionDurationSeconds=totalDuration）→clearSession+syncUndoAvailability+syncTodayCounts→触发 cloudBackupManager.performImmediateAutoBackup。 |
| D1，§31syncTodayCounts (L1998-2036) | 私有方法，从数据库安全同步今日新学数与复习数。`viewModelScope.launch(Dispatchers.IO)`。按模式调 getTodayLearnedWordsCountUseCase/GrammarsCountUseCase + wordRepository.getTodayReviewedWords/Grammars→更新 uiState（sessionNewCount/sessionReviewCount/sessionRelearnCount/sessionMaxCombo）。异常时 fallback 到 sessionStatsManager 的值。 |
| D1，§32pauseTimer (L2041-2044) | 私有方法，委托 sessionStatsManager.pauseTimer(mode, today)。 |
| D1，§33resumeTimer (L2049-2055) | 私有方法，仅当 sessionStartTime==0L 且 status==Learning 时 checkAndRestoreOrReset+sessionStatsManager.resumeTimer。 |
| D1，§34checkAndRestoreOrReset (L2060-2063) | 私有方法，委托 sessionStatsManager.checkAndRestoreOrReset(mode, today)，检查学习日天数用于恢复或跨天重置。 |
| D1，§35navigateNext/navigatePrev (L2068-2103) | 私有方法，导航到下一个/上一个。前置检查 isNavigating/Processing 拒绝。计算 nextIndex/prevIndex，边界检查后调 navigateTo。 |
| D1，§36navigateTo (L2108-2171) | 私有方法，导航到指定索引。navigationJob.cancel+launch→设 isNavigating=true→按模式更新 uiState（currentIndex/currentWord|Grammar/isAnswerShown=false/isCardFlipped=false/ratingIntervals/slideDirection）→armShowAnswerDelay→saveSessionState→delay(NAVIGATION_DEBOUNCE_MS)→设 isNavigating=false。 |
| D1，§37getCurrentItem (L2173-2179) | 私有方法，按模式从 uiState 构建 LearningItem.WordItem/GrammarItem 返回。 |
| D1，§38calculateRatingIntervals (L2181-2190) | 私有方法，按模式委托 srsIntervalPreview.calculate(item, id, _learningSteps, _learningStepsConfig, _relearningStepsConfig, today)，返回 Map<Int, String>（quality 0-5 → 格式化间隔文本）。 |
| D1，§39updateQueueBadgeCounts (L2197-2226) | 私有方法，统计当前队列中各 CardBadge 状态数量（NEW/LEARNING/REVIEW/RELEARN），更新 uiState 的 queueNewCount/queueLearningCount/queueReviewCount/queueRelearnCount。 |
| D1，§40saveSessionState/saveCurrentSessionState (L2228-2249) | 私有方法，按模式调 settingsRepository.saveWordSession/saveGrammarSession（ids/index/level/_learningSteps/waitingUntil/isAnswerShown）。saveCurrentSessionState 是快捷封装。 |
| D1，§41clearSession (L2251-2258) | 私有方法，按模式调 settingsRepository.clearWordSession/clearGrammarSession。 |
| D1，§42changeLearningMode (L2260-2277) | 私有方法，切换学习模式。设 uiState.learningMode→协程内读取新模式对应的 showAnswerDelay 配置→更新 _showAnswerDelayMs 和 uiState→armShowAnswerDelay→调 startLearning() 立即触发新模式会话加载。 |
| D1，§43toggleGrammarDetail (L2281-2284) | 私有方法，切换语法详情可见性。非 Learning 状态拒绝。 |
| D1，§44exitLearning (L2286-2294) | 私有方法，退出学习。清空撤销快照→syncUndoAvailability→sessionStatsManager.clearSessionTimer→`_uiState.value = LearningUiState()`（重置全部状态）。 |
| D1，§45handleLeech (L2301-2343) | 私有 suspend 方法，处理钉子户。按 _leechAction 分支：**bury_today**→copy buriedUntilDay=today→updateWordUseCase/updateGrammarUseCase→移除 _learningSteps/_learningDueTimes→提示"已埋藏顽固卡片"→removeCurrentAndMoveNext；**skip**（默认）→提示"已冻结顽固卡片"→suspendItem。 |
| D1，§46suspendCurrentItem (L2348-2360) | 私有方法，手动暂停当前项。协程内调 suspendItem→提示"已暂停"。 |
| D1，§47buryCurrentItem (L2366-2402) | 私有方法，今日暂缓（Bury）。协程内：持久化 buriedUntilDay=today→移除 _learningSteps/_learningDueTimes→提示"已暂缓"→removeCurrentAndMoveNext。与 handleLeech 的 bury_today 分支逻辑重复。 |
| D1，§48suspendItem (L2409-2435) | 私有 suspend 方法，执行暂停逻辑。持久化 isSkipped=true→移除 _learningSteps/_learningDueTimes→removeCurrentAndMoveNext。持久化失败也继续内存移除（不阻塞用户）。 |
| D1，§49onCleared (L2439-2443) | override 方法，调 learningTtsManager.stop()。 |

### D2：LearningEvent.kt

> UI 事件 sealed interface，定义全部用户交互事件 — 来源：`feature/learning/presentation`

| 章节 | 内容摘要 |
| --- | --- |
| D2，§1sealed interface 声明 (L9) | `sealed interface LearningEvent`，所有用户交互封装为事件。 |
| D2，§2会话启动事件 (L14-35) | `StartLearning(mode)` 默认 Word 模式；`StartBonusLearning(count, bonusMode, mode)` 加餐学习支持 ReviewBacklog/NewItems 双模式；`ChangeLearningMode(mode)` 切换单词↔语法。 |
| D2，§3卡片交互事件 (L42-57) | `FlipCard`（data object，翻转卡片）、`ShowAnswer`（data object，显示答案）、`RateWord(quality: Int)`（0-5 评分）、`RateGrammar(quality: Int)`（0-5 评分）。 |
| D2，§4导航事件 (L62-72) | `NavigateNext`（data object）、`NavigatePrev`（data object）、`GoToIndex(index: Int)`（跳转指定索引，用于手势滑动同步）。 |
| D2，§5打字练习事件 (L79-84) | `ShowTypingPractice`（data object）、`HideTypingPractice`（data object）。 |
| D2，§6TTS 朗读事件 (L94-100) | `SpeakWord(text, chinese)` 播放发音、`SpeakExample(japanese, chinese, id)` 朗读例句（id 用于控制声纹动画）。 |
| D2，§7撤销与恢复事件 (L105-123) | `Undo`（撤销评分）、`ResumeFromWaiting`（从等待恢复，Learn Ahead Limit 到期或用户点击继续）、`SuspendCurrent`（手动暂停）、`BuryCurrent`（今日暂缓）、`ExitLearning`（退出学习）。 |
| D2，§8语法专用事件 (L130-145) | `ToggleGrammarDetail`（切换详情可见性）、`LoadPreviousGrammar`（加载上一个）、`LoadNextGrammar`（加载下一个）、`MasterGrammar`（掌握当前语法）。 |
| D2，§9设置切换事件 (L150-235) | `ToggleAutoPlayAudio(enabled)`、`ToggleShowAnswerDelay(enabled)`、`CycleShowAnswerDelayDuration`、`SetShowAnswerDelayMs(ms)`、`ToggleAutoRevealAnswer(enabled)` Word、`CycleAutoRevealAnswerDuration` Word、`SetAutoRevealAnswerMs(ms)` Word、`ToggleGrammarAutoRevealAnswer(enabled)` Grammar、`CycleGrammarAutoRevealAnswerDuration` Grammar、`SetGrammarAutoRevealAnswerMs(ms)` Grammar。Word 和 Grammar 的自动翻面事件完全对称分离。 |
| D2，§10反馈与提示事件 (L200-220) | `ReportContentError(errorType, description?)`、`OpenReportErrorDialog`、`CancelReportErrorDialog`、`ClearSuccessMessage`、`ClearErrorMessage`。 |
| D2，§11显示模式事件 (L226-235) | `SetDarkMode(enabled: Boolean?)`（true=深色/false=浅色/null=跟随系统）、`CycleDarkMode`（循环切换 系统→浅色→深色→系统）、`ToggleWhiteboard(enabled)`（白板开关）。 |
| D2，§12计时器事件 (L240-245) | `PauseTimer`、`ResumeTimer`。 |

### D3：LearningUiState.kt

> MVI 单一不可变状态源 data class + 状态枚举 — 来源：`feature/learning/presentation`

| 章节 | 内容摘要 |
| --- | --- |
| D3，§1SlideDirection 枚举 (L9-12) | `enum class SlideDirection { FORWARD, BACKWARD }`，滑动方向用于动画。 |
| D3，§2LearningUiState data class (L18-127) | 40+ 状态字段分 10 组：学习模式（learningMode/status/currentWord/currentIndex/wordList/currentGrammar/grammarList/currentGrammarIndex/isGrammarDetailVisible）、卡片状态（isCardFlipped）、学习设置（selectedLevel/dailyGoal/completedToday）、SRS（isAnswerShown/sessionQueue 预留/completedThisSession/ratingIntervals/slideDirection/isNavigating）、复习（reviewDueCount）、打字练习（showTypingPractice）、错误（error）、会话统计（sessionInitialSize/sessionCorrectCount/sessionProcessedCount/canUndo/waitingUntil/playingAudioId）、自定义开关（isAutoAudioEnabled/isShowAnswerDelayEnabled/showAnswerDelayMs/showAnswerAvailableAt/isAutoRevealAnswerEnabled/autoRevealAnswerMs/isGrammarAutoRevealAnswerEnabled/grammarAutoRevealAnswerMs）、报告错误（showReportErrorDialog/successMessage）、队列计数（queueNewCount/queueLearningCount/queueReviewCount/queueRelearnCount）、显示模式（isDarkMode/isWhiteboardEnabled）、结算统计（sessionDurationSeconds/sessionMaxCombo/sessionNewCount/sessionReviewCount/sessionRelearnCount/tomorrowReviewForecastCount/todayDueReviewCount/dailyStreak/settingIconStyle）。两个计算属性：`hasPendingItems`（按模式判断是否有待处理项）、`shouldShowDailyGoalMet`（completedToday >= dailyGoal 且无待处理项）。 |
| D3，§3LearningStatus 枚举 (L132-141) | 8 种状态：`Idle`（空闲）、`Loading`（加载中）、`Learning`（学习中）、`Processing`（处理中，掌握/跳过）、`Waiting`（等待中，Learn Ahead Limit 超限）、`SessionCompleted`（会话完成）、`NoMoreWords`（无更多单词）、`Error`（错误）。 |

### D4：LearningModels.kt

> LearningItem sealed class 统一封装 Word/Grammar + CardBadge 状态标记 — 来源：`feature/learning/presentation`

| 章节 | 内容摘要 |
| --- | --- |
| D4，§1LearningItem sealed class (L9-43) | 抽象属性：`id: Int`、`isNew: Boolean`、`displayName: String`、`repetitionCount: Int`、调度系统必需字段 `step: Int`/`dueTime: Long`/`type: Int`。两个子类：`WordItem(word: Word, step, dueTime, type)`（id=word.id、isNew=word.repetitionCount==0、displayName=word.japanese）、`GrammarItem(grammar: Grammar, step, dueTime, type)`（id=grammar.id、isNew=grammar.repetitionCount==0、displayName=grammar.grammar）。 |
| D4，§2CardBadge 枚举 (L48-53) | 4 种卡片状态标记：`NEW`（新词 Type 0）、`LEARNING`（学习中 Type 1）、`REVIEW`（复习 Type 2）、`RELEARN`（重学 Type 3）。 |

### D5：LearningExt.kt

> 扩展函数与工具函数 — 来源：`feature/learning/presentation`

| 章节 | 内容摘要 |
| --- | --- |
| D5，§1toLearningItems 扩展 (L10-18) | `fun <T> List<T>.toLearningItems(): List<LearningItem>`，将 `List<Word>` 或 `List<Grammar>` 转换为 `List<LearningItem>`，使用 `mapNotNull` 过滤非 Word/Grammar 类型。`@Suppress("UNCHECKED_CAST")`。 |
| D5，§2cardBadge 扩展属性 (L23-36) | `val LearningItem.cardBadge: CardBadge`，根据 type 映射到 CardBadge。含容错处理：`repetitionCount > 0 && type == 0` 时修正为 REVIEW(2)（处理导入无 type 字段的历史数据或同步缺失）。type 超出 0-3 范围时默认 REVIEW。 |
| D5，§3parseSteps 函数 (L41-48) | `fun parseSteps(stepsStr: String): List<Int>`，按空白字符分割字符串转 Int 列表。异常时返回默认值 `listOf(1, 10)`。用于解析学习步进/重学步进配置。 |

### D6：LearningMode.kt

> 学习模式 sealed class — 来源：`feature/learning/presentation`

| 章节 | 内容摘要 |
| --- | --- |
| D6，§1sealed class 声明 (L7-25) | `sealed class LearningMode`，两个 data object 子类：`Word`（单词学习）、`Grammar`（语法学习）。`getDisplayName()` 方法返回"单词"/"语法"。使用 sealed class 而非 enum，但语义上等价于二值枚举。 |

### D7：LearningModule.kt

> Hilt DI 模块 — 来源：`feature/learning/di`

| 章节 | 内容摘要 |
| --- | --- |
| D7，§1Module 声明 (L11-20) | `@Module @InstallIn(SingletonComponent::class) abstract class LearningModule`。仅包含一个 `@Binds @Singleton` 方法绑定 `CategoryLearningPreferencesImpl → CategoryLearningPreferences`。domain 层的 8 个类（LearningQueueManager/LearningScheduler/SessionLoader 等）均通过 `@Inject constructor` 自动注入，无需在此显式声明。注意 LearningSessionStatsManager 直接 `new`（在 LearningViewModel L132），未走 DI。 |

### D8：domain 层已抽取类（8 个文件）

> 已抽取的领域类，承担纯逻辑计算职责 — 来源：`feature/learning/domain`

| 章节 | 内容摘要 |
| --- | --- |
| D8，§1LearningQueueManager.kt (281行) | `@Inject constructor()` 无依赖。负责队列平滑单向调度：`handleSkippedItemsOnRating`（跳过卡片顺延到队尾）、`insertAtRelativeOffset`（相对位置动态插入）、`sortAnkiStyle`（按到期时间分三组排序：已到期在学→常规→冷却中在学）、`reorderQueueAnkiStyle`（Anki 重入队重排，含防瞬间重复逻辑 Anti-immediate-repeat）、`removeCurrent`（移除当前项）、`selectNextItem`（时间感知选择下一个，支持 Learn Ahead Limit 容差）。输出 `QueueSelectionResult<T>` sealed class（Next/Wait/Empty）。 |
| D8，§2LearningScheduler.kt (155行) | `@Inject constructor()` 无依赖。纯逻辑调度器，不涉及数据库。`scheduleFailure(item, currentLapseCount, stepConfig, leechThreshold)`：钉子户检测→Again 重置 Step 0→计算 dueTime（firstStepMin）→返回 Requeue(isLapse=true)。`schedulePass(item, quality, currentStep, stepConfig)`：Hard(3) 保持当前 Step（特殊延迟计算）、Good(4) 有下一步则进阶无则毕业、Easy(5) 直接毕业。返回 `ScheduleResult` sealed class（Requeue/Graduate/Leech）。 |
| D8，§3SessionLoader.kt (242行) | `@Inject constructor(learningSessionPolicy)`。会话加载器，泛型 `<T : SrsItem>`。`loadSession` 方法：1.尝试恢复已保存会话→2.获取到期复习项+跨天学习中项→3.`learningSessionPolicy.partitionOverdueItems` 四级切片→4.微会话容量(sessionBatchSize 5..100)与每日复习上限(maxDailyReviews)配额调度→5.积压状态判定与新词配额计算（review_only=0新词 / taste=最多3新词 / 无积压=剩余容量）→6.`learningSessionPolicy.mixSessionItems` 智能穿插混合。返回 `SessionLoadResult<T>` sealed class（Restored/NewSession/Completed）。还定义 `SavedSession` data class。 |
| D8，§4LearningSessionPolicy.kt (218行) | `@Inject constructor()` 内部 new FsrsAlgorithm。纯逻辑策略，泛型 `<T : SrsItem>`。`calculateRetrievability`（FSRS 6 幂律遗忘曲线 R=(1+factor*(t/S))^(-decay)）、`calculateRelativeOverdue`（RO=逾期天数/I）、`isItemStale`（逾期≥30天且经过≥3*间隔）、`partitionOverdueItems`（四级互斥穷尽切片：P0 抢救级 0.6≤R<0.9 / P1 深度逾期 R<0.6 / P2 常规 R≥0.9 / P3 休眠隔离池）、`calculateAdjustedNewQuota`（当前不减压，原样返回）、`mixSessionItems`（Sandwich Mix 三明治穿插：Top 20% 高危复习优先→新词均匀分散到普通复习中）。定义 `OverduePartition<T>` data class。 |
| D8，§5LearningSessionStatsManager.kt (244行) | `constructor(context: Context)` 非 DI 注入（直接 new）。战报统计与本地持久化。基于 SharedPreferences(`nemo_learning_time_prefs`)。`checkAndRestoreOrReset`（跨天清零/同天恢复）、`resumeTimer/pauseTimer`（计时器）、`onItemRated`（评分时更新新学/复习/重学/连击计数并实时持久化）、`completeSession`（结算返回总时间秒数）、`getCurrentStats`（快照）、`clearSessionTimer`（仅清计时器不抹总计）。按 Word/Grammar 分前缀持久化。定义 `SessionStats` data class。 |
| D8，§6LearningTtsManager.kt (71行) | `@Inject constructor(audioRepository)`。TTS 朗读管理。`_playingAudioId: MutableStateFlow<String?>` 暴露为 StateFlow。`observeTtsEvents(scope)` 监听 AudioRepository 的 TtsEvent（OnStart/OnDone/OnError/GoogleTtsMissing），处理日语/中文分段播放的 ID 归一化（substringBefore("-jp"/"-cn")）。`speakWord(text, chinese)`（有中文时 playExampleTts 否则 playTts）、`speakExample(japanese, chinese, id)`、`stop()`。 |
| D8，§7LearningUndoHelper.kt (79行) | `@Inject constructor()` 无依赖。撤销功能辅助类，单步撤销（保留最后一次快照）。`saveSnapshot(UndoSnapshot)`、`popSnapshot()`（获取后清空防多次撤销）、`clear()`、`canUndo()`。定义 `UndoSnapshot` data class（item/originalWord/originalGrammar/wordList/grammarList/currentIndex/currentGrammarIndex/learningSteps/lapseCounts/requeuedItems/learningDueTimes/completedToday/completedThisSession/sessionProcessedCount/wasNew/wasLeech）。 |
| D8，§8SrsIntervalPreview.kt (126行) | `@Inject constructor(srsCalculator)`。SRS 间隔预览计算器，泛型支持 SrsItem。`calculate(item, itemId, steps, learningStepsConfig, relearningStepsConfig, today)` 方法：为 quality 0-5 各计算下次复习间隔文本。Fail(q<3) 显示首个重学/学习步长；Learning 模式下 Hard(3)/Good(4)/Easy(5) 按步进逻辑计算；毕业时走 srsCalculator。含单调性防御约束（Hard ≤ Good ≤ Easy）。输出 `Map<Int, String>`（quality → SrsTimeFormatter 格式化的间隔文本）。 |

### D9：README.md

> 项目背景与技术栈 — 来源：项目根目录

| 章节 | 内容摘要 |
| --- | --- |
| D9，§1项目简介 (L36-42) | Nemo 是专为日语学习者设计的灵动词汇应用，结合日语学习特点，通过沉浸式设计和云端同步功能。定位：现代设计、前沿技术与纯粹学习之间的平衡。 |
| D9，§2核心特性 (L62-70) | 7 大特性：液态玻璃视觉系统、日语专属优化（平假名/片假名/汉字词汇/假名检索/假名拼词）、灵动词库系统（卡片式/JLPT 等级筛选/关键词搜索）、AI 算法助学（复习算法/Leech 识别）、AI 例文工坊（自由模式/语法专项双模式）、可视化统计（Bento 栅格/环形图表）、云端数据中心（Supabase）。 |
| D9，§3技术栈 (L74-85) | Kotlin 1.9+、Jetpack Compose、模块化+MVI 架构、Ktor/Supabase SDK 网络层、Room+Cloud DB 持久化、Hilt(Dagger) 依赖注入、Coroutines & Flow 异步流。 |
| D9，§4模块结构 (L88-108) | 分层模块化：`:app`（入口）→`:feature`（library/learning/user/settings）→`:core`（ui/data/domain）。Mermaid 图展示依赖关系。 |
| D9，§5学习算法高级设置 (L130-155) | 5 个可调参数：学习阶段 Learning Steps（默认 `1 10` 分钟）、重学阶段 Relearning Steps（默认 `1 10`）、提前复习阈值 Learn Ahead Limit（默认 20 分钟）、Leech 阈值（默认 5 次）、Leech 行为（默认 skip，可选 bury_today）。Leech 行为说明：skip=暂停不再进入复习队列，bury_today=埋到次日明天自动回归。 |

### D10：settings.gradle.kts

> 模块结构定义 — 来源：项目根目录

| 章节 | 内容摘要 |
| --- | --- |
| D10，§1pluginManagement (L1-15) | 仓库配置：阿里云 public/google 镜像 + google + mavenCentral + gradlePluginPortal。 |
| D10，§2dependencyResolutionManagement (L17-26) | `RepositoriesMode.FAIL_ON_PROJECT_REPOS`，仓库：阿里云镜像 + google + mavenCentral + jitpack。 |
| D10，§3模块 include (L28-41) | `rootProject.name = "Nemo"`。共 12 个模块：`:app`、`:core:common`、`:core:domain`、`:core:data`、`:core:designsystem`、`:core:ui`、`:feature:collection`、`:feature:learning`、`:feature:library`、`:feature:statistics`、`:feature:test`、`:feature:user`、`:feature:settings`。core 层 5 个子模块，feature 层 7 个子模块。 |

---

## 3. 冲突记录

> 不同资料对同一事实描述矛盾时，**并列保留两个版本**，不做裁决。

| 编号 | 冲突主题 | 版本 A | 出处 A | 版本 B | 出处 B | 差异说明 |
| --- | --- | --- | --- | --- | --- | --- |
| X1 | LearningMode 类型设计 | sealed class（data object Word / data object Grammar） | D6，§sealed class 声明 (L7-25) | 无冲突版本 | — | 无冲突，但记录设计观察：语义上等价于二值枚举，sealed class 用法偏重；重构方案需评估是否改为 enum（不裁决，交下游） |
| X2 | LearningSessionStatsManager 注入方式 | 直接 `new LearningSessionStatsManager(context)` 在 LearningViewModel 构造体内 | D1，§私有状态字段 (L132) | domain 层其他 7 个类均通过 `@Inject constructor` 自动注入 Hilt | D7，§Module 声明；D8 各文件 | 注入方式不一致：StatsManager 是唯一未走 DI 的 domain 类，破坏了 DI 一致性；重构方案需评估是否改为 @Inject（不裁决，交下游） |
| X3 | Word/Grammar 自动翻面配置对称性 | Word 和 Grammar 的自动翻面开关/时长使用完全独立的事件和设置 Flow | D2，§设置切换事件 (L150-185)；D1，§init 设置监听块 (L307-333) | 显示答案等待开关/时长也按 Word/Grammar 分离，但 toggle/cycle/set 方法通过 `isGrammar` 参数统一处理 | D1，§toggle* 系列设置方法 (L995-1026) | 设计不统一：显示答案等待用参数化统一，自动翻面用完全分离的事件和方法；重构方案需评估统一策略（不裁决，交下游） |
| X4 | processSrsUpdate 与 processRelearningGraduation 结构相似度 | processSrsUpdate：srsCalculator.calculate 计算 SRS 字段后 copy | D1，§processSrsUpdate (L1531-1585) | processRelearningGraduation：手动 copy（interval+1/repetitionCount+1/nextReviewDate=today+interval） | D1，§processRelearningGraduation (L1413-1460) | 两者都做"乐观更新 UI → removeCurrentAndMoveNext → persistRatingItemAsync"，但 SRS 字段计算来源不同（srsCalculator vs 手动）；存在可合并的重复结构（不裁决，交下游） |
| X5 | buryCurrentItem 与 handleLeech(bury_today) 逻辑重复 | buryCurrentItem：手动今日暂缓，持久化 buriedUntilDay→移除内存状态→removeCurrentAndMoveNext | D1，§buryCurrentItem (L2366-2402) | handleLeech 的 bury_today 分支：同样持久化 buriedUntilDay→移除内存状态→removeCurrentAndMoveNext | D1，§handleLeech (L2301-2343) | 逻辑高度重复，唯一差异是提示文案和调用上下文（手动 vs 钉子户触发）；重构方案可评估抽取公共方法（不裁决，交下游） |
| X6 | loadWordSession 与 loadGrammarSession 代码重复 | loadWordSession：委托 sessionLoader.loadSession<Word>，9 个 lambda + 5 个配置参数 | D1，§loadWordSession (L706-749) | loadGrammarSession：委托 sessionLoader.loadSession<Grammar>，结构完全同构 | D1，§loadGrammarSession (L751-794) | 仅类型替换（Word→Grammar、WordRepository→GrammarRepository），存在大量重复代码；重构方案可评估泛型合并（不裁决，交下游） |
| X7 | selectedLevel 默认值不一致 | LearningUiState 默认 `selectedLevel = "N5"` | D3，§LearningUiState data class (L43) | startLearning 和 startBonusLearning 中硬编码设 `selectedLevel = "GLOBAL"` | D1，§startLearning (L476)；D1，§startBonusLearning (L550) | 默认值 N5 与实际运行值 GLOBAL 不一致，全局进阶已不再按等级过滤（filterByLevel 固定返回 true）；遗留字段（不裁决，交下游） |
| X8 | LearningStatus.NoMoreWords 使用情况 | 枚举中定义了 NoMoreWords（无更多单词）状态 | D3，§LearningStatus 枚举 (L139) | 全文搜索 LearningViewModel.kt 未发现任何代码将 status 设为 NoMoreWords | D1 全文 | NoMoreWords 状态定义但未使用，可能为遗留未实现状态（不裁决，交下游） |
| X9 | sessionQueue 字段使用情况 | LearningUiState 中定义 `sessionQueue: List<Word> = emptyList()`，注释标注"暂未完全实现，预留" | D3，§LearningUiState data class (L49) | LearningViewModel.kt 全文未对 sessionQueue 赋值 | D1 全文 | 预留字段未实现，重构时需评估是否移除或补实现（不裁决，交下游） |
| X10 | sessionCorrectCount 使用情况 | LearningUiState 中定义 `sessionCorrectCount: Int = 0`，注释标注 "[Fix]: Session Stats" | D3，§LearningUiState data class (L68) | LearningViewModel.kt 全文未对 sessionCorrectCount 赋值或读取 | D1 全文 | 定义但未使用的统计字段，可能为遗留（不裁决，交下游） |

---

## 4. 硬指标清单

| 章节 | 硬指标 | 状态 |
| --- | --- | --- |
| §1 | 每份资料有解析状态，失败/跳过注明原因 | ✅ D1~D10 全部 10 份资料标注"已解析"，无失败或跳过 |
| §2 | 每份文档按章节逐条摘要，每条标注了 `D编号，§章节` | ✅ D1 逐方法摘要 40+ 条（每条标注 D1，§方法名+行号范围）；D2~D10 按自身结构逐条摘要（每条标注 D编号，§章节）；D8 的 8 个 domain 类逐一摘要 |
| §3 | 冲突信息并列保留，不做裁决 | ✅ 记录 X1~X10 共 10 条，全部并列保留两个版本并标注出处，未做裁决 |

---

## 附录 A：生成流程

### 流程总览

| 步骤 | 动作 | 落入章节 |
| --- | --- | --- |
| Step0 | 读取模板 + 全部原始资料（D1~D10 共 10 份） | — |
| Step1 | 盘点资料清单，标注解析状态 | §1 |
| Step2 | 逐份打开资料，按自身章节结构逐条摘要（D1 LearningViewModel.kt 逐方法摘要为重点） | §2 |
| Step3 | 交叉比对不同资料，发现并记录矛盾（X1~X10） | §3 |
| Step4 | 逐项核验硬指标 | §4 |

```mermaid
flowchart LR
    S0[读取模板与资料] --> S1[盘点资料清单]
    S1 --> S2[逐份精读逐章节摘要]
    S2 --> S3[交叉比对记录冲突]
    S3 --> S4[硬指标自检]
```

### 整理原则

1. **逐份精读，不跨文档归并**：摘要按文档自身章节结构组织，不做跨文档的主题重组（那是下游的事）
2. **出处即章节号**：每条摘要标注 `D编号，§章节`，直接映射回原文位置（对 Kotlin 源码，章节=类/方法/逻辑块，附行号范围）
3. **冲突保留**：矛盾信息并列保留两个版本，不擅自裁决
4. **事实驱动**：以原始资料中的事实为准，不添加主观推断

---

## 附录 B：解析 Skill

- `kotlin`：Kotlin 源码文件（.kt），按类/方法/逻辑块/构造参数等结构解析
- `gradle`：Gradle 构建配置文件（.kts），按代码块结构解析
- `markdown`：Markdown 文档，按标题章节结构解析
