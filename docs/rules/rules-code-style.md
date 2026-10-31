# Nemo 代码风格、注释与命名纪律（Rules）

> **适用对象**：本仓库的所有 AI 与人工开发工作（代码生成、平移、重构、修复、代码审查）  
> **配套文档**：`.agent/rules/nemorules.md`（行为硬约束）· `docs/rules/rules-ui-fidelity.md`（UI 保真铁律）· `docs/rules/rules-engineering.md`（Android 15 原生工程底线纪律）

---

## 0. 核心总纲

> **代码释“怎么做”（How），类型释“是什么”（What），注释仅释“为什么”（Why）。**  
> **命名即架构：清晰精准的命名能消除 80% 的多余注释。**  
> **代码库是可长期演进的生产资产，不是迁移备忘录。严禁把过程中的元说明倾倒进源代码。**

---

## 1. 绝对禁止的注释红线（违规即阻断）

在生成的源代码（`.kt` / `.kts` / `.xml` 等）中，**严禁**出现以下任何类型的注释：

| 违规类型 | 典型特征与错误示例 | 正确处置方式 |
|---|---|---|
| **溯源型元注释** | `// 按照原项目设计设置 16.dp`<br>`// 搬运自旧文件第 42 行`<br>`// 与旧工程保持一致`<br>`// 旧版这里用了 tween(200)` | **严禁留在源码中**。<br>溯源信息一律记录在 Git Commit Message 的 `source:` 标签，或登记在任务/待办台账中 |
| **代码复述型废话** | `// 设置内边距为 12dp`<br>`// 声明一个计数变量`<br>`// 判断列表是否为空`<br>`// 返回处理后的数据` | **直接删除**。良好的命名与清晰的代码结构自明其意，禁止无意义的文字翻译 |
| **大段废弃代码残留** | `// val oldPadding = 8.dp`<br>`// fun deprecatedMethod() { ... }` (整段注释掉的代码) | **直接删除**。旧逻辑由 Git 历史负责记录，新工程严禁留存僵尸注释代码 |
| **防御性/对话型口水** | `// AI 生成：根据用户指令处理`<br>`// 这里可能需要优化，暂且这样写`<br>`// 修复了一个可能的 bug` | **严禁输出**。AI 仅生成干净的生产级代码，不得在代码中自言自语或推卸责任 |
| **无意义模板头** | `/** Created by Android Studio on 2026/10/26 **/` | **彻底禁止**，创建新文件时清理所有 IDE 默认注释模板 |

---

## 2. 合规与倡导的注释规范

代码中的注释必须**少而精**，仅在具备实际技术价值时出现：

### 2.1 公共 API 与顶层 Composable（使用标准 KDoc）

仅对**跨模块共享的公共 API、核心数据模型、供多处引用的公共 Composable** 编写简洁的 KDoc：

```kotlin
/**
 * 单词学习卡片组件。
 *
 * @param wordItem 要展示的词汇实体，包含拼写、音标与释义元数据。
 * @param isSelected 当前卡片是否处于选中态。
 * @param onToggleSelect 点击选择触发的回调。
 * @param modifier 外部布局修饰符。
 */
@Composable
fun WordCard(
    wordItem: WordItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

- **说明**：模块私有的内部辅助函数、局部 Composable，若命名清晰，**无须强行添加 KDoc**。

### 2.2 复杂算法与非常规业务逻辑（解释 Why）

当且仅当出现非常规算法、密集状态转换、特殊数学计算时，简短说明业务原理：

```kotlin
// 计算两倍阻尼衰减系数，防止高帧率滑动时动量过度累积导致越界
val decayFactor = calculateDecay(velocity, currentOffset)
```

### 2.3 硬件/平台特殊限制规避（Workaround 说明）

当代码为了规避底层系统驱动 Bug、特定 OEM 厂商硬件行为或非常规系统限制而采用特殊写法时，必须清晰注释：

```kotlin
// 部分折叠屏展开态下 WindowMetrics 触发异步重算的临时防抖保护
if (isDisplayMetricsChanging) {
    scheduleLayoutRetry()
}
```

### 2.4 待办与缺陷标记（规范化 TODO）

若因依赖未就绪或需暂缓处理（必须在项目任务台账中登记）：

```kotlin
// TODO(Task-104): 待 Settings 模块持久化架构就绪后接入 DataStore
```

---

## 3. 溯源信息的合法落脚点

为了在代码审查和后续对拍时能够追根溯源，同时保证源码绝对纯净，溯源信息**必须且只能**存在于以下位置：

### 3.1 Git Commit Message（强约束）

所有功能重构或迁移相关提交，按既定规范在提交说明末尾标注来源：

```text
feat(learning): 重构学习卡片组件

