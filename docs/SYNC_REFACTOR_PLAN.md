# Nemo 同步系统重构方案

本文档用于交给 Claude Code 执行后续设计/实现。重点是基于当前代码链路审查后的方案，不依赖未经验证的猜测。

## 1. 背景与目标

当前 Nemo 的账号登录、基础资料初始化、学习数据同步之间存在耦合风险。用户反馈的问题包括：

- 新设备登录后 UI 卡在登录页，必须等待同步/初始化完成。
- 数据量变大后，同步耗时增长，且出现单词学习记录大量缺失。
- 语法数据相对完整，单词数据异常，说明同步链路可能存在依赖顺序、批处理、checkpoint 或写入失败处理问题。
- 当前项目不需要兼容历史同步数据，可以从零重构用户学习数据同步。
- Supabase 使用免费版，不应依赖长时间 Edge Function 或大事务。

重构目标：

- 登录认证不被资料初始化或用户数据恢复阻塞。
- 新手机登录后可进入 App，后台渐进恢复数据。
- 用户学习数据恢复必须可靠，不允许静默丢批次。
- 基础资料与用户状态的依赖顺序明确。
- 同步过程可观察、可重试、可恢复、可校验。

## 2. 当前代码审查结论

### 2.1 登录链路

当前登录链路：

```text
AuthViewModel.login()
  -> LoginUseCase.invoke()
  -> AuthRepositoryImpl.login()
```

相关文件：

- `feature/user/src/main/java/com/jian/nemo/feature/user/AuthViewModel.kt`
- `core/domain/src/main/java/com/jian/nemo/core/domain/usecase/auth/LoginUseCase.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/repository/AuthRepositoryImpl.kt`

关键问题：

`AuthRepositoryImpl.login()` 登录成功后会调用：

```kotlin
dataSeedService.ensureDataSeeded()
```

这意味着登录成功返回前，仍可能执行基础资料检查/导入/修复逻辑。该逻辑不应位于登录认证路径中。

### 2.2 启动初始化链路

当前 `MainActivity.onCreate()` 中会启动：

```kotlin
applicationScope.launch {
    databaseInitializer.initialize()
}
```

`DatabaseInitializer.initialize()` 会调用：

```kotlin
syncManager.performDictionarySync()
```

相关文件：

- `app/src/main/java/com/jian/nemo/MainActivity.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/util/DatabaseInitializer.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/manager/DictionarySyncManager.kt`

结论：

启动后会触发基础资料同步检查。这个过程虽然在 IO scope 中，但它和其他初始化入口并存，整体编排不集中。

### 2.3 数据库打开回调链路

`NemoDatabaseCallback.onOpen()` 每次数据库打开后都会执行：

```kotlin
applicationScope.launch {
    dataSeedService.ensureDataSeeded()
}
```

相关文件：

- `core/data/src/main/java/com/jian/nemo/core/data/local/NemoDatabaseCallback.kt`

结论：

基础资料初始化目前至少有这些入口：

- 登录成功后 `AuthRepositoryImpl.login()`
- App 启动后 `DatabaseInitializer.initialize()`
- Room 数据库打开后 `NemoDatabaseCallback.onOpen()`

后续必须收敛为单一编排器，否则同步阶段、状态和失败处理会继续分散。

### 2.4 基础资料同步链路

当前基础资料同步链路：

```text
DictionarySyncManagerImpl.performDictionarySync()
  -> ContentRepositoryImpl.getRemoteContentVersion()
  -> ContentRepositoryImpl.fetchAllRemoteWords() / fetchAllRemoteGrammars()
  -> ContentRepositoryImpl.fetchWordsModifiedSince() / fetchGrammarsModifiedSince()
  -> ContentUpdateApplierImpl.applyAllWords() / applyAllGrammars()
```

相关文件：

- `core/data/src/main/java/com/jian/nemo/core/data/manager/DictionarySyncManager.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/repository/ContentRepositoryImpl.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/repository/ContentUpdateApplierImpl.kt`

已确认风险：

1. 增量同步使用远端 `updated_at` 时间戳，并通过 `+1ms` 推进 timestamp。这比单纯 timestamp 好一点，但仍不如服务端单调递增版本号可靠。

2. `ContentUpdateApplierImpl.applyAllWords()` / `applyAllGrammars()` 内部捕获异常但不向上抛：

```kotlin
catch (e: Exception) {
    Log.e(TAG, "applyAllWords failed", e)
}
```

这会导致上层可能继续推进同步时间戳或版本，形成“写入失败但 checkpoint 已推进”的风险。

3. `applyAllWords()` 分批写入，但没有显式事务包裹整个批次和 checkpoint 更新。

