package com.jian.nemo.feature.settings

import com.jian.nemo.core.domain.model.AppUpdateConfig
import com.jian.nemo.core.domain.repository.ConfigRepository
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class AboutViewModelTest {

    @Test
    fun `命中缓存更新日志时不请求网络并直接展示`() {
        runBlocking {
            val cached = sampleConfig(versionName = "2.1.0", updateLog = "• 内存优化")
            val configRepo = FakeConfigRepository(cachedConfig = cached)
            val metadataSource = FakeAboutMetadataSource(
                loaded = AboutLocalMetadata("2.1.0", 21L, checkUpdateOnStartup = true)
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val viewModel = createViewModel(
                    configRepository = configRepo,
                    metadataSource = metadataSource,
                    scope = scope
                )
                val state = viewModel.uiState.value
                assertFalse(state.isLoadingLog)
                assertEquals(cached, state.updateConfig)
                assertNull(state.errorMessage)
                assertEquals(0, configRepo.getUpdateConfigCalls.size)
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun `未命中缓存时自动请求更新日志且支持手动强制刷新`() {
        runBlocking {
            val firstRemote = sampleConfig(versionName = "2.1.0", updateLog = "• 首次拉取")
            val refreshedRemote = sampleConfig(versionName = "2.2.0", updateLog = "• 强制刷新")
            val configRepo = FakeConfigRepository(
                cachedConfig = null,
                remoteHandler = { force -> if (force) refreshedRemote else firstRemote }
            )
            val metadataSource = FakeAboutMetadataSource(
                loaded = AboutLocalMetadata("2.1.0", 21L, checkUpdateOnStartup = false)
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val viewModel = createViewModel(
                    configRepository = configRepo,
                    metadataSource = metadataSource,
                    scope = scope
                )
                assertEquals(firstRemote, viewModel.uiState.value.updateConfig)
                assertEquals(listOf(false), configRepo.getUpdateConfigCalls)

                viewModel.fetchUpdateLog(forceRefresh = true)
                assertEquals(refreshedRemote, viewModel.uiState.value.updateConfig)
                assertFalse(viewModel.uiState.value.isLoadingLog)
                assertEquals(listOf(false, true), configRepo.getUpdateConfigCalls)
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun `日志接口返回空或抛异常时正确展示错误文本`() {
        runBlocking {
            val emptyRepo = FakeConfigRepository(cachedConfig = null, remoteHandler = { null })
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val emptyVm = createViewModel(
                    configRepository = emptyRepo,
                    metadataSource = FakeAboutMetadataSource(
                        loaded = AboutLocalMetadata("2.0.0", 20L, true)
                    ),
                    scope = scope
                )
                assertFalse(emptyVm.uiState.value.isLoadingLog)
                assertNull(emptyVm.uiState.value.updateConfig)
                assertEquals("no_log_str", emptyVm.uiState.value.errorMessage)

                val errorWithMsgRepo = FakeConfigRepository(
                    cachedConfig = null,
                    remoteHandler = { throw IllegalStateException("网络超时") }
                )
                val errorWithMsgVm = createViewModel(
                    configRepository = errorWithMsgRepo,
                    metadataSource = FakeAboutMetadataSource(
                        loaded = AboutLocalMetadata("2.0.0", 20L, true)
                    ),
                    scope = scope
                )
                assertEquals("网络超时", errorWithMsgVm.uiState.value.errorMessage)

                val errorNoMsgRepo = FakeConfigRepository(
                    cachedConfig = null,
                    remoteHandler = { throw IllegalStateException() }
                )
                val errorNoMsgVm = createViewModel(
                    configRepository = errorNoMsgRepo,
                    metadataSource = FakeAboutMetadataSource(
                        loaded = AboutLocalMetadata("2.0.0", 20L, true)
                    ),
                    scope = scope
                )
                assertEquals("fetch_failed_str", errorNoMsgVm.uiState.value.errorMessage)
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun `存在预热本地元数据时初始状态立即可用`() {
        runBlocking {
            val prewarmed = AboutLocalMetadata("3.0.1", 301L, checkUpdateOnStartup = false)
            val pendingRefresh = CompletableDeferred<AboutLocalMetadata>()
            val metadataSource = FakeAboutMetadataSource(
                cached = prewarmed,
                loadDeferred = pendingRefresh
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val viewModel = createViewModel(
                    configRepository = FakeConfigRepository(cachedConfig = sampleConfig()),
                    metadataSource = metadataSource,
                    scope = scope
                )
                val initialState = viewModel.uiState.value
                assertTrue(initialState.isMetadataReady)
                assertEquals("3.0.1", initialState.versionName)
                assertEquals(301L, initialState.versionCode)
                assertFalse(initialState.checkUpdateOnStartup)

                pendingRefresh.complete(prewarmed)
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun `异步元数据尚未加载完毕时用户先切换开关不会被后续返回覆盖`() {
        runBlocking {
            val loadDeferred = CompletableDeferred<AboutLocalMetadata>()
            val metadataSource = FakeAboutMetadataSource(
                cached = null,
                loadDeferred = loadDeferred
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val viewModel = createViewModel(
                    configRepository = FakeConfigRepository(cachedConfig = sampleConfig()),
                    metadataSource = metadataSource,
                    scope = scope
                )
                assertFalse(viewModel.uiState.value.isMetadataReady)

                viewModel.setCheckUpdateOnStartup(false)
                assertFalse(viewModel.uiState.value.checkUpdateOnStartup)
                assertEquals(listOf(false), metadataSource.savedStartupFlags)

                loadDeferred.complete(AboutLocalMetadata("2.5.0", 25L, checkUpdateOnStartup = true))

                val settled = viewModel.uiState.value
                assertTrue(settled.isMetadataReady)
                assertEquals("2.5.0", settled.versionName)
                assertEquals(25L, settled.versionCode)
                assertFalse(settled.checkUpdateOnStartup)
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun `settingIconStyle 状态流同步 SettingsRepository 的图标风格`() {
        runBlocking {
            val styleFlow = MutableStateFlow(SettingIconStyle.VIBRANT_SOLID)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val viewModel = createViewModel(
                    configRepository = FakeConfigRepository(cachedConfig = sampleConfig()),
                    metadataSource = FakeAboutMetadataSource(
                        loaded = AboutLocalMetadata("1.0.0", 1L, true)
                    ),
                    settingsStyleFlow = styleFlow,
                    scope = scope
                )
                val observed = mutableListOf<SettingIconStyle>()
                val collector = scope.launch {
                    viewModel.settingIconStyle.collect { observed.add(it) }
                }
                assertEquals(SettingIconStyle.VIBRANT_SOLID, viewModel.settingIconStyle.value)

                styleFlow.value = SettingIconStyle.SOFT_TRANSLUCENT
                assertEquals(SettingIconStyle.SOFT_TRANSLUCENT, viewModel.settingIconStyle.value)
                assertTrue(observed.contains(SettingIconStyle.SOFT_TRANSLUCENT))
                collector.cancel()
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun `切换开关后立即取消 ViewModel 作用域仍可靠写入持久化与内存缓存`() {
        runBlocking {
            val persisted = AtomicBoolean(true)
            val writeDone = CountDownLatch(1)
            val store = AboutMetadataCacheStore()
            val metadataSource = AndroidAboutMetadataSource(
                loadVersionInfo = { "2.3.0" to 23L },
                readStartupCheck = { persisted.get() },
                writeStartupCheck = { enabled ->
                    persisted.set(enabled)
                    writeDone.countDown()
                },
                ioDispatcher = Dispatchers.IO,
                cacheStore = store
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val viewModel = createViewModel(
                configRepository = FakeConfigRepository(cachedConfig = sampleConfig()),
                metadataSource = metadataSource,
                ioDispatcher = Dispatchers.IO,
                scope = scope
            )

            viewModel.setCheckUpdateOnStartup(false)
            scope.cancel()

            assertTrue(writeDone.await(2, TimeUnit.SECONDS))
            assertFalse(persisted.get())
            assertEquals(false, store.getCachedMetadata()?.checkUpdateOnStartup)
        }
    }

    @Test
    fun `AndroidAboutMetadataSource 在异步读取期间发生写入时不被旧快照脏覆盖`() {
        runBlocking {
            val persisted = AtomicReference(true)
            val readStarted = CountDownLatch(1)
            val releaseRead = CountDownLatch(1)
            val store = AboutMetadataCacheStore()
            val metadataSource = AndroidAboutMetadataSource(
                loadVersionInfo = { "2.4.0" to 24L },
                readStartupCheck = {
                    readStarted.countDown()
                    releaseRead.await(2, TimeUnit.SECONDS)
                    true
                },
                writeStartupCheck = { enabled ->
                    persisted.set(enabled)
                },
                ioDispatcher = Dispatchers.IO,
                cacheStore = store
            )

            val loadDeferred = async(Dispatchers.IO) {
                metadataSource.loadMetadata()
            }
            assertTrue(readStarted.await(2, TimeUnit.SECONDS))

            metadataSource.setCheckUpdateOnStartup(false)
            releaseRead.countDown()
            val loaded = loadDeferred.await()

            assertFalse(loaded.checkUpdateOnStartup)
            assertFalse(persisted.get())
            assertEquals(false, metadataSource.getCachedMetadata()?.checkUpdateOnStartup)
        }
    }

    @Test
    fun `连续快速切换开关最终持久化与 UI 状态收敛到最后一次操作`() {
        runBlocking {
            val persisted = AtomicReference(true)
            val store = AboutMetadataCacheStore()
            val metadataSource = AndroidAboutMetadataSource(
                loadVersionInfo = { "2.5.0" to 25L },
                readStartupCheck = { persisted.get() },
                writeStartupCheck = { enabled -> persisted.set(enabled) },
                ioDispatcher = Dispatchers.Unconfined,
                cacheStore = store
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val viewModel = createViewModel(
                    configRepository = FakeConfigRepository(cachedConfig = sampleConfig()),
                    metadataSource = metadataSource,
                    ioDispatcher = Dispatchers.Unconfined,
                    scope = scope
                )
                viewModel.setCheckUpdateOnStartup(false)
                viewModel.setCheckUpdateOnStartup(true)
                viewModel.setCheckUpdateOnStartup(false)

                assertFalse(viewModel.uiState.value.checkUpdateOnStartup)
                assertFalse(persisted.get())
                assertEquals(false, metadataSource.getCachedMetadata()?.checkUpdateOnStartup)
            } finally {
                scope.cancel()
            }
        }
    }

    private fun createViewModel(
        configRepository: ConfigRepository,
        metadataSource: AboutMetadataSource,
        settingsStyleFlow: Flow<SettingIconStyle> = MutableStateFlow(SettingIconStyle.VIBRANT_SOLID),
        ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        scope: CoroutineScope
    ): AboutViewModel {
        return AboutViewModel(
            configRepository = configRepository,
            settingsRepository = fakeSettingsRepository(settingsStyleFlow),
            metadataSource = metadataSource,
            ioDispatcher = ioDispatcher,
            stringProvider = { resId ->
                when (resId) {
                    R.string.settings_about_label_no_log -> "no_log_str"
                    R.string.settings_about_label_fetch_failed -> "fetch_failed_str"
                    else -> "res_$resId"
                }
            },
            coroutineScopeOverride = scope
        )
    }

    private fun fakeSettingsRepository(styleFlow: Flow<SettingIconStyle>): SettingsRepository {
        return Proxy.newProxyInstance(
            SettingsRepository::class.java.classLoader,
            arrayOf(SettingsRepository::class.java)
        ) { proxy, method, _ ->
            when (method.name) {
                "getSettingIconStyleFlow" -> styleFlow
                "toString" -> "FakeSettingsRepository"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> false
                else -> throw UnsupportedOperationException("Unexpected method: ${method.name}")
            }
        } as SettingsRepository
    }

    private fun sampleConfig(
        versionCode: Int = 20,
        versionName: String = "2.0.0",
        updateLog: String = "• 测试日志"
    ): AppUpdateConfig = AppUpdateConfig(
        versionCode = versionCode,
        versionName = versionName,
        updateLog = updateLog,
        downloadUrl = "https://example.com/nemo.apk",
        isForce = false,
        canClose = true
    )

    private class FakeConfigRepository(
        private val cachedConfig: AppUpdateConfig?,
        private val remoteHandler: suspend (Boolean) -> AppUpdateConfig? = { cachedConfig }
    ) : ConfigRepository {
        val getUpdateConfigCalls = mutableListOf<Boolean>()

        override suspend fun getUpdateConfig(forceRefresh: Boolean): AppUpdateConfig? {
            getUpdateConfigCalls.add(forceRefresh)
            return remoteHandler(forceRefresh)
        }

        override suspend fun getCachedUpdateConfig(): AppUpdateConfig? = cachedConfig

        override fun getCurrentVersionCode(): Int = 1
    }

    private class FakeAboutMetadataSource(
        private var cached: AboutLocalMetadata? = null,
        private val loaded: AboutLocalMetadata? = null,
        private val loadDeferred: CompletableDeferred<AboutLocalMetadata>? = null
    ) : AboutMetadataSource {
        val savedStartupFlags = mutableListOf<Boolean>()

        override fun getCachedMetadata(): AboutLocalMetadata? = cached

        override suspend fun loadMetadata(): AboutLocalMetadata {
            val result = loadDeferred?.await()
                ?: requireNotNull(loaded) { "Either loaded or loadDeferred must be provided" }
            cached = result
            return result
        }

        override suspend fun setCheckUpdateOnStartup(enabled: Boolean) {
            savedStartupFlags.add(enabled)
            cached = cached?.copy(checkUpdateOnStartup = enabled)
        }
    }
}