- 优化 WordCard 核心渲染与状态绑定逻辑
- 文本收编至 strings.xml，色值提取至 Color.kt

source: components/WordCard.kt
```

### 3.2 发现台账

遇到特殊实现、存疑色值、待确认逻辑，统一记入项目待办或设计台账，禁止在代码中写口水注释。

---

## 4. 全局统一命名规约（Naming Conventions）

### 4.1 文件与包命名
- **单类/单单例文件**：与主声明完全同名，`PascalCase.kt`（如 `WordRepository.kt`）。
- **页面级 Screen 文件**：以 `Screen.kt` 结尾（如 `LearningScreen.kt`、`SettingsScreen.kt`）。
- **独立组件 Composable 文件**：名词大驼峰（如 `WordCard.kt`、`CelebrationConfetti.kt`）。
- **扩展函数文件**：必须以 `{被扩展类}Ext.kt` 命名（如 `ContextExt.kt`、`ModifierExt.kt`），**严禁出现 `Util.kt`、`Common.kt` 这类垃圾桶文件**。
- **单元测试文件**：位于 `src/test/`，命名为 `{被测类}Test.kt`（如 `LearningViewModelTest.kt`）。
- **UI/仪器化测试文件**：位于 `src/androidTest/`，命名为 `{被测组件}UiTest.kt`。
- **包名**：全小写，禁止下划线与大写字母（如 `com.jian.nemo.feature.learning.presentation`）。

### 4.2 Compose UI 函数与参数签名纪律
- **Composable 函数**：
  - 无返回值组件：必须为**名词/名词短语大驼峰**（如 `WordList()`、`FilterBar()`，严禁 `renderList()`、`showFilter()`）。
  - 有返回值工厂函数：小驼峰，以 **`remember`** 开头（如 `rememberWordListState()`）。
- **事件回调参数**：
  - 一律使用 **`on + 动作/过去分词`**（如 `onClick: () -> Unit`、`onItemSelected: (Long) -> Unit`、`onDismissRequest: () -> Unit`）。
  - 严禁 `handleClick`、`clickCallback`、`deleteAction`。
- **Composable 参数标准顺序**（从上至下严格排布）：
  1. **必选数据参数**（如 `item: WordItem`）
  2. **`modifier: Modifier = Modifier`**（必须是**第一个带默认值的可选参数**）
  3. **可选样式与状态配置**（如 `isSelected: Boolean = false`）
  4. **事件回调参数**（如 `onClick: () -> Unit`）
  5. **尾随 Slot 插槽**（如 `content: @Composable () -> Unit`，必须排在末尾以支持尾随 lambda）

### 4.3 业务与架构函数命名
- **动作与业务执行**：动词或动宾短语，小驼峰（如 `fetchWordItems()`、`deleteSelectedWords()`）。
- **布尔判断函数**：必须以 **`is / has / should / can / contains`** 开头（如 `hasPermission(): Boolean`、`canStudy(): Boolean`，严禁 `checkPermission(): Boolean`）。
- **类型转换函数**：
  - 轻量无开销转换：`to{Target}()` 或 `as{Target}()`（如 `Word.toUiModel()`、`cursor.toWordItem()`）。
  - 重度计算/转换：`convertTo{Target}()`。
- **数据流与挂起函数**：
  - 返回 Flow/StateFlow：以数据流或观察语义命名，如 `observeWords(): Flow<...>`。
  - 挂起函数 (`suspend fun`)：使用常规动词（如 `fetchWords()`），**严禁在方法名尾部加 `Async`**。

### 4.4 变量、状态流与常量命名
- **普通变量与字段**：小驼峰，描述性名词（如 `selectedIds`、`currentWordIndex`）。
- **布尔变量与字段**：必须带状态前缀（如 `isLoading`、`hasSelection`、`isVisible`，严禁 `loading`、`select`）。
- **MVI 状态流命名配对**：
  - 私有可变流：`private val _uiState = MutableStateFlow(...)`
  - 公开只读流：`val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()`
- **常量命名**：
  - 编译期常量 (`const val`)：全大写下划线 `SCREAMING_SNAKE_CASE`（如 `MAX_WORD_COUNT = 100`）。
  - 顶级只读配置/单例对象：大驼峰（如 `NemoTypography`、`DefaultMotionSpec`）。
  - 枚举项（Enum Entries）：全大写下划线（如 `AppTheme.DARK`、`AppTheme.FOLLOW_SYSTEM`）。

### 4.5 类型、数据模型与架构分层
- **类名与接口名**：名词或形容词，大驼峰。
  - 接口名：**严禁加 `I` 前缀**（使用 `WordRepository`，严禁 `IWordRepository`）。
  - 默认实现类：以特征前缀命名（如 `DefaultWordRepository`、`OfflineFirstWordRepository`）。
- **分层实体模型**：
  - 数据库持久化实体：`{Name}Entity`（如 `WordEntity`）。
  - 领域核心模型：`{Name}`（如 `WordItem`、`WordBook`）。
  - UI 表现模型（仅在有复杂差异时使用）：`{Name}UiModel`（如 `WordCardUiModel`）。
- **MVI 状态机体系**：
  - 状态容器：`{Feature}UiState`（只读不可变 `data class`，字段必带默认值）。
  - 用户意图/动作：`{Feature}UiIntent`（`sealed interface`，如 `LearningUiIntent.SelectOption`）。
  - 一次性副作用：`{Feature}UiEffect`（`sealed interface`，用于 Navigation / Toast / Snackbar）。

### 4.6 Navigation 类型安全路由规约
- **无参路由**：以 `Route` 结尾的 `data object`（如 `data object LearningRoute`、`data object SettingsRoute`，严禁与 Composable 重名）。
- **有参路由**：以 `Route` 结尾的 `data class`（如 `@Serializable data class WordDetailRoute(val wordId: Long)`）。
- **导航扩展函数**：
  - 页面注册：`fun NavGraphBuilder.learningScreen(...)`
  - 跳转调用：`fun NavController.navigateToWordDetail(id: Long)`

### 4.7 Room 数据库与 DAO 规约
- **表名（`@Entity(tableName = "...")`）**：强制使用 **小写下划线复数（snake_case）**（如 `words`、`word_books`、`learning_records`）。
- **主键字段名**：`id` 或 `{entity}_id`。
- **DAO 接口与方法命名**：
  - DAO 接口名：`{Entity}Dao`（如 `WordDao`）。
  - 观察流（Flow）：`observe{Entities}()`（如 `observeAllWords(): Flow<List<WordEntity>>`）。
  - 单次挂起查询：`get{Entity}ById(id: Long)`、`find{Entities}ByRange(...)`。
  - 写入与删除：`insert{Entities}()`、`upsert{Entities}()`、`delete{Entities}()`。

### 4.8 数据源层（DataSource）规约
- 本地数据库/缓存：`{Entity}LocalDataSource`（如 `WordLocalDataSource`）。
- 系统服务/框架数据源：以实际能力命名（如 `PreferencesDataSource`、`AssetDataSource`）。
- **禁止**出现语义模糊的 `DataManager`、`DataHelper`。

### 4.9 依赖注入（Hilt / DI）规约
- **模块（Module）**：`{功能/分层}Module`（如 `DatabaseModule`、`RepositoryModule`、`DispatchersModule`）。
- **工厂与绑定方法**：
  - 实例提供：`provide{Type}()`（如 `provideWordDatabase()`）。
  - 接口绑定：`bind{Interface}()`（如 `bindWordRepository()`）。
- **自定义限定符（Qualifiers）**：大驼峰动名词短语，如 `@IoDispatcher`、`@DefaultDispatcher`、`@ApplicationScope`。

### 4.10 测试命名规约
- **测试方法名**：使用 Kotlin 反引号清晰表述行为，或规范的三段式结构：
  - 反引号规范：``fun `deleteWords should emit success when database available`()``
  - 三段式规范：`fun deleteWords_whenDatabaseAvailable_emitsSuccess()`
- **测试替身（Test Doubles）**：
  - 内存假实现：`Fake{Interface}`（如 `FakeWordRepository`、`FakeLocalDataSource`）。

### 4.11 异常模型与 UI 测试标签
- **业务异常**：以 `Exception` 结尾（如 `WordNotFoundException`、`InsufficientStorageException`）。
- **结果包装模型**：统一密封接口 `NemoResult<T>`，子类为 `Success`、`Error`、`Loading`。
- **自动化测试标签（TestTag）**：全小写下划线 `{feature}_{component}_{identifier}`（如 `Modifier.testTag("learning_word_card_$id")`）。

### 4.12 Android 资源命名规约
- **Drawable / Vector 图标**：`{模块前缀}_ic_{名称}.xml`（如 `feature_learning_ic_play.xml`）。
- **背景 / Shape**：`{模块前缀}_bg_{名称}.xml`（如 `core_designsystem_bg_card.xml`）。
- **字符串 Key**：`{模块前缀}_{名称}`（如 `learning_btn_continue`）。
- **字体文件**：`{family}_{weight}.ttf`（如 `inter_bold.ttf`）。

---

## 5. 正反对比速查表

| 场景 | ❌ 违规写法（垃圾元注释 / 混乱命名） | ✅ 合规写法（干净纯粹 / 严谨命名） |
|---|---|---|
| **组件声明** | `// 按照原项目定义卡片`<br>`@Composable fun renderCard(data: Word, modifier: Modifier = Modifier, onClick: () -> Unit)` | `@Composable fun WordCard(wordItem: WordItem, modifier: Modifier = Modifier, onClick: () -> Unit)` |
| **事件命名** | `Button(onClick = { handleClick() })`<br>`fun setSelectCallback(callback: () -> Unit)` | `Button(onClick = { onWordClick() })`<br>`fun setOnSelectListener(onSelect: () -> Unit)` |
| **状态持有** | `var loading = false`<br>`val dataList = MutableStateFlow<List<Item>>(emptyList())` | `var isLoading by remember { mutableStateOf(false) }`<br>`private val _uiState = MutableStateFlow(LearningUiState())`<br>`val uiState = _uiState.asStateFlow()` |
| **路由定义** | `data object LearningPage`<br>`data class Detail(val id: Long)` | `data object LearningRoute`<br>`data class WordDetailRoute(val wordId: Long)` |
| **数据库表** | `@Entity(tableName = "WordTable")`<br>`@Entity(tableName = "item")` | `@Entity(tableName = "words")`<br>`@Entity(tableName = "learning_records")` |
| **挂起函数** | `suspend fun getWordsAsync(): List<Word>` | `suspend fun fetchWords(): List<Word>` |
| **工具类文件** | 新建 `Util.kt`、`CommonHelper.kt`，塞满各类扩展 | 拆分为 `ContextExt.kt`、`ModifierExt.kt`，按类型收敛 |

---

> **给 AI 的最终纪律**：写代码之前对照本规范自检——“文件放对地方了吗？类和函数命名自明其意了吗？参数顺序合规了吗？代码里是否留下了任何口水注释？”凡不符合规范的代码，一律拒绝提交。