### 2.5 用户学习状态依赖基础资料

本地学习状态表依赖基础资料表：

- `word_study_states.word_id` 外键依赖 `words.id`
- `grammar_study_states.grammar_id` 外键依赖 `grammars.id`

相关文件：

- `core/data/src/main/java/com/jian/nemo/core/data/local/entity/WordStudyStateEntity.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/local/entity/GrammarStudyStateEntity.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/local/entity/WordEntity.kt`
- `core/data/src/main/java/com/jian/nemo/core/data/local/entity/GrammarEntity.kt`

结论：

新设备恢复用户学习状态前，必须保证对应的 `words` / `grammars` 基础资料已存在。否则可能出现：

- 外键失败。
- 状态写入被跳过。
- 状态写入后列表查询不到。
- 后续基础资料刷新时造成覆盖或下架状态异常。

### 2.6 旧同步痕迹不构成可靠协议

当前实体有：

- `last_modified_time`
- `is_deleted`
- `deleted_time`

迁移中也提到：

- `sync_status`
- `sync_audit_logs`

相关文件：

- `core/data/src/main/java/com/jian/nemo/core/data/local/migration/Migration_23_24.kt`

但当前没有看到完整的：

- outbox
- mutation ack
- upload checkpoint
- download checkpoint
- failed batch retry
- server version cursor
- count verification
- staging apply

结论：

后续不能继续在旧同步痕迹上修修补补，应从零设计用户学习数据同步协议。

## 3. 推荐架构

推荐采用：

```text
Room 本地主库
  + SyncOrchestrator 同步编排器
  + Local Outbox 本地变更队列
  + Supabase 云端状态快照
  + Server Version 增量游标
  + Checkpoint / Audit / Verification
```

核心原则：

- UI 永远优先读本地 Room。
- 登录只负责认证，不负责完整同步。
- 基础资料先就绪，用户学习状态后应用。
- 用户数据同步必须可重试、幂等、可恢复。
- 不使用长时间 Edge Function 作为主同步流程。
- 不使用 offset 作为可靠增量分页依据。

## 4. 新状态机

新增 `SyncOrchestrator`，统一管理同步状态：

```text
AUTH_ONLY
  -> APP_READY
  -> CONTENT_CHECK
  -> CONTENT_SYNCING
  -> CONTENT_READY
  -> USER_RESTORE
  -> USER_SYNCING
  -> SYNCED
```

异常状态：

```text
DEGRADED
FAILED
PAUSED_OFFLINE
NEEDS_REPAIR
```

状态含义：

- `AUTH_ONLY`：只完成 Supabase Auth。
- `APP_READY`：用户已进入 App，本地 UI 可用。
- `CONTENT_CHECK`：检查本地 words / grammars 是否存在且版本满足。
- `CONTENT_SYNCING`：同步基础资料。
- `CONTENT_READY`：基础资料可用。
- `USER_RESTORE`：新设备首次恢复用户学习状态。
- `USER_SYNCING`：常规双向增量同步。
- `SYNCED`：同步完成。
- `DEGRADED`：部分失败但 App 可继续用。
- `NEEDS_REPAIR`：数量校验失败，需要补偿修复。

## 5. 新手机登录流程

目标体验：

```text
登录账号
  -> Auth 成功
  -> 立即进入主界面
  -> 后台准备基础资料
  -> 后台恢复用户学习状态
  -> 分批显示恢复进度
  -> 完成后显示已同步
```

详细流程：

1. `AuthRepositoryImpl.login()` 只做 Supabase Auth 和本地 User 缓存。
2. 登录成功后 UI 立即设置 `isLoggedIn = true`。
3. `SyncOrchestrator` 在后台启动。
4. 检查 `words` / `grammars`：
   - 如果本地存在且版本满足，进入用户数据恢复。
   - 如果不存在或版本过旧，先同步基础资料。
5. 基础资料 ready 后恢复用户学习状态：
   - `word_study_states`
   - `grammar_study_states`
6. 每批恢复后更新 UI 可观察进度：
   - 单词 400 / 2300
   - 语法 100 / 350
   - 最后同步时间
7. 全部完成后做数量校验。

重要规则：

基础资料可以阻塞“用户学习状态应用”，但不能阻塞“登录进入 App”。

## 6. 本地表设计

### 6.1 sync_outbox

用于记录本地尚未成功上传的用户变更。

字段建议：

```sql
CREATE TABLE sync_outbox (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    local_version INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    next_retry_at INTEGER NOT NULL DEFAULT 0
);
```

说明：

- `id` 使用 UUID，即 mutation_id。
- `entity_type` 示例：`word_state`、`grammar_state`。
- `operation` 示例：`upsert`、`delete`。
- 本地业务表写入和 outbox 写入必须在同一个 Room transaction 中完成。

