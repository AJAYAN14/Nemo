package com.jian.nemo.core.data.cache

import com.jian.nemo.core.common.di.ApplicationScope
import com.jian.nemo.core.common.util.GrammarSearchUtils
import com.jian.nemo.core.data.local.dao.GrammarDao
import com.jian.nemo.core.data.local.dao.WordDao
import com.jian.nemo.core.data.local.dao.WordStudyStateDao
import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import com.jian.nemo.core.data.local.entity.relations.GrammarWithUsages
import com.jian.nemo.core.data.mapper.GrammarMapper.toDomainModels
import com.jian.nemo.core.data.mapper.WordMapper
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.GrammarListSnapshot
import com.jian.nemo.core.domain.repository.GrammarPreloadItem
import com.jian.nemo.core.domain.repository.LibraryPreloadCache
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordListSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 词库列表应用级单例预加载缓存实现
 *
 * 在后台长驻订阅活跃单词、活跃语法与图标风格设置流，首帧发射后采用轻量防抖吸收批量写入，
 * 使列表页在进入瞬间即可从内存快照直接构建首帧。
 */
@Singleton
class DefaultLibraryPreloadCache internal constructor(
    private val wordDao: WordDao,
    private val wordStudyStateDao: WordStudyStateDao,
    private val grammarDao: GrammarDao,
    private val settingsRepository: SettingsRepository,
    private val dictionarySyncManager: DictionarySyncManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS
) : LibraryPreloadCache {

    @Inject
    constructor(
        wordDao: WordDao,
        wordStudyStateDao: WordStudyStateDao,
        grammarDao: GrammarDao,
        settingsRepository: SettingsRepository,
        dictionarySyncManager: DictionarySyncManager,
        @ApplicationScope applicationScope: CoroutineScope
    ) : this(
        wordDao = wordDao,
        wordStudyStateDao = wordStudyStateDao,
        grammarDao = grammarDao,
        settingsRepository = settingsRepository,
        dictionarySyncManager = dictionarySyncManager,
        applicationScope = applicationScope,
        ioDispatcher = Dispatchers.IO,
        defaultDispatcher = Dispatchers.Default
    )

    private val _wordListSnapshot = MutableStateFlow(WordListSnapshot())
    override val wordListSnapshot: StateFlow<WordListSnapshot> = _wordListSnapshot.asStateFlow()

    private val _grammarListSnapshot = MutableStateFlow(GrammarListSnapshot())
    override val grammarListSnapshot: StateFlow<GrammarListSnapshot> = _grammarListSnapshot.asStateFlow()

    private val _settingIconStyleState = MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)
    override val settingIconStyleState: StateFlow<SettingIconStyle> = _settingIconStyleState.asStateFlow()

    private val isCollectingStarted = AtomicBoolean(false)
    private val isWarmUpScheduled = AtomicBoolean(false)

    override fun warmUp() {
        if (isCollectingStarted.get()) return
        if (!isWarmUpScheduled.compareAndSet(false, true)) return

        applicationScope.launch(ioDispatcher) {
            try {
                if (!dictionarySyncManager.isDictionaryReady()) {
                    dictionarySyncManager.isDictionaryReadyFlow.first { it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 就绪检查异常时不阻断后续保底订阅
            }
            startCollection()
        }
    }

    override fun ensureStartedNow() {
        startCollection()
    }

    private fun startCollection() {
        if (!isCollectingStarted.compareAndSet(false, true)) return

        launchWordCollection()
        launchGrammarCollection()
        launchIconStyleCollection()
    }

    private fun launchWordCollection() {
        applicationScope.launch(defaultDispatcher) {
            wordDao.observeAllActiveWords()
                .debounceAfterFirst(debounceMillis)
                .map { entities ->
                    val statesById = if (entities.isEmpty()) {
                        emptyMap()
                    } else {
                        wordStudyStateDao.getAllSync().associateBy { it.wordId }
                    }
                    buildWordSnapshot(entities, statesById)
                }
                .flowOn(ioDispatcher)
                .catch {
                    if (!_wordListSnapshot.value.isReady) {
                        _wordListSnapshot.value = WordListSnapshot(isReady = true)
                    }
                }
                .collect { snapshot ->
                    _wordListSnapshot.value = snapshot
                }
        }
    }

    private fun launchGrammarCollection() {
        applicationScope.launch(defaultDispatcher) {
            grammarDao.observeAllActiveGrammarsWithUsages()
                .debounceAfterFirst(debounceMillis)
                .map { relations ->
                    withContext(defaultDispatcher) {
                        buildGrammarSnapshot(relations)
                    }
                }
                .flowOn(ioDispatcher)
                .catch {
                    if (!_grammarListSnapshot.value.isReady) {
                        _grammarListSnapshot.value = GrammarListSnapshot(isReady = true)
                    }
                }
                .collect { snapshot ->
                    _grammarListSnapshot.value = snapshot
                }
        }
    }

    private fun launchIconStyleCollection() {
        applicationScope.launch(ioDispatcher) {
            settingsRepository.settingIconStyleFlow
                .distinctUntilChanged()
                .catch {
                    // 保留默认风格
                }
                .collect { style ->
                    _settingIconStyleState.value = style
                }
        }
    }

    private suspend fun buildWordSnapshot(
        entities: List<WordEntity>,
        statesById: Map<Int, WordStudyStateEntity>
    ): WordListSnapshot = withContext(defaultDispatcher) {
        val domainWords = entities.map { entity ->
            WordMapper.toDomainModel(entity, statesById[entity.id])
        }
        val grouped = groupByNormalizedLevel(domainWords) { it.level }
        WordListSnapshot(
            wordsByLevel = grouped.itemsByLevel,
            countsByLevel = grouped.countsByLevel,
            allWords = grouped.allItems,
            isReady = true
        )
    }

    private fun buildGrammarSnapshot(
        relations: List<GrammarWithUsages>
    ): GrammarListSnapshot {
        val domainGrammars = relations.toDomainModels()
        val preloadItems = domainGrammars.map { grammar ->
            GrammarPreloadItem(
                grammar = grammar,
                cleanedTitle = GrammarSearchUtils.cleanRubi(grammar.grammar),
                cleanedFirstExplanation = GrammarSearchUtils.cleanRubi(grammar.getFirstExplanation())
            )
        }
        val grouped = groupByNormalizedLevel(preloadItems) { it.grammar.grammarLevel }
        return GrammarListSnapshot(
            grammarsByLevel = grouped.itemsByLevel,
            countsByLevel = grouped.countsByLevel,
            allGrammars = grouped.allItems,
            isReady = true
        )
    }

    private inline fun <T> groupByNormalizedLevel(
        items: List<T>,
        levelSelector: (T) -> String
    ): GroupedSnapshotData<T> {
        val buckets = LinkedHashMap<String, MutableList<T>>(LEVEL_ORDER.size)
        LEVEL_ORDER.forEach { level -> buckets[level] = ArrayList() }

        items.forEach { item ->
            val normalizedLevel = levelSelector(item).uppercase()
            buckets[normalizedLevel]?.add(item)
        }

        val itemsByLevel = LinkedHashMap<String, List<T>>(LEVEL_ORDER.size)
        val countsByLevel = LinkedHashMap<String, Int>(LEVEL_ORDER.size)
        val allItems = ArrayList<T>(items.size)

        LEVEL_ORDER.forEach { level ->
            val bucket = buckets[level]
            if (!bucket.isNullOrEmpty()) {
                itemsByLevel[level] = bucket
                countsByLevel[level] = bucket.size
                allItems.addAll(bucket)
            }
        }

        return GroupedSnapshotData(
            itemsByLevel = itemsByLevel,
            countsByLevel = countsByLevel,
            allItems = allItems
        )
    }

    /**
     * 首次发射立即直出以缩短冷启动等待，后续频繁变更通过防抖合并计算
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> Flow<T>.debounceAfterFirst(waitMillis: Long): Flow<T> = channelFlow {
        var hasEmittedFirst = false
        collectLatest { value ->
            if (!hasEmittedFirst) {
                hasEmittedFirst = true
                send(value)
            } else {
                if (waitMillis > 0L) {
                    delay(waitMillis)
                }
                send(value)
            }
        }
    }

    private data class GroupedSnapshotData<T>(
        val itemsByLevel: Map<String, List<T>>,
        val countsByLevel: Map<String, Int>,
        val allItems: List<T>
    )

    companion object {
        private const val DEFAULT_DEBOUNCE_MILLIS = 200L
        private val LEVEL_ORDER = listOf("N1", "N2", "N3", "N4", "N5")
    }
}