### 6.2 sync_checkpoint

用于记录每个方向、每个实体类型的同步进度。

```sql
CREATE TABLE sync_checkpoint (
    user_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    direction TEXT NOT NULL,
    server_version INTEGER NOT NULL DEFAULT 0,
    updated_at_cursor TEXT,
    id_cursor TEXT,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, entity_type, direction)
);
```

推荐优先使用 `server_version`，不推荐单独依赖时间戳。

### 6.3 sync_audit_logs

用于记录批次级结果，方便诊断“丢了哪一批”。

```sql
CREATE TABLE sync_audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    user_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    direction TEXT NOT NULL,
    batch_id TEXT NOT NULL,
    record_count INTEGER NOT NULL,
    success_count INTEGER NOT NULL,
    status TEXT NOT NULL,
    checkpoint_before TEXT,
    checkpoint_after TEXT,
    error_message TEXT
);
```

### 6.4 pending_user_states

用于暂存依赖基础资料但当前无法应用的数据。

```sql
CREATE TABLE pending_user_states (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    server_version INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
```

适用场景：

- 云端用户状态已拉到。
- 本地缺少对应 `word_id` 或 `grammar_id`。
- 不丢弃，先暂存。
- 基础资料补齐后再 apply。

## 7. Supabase 表设计

### 7.1 user_word_states

```sql
CREATE TABLE user_word_states (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    word_id INTEGER NOT NULL,
    payload JSONB NOT NULL,
    server_version BIGINT NOT NULL,
    mutation_id UUID NOT NULL,
    client_id TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, word_id)
);
```

索引：

```sql
CREATE INDEX idx_user_word_states_user_version
ON user_word_states(user_id, server_version);

CREATE UNIQUE INDEX idx_user_word_states_mutation
ON user_word_states(user_id, mutation_id);
```

### 7.2 user_grammar_states

结构与 `user_word_states` 类似：

```sql
CREATE TABLE user_grammar_states (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    grammar_id INTEGER NOT NULL,
    payload JSONB NOT NULL,
    server_version BIGINT NOT NULL,
    mutation_id UUID NOT NULL,
    client_id TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, grammar_id)
);
```

索引：

```sql
CREATE INDEX idx_user_grammar_states_user_version
ON user_grammar_states(user_id, server_version);

CREATE UNIQUE INDEX idx_user_grammar_states_mutation
ON user_grammar_states(user_id, mutation_id);
```

### 7.3 sync_versions

用于给用户数据生成单调递增版本。

```sql
CREATE TABLE sync_versions (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    current_version BIGINT NOT NULL DEFAULT 0
);
```

如果不使用 RPC，也可以短期使用 `updated_at + id` 复合游标，但推荐最终使用 `server_version`。

### 7.4 RLS

所有用户同步表必须启用 RLS：

```sql
ALTER TABLE user_word_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_grammar_states ENABLE ROW LEVEL SECURITY;
```

策略：

```sql
CREATE POLICY "Users can read own word states"
ON user_word_states
FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can write own word states"
ON user_word_states
FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own word states"
ON user_word_states
FOR UPDATE
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);
```

`user_grammar_states` 同理。

## 8. 上传协议

本地用户操作流程：

```text
用户学习/收藏/跳过
  -> Room transaction
     -> 写 word_study_states 或 grammar_study_states
     -> 写 sync_outbox
  -> UI 立即更新
```

后台上传流程：

```text
读取 sync_outbox status=PENDING
  -> 按 entity_type 分批，每批 100-300 条
  -> 上传到 Supabase
  -> 云端按 mutation_id 幂等 upsert
  -> 成功后本地标记 outbox 为 SYNCED
  -> 失败则保留并增加 retry_count
```

关键要求：

- 不允许上传失败后删除 outbox。
- 不允许无 ack 标记成功。
- 不允许同一批部分成功但整体当作全部成功。
- 每批必须写 audit log。

## 9. 下载/恢复协议

新设备或常规拉取流程：

```text
读取本地 checkpoint
  -> 从 Supabase 拉 server_version > checkpoint 的数据
  -> 每批 100-300 条
  -> Room transaction 应用
  -> 成功后更新 checkpoint
  -> 写 audit log
```

应用用户状态前必须检查依赖：

```text
word_state.word_id 是否存在于 words.id
grammar_state.grammar_id 是否存在于 grammars.id
```

如果不存在：

```text
写入 pending_user_states
不推进该实体的最终完成状态
触发基础资料修复/补拉
```

## 10. 基础资料同步调整

短期修复：

- `ContentUpdateApplierImpl.applyAllWords()` / `applyAllGrammars()` 不应吞异常。
- 写入失败必须向上返回失败。
- `DictionarySyncManagerImpl` 只有在 apply 成功后才能推进 timestamp/version。
- `DatabaseInitializer` 只负责触发编排器，不直接决定词库同步。

长期建议：

- 基础资料也使用 `content_version` + 分阶段状态。
- 优先保证 App 内置 assets 能提供基础资料兜底。
- 云端基础资料更新不应和用户学习状态恢复混在同一个同步任务里。

## 11. UI 状态建议

设置页或同步诊断页显示：

- 当前同步状态。
- 基础资料状态。
- 用户数据恢复进度。
- 待上传数量。
- 待下载数量。
- 最近成功同步时间。
- 最近失败原因。
- 是否存在 pending 状态。
- 本地/云端数量校验结果。

新手机首次登录建议显示：

```text
正在准备学习资料
正在恢复学习进度：单词 400 / 2300，语法 120 / 350
网络中断，将稍后继续
数据恢复完成
```

## 12. 分阶段实施计划

### 阶段 1：登录解耦与同步编排

目标：

- 登录成功不再执行 `ensureDataSeeded()`。
- 新增 `SyncOrchestrator` 状态机。
- `MainActivity` / `NemoDatabaseCallback` 不直接触发重任务。

改动点：

- `AuthRepositoryImpl.login()`
- `AuthRepositoryImpl.register()`
- `NemoDatabaseCallback`
- `DatabaseInitializer`

验收：

- 新设备登录后 1-2 秒内进入主界面。
- 网络慢时登录页不因资料同步卡住。

### 阶段 2：基础资料 ready 检查

目标：

- 明确 `CONTENT_READY` 判断。
- 修复 `ContentUpdateApplierImpl` 吞异常问题。
- 只有基础资料 ready 后才能应用用户状态。

验收：

- 缺少 `words` / `grammars` 时，用户学习状态不会被直接丢弃。
- 基础资料写入失败不会推进同步 checkpoint。

### 阶段 3：单词/语法用户状态同步

目标：

- 新增本地 `sync_outbox`、`sync_checkpoint`、`sync_audit_logs`。
- 新增云端 `user_word_states`、`user_grammar_states`。
- 实现分批上传、分批下载、幂等 upsert。

验收：

- 2000+ 单词状态可完整上传。
- 新设备可完整恢复。
- 中断后可断点续传。
- 重试不会重复写入。

### 阶段 4：数量校验与修复

目标：

- 同步完成后进行本地/云端 count 校验。
- 如果缺失，进入 `NEEDS_REPAIR`。
- 支持按 ID 差异补拉。

验收：

- 云端 2300 条，本地恢复后必须为 2300 条。
- 如果只恢复 1700 条，不能显示“已同步”，必须显示可修复异常。

### 阶段 5：扩展其他用户数据

同步范围扩展到：

- wrong answers
- grammar wrong answers
- test records
- study records
- favorite questions
- app settings，可选

## 13. 验收测试场景

必须覆盖：

1. 新手机空库登录，云端有 2000+ 单词状态、几百条语法状态。
2. 基础资料未就绪时登录。
3. 基础资料同步到一半失败。
4. 用户状态恢复到一半断网。
5. App 被杀后重启继续恢复。
6. 同一批上传重复请求。
7. 同一个 word_id 多设备修改。
8. 本地有 outbox 未上传时退出登录。
9. 云端 count 与本地 count 不一致。
10. Supabase 请求失败、限流或超时。

## 14. 不建议做的事

- 不要登录后全量同步完成才进入主界面。
- 不要继续让多个入口各自触发 seed/sync。
- 不要使用 offset 作为长期可靠分页方案。
- 不要写入失败后仍推进 checkpoint。
- 不要只靠 `last_modified_time` 判断用户数据冲突。
- 不要物理删除用户学习状态，使用 tombstone。
- 不要把大同步塞进长时间 Edge Function。

## 15. 最终结论

推荐重构方向：

```text
登录解耦
  + 单一 SyncOrchestrator
  + 基础资料先决条件
  + 用户状态 outbox
  + server_version 增量游标
  + checkpoint
  + audit log
  + pending staging
  + count verification
```

这不是简单的“离线优先 + 后台同步”，而是一套可靠同步协议。它针对当前项目已确认的风险点：

- 登录链路仍执行资料初始化。
- 基础资料同步入口分散。
- 用户状态依赖 words / grammars。
- 写入异常可能被吞掉。
- 旧同步状态不构成完整可靠闭环。

优先实现阶段 1-4，即可先解决新手机登录卡顿和单词学习记录丢失问题。
